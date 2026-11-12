# SURAN — Test e verifica

Dispositivo di riferimento (rilevato dal dispositivo): OnePlus A6003 (OnePlus 6), Android 11 (API 30), OxygenOS 11.1.2.2, build ONEPLUS A6003_22_211125.

**Stato:** smoke test #1 (ST1) superato su questo dispositivo; esiti e voci non ancora provate in §6. Smoke test #2 (ST2, tema scuro): esito complessivo positivo, voci non registrate singolarmente. Nella checklist `[x]` = confermato in ST1; `[ ]` = non confermato o non registrato.

## 1. Test automatici esistenti

**Nessuno.** Non ci sono `src/test` né `src/androidTest` e `app/build.gradle.kts` non dichiara librerie di test. L'unico controllo automatizzabile è `tools/check_logo.py` (script Python, **non eseguito**): confronta path, colore, spessore, cap e join tra `design/logo.svg` e `ic_launcher_foreground.xml`; con un argomento verifica anche l'hash del SVG rispetto a quello indicato. Funziona con Python 3.9+ se lanciato da script (`__file__` assoluto): non verificato.

## 2. Test mancanti

- Unitari su logica pura: `normalize()`, `hasChanges`, round-trip di `ConfigStore`. `ConfigStore` e `EventLog` usano `org.json`: su JVM serve un'alternativa (es. Robolectric), non presente.
- UI (Compose/strumentati): flusso di scelta, modifica, sheet Home.
- Nessuna CI, nessun lint configurato.

Non vengono proposti ora: la priorità è la prima build e la prova su dispositivo.

## 3. Compilazione

Requisiti (README): Android Studio Koala 2024.1 o successivo, JDK 17. Versioni: `ARCHITECTURE.md` §6.

1. Aprire la cartella `suran-prototype` e lasciare completare la sync Gradle (Studio crea il wrapper).
2. Build → Build APK(s), variante **debug** (serve per l'intent demo Outside).
3. CLI: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`. Se il wrapper manca (TS-03): `gradle wrapper --gradle-version 8.7`.
4. Il README prevedeva "qualche piccola correzione di compilazione" alla prima sync. Esito prima build (ST1): compilazione riuscita; se siano servite correzioni non è stato registrato.
5. Installazione: `adb install -r app-debug.apk`.

## 4. Ambienti

**Emulatore** (README v0.1.1): immagine **Google Play**, non "Google APIs". API 30 corrisponde al OnePlus 6 (Android 11) e prova il dialog di ruolo Home (`RoleManager`). Accedere a un account Google e installare le app da provare; WhatsApp è inaffidabile su emulatore (basta la sua schermata di registrazione). Mancano le app OnePlus, quindi Phone/Messages/Camera si risolvono con le app Google/AOSP.

**OnePlus 6** (A6003, Android 11 / API 30, OxygenOS 11.1.2.2, build ONEPLUS A6003_22_211125): la sezione "Install and set up the OnePlus 6" del README è stata riscritta con le patch v0.1.8–v0.1.10; i passi su batteria OxygenOS (TS-08) e sul percorso dell'accesso alle notifiche restano da confermare e annotare sul dispositivo.

**Comandi utili**
- Log: `adb pull /sdcard/Android/data/app.suran/files/events.jsonl`
- Accesso alle notifiche (a mano): impostazioni Android → app e notifiche → accesso speciale alle app → accesso alle notifiche → SURAN. Il percorso esatto su OxygenOS 11 non è verificato: annotarlo.
- Demo Outside (solo debug): `adb shell am start -a app.suran.OUTSIDE --es package com.instagram.android`

## 5. Checklist

Legenda ambiente: **B** build · **E** emulatore · **D** solo dispositivo fisico (E se indicato anche utile).

### Build
- [x] B: sync Gradle e compilazione riescono (ST1; metodo e variante non registrati)
- [ ] B: `assembleDebug` / APK debug (necessario per l'intent Outside): variante non registrata
- [ ] B: APK Analyzer sul manifest unito: nessun permesso `INTERNET`
- [ ] B: `python3 tools/check_logo.py` tutto OK

### Scelta e modifica (E, poi D)
- [ ] Primo avvio apre Choose; nulla preselezionato; Back non fa nulla; CTA inattiva a 0
- [ ] CTA: "Start with this one" / "Start with these N"
- [ ] Una voce con due app corrispondenti apre "Which one for …?"
- [ ] "Everything else" è chiuso, si espande, esclude SURAN e le app di catalogo
- [ ] Voce non installata: riga attenuata "Not installed"; il tocco apre la modifica
- [ ] "Change": Save inattivo finché non cambia qualcosa; Cancel/Back/Home scartano
- [ ] Save changes aggiorna la Home; `config_saved` registra added/removed

### Home e avvio app (E, poi D)
- [ ] Start mostra solo le voci scelte, lista centrata, conteggio nel footer corretto
- [ ] Ready line compare solo se SURAN è già Home e sparisce
- [x] Ogni riga apre l'app reale e si torna a SURAN (ST1; modalità di ritorno non registrata)
- [ ] Back alla radice dell'app → SURAN
- [x] Wi-Fi e Bluetooth aprono le schermate di sistema (ST1)
- [ ] Back e Home riportano a SURAN invariata; azione effettiva (`connectivity_open.action`) non registrata (su alcune immagini emulatore la pagina Bluetooth può mancare: deve intervenire il fallback)
- [ ] App con login (Google, ecc.): si apre la schermata dell'app; nessun crash SURAN

### Home predefinita (E API 30, D)
- [x] SURAN risulta Home predefinita (ST1; schermata di sistema effettiva non registrata)
- [ ] Dopo Start compare "Make SURAN your Home"; "Not now" porta alla Home con la riga discreta; la riga riapre lo sheet
- [ ] "Choose SURAN as Home" apre il dialog di ruolo di Android (percorso `RoleManager`, API 29+; su OnePlus 6 / Android 11 è il percorso atteso)
- [ ] Dopo la scelta sheet e riga spariscono; il tasto Home mostra SURAN
- [ ] Tasto Home dentro un'app → SURAN
- [ ] D: riavvio: config persistente e SURAN ancora Home

### Solo dispositivo (D)
- [x] APK installabile via sideload (ST1; metodo non registrato)
- [ ] Chiamata di emergenza dalla schermata di blocco e dal dialer funziona
- [ ] Modalità aereo/senza rete: tutto funziona
- [ ] OxygenOS non termina SURAN con le impostazioni batteria applicabili (da individuare sul dispositivo)
- [ ] Quali app risolvono Phone/Messages/Camera/Maps (TS-09)
- [ ] L'aspetto è chiaramente diverso dal launcher stock
- [ ] Icona launcher = segno "S" canonico (Impostazioni → App e launcher)

### Accessibilità e dimensioni (E, D)
- [ ] Font al massimo: nessuna riga tagliata o irraggiungibile
- [ ] TalkBack: si completano setup, avvio e modifica

### Outside (E, build debug)
- [ ] L'intent demo mostra lo sheet con tre azioni nell'ordine previsto
- [ ] Back, "Open once", "Add to this phone" si comportano come nel README

### Tema scuro (F-18; D su OnePlus 6). ST2: esito complessivo positivo; le voci seguenti restano `[ ]` perché non registrate una per una
- [x] B: sync e compilazione dopo la modifica (ST2; nuovo file `ThemeStore.kt`; modifiche in `Theme.kt`, `HomeActivity`, `HomeScreen`, `ChooseScreen`, `OutsideActivity`, `SuranApp`, `SuranViewModel`)
- [ ] Light invariato: Home, Choose, sheet "Make SURAN your Home", "Which one for …?", Outside (debug) identici a prima della modifica
- [ ] Primo avvio, o dopo cancellazione dei dati dell'app: tema light
- [ ] Footer Home: "This phone is for N things · Change" e a destra "Dark"; il tocco su "Change" apre ancora la modifica, il tocco su "Dark" cambia tema
- [ ] Cambio immediato (nessun riavvio, nessuna perdita di stato): lo sfondo diventa scuro, l'etichetta diventa "Light", le icone di stato e navigazione diventano chiare; il ritorno a light le riporta scure
- [ ] Dark, tutte le schermate: Home (ora, data, righe, "Not installed", ready line, riga "Make SURAN your Home", link Wi-Fi/Bluetooth, footer), Choose (titolo, gruppi, righe selezionate e non, pallino, "Everything else", bottone attivo e disattivo, "Couldn't save"), sheet Home, "Which one for …?", Outside (debug)
- [ ] Transizione Home ↔ Choose in dark: nessun lampo chiaro durante il crossfade
- [x] Persistenza (ST2: confermata dopo riavvio del dispositivo; chiusura dai recenti e caso light non distinti): scelto dark, chiudere SURAN (dai recenti) e riaprire → dark; riavvio del dispositivo → SURAN ancora Home e ancora dark; lo stesso con light
- [ ] Debug: `adb shell run-as app.suran cat files/theme` contiene `dark` o `light`; `config.json` e `events.jsonl` non cambiano per il solo cambio tema
- [x] Avvio a freddo in dark (TS-18): descrivere il lampo chiaro, se c'è — ST2: il lampo c'è; durata e percezione non descritte
- [x] Footer con font al massimo (ST2: ok; numero di voci non registrato; TS-19 chiusa), con 1 e con 10 voci (TS-19): nessun testo tagliato, "Dark"/"Light" raggiungibile
- [ ] TalkBack: il link viene letto come "Switch to dark theme" / "Switch to light theme"
- [x] (ST2: TS-20 chiusa su indicazione dell'utente; dettagli non registrati) Tema di sistema attivo e disattivo (e l'eventuale opzione OxygenOS per le app di terze parti; percorso da annotare): SURAN appare uguale in entrambi i casi (TS-20)
- [x] App esterne e schermate di sistema (Wi-Fi, Bluetooth, impostazioni Home) non cambiano aspetto per effetto del tema di SURAN — ST2: l'utente riferisce che cambia solo SURAN
- [ ] Giudizio visivo sulla leggibilità del dark (testo, hairline, bottone disattivo) in uso reale; registrare l'esito

**Contrasti WCAG calcolati con script sui valori in `Theme.kt` (calcolo, non misura su schermo).** Light: testo 16,1:1, secondario 6,2:1, accento 8,5:1 (invariati).

| Coppia (dark) | Valori | Contrasto |
|---|---|---|
| `ink` su `bg` | #F7F5F1 su #1A1918 | 16,1:1 |
| `ink` su `surfaceAlt` | #F7F5F1 su #262422 | 14,2:1 |
| `muted` su `bg` | #B5B0A6 su #1A1918 | 8,1:1 |
| `muted` su `surfaceAlt` | #B5B0A6 su #262422 | 7,2:1 |
| `muted` su `line` (etichetta bottone disattivo) | #B5B0A6 su #3A3834 | 5,4:1 |
| `accent` su `bg` (pallino selezionato) | #A3B59E su #1A1918 | 8,1:1 |
| `bg` su `accent` (etichetta bottone) | #1A1918 su #A3B59E | 8,1:1 |
| `bg` su `accentPressed` | #1A1918 su #8FA08A | 6,3:1 |
| `error` su `bg` / su `surfaceAlt` | #E08A72 | 6,7:1 / 5,9:1 |
| `line` su `bg` (hairline, non testo) | #3A3834 su #1A1918 | 1,5:1 |

### Verifiche derivate dall'analisi (non presenti nel README)
- [ ] TS-04: dopo "Add to this phone" e ritorno alla Home, la nuova riga compare?
- [ ] TS-05: "Add" con config assente o illeggibile: cosa viene salvato?
- [ ] TS-06: con `config.json` illeggibile (su debug via `run-as app.suran`) l'app riparte dalla scelta iniziale?
- [ ] TS-07: tempo di avvio a freddo e fluidità con molte app installate
- [ ] TS-10: formato righe di `events.jsonl` e crescita del file dopo uso reale
- [x] TS-02 (audit AU-02): SURAN impostato come Home su OnePlus 6 (ST1); schermata effettiva non registrata

### Etichette da Android, `home_ui_started`, NotificationGate fase 1 (v0.1.8–v0.1.10; D su OnePlus 6). Aggiornata con gli esiti di ST3 (§6)
Legenda: `[x]` eseguita · `[~]` eseguita in parte (vedi nota) · `[ ]` non eseguita.
Build
- [x] B: build debug riuscita in Android Studio con tutte e tre le modifiche (ST3), installata sul OnePlus 6; `:app:assembleDebug` riuscito anche sul branch di consolidamento (vedi sotto). Non registrato: esito della sync
- [~] B: manifest unito (ST3: `<service>` presente con permesso e action; **elenco dei permessi non ispezionato**): assente `android.permission.INTERNET` e ogni altro `android.permission.*`; presente il `<service>` `NotificationGate` con `BIND_NOTIFICATION_LISTENER_SERVICE`. Il manifest *sorgente* non dichiara alcun `uses-permission`; il manifest unito può contenerne uno aggiunto da una libreria (probabile: `app.suran.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` di androidx.core, non verificato): annotare l'elenco reale
- [ ] D: `sha256sum` di `config.json` (via `run-as`) prima e dopo ogni prova: identico

Etichette (TS-22)
- [x] Scegliere almeno una voce di catalogo con app nota (es. Music) e un'app di "Everything else"; cambiare la lingua di sistema e tornare a SURAN: tutte le righe mostrano il nome Android nella nuova lingua, catalogo compreso — esito complessivo riferito dall'utente (ST3); dettaglio per voce non registrato
- [ ] Change: la riga secondaria delle voci di catalogo scelte è aggiornata; Save resta inattivo; nessuna selezione persa
- [ ] Disinstallare un'app scelta: riga attenuata "Not installed" con l'etichetta di ripiego
- [ ] Font al massimo, light e dark: descrivere i nomi lunghi troncati in Home (non corretti)
- [ ] Riavvio: selezione intatta, etichette nella lingua corrente

`home_ui_started` e `app_open`
- [ ] Aprire SURAN dall'icona: un solo `home_ui_started`
- [ ] Cambio lingua, tema di sistema o dimensione del font con SURAN aperto: nessun `home_ui_started` aggiuntivo
- [ ] Tasto Home con SURAN già vivo: nessun nuovo `home_ui_started` (atteso: non è un avvio dell'interfaccia)
- [x] Riavvio senza aprire SURAN, con accesso alle notifiche concesso: `app_open` e `notification_listener_connected` presenti; annotare se compare anche `home_ui_started` (atteso se Android mostra SURAN come Home) — ST3 (2026-11-10): `app_open` 13:30:35, `home_ui_started` 13:30:38 (compare perché Android mostra SURAN come Home), `notification_listener_connected` 13:30:41
- [ ] `am force-stop` e riapertura: un solo `home_ui_started`

NotificationGate fase 1 (solo registrazione; TS-23, TS-24, TS-25)
- [x] Accesso alle notifiche concesso a mano; annotare il percorso su OxygenOS 11; `notification_listener_connected` nel log — ST3: fatto; **percorso su OxygenOS 11 non registrato**; `notification_listener_connected` il 2026-11-10 alle 12:34:27
- [~] **Nessuna notifica sparisce né cambia**: app selezionata, non selezionata, dialer, SMS predefinita, sveglia, app di autenticazione, banca, e-mail, notifica ongoing — ST3: provate solo una notifica di sistema (`com.android.shell`) e una di Google Calendar: comparse e rimaste. Le altre categorie elencate non provate
- [~] Per ognuna, `decision` e `reason` plausibili in `events.jsonl`; rivedere l'elenco dei package `would_suppress` (TS-25) — ST3: due righe osservate (`no_launcher`; `would_suppress`/`unselected`, categoria `event`)
- [ ] Per ogni package di `ALLOW_TO_VERIFY` installato sul telefono: compare con `whitelist_to_verify` quando pubblica una notifica? Annotare i nomi errati o mancanti
- [~] Nessun contenuto in `events.jsonl`: cercare con `grep` titoli e testi noti delle notifiche provate — ST3: le righe osservate hanno solo i quattro campi; `grep` sistematico non eseguito
- [ ] Dedupe: la stessa app che notifica più volte produce una sola riga per (package, categoria, motivo)
- [x] Riavvio senza aprire SURAN: `notification_listener_connected` compare dopo il boot (TS-23); `am force-stop`: osservare se il servizio torna — ST3: kill (2026-11-10 13:24), riavvio (13:30) e `am force-stop` (2026-11-11 09:37): il processo riparte con avvio di tipo `service` e il listener si ricollega
- [ ] Chiamata in arrivo con Phone non selezionata e sveglia di un'app non selezionata: normali
- [ ] Revoca dell'accesso: nessuna notifica alterata, nessun nuovo evento `notification_*`; modalità aereo: tutto funziona; regressione del tema scuro

## 6. Registrazione degli esiti

Per ogni sessione: data, build (variante), ambiente, voci provate, esito. Poi aggiornare "Verifica" in `M1_FEATURES.md` e chiudere o riclassificare le voci `TS-xx` in `TECHNICAL_STATUS.md`.

### ST1 — smoke test #1 (registrato il 2026-11-06)

- **Ambiente:** OnePlus A6003 (OnePlus 6), Android 11 (API 30), OxygenOS 11.1.2.2, build ONEPLUS A6003_22_211125. Variante di build e metodo di installazione non registrati.
- **Superati:** compilazione · installazione e avvio · SURAN impostato come Home predefinita · funzionamento della Home (in generale) · apertura delle app e ritorno a SURAN · accesso alle impostazioni Wi-Fi e Bluetooth.
- **Non registrati o da fare:**
  - Home: schermata di sistema effettiva (dialog di ruolo), riavvio e persistenza, diniego del ruolo (AU-03), tasto Home dentro un'app.
  - Apertura app e connettività: Back alla radice dell'app; ritorno da Wi-Fi/Bluetooth e azione di fallback usata.
  - Scelta e modifica: dettagli di Choose, modifica, "Not installed", ambiguità, ready line.
  - Dati e sistema: contenuto del log eventi, latenza di ritorno a Home (AU-05), impostazioni batteria OxygenOS (AU-06), app risolte per Phone/Messages/Maps/Camera (AU-07).
  - Altro: chiamata di emergenza, modalità aereo, font grande, TalkBack, manifest unito senza `INTERNET`, `check_logo.py`.

### ST2 — smoke test #2, tema scuro (registrato il 2026-11-06)

- **Ambiente:** OnePlus A6003 (OnePlus 6), Android 11 (API 30), OxygenOS 11.1.2.2. Build con la modifica del tema scuro (patch `suran-dark-theme`); variante e metodo non registrati.
- **Esito:** l'utente riferisce che funziona tutto. Compilazione e avvio con la modifica riusciti. Confermato: il lampo chiaro all'avvio a freddo in dark c'è (TS-18); il footer è ok con font al massimo (TS-19, chiusa); il tema cambia solo SURAN; la scelta persiste anche dopo il riavvio del dispositivo; TS-20 chiusa su indicazione dell'utente.
- **Non registrati:** cambio immediato, aspetto del light invariato schermata per schermata, TalkBack, giudizio sulla leggibilità, numero di voci nel test del footer. TS-21 resta aperta.

### Controlli sul sorgente per v0.1.8–v0.1.10 (2026-11-09) — **non sono una build né un test sul dispositivo**

- **Eseguiti:** `NotificationGate.kt` più `EventLog.kt`, `Config.kt`, `Catalog.kt`, `Apps.kt` compilati con `kotlinc` 1.9.24 (JVM 17) contro `android.jar` API 30, senza errori né avvisi. Non è la build Gradle/AGP: le modifiche a `HomeScreen.kt`, `ChooseScreen.kt` e `SuranViewModel.kt` dipendono da Compose e lifecycle e **non sono state compilate**. Manifest: XML ben formato. `grep` su `NotificationGate.kt`: nessun riferimento a `extras`, `tickerText`, `contentView`, `actions`; nessuna chiamata di cancellazione o snooze.
- **Non eseguiti allora:** build Gradle, installazione, qualsiasi prova su emulatore o OnePlus 6. Eseguiti dopo: vedi ST3.

### ST3 — etichette, `home_ui_started`, NotificationGate fase 1 (registrato il 2026-11-11; prove del 2026-11-10 e del 2026-11-11)

- **Ambiente:** OnePlus A6003 (OnePlus 6), Android 11 (API 30), OxygenOS 11.1.2.2. Build debug del codice di v0.1.8–v0.1.10 compilata in Android Studio da una copia di lavoro separata (commit locale `e75ba1d`; i sorgenti in `app/` coincidono con quelli di questo consolidamento, confronto dell'albero eseguito) e installata il 2026-11-10 alle 12:30:19. Il primo tentativo di installazione è fallito con `INSTALL_PARSE_FAILED_NOT_APK` (causa non accertata); l'installazione successiva è riuscita. Il branch di consolidamento è stato compilato in seguito (vedi "Build del branch di consolidamento").
- **Prima delle prove:** l'APK installato alle 11:49:42 (manifest ispezionato) non conteneva `NotificationGate`: la diagnosi "servizio non registrato" dipendeva dalla build installata, non dal codice (TS-26). Ispezionare il manifest dell'APK installato prima di ogni prova.
- **Superati (con evidenza):**
  - Etichette nella lingua di sistema: verificate dall'utente (esito complessivo; dettaglio per voce non registrato).
  - Manifest unito con il `<service>` e `dumpsys package`: servizio registrato con filtro e permesso; SURAN compare in Accesso alle notifiche.
  - Accesso concesso a mano: `enabled_notification_listeners` contiene `app.suran/app.suran.NotificationGate`; `dumpsys notification` lo mostra tra i listener attivi.
  - `notification_listener_connected` (2026-11-10 12:34:27).
  - `notification_decision`: `com.android.shell`, categoria `null`, `allow`/`no_launcher` (12:38:24); `com.google.android.calendar`, categoria `event`, `would_suppress`/`unselected` (12:44:35). Le due notifiche sono comparse e rimaste sul telefono.
  - Persistenza: (a) kill del processo (2026-11-10 13:24:24): nuovo processo di tipo `service` per `NotificationGate` dopo circa 4 s, `connected` 13:24:38; (b) riavvio: primo avvio dopo il boot di tipo `service` (13:30:33), `connected` 13:30:41; (c) `am force-stop` (2026-11-11 09:37:48): nel log di sistema `binding died` e, 44 ms dopo, un nuovo `binding:` del servizio; nuovo processo con avvio di tipo `service`, `connected` nello stesso secondo, concessione intatta. `events.jsonl` conservato in tutti e tre i casi.
- **Osservati, non spiegati:** dopo il force-stop un secondo bind 10,0 s dopo il primo, con `notification_listener_disconnected` e `_connected` (nessuna riga di unbind nel log di sistema); `connected` 8–10 s dopo l'avvio del processo (kill e riavvio); al boot (2026-11-11 09:35) primo bind fallito ("not found") e secondo riuscito 19 s dopo. Nessuna causa accertata (TS-23).
- **Non eseguiti:** rami `selected`, `self`, `no_config`, `category` (call, alarm, system, error), `ongoing`, `default_sms`, `default_dialer`, whitelist 2FA; dedupe; `grep` di titoli e testi noti; elenco permessi del manifest unito; `sha256sum` di `config.json`; blocco schermo e primo sblocco; gestione batteria OxygenOS; pulizia dalle app recenti; inattività lunga; revoca dell'accesso; modalità aereo; regressione del tema scuro; `requestRebind` (effetto non attribuibile); percorso di concessione nelle impostazioni.
- **Aggiornato dopo ST3:** `M1_FEATURES.md` (F-06, F-12, F-19) e TS-22…TS-26.

### Build del branch di consolidamento (registrato il 2026-11-12; data della build non registrata)

- **Branch:** `cons/m1-notifications`, 4 commit su `origin/main` (`37d2176`): `feat: log home UI start event`, `feat: add NotificationGate phase 1 logging`, `docs: update M1 implementation and test status`, `chore: ignore local configuration and event logs`. Working directory pulita (`git status --short` vuoto).
- **Comando:** `:app:assembleDebug`, eseguito dall'utente (log di Gradle riportato).
- **Esito:** `BUILD SUCCESSFUL in 23s`; 35 task, 10 eseguiti e 25 `UP-TO-DATE`. Tra gli eseguiti: `processDebugMainManifest`, `compileDebugKotlin`, `dexBuilderDebug`, `packageDebug`. Il log riportato non contiene errori né warning.
- **Non eseguiti:** lint, test (non esistono test automatici), installazione e prova sul dispositivo di questo APK. Il codice in `app/` coincide con quello provato in ST3 (confronto eseguito sui file delle patch).
