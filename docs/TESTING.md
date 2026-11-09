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

**OnePlus 6** (A6003, Android 11 / API 30, OxygenOS 11.1.2.2, build ONEPLUS A6003_22_211125): i passi di setup 1–6 e il titolo della sezione "Install and set up the Honor 10 Lite" del README sono scritti per un altro dispositivo e vanno riletti per OnePlus 6 (debug USB, installazione, Home predefinita, batteria, notifiche). Annotare cosa funziona davvero al passo 4.

**Comandi utili**
- Log: `adb pull /sdcard/Android/data/app.suran/files/events.jsonl`
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
