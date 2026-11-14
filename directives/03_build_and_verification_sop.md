# SOP-DEV-03: Validazione del Codice, Performance Compose e Build Automation

## 1. Obiettivo
Stabilire le regole di conformità architetturale, i vincoli anti-allocazione per il rendering a 120 Hz in Jetpack Compose, l'adozione universale dei font a spaziatura tabulare (`tnum`), e i processi di automazione deterministica per versionamento e build.

---

## 2. Vincoli di Performance Jetpack Compose (120 Hz Smoothness)

### 2.1 Divieto di Allocazione in `DrawScope`
In dispositivi moderni a 90 Hz, 120 Hz o 144 Hz, il blocco di disegno di un `Canvas` viene invocato ogni $8,33\text{ ms}$ (a 120 FPS). L'istanziazione di oggetti heap all'interno di questo loop genera elevato GC Churn (Garbage Collection overhead), provocando frame drop ("jank") inaccettabili in un'app di misura di precisione.

**Regole tassative**:
1. **Nessuna istanziazione di `Paint` o `Path` o `Stroke`**: Oggetti `Path()`, `Stroke(width = ...)`, o gradienti non devono MAI essere creati all'interno della lambda `Canvas { ... }`.
2. **Utilizzo di `remember` o Cache di Livello Componente**: Se un `Path` o `Stroke` è necessario, deve essere istanziato in `remember { ... }` e aggiornato tramite metodi mutabili (`path.reset()`, `path.moveTo()`, etc.) oppure passato come argomento stabile pre-allocato.
3. **Colori e Brush**: Utilizzare costanti colore o definizioni memorizzate all'esterno del ciclo di rendering.

### 2.2 Requisito Font a Spaziatura Tabulare (`tnum`)
Durante il trascinamento di un cursore o il campionamento di un angolo a 100 Hz, le cifre decimali cambiano continuamente. Se i caratteri hanno larghezza proporzionale standard, la larghezza del testo varia rapidamente, causando fastidiosi scatti orizzontali del testo ("jittering numerico").

**Regola tassativa**:
Tutti i testi contenenti numeri variabili (lunghezze, aree, angoli, pendenze, dimensioni anelli) DEVONO utilizzare:
```kotlin
TextStyle(
    fontFeatureSettings = "tnum",
    // ...
)
```
attraverso lo stile tematico `TabularNumberStyle` di Material 3 Expressive.

---

## 3. Protocollo di Versionamento Semantico (SemVer)
Il file `app/build.gradle.kts` definisce la versione secondo lo schema:
- `val versionMajor = 1` // Nuove funzionalità architetturali
- `val versionMinor = 0` // Aggiornamenti sostanziosi
- `val versionPatch = 0` // Bug fix e ottimizzazioni minori

Formula di calcolo del `versionCode`:
$$\text{versionCode} = \text{versionMajor} \times 10000 + \text{versionMinor} \times 100 + \text{versionPatch}$$
$$\text{versionName} = \text{"v\$\text{versionMajor}.\$\text{versionMinor}.\$\text{versionPatch}"}$$

Esempio: per `v1.0.0` $\rightarrow$ `versionCode = 10000`.

---

## 4. Pipeline di Esecuzione Deterministica (Execution Layer)
Prima di qualsiasi commit o rilascio di build, devono essere eseguiti in sequenza:

1. **Verifica Matematica Metrologica**:
   ```bash
   python3 execution/verify_metrology_math.py
   ```
   Valida formule `pxPerMm`, anelli ISO 8653, angoli `atan2` e filtro EMA.

2. **Audit Performance Compose**:
   ```bash
   python3 execution/audit_compose_perf.py
   ```
   Scansiona i file sorgente Kotlin (`app/src/main/java/com/example/truescale/ui/components/*.kt`) verificando l'assenza di allocazioni proibite in `Canvas` e l'applicazione di `tnum`.

3. **Compilazione ed Esecuzione Test Unitari**:
   ```bash
   python3 execution/bump_version_and_build.py --check-only
   ```
   Esegue `./gradlew assembleDebug testDebugUnitTest` indirizzando tutti i log intermedi in `.tmp/build_logs/`.

---

## 5. Regole di Igiene `.tmp/` e Casi Limite di Linting
1. Nessun comando di build o script deve lasciare file di log nella root del repository.
2. La cartella `.tmp/` e le relative sottocartelle (`.tmp/build_logs/`, `.tmp/reports/`) sono ignorate da git.
3. All'avvio degli script in `execution/`, la directory `.tmp/` viene creata automaticamente se non presente.
4. **Caso Limite Static Analysis Composable Name**: Nello scanning regex delle allocazioni in `DrawScope`, i nomi dei composable personalizzati che terminano con la parola `Canvas` (es. `RulerCanvas`, `LevelCanvas`) non devono essere scambiati per il blocco di disegno `androidx.compose.foundation.Canvas`. Il linter impone il vincolo di word boundary `\bCanvas` per isolare esclusivamente la lambda del frame-loop.
5. **Caso Limite Gestione Gesti nel Tamburo Cilindrico 3D (RotaryDrumPicker)**:
   - *Causa*: L'uso di `Modifier.draggable` con accumulatore continuo e soglia fissa in pixel (`30f`) su schermi moderni ad alta densità (~500 ppi) causava selezioni a raffica incontrollate al minimo movimento, bloccando inoltre gli eventi di tap sulle singole voci e distruggendo la vista se smontata a metà transizione.
   - *Risoluzione*: Sostituzione con `pointerInput` e `detectVerticalDragGestures` controllato a step singolo per swipe (`hasTriggeredInDrag = true` fino al rilascio), soglia ergonomica in `Dp` (`28.dp.toPx()`), aree di tap diretto reattive con `clickable` su ciascuna fascia del cilindro e sulle frecce SU/GIÙ, e permanenza del tamburo di navigazione in tutte le sezioni dell'app.
