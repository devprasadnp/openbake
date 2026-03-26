# Mobile App UI Design Prompt
## Bakery Ordering System — Android (Jetpack Compose)

---

## Design Language

**Theme:** Warm, cozy bakery feel with a modern and clean touch
**Mood:** Inviting, delicious, trustworthy

**Color Palette:**
- Primary: `#C2773A` (warm caramel brown)
- Secondary: `#F4A261` (soft amber/orange)
- Background: `#FFF8F2` (warm off-white / cream)
- Surface: `#FFFFFF` (card backgrounds)
- Text Primary: `#1A1A1A`
- Text Secondary: `#6B6B6B`
- Accent: `#E76F51` (rust/terracotta for CTAs)
- Success: `#52B788` (green for order confirmed)
- Error: `#E63946` (red for alerts)

**Typography:**
- Title / Display: Playfair Display (serif — elegant bakery feel)
- Body / UI: Nunito (rounded, friendly, readable)
- Sizes: H1 28sp, H2 22sp, Body 16sp, Caption 12sp

**Shape:**
- Cards: 16dp corner radius
- Buttons: fully rounded (pill shape, 50dp radius)
- Bottom sheets: 24dp top corner radius
- Images: 12dp corner radius

**Elevation:** Subtle shadows — 2dp for cards, no heavy drop shadows

---

## Screen-by-Screen Design Guide

---

### 1. Splash Screen
- Full-screen cream background `#FFF8F2`
- Centered bakery logo (illustrated icon — e.g., a croissant or cupcake with leaves)
- App name in Playfair Display, caramel brown
- Tagline below in Nunito: *"Fresh. Warm. Delivered."*
- 1.5s display then navigate to onboarding or home

---

### 2. Onboarding (3 slides)
- Full-screen illustrated cards with:
  1. "Freshly Baked Daily" — illustration of bread basket
  2. "Customize Your Order" — cake customization illustration
  3. "Delivered to Your Door" — delivery scooter with bakery bag
- Bottom: dots indicator + "Next" button + "Skip" text link
- Last slide: "Get Started" filled pill button

---

### 3. Login / Register Screen
- Soft cream background
- Logo at top (smaller, centered)
- Tab row: "Login" / "Sign Up" — underlined tab style in caramel brown
- Input fields: outlined, rounded corners, label floats on focus
- "Continue with Google" — white card button with Google icon
- "Continue with Phone" — amber outlined button
- Divider: thin line with "or" text in center
- Login CTA: filled pill button in caramel brown

---

### 4. OTP Verification Screen
- Simple centered layout
- Phone number shown, option to change
- 4 or 6 digit OTP boxes — square outlined inputs
- Countdown timer: "Resend in 00:45"
- "Verify" pill button

---

### 5. Home Screen
- **Top bar:**
  - Left: delivery address with dropdown arrow (tappable)
  - Right: notification bell icon
- **Search bar** — full-width rounded, with search icon inside
- **Banner carousel** — auto-scrolling cards with offers (rounded corners, 200dp height)
- **Category row** — horizontal scroll
  - Each item: round image + label below (Cakes, Pastries, Breads, Snacks, Beverages)
- **"Bestsellers" section** — horizontal scroll product cards
- **"Special Occasion" section** — 2-column grid for birthday/wedding cakes
- **Product Card:**
  - Image (top, rounded corners)
  - Name, price
  - Star rating
  - "+" add to cart floating button (amber circle, bottom-right of card)

---

### 6. Category / Product List Screen
- Top: Back arrow + category title
- Filter chips row: All, Eggless, Under ₹500, Rating 4+
- 2-column product grid
- Each card: image, name, short description, price, rating, add button
- Loading state: shimmer skeleton cards

---

### 7. Product Detail Screen
- Full-width image at top (hero image with a light overlay at bottom)
- White sheet slides up from bottom (bottom sheet style within the screen)
- Product name, price, rating + review count
- Description (expandable "Read more")
- **Customization section** (shown only if product is customizable):
  - Toggle: Eggless (yes/no)
  - Size selector: pill chips (0.5kg, 1kg, 2kg) — selected chip is filled caramel
  - Flavor selector: pill chips
  - Message on cake: text input
- Quantity selector: `-` `1` `+` in a row
- Fixed bottom bar: total price + "Add to Cart" filled button

---

### 8. Cart Screen
- Title: "Your Cart" with item count badge
- Cart items list:
  - Each row: thumbnail, name, customization summary, price, quantity +/- controls
  - Swipe-to-delete gesture
- Coupon section: outlined input with "Apply" button
- Order summary card:
  - Subtotal, Discount, Delivery Fee, Total — each on its own row
- "Proceed to Checkout" pill button — sticky at bottom

---

### 9. Checkout Screen
Stepper at top: Address → Slot → Payment

**Step 1 — Address:**
- List of saved addresses (radio select)
- "Add New Address" option at bottom

**Step 2 — Delivery/Pickup + Time Slot:**
- Toggle: Delivery / Pickup (pill toggle style)
- If Delivery: date picker (horizontal date scroll) + time slot chips (10AM-12PM, 12PM-2PM…)
- If Pickup: just show bakery address + time slot

**Step 3 — Payment:**
- Options list: UPI, Card, Wallets, COD
- Each option with icon and radio button
- For UPI: text input for UPI ID
- "Place Order" — large filled pill button in accent color (rust/terracotta)

---

### 10. Order Confirmation Screen
- Large animated checkmark (Lottie animation — green bouncing circle with tick)
- "Order Placed!" in Playfair Display
- Order ID, estimated delivery time
- Quick summary of items
- Two buttons: "Track Order" and "Continue Shopping"

---

### 11. Order Tracking Screen
- Order ID + date at top
- **Status stepper** (vertical):
  - Placed ✓ → Accepted ✓ → Preparing → Dispatched → Delivered
  - Active step highlighted in caramel, completed in green
- Items in order (mini list)
- Delivery address shown
- "Need help?" button links to support

---

### 12. Order History Screen
- List of past orders
- Each card: order date, item count, total price, status badge (colored chips: green=delivered, amber=preparing, red=cancelled)
- "Reorder" button on delivered orders
- "Rate Order" prompt if not yet reviewed

---

### 13. Profile Screen
- Avatar circle with initials or photo
- Name, email, phone
- Menu list items:
  - My Addresses
  - Wishlist
  - My Reviews
  - Notifications Settings
  - Help & Support
  - Logout
- Each item: icon + label + right arrow

---

### 14. Wishlist Screen
- 2-column grid of saved products
- Same product card design as catalog
- Heart icon filled (to remove)
- "Add to Cart" on each card

---

## Navigation

- **Bottom Navigation Bar:** Home | Categories | Orders | Profile
  - Active icon filled + label, inactive outlined
  - Cart accessible via floating cart icon (top-right of home) or from bottom bar badge
- **FAB (Cart):** Floating cart button, amber circle, shows item count badge

---

## Component Guidelines

- All CTA buttons: pill shape, minimum 48dp height (accessibility)
- Input fields: outlined style, 12dp radius, 56dp height
- Loading states: shimmer skeleton (same shape as actual content)
- Empty states: centered illustration + message + CTA button
- Error states: red outline on input + small error text below
- Toast/Snackbar: short feedback messages (item added, coupon applied)
- Bottom sheets: for filters, address picker, confirmation dialogs
- All icons from Material Symbols (rounded style)

---

## Animation Guidelines

- Screen transitions: fade + slide (300ms)
- Cart add animation: item "flies" to cart icon (spring animation)
- Lottie for: order confirmation, empty cart, loading states
- Skeleton shimmer for list loading
- Smooth scroll + sticky headers on category/product screens
