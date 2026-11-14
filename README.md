# TrueScale 📐 — Suite Metrologica di Precisione Multi-Strumento

[![Android Platform](https://img.shields.io/badge/Platform-Android%2015%2B%20(API%2035%E2%80%9337)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203%20Expressive-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/badge/Release-v1.3.2-FC6000)](https://github.com/NewtonH725/TrueScale/releases/latest)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**TrueScale** trasforma il tuo smartphone in uno strumento di misura di precisione metrologica ad alta frequenza (120 Hz) con azzeramento sub-pixel, calibrazione conforme agli standard internazionali e fusione dei sensori inerziali.

---

## 📥 Download Rapido APK

Puoi scaricare direttamente l'applicazione pre-compilata e installarla su qualsiasi smartphone Android compatibile:

👉 **[Scarica l'Ultima Versione APK (v1.3.2)](https://github.com/NewtonH725/TrueScale/releases/latest/download/TrueScale-v1.3.2.apk)**

Oppure consulta tutte le versioni disponibili nella sezione **[Releases](https://github.com/NewtonH725/TrueScale/releases)**.

---

## ⚡ Caratteristiche Principali

### 1. 📏 Regolo Millimetrico Multi-Modalità (5-in-1)
* **Righello Singolo (1D)**: Cursore continuo con lettura millimetrica istantanea e feedback aptico a ogni scatto di millimetro.
* **Calibro Differenziale**: Doppio cursore parallelo per la misurazione di spessori, gole e $\Delta\text{ mm}$ con visualizzazione simultanea delle quote parziali.
* **Area 2D**: Rettangolo interattivo a coordinate libere per il rilievo in tempo reale di base, altezza e superficie calcolata in $\text{cm}^2$.
* **Anelli & Cilindri (ISO 8653)**: Griglia concentrica per misurare diametri interni ed esterni con conversione automatica nella taglia europea ufficiale per anelli e gioielleria.
* **Goniometro 180°**:
  * Bracci mobili indipendenti per la misura degli angoli da $0,0^\circ$ a $180,0^\circ$ con risoluzione al decimo di grado.
  * **Calamita Magnetica Opzionale**: Interruttore rapido per agganciare istantaneamente gli angoli cardine ($0^\circ, 30^\circ, 45^\circ, 60^\circ, 90^\circ, 120^\circ, 135^\circ, 150^\circ, 180^\circ$) con feedback aptico e blocco visivo.
  * **Modalità Libera**: Disattivando la calamita, il goniometro scorre in modo 100% fluido e continuo senza alcuna resistenza o scatto forzato.

---

### 2. ⚖️ Livella a Bolla 2D & Clinometro Inerziale
* **Bolla Circolare 2D**: Monitoraggio simultaneo dell'inclinazione totale su piano con scomposizione istantanea in *Pitch* e *Roll*.
* **Clinometro Verticale & Ibrido**: Misurazione dell'inclinazione su spigolo con calcolo automatico della pendenza percentuale ($\%$) e indicatore di allineamento perfetto con feedback aptico di centratura.
* **Filtro EMA Adattivo**: Algoritmo proprietario di filtraggio dinamico del rumore: alta reattività ($\alpha = 0.25$) durante i movimenti rapidi e massima stabilizzazione ($\alpha = 0.06$) su micro-oscillazioni per un readout privo di tremolii.
* **Matrice di Tara Indipendente su 5 Facce**: Possibilità di calibrare lo zero relativo separatamente per:
  1. Superficie piana orizzontale
  2. Bordo inferiore
  3. Bordo superiore
  4. Bordo sinistro
  5. Bordo destro

---

### 3. 💳 Calibrazione Sub-Pixel (ISO/IEC 7810 ID-1)
* Bypass completo del downscaling software di sistema e lettura fisica dei valori `xdpi` / `ydpi` del display.
* Procedura guidata su sagoma vettoriale calibrata su carta di credito formato standard ($85,60\text{ mm} \times 53,98\text{ mm}$).
* Regolazione micrometrica fine a step dello $0,05\%$ ($0,0005$) con persistenza su DataStore Preferences.

---

### 4. 🎨 Design Material 3 Expressive & Performance a 120 Hz
* **Zero Allocazioni in DrawScope**: Nessuna creazione di oggetti o allocazione di memoria nei loop di rendering a 120 Hz, garantendo zero dropped frame e massima fluidità grafica.
* **Tipografia Numerica Tabulare (`tnum`)**: Tutti i valori di misura utilizzano font a larghezza fissa per evitare il saltellamento visivo dei decimali durante la misurazione.
* **Temi Cromatici Bull Moose**:
  * *Dark OLED*: Nero carbonio `#141312` con accenti Flame Orange `#FC6000` ad alta visibilità.
  * *Light Mode*: Sfondo pietra ad alto contrasto per utilizzo all'aperto o in officina.
  * *Auto/System*: Adattamento dinamico al tema del sistema operativo.
* **Nuova Adaptive Icon & Themed Icon**: Icona vettoriale metrologica $108\text{ dp} \times 108\text{ dp}$ con supporto nativo alle icone monocromatiche a tema di Android 13–16+.
* **Supporto Bilingue Completo**: Italiano e Inglese con selettore dedicato e rilevamento automatico della lingua di sistema.

---

## 🛠️ Requisiti di Sistema & Compilazione

### Prerequisiti
* Smartphone Android con **Android 15+ (API 35–37)**
* **Java Development Kit (JDK) 17+**
* **Android SDK Build-Tools**

### Compilazione da Sorgente
```bash
# Clona il repository
git clone https://github.com/NewtonH725/TrueScale.git
cd TrueScale

# Esegui le verifiche matematiche e prestazionali deterministiche (DOE)
python3 execution/verify_metrology_math.py
python3 execution/audit_compose_perf.py

# Compila l'APK di Debug
./gradlew assembleDebug

# Installa su dispositivo Android collegato via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Licenza

Rilasciato sotto licenza [MIT](LICENSE). Sviluppato per professionisti, maker e appassionati di misurazioni fisiche ad alta precisione.
