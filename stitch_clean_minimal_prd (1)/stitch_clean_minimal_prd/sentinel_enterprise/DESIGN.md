# Design System Specification: Exception Monitoring Platform

## 1. Overview & Creative North Star
### The North Star: "The Architectural Observer"
In the high-stakes environment of exception monitoring, the interface must transition from a "tool" to an "environment." This design system departs from the rigid, grid-locked look of standard enterprise dashboards to embrace **Architectural Observation**. 

By utilizing intentional asymmetry, tonal depth, and high-end editorial typography, we create a signature experience that feels calm under pressure. The system prioritizes "breathing room" (negative space) and sophisticated layering to guide the user’s eye to critical failures without inducing cognitive fatigue. We aren't just displaying logs; we are curating technical intelligence.

---

## 2. Colors: Tonal Depth & Soul
The palette is rooted in a refined Material-inspired range, but its application is non-traditional. 

### The "No-Line" Rule
**Explicit Instruction:** Prohibit the use of 1px solid borders for sectioning or layout containment. 
Boundaries must be defined solely through **Background Color Shifts**. For example:
- A `surface-container-low` code block sitting on a `surface` background.
- A `surface-container-highest` navigation rail adjacent to a `surface-bright` main content area.

### Surface Hierarchy & Nesting
Treat the UI as physical layers of stacked fine paper. Use the surface-container tiers to define importance:
- **`surface-container-lowest` (#ffffff):** Reserved for primary interactive cards or active input fields.
- **`surface` (#fcf9f8):** The base canvas of the application.
- **`surface-container-high` (#eae7e7):** Used for persistent sidebars or global navigation to ground the experience.

### The "Glass & Gradient" Rule
To elevate the "tech" aesthetic beyond flat Ant Design defaults:
- **Glassmorphism:** For floating modals or "always-on-top" alerts, use `surface` colors at 80% opacity with a `20px` backdrop-blur.
- **Signature Textures:** Main CTAs (Primary) should utilize a subtle linear gradient from `primary` (#005daa) to `primary_container` (#0075d5) at 135 degrees. This adds a subtle "sheen" that communicates premium reliability.

---

## 3. Typography: Editorial Authority
We use **Inter** not just for legibility, but as a structural element.

| Level | Size | Weight | Use Case |
| :--- | :--- | :--- | :--- |
| **Display LG** | 3.5rem | 700 (Bold) | Hero metrics (e.g., Total Errors). |
| **Headline MD** | 1.75rem | 600 (Semi-Bold) | Section headers with intentional tracking (-0.02em). |
| **Title SM** | 1rem | 500 (Medium) | Card titles and modal headers. |
| **Body MD** | 0.875rem | 400 (Regular) | Primary data tables and log descriptions. |
| **Label SM** | 0.6875rem | 700 (Bold) | Overline text, micro-labels (ALL CAPS). |

**Identity Logic:** Headlines use tight letter-spacing and heavy weights to convey authority, while body text uses generous line-heights (1.6) to ensure complex error logs remain readable.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are largely abandoned in favor of **Tonal Stacking**.

*   **The Layering Principle:** Place a `surface-container-lowest` card on a `surface-container-low` section. The slight delta in hex value creates a "soft lift" that feels integrated into the architecture.
*   **Ambient Shadows:** For floating elements (e.g., Tooltips, Popovers), use a shadow with a blur of `32px`, an offset of `y: 8px`, and a 6% opacity of the `on_surface` color (#1b1c1c).
*   **The Ghost Border Fallback:** If a boundary is required for accessibility, use the `outline_variant` token at **15% opacity**. Never use a 100% opaque border.

---

## 5. Components: Minimalist Refinement

### Buttons & Chips
*   **Primary Button:** Gradient fill (`primary` to `primary_container`), `md` (0.375rem) roundedness, and `on_primary` text.
*   **Action Chips:** Used for error tags (e.g., "Critical," "Resolved"). Use `error_container` with `on_error_container` text. Avoid outlines; use color-fills only.

### Input Fields
*   **The Minimalist Input:** Transparent background with a `surface-container-highest` bottom-border only (2px). On focus, animate to a `primary` color-fill.

### Cards & Lists
*   **Strict Rule:** No divider lines between list items. Use `8px` of vertical white space and a `surface-container-low` hover state to indicate row selection.
*   **Anatomy:** Cards use `xl` (0.75rem) corner radius to soften the technical nature of the data.

### Specialized Component: The Trace Graph
*   For exception monitoring, use a custom "Trace Path" component. Instead of standard lines, use `secondary` colored paths with varying thicknesses to represent data flow volume, utilizing the `glassmorphism` rule for nodes.

---

## 6. Do's and Don'ts

### Do:
*   **Do** use asymmetrical layouts. Place a high-density table next to a wide-margin "Details" pane.
*   **Do** use `tertiary` (#bb0018) sparingly. It is a "Heat Signal"—only for the most severe exceptions.
*   **Do** use `label-sm` in all caps for secondary metadata to create a hierarchy between "Data" and "Descriptor."

### Don't:
*   **Don't** use standard AntD borders. Remove `border-split` and `border-base` CSS variables.
*   **Don't** use pure black (#000000). Use `on_surface` (#1b1c1c) for all primary text to maintain a high-end, ink-on-paper feel.
*   **Don't** crowd the interface. If a screen feels full, increase the `surface` padding rather than adding more containers.