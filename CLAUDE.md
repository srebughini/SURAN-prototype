# CLAUDE.md — SURAN (prototipo Android M1)

Istruzioni operative per chi interviene sul codice. Il **codice è la fonte di verità**: se README, commenti o documenti divergono, segnalalo prima di procedere (elenco noto: `docs/TECHNICAL_STATUS.md`, sezione "Incongruenze").

## Mappa dei documenti

| File | Contenuto |
|---|---|
| `docs/PROJECT_OVERVIEW.md` | Visione, problema, obiettivo del prototipo, perimetro M1 |
| `docs/ARCHITECTURE.md` | Struttura, classi, flussi, stato, persistenza, dipendenze e versioni |
| `docs/M1_FEATURES.md` | Stato di ogni funzionalità M1 |
| `docs/TECHNICAL_STATUS.md` | Rischi, limiti, debito tecnico, incongruenze doc/codice |
| `docs/TESTING.md` | Test esistenti e mancanti, build, checklist |

## Regole di verifica

- Mai scrivere "compila", "funziona", "testato" senza evidenza (log di build, esito su emulatore o dispositivo).
- Distinguere sempre: ispezione del sorgente · compilazione · emulatore · dispositivo fisico (OnePlus 6).
- Dopo ogni verifica reale aggiornare la colonna "Verifica" di `docs/M1_FEATURES.md` e le voci di `docs/TECHNICAL_STATUS.md`.

## Compatibilità

- minSdk 26, targetSdk 34. Dispositivo di riferimento (rilevato dal dispositivo): OnePlus A6003 (OnePlus 6), Android 11 (API 30), OxygenOS 11.1.2.2, build ONEPLUS A6003_22_211125. Versioni della toolchain: `docs/ARCHITECTURE.md`, sezione Dipendenze.
- Percorso Home sul dispositivo di riferimento: `RoleManager` (API 29+). In ST1 SURAN è stato impostato come Home (schermata effettiva non registrata; riavvio e diniego da verificare). Il ramo API 26–28 (`ACTION_HOME_SETTINGS`) resta per minSdk e non è verificato.
- Stato verifiche: smoke test #1 superato (compilazione, installazione, Home predefinita, Home, avvio app e ritorno, Wi-Fi/Bluetooth): `docs/TESTING.md` §6. Criticità aperte: `docs/TECHNICAL_STATUS.md`.
- API 27+/29+ solo dietro guardia `Build.VERSION.SDK_INT` (modelli: `isDefaultHome`, `requestHome`) o in cartelle risorse per API (`values-v27/`).
- Package visibility (Android 11+): ogni nuovo intent risolto via `PackageManager` richiede una voce in `<queries>` del manifest.
- Nessun permesso `INTERNET`, nessuna trasmissione di dati, `allowBackup=false`. Il log eventi è solo locale (il manifest lo cita come AC-33).

## Architettura: regole da rispettare

- Un solo modulo `:app`, package `app.suran`. Nessun DI, nessun NavHost, nessun Room/DataStore.
- Stato UI: un solo `UiState` in `SuranViewModel` (StateFlow). Navigazione = `Crossfade` sull'enum `Screen` in `HomeActivity`. Eccezione: il tema è `Ink.dark`, stato Compose globale caricato in `SuranApp`.
- Persistenza: `config.json` (`ConfigStore`), `events.jsonl` (`EventLog`) con `org.json`, e `theme` (`ThemeStore`, testo "dark"/"light"). Il campo `schema` (=1) viene scritto ma non letto da `load()`: ogni cambio di formato deve restare compatibile con i file esistenti.
- UI: Compose foundation con `BasicText` tramite `T()` / `TA()`. Niente Material. Nessuna nuova dipendenza senza approvazione.
- Perimetro: M2 (`m2-spike`, servizio di accessibilità) resta fuori dal build M1. Non importare codice M2 in `app`.

## Design (non derogare)

- Colori solo da `Ink` (`Theme.kt`, valori light e dark) e `res/values/colors.xml`; palette di suran.tech. Il dark deriva dagli stessi neutri caldi e dall'accento `#3F4A3C` schiarito; il light non va modificato. Nessun colore hard-coded nelle schermate.
- Hairline da 1 dp al posto delle ombre; raggio 10 dp; niente icone decorative, card, badge, notifiche.
- Logo canonico `design/logo.svg`: non modificarlo. Dopo ogni intervento sulle icone eseguire `python3 tools/check_logo.py`.
- Testi UI in inglese (nessuna localizzazione), tema chiaro di default con tema scuro opzionale (toggle nel footer, nessun legame con il tema di sistema), solo portrait.

## Build

- Debug (necessario per l'intent demo Outside): `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
- `gradlew` e `gradle-wrapper.jar` non risultano tra i file analizzati: vedi `docs/TESTING.md`.

## Workflow

1. Analizzare i file coinvolti e le dipendenze.
2. Segnalare i rischi.
3. Proporre la modifica minima.
4. Attendere conferma per interventi significativi o architetturali.
5. Implementare in modo circoscritto: niente riscrittura di classi o refactoring se basta una modifica locale.
6. Elencare esattamente i file modificati.
7. Fornire istruzioni di build e verifica.
8. Separare ciò che è verificato da ciò che va testato su dispositivo.
9. Aggiornare la documentazione toccata.

Non rimuovere funzionalità senza autorizzazione. Non inventare file, API, test o esiti di build.

## Lean Startup

Per ogni funzionalità: problema utente → ipotesi da verificare → soluzione tecnica minima → serve al prossimo test con utenti? → come si valida. Senza obiettivo di validazione non si sviluppa. Priorità: imparare dai test reali, non aumentare il numero di funzionalità.

## Comunicazione

Italiano, sintetico, tecnico. Le scelte di prodotto non concordate si chiedono, non si assumono. Nel codice: identificatori e testi UI in inglese.
