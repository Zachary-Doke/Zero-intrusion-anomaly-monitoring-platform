# Design System Strategy: 澄澈智析 (The Lucid Insight)

## 1. Overview & Creative North Star
**The Creative North Star: "The Digital Curator"**
Enterprise anomaly detection often suffers from "data noise." This design system rejects the cluttered, dashboard-heavy aesthetic of traditional security tools in favor of a **High-End Editorial** experience. We treat data as content and anomalies as focal points. 

By utilizing **The Lucid Insight** philosophy, we break the "template" look through intentional white space (breathing room), high-contrast typography scales, and a "Layered Intelligence" approach. Instead of a rigid grid of boxes, we use depth and tonal shifts to guide the user’s eye to what matters most: the outlier.

---

## 2. Colors: Tonal Depth & The "No-Line" Rule
This system moves away from wireframe-style containers. We define space through color temperature and transparency rather than strokes.

### The Palette (Material 3 Logic)
*   **Primary (`#00288e`):** The "Intelligence Blue." Used for decisive actions and identifying confirmed patterns.
*   **Primary Container (`#1e40af`):** A lighter, more vibrant wash for active states and hero highlights.
*   **Surface (`#f7f9fb`):** The base canvas. A cool, crisp grey that reduces eye strain during long-form analysis.
*   **Tertiary/Alert (`#ba1a1a`):** The "Anomaly Rose." Reserved strictly for critical data deviations.

### The "No-Line" Rule
**Prohibit 1px solid borders for sectioning.** Conventional enterprise UIs use lines to separate data; we use **Background Shifts**. 
- To separate a sidebar from a main content area, transition from `surface-container-low` to `surface`. 
- Boundaries are felt through value changes, not seen through strokes.

### Surface Hierarchy & Nesting
Treat the UI as physical layers of fine paper. 
- **Level 0 (Background):** `surface` (#f7f9fb)
- **Level 1 (Main Content Area):** `surface-container-low` (#f2f4f6)
- **Level 2 (Active Cards):** `surface-container-lowest` (#ffffff)
- **The "Glass" Rule:** For floating modals or "quick-view" anomaly details, use `surface` at 80% opacity with a `20px` backdrop-blur. This ensures the data beneath remains visible but blurred, maintaining the user’s mental context.

---

## 3. Typography: Editorial Authority
We use a high-contrast scale to create an "Editorial Intelligence" feel.

*   **Headings (Outfit):** Chosen for its geometric precision. 
    *   **Display-LG (3.5rem):** Use for high-level "Anomaly Count" or "System Health" percentages.
    *   **Headline-SM (1.5rem):** The standard for module titles. Use tight letter-spacing (-0.02em) for a premium, "locked-in" look.
*   **Body (Noto Sans SC / Inter):** 
    *   **Body-MD (0.875rem):** The workhorse for data tables and logs. 
    *   **Label-SM (0.6875rem):** Used for metadata and timestamps. Always in `on-surface-variant` to recede visually.

---

## 4. Elevation & Depth: Tonal Layering
Traditional shadows are too heavy for a "Clean Minimal" aesthetic. We utilize **Ambient Light**.

*   **The Layering Principle:** Depth is achieved by stacking. A `surface-container-lowest` card sitting on a `surface-container-high` background creates a natural lift.
*   **Ambient Shadows:** If a card must float (e.g., a hovered anomaly), use a shadow tinted with the primary color: `box-shadow: 0 12px 32px -4px rgba(30, 64, 175, 0.06)`. This feels like light passing through blue glass rather than a dirty grey smudge.
*   **The "Ghost Border" Fallback:** If a border is required for accessibility in data tables, use `outline-variant` at **15% opacity**. It should be a suggestion of a line, not a boundary.

---

## 5. Components: The Intelligence Kit

### Buttons (The Action Signature)
*   **Primary:** A subtle gradient from `primary` to `primary_container`. Sharp **6px radius**. No border.
*   **Tertiary (Ghost):** Text-only with a subtle `surface-variant` background shift on hover. 
*   **Anomaly Action:** A `tertiary` (Rose Red) button used only for "Dismiss" or "Flag" actions.

### Cards & Lists (The "No Divider" Rule)
*   **Forbid divider lines.** Separate list items using `12px` of vertical white space or by alternating background tones (`surface-container-low` vs `surface-container-lowest`).
*   **Anatomy:** All cards must use the **6px sharp radius**. This conveys precision and technical rigor.

### Input Fields (The Silent Input)
*   Background: `surface-container-high`.
*   Border: None, until focus.
*   Focus State: A 1px "Ghost Border" using `primary` at 40% opacity and a subtle glow.

### Specialized Anomaly Components
*   **Trend Sparklines:** Ultra-thin (1pt) lines using `primary`. When an anomaly is detected, the line transitions into a `tertiary` (Rose Red) gradient.
*   **Status Chips:** Use "Glass" variants. A "Healthy" chip is `primary` at 10% opacity with 100% opaque text. No heavy solid backgrounds for status.

---

## 6. Layout: The Professional Login
The login page is the user's first encounter with the platform's "Intelligence."

*   **Layout:** Perfectly centered, but the login card should be slightly asymmetrical—perhaps a wide left margin for a "Digital Curator" quote or a minimalist system status graphic.
*   **The Card:** Use `surface-container-lowest` (#FFFFFF) with a 6px radius. 
*   **The Signature Touch:** A very subtle, slow-moving background gradient (from `surface` to `primary-fixed-dim` at 5% opacity) to give the page a sense of "life" and active processing.

---

## 7. Do's and Don'ts

### Do:
*   **Do** use white space as a structural element. If a page feels "empty," increase the typography size of the headline rather than adding a box.
*   **Do** use `Outfit` for all numerical data. Its geometric nature makes numbers look like high-precision measurements.
*   **Do** nest containers (Highest inside Lowest) to create natural hierarchy.

### Don't:
*   **Don't** use 100% black (#000000). Use `on-surface` (#191c1e) for all text to maintain the "Lucid" softness.
*   **Don't** use standard 1px borders. If you can't define the space with color, increase the margin.
*   **Don't** use heavy "Drop Shadows." If the element doesn't feel like it's lifting, change the background color of the layer beneath it instead.