# SURAN — Test e verifica

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
4. Il README prevede "qualche piccola correzione di compilazione" alla prima sync: registrare qui l'esito e gli errori.
5. Installazione: `adb install -r app-debug.apk`.

## 4. Ambienti

**Emulatore** (README v0.1.1): immagine **Google Play**, non "Google APIs". API 28 corrisponde al Honor 10 Lite; API 34 prova il dialog di ruolo Home. Accedere a un account Google e installare le app da provare; WhatsApp è inaffidabile su emulatore (basta la sua schermata di registrazione). Mancano le app Honor, quindi Phone/Messages/Camera si risolvono con le app Google/AOSP.

**Honor 10 Lite**: passi di setup 1–6 nel README, sezione "Install and set up the Honor 10 Lite" (debug USB, installazione, Home predefinita, batteria manuale, notifiche). Annotare cosa funziona davvero al passo 4.

**Comandi utili**
- Log: `adb pull /sdcard/Android/data/app.suran/files/events.jsonl`
- Demo Outside (solo debug): `adb shell am start -a app.suran.OUTSIDE --es package com.instagram.android`

## 5. Checklist

Legenda ambiente: **B** build · **E** emulatore · **D** solo dispositivo fisico (E se indicato anche utile).

### Build
- [ ] B: la sync Gradle riesce
- [ ] B: `assembleDebug` riesce
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
- [ ] Ogni riga apre l'app reale; Back alla radice dell'app torna a SURAN
- [ ] Wi-Fi e Bluetooth aprono le schermate di sistema; Back e Home riportano a SURAN invariata (su alcune immagini emulatore la pagina Bluetooth può mancare: deve intervenire il fallback)
- [ ] App con login (Google, ecc.): si apre la schermata dell'app; nessun crash SURAN

### Home predefinita (E API 28 e 34, D)
- [ ] Dopo Start compare "Make SURAN your Home"; "Not now" porta alla Home con la riga discreta; la riga riapre lo sheet
- [ ] "Choose SURAN as Home" apre la schermata di Android (API 34: dialog di ruolo; API 28 e Honor: impostazioni Home)
- [ ] Dopo la scelta sheet e riga spariscono; il tasto Home mostra SURAN
- [ ] Tasto Home dentro un'app → SURAN
- [ ] D: riavvio: config persistente e SURAN ancora Home

### Solo dispositivo (D)
- [ ] APK installabile via sideload
- [ ] Chiamata di emergenza dalla schermata di blocco e dal dialer funziona
- [ ] Modalità aereo/senza rete: tutto funziona
- [ ] EMUI non termina SURAN con la gestione batteria manuale
- [ ] Quali app risolvono Phone/Messages/Camera/Maps (TS-09)
- [ ] L'aspetto è chiaramente diverso dal launcher stock
- [ ] Icona launcher = segno "S" canonico (Impostazioni → App e launcher)

### Accessibilità e dimensioni (E, D)
- [ ] Font al massimo: nessuna riga tagliata o irraggiungibile
- [ ] TalkBack: si completano setup, avvio e modifica

### Outside (E, build debug)
- [ ] L'intent demo mostra lo sheet con tre azioni nell'ordine previsto
- [ ] Back, "Open once", "Add to this phone" si comportano come nel README

### Verifiche derivate dall'analisi (non presenti nel README)
- [ ] TS-04: dopo "Add to this phone" e ritorno alla Home, la nuova riga compare?
- [ ] TS-05: "Add" con config assente o illeggibile: cosa viene salvato?
- [ ] TS-06: con `config.json` illeggibile (su debug via `run-as app.suran`) l'app riparte dalla scelta iniziale?
- [ ] TS-07: tempo di avvio a freddo e fluidità con molte app installate
- [ ] TS-10: formato righe di `events.jsonl` e crescita del file dopo uso reale
- [ ] TS-02: percorso effettivo per impostare Home su Honor 10 Lite

## 6. Registrazione degli esiti

Per ogni sessione: data, build (variante), ambiente, voci provate, esito. Poi aggiornare "Verifica" in `M1_FEATURES.md` e chiudere o riclassificare le voci `TS-xx` in `TECHNICAL_STATUS.md`.
