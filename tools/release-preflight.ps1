[CmdletBinding()]
param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [switch]$SkipBuild,
    [string]$BundletoolPath,
    [string]$GitleaksPath,
    [string]$PrivacyUrl = "https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html",
    [string]$ReportPath = "",
    [switch]$DependencyOnly
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path -LiteralPath $RepoRoot).Path
if ([string]::IsNullOrWhiteSpace($ReportPath)) {
    $ReportPath = Join-Path $RepoRoot "build/reports/release-preflight.txt"
}

$results = [System.Collections.Generic.List[object]]::new()
$hasFailure = $false

function Add-Result {
    param(
        [ValidateSet("PASS", "FAIL", "OWNER ACTION", "PHYSICAL DEVICE REQUIRED")]
        [string]$Status,
        [string]$Check,
        [string]$Evidence
    )
    if ($Status -eq "FAIL") { $script:hasFailure = $true }
    $script:results.Add([pscustomobject]@{ Status = $Status; Check = $Check; Evidence = $Evidence })
    Write-Output ("{0,-24} {1}: {2}" -f $Status, $Check, $Evidence)
}

function Invoke-Tool {
    param([string]$FilePath, [string[]]$Arguments)
    $output = (& $FilePath @Arguments 2>&1 | Out-String).Trim()
    return [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = $output }
}

function Get-SdkDirectory {
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) { return $env:ANDROID_HOME }
    $localProperties = Join-Path $RepoRoot "local.properties"
    if (Test-Path -LiteralPath $localProperties) {
        $line = Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line -and $line -match '^sdk\.dir=(.+)$') {
            return ($Matches[1] -replace '\\:', ':' -replace '\\\\', '\')
        }
    }
    return $null
}

Write-Output "WallpaperCropFixer release preflight"
Write-Output "Repository: $RepoRoot"
Write-Output ""

$requiredFiles = @(
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "PRIVACY.md",
    "QA_CHECKLIST.md",
    "docs/PLAY_RELEASE_PACKET.md",
    "docs/signing-policy.json",
    ".github/workflows/ci.yml",
    ".github/workflows/pages.yml"
)
foreach ($relativePath in $requiredFiles) {
    if (Test-Path -LiteralPath (Join-Path $RepoRoot $relativePath)) {
        Add-Result "PASS" "required file $relativePath" "present"
    } else {
        Add-Result "FAIL" "required file $relativePath" "missing"
    }
}

$buildFile = Get-Content -Raw -LiteralPath (Join-Path $RepoRoot "app/build.gradle.kts")
$catalog = Get-Content -Raw -LiteralPath (Join-Path $RepoRoot "gradle/libs.versions.toml")
if ($buildFile -match 'compileSdk\s*=\s*36' -and $buildFile -match 'targetSdk\s*=\s*36' -and $buildFile -match 'minSdk\s*=\s*26') {
    Add-Result "PASS" "SDK policy" "compile/target 36, min 26"
} else {
    Add-Result "FAIL" "SDK policy" "compile/target/min values do not match 36/36/26"
}
if ($buildFile -match 'create\("releaseVerification"\)' -and $buildFile -match 'signingConfig\s*=\s*signingConfigs\.getByName\("debug"\)') {
    Add-Result "PASS" "verification build semantics" "releaseVerification is explicit and debug-signed"
} else {
    Add-Result "FAIL" "verification build semantics" "missing explicit verification build"
}
if ($buildFile -match 'RELEASE_STORE_FILE' -and $buildFile -match 'RELEASE_STORE_PASSWORD' -and
    $buildFile -match 'RELEASE_KEY_ALIAS' -and $buildFile -match 'RELEASE_KEY_PASSWORD' -and
    $buildFile -notmatch 'WCF_UPLOAD_') {
    Add-Result "PASS" "signing contract" "only runtime RELEASE_* names are present"
} else {
    Add-Result "FAIL" "signing contract" "legacy or incomplete signing contract"
}
if ($catalog -notmatch '(?m)^\s*[A-Za-z0-9._-]+\s*=\s*"[^"]*(\+|SNAPSHOT|latest\.release)' -and
    $buildFile -notmatch '"[^"]*(SNAPSHOT|latest\.release)' -and
    $buildFile -notmatch '"[0-9][^"]*\+') {
    Add-Result "PASS" "dependency version policy" "no dynamic or snapshot versions found"
} else {
    Add-Result "FAIL" "dependency version policy" "dynamic or snapshot dependency version found"
}

# Single source of truth for the CI "Dependency declaration policy" job: it runs
# this script with -DependencyOnly, so both gates enforce the identical rule.
if ($DependencyOnly) {
    if ($hasFailure) { exit 1 }
    exit 0
}

$forbiddenFiles = Get-ChildItem -LiteralPath $RepoRoot -Recurse -File -Force |
    Where-Object {
        $_.FullName -notmatch '[\\/]\.git([\\/]|$)' -and
        $_.FullName -notmatch '[\\/](build|\.gradle|\.kotlin)([\\/]|$)' -and
        $_.Name -match '\.(jks|keystore|p12|pfx|pem|key)$'
    }
if ($forbiddenFiles.Count -eq 0) {
    Add-Result "PASS" "forbidden signing files" "none in source tree"
} else {
    Add-Result "FAIL" "forbidden signing files" ($forbiddenFiles.FullName -join "; ")
}
$trackedLocalProperties = (& git -C $RepoRoot ls-files --error-unmatch local.properties 2>$null | Out-String).Trim()
if ([string]::IsNullOrWhiteSpace($trackedLocalProperties)) {
    Add-Result "PASS" "local configuration" "local.properties is not tracked"
} else {
    Add-Result "FAIL" "local configuration" "local.properties is tracked"
}

[string]$manifestPath = Join-Path $RepoRoot "app/src/main/AndroidManifest.xml"
$mergedManifest = Get-ChildItem -LiteralPath (Join-Path $RepoRoot "app/build/intermediates/merged_manifests/releaseVerification") -Filter "AndroidManifest.xml" -Recurse -File -ErrorAction SilentlyContinue | Select-Object -First 1
if ($mergedManifest) {
    $manifestPath = $mergedManifest.FullName
    Add-Result "PASS" "merged release manifest" "auditing $($mergedManifest.FullName)"
} else {
    Add-Result "OWNER ACTION" "merged release manifest" "run the verification build before relying on final permission evidence"
}
[xml]$manifest = Get-Content -Raw -LiteralPath $manifestPath
$androidNamespace = "http://schemas.android.com/apk/res/android"
$permissions = @($manifest.manifest.ChildNodes | Where-Object { $_.LocalName -eq "uses-permission" } | ForEach-Object {
    $_.GetAttribute("name", $androidNamespace)
})
$allowedPermissions = @(
    "android.permission.SET_WALLPAPER",
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE",
    "com.wallpapercropfixer.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
)
$unexpected = @($permissions | Where-Object { $_ -and $_ -notin $allowedPermissions })
if ($unexpected.Count -eq 0) {
    Add-Result "PASS" "merged release manifest permissions" (($permissions -join ", ") + "; no unexpected permissions")
} else {
    Add-Result "FAIL" "merged release manifest permissions" ("unexpected: " + ($unexpected -join ", "))
}

$privacy = Get-Content -Raw -LiteralPath (Join-Path $RepoRoot "PRIVACY.md")
if ($privacy -match 'OWNER_PROVIDE_' -or $privacy -match 'github\.com/.+/issues') {
    Add-Result "OWNER ACTION" "privacy contact" "a durable public contact address must be supplied"
} else {
    Add-Result "PASS" "privacy contact" "policy contains a non-issue contact mechanism"
}
if ($privacy -match 'ML Kit' -and $privacy -match 'diagnostic' -and $privacy -match 'retention') {
    Add-Result "PASS" "privacy disclosure" "ML Kit diagnostics and retention sections present"
} else {
    Add-Result "FAIL" "privacy disclosure" "required SDK/retention disclosure is incomplete"
}
Add-Result "OWNER ACTION" "public privacy URL" "$PrivacyUrl must return unauthenticated HTTP success after Pages is enabled"
Add-Result "PHYSICAL DEVICE REQUIRED" "runtime privacy" "capture app and ML Kit traffic; verify no photo bytes leave the device"
Add-Result "PHYSICAL DEVICE REQUIRED" "wallpaper behavior" "verify launcher-specific HOME/LOCK/BOTH behavior on reference devices"

if (-not $SkipBuild) {
    $gradle = if ($IsWindows) { Join-Path $RepoRoot "gradlew.bat" } else { Join-Path $RepoRoot "gradlew" }
    if (Test-Path -LiteralPath $gradle) {
        if (-not $IsWindows) { & chmod +x $gradle }
        $buildResult = Invoke-Tool $gradle @(
            ":app:lintDebug",
            ":app:lintReleaseVerification",
            ":app:testDebugUnitTest",
            ":app:assembleDebug",
            ":app:assembleReleaseVerification",
            ":app:bundleReleaseVerification",
            "--no-daemon"
        )
        if ($buildResult.ExitCode -eq 0) {
            Add-Result "PASS" "verification build" "lint, tests, debug APK, verification APK, verification AAB"
        } else {
            Add-Result "FAIL" "verification build" $buildResult.Output
        }
    } else {
        Add-Result "FAIL" "verification build" "Gradle wrapper missing"
    }
}

$apk = Get-ChildItem -LiteralPath (Join-Path $RepoRoot "app/build/outputs/apk/releaseVerification") -Filter "*.apk" -File -ErrorAction SilentlyContinue | Select-Object -First 1
$aab = Get-ChildItem -LiteralPath (Join-Path $RepoRoot "app/build/outputs/bundle/releaseVerification") -Filter "*.aab" -File -ErrorAction SilentlyContinue | Select-Object -First 1
$sdk = Get-SdkDirectory
$zipalign = $null
$readelf = $null
if ($sdk -and (Test-Path -LiteralPath $sdk)) {
    $zipalign = Get-ChildItem -LiteralPath (Join-Path $sdk "build-tools") -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^zipalign(\.exe)?$' } | Sort-Object FullName | Select-Object -Last 1
    $readelf = Get-Command llvm-readelf -ErrorAction SilentlyContinue
    if (-not $readelf) {
        $readelf = Get-ChildItem -LiteralPath $sdk -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^llvm-readelf(\.exe)?$' } | Select-Object -First 1
    }
}

if ($apk -and $zipalign) {
    $zipResult = Invoke-Tool $zipalign.FullName @("-c", "-P", "16", "-v", "4", $apk.FullName)
    if ($zipResult.ExitCode -eq 0) {
        Add-Result "PASS" "APK ZIP ALIGNMENT" "zipalign -c -P 16 verified $($apk.Name)"
    } else {
        Add-Result "FAIL" "APK ZIP ALIGNMENT" $zipResult.Output
    }
} else {
    Add-Result "FAIL" "APK ZIP ALIGNMENT" "verification APK or Android SDK zipalign missing"
}

if ($aab -and $BundletoolPath -and (Test-Path -LiteralPath $BundletoolPath)) {
    $bundleResult = Invoke-Tool "java" @("-jar", $BundletoolPath, "dump", "config", "--bundle=$($aab.FullName)")
    if ($bundleResult.ExitCode -eq 0 -and $bundleResult.Output -match "PAGE_ALIGNMENT_16K") {
        Add-Result "PASS" "AAB PAGE ALIGNMENT" "bundletool reports PAGE_ALIGNMENT_16K"
    } else {
        Add-Result "FAIL" "AAB PAGE ALIGNMENT" $bundleResult.Output
    }
} else {
    Add-Result "FAIL" "AAB PAGE ALIGNMENT" "verification AAB or pinned bundletool path missing"
}

if ($aab -and $readelf) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $elfDir = Join-Path $RepoRoot "build/reports/release-preflight-elf"
    New-Item -ItemType Directory -Force -Path $elfDir | Out-Null
    $archive = [System.IO.Compression.ZipFile]::OpenRead($aab.FullName)
    try {
        $nativeEntries = @($archive.Entries | Where-Object { $_.FullName -match '(^|/)lib/[^/]+/[^/]+\.so$' })
        $badElfs = [System.Collections.Generic.List[string]]::new()
        foreach ($entry in $nativeEntries) {
            $safeName = ($entry.FullName -replace '[\\/]', "_")
            $nativePath = Join-Path $elfDir $safeName
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $nativePath, $true)
            $readelfExecutable = if ($readelf.PSObject.Properties.Name -contains "Source") { $readelf.Source } else { $readelf.FullName }
            $elfResult = Invoke-Tool $readelfExecutable @("-lW", $nativePath)
            $loadLines = @($elfResult.Output -split "`r?`n" | Where-Object { $_ -match '\bLOAD\b' })
            if ($elfResult.ExitCode -ne 0 -or $loadLines.Count -eq 0 -or @($loadLines | Where-Object { $_ -notmatch '0x4000' }).Count -gt 0) {
                $badElfs.Add($entry.FullName)
            }
        }
        if ($nativeEntries.Count -gt 0 -and $badElfs.Count -eq 0) {
            Add-Result "PASS" "NATIVE ELF ALIGNMENT" "all $($nativeEntries.Count) packaged .so files have 16 KB LOAD alignment"
        } elseif ($nativeEntries.Count -eq 0) {
            Add-Result "PASS" "NATIVE ELF ALIGNMENT" "no native .so files packaged"
        } else {
            Add-Result "FAIL" "NATIVE ELF ALIGNMENT" ("misaligned or unreadable: " + ($badElfs -join ", "))
        }
    } finally {
        $archive.Dispose()
    }
} else {
    Add-Result "FAIL" "NATIVE ELF ALIGNMENT" "verification AAB or llvm-readelf missing"
}

if ($GitleaksPath -and (Test-Path -LiteralPath $GitleaksPath)) {
    $secretResult = Invoke-Tool $GitleaksPath @("detect", "--source", $RepoRoot, "--redact", "--no-banner")
    if ($secretResult.ExitCode -eq 0) {
        Add-Result "PASS" "secret scan" "gitleaks found no secrets"
    } else {
        Add-Result "FAIL" "secret scan" $secretResult.Output
    }
} else {
    Add-Result "OWNER ACTION" "secret scan" "run the pinned CI gitleaks binary or provide -GitleaksPath"
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $ReportPath) | Out-Null
$results | ForEach-Object { "{0,-24} {1}: {2}" -f $_.Status, $_.Check, $_.Evidence } | Set-Content -LiteralPath $ReportPath -Encoding UTF8
Write-Output ""
Write-Output "Report: $ReportPath"
if ($hasFailure) { exit 1 }
exit 0
