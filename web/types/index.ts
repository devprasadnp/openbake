// OpenBake — shared TypeScript types

export interface User {
  id: string;
  name: string;
  email?: string;
  phone?: string;
  auth_provider: string;
  role: "customer" | "admin";
}

export interface Address {
  id: string;
  label: string;
  full_address: string;
  city: string;
  pincode: string;
  lat?: number;
  lng?: number;
  is_default: boolean;
}

export interface Category {
  id: string;
  name: string;
  image_url?: string;
  is_active: boolean;
}

export interface ProductVariant {
  id: string;
  variant_type: "size" | "flavor";
  value: string;
  extra_price: number;
}

export interface Product {
  id: string;
  category_id: string;
  name: string;
  description?: string;
  price: number;
  images: string[];
  is_available: boolean;
  is_eggless_available: boolean;
  customizable: boolean;
  stock_count: number;
  rating: number;
  variants: ProductVariant[];
}

export interface CartItem {
  product: Product;
  quantity: number;
  customization?: {
    eggless?: boolean;
    size?: string;
    flavor?: string;
    cake_message?: string;
  };
}

export interface OrderItem {
  id: string;
  product_id: string;
  product_name?: string;
  quantity: number;
  unit_price: number;
  customization?: Record<string, unknown>;
}

export type OrderStatus =
  | "placed"
  | "accepted"
  | "preparing"
  | "dispatched"
  | "delivered"
  | "cancelled";

export interface Order {
  id: string;
  user_id: string;
  address_id?: string;
  address?: Address;
  order_type: "delivery" | "pickup";
  status: OrderStatus;
  subtotal: number;
  discount: number;
  delivery_fee: number;
  total: number;
  coupon_code?: string;
  payment_method?: string;
  payment_status: "pending" | "paid" | "failed";
  scheduled_date?: string;
  time_slot?: string;
  special_note?: string;
  created_at: string;
  updated_at: string;
  items: OrderItem[];
}

export interface Coupon {
  id: string;
  code: string;
  discount_type: "flat" | "percent";
  discount_value: number;
  min_order_value: number;
  max_uses: number;
  used_count: number;
  valid_from: string;
  valid_until: string;
  is_active: boolean;
}

export interface Review {
  id: string;
  user_id: string;
  product_id: string;
  order_id: string;
  rating: number;
  comment?: string;
  created_at: string;
}

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
}

export interface WishlistItem {
  id: string;
  product_id: string;
  product: Product;
}
