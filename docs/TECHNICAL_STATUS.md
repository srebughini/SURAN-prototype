# SURAN — Stato tecnico

Dispositivo di riferimento (rilevato dal dispositivo): OnePlus A6003 (OnePlus 6), Android 11 (API 30), OxygenOS 11.1.2.2, build ONEPLUS A6003_22_211125.

**Stato dopo lo smoke test #1 (ST1, `TESTING.md` §6):** TS-01 e TS-02 (audit AU-01, AU-02) chiusi per la configurazione verificata. Tutte le altre voci restano aperte finché non verificate direttamente. Le voci chiuse sono conservate per cronologia.

## Rischi e debito tecnico

| ID | Pri | Problema | Evidenza | Azione suggerita (non eseguita) |
|---|---|---|---|---|
| TS-01 (audit AU-01) | P0 — **Chiuso** (ST1) | Il codice non è mai stato compilato né eseguito | README: "source only"; la lettura non ha mostrato errori di compilazione evidenti, ma non equivale a una build | **Chiuso** per la configurazione verificata: compilazione riuscita, app installata e avviata su OnePlus 6. Non copre altri dispositivi, la variante di build (non registrata) né il manifest unito |
| TS-02 (audit AU-02) | P0 — **Chiuso** (ST1) | Impostazione come Home su OnePlus 6 / Android 11 (API 30). Percorso atteso: `RoleManager` (API 29+: `createRequestRoleIntent(ROLE_HOME)`). Il ramo API 26–28 (`ACTION_HOME_SETTINGS`, poi selettore di sistema) resta per minSdk e non è il percorso del dispositivo di riferimento | `HomeActivity.requestHome`; README passo 4 (scritto per un altro dispositivo) | **Chiuso** per questa configurazione: SURAN impostato come Home predefinita. Schermata di sistema effettiva non registrata; riavvio e diniego non verificati (vedi AU-03); ramo API 26–28 non verificato |
| TS-03 | P1 | `gradlew` e `gradle-wrapper.jar` non tra i file analizzati; presente solo `gradle-wrapper.properties` | Elenco file | Build da Android Studio o generare il wrapper (`TESTING.md`) |
| TS-04 | P1 | Dopo "Add to this phone" la Home può mostrare una config vecchia: `onResume` non ricarica `config` e `OutsideActivity` scrive con un proprio `ConfigStore` | `SuranViewModel.onResume`; `OutsideActivity.onAdd` | Verificare su dispositivo; poi decidere con te se ricaricare la config |
| TS-05 | P1 | In `onAdd`, se `config.json` manca o è illeggibile si salva `Config(setupDone=false, …)`: al riavvio riparte la scelta iniziale | `OutsideActivity.onAdd` | Verificare; in M1 è raggiungibile solo con l'intent di debug |
| TS-06 | P1 | `ConfigStore.load()` restituisce `null` su qualsiasi eccezione: un file corrotto equivale a primo avvio, senza backup. `schema` è scritto ma non letto | `Config.kt` | Valutare dopo i primi test |
| TS-07 (audit AU-05) | P1 | `queryInstalled`, `catalogCandidates` (una `queryIntentActivities` per voce) e `isDefaultHome` girano sul main thread alla creazione del ViewModel e a ogni `onResume`; anche i log scrivono su file dal main thread | `SuranViewModel`, `Apps.kt`, `Catalog.kt`, `EventLog.kt` | Misurare tempo di avvio e jank sul OnePlus 6 |
| TS-08 (audit AU-06) | P2 | OxygenOS può terminare il processo del launcher in base alle impostazioni di gestione batteria. Il passo 5 del README è scritto per un altro dispositivo | README passo 5 | Individuare sul dispositivo le impostazioni batteria applicabili a SURAN e confermare il passo operativo |
| TS-09 (audit AU-07) | P2 | Phone/Messages/Camera/Maps si risolvono per intent: su OxygenOS possono comparire app OnePlus con etichette diverse, e Maps può avere più candidati | README deviazione 8; `catalogCandidates` | Verificare sul dispositivo |
| TS-10 (audit AU-08) | P2 | `EventLog`: istanze multiple (Application, ViewModel, Outside) scrivono sullo stesso file; il conteggio per il taglio è per istanza, quindi il tetto di 5.000 righe non è garantito. Il file sta nella cartella external files e contiene i nomi dei pacchetti lanciati; su Android 9 le app con permesso di storage possono leggere quelle cartelle (da valutare) | `EventLog.kt` | Controllare dimensione e formato dopo uso reale |
| TS-11 | P2 | Font grande, TalkBack: non provati. Le righe Home hanno `maxLines = 1` con ellissi | `HomeScreen.HomeRow`, `T()` | Checklist in `TESTING.md` |
| TS-12 | P2 | Lista Home centrata, non più nella zona bassa del pollice: decisione UX aperta | README v0.1.5 | Giudicare sul dispositivo |
| TS-13 | P3 | Nessun test automatico, CI o configurazione lint | Nessuna cartella di test; nessuna dipendenza di test | `TESTING.md` |
| TS-14 | P3 | Release firmata con chiave debug e senza minify | `app/build.gradle.kts` | Accettabile per prototipo |
| TS-15 | P3 | Stringhe UI hard-coded nel codice (solo `app_name` in `strings.xml`); solo inglese, portrait; tema chiaro di default (scuro opzionale: F-18) | Sorgente | Nessuna azione per M1 |
| TS-16 | P3 | `res/drawable/ic_suran.xml` è un segnaposto non referenziato (il manifest usa `@mipmap/ic_launcher`) | Manifest, D-05 | Chiedere autorizzazione prima di eliminarlo |
| TS-18 | P2 | Tema scuro, avvio a freddo: la finestra iniziale di Android e `windowBackground` (`@color/suran_bg`) restano chiari, quindi con il tema scuro attivo può comparire un breve lampo chiaro prima del primo frame di Compose. Non evitabile senza legare il tema a quello di sistema (escluso) | `res/values/themes.xml`, `HomeActivity` | Misurare sul OnePlus 6; accettato salvo diversa indicazione |
| TS-19 | P2 | Footer Home con tre elementi ("This phone is for N things", "Change", "Dark"/"Light"): con font grande il testo può andare a capo. La riga ha altezza minima 56 dp e il link almeno 48 dp; non si taglia in teoria, non provato. Durante `launchError` (3 s) il footer è sostituito dall'errore e il link sparisce | `HomeScreen` | Checklist in `TESTING.md` (font al massimo, 1 e 10 voci) |
| TS-20 | P2 | Tema scuro e tema di sistema: SURAN non lo legge e usa `Theme.Material.Light`. Con il tema di sistema scuro (o l'opzione OxygenOS per le app di terze parti) Android potrebbe applicare un "force dark" al tema chiaro di SURAN. Non previsto, non verificato | Manifest, `themes.xml` | Provare con tema di sistema attivo e disattivo; solo se serve, `forceDarkAllowed=false` in `values-v29` |
| TS-21 | P3 | Il tema è `Ink.dark`, stato Compose globale di processo fuori da `UiState` (eccezione alla regola "un solo `UiState`", motivata dal fatto che `Ink` è un `object` letto ovunque). Caricato in `SuranApp.onCreate` e salvato in `ThemeStore` sul main thread (file di pochi byte, come TS-07). Se il salvataggio fallisce il tema cambia comunque per la sessione, senza errore in UI | `Theme.kt`, `ThemeStore.kt`, `SuranViewModel.toggleTheme` | Valutare dopo i primi test |
| TS-17 | P3 | `registerReceiver` senza flag di esportazione con targetSdk 34: i broadcast usati sono di sistema, quindi probabilmente esenti. Non verificato | `HomeScreen.rememberNow` | Verificare in build |

## Criticità dall'audit non presenti sopra (aperte)

| ID | Pri | Problema | Stato |
|---|---|---|---|
| AU-03 | P1 | `HomeActivity.roleLauncher` non controlla l'esito della richiesta di ruolo Home e non ha fallback: se il dialog è negato con "non chiedere più", il pulsante può sembrare inattivo | Aperta, non verificata |
| AU-04 | P1 | Il log non copre le ipotesi del test (richiesta/esito Home, sheet, apertura/annullamento modifica, `launchError`); si recupera solo con `adb pull` | Aperta |
| AU-09 | P2 | `HomeScreen.rememberNow` non si aggiorna al resume: possibile orologio fermo fino al tick successivo | Aperta, non verificata |
| AU-10 | P2 | Lo sheet Home dice "press the Home button"; con navigazione a gesti non c'è alcun pulsante | Aperta; dipende dal tipo di navigazione, non registrato |

Corrispondenze: AU-05 = TS-07, AU-06 = TS-08, AU-07 = TS-09, AU-08 = TS-10 (tutte aperte). Rilievi documentali dell'audit non ancora corretti: F-06 e `PROJECT_OVERVIEW` (nome app secondario, v0.1.6); D-07 (il README arriva a v0.1.6); `ARCHITECTURE` §2 ("unica Activity UI"); `check_logo.py` su Windows; `TESTING` §3 ("Studio crea il wrapper", non verificato).

## Incongruenze documentazione ↔ codice

Il codice prevale. Nessuna è stata corretta (analisi senza modifiche).

| ID | Dove | Dice | Il codice mostra |
|---|---|---|---|
| D-01 | README, setup passo 4 | Su Android 9 compare un toast breve | `requestHome` non mostra alcun toast (la spiegazione è nello sheet) |
| D-02 | README checklist; commento in `HomeScreen` | Lista ancorata in basso | Lista centrata (v0.1.5) |
| D-03 | README, "UX decisions to revisit" | Righe 56 dp / 28 sp | Righe 64 dp / 26 sp |
| D-04 | README v0.1.2 | Link #6B6B68, pressed verde tenue | `Ink.muted` #5F5B54, pressed `surfaceAlt` #EFECE6 |
| D-05 | README (Files; "placeholder deleted") | Segnaposto icona assente o eliminato | `drawable/ic_suran.xml` presente e non usato |
| D-06 | README, sezione Logo | #3F4A3C è il verde "vecchio", fuori palette | Dalla v0.1.4 l'accento è #3F4A3C, uguale al logo |
| D-07 | `app/build.gradle.kts` | `versionName` 0.1.0-m1 | README documenta versioni fino a v0.1.5 |
| D-08 | KDoc di `SuranViewModel.save()` | Il chiamante richiede il ruolo Home | Il salvataggio mostra lo sheet; la richiesta parte da un pulsante |
| D-09 | KDoc di `Session` | `onceAllowed` si azzera anche allo spegnimento schermo | Nel codice M1 solo `Session.reset()` in `HomeActivity.onResume` |
| D-10 | KDoc di `pressable` (`Theme.kt`) | Dissolvenza verso #E8E8ED | Usa `Ink.surfaceAlt` #EFECE6 |
| D-11 | KDoc di `NoticeStrip` | Superficie "soft-green" | Neutro caldo #EFECE6 |
| D-12 | `Session`, `OutsideActivity` | Citano un servizio M2 | Non tra i file analizzati; README: `m2-spike/` non compilato |

## M2

Fuori perimetro e non analizzato (`m2-spike/`). Il README segnala che la allowlist del servizio include `com.android.settings` e che va controllato il pacchetto Settings di OxygenOS: da verificare solo quando M2 sarà in ambito.

## Informazioni non verificabili con i file disponibili

- Verificati in ST1 solo su OnePlus 6 / Android 11 (compilazione, avvio, Home predefinita, Home, avvio app, Wi-Fi/Bluetooth). Non verificati: emulatore, altre versioni Android, riavvio, ramo API 26–28 e quanto elencato in `TESTING.md` §6.
- Contenuto di `m2-spike/`, `gradlew`, `gradle-wrapper.jar` e di eventuali altri file non inclusi nella conoscenza di progetto.
- Hash `0556a85c…` del logo e identità con l'asset della landing page; esito di `check_logo.py`.
- Manifest unito dell'APK (assenza effettiva di `INTERNET`).
- Versioni risolte dal Compose BOM 2024.06.00.
- Repository remoto citato nella skill di progetto.
