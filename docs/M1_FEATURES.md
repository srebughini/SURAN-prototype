# SURAN — Funzionalità M1

**Legenda stato:** Implementata = presente e coerente nel sorgente · Parziale = presente ma incompleta o con limiti noti · Non implementata.

**Verifica:** ST1 = smoke test #1 su OnePlus 6 (Android 11, API 30), `TESTING.md` §6. Scelta iniziale, salvataggio e catalogo sono stati attraversati per arrivare alla Home, ma gli esiti puntuali non sono stati registrati: restano "Non verificata". ST2 = smoke test #2 sul tema scuro (F-18), stesso dispositivo: esito complessivo positivo riferito dall'utente, voci non registrate singolarmente (`TESTING.md` §6).

| ID | Funzionalità | Stato | Verifica | Note |
|---|---|---|---|---|
| F-01 | Scelta iniziale "What is this phone for?": nulla preselezionato, CTA inattiva a 0 ("Choose what this phone is for"), "Start with this one" / "Start with these N", Back non fa nulla | Implementata | Non verificata | `ChooseScreen`, `SuranViewModel.initial` |
| F-02 | Catalogo curato di 8 voci in 5 gruppi; candidati installati risolti per pacchetti o per intent; voci senza candidato nascoste | Implementata | Non verificata | Voci: phone, messages (Talk); whatsapp (Talk); maps (Get around); camera (Capture); music (Listen); authenticator, wallet (Pay and prove). Su OxygenOS possibili app OnePlus con etichette diverse: TS-09 |
| F-03 | Disambiguazione "Which one for …?" quando una voce ha più candidati | Implementata | Non verificata | |
| F-04 | "Everything else (N)": sezione collassata; esclude SURAN e le app candidate del catalogo | Implementata | Non verificata | |
| F-05 | Persistenza della configurazione in `config.json` (scrittura atomica, `schema` 1) | Implementata | Non verificata | TS-06 (file illeggibile = primo avvio) |
| F-06 | Home: ora e data aggiornate, lista centrata, righe da 64 dp, nome app secondario se diverso, "Not installed" attenuato | Implementata | Verificata in generale (ST1); dettagli non registrati | TS-11, TS-12 |
| F-07 | Avvio dell'app dalla riga; app non installata → apre la modifica; errore "Couldn't open X." per 3 s | Implementata | Verificata: avvio app e ritorno a SURAN (ST1). Non verificati: app non installata, errore di lancio | Lo schermo successivo appartiene all'app lanciata |
| F-08 | Modifica da footer "Change": Save attivo solo se cambia qualcosa; Cancel/Back/Home scartano; gruppo "Not installed" per rimuovere voci non più disponibili | Implementata | Non verificata | Il gruppo "Not installed" non era nelle specifiche (README, deviazione 4) |
| F-09 | Impostazione come Home predefinita: sheet esplicativo, richiesta di ruolo (API 29+) o impostazioni Home (API 26–28), fallback al selettore di sistema, riga "Make SURAN your Home", chiusura automatica quando SURAN è Home | Implementata | Verificata in parte (ST1): SURAN impostato come Home su OnePlus 6. Non verificati: riavvio, diniego (AU-03), sheet e riga, ramo API 26–28 | Percorso atteso `RoleManager` (API 29+); schermata di sistema effettiva non registrata. TS-02 chiuso per questa configurazione |
| F-10 | "Ready line" ("Your phone is ready…") se SURAN è già Home dopo il primo Start; scompare dopo 6 s o al primo lancio | Implementata | Non verificata | |
| F-11 | Link "Wi-Fi" e "Bluetooth" in Home verso le schermate di sistema, con fallback a impostazioni wireless e generali | Implementata | Verificata: accesso alle schermate Wi-Fi e Bluetooth (ST1). Non verificati: ritorno a SURAN, fallback | Nessun permesso; SURAN non modifica nulla |
| F-12 | Log eventi locale (`events.jsonl`) | Implementata | Non verificata | Eventi in `ARCHITECTURE.md` §5; TS-10 |
| F-13 | Schermata Outside: sheet con "Back to your phone", "Open once", "Add to this phone" | Parziale | Non verificata | Raggiungibile solo con l'intent di debug. "Open once" scrive `Session.onceAllowed`, che in M1 nessuno legge. "Add" salva in config ma la Home non si ricarica: TS-04, TS-05 |
| F-14 | Icona launcher adaptive derivata 1:1 da `design/logo.svg` | Implementata | Non verificata | `tools/check_logo.py` non eseguito; asset inutilizzato `ic_suran.xml`: D-05 |
| F-15 | Nessun permesso `INTERNET`; `allowBackup=false` | Implementata | Non verificata | Visto nel manifest sorgente; manifest unito dell'APK non ispezionato |
| F-16 | Accessibilità di base: `semantics` con etichette, ruoli (Button, Checkbox), aree di tocco ≥ 48 dp | Implementata | Non verificata | Presenza nel codice; TalkBack non provato (TS-11) |
| F-17 | Vincoli dichiarati: solo portrait, solo inglese; tema chiaro di default (il tema scuro è opzionale: F-18) | Implementata | Non verificata | TS-15 |
| F-18 | Tema scuro opzionale, solo dentro SURAN: link "Dark"/"Light" nel footer Home accanto a "Change" (mostra il tema a cui si passa); cambio immediato; default light; scelta salvata in `filesDir/theme` e ripristinata all'avvio; non segue il tema di sistema; icone delle barre di sistema coerenti; tema chiaro invariato | Implementata | Verificata (ST2, OnePlus 6): cambia solo SURAN, la scelta persiste dopo il riavvio del dispositivo, footer ok con font al massimo, lampo chiaro all'avvio a freddo presente (TS-18). Non registrati: cambio immediato, contrasti, TalkBack. TS-20 chiusa | `Theme.kt` (`Ink.dark`, `Ink.scrim`, `barStyle`), `ThemeStore.kt`. Contrasti calcolati, non misurati su schermo: `TESTING.md` §5. TS-18, TS-19, TS-20, TS-21. Nessun evento di log aggiunto |

## Non implementate (in M1)

| ID | Elemento | Nota |
|---|---|---|
| N-01 | Rilevamento reale delle app "esterne" (apertura automatica dello sheet Outside) | Previsto con M2 (servizio di accessibilità, `m2-spike/`, non analizzato) |
| N-02 | Gestione/intercettazione delle notifiche | README: limite noto |
| N-03 | Localizzazione, orientamento landscape, tema scuro legato al tema di sistema | Il tema scuro manuale è F-18 |
| N-04 | Test automatici | `TESTING.md` |
