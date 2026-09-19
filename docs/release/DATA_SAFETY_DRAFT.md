# Google Play Data Safety draft — RC2 preparation

Package: `com.wallpapercropfixer`. Policy source: [PRIVACY.md](../../PRIVACY.md).
Reviewed official sources on 2026-09-19; final Console declaration remains an owner action.

## Documented SDK facts

[Google ML Kit disclosure](https://developers.google.com/ml-kit/android-data-disclosure) includes bundled-feature installation identifiers, device/app metadata and diagnostic/performance events. Transport is HTTPS; Google states this data is not transferred to third parties. On-device inference does not mean zero SDK collection. The pinned bundled Face Detection dependency is `16.1.7`; confirm applicable SDK guidance when upgrading.

[Google Play guidance](https://support.google.com/googleplay/android-developer/answer/10787469) requires SDK collection in the declaration. Local-only photo processing is not collection. Optional collection requires user choice; ephemeral treatment requires the documented processing conditions, not merely an absence of observed traffic.

## Proposed form answers (inferred mapping; owner must confirm)

- **Collects data: Yes.** Do not paste the superseded "No data collected" answer.
- **Shares data: No**, based on the SDK disclosure and absence of app-owned transfers.
- **Encrypted in transit: Yes**, for the documented SDK diagnostics.

| Data type | Collection | Purpose | Optional / ephemeral |
|---|---|---|---|
| Device or other IDs | Yes: installation identifiers | Analytics | Required / No (conservative; no verified opt-out or ephemeral retention evidence) |
| App info and performance: Diagnostics; Other app performance data | Yes: SDK diagnostics/performance | Analytics | Required / No, same basis |
| Photos/videos and face results | No: processed locally | App functionality locally | Not applicable |

Do not promise SDK data deletion: no app-operated deletion mechanism is established. Confirm the corresponding Console answer and provider behavior before submission. The face-aware toggle is not a verified telemetry opt-out. Device/app metadata is included in the diagnostics mapping; confirm taxonomy against the actual Console form.

## Remaining evidence

Physical privacy traffic capture has **not** been performed for this candidate. Record exact build provenance, destinations and absence of photo uploads. Retain documented SDK collection even if a short capture is quiet. Owner must supply developer entity/contact, check the public policy URL and submit the form. No Play Console validation is claimed.
