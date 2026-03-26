# Design System Strategy: The Artisanal Hearth

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Artisanal Hearth."** 

This isn't a generic e-commerce interface; it is a digital extension of the bakery experience. We move beyond the "template" look by blending high-end editorial typography with a tactile, layered interface. We achieve a "modern and clean" feel not through cold minimalism, but through intentional asymmetry, generous negative space, and organic depth. 

The layout should feel like a premium cookbook—breathable, high-contrast, and deeply appetizing. We break the rigid grid by allowing high-quality food photography to overlap containers and using typography scales that command attention.

---

## 2. Colors & Surface Architecture
Our palette transitions from the warmth of raw flour (`background: #FFF8F2`) to the rich depth of a perfect crust (`primary: #8C4B10`).

### The "No-Line" Rule
To maintain a cozy, inviting atmosphere, **1px solid borders are strictly prohibited** for sectioning. Structural boundaries must be defined through:
- **Tonal Shifts:** Placing a `surface_container_low` card against a `surface` background.
- **Negative Space:** Using the Spacing Scale (specifically `spacing-8` and `spacing-12`) to define content groups.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of fine parchment. Use the surface-container tiers to create organic depth:
- **Base Layer:** `surface` (#FFF8F2) for the main canvas.
- **Sectional Layer:** `surface_container_low` (#F9F2EC) for secondary content areas.
- **Interactive Layer:** `surface_container_lowest` (#FFFFFF) for primary cards and input fields to make them "pop" against the cream base.

### The "Glass & Gradient" Rule
To elevate the "Modern" requirement, floating elements (like Navigation Bars or Filter Chips) should utilize **Glassmorphism**:
- **Fill:** `surface` at 80% opacity.
- **Effect:** 16px-24px Backdrop Blur.
- **Gradients:** Primary CTAs should use a subtle linear gradient from `primary` (#8C4B10) to `primary_container` (#AA6328) at a 135° angle to simulate the golden-hour glow of a bakery window.

---

## 3. Typography: The Editorial Voice
We use a high-contrast pairing to balance "Trustworthy" and "Inviting."

- **Display & Headlines (Playfair Display):** These are our "Artisanal" elements. Use `display-lg` for hero titles with tighter letter spacing (-2%) to create an authoritative, editorial feel. 
- **Body & UI (Nunito):** Our "Modern" workhorse. The rounded terminals of Nunito mirror the softness of dough.
- **Intentional Contrast:** Pair a large `headline-lg` serif title with a small, all-caps `label-md` sans-serif subtitle (letter-spaced at +10%) to achieve a high-end boutique aesthetic.

---

## 4. Elevation & Depth
We eschew traditional "box shadows" in favor of **Tonal Layering**.

- **The Layering Principle:** Softness is achieved by stacking. A `surface_container_lowest` card placed on a `surface_container_high` background creates a natural, shadowless lift that feels sophisticated and clean.
- **Ambient Shadows:** Where a floating effect is mandatory (e.g., a "Quick Add" FAB), use an extra-diffused shadow:
    - **Color:** `on_surface` (#1E1B18) at 6% opacity.
    - **Blur:** 24dp.
    - **Y-Offset:** 8dp.
- **The "Ghost Border" Fallback:** If a container requires more definition for accessibility, use a **Ghost Border**: `outline_variant` at 15% opacity. Never use 100% opaque lines.

---

## 5. Components

### Buttons
- **Primary:** Pill-shaped (`rounded-full`), utilizing the "Golden Crust" gradient. Text is `on_primary` (#FFFFFF) using `title-sm` weight.
- **Secondary:** Transparent fill with a `Ghost Border` and `primary` text.
- **Tertiary:** No background, `primary` text, with a subtle underline appearing only on hover.

### Cards (The "Pastry Box" Card)
- **Shape:** `rounded-DEFAULT` (16dp).
- **Styling:** No borders. Use `surface_container_lowest` for the fill.
- **Imagery:** Product images must use `rounded-sm` (8dp) and should occasionally "break the container" (overflowing the top edge by 16dp) for a dynamic, non-standard look.

### Input Fields
- **Surface:** `surface_container_highest` with `rounded-sm`. 
- **Interaction:** On focus, the background transitions to `surface_container_lowest` with a 2px `primary` ghost-border.

### Selection Controls
- **Checkboxes/Radios:** Use `primary` for selected states. The "Unselected" state should be a subtle `surface_container_highest` fill, avoiding empty high-contrast outlines.

### Cards & Lists: The Divider Ban
**Divider lines are forbidden.** Separate list items using `spacing-4` (1rem) vertical gaps and subtle alternating background tones (`surface` vs `surface_container_low`) to guide the eye without "cutting" the layout.

---

## 6. Do’s and Don’ts

### Do
- **Do** use asymmetrical margins. For example, a hero image might be flush to the right edge while the text is padded 32dp from the left.
- **Do** use "White Space" as a functional element. Let the `background` color breathe.
- **Do** use Material Symbols (Rounded) at a `200` weight to keep the icons light and sophisticated.

### Don’t
- **Don’t** use pure black (#000000) for text. Always use `on_surface` (#1E1B18) to keep the "warmth."
- **Don’t** use harsh shadows. If the shadow is clearly visible as a "dark smudge," it is too heavy.
- **Don’t** use default grid gutters. Vary spacing between sections to create a rhythmic, curated reading experience.