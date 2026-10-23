# M2 spike — foreground detection (NOT part of the M1 build)

Do this only after M1 passes on the Honor 10 Lite.

## Install the spike
1. Copy `Allowlist.kt` and `SuranAccessibilityService.kt` to `app/src/main/java/app/suran/`.
2. Copy `res-xml/suran_accessibility.xml` to `app/src/main/res/xml/` (create the folder).
3. Add to `app/src/main/res/values/strings.xml`:
   `<string name="a11y_description">Lets SURAN notice which app is open, so it can show what this phone is for. It does not read your screen.</string>`
4. Add inside `<application>` in `AndroidManifest.xml`:
```xml
<service
    android:name=".SuranAccessibilityService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/suran_accessibility" />
</service>
```
5. Add inside `<queries>`:
```xml
<intent>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
</intent>
```

## Enable it
- Settings → Accessibility → SURAN → On, or with USB debugging:
```
adb shell settings put secure enabled_accessibility_services app.suran/app.suran.SuranAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

## What to record (the 5 questions)
| # | Question | How to check |
|---|---|---|
| 1 | Does the service stay connected after enabling, and after app switching? | `events.jsonl` has `m2_service connected`; no unexpected `destroyed` |
| 2 | Does a non-chosen app open (icon in a notification, a link, Recents) produce `outside_shown`? | open 5 non-chosen apps, 3 ways each |
| 3 | Does OutsideActivity appear over the app within ~1 s, every time? | 10 trials, count misses |
| 4 | Does EMUI interfere (kills service, blocks background start, double sheets)? | note after screen off, after 30 min idle, after reboot |
| 5 | Does battery optimization change the result? | repeat after Settings → Battery → App launch → SURAN → manual, all toggles on; and with it off |

Stop condition: if 2 or 3 fail more than ~1 in 10, or the service is killed within an hour, stop. Keep the debug intent for demos. Do not redesign around it.
