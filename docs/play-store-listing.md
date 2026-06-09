# Google Play listing — National Grid: Live (Android)

UK-only release. Default language: English (United Kingdom). British spelling throughout.
Adapted from the iOS App Store listing (`national-grid-live/docs/app-store-listing.md`),
with Android-specific copy (home-screen widget, Material You theme colours, Material 3).

---

## App name  (max 30)
```
National Grid: Live
```
(19 chars)

## Short description  (max 80 — shown in search results & at the top of the listing)
```
Britain's live electricity: the generation mix, carbon intensity and demand.
```
(76 chars)

## Full description  (max 4000)
```
National Grid: Live is an independent app that displays publicly available open data about Great Britain's electricity grid. It is not affiliated with, endorsed by, or connected to National Grid plc, the National Energy System Operator (NESO), Elexon, or any government entity, and it does not represent or provide any government service.

All data comes from official, publicly available open sources:
• Elexon Insights (BMRS) — https://bmrs.elexon.co.uk
• National Energy System Operator (NESO) Data Portal — https://www.neso.energy/data-portal
• Carbon Intensity API, by NESO and the University of Oxford — https://carbonintensity.org.uk

National Grid: Live shows you how Great Britain's electricity is being generated right now — and how clean it is.

Open the app to see the live generation mix at a glance: how much power is coming from gas, wind, solar, nuclear, hydro, biomass and storage, alongside the current demand, carbon intensity and wholesale price. Everything refreshes automatically as new readings are published.

LIVE
• The current generation mix as a clear bar or donut chart
• Demand = generation + transfers, balanced the way the grid actually works
• Carbon intensity and market price for the latest period
• Imports and exports across the interconnectors to France, Norway, Belgium, Denmark, Ireland and the Netherlands
• Pumped-storage and battery storage

HISTORIC
• Past day, week, year and all-time views
• Trend graphs for price, emissions, demand, generation and transfers
• Tap any point on a graph to read the exact values

MADE FOR ANDROID
• A clean Material 3 interface that's quick to read at a glance
• A home-screen widget with live demand, price, carbon and the top sources
• Theme colours to suit you — Green, Sage, Grey, or Dynamic (Material You, matched to your wallpaper)
• Light and dark themes that follow your system setting
• Choose a bar chart or donut, or hide it
• No sign-in, no clutter

PRIVATE BY DESIGN
National Grid: Live collects no personal data, has no accounts, and contains no advertising or tracking of any kind.

DATA SOURCES
The app uses publicly available open data from the Elexon Insights Solution, the National Energy System Operator (NESO) Data Portal, and the Carbon Intensity API (a project by NESO and the University of Oxford Department of Computer Science). Contains BMRS data © Elexon Limited copyright and database right 2026.

National Grid: Live is an independent project and is not affiliated with National Grid plc, NESO or Elexon.
```

## What's new / Release notes  (max 500, v1.0)
```
Initial release.
```

---

## Graphic assets
| Asset | Spec | Status |
|---|---|---|
| App icon | 512×512 PNG, 32-bit (with alpha) | ✅ `docs/play/play-icon-512.png` (rendered from the adaptive launcher icon) |
| Feature graphic | 1024×500 PNG/JPEG, no alpha | ✅ `docs/play/feature-graphic-dark.png` / `-light.png` |
| Phone screenshots | 2–8, 1080×2160 (2:1), 24-bit PNG | ✅ `~/Desktop/ngl-play-screenshots/play-ready/` (8 shots) |
| Tablet / 7"/10" screenshots | optional | — (phone-only app) |
| Promo video | optional (YouTube URL) | — |

> The feature graphic uses the app's brand mark (segmented ring + bolt on the
> green→teal→blue gradient) with a Grey-theme phone mockup. Pick the dark or light
> variant. Source: `docs/play/feature-graphic.html` (re-render with the Chrome command in this repo's notes).

## Categorisation
- **App category:** Tools  _(Play's nearest equivalent to iOS "Utilities"; Weather is a viable alternative)_
- **Tags (chosen):** *News*, *Education* (kept tight — 2 clearly-relevant tags rather than padding to 5)

## Content rating  (IARC questionnaire)
- Answer "No" to every content question → expected rating **Everyone / PEGI 3**.
- No violence, no user-generated content, no data sharing, no ads.

## Data safety  (Play Console → App content → Data safety)
- **Does your app collect or share any user data?** → **No.**
- No data collected, no data shared, no data types. (Matches iOS "Data Not Collected".)
- Note: still complete the form even though the answer is "No" — it's mandatory.

## Privacy & support URLs
- **Privacy Policy URL (required):** https://jameswestgate.github.io/national-grid-live/privacy.html
- **Support / Website:** https://jameswestgate.github.io/national-grid-live/support.html  _(directs to GitHub issues; no email by choice — confirm Play accepts a no-email support page, otherwise a contact email is required in the Console "Store settings")_
- ⚠️ The privacy page currently reads as the iOS listing's policy — confirm its wording is platform-neutral (or add an Android line). Same "no data collected" stance applies.

## Target audience & content
- **Target age:** adults / general (not directed at children) — declare appropriately so it isn't classed as a "for families" app.
- **Ads:** contains no ads → declare "No ads".

## App access
- All functionality is available **without any login or special access** → declare "All functionality is available without special access".

## Countries / regions
- **United Kingdom only.**

## Pricing
- **Free**, no in-app purchases.

---

## Release build & signing (runbook)

Gradle is already wired: `app/build.gradle.kts` has a release `signingConfig` that reads
a **gitignored `keystore.properties`**; `keystore.properties` + `*.jks` are in `.gitignore`.
A release `bundleRelease` is verified to compile with R8/minify (≈4.4 MB AAB). Current
version: `versionCode 2`, `versionName 1.1` — bump these for each Play upload.

1. **Create your upload keystore** (you choose & keep the passwords — back this file up; losing it means you can't update the app):
   ```sh
   keytool -genkeypair -v -keystore upload-keystore.jks -storetype JKS \
     -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. **Wire it up:** `cp keystore.properties.template keystore.properties`, then fill in
   `storeFile` (path to the `.jks`), `storePassword`, `keyAlias` (`upload`), `keyPassword`.
3. **Build the signed bundle** (this machine's default `java` is JDK 11; the build needs 17+):
   ```sh
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
     ./gradlew :app:bundleRelease
   # → app/build/outputs/bundle/release/app-release.aab  (now signed)
   ```
4. **Play App Signing:** when you create the app in Play Console and upload this AAB,
   opt into Play App Signing — Google manages the *app signing key*; your keystore above
   is just the *upload key*. (Recommended; lets Google re-sign and protects you if the
   upload key is ever lost.)

---

### ⚠️ Before submitting — still your action
- Create a Google Play Developer account ($25 one-time) and the app in Play Console.
- **Signing & AAB:** follow the "Release build & signing" runbook above — create the upload keystore (`keytool`), fill `keystore.properties`, build the signed AAB. Gradle wiring is done; the keystore + passwords are yours to create and back up.
- ~~Export the 512×512 icon~~ — done (`docs/play/play-icon-512.png`).
- Ensure the `national-grid-live` repo is **public** so the privacy/support GitHub Pages links resolve.
- Complete Data safety, Content rating (IARC), Target audience, Ads, and App access declarations.
- Set a **Store settings contact email** (Play requires a developer contact email even if the support page has none).
