# OWNER_ACTIONS.md — WallpaperCropFixer Release Dashboard

Owner: Jonathan Gomez Aguilar | Package: `com.wallpapercropfixer` | v1.0 (versionCode 1)
Prepared by: release agent, 2026-09-05. Everything below is action only the account/key holder can perform.
Support email: **OWNER_MUST_CONFIRM** (placeholder in docs; a real address is needed before submission).

---

## 1. DO NOW

| # | Action | Why | Where | What the agent prepared |
|---|--------|-----|-------|-------------------------|
| 1.1 | ~~Enable GitHub Pages~~ **DONE by agent 2026-09-05** — Pages enabled and the privacy URL verified live (HTTP 200) | Privacy policy is a hard Play requirement; it must stay reachable from now on | `https://gthgomez.github.io/WallpaperCropFixer/PRIVACY.html` | Privacy page source in repo; deploy workflow runs on `PRIVACY.md` changes |
| 1.2 | Verify Play Console registration is complete and **identity verification** has started | New personal accounts require identity verification before app creation; closed-testing requirements then apply (12 testers / 14 days) | https://play.google.com/console | Play-side implications mapped in PLAY_SUBMISSION_CHECKLIST.md |
| 1.3 | Confirm GitHub Actions billing is healthy (no payment failure / spending cap block) | CI verification builds (debug-signed `bundleReleaseVerification`) run on Actions; a billing stop silently halts release verification | GitHub → Settings → Billing | N/A |
| 1.4 | Review and **merge licensing PR #3**; close superseded **PR #4** if agreed | Third-party license hygiene is a legal call only the owner can make | GitHub repo → Pull requests | Both PRs inspected; PR #3 supersedes PR #4 — summary of differences available on request |
| 1.5 | Confirm the real support email | Store listing and privacy policy both need a working contact address | — | Placeholder flagged `OWNER_MUST_CONFIRM` in every draft doc |

## 2. BEFORE INTERNAL / CLOSED TESTING

| # | Action | Why | Where | What the agent prepared |
|---|--------|-----|-------|-------------------------|
| 2.1 | Generate the upload keystore **offline, yourself** (2048-bit RSA, alias `wcf-upload`) | Upload key signs every AAB you upload; nobody else should ever hold it | Your own machine, per SIGNING_RUNBOOK.md | Exact `keytool` command, backup procedure, and env-var wiring in SIGNING_RUNBOOK.md |
| 2.2 | Run physical-device QA | Play review and real users will hit wallpaper behavior we must not ship broken; Samsung canvas behavior is the app's differentiator | Your devices, per DEVICE_QA_RUNBOOK.md | 20-check runbook with pass/fail/block template; Galaxy S25 Ultra primary, Pixel-class secondary |
| 2.3 | Create the Play Console app entry: **free, no ads, category Personalization** | Free/no-ads declarations are irreversible-after-publish choices that shape the listing | Play Console → All apps → Create app | Recommended values listed in PLAY_SUBMISSION_CHECKLIST.md |
| 2.4 | Open the **closed testing** track and add ≥ 12 testers | New personal accounts created after Nov 13, 2023 need 12 opted-in testers for 14 consecutive days before production access (VERIFIED_CURRENT_GOOGLE_REQUIREMENT: https://support.google.com/googleplay/android-developer/answer/14151465) | Play Console → Testing → Closed testing | Tester-recruit checklist and 14-day tracking notes in PLAY_SUBMISSION_CHECKLIST.md |

## 3. BEFORE PRODUCTION SUBMISSION

| # | Action | Why | Where | What the agent prepared |
|---|--------|-----|-------|-------------------------|
| 3.1 | Approve listing copy, screenshots, and feature graphic | The listing is the owner's public voice | Play Console → Grow → Store listing | STORE_LISTING_DRAFT.md: title/short/full description within verified limits, 5-screenshot plan, feature-graphic concept |
| 3.2 | Submit App content declarations: Data safety, content rating (IARC), target audience, **ads = none**, **app access = none** | Each declaration is a policy statement made under your account | Play Console → Policy and programs → App content | Ready-to-paste answers in DATA_SAFETY_DRAFT.md and PLAY_SUBMISSION_CHECKLIST.md |
| 3.3 | Upload the **owner-signed release AAB** to the closed testing track | The CI `bundleReleaseVerification` artifact is debug-signed and NOT upload-ready; only your locally signed build is | Play Console → Testing → Closed testing → Releases | Artifact naming and verification steps in SIGNING_RUNBOOK.md §7 |
| 3.4 | Decide rollout strategy for production (staged vs full) | Staged rollout limits blast radius of a v1.0 surprise | Play Console → Production → Releases | Recommendation: staged 20% → 100% after first clean week |

## 4. AFTER SUBMISSION

| # | Action | Why | Where |
|---|--------|-----|-------|
| 4.1 | Monitor review status; respond to any Play policy email within the stated deadline | Silent timeouts can kill the submission; only the account holder receives the emails | Play Console inbox + registered email |
| 4.2 | After 14 days of qualifying closed testing, apply for **production access** | Required gate for new personal accounts | Play Console → Production → Apply for access |
| 4.3 | Keep the closed test alive until production approval | Tester count dipping below 12 can invalidate the qualification window | Play Console → Testing → Closed testing |

---

**Rule of thumb:** if it needs your password, your key, or your legal judgment — it is on this page. Everything else the agent has already prepared in the sibling documents.
