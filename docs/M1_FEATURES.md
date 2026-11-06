# SURAN — Funzionalità M1

**Legenda stato:** Implementata = presente e coerente nel sorgente · Parziale = presente ma incompleta o con limiti noti · Non implementata.

| ID | Funzionalità | Stato | Verifica | Note |
|---|---|---|---|---|
| F-01 | Scelta iniziale "What is this phone for?": nulla preselezionato, CTA inattiva a 0 ("Choose what this phone is for"), "Start with this one" / "Start with these N", Back non fa nulla | Implementata | Non verificata | `ChooseScreen`, `SuranViewModel.initial` |
| F-02 | Catalogo curato di 8 voci in 5 gruppi; candidati installati risolti per pacchetti o per intent; voci senza candidato nascoste | Implementata | Non verificata | Voci: phone, messages (Talk); whatsapp (Talk); maps (Get around); camera (Capture); music (Listen); authenticator, wallet (Pay and prove). Su EMUI possibili app Huawei/Honor: TS-09 |
| F-03 | Disambiguazione "Which one for …?" quando una voce ha più candidati | Implementata | Non verificata | |
| F-04 | "Everything else (N)": sezione collassata; esclude SURAN e le app candidate del catalogo | Implementata | Non verificata | |
| F-05 | Persistenza della configurazione in `config.json` (scrittura atomica, `schema` 1) | Implementata | Non verificata | TS-06 (file illeggibile = primo avvio) |
| F-06 | Home: ora e data aggiornate, lista centrata, righe da 64 dp, nome app secondario se diverso, "Not installed" attenuato | Implementata | Non verificata | TS-11, TS-12 |
| F-07 | Avvio dell'app dalla riga; app non installata → apre la modifica; errore "Couldn't open X." per 3 s | Implementata | Non verificata | Lo schermo successivo appartiene all'app lanciata |
| F-08 | Modifica da footer "Change": Save attivo solo se cambia qualcosa; Cancel/Back/Home scartano; gruppo "Not installed" per rimuovere voci non più disponibili | Implementata | Non verificata | Il gruppo "Not installed" non era nelle specifiche (README, deviazione 4) |
| F-09 | Impostazione come Home predefinita: sheet esplicativo, richiesta di ruolo (API 29+) o impostazioni Home (API 26–28), fallback al selettore di sistema, riga "Make SURAN your Home", chiusura automatica quando SURAN è Home | Implementata | Non verificata | Flusso critico su Honor 10 Lite: TS-02; D-01 |
| F-10 | "Ready line" ("Your phone is ready…") se SURAN è già Home dopo il primo Start; scompare dopo 6 s o al primo lancio | Implementata | Non verificata | |
| F-11 | Link "Wi-Fi" e "Bluetooth" in Home verso le schermate di sistema, con fallback a impostazioni wireless e generali | Implementata | Non verificata | Nessun permesso; SURAN non modifica nulla |
| F-12 | Log eventi locale (`events.jsonl`) | Implementata | Non verificata | Eventi in `ARCHITECTURE.md` §5; TS-10 |
| F-13 | Schermata Outside: sheet con "Back to your phone", "Open once", "Add to this phone" | Parziale | Non verificata | Raggiungibile solo con l'intent di debug. "Open once" scrive `Session.onceAllowed`, che in M1 nessuno legge. "Add" salva in config ma la Home non si ricarica: TS-04, TS-05 |
| F-14 | Icona launcher adaptive derivata 1:1 da `design/logo.svg` | Implementata | Non verificata | `tools/check_logo.py` non eseguito; asset inutilizzato `ic_suran.xml`: D-05 |
| F-15 | Nessun permesso `INTERNET`; `allowBackup=false` | Implementata | Non verificata | Visto nel manifest sorgente; manifest unito dell'APK non ispezionato |
| F-16 | Accessibilità di base: `semantics` con etichette, ruoli (Button, Checkbox), aree di tocco ≥ 48 dp | Implementata | Non verificata | Presenza nel codice; TalkBack non provato (TS-11) |
| F-17 | Vincoli dichiarati: solo portrait, solo tema chiaro, solo inglese | Implementata | Non verificata | TS-15 |

## Non implementate (in M1)

| ID | Elemento | Nota |
|---|---|---|
| N-01 | Rilevamento reale delle app "esterne" (apertura automatica dello sheet Outside) | Previsto con M2 (servizio di accessibilità, `m2-spike/`, non analizzato) |
| N-02 | Gestione/intercettazione delle notifiche | README: limite noto |
| N-03 | Tema scuro, localizzazione, orientamento landscape | |
| N-04 | Test automatici | `TESTING.md` |
