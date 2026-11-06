# SURAN — Architettura (M1)

## 1. Struttura del repository

```
build.gradle.kts, settings.gradle.kts, gradle.properties
gradle/wrapper/gradle-wrapper.properties      (gradlew e gradle-wrapper.jar: non tra i file analizzati)
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/debug/AndroidManifest.xml             intent di debug app.suran.OUTSIDE
app/src/main/java/app/suran/                  vedi sezione 2
app/src/main/res/                             values, values-v27, drawable, mipmap-anydpi-v26
design/logo.svg                               logo canonico
tools/check_logo.py                           controllo coerenza logo ↔ icona
README.md
m2-spike/                                     citato dal README; contenuto non analizzato
```

Un solo modulo (`:app`), package `app.suran`, nessuna directory di test.

## 2. Responsabilità dei file

| File | Elementi principali | Responsabilità |
|---|---|---|
| `SuranApp.kt` | `SuranApp` | `Application`; logga `app_open{defaultHome}` a ogni avvio processo |
| `HomeActivity.kt` | `HomeActivity` | Unica Activity UI di M1 (filtri HOME e LAUNCHER, `singleTask`, portrait). Ospita Compose; `Crossfade` tra `Screen.HOME` e `Screen.CHOOSE`; `onResume` → `Session.reset()` + `vm.onResume()`; `onNewIntent` (tasto Home); `openConnectivity`; `requestHome` |
| `OutsideActivity.kt` | `OutsideActivity` | Bottom sheet "Not part of this phone" con tre azioni. Non esportata nel build principale; esportata con action `app.suran.OUTSIDE` nel manifest debug |
| `SuranViewModel.kt` | `UiState`, `Screen`, `ChooseMode`, `SuranViewModel` | Stato UI e logica: caricamento config, lancio app, bozza di selezione, salvataggio, log |
| `Config.kt` | `Entry`, `Config`, `ConfigStore`, `normalize()` | Modello e persistenza di `config.json`; ordinamento (voci di catalogo nell'ordine del catalogo, poi le altre) e deduplica per `pkg` |
| `Catalog.kt` | `Group`, `CatalogItem`, `CATALOG`, `catalogItem()`, `catalogCandidates()` | Catalogo curato; app installate candidate per voce (per elenco di pacchetti o per intent) |
| `Apps.kt` | `InstalledApp`, `queryInstalled`, `launchApp`, `isDefaultHome`, `appLabelOf` | Accesso a `PackageManager`/`RoleManager` |
| `EventLog.kt` | `EventLog` | Log JSON-lines locale |
| `Session.kt` | `Session` | Stato in memoria condiviso tra Activity (`onceAllowed`, `lastOutside`) |
| `Theme.kt` | `Ink`, `T`, `TA`, `pressable`, `sheetSurface` | Token di design, testo, stato "pressed", superficie dei bottom sheet |
| `HomeScreen.kt` | `HomeScreen`, `HomeRow`, `HomeSetupSheet`, `QuietLink`, `NoticeStrip`, `rememberNow` | UI Home |
| `ChooseScreen.kt` | `ChooseScreen`, `PrimaryButton`, `SelectRow`, `Dot`, `AmbiguitySheet` | UI di scelta/modifica; `PrimaryButton` è usato anche da Home e Outside |

## 3. Flussi applicativi

**Avvio.** `SuranApp.onCreate` registra `app_open`. `SuranViewModel.initial()` carica `config.json`, interroga le app installate e i candidati di catalogo. Se `setupDone` e `entries` non vuote → `Screen.HOME`; altrimenti `Screen.CHOOSE` in modalità `FIRST_RUN`. A ogni `onResume` il ViewModel aggiorna app installate, candidati e `isDefaultHome` (non ricarica `config`) e chiude `homeSheet` se SURAN è già Home.

**Primo setup.** Choose → selezione in `draft` → `save()`: `normalize` → `ConfigStore.save` → log `setup_done` → Home. Se SURAN è già Home: `readyLine`; altrimenti: `homeSheet` (sheet esplicativo).

**Selezione.** Voce di catalogo con 1 candidato: aggiunta diretta. Con più candidati: `ambiguity` apre "Which one for …?". Le app di "Everything else" entrano come `Entry` con `intentId = null`. Un'app già in bozza viene sostituita se scelta come voce di catalogo.

**Avvio di un'app dalla Home.** `launch(entry)`: se non installata → `openEdit()`; altrimenti `getLaunchIntentForPackage` + `FLAG_ACTIVITY_NEW_TASK`, log `launch`; in caso di errore `launchError` ("Couldn't open X.", 3 s).

**Modifica.** Footer "Change" → `openEdit()` (bozza = config corrente, modalità `EDIT`). Salva solo se la config cambia (`hasChanges`). Cancel, Back e tasto Home scartano la bozza. Log `config_saved{added, removed}`.

**Home predefinita (`requestHome`).** API ≥ 29 con ruolo disponibile → `RoleManager.createRequestRoleIntent(ROLE_HOME)`, al ritorno `vm.onResume()`. Altrimenti `Settings.ACTION_HOME_SETTINGS`; se fallisce, `ACTION_MAIN`+`CATEGORY_HOME` con `NEW_TASK` (selettore di sistema). `isDefaultHome`: `isRoleHeld` da API 29, altrimenti `resolveActivity` sull'intent HOME.

**Connettività.** `openConnectivity(target)` prova in sequenza l'azione specifica (Wi-Fi o Bluetooth), `ACTION_WIRELESS_SETTINGS`, `ACTION_SETTINGS`; senza `NEW_TASK`, così Back torna a SURAN. Log `connectivity_open{target, action}`; se nulla si apre: "Couldn't open settings.".

**Outside (solo demo in M1).** `OutsideActivity` legge l'extra `package`. Back → log e ritorno a `HomeActivity`. "Open once" → `Session.onceAllowed.add(pkg)` e lancio. "Add to this phone" → aggiunge `Entry(pkg, null, label)` a `config.json` con proprio `ConfigStore`, log, lancio. In M1 nessun codice legge `Session.onceAllowed` né imposta `Session.lastOutside`.

## 4. Gestione dello stato

- **Fonte unica UI**: `UiState` in `MutableStateFlow` dentro `SuranViewModel`, letto con `collectAsStateWithLifecycle`. Campi: `config`, `installed`, `catalog`, `screen`, `chooseMode`, `draft`, `ambiguity`, `longTailExpanded`, `readyLine`, `homeSheet`, `launchError`, `saveError`, `isDefaultHome`.
- **Stato locale Compose**: `OutsideActivity` (`shown`, `error`, `pkg`) e `HomeScreen` (`now` via broadcast `TIME_TICK`/`TIME_CHANGED`/`TIMEZONE_CHANGED`).
- **Stato di processo**: `Session` (oggetto singleton, azzerato in `HomeActivity.onResume`).
- `HomeActivity` e `OutsideActivity` non condividono il ViewModel: ciascuna usa un proprio `ConfigStore` e un proprio `EventLog` sullo stesso file.

## 5. Persistenza

| Dato | Posizione | Formato |
|---|---|---|
| Configurazione | `filesDir/config.json` (+ `config.json.tmp`) | `{"schema":1,"setupDone":bool,"entries":[{"pkg","intentId"|null,"appLabel"}]}`; scrittura su file temporaneo + `renameTo`; `load()` restituisce `null` se il file manca o su qualsiasi eccezione |
| Log eventi | `getExternalFilesDir(null)/events.jsonl` (fallback `filesDir`) | una riga JSON per evento: `t` (ISO 8601 con offset), `e` (tipo) + campi; taglio a 5.000 righe ogni 100 scritture **per istanza** di `EventLog` |

Eventi: `app_open{defaultHome}`, `setup_done{entries}`, `config_saved{added,removed}`, `launch{pkg,intentId}`, `connectivity_open{target,action}`, `outside_shown{pkg}`, `outside_action{action[,pkg]}`.

Nessun database, SharedPreferences o DataStore. `allowBackup=false`.

## 6. Dipendenze e build

| Voce | Valore | Origine |
|---|---|---|
| Gradle | 8.7 | `gradle-wrapper.properties` |
| Android Gradle Plugin | 8.5.2 | `build.gradle.kts` |
| Kotlin | 1.9.24 | `build.gradle.kts` |
| Compose compiler | 1.5.14 | `app/build.gradle.kts` |
| Java / jvmTarget | 17 | `app/build.gradle.kts` |
| compileSdk / targetSdk / minSdk | 34 / 34 / 26 | `app/build.gradle.kts` |
| Compose BOM | 2024.06.00 → `ui`, `foundation`, `animation` | versioni risolte dal BOM, non indicate nel repo |
| activity-compose | 1.9.0 | |
| lifecycle-viewmodel-compose / lifecycle-runtime-compose | 2.8.2 | |
| core-ktx | 1.13.1 | |
| Repository | `google()`, `mavenCentral()` (`FAIL_ON_PROJECT_REPOS`) | `settings.gradle.kts` |

Assenti: Material, Navigation, DI, Room/DataStore, librerie di test. `kotlinx.coroutines` (usato in `HomeScreen`) non è dichiarato: arriva in modo transitivo. `versionName` = `0.1.0-m1`, `versionCode` = 1. Release: firmata con la chiave debug, senza minify.

## 7. Manifest

- `HomeActivity`: `exported`, `singleTask`, portrait; filtri MAIN+HOME+DEFAULT e MAIN+LAUNCHER.
- `OutsideActivity`: non esportata, `excludeFromRecents`, `taskAffinity=""`, tema traslucido `Theme.Suran.Outside`; il manifest debug la esporta con action `app.suran.OUTSIDE`.
- `<queries>`: MAIN/LAUNCHER, DIAL, SENDTO `smsto`, VIEW `geo`, `STILL_IMAGE_CAMERA`.
- Permessi: nessuno dichiarato.
- Tema: `Theme.Suran` (genitore `android:Theme.Material.Light.NoActionBar`); `values-v27` aggiunge la barra di navigazione chiara. Icona: adaptive (`mipmap-anydpi-v26`) con foreground vettoriale derivato da `design/logo.svg`.
