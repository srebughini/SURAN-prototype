# SURAN Prototype v0 — M1

Native Android (Kotlin + Jetpack Compose), one module, package `app.suran`, minSdk 26, targetSdk 34.
Reference device: Honor 10 Lite, Android 9 / EMUI 9.

**Status:** built, installed and tested on the OnePlus 6 (ST1, ST2 and ST3, results in `docs/TESTING.md` §6; many items were
not recorded one by one). v0.1.8–v0.1.10 (app labels from Android, `home_ui_started`, NotificationGate phase 1) were built with Gradle
(Android Studio, debug) and are **only partly verified on the device**: labels verified by the user; `home_ui_started` and
NotificationGate seen working in the log; most decision branches, the 2FA whitelist, `requestRebind` and OxygenOS battery behaviour
are **not verified**. `m2-spike/` is not compiled. The authoring environment has no Android SDK or Gradle: builds and device tests
are run by the user.

## Files
```
build.gradle.kts, settings.gradle.kts, gradle.properties, gradle/wrapper/gradle-wrapper.properties
app/build.gradle.kts
app/src/main/AndroidManifest.xml          HOME + LAUNCHER filters, <queries>, no INTERNET
app/src/debug/AndroidManifest.xml         debug-only intent app.suran.OUTSIDE
app/src/main/res/                         theme, colors, icon (placeholder)
app/src/main/java/app/suran/
  Config.kt          Entry, Config, ConfigStore (config.json, atomic), normalize()
  Catalog.kt         curated catalog + candidate resolution
  Apps.kt            installed apps, launch, default-Home check, labels
  EventLog.kt        local events.jsonl (cap 5,000 lines)
  Session.kt         in-memory "Open once" + Outside loop guard
  SuranApp.kt        loads the theme, logs app_open{defaultHome} (process start)
  NotificationGate.kt  notification listener, phase 1: log-only, never touches a notification
  ThemeStore.kt      theme file (filesDir/theme: "dark" or "light")
  SuranViewModel.kt  single UiState StateFlow; logs home_ui_started{defaultHome}
  Theme.kt           tokens (light + dark) + text + press helper
  HomeScreen.kt, ChooseScreen.kt, HomeActivity.kt, OutsideActivity.kt
m2-spike/            NOT compiled: accessibility service, allowlist, instructions
```

## Build the APK
1. Android Studio Koala (2024.1) or newer, JDK 17. Open the `suran-prototype` folder and let Gradle sync
   (Studio creates the wrapper; or run `gradle wrapper --gradle-version 8.7`).
2. Build → Build APK(s) with the **debug** variant (needed for the Outside demo intent).
   CLI: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

## Install and set up the OnePlus 6
1. Settings → About phone → tap Build number 7 times; Developer options → USB debugging on.
2. `adb install -r app-debug.apk` (or copy the APK to the phone and allow "Install unknown apps" for your file manager).
3. Open SURAN from the normal launcher. Choose what the phone is for → "Start with these N".
4. Android 11 shows the system Home-role dialog (`RoleManager`). In ST1 SURAN was set as Home; the exact screen was not recorded.
   **Write down what the dialog looks like and what happens if you deny it.**
5. OxygenOS may stop a launcher according to its battery settings. The exact setting path on OxygenOS 11 is NOT verified
   (TS-08): find it on the device, apply it, and write it down.
6. Notifications of apps not chosen. SURAN does not suppress notifications. Since v0.1.10 an experimental, log-only service
   (`NotificationGate`) can record what it *would* do, but only if you grant it Notification access by hand (Android settings → Apps
   & notifications → Special app access → Notification access → SURAN; the exact path on OxygenOS 11 is unverified, write it down).
   Without that access nothing happens. To silence notifications today, turn them off app by app (concierge step).

*History (original plan, Honor 10 Lite, Android 9 / EMUI 9, never run):* there is no Home role dialog on Android 9, so SURAN
opens Settings (`ACTION_HOME_SETTINGS`); if that lists no launchers, use Settings → Apps → Default apps or press Home and pick SURAN →
Always. EMUI battery: Settings → Battery → App launch → SURAN → manage manually. Both are unverified and kept only for the API 26–28 branch.

Pull the research log: `adb pull /sdcard/Android/data/app.suran/files/events.jsonl`.
Demo the Outside sheet: `adb shell am start -a app.suran.OUTSIDE --es package com.instagram.android`.

## Manual test checklist (M1)
- [ ] APK installs by sideload
- [ ] SURAN can become the default Home; the Home button then shows SURAN
- [ ] First launch opens Choose; nothing pre-selected; Back does nothing; CTA inactive at 0
- [ ] Select several things; CTA reads "Start with this one" / "Start with these N"
- [ ] A catalog item with two matching apps opens "Which one for …?"
- [ ] "Everything else" is collapsed, expands, excludes SURAN and catalog apps
- [ ] Start → the "Make SURAN your Home" sheet appears; Not now → Home with the quiet line; tap the line → sheet again
- [ ] Choose SURAN as Home → Android's own screen → SURAN becomes Home → sheet and line disappear
- [ ] Launcher icon is the canonical S mark (Settings → Apps and the launcher)
- [ ] Start → Home shows only chosen things, bottom-anchored, footer count correct, ready line fades
- [ ] Tap each row → the real app opens; Back at its root → SURAN
- [ ] Press Home inside an app → SURAN
- [ ] Reboot → config persists, SURAN is Home
- [ ] Uninstall a chosen app → row dims "Not installed"; tap opens Choose (edit)
- [ ] Footer "Change" → edit mode; Save inactive until something changes; Cancel/Back/Home discard
- [ ] Save changes → Home updates; `config_saved` logged with added/removed
- [ ] Outside demo intent: sheet shows, 3 actions in order; Back / Once / Add behave as specified
- [ ] Wi-Fi and Bluetooth links open the system screens; Back and Home both return to an unchanged SURAN
- [ ] Emergency call from lock screen and dialer still works
- [ ] Airplane mode/no network: everything works; no INTERNET permission in the APK
- [ ] Font size at largest setting: no clipped or unreachable rows
- [ ] TalkBack: can set up, launch, change
- [ ] Looks and feels clearly unlike the stock launcher

## Deviations from the spec and things I flagged
1. **Android 9 has no `RoleManager`.** Default-Home goes through Settings; EMUI behavior is unverified.
2. **A LAUNCHER filter was added** next to HOME so SURAN can be opened once before it is the default.
3. **Ready line** clears after 6 s or on the first app launch (not on "next resume"), because the Settings/role step triggers a resume.
4. **Choose shows a "Not installed" group** in edit mode so unavailable entries can be removed. The spec was silent.
5. **Home list (v0.1.5):** centred horizontally, and vertically centred in the free space between the clock and the footer; app name (when it differs) sits under the label; when the list overflows it scrolls from the top.
6. **Palette (v0.1.4): the website's palette**, see below. Primary buttons use the accent #3F4A3C with a #F7F5F1 label (8.5:1).
7. `m2-spike/` is kept out of the build so M1 stays independent.
8. `Phone`/`Messages`/`Camera`/`Maps` resolve through intents, so on OxygenOS (and EMUI) they may bring up the maker's own apps, and `Maps` may offer more than one app. Since v0.1.8 Home shows the name Android reports for the chosen app.

## v0.1.1 fixes

### 1. Apps that need Google or their own sign-in
- SURAN only launches the installed app (`getLaunchIntentForPackage` + `startActivity`). Whatever the app shows next
  (Google sign-in, Play Store prompts, a first-run tour) belongs to that app and is not a SURAN error. SURAN holds no
  accounts and requests no account permission.
- Reviewed and unchanged: a missing app shows "Not installed"; a disabled app or failed launch shows
  "Couldn't open {App}." in the footer (all exceptions from `startActivity` are caught); apps uninstalled after the
  last resume are caught the same way. No crash path found by reading the code. Not run.
- **Emulator setup for Play-dependent apps**
  1. Device Manager → Create device → a phone profile marked with the Play Store icon.
  2. System image: **"Google Play"** (not "Google APIs", which has no Play Store). API 28 matches the Honor 10 Lite
     (Android 9); API 34 exercises the system Home-role dialog.
  3. Cold boot, sign in to a Google account (Settings → Accounts, or Play Store), install Spotify, Maps, etc.
  4. WhatsApp needs SMS verification and is unreliable on emulators; opening it to its own registration screen is
     enough to test SURAN's behaviour. You can also `adb install` an APK.
  5. The emulator has no Honor apps, so Phone/Messages/Camera resolve to the Google/AOSP apps.
- Test: choose 2–3 apps that show a sign-in screen → tap each from Home → the app's own screen opens → Back at its
  root returns to SURAN Home. Uninstall one → row dims "Not installed".

### 2. Setting SURAN as the Home app
- After first-run "Start", SURAN shows a bottom sheet over Home (not a new screen):
  *Make SURAN your Home* — what Home is, why SURAN needs to be it, what Android will ask, and what happens if you skip.
- Buttons: **Choose SURAN as Home** (Android's own flow: role dialog on API 29+, Home settings on API 26–28, with the
  system chooser as fallback) and **Not now**. Back, or tapping outside, equals Not now.
- If skipped, Home works and shows the quiet line "Make SURAN your Home"; tapping it reopens the same sheet. The line
  and sheet disappear once SURAN is the Home app (checked on every resume).
- The text hedges on purpose ("Look for Home or Launcher") because Android settings differ by version and maker.
  No new permissions; the system confirmation is never bypassed.
- The ready line ("Your phone is ready…") appears only when SURAN is already the Home app; otherwise the sheet comes first.

### 3. Logo
- Canonical: `design/logo.svg` (byte-identical copy of the landing-page `logo.svg`, sha256 `0556a85c…cbf0e`). Not modified.
- **Root cause:** the manifest pointed at `@drawable/ic_suran`, a placeholder I drew by hand (dark disc + green dot) in
  the first build. It was never derived from the SVG; it was also a plain vector, not an adaptive icon. There is no
  logo anywhere in the app UI (Home has none by design), so the launcher icon was the only usage.
- **Fix:** `ic_launcher_foreground.xml` is a 1:1 conversion (same path data, stroke width 11, round caps/joins, colour
  #3F4A3C). Only framing differs: scale 0.7 + translate 19 inside the 108 dp adaptive canvas to respect the 66 dp safe
  zone. Background is #F7F5F1 (website background, since v0.1.4). `mipmap-anydpi-v26/ic_launcher(.xml|_round.xml)` wire it up; the manifest uses
  `@mipmap/ic_launcher` and `ic_launcher_round`. The placeholder was deleted.
- Check: `python3 tools/check_logo.py /path/to/logo.svg` compares path, colour, width, caps and joins, and the SVG hash.
- Note: the SVG's #3F4A3C is the older moss green and is not in the v0.2 palette. I did not recolour it, as instructed.

## v0.1.2: Wi-Fi and Bluetooth entry

- **Where:** top-right of Home, opposite the clock: two quiet text links, "Wi-Fi" and "Bluetooth" (15 sp, #6B6B68,
  48 dp touch height, soft-green pressed state). No icons, panel, cards or badges. Home layout, list and footer are unchanged.
- **How:** `startActivity` of the system intent, no permission, nothing toggled by SURAN, no `<queries>` needed.
  Wi-Fi: `ACTION_WIFI_SETTINGS` → `ACTION_WIRELESS_SETTINGS` → `ACTION_SETTINGS`.
  Bluetooth: `ACTION_BLUETOOTH_SETTINGS` → `ACTION_WIRELESS_SETTINGS` → `ACTION_SETTINGS`.
  If none opens, the footer shows "Couldn't open settings." for 3 seconds.
- **Return:** Settings starts in SURAN's task (no NEW_TASK), so Back returns to SURAN. Pressing Home returns to SURAN too.
  SURAN's state is untouched: `config.json` is not read or written, and the ViewModel survives.
- **Log:** one extra event, `connectivity_open{target, action}`. It records which intent actually opened, which also
  documents device differences, and shows how often people needed it.
- **Not tested:** I could not run an emulator here. Test on the emulator and the Honor:
  1. Tap Wi-Fi → the Wi-Fi list opens → Back → Home is unchanged. Repeat with Home pressed instead of Back.
  2. Same for Bluetooth.
  3. Change the configuration first, then do 1–2: the configuration is unchanged afterwards.
  4. Check `events.jsonl`: the `action` value shows which screen opened (a fallback value means the specific screen was missing).
- **Expected platform limits (unverified):**
  - Emulators: Wi-Fi settings exist; some AVD images have no Bluetooth page, so the fallback should engage and the log will show it.
  - Settings screens differ by Android version and maker; on the Honor 10 Lite (EMUI 9) the pages are Huawei's own.
  - Android 10+ has a Wi-Fi/Internet panel (`Settings.Panel`); not used, because the full screen works on every API level in scope and covers Bluetooth the same way.
  - With the M2 service on, `com.android.settings` is allowlisted; check that the EMUI settings package is as well.

## v0.1.3: visual refinement (calm and warmth)

Visual only. No behaviour, wording, navigation, persistence, logging or connectivity code changed.
Reference: the live suran.tech and its `style.css` (inspected): system font stack, headings with -0.02em tracking,
1.6 body line height, 10 px radius, 1 px hairlines instead of shadows, footer with a top border and underlined
secondary links, generous vertical rhythm, accent used for primary actions only.

| # | Change | Why |
|---|---|---|
| 1 | Home focal point: clock 44 sp Normal (was 17 sp Medium), date 15 sp, more top space; rows 26 sp Normal at 60 dp (was 28 sp Medium at 56 dp) | The screen opens on a quiet "hero" like the website, and the list stops competing with it. Lighter weight, calmer; row labels are still the largest interactive text |
| 2 | Type system: -0.02em tracking on any text >= 24 sp, 1.5 line height on sheet paragraphs, small tracked group labels in Choose | Same rules as the website; improves reading rhythm without larger text |
| 3 | Hairlines instead of containers: 1 dp #E8E8E5 above the Home footer; sheets get a 1 dp top border instead of relying only on the scrim; no cards, no shadows | The website's border treatment; edges are defined softly |
| 4 | Radius 10 dp everywhere (buttons, sheets, notice), matching `--radius: 10px` | Coherence with the website; slightly less bubbly than before |
| 5 | Home notice ("Make SURAN your Home" / ready line) on the website's alt surface (since v0.1.4: #EFECE6); Wi-Fi and Bluetooth links underlined (as the site's footer links) | Warmth through the palette's soft green, used once; the underline keeps the links discoverable while staying secondary |
| 6 | Choose: more space above the headline and between groups; quieter group labels; more distance before "Everything else" | The curated list reads as primary, the long tail as an optional aside |
| 7 | Navigation bar icons dark on API 27+ (`values-v27/themes.xml`) | Coherent with the light page |

Screens/components touched: Home, Choose, Home-setup sheet, Outside sheet, "Which one" sheet, theme.
Files: `Theme.kt`, `HomeScreen.kt`, `ChooseScreen.kt`, `OutsideActivity.kt`, `res/values-v27/themes.xml`.

**Palette inconsistency:** resolved in v0.1.4 (below).

**Not verified:** not built, not run, no screenshots (no Android SDK in the authoring environment). Check on device:
the 44 sp clock and 60 dp rows against how many list rows fit on the Honor 10 Lite; the underline on the links; and
that the soft-green notice reads as a quiet strip, not a banner.

## v0.1.4: palette aligned with suran.tech

Colour values only; no layout, behaviour or code logic changed. Tokens live in `Theme.kt` (`Ink`) and `res/values/colors.xml`.

| Role | Was (v0.2) | Now (website token) | Contrast on bg |
|---|---|---|---|
| Background, window, launcher-icon background | #FAFAF8 | #F7F5F1 `--color-bg` | n/a |
| Primary text, scrim (40%) | #1A1A1A | #1A1918 `--color-text` | 16.1:1 |
| Secondary text, unselected ring | #6B6B68 | #5F5B54 `--color-text-secondary` | 6.2:1 |
| Brand accent: primary button, selected dot | #1F8A4D | #3F4A3C `--color-accent` (also the logo colour) | 8.5:1 |
| Accent pressed | ~12% darker green | #333C31 `--color-accent-hover` | n/a |
| Button label | #FAFAF8 | #F7F5F1 `--color-accent-contrast` | 8.5:1 |
| Dividers, borders, disabled button fill | #E8E8E5 | #DEDAD2 `--color-border` | not for text (disabled label 4.8:1) |
| Pressed rows, quiet notice surface | soft green #E8F4EC | #EFECE6 `--color-bg-alt` | n/a |
| Error text | #B3261E | #9C4530 `--color-error` | 5.8:1 |

- The website has no soft-green token; its own tinted surface (`--color-bg-alt`) replaces soft green. The notice strip on Home is now a warm neutral, not green.
- The accent now equals the logo colour (#3F4A3C), so the launcher icon and the UI are one colour family. The earlier mismatch is gone.
- The large-text contrast caveat for the button label no longer applies (8.5:1); the 19 sp size is unchanged because this task is palette only.
- Not verified: not built or run.

## v0.1.5: selected-apps list composition

`HomeScreen.kt` only (the list container and `HomeRow`). Behaviour, data and every other element are unchanged.
- Labels are centred on one axis; the app name (shown only when it differs, or "Not installed") moves from inline to a quiet line below, so centred rows stay aligned and the secondary text never makes the row ragged.
- The list is vertically centred in the free space between the clock and the footer, instead of anchored to the bottom.
- Rows are 64 dp (all rows, with or without a second line); the pressed state is a 10 dp rounded band.
- Reasoning on realistic cases (760 dp tall phone, about 530 dp free): 1 item = one calm word in the middle; 3-5 items = a balanced block with equal space above and below; 8 items = about 512 dp, fills the space; more, or a 640 dp phone, or large fonts = scrolls from the top with 8 dp padding.
- Trade-off to judge on the device: the list is no longer in the lowest thumb zone.
- Not built or run.

## v0.1.6: one label per app on Home

`HomeScreen.kt` only. The Home row no longer shows the app's own (system-localized) name under the label; it shows the existing
primary label only. The "Not installed" line for unavailable apps is kept. Layout, spacing and typography are unchanged
(rows were already 64 dp). Not built or run. Not changed: Choose still shows app names as its secondary line.

## v0.1.7: optional dark theme (SURAN only)

- **What:** a "Dark" link at the right of the Home footer (next to "Change") switches SURAN to a dark theme; it then reads "Light". Default is light, and the light theme is unchanged (same values, same layout). The choice is saved in `filesDir/theme` and restored at start. It never follows the system theme and does not touch Android settings or other apps.
- **Palette:** the same warm neutrals, inverted, with the accent #3F4A3C lightened. bg #1A1918, text #F7F5F1, secondary #B5B0A6, hairline/disabled #3A3834, quiet surface #262422, accent #A3B59E (pressed #8FA08A), error #E08A72, sheet scrim black at 60%. No blue, no other greens. Contrast ratios (calculated, not measured on screen): `docs/TESTING.md` §5.
- **How:** `Ink` colours became getters on `Ink.dark` (Compose state), so screens did not change. New `Ink.scrim` replaces `Ink.ink.copy(alpha = 0.4f)` in three sheets; `barStyle()` sets the system bar icons; `Ink.bg` sits behind the `Crossfade`. No new dependency, no new log event, `config.json` untouched.
- **Files:** `Theme.kt`, `ThemeStore.kt` (new), `SuranApp.kt`, `SuranViewModel.kt`, `HomeActivity.kt`, `HomeScreen.kt`, `ChooseScreen.kt`, `OutsideActivity.kt`.
- **Known limits:** a brief light flash at cold start in dark (the system starting window stays light); the footer may wrap with very large fonts. Not built or run in the authoring environment. The author reports it works on the OnePlus 6 (ST2): the theme change affects SURAN only, it persists, the footer is fine at the largest font; the brief light flash at cold start in dark is confirmed (`docs/TESTING.md` §5-6).

## v0.1.8: app labels follow the system language

`HomeScreen.kt` and `ChooseScreen.kt` only. For every installed selected app, catalog items included, Home shows the name Android
reports now (`InstalledApp.label`, recomputed on each resume), e.g. Music → "Spotify". Not installed: catalog label, then the label
saved in `config.json`. Choose shows the same live name as the secondary line of chosen catalog items. `config.json`, the ViewModel,
the manifest and the dependencies are unchanged; `Entry.appLabel` is now only a fallback. SURAN's own UI language stays English.
Supersedes v0.1.6 (the single Home label now comes from Android, not from the catalog).
**Implemented. Built with Gradle (Android Studio, debug). Verified on the OnePlus 6 by the user** (system-language change; individual
checklist items were not recorded). Not checked: truncation of long names at 26 sp / one line (TS-22).

## v0.1.9: `home_ui_started`

`SuranViewModel.kt` only. New event `home_ui_started{defaultHome}` in the ViewModel `init`: the Home UI started (new Activity, or restore
after process death). Not logged on configuration changes, and not when the process is started for another reason (for example the
notification listener). It does not say whether the user meant to open SURAN: Android may start it as Home. `app_open` is unchanged
and now means only "the process started". Old `events.jsonl` data has no `home_ui_started`.
**Implemented. Built with Gradle. Partly verified on the OnePlus 6 (ST3):** the event appears in `events.jsonl`. It was not logged when
the process restarted with a `service` start type (after `am force-stop`); after a kill and after a reboot it appears
once Android reopens Home. The other checklist items were not run.

## v0.1.10: NotificationGate, phase 1 (log-only, experimental)

- **Hypothesis:** showing notifications only from the chosen apps makes the phone less distracting. **Phase 1 only records**; it never
  cancels, snoozes or changes a notification.
- **How:** `NotificationGate.kt` (a `NotificationListenerService`) and a `<service>` in the manifest (no `uses-permission`, no INTERNET).
  It works only after you grant Notification access in Android settings. For each notification it writes one deduplicated line
  `notification_decision{pkg,category,decision,reason}`, plus `notification_listener_connected` / `_disconnected`. It reads only the
  package, the category and the flags: never title, text, extras, and it does not look for one-time codes.
- **Always `allow`:** SURAN, chosen apps, the 2FA whitelist, the default SMS app, the default dialer, categories call/alarm/system/error,
  ongoing and foreground-service notifications, and packages with no launcher icon (system and service packages). Anything else is
  logged as `would_suppress`.
- **The 2FA whitelist is not complete** and **not verified**: `ALLOW_VERIFIED` is empty; `ALLOW_TO_VERIFY` holds names from the catalog and
  from general knowledge. Banks, e-mail, chat and push approvals are not covered. Android cannot tell a code from an ordinary
  notification without reading its content, which SURAN does not do (TS-25). **No suppression may be switched on before these
  packages are checked on the OnePlus 6 and you decide explicitly.**
- **Undo:** revoke the access in Android settings, or remove the `<service>` element. `config.json` is only read.
- `requestRebind` is called when the listener disconnects; whether OxygenOS 11 honours it is unverified (TS-23).
**Implemented. Built with Gradle (Android Studio, debug). Partly verified on the OnePlus 6 (ST3, 2026-11-10/11):** the service is
registered, Notification access was granted by hand, `notification_listener_connected` and `notification_decision` are written
(`no_launcher` for a system notification; `would_suppress`/`unselected` for Google Calendar, category `event`), the notifications stayed
untouched, and after a kill, a reboot and `am force-stop` the process started again with a `service` start type and the listener reconnected. **Not verified:** the other decision branches
(`selected`, `category`, `ongoing`, default SMS/dialer), the 2FA whitelist, `requestRebind` (TS-23), the lock screen before the first
unlock, OxygenOS battery management, revoking the access. Phase 2 (suppression) is not implemented.

## Known limitations
- Built and tested on the OnePlus 6 (ST1–ST3; many items not recorded). v0.1.8–v0.1.10 are only partly verified (`docs/TESTING.md` §6,
  ST3). Not tested on any other device (the original plan, Honor 10 Lite, was never run).
- No outside detection in M1 (Outside sheet is demo-only until M2 works).
- Notifications from non-chosen apps are NOT suppressed. v0.1.10 only logs what SURAN would do; the 2FA whitelist is incomplete.
- Real apps keep their own feeds once opened; SURAN controls only the doorway.
- English only; light theme by default (dark is optional, SURAN only); portrait only.
- Debug-signed release APK (prototype only).

## M2 spike result
**Not run.** It needs the physical Honor 10 Lite. Plan and the five questions to record are in `m2-spike/README-M2.md`.

## UX decisions to revisit on the device
- Row size and type scale (56 dp / 28 sp) vs how many rows really fit on this screen.
- Whether the footer ("This phone is for N things · Change") is discoverable enough.
- Whether secondary app names on Home help or add noise.
- Whether "Everything else" is too hidden or still too prominent.
- Whether "Add to this phone" saving instantly (no Save step) causes drift.
- Whether Home's bottom anchoring feels right with the gesture/nav bar on this device.
