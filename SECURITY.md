# Security Policy — WallpaperCropFixer

## Project status: proprietary, not open source

WallpaperCropFixer is proprietary software. The source is published for source visibility and transparency only. The repository [LICENSE](LICENSE) is a proprietary license notice that grants no permission to copy, modify, redistribute, or build derivative works. The [README](README.md) states this in its opening banner.

Because no permission to use the code has been granted, a defect in it is not a "vulnerability" in the open-source sense. It is a question about unauthorized use of unlicensed software, and that question belongs to the owner of the code, not to a public disclosure process. This file exists so the boundary is stated plainly instead of left to inference.

## What this repository does not offer

- **No security support.** The maintainer does not triage, investigate, or remediate security reports for WallpaperCropFixer.
- **No coordinated disclosure program.** There is no embargo, no safe harbor, and no private disclosure window.
- **No bug bounty.** No reward is offered.
- **No response-time commitment.** There is no SLA and no support window.
- **No supported versions.** No release is a supported security-fix channel.

## Documented integrity controls

The signing model is the most consequential control in this repository, and it is written down rather than assumed.

- **Signing keys are never stored in the repository.** The release build task fails closed unless the isolated release domain injects `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` at runtime. The [README](README.md) states directly: never store those values in this repository or in ordinary CI.
- **The verification build is not an upload artifact.** `:app:assembleReleaseVerification` and `:app:bundleReleaseVerification` are debug-signed and are not upload-ready. The verification AAB must never be used for a Play upload. [docs/signing-policy.json](docs/signing-policy.json) records this contract, including `fail_closed_when_inputs_missing: true` and an explicit instruction to record the upload certificate without committing private key material.
- **Key holders are separated.** App-signing key holder is Google Play; upload key holder is the dedicated release domain. This separation is recorded in the same policy file, and its configured state is tracked there rather than assumed.

If a signing secret is ever exposed, treat it as compromised: rotate it through the release domain and review the full history before any public push.

## Data handling

[PRIVACY.md](PRIVACY.md) documents the data model: on-device photo processing, no photo-library permission, system Photo Picker selection, `allowBackup` disabled for wallpaper bitmaps, and the ML Kit SDK diagnostic telemetry disclosure. Note that the ML Kit SDK is a third party; issues in it belong to Google, not here. [QA_CHECKLIST.md](QA_CHECKLIST.md) and [STATUS.md](STATUS.md) are the project's own verification record.

## Reporting a genuine concern

If you believe you have found a genuine security concern, the honest position is that the maintainer has not accepted a support obligation, so there is no guaranteed response. If you choose to raise it anyway:

- Prefer GitHub's private vulnerability reporting for this repository (the **Security** tab → **Report a vulnerability**), if it is available to you.
- Otherwise contact the repository owner through their public profile at <https://github.com/gthgomez>.
- If the concern involves a signing key or upload credential, say so explicitly and first, and do not paste the value into any report.
- You receive no service commitment, no bounty, and no assurance of a fix.

## Visibility is not permission

The repository being public creates no support obligation. Publishing source does not grant a license, does not create a support contract, and does not make the maintainer a vendor to you. Opening an issue or submitting a pull request grants you no rights and creates no partnership; contributions are not accepted for reuse, and no license is granted over anything you send here.
