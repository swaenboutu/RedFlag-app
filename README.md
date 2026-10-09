# Red Flag (Android)

An app for **intentional, ethical friction**: when you open an app you have flagged as problematic yourself, a screen reminds
you of your values and asks whether you really want to continue. The goal is not to block, but to create a conscious pause.

Name: **Red Flag**, subtitle "Ethical app check" (store listing title: "Red Flag – Ethical app check"). Both are defined by
`app_name` and `app_subtitle` in [brand.xml](app/src/main/res/values/brand.xml) and used everywhere (icon, screens, accessibility
service); the title of this README and the website (`docs/`) have to be changed by hand. The technical identifier
`app.redflag` is the one from before the name was chosen: it cannot change after the first publication.

Specifications: [Android](pitch-dev-android.md), [iOS](pitch-dev-ios.md), [design](pitch-designer.md) (in French).
License: [GPL v3](LICENSE) (copyleft: any modified version that is distributed must remain under the GPL, with its source code open).

## Stack

Kotlin, MVVM, Room (KSP), XML views + ViewBinding, Material 3. `minSdk 26`, `targetSdk 37`.
100% local: no backend, no telemetry, backups disabled.

## Structure

```
app/src/main/java/app/redflag/
├── data/      Room (entities, one DAO per table, database), repository, installed apps list, icon cache
├── service/   FrictionAccessibilityService (foreground app detection), FrictionGate
├── ui/        One folder per area: apps (list, app detail), problems (list and detail of the issues),
│              stats, settings (Settings and FAQ), interruption ("One second" screen), onboarding, common (shared pieces)
└── util/      Pure utilities (parseProblems)
```

`fixtures/`: two empty apps (Fixture A and B), installed by `scripts/test.sh` during the device tests so that the screens have
ordinary apps to list (a basic emulator only has system apps, which are hidden), then removed. Never published.

## Running

```
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest         # unit tests (no device)
gradlew.bat :app:connectedDebugAndroidTest # device tests (emulator running): migrations, database, screens
```

To avoid replaying the whole suite after every change, `scripts/test.sh` (Git Bash) picks the tests:

```
scripts/test.sh focus FrictionGateTest InterstitialActivityTest   # only these classes (unit or device, detected automatically)
scripts/test.sh commit                                            # all unit tests, before a commit
scripts/test.sh auto                                              # unit tests + the device tests concerned by the modified files (reports files that have no test)
scripts/test.sh full                                              # before a push or a release: everything + lint
```

A `pre-push` hook (`scripts/hooks/`) runs `full` before every `git push` and cancels the push if it fails (an emulator is
required; `git push --no-verify` to bypass it). After a clone: `git config core.hooksPath scripts/hooks`.

The device tests clear the app's data at the start, then reinstall it (as debug) at the end, re-enabling its accessibility
service if it was on: the app therefore stays on the emulator (only its data is reset).

Open the folder in Android Studio, then run on the AVD. Then enable the service in
*Settings > Accessibility > Red Flag* (the banner on the main screen leads there).

## Publishing a test version (GitHub Releases)

`scripts/release.sh` builds the signed production APK in `build/release/` (with its SHA-256 checksum); with `--publish`, it creates the
GitHub release (pre-release) after confirmation. It needs the GitHub CLI (`winget install GitHub.cli`, then `gh auth login`), a clean
working tree, pushed commits, and a version (`versionName`/`versionCode` in `app/build.gradle.kts`) that has not been published yet.
Without the CLI, attach the APK by hand: GitHub > Releases > Draft a new release.

The signing key (`~/.android-keys/conscience-numerique.jks`) and `keystore.properties` are never versioned: **back them up**
(an app signed with another key can no longer be updated over the old one). On the tester's phone: allow installation from the
chosen source, then, on Android 13 and later, allow the app's "restricted settings" before enabling the accessibility service.

## Website (docs/)

A static website, with no server or dependency: one page per language (`docs/index.html` (English) and `docs/fr/` (French)), an
anchor menu, the FAQ, screenshots and a contact form. No external resource (fonts and scripts are in `docs/assets/`).

```bash
python -m http.server 8000 -d docs     # preview at http://localhost:8000
node scripts/site.mjs sync-faq         # copies the app's FAQ (res/raw/faq.xml and raw-fr/faq.xml) into both pages
node scripts/site.mjs check            # links, anchors, images, languages, FAQ up to date, contact address (also run by scripts/test.sh)
node scripts/site.mjs set-email address@example.com   # contact address: currently example@email.com
```

The FAQ therefore has a single source (the app's XML files): edit it there, then run `sync-faq`. The form prepares a message in the
visitor's mail client (a `mailto:` link): no third-party service receives their data. Possible hosting: GitHub Pages (branch
`main`, folder `/docs`; a private repository requires a paid account) or any static file host.

To complete when the name is final: the name (headers, titles, footer, privacy policy), the contact address, the download link, the
date of the privacy policy, and the screenshots if the interface changes.

## Issues and languages

The predefined catalog (7 categories, 23 issues) is in `data/ProblemCatalog.kt`. Each entry has a **stable key** stored in the
database; its label comes from the resources: `values/strings.xml` (en-US, the default) and `values-fr/strings.xml` (fr-FR). To
add an issue: one entry in the catalog + one `problem_*` string in **both** files. Custom issues are free text, displayed as is
in every language. The app's language can be changed in the Android settings (`locales_config.xml`).

## Design choices to know

- "No" sends the user back to the home screen; the target app is not killed (Android does not allow it without a dedicated
  permission).
- The service reads **no screen content** (`canRetrieveWindowContent="false"`).
- The icons of third-party apps are loaded through `PackageManager`, never bundled.
