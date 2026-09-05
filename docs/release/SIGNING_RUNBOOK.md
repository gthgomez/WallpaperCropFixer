# SIGNING_RUNBOOK.md — Owner-only signing procedure (Windows, offline)

**Only Jonathan runs these commands, on his own machine, with his own JDK.** Never hand the keystore or passwords to anyone — including the release agent.
Repo facts: `.gitignore` already blocks `*.jks` / `*.keystore`; release builds **fail closed** without the four `RELEASE_*` variables.

## 1. Generate the upload key (once)
In PowerShell, from your JDK's `bin` directory (or with `keytool` on PATH):

```powershell
keytool -genkeypair -v `
  -keystore wcf-upload.jks `
  -alias wcf-upload `
  -keyalg RSA -keysize 2048 -validity 10000
```
(No `-storetype` flag: modern keytool defaults to PKCS12, which is the recommended format. The `.jks` extension is kept only out of habit — any filename works.)

You will be prompted for:
- **Keystore password** and **key password** — see §2.
- Distinguished name — suggested: your name `Jonathan Gomez Aguilar`, org = personal, `OU=` blank, country code as appropriate. This is embedded in the certificate; keep it accurate.

Result: `wcf-upload.jks` with a 2048-bit RSA key, alias `wcf-upload`, valid ~27 years (10000 days ≥ any realistic app lifetime).

## 2. Password guidance
- Minimum **20 characters**, unique to this keystore, not reused anywhere (no password-manager sharing across contexts, no dictionary phrases you've used elsewhere).
- Store the passwords in a password manager **now**, in the same entry as the keystore location. An unrecoverable password = unusable upload key.

## 3. Backup guidance
- Keep the working copy in an **encrypted** location (BitLocker/encrypted volume or encrypted archive).
- One **separate offline copy** on a different physical medium (USB drive stored elsewhere), also encrypted.
- Losing the upload key **after** Play App Signing enrollment is recoverable via Google's key-reset process, but it takes days and support contact. Losing it **before/without** enrollment means you can never update the app under this package name. Treat this file like a house key.

## 4. Environment variables for a local signed build
The Gradle release tasks read exactly these four names:

```powershell
# PowerShell — current session
$env:RELEASE_STORE_FILE = "C:\path\to\wcf-upload.jks"
$env:RELEASE_STORE_PASSWORD = "<keystore password>"
$env:RELEASE_KEY_ALIAS = "wcf-upload"
$env:RELEASE_KEY_PASSWORD = "<key password>"
```

Then build in the **same** shell session:

```powershell
.\gradlew.bat :app:bundleRelease
```

(For a durable setup use `setx`, or a user-level environment variable panel — but never store the `.jks` path/passwords in any repo file, `gradle.properties`, or CI secret tied to a public repo.)

## 5. What must NEVER be committed or shared
- `wcf-upload.jks` (and any `*.jks` / `*.keystore` — already excluded by the repo's `.gitignore`).
- The four values from §4 (passwords especially).
- Never paste keystore contents or passwords into issues, chats, CI logs, or screenshots.

## 6. Build-task behavior (verified repository facts)
- `:app:assembleRelease` / `:app:bundleRelease` **FAIL CLOSED**: they throw a `GradleException` at configuration time when any of the four `RELEASE_*` env vars is absent. No accidental unsigned/incorrectly-signed release builds are possible.
- `:app:bundleReleaseVerification` is the **debug-signed CI/verification boundary**. It proves the bundle assembles and verifies; it is **NOT upload-ready**. Never upload it to Play Console.

## 7. Verify the signed AAB before upload
Using jarsigner (from the JDK):

```powershell
jarsigner -verify -verbose:summary app-release.aab
```

Expect the jar to be verified with certificate `CN=Jonathan Gomez Aguilar` (per §1 DN) and the phrase "jar verified."

Optional deeper check with bundletool (download from Google's bundletool GitHub releases):

```powershell
java -jar bundletool.jar build-apks --bundle=app-release.aab --output=check.apks --mode=universal
```

If this parses without signature errors, the bundle is structurally sound.

## 8. Play App Signing — what holds which key
- **Google holds the app signing key.** After your first AAB upload, Play enrolls the app in Play App Signing and generates/holds the key that will actually sign what users install. You never manage that key.
- **Your local `wcf-upload.jks` is only the upload key** — a passport proving updates come from you. Every future AAB upload is signed with it; Google re-signs with the app signing key.
- Enrollment happens on the **first AAB upload** in Play Console (no separate step). Keep the upload key per §3 forever, or use Play Console's key reset if it is ever lost.

## Checklist
- ☐ Keystore generated (§1), passwords in manager (§2)
- ☐ Encrypted local copy + offline backup (§3)
- ☐ Signed AAB built and jarsigner-verified (§4, §7)
- ☐ AAB uploaded by owner; Play App Signing enrolled (§8)
