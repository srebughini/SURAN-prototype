# SURAN — Panoramica di progetto

## Visione

SURAN è un'app Android sperimentale per rendere lo smartphone **esistente** più calmo e intenzionale. Si usa come schermata Home (launcher) e mostra solo le cose che la persona ha scelto per quel telefono. L'identità visiva è allineata al sito suran.tech (palette, tipografia, hairline); il segno è una "S" monolinea (`design/logo.svg`).

## Problema (desunto)

Un launcher standard espone l'intero telefono, distrazioni comprese. Il codice risponde con: scelta iniziale "What is this phone for?", Home con i soli elementi scelti, footer "This phone is for N things". Target, metriche e ipotesi misurabili **non sono formalizzati** nel repository.

## Obiettivo del prototipo

Validare M1 su dispositivi reali (riferimento: OnePlus A6003 / OnePlus 6, Android 11 (API 30), OxygenOS 11.1.2.2, build ONEPLUS A6003_22_211125, dati rilevati dal dispositivo), non costruire un prodotto completo. Il README elenca le domande aperte da chiarire sul dispositivo (non sono ipotesi formali):

- quante righe entrano davvero nella Home e con quale scala tipografica;
- se il footer "Change" è abbastanza scopribile;
- se il nome app secondario sotto l'etichetta aiuta o disturba;
- se "Everything else" è troppo nascosto o ancora troppo prominente;
- se "Add to this phone" con salvataggio immediato provoca deriva;
- se la lista centrata (non più nella zona bassa del pollice) funziona.

Il log locale `events.jsonl` serve a raccogliere dati su questi punti; non viene trasmesso.

**Stato:** smoke test #1 superato sul dispositivo di riferimento (compilazione, installazione, Home predefinita, Home, avvio app e ritorno, Wi-Fi/Bluetooth); dettagli in `TESTING.md` §6. Smoke test #2 (tema scuro, F-18) superato in generale sullo stesso dispositivo. Interventi v0.1.8–v0.1.10 (etichette da Android, `home_ui_started`, NotificationGate fase 1): compilati con Gradle e verificati in parte sul OnePlus 6 (ST3, `TESTING.md` §6): etichette verificate dall'utente; NotificationGate fase 1 (solo registrazione) si collega, registra `notification_decision` e, dopo kill, riavvio e force-stop, il processo riparte (avvio di tipo `service`) e il listener si ricollega; restano non verificati gli altri rami di decisione, la whitelist 2FA e `requestRebind`. Criticità aperte: `TECHNICAL_STATUS.md`.

## Esperimento: notifiche (sperimentale, fase 1)

- **Problema:** un'app non scelta può continuare a mostrare notifiche e distrarre.
- **Ipotesi:** mostrare notifiche soltanto dalle app scelte rende lo smartphone meno distraente.
- **Soluzione minima:** fase 1 = sola registrazione (`NotificationGate`): per ogni notifica si registra package, categoria, decisione prevista e motivo; nessuna notifica viene toccata. L'accesso alle notifiche lo concede l'utente a mano.
- **Validazione:** prova sul OnePlus 6, lettura di `events.jsonl` per capire quali package pubblicano notifiche e quali sarebbero soppressi. La soppressione (fase 2) si valuta solo dopo, con una decisione esplicita, e non prima di aver verificato i package potenzialmente importanti (2FA, banche, e-mail, approvazioni push: TS-25).
- **Vincoli:** nessuna lettura del contenuto, nessun DND globale né modifica delle impostazioni di sistema, nessuna statistica o dashboard. Chiamate, emergenze e servizi essenziali non vanno soppressi indiscriminatamente.

## Perimetro M1

Dentro (stato di ciascuna voce: `M1_FEATURES.md`): scelta e modifica di ciò per cui serve il telefono, Home personalizzata, avvio delle app scelte, impostazione come Home predefinita, accesso alle impostazioni Wi-Fi e Bluetooth, tema scuro opzionale solo dentro SURAN (toggle nel footer, default light), log eventi locale, schermata "Outside" dimostrativa (solo intent di debug), icona derivata dal logo canonico, esperimento sulle notifiche in sola registrazione (fase 1, sperimentale).

Fuori da M1: rilevamento reale delle app "esterne", soppressione effettiva delle notifiche, tema legato al sistema, localizzazione, test automatici.

## M2 (separato)

Il servizio di accessibilità sperimentale vive in `m2-spike/`: non è nel build, non è stato eseguito, e il suo contenuto **non era tra i file analizzati**. Il codice M1 contiene solo predisposizioni (`Session`, `OutsideActivity`). Il `NotificationListenerService` dell'esperimento notifiche è parte di M1 e non è il servizio di accessibilità di M2. Non mescolare M1 e M2 senza richiesta esplicita.

## Documenti collegati

Architettura: `ARCHITECTURE.md` · Funzionalità: `M1_FEATURES.md` · Rischi: `TECHNICAL_STATUS.md` · Test: `TESTING.md` · Regole operative: `../CLAUDE.md`.
