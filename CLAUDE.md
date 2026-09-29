# Active40

Native Android training journal (strength, manual running, daily check-in) for women 40+.
The implementation plan is the source of truth: `C:\Users\EmmaH\Downloads\Active40_Implementation_Plan.md`.
Design mockups: Active40 canvas, page "Green" (https://claude.ai/artifact/T5XzCuwjKt3EHvJRjh7fhx).

## Status

- Phases 0 to 10 implemented and verified on the `medium_phone` emulator (not yet on the Pixel 9).
- Open decisions (do not decide in code): final accent pair, payment model, insights, progression, languages.

## Build & Test

- `./gradlew assembleDebug`: debug build
- `./gradlew testDebugUnitTest`: unit tests (calculations, units, backup format)
- `./gradlew assembleRelease`: minified release (R8). Signed only when `ACTIVE40_KEYSTORE_PATH`,
  `ACTIVE40_KEYSTORE_PASSWORD`, `ACTIVE40_KEY_ALIAS`, `ACTIVE40_KEY_PASSWORD` are set
- ktlint config in `.editorconfig` (Composable naming allowed, max line 140)

## Stack

- Kotlin, Jetpack Compose, Material 3, Navigation Compose (type-safe `@Serializable` routes)
- Room (KSP), DataStore Preferences, kotlinx-serialization-json (export file)
- No DI library: `AppContainer` built in `Active40Application`, ViewModels via `ui/AppViewModel.kt` (`appViewModel {}`)
- Charts drawn with Compose Canvas (`ui/components/Charts.kt`), no chart library
- compileSdk 37 (required by current AndroidX), targetSdk 36, minSdk 26
- No INTERNET permission. Auto Backup disabled. Permissions: VIBRATE, POST_NOTIFICATIONS (rest timer)

## Structure

- `AppContainer`: database, repositories, `RestTimer`; seeds built-in exercises and symptoms on start
- `data/db/`: `Entities.kt`, `Daos.kt`, `BackupDao.kt`, `Converters.kt`, `Active40Database.kt` (schema version 1,
  schemas exported to `app/schemas/`)
- `data/`: repositories (`Repositories.kt`, `StrengthRepository.kt`), `SettingsRepository` (DataStore), `BuiltInData`
- `data/backup/`: export format (`BACKUP_FORMAT_VERSION`), parsing, integrity check, `BackupRepository`
- `domain/`: pure calculation functions with unit tests (units, pace, weekly totals, heaviest set, percentage change,
  balance, consistency, last time summary, run input validation, month grid, progress ranges)
- `timer/RestTimer.kt`: app-wide timestamp-based rest timer (vibrate in foreground, notification in background)
- `ui/navigation/`: `Routes.kt`, `Active40NavHost.kt` (`Active40Root`: onboarding gate, bottom bar on tabs only)
- `ui/<feature>/`: today, strength (session, detail, start sheet, finish sheet), run, checkin, recovery, history,
  progress, settings, onboarding, exercises (exercises and symptoms), routines
- `ui/components/`: shared composables (cards, buttons, fields, dialogs, FullScreen, pickers, chips, charts,
  markers, font-scale helpers)
- `ui/theme/`: `Active40Colors.kt` (all colour tokens), `Type.kt` (DM Sans + Plus Jakarta Sans for numbers),
  `Shape.kt`, `Theme.kt` (`LightSurfaceTheme` for Material pickers on light surfaces)
- `docs/`: privacy policy draft, Google Play Data safety answers

## Conventions

- All colours from `Active40Colors`; accent pair is temporary, change only those lines (and `res/values/colors.xml`
  icon colours) when the final pair is chosen
- Numbers: `NumberFont` (Plus Jakarta Sans, tabular figures) because DM Sans has no `tnum`
- Secondary text on glass cards uses `onGlassSecondary` (white) for contrast; `backgroundTop` is darkened to #4B6963
- All UI text in `res/values/strings.xml`. No em/en dashes or interpuncts as separators in UI text
- Store kg, meters, seconds; convert only for display. Dates as ISO strings in Room
- Missing check-in = no information; symptoms hidden (never deleted) when tracking is off
- Large font (fontScale >= 1.5): `AdaptiveRow` stacks side-by-side items; nav labels capped at 1.3x
- Finnish in code comments and commit messages
- Don't add screens, settings or features beyond the plan; ask when unclear
