# Web UI Design Prompt
## Bakery Ordering System — Next.js / React

---

## Design Language

**Theme:** Warm, artisan bakery with a clean modern web aesthetic
**Mood:** Welcoming, premium, appetizing

**Color Palette:**
- Primary: `#C2773A` (caramel brown)
- Secondary: `#F4A261` (amber orange)
- Background: `#FFF8F2` (warm cream)
- White: `#FFFFFF` (cards, navbar)
- Text Primary: `#1A1A1A`
- Text Secondary: `#6B6B6B`
- Border: `#E8DDD4`
- Accent CTA: `#E76F51` (terracotta — buy/checkout buttons)
- Success: `#52B788`
- Error: `#E63946`

**Typography (Google Fonts):**
- Headings: `Playfair Display` — elegant serif for bakery personality
- Body / UI: `Nunito` — friendly and readable
- Code/data: `Inter` if needed

**Spacing:** 8px base unit (8, 16, 24, 32, 48, 64px)

**Borders & Radius:**
- Cards: `rounded-2xl` (16px)
- Buttons: `rounded-full` (pill)
- Inputs: `rounded-xl` (12px)
- Images: `rounded-2xl`

**Shadows:** Soft, warm-toned: `shadow: 0 4px 20px rgba(194, 119, 58, 0.08)`

---

## Layout Principles

- Max content width: `1280px`, centered
- Responsive: mobile (< 640px), tablet (640–1024px), desktop (> 1024px)
- Mobile-first Tailwind approach
- Navbar is sticky with backdrop blur

---

## Page-by-Page Design Guide

---

### 1. Homepage (Customer-facing)

**Navbar:**
- Logo (left): bakery name + icon
- Center links: Home, Menu, About, Contact
- Right: Search icon, Wishlist heart icon, Cart bag icon (with count badge), Login/Avatar button
- Sticky with `backdrop-blur-sm` and light cream background on scroll

**Hero Section:**
- Full-width banner with a warm background gradient (cream to amber)
- Left side: headline "Baked Fresh, Delivered Warm" + subtext + two CTA buttons: "Order Now" (filled, terracotta) + "View Menu" (outlined)
- Right side: large hero image of signature cake or pastry spread
- Soft wave SVG divider at bottom

**Category Pills Section:**
- Horizontal scrollable chip row on mobile; centered flex row on desktop
- Each chip: icon + label (Cakes 🎂, Pastries 🥐, Breads 🍞, Snacks, Beverages ☕)
- Active/selected chip: filled caramel brown

**Bestsellers Section:**
- Section title: "Our Bestsellers" in Playfair Display
- 4-column grid (desktop) / 2-column (tablet) / 1-column (mobile)
- Product Card:
  - Image (aspect-ratio 4:3, rounded top)
  - Category tag (small amber chip)
  - Product name
  - Short description (1 line, truncated)
  - Price + rating stars
  - "Add to Cart" button (full width, filled on hover)

**Special Occasions Banner:**
- 2-column grid: Birthday Cakes | Wedding Cakes
- Each: full-width image with overlay text + "Order Now" button

**How It Works:**
- 3-step section: Browse → Customize → Delivered
- Icons + short description under each step

**Testimonials:**
- Horizontal scroll of review cards
- Star rating, reviewer name, short quote, product name

**Footer:**
- 4-column: Logo+tagline | Quick Links | Contact | Social Icons
- Bottom bar: copyright + payment icons

---

### 2. Menu / Product Listing Page

**Layout:**
- Left sidebar (desktop): category filter, price range slider, rating filter, eggless toggle
- On mobile: filters in a drawer/sheet opened by a "Filter" button
- Right: product grid (3-column desktop, 2 tablet, 1 mobile)

**Top of listing:**
- Breadcrumb: Home > Cakes
- Category title + item count
- Sort dropdown: Popularity, Price Low–High, Rating

**Product Card:** (same as homepage, consistent)

**Pagination or Infinite scroll** at bottom

---

### 3. Product Detail Page

**Layout:** 2-column on desktop (image left, details right); stacked on mobile

**Left (Images):**
- Main large image
- Thumbnail strip below (click to switch)

**Right (Details):**
- Category breadcrumb
- Product name (Playfair Display, 28px)
- Rating: stars + review count (clickable, scrolls to reviews)
- Price (large, caramel brown)
- Description
- **Customization Panel** (card with soft border):
  - Eggless toggle (if available)
  - Size: chip group (0.5kg / 1kg / 2kg) — price updates on selection
  - Flavor: chip group
  - Message on Cake: text area with character counter
- Quantity: `-` `1` `+`
- Two buttons: "Add to Cart" (outlined) | "Buy Now" (filled, terracotta)
- Wishlist heart icon

**Below:**
- Reviews section: overall rating bar chart + individual review cards
- Related Products grid

---

### 4. Cart Page (or Slide-out Drawer)

**Option A — Full page cart:**
- 2-column layout: Cart items (left 2/3) | Order summary (right 1/3)
- Each item row: thumbnail, name, customization, price, quantity control, remove icon
- Coupon input below item list
- Order summary card: subtotal, discount, delivery, total
- "Proceed to Checkout" button — full width, filled

**Option B — Cart Drawer (Slide-in from right):**
- 400px wide panel with overlay
- Same content, scrollable
- Fixed bottom: total + checkout button

---

### 5. Checkout Page

**Multi-step form** (progress bar at top):

**Step 1 — Contact & Address:**
- Pre-filled if logged in
- Saved addresses shown as selectable cards
- "Add new address" expands inline form
- Delivery vs Pickup toggle

**Step 2 — Delivery Time:**
- Date picker (inline calendar or horizontal day scroll)
- Time slot buttons (grid of available slots, disabled if full)

**Step 3 — Payment:**
- Payment method cards (UPI, Card, Wallet, COD) — radio select
- UPI: input field + "Pay via UPI" button
- Card: Stripe-like card input form
- Order summary always visible on right (sticky on desktop)

**Step 4 — Review & Place Order:**
- Full order summary, address, slot, payment
- "Place Order" CTA — large, full width, terracotta

---

### 6. Order Confirmation Page

- Centered layout, white card on cream background
- Lottie/CSS animated green checkmark
- "Your order is confirmed! 🎉"
- Order ID, estimated time
- "Track Order" button + "Continue Shopping" link

---

### 7. Order Tracking Page

- Timeline stepper (horizontal on desktop, vertical on mobile):
  Placed → Accepted → Preparing → Dispatched → Delivered
- Estimated time and current status shown
- Items in order (compact list)
- Address + map placeholder

---

### 8. Order History Page

- Table or card list of past orders
- Each row/card: date, order ID, items count, total, status badge, actions
- Status badges: colored pills (green, amber, blue, red)
- "Reorder" button on delivered orders
- Click row → Order Detail page

---

### 9. Profile / Account Page

- Sidebar navigation (desktop): Profile | Addresses | Wishlist | Reviews | Notifications | Logout
- Profile section: avatar upload, name, email, phone edit
- Addresses: card list with add/edit/delete
- Wishlist: product grid

---

## Admin Panel

### Admin Layout
- **Left sidebar** (fixed, 240px): logo + navigation links
  - Dashboard, Orders, Products, Categories, Coupons, Inventory, Analytics
- **Top bar**: admin name + logout

### Admin Dashboard
- KPI cards row: Today's Orders, Revenue, New Customers, Pending Orders
- Recent Orders table (last 10)
- Top Products bar chart (Chart.js or Recharts)
- Orders by status donut chart

### Admin Orders Page
- Table with columns: Order ID, Customer, Items, Total, Status, Date, Action
- Status filter tabs: All | Pending | Accepted | Preparing | Dispatched | Delivered
- Click row → modal with order details + status update dropdown

### Admin Products Page
- Table: image thumbnail, name, category, price, stock, status toggle
- "Add Product" button → modal or right slide-panel with form
  - Name, category dropdown, description, price, images (drag-drop upload)
  - Customizable toggle → conditional fields (variants)
  - Stock count, active toggle
- Edit / Delete actions per row

### Admin Coupons Page
- Table: code, type, value, min order, uses, valid until, active toggle
- "Create Coupon" button → simple form modal

### Admin Inventory Page
- Filtered view: low-stock items (stock < threshold)
- Update stock inline or via modal

---

## Component Library (Tailwind-based)

```
Button variants: primary | secondary | outline | ghost | danger
Badge: success | warning | error | info | default
Input: text | password | search | textarea | select
Card: product card | order card | stat card
Modal: centered overlay
Drawer: right-side slide-in
Toast: top-right notification stack
Skeleton: product card skeleton | row skeleton
Avatar: image or initials fallback
Stepper: horizontal and vertical
Rating: star display + input
```

---

## Responsive Behavior

| Element | Mobile | Tablet | Desktop |
|---------|--------|--------|---------|
| Navbar | hamburger menu | partial links | full links |
| Product grid | 1 col | 2 col | 3–4 col |
| Cart | full page | full page | drawer or page |
| Checkout | stacked steps | stacked | side-by-side |
| Admin sidebar | hidden (bottom nav) | icons only | full labels |

---

## Micro-interactions & Animations

- Button hover: slight scale `1.02` + shadow increase
- Cart icon: bounce + count badge pop on add
- Product card hover: image zoom (`scale-105`) + shadow lift
- Page transitions: fade-in (`opacity 0 → 1`, 200ms)
- Skeleton shimmer on loading
- Toast notifications: slide in from top-right, auto-dismiss after 3s
- Checkout stepper: smooth step transition with progress bar fill
