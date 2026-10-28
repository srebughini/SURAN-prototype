# SURAN Prototype v0 — M1

Native Android (Kotlin + Jetpack Compose), one module, package `app.suran`, minSdk 26, targetSdk 34.
Reference device: Honor 10 Lite, Android 9 / EMUI 9.

**Status: source only. It has NOT been compiled or run.** The environment it was written in cannot reach the
Android SDK or Maven repositories. Expect a few small compile fixes on first sync.

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
  SuranApp.kt        logs app_open{defaultHome}
  SuranViewModel.kt  single UiState StateFlow
  Theme.kt           tokens + text + press helper
  HomeScreen.kt, ChooseScreen.kt, HomeActivity.kt, OutsideActivity.kt
m2-spike/            NOT compiled: accessibility service, allowlist, instructions
```

## Build the APK
1. Android Studio Koala (2024.1) or newer, JDK 17. Open the `suran-prototype` folder and let Gradle sync
   (Studio creates the wrapper; or run `gradle wrapper --gradle-version 8.7`).
2. Build → Build APK(s) with the **debug** variant (needed for the Outside demo intent).
   CLI: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

## Install and set up the Honor 10 Lite
1. Settings → About phone → tap Build number 7 times; Developer options → USB debugging on.
2. `adb install -r app-debug.apk` (or copy the APK to the phone and allow "Install unknown apps" for your file manager).
3. Open SURAN from the normal launcher. Choose what the phone is for → "Start with these N".
4. Android 9 has no Home role dialog. SURAN opens Settings (`ACTION_HOME_SETTINGS`) and shows a short toast.
   If that page lists no launchers, use Settings → Apps → Default apps (path varies on EMUI), or press Home and pick
   SURAN → Always. **Write down what actually works on this device.**
5. Settings → Battery → App launch → SURAN → manage manually, all toggles on (EMUI may otherwise kill it).
6. Concierge step: turn off notifications for apps not chosen (SURAN does not intercept notifications).

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
8. `Phone`/`Messages`/`Camera`/`Maps` resolve through intents, so on EMUI they may bring up Huawei/Honor apps whose labels differ (they show as quiet secondary text), and `Maps` may offer more than one app.

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

## Known limitations
- Not compiled or device-tested. Not tested on any device other than the planned Honor 10 Lite.
- No outside detection in M1 (Outside sheet is demo-only until M2 works).
- Notifications from non-chosen apps are not handled.
- Real apps keep their own feeds once opened; SURAN controls only the doorway.
- English only; light theme; portrait only.
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
