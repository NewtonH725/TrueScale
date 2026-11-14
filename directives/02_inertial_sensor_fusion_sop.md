# SOP-SEN-02: Fusione Sensori Inerziali, Filtraggio EMA e Livella a 5 Assetti

## 1. Obiettivo
Definire le procedure operative, le equazioni cinematiche/trigonometriche e la logica di calibrazione a 5 profili per trasformare i vettori di gravità grezzi dell'IMU dello smartphone in una livella digitale professionale con risoluzione di $0,1^\circ$, immunità al rumore e azzeramento meccanico del camera bump.

---

## 2. Acquisizione Sensoriale e Gerarchia Fallback
1. **Sensore Primario**: `Sensor.TYPE_GRAVITY` a frequenza `SensorManager.SENSOR_DELAY_GAME` (50–100 Hz).
   - Questo sensore virtuale è sintetizzato dal sensor hub hardware Android e sottrae l'accelerazione lineare dei movimenti manuali, isolando il solo vettore gravitazionale $\vec{g} = [g_x, g_y, g_z]^T$.
2. **Sensore Fallback**: `Sensor.TYPE_ACCELEROMETER`.
   - Se `TYPE_GRAVITY` non è disponibile a livello hardware, viene utilizzato l'accelerometro grezzo con frequenza `SENSOR_DELAY_GAME`.

---

## 3. Filtro Passa-Basso Adattivo EMA (Exponential Moving Average)
Per eliminare il micro-tremolio della mano e il rumore termico del silicio MEMS senza introdurre ritardo percettibile (latenza) durante le rotazioni veloci:

$$y_k = \alpha \cdot x_k + (1 - \alpha) \cdot y_{k-1}$$

### Coefficiente Adattivo $\alpha$:
La norma della derivata prima (differenza istantanea dei campioni):
$$\Delta_g = \sqrt{(g_{x,k} - g_{x,k-1})^2 + (g_{y,k} - g_{y,k-1})^2 + (g_{z,k} - g_{z,k-1})^2}$$

- **Stato Stazionario** ($\Delta_g < 0,15\text{ m/s}^2$): $\alpha = 0,06f$ (inchioda il decimo di grado $0,1^\circ$ impedendo il flicker dell'ultima cifra).
- **Stato Dinamico** ($\Delta_g \ge 0,15\text{ m/s}^2$): $\alpha = 0,25f$ (garantisce fluidità a 60/120 Hz e reattività immediata).

---

## 4. Trigonometria degli Assetti e Commutazione Automatica

### 4.1 Assetto Orizzontale — Bolla 2D (Piano d'Appoggio)
Attivo quando la componente verticale della gravità è dominante:
$$|g_z| > 7,5\text{ m/s}^2$$

Equazioni trigonometriche:
$$\text{roll} = \text{atan2}\left(g_x, \sqrt{g_y^2 + g_z^2}\right) \times \frac{180}{\pi}$$
$$\text{pitch} = \text{atan2}\left(g_y, \sqrt{g_x^2 + g_z^2}\right) \times \frac{180}{\pi}$$
$$\text{inclinazione totale} = \sqrt{\text{roll}^2 + \text{pitch}^2}$$

### 4.2 Assetto Verticale — Clinometro / Filo a Piombo
Attivo quando lo smartphone è in posizione eretta o poggia su uno dei suoi 4 bordi perimetrali:
$$|g_z| \le 7,5\text{ m/s}^2$$

- Se $|g_y| \ge |g_x|$ (bordo inferiore o superiore appoggiato):
  $$\theta = \text{atan2}(g_x, g_y) \times \frac{180}{\pi}$$
- Se $|g_x| > |g_y|$ (bordo laterale sinistro o destro appoggiato):
  $$\theta = \text{atan2}(g_y, g_x) \times \frac{180}{\pi}$$

### 4.3 Assetto Ibrido (Righello + Clinometro)
Visualizza simultaneamente il righello millimetrico calibrato sul bordo sinistro e la bolla/inclinometro in overlay centrale con indicatore di pendenza percentuale:
$$\text{Pendenza } [\%] = |\tan(\theta_{\text{rad}})| \times 100$$

---

## 5. Matrice di Tara Indipendente a 5 Profili (Camera Bump Zeroing)
I moderni smartphone presentano moduli fotocamera sporgenti (camera bump) che impediscono l'appoggio complanare sul dorso, oltre a tasti laterali fisici (volume, accensione) che alterano l'inclinazione sui bordi laterali.

True Scale gestisce 5 profili di tara memorizzati in DataStore Preferences:
1. `FLAT_BACK`: Piatto sul dorso (offset pitch & roll).
2. `BOTTOM_EDGE`: Piatto sulla base inferiore (porta USB-C).
3. `TOP_EDGE`: Piatto sulla testa superiore.
4. `LEFT_EDGE`: Piatto sul fianco sinistro.
5. `RIGHT_EDGE`: Piatto sul fianco destro.

### Formula di Compensazione:
$$\theta_{\text{calibrato}} = \theta_{\text{grezzo}} - \theta_{\text{offset\_profilo}}$$

Al comando "Azzera / Calibra Tara", il valore grezzo filtrato corrente viene salvato come offset del profilo attivo. Al ripristino ("Reset Tara"), l'offset torna a $0,0^\circ$.

---

## 6. Feedback Aptico e Blocco di Precisione Expressive
- **Soglia di Perfetta Planarità**: $|\theta_{\text{calibrato}}| \le 0,1^\circ$.
- **Transizione Cromatica**: Il colore del cuneo o della bolla passa istantaneamente da tonalità di allineamento (Surface Variant / Outline) al colore d'accento primario o verde di precisione metrologica.
- **Aptica di Conferma**: All'ingresso nella soglia di $\pm 0,1^\circ$, viene emesso un impulso aptico breve e secco (`HapticFeedbackConstants.CONFIRM`). Durante il movimento tra tacche graduate, viene generato `HapticFeedbackConstants.SEGMENT_FREQUENT_TICK`.

---

## 7. Casi Limite Hardware & Soluzioni
- **Inversione a 180° (Schermo verso il basso)**: Se $g_z < -7,5\text{ m/s}^2$, il calcolo inverte l'orientamento grafico per mantenere la coerenza visiva della bolla.
- **Accelerazioni Esterne Lineari (Auto o Mezzo in movimento)**: Rilevate confrontando la norma euclidea $|\vec{g}| = \sqrt{g_x^2 + g_y^2 + g_z^2}$ con $9,81\text{ m/s}^2$. Se $||\vec{g}| - 9,81| > 2,0\text{ m/s}^2$, viene visualizzato un alert di non stazionarietà.
