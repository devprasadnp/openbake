# Design System Strategy: The Artisanal Digital Hearth

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Modern Hearth."** 

We are moving away from the cold, clinical layouts of standard e-commerce. Instead, we treat the digital screen as a curated editorial spread. This system balances the rustic, tactile nature of a physical bakery with a "Clean Modern" aesthetic. We achieve this by breaking the rigid grid through **intentional asymmetry**—images of artisanal loaves should break container boundaries, and typography should overlap soft-colored surfaces to create a sense of depth and movement. 

The goal is a "premium-organic" feel: nothing is perfectly sharp or clinical; every corner is softened, every shadow is warm, and the layout feels like it was composed by hand, not a machine.

---

## 2. Colors & Tonal Depth
Our palette is rooted in the "Maillard reaction"—the warm, browning transition of baking dough. We use these tones not just as decoration, but as functional depth.

### The "No-Line" Rule
**Explicit Instruction:** Do not use 1px solid borders to separate sections. Structure must be defined solely through background shifts. For example, a `surface-container-low` (#f9f2ec) hero section should sit directly against a `surface` (#fff8f2) background. The change in tone provides the boundary, keeping the UI feeling airy and "unbound."

### Surface Hierarchy & Nesting
Treat the UI as a series of layered fine papers.
- **Surface (Base):** `#fff8f2` (The foundation).
- **Surface-Container-Lowest:** `#ffffff` (Used for floating cards or elevated interaction points).
- **Surface-Container-High:** `#eee7e1` (Used for "inset" content like sidebars or secondary information).

### The "Glass & Gradient" Rule
To elevate the "Premium" mood, use Glassmorphism for the sticky navbar and floating action headers. Apply `surface` with a 70% opacity and a `backdrop-blur` of 12px. For primary CTAs, avoid flat fills; use a subtle linear gradient from `primary` (#8c4b10) to `primary-container` (#aa6328) at a 135-degree angle to mimic the glow of a warm oven.

---

## 3. Typography: The Editorial Voice
We use a high-contrast pairing to balance heritage and modern utility.

- **Display & Headlines (Playfair Display):** These are our "Artisanal" markers. Use `display-lg` (3.5rem) with tight letter-spacing for hero headlines. Do not be afraid to let a headline overlap an image slightly to create an editorial, magazine-like feel.
- **Body & UI (Nunito/Plus Jakarta Sans):** These are our "Utility" markers. While the brand is warm, the interface must remain highly legible. Use `body-md` (0.875rem) for descriptions and `label-md` (0.75rem) for metadata.
- **The Hierarchy Rule:** Never use more than two serif levels on a single screen. If the title is `headline-lg`, the supporting text must be `body-lg` to provide visual breathing room.

---

## 4. Elevation & Depth
Traditional drop shadows are forbidden. We use **Tonal Layering** and **Ambient Glows**.

- **The Layering Principle:** Depth is achieved by "stacking." A `surface-container-lowest` (#ffffff) card placed on a `surface-container-low` (#f9f2ec) background creates a soft, natural lift without the need for structural lines.
- **Ambient Shadows:** When a card must float (e.g., a product hover state), use a shadow tinted with our primary hue: `rgba(140, 75, 16, 0.06)` with a 32px blur and 8px Y-offset. This mimics natural ambient light in a sunlit bakery.
- **The Ghost Border Fallback:** If accessibility requires a border, use `outline-variant` (#d8c2b4) at **15% opacity**. This creates a "suggestion" of a boundary rather than a hard edge.

---

## 5. Components & Primitive Styling

### Buttons
- **Primary:** Pill-shaped (`rounded-full`). Gradient fill (`primary` to `primary-container`). White text. No border.
- **Secondary:** Pill-shaped. Background: `primary-fixed` (#ffdcc5). Text: `on-primary-fixed` (#301400).
- **Tertiary:** No background. Underlined Playfair Display text for a "boutique" look.

### Cards & Lists
- **Rule:** Forbid divider lines. 
- **Implementation:** Separate items in a list using 16px of vertical white space (`spacing-4`). For cards, use `rounded-md` (1.5rem) and rely on background color shifts to define the card area.
- **Artisan Product Card:** Image should have a `rounded-sm` (0.5rem) radius *inside* a `surface-container-lowest` card to create a nested, framed effect.

### Input Fields
- **Style:** `rounded-xl` (12px). 
- **State:** On focus, the background should shift from `surface-container-highest` (#e8e1dc) to `surface-container-lowest` (#ffffff) with a 2px `primary` ghost border (20% opacity).

### Signature Component: The "Dough" Loader
For loading states, use an organic, morphing shape (a "blob") using the `secondary-container` (#ffab69) color, rather than a standard circular spinner.

---

## 6. Do’s and Don’ts

### Do:
- **Do** use generous white space. If you think there is enough padding, add 8px more.
- **Do** use asymmetrical image layouts. A photo of a baguette should bleed off the right edge of the 1280px container.
- **Do** use `tertiary` (#a03b21) for "Limited Edition" or "Fresh Out of the Oven" tags to create urgency without breaking the warm aesthetic.

### Don’t:
- **Don’t** use pure black (#000000). Always use `on-surface` (#1e1b18) for text to maintain the "Warm" mood.
- **Don’t** use 1px solid borders or horizontal rules (`<hr>`). Use a 4px `surface-variant` gap instead.
- **Don’t** use standard "Material" elevation. If a component feels "pasted on," soften the shadow and increase the blur.