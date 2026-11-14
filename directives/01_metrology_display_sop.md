# SOP-MET-01: Metrologia del Display e Calibrazione Sub-Pixel

## 1. Obiettivo
Fornire le specifiche matematiche, architetturali e i requisiti fisici per convertire le coordinate digitali del display Android in misure di lunghezza assolute reali (millimetri e centimetri) con precisione sub-pixel, compensando downscaling software, asimmetria OLED e imperfezioni di fabbrica tramite standard ISO/IEC 7810.

---

## 2. Modello Matematico di Conversione Display
Nei display moderni, l'utilizzo di densità logiche (`Density.density`, unità Compose `Dp`) è severamente vietato per scopi metrologici in quanto basato su bucket discreti (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`) approssimati.

### 2.1 Lettura Indipendente degli Assi (Compensazione Asimmetria OLED)
A causa della disposizione fisica dei sub-pixel (matrici PenTile, Diamond OLED o layout RGB a passo differenziato), il passo dei pixel lungo l'asse orizzontale e verticale può differire.
I valori reali forniti dal pannello devono essere acquisiti separatamente:
- `xdpi = resources.displayMetrics.xdpi`
- `ydpi = resources.displayMetrics.ydpi`

### 2.2 Rilevamento e Compensazione del Downscaling Risoluzione
Molti dispositivi (es. Samsung Galaxy, Google Pixel, Xiaomi) consentono all'utente di impostare la risoluzione di rendering di sistema a FHD+ (es. 1080p) su pannelli fisicamente QHD+ (es. 1440p). Se il sistema scala il frame-buffer, i valori `xdpi`/`ydpi` esposti a livello finestra possono risultare alterati.

La compensazione avviene confrontando la dimensione fisica del display con i bounds della finestra:
```kotlin
val physicalMode = display.mode
val physicalWidth = physicalMode.physicalWidth.toFloat()
val physicalHeight = physicalMode.physicalHeight.toFloat()

val windowBounds = windowMetrics.bounds
val windowWidth = windowBounds.width().toFloat()
val windowHeight = windowBounds.height().toFloat()

val scaleX = if (windowWidth > 0f) physicalWidth / windowWidth else 1.0f
val scaleY = if (windowHeight > 0f) physicalHeight / windowHeight else 1.0f
```

### 2.3 Formula Finale Pixel per Millimetro
Dato che 1 pollice internazionale equivale esattamente a $25,4\text{ mm}$:

$$\text{pxPerMmX} = \left(\frac{xdpi \times scaleX}{25.4}\right) \times \text{calibrationFactor}$$

$$\text{pxPerMmY} = \left(\frac{ydpi \times scaleY}{25.4}\right) \times \text{calibrationFactor}$$

Dove:
- `calibrationFactor`: coefficiente scalare a virgola mobile a 4 cifre decimali (default $1.0000$), tarato tramite standard ISO.

---

## 3. Specifiche di Rendering Sub-Pixel (Canvas)
1. **Coordinate Sub-Pixel**: Tutte le coordinate di rendering devono essere espresse in `Float` nativo su `DrawScope`, senza arrotondamenti preventivi a `Int`.
2. **Tracciamento Tacche**:
   - Spessore linea: `1.5f` px fissi con anti-aliasing hardware abilitato.
   - Tacca 1 mm: lunghezza $4.0\text{ mm}$ (pixel calcolati: $4.0 \times \text{pxPerMmX}$).
   - Tacca 5 mm: lunghezza $7.0\text{ mm}$.
   - Tacca 10 mm (1 cm): lunghezza $10.0\text{ mm}$ con etichetta numerica tabulare (`tnum`).
3. **Punto Zero Assoluto**: Il bordo fisico superiore o sinistro della scocca coincide con la coordinata `0f` grazie alla modalità full-screen edge-to-edge con cutoff rimosso.

---

## 4. Modalità Operative del Regolo Metrologico

### 4.1 Cursore Singolo (`SINGLE_CURSOR`)
- Misura lineare unidimensionale dal punto zero (`0.00 cm`) alla coordinata `y` del cursore.
- Readout: $\text{valore} = \frac{y}{\text{pxPerMmY} \times 10.0}\text{ cm}$.

### 4.2 Calibro Doppio Cursore (`DUAL_CALIPER`)
- Due cursori multi-touch indipendenti con gestione di puntamento simultaneo.
- Distanza calibro: $\Delta y = \frac{|y_2 - y_1|}{\text{pxPerMmY} \times 10.0}\text{ cm}$.

### 4.3 Rettangolo 2D (`RECTANGLE_2D`)
- Bounding box bidimensionale con vertici ridimensionabili.
- Base: $B = \frac{|x_2 - x_1|}{\text{pxPerMmX} \times 10.0}\text{ cm}$
- Altezza: $H = \frac{|y_2 - y_1|}{\text{pxPerMmY} \times 10.0}\text{ cm}$
- Area: $S = B \times H\text{ cm}^2$

### 4.4 Mirino Circolare & Taglie Anelli EU (`CIRCLE_RING`)
- Mirino radiale con reticolo a 45° e cerchi concentrici.
- Diametro interno: $\emptyset = \frac{2 \times r}{\text{pxPerMmY} \times 10.0}\text{ cm}$
- Circonferenza interna: $C = \pi \times \emptyset \times 10.0\text{ mm}$
- Standard ISO 8653 (EU Ring Size): la misura europea dell'anello corrisponde esattamente alla circonferenza interna espressa in millimetri:
  $$\text{EU Size} = \text{round}(C_{\text{mm}}) = \text{round}(\pi \times d_{\text{mm}})$$
  Con range standard normalizzato da taglia EU 41 ($d \approx 13,05\text{ mm}$) a EU 76 ($d \approx 24,20\text{ mm}$).

### 4.5 Goniometro 180° (`PROTRACTOR_180`)
- Vertice di origine $V(x_0, y_0)$ e due semirette direzionali $P_1(x_1, y_1)$ e $P_2(x_2, y_2)$.
- Angoli polari rispetto all'asse X calcolati con arcotangente a due argomenti:
  $$\theta_1 = \text{atan2}(y_1 - y_0, x_1 - x_0)$$
  $$\theta_2 = \text{atan2}(y_2 - y_0, x_2 - x_0)$$
- Angolo convesso compreso:
  $$\Delta\theta = |\theta_2 - \theta_1|$$
  Se $\Delta\theta > \pi$, allora $\Delta\theta = 2\pi - \Delta\theta$.
  Conversione in gradi decimali: $\alpha = \Delta\theta \times \frac{180}{\pi}$ con risoluzione al decimo ($0,1^\circ$).

### 4.6 Visualizzazione Frameless nel Terzo Superiore e Architettura Doppio Tamburo 3D
- **Misurazione Frameless nel Terzo Superiore**:
  - Il valore di misurazione (`mainReadout` in $\text{cm}$ / $\text{cm}^2$ / $\emptyset\text{ cm}$ / $\text{gradi}$ e `subReadout` in $\text{mm}$ / $\text{rad}$) è posizionato direttamente al centro orizzontale nel terzo superiore dello schermo senza alcun riquadro, card o bordo contenitore.
  - Questa impostazione a filo continuo garantisce che la scala fisica sul bordo sinistro ($0..48\text{ dp}$) rimanga interamente libera da occlusioni grafiche e che lo sfondo del Canvas metrologico resti visibile.
  - La formattazione impiega obbligatoriamente cifre tabulari (`fontFeatureSettings = "tnum"`) per prevenire jitter orizzontale.
- **Doppio Selettore a Tamburo Cilindrico 3D (`RotaryDrumPicker`)**:
  1. **Tamburo Superiore Funzionalità**:
     - Posizionato al centro dello schermo, direttamente sotto l'indicazione numerica di misura.
     - Consente di commutare verticalmente tra le 5 funzionalità del regolo: `Righello`, `Calibro`, `Area 2D`, `Anelli e Ø`, `Goniometro` (e le 4 modalità livella in modalità clinometro: `Auto`, `Bolla 2D`, `Clinometro`, `Ibrido`).
  2. **Tamburo Inferiore Strumentazione & Impostazioni**:
     - Posizionato al centro in basso (sostitutivo della barra pill), consente di ruotare tra le 4 macro-aree: `Regolo`, `Livella`, `Taratura ISO` e `Impostazioni`.
     - Nel profilo `Impostazioni`, l'utente può attivare/disattivare il feedback aptico, consultare i parametri hardware del pannello e dei sensori inerziali, e resettare le tare.
  - **Fisica Prospettica**: Gli elementi subiscono una rotazione 3D (`rotationX = ±35°`), attenuazione di scala ($0.85 \leftrightarrow 1.0$) e dissolvenza di trasparenza ($\alpha = 0.45 \leftrightarrow 1.0$) con viewport centrale Laser Cyan e feedback aptico a scatto.

---

## 5. Taratura Micrometrica ISO/IEC 7810
Lo standard internazionale ISO/IEC 7810 definisce le dimensioni nominali delle carte formato ID-1 (bancomat, carte di credito, patenti di guida e carte d'identità elettroniche):
- **Larghezza nominale**: $85,60\text{ mm}$ ($8,56\text{ cm}$)
- **Altezza nominale**: $53,98\text{ mm}$ ($5,40\text{ cm}$)
- **Raggio di raccordo angoli**: $3,18\text{ mm}$

### Procedura di Taratura:
1. L'utente posiziona fisicamente una carta conforme sul display.
2. Viene visualizzata una sagoma vettoriale calibrata di $85,60\text{ mm}$.
3. Tramite regolazione micrometrica a step di $0,0005$ ($0,05\%$), l'utente allinea perfettamente il contorno virtuale a quello della carta reale.
4. Il valore viene memorizzato atomicamente in DataStore Preferences con precisione Float a 4 decimali.

---

## 6. Casi Limite Documentati & Soluzioni
- **Cutout e Angoli Curvi del Display**: La scala millimetrica parte dal bordo fisico assoluto impostando `WindowCompat.setDecorFitsSystemWindows(window, false)` e compensando gli insets solo per gli elementi interattivi di comando, non per la graduazione del regolo.
- **Risoluzione Dinamica (Foldable / Display Switch)**: All'evento `onConfigurationChanged` o `onWindowMetricsChanged`, il motore ricalcola immediatamente `xdpi`/`ydpi` e il fattore di downscaling.
- **DPI Frazionari e Valori Irrealistici**: Se `xdpi` o `ydpi` restituiti dal driver sono inferiori a $96$ o superiori a $1000$ (dispositivi buggati), il motore imposta un fallback a `densityDpi` con alert diagnostico.
- **Effetto Calamita Goniometrico (Magnetic Snap su Angoli Cardine)**:
  - Multipli angolari comuni: $0^\circ, 30^\circ, 45^\circ, 60^\circ, 90^\circ, 120^\circ, 135^\circ, 150^\circ, 180^\circ$.
  - Finestra di cattura magnetica: $\pm 2,2^\circ$. Quando l'angolo rientra nella tolleranza, le coordinate del braccio mobile vengono bloccate matematicamente sul valore nominale con feedback aptico istantaneo (`HapticFeedbackConstants.CLOCK_TICK`) ed evidenziazione visiva `PrecisionGreen`.
- **Disattivazione Calamita e Ripristino Goniometro Libero (Stale PointerInput Closure & Haptic Suppression)**:
  - Causa Software: In Jetpack Compose, l'uso di `Modifier.pointerInput(mode)` senza includere `isMagnetEnabled` o senza `rememberUpdatedState` provocava la cattura di una closure stantia del valore booleano iniziale, continuando ad applicare lo snap anche dopo il toggle del pulsante calamita. Inoltre, l'invocazione di tick aptici per grado intero generava una sensazione a scatti non desiderata.
  - Soluzione Metrologica: Adozione di `rememberUpdatedState(isMagnetEnabled)`, inclusione di entrambe le chiavi in `pointerInput(mode, isMagnetEnabled)`, azzeramento reattivo di `isProtractorSnapped` e `lastSnappedAngle = -1f` con aggiornamento immediato della lettura tabulare tramite `LaunchedEffect(mode, isMagnetEnabled)`. Soppressione totale dei tick aptici intermedi quando la calamita è disattivata: il goniometro torna a ruotare in maniera 100% libera, continua e priva di vibrazioni.
- **Isolamento Haptico Sensori Inerziali**: Il listener di allineamento a $0,0^\circ$ dei sensori inerziali (`onAlignmentConfirmed`) deve essere attivo esclusivamente quando la sezione `MainAppTab.LEVEL` è visibile a schermo, per evitare che le vibrazioni della livella scattino mentre l'utente maneggia il dispositivo per misurare oggetti sul Regolo.
