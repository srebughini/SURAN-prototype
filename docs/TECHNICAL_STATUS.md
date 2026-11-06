# SURAN — Stato tecnico

## Rischi e debito tecnico

| ID | Pri | Problema | Evidenza | Azione suggerita (non eseguita) |
|---|---|---|---|---|
| TS-01 | P0 | Il codice non è mai stato compilato né eseguito | README: "source only"; la lettura non ha mostrato errori di compilazione evidenti, ma non equivale a una build | Prima build e correzioni minime |
| TS-02 | P0 | Impostazione come Home su Android 9 / EMUI non verificata (nessun dialog di ruolo; si passa da `ACTION_HOME_SETTINGS` o dal selettore di sistema) | `HomeActivity.requestHome`; README passo 4 | Provare su Honor 10 Lite e annotare il percorso che funziona |
| TS-03 | P1 | `gradlew` e `gradle-wrapper.jar` non tra i file analizzati; presente solo `gradle-wrapper.properties` | Elenco file | Build da Android Studio o generare il wrapper (`TESTING.md`) |
| TS-04 | P1 | Dopo "Add to this phone" la Home può mostrare una config vecchia: `onResume` non ricarica `config` e `OutsideActivity` scrive con un proprio `ConfigStore` | `SuranViewModel.onResume`; `OutsideActivity.onAdd` | Verificare su dispositivo; poi decidere con te se ricaricare la config |
| TS-05 | P1 | In `onAdd`, se `config.json` manca o è illeggibile si salva `Config(setupDone=false, …)`: al riavvio riparte la scelta iniziale | `OutsideActivity.onAdd` | Verificare; in M1 è raggiungibile solo con l'intent di debug |
| TS-06 | P1 | `ConfigStore.load()` restituisce `null` su qualsiasi eccezione: un file corrotto equivale a primo avvio, senza backup. `schema` è scritto ma non letto | `Config.kt` | Valutare dopo i primi test |
| TS-07 | P1 | `queryInstalled`, `catalogCandidates` (una `queryIntentActivities` per voce) e `isDefaultHome` girano sul main thread alla creazione del ViewModel e a ogni `onResume`; anche i log scrivono su file dal main thread | `SuranViewModel`, `Apps.kt`, `Catalog.kt`, `EventLog.kt` | Misurare tempo di avvio e jank sul Honor |
| TS-08 | P2 | EMUI può terminare il processo del launcher se la gestione batteria non è manuale | README passo 5 | Passo operativo di setup, da confermare |
| TS-09 | P2 | Phone/Messages/Camera/Maps si risolvono per intent: su EMUI possono comparire app Huawei/Honor con etichette diverse, e Maps può avere più candidati | README deviazione 8; `catalogCandidates` | Verificare sul dispositivo |
| TS-10 | P2 | `EventLog`: istanze multiple (Application, ViewModel, Outside) scrivono sullo stesso file; il conteggio per il taglio è per istanza, quindi il tetto di 5.000 righe non è garantito. Il file sta nella cartella external files e contiene i nomi dei pacchetti lanciati; su Android 9 le app con permesso di storage possono leggere quelle cartelle (da valutare) | `EventLog.kt` | Controllare dimensione e formato dopo uso reale |
| TS-11 | P2 | Font grande, TalkBack: non provati. Le righe Home hanno `maxLines = 1` con ellissi | `HomeScreen.HomeRow`, `T()` | Checklist in `TESTING.md` |
| TS-12 | P2 | Lista Home centrata, non più nella zona bassa del pollice: decisione UX aperta | README v0.1.5 | Giudicare sul dispositivo |
| TS-13 | P3 | Nessun test automatico, CI o configurazione lint | Nessuna cartella di test; nessuna dipendenza di test | `TESTING.md` |
| TS-14 | P3 | Release firmata con chiave debug e senza minify | `app/build.gradle.kts` | Accettabile per prototipo |
| TS-15 | P3 | Stringhe UI hard-coded nel codice (solo `app_name` in `strings.xml`); solo inglese, tema chiaro, portrait | Sorgente | Nessuna azione per M1 |
| TS-16 | P3 | `res/drawable/ic_suran.xml` è un segnaposto non referenziato (il manifest usa `@mipmap/ic_launcher`) | Manifest, D-05 | Chiedere autorizzazione prima di eliminarlo |
| TS-17 | P3 | `registerReceiver` senza flag di esportazione con targetSdk 34: i broadcast usati sono di sistema, quindi probabilmente esenti. Non verificato | `HomeScreen.rememberNow` | Verificare in build |

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

Fuori perimetro e non analizzato (`m2-spike/`). Il README segnala che la allowlist del servizio include `com.android.settings` e che va controllato il pacchetto Settings di EMUI: da verificare solo quando M2 sarà in ambito.

## Informazioni non verificabili con i file disponibili

- Compilazione, esecuzione, comportamento su emulatore o Honor 10 Lite.
- Contenuto di `m2-spike/`, `gradlew`, `gradle-wrapper.jar` e di eventuali altri file non inclusi nella conoscenza di progetto.
- Hash `0556a85c…` del logo e identità con l'asset della landing page; esito di `check_logo.py`.
- Manifest unito dell'APK (assenza effettiva di `INTERNET`).
- Versioni risolte dal Compose BOM 2024.06.00.
- Repository remoto citato nella skill di progetto.
