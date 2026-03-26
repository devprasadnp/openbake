# Business Requirements Document (BRD)
## Bakery Ordering System

**Version:** 1.0
**Date:** March 2026
**Status:** Draft

---

## 1. Project Overview

A digital ordering platform for a bakery business that allows customers to browse items, place orders online, and track delivery or pickup — available as a mobile app (Android) and a website.

---

## 2. Problem Statement

Customers currently have no easy way to order bakery items online. Orders are taken manually via phone or walk-in, causing missed sales, long wait times, and no way to track orders. The bakery also has no central system to manage inventory, products, or offers.

---

## 3. Goals

- Let customers order bakery items from anywhere, anytime
- Reduce manual order-taking effort for the bakery team
- Provide the admin full control over products, orders, and offers
- Increase sales through a smooth and attractive ordering experience

---

## 4. Stakeholders

| Role | Responsibility |
|------|---------------|
| Bakery Owner | Business requirements, final approval |
| Admin Staff | Manage orders, products, inventory |
| Customers | Browse and order products |
| Delivery Team | Fulfill delivery orders |
| Dev Team | Build and maintain the platform |

---

## 5. User Stories

### Customer

- As a customer, I want to register and log in so I can place orders
- As a customer, I want to browse bakery items by category so I can find what I like
- As a customer, I want to customize my cake (size, flavor, message) before ordering
- As a customer, I want to add items to cart and apply a coupon
- As a customer, I want to choose delivery or pickup and select a time slot
- As a customer, I want to pay via UPI, card, or wallet
- As a customer, I want to track my order status in real time
- As a customer, I want to see my past orders and reorder easily
- As a customer, I want to rate and review items I have ordered
- As a customer, I want to save items to a wishlist

### Admin

- As an admin, I want to add, edit, or delete bakery products
- As an admin, I want to manage orders — accept, mark as preparing, dispatch
- As an admin, I want to track low-stock items
- As an admin, I want to create and manage discount coupons
- As an admin, I want to see sales analytics and top-selling items

---

## 6. Functional Requirements

### Authentication
- Email/password registration and login
- Google Sign-In
- Phone OTP login
- JWT-based session management

### Product Catalog
- Categories: Cakes, Pastries, Breads, Snacks, Beverages
- Product details: name, price, images, description
- Customization options: eggless toggle, size, flavor, message on cake
- Search and filter by category, price, rating

### Cart & Checkout
- Add/remove items, update quantity
- Apply coupon codes
- Select delivery address or pickup
- Choose delivery date and time slot
- Order summary before payment

### Payments
- UPI, debit/credit card, digital wallets
- Payment confirmation and invoice

### Order Management
- Real-time order status: Placed → Accepted → Preparing → Dispatched → Delivered
- Push notification at each status change
- Order history with reorder option

### Reviews & Wishlist
- Star ratings and text reviews on delivered items
- Save items to a wishlist

### Admin Panel (Web)
- Product CRUD with image upload
- Category management
- Order status management
- Coupon creation and management
- Basic analytics: total sales, orders per day, top items
- Inventory tracking with low-stock alerts

---

## 7. Non-Functional Requirements

| Requirement | Target |
|------------|--------|
| Page/screen load time | Under 2 seconds |
| API response time | Under 500ms |
| Uptime | 99.5% |
| Mobile support | Android 8.0+ |
| Web support | Chrome, Firefox, Safari (last 2 versions) |
| Security | HTTPS, JWT auth, input validation |
| Scalability | Handle up to 1000 concurrent users |

---

## 8. Out of Scope (v1)

- iOS app
- Multi-vendor / multi-bakery support
- AI-based product recommendations
- Subscription / loyalty program
- WhatsApp ordering integration

---

## 9. Assumptions

- The bakery delivers within a defined radius (city-level)
- Payment gateway (Razorpay or Stripe) is available and integrated
- Firebase is used for auth and push notifications
- Admin access is web-only (no mobile admin app)

---

## 10. Success Metrics

- Minimum 100 orders placed in first month
- Cart-to-order conversion rate above 60%
- Average order rating above 4/5
- Admin can manage all orders without any manual coordination
