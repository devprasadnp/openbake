"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import Button from "@/components/ui/Button";
import { useCartStore } from "@/store/cartStore";
import { formatPrice } from "@/lib/utils";
import { Trash2, Minus, Plus, AlertTriangle } from "lucide-react";
import api from "@/lib/api";
import toast from "react-hot-toast";

export default function CartPage() {
  const { items, removeItem, updateQuantity, subtotal, clearCart } = useCartStore();
  const [stockIssues, setStockIssues] = useState<Record<string, string>>({});
  const [validating, setValidating] = useState(false);

  // Validate cart stock on mount and when items change
  useEffect(() => {
    if (items.length === 0) return;
    const validate = async () => {
      try {
        const res = await api.post<{ valid: boolean; errors: string[]; items: Array<{ product_id: string; product_name: string; stock_count: number; quantity: number }> }>("/cart/validate", {
          items: items.map((i) => ({ product_id: i.product.id, quantity: i.quantity })),
        });
        const issues: Record<string, string> = {};
        if (!res.data.valid && res.data.errors) {
          res.data.errors.forEach((err) => {
            // Try to match product from error message
            const match = err.match(/[''](.+?)['']/);
            const productName = match ? match[1] : "";
            const item = items.find((i) => i.product.name === productName);
            if (item) issues[item.product.id] = err;
          });
        }
        // Also check from validated items for low stock
        if (res.data.items) {
          res.data.items.forEach((vi) => {
            const cartItem = items.find((i) => i.product.id === vi.product_id);
            if (cartItem && vi.stock_count < cartItem.quantity) {
              issues[vi.product_id] = vi.stock_count === 0
                ? `${vi.product_name} is out of stock`
                : `Only ${vi.stock_count} available (you have ${cartItem.quantity})`;
            }
          });
        }
        setStockIssues(issues);
      } catch {
        // Silently fail — stock will be checked at order placement
      }
    };
    validate();
  }, [items]);

  const hasStockIssues = Object.keys(stockIssues).length > 0;

  const handleCheckout = async () => {
    if (hasStockIssues) {
      toast.error("Please fix stock issues before checkout");
      return;
    }
    // Quick revalidate before navigating
    setValidating(true);
    try {
      const res = await api.post<{ valid: boolean; errors: string[] }>("/cart/validate", {
        items: items.map((i) => ({ product_id: i.product.id, quantity: i.quantity })),
      });
      if (!res.data.valid) {
        toast.error(res.data.errors?.[0] || "Some items are no longer available");
        setValidating(false);
        return;
      }
    } catch {
      // Continue anyway — backend will catch it
    }
    setValidating(false);
    window.location.href = "/checkout";
  };

  if (items.length === 0) {
    return (
      <>
        <Navbar />
        <main className="flex-1 flex flex-col items-center justify-center py-20">
          <div className="text-6xl mb-4">🛒</div>
          <h1 className="font-playfair text-2xl font-bold mb-2">Your cart is empty</h1>
          <p className="text-text-secondary mb-6">Add some delicious items to get started!</p>
          <Link href="/menu">
            <Button>Browse Menu</Button>
          </Link>
        </main>
        <Footer />
      </>
    );
  }

  return (
    <>
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="font-playfair text-2xl font-bold mb-8">
          Your Cart ({items.length} {items.length === 1 ? "item" : "items"})
        </h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Cart Items */}
          <div className="lg:col-span-2 space-y-4">
            {items.map((item) => (
              <div
                key={item.product.id}
                className={`bg-white rounded-2xl p-4 shadow-sm flex items-center gap-4 ${
                  stockIssues[item.product.id] ? "ring-2 ring-red-300" : ""
                }`}
              >
                <div className="w-20 h-20 bg-cream rounded-xl flex items-center justify-center text-3xl shrink-0">
                  🧁
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-text-primary truncate">
                    {item.product.name}
                  </h3>
                  <p className="text-primary font-bold">
                    {formatPrice(item.product.price)}
                  </p>
                  {item.product.stock_count <= 0 && (
                    <p className="text-xs text-red-600 font-medium flex items-center gap-1 mt-1">
                      <AlertTriangle size={12} /> Out of stock
                    </p>
                  )}
                  {item.product.stock_count > 0 && item.product.stock_count <= 5 && (
                    <p className="text-xs text-amber-600 font-medium mt-1">
                      Only {item.product.stock_count} left
                    </p>
                  )}
                  {stockIssues[item.product.id] && (
                    <p className="text-xs text-red-600 font-medium flex items-center gap-1 mt-1">
                      <AlertTriangle size={12} /> {stockIssues[item.product.id]}
                    </p>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => updateQuantity(item.product.id, item.quantity - 1)}
                    className="w-8 h-8 rounded-full border border-border flex items-center justify-center hover:bg-cream"
                  >
                    <Minus size={14} />
                  </button>
                  <span className="w-8 text-center font-medium">{item.quantity}</span>
                  <button
                    onClick={() => updateQuantity(item.product.id, item.quantity + 1)}
                    className="w-8 h-8 rounded-full border border-border flex items-center justify-center hover:bg-cream"
                  >
                    <Plus size={14} />
                  </button>
                </div>
                <button
                  onClick={() => removeItem(item.product.id)}
                  className="text-text-secondary hover:text-error transition-colors"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            ))}
          </div>

          {/* Order Summary */}
          <div className="bg-white rounded-2xl p-6 shadow-sm h-fit">
            <h2 className="font-semibold text-lg mb-4">Order Summary</h2>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-text-secondary">Subtotal</span>
                <span>{formatPrice(subtotal())}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-text-secondary">Delivery Fee</span>
                <span>{formatPrice(40)}</span>
              </div>
              <div className="border-t border-border pt-2 mt-2 flex justify-between font-bold text-lg">
                <span>Total</span>
                <span className="text-primary">{formatPrice(subtotal() + 40)}</span>
              </div>
            </div>
            {hasStockIssues && (
              <div className="flex items-center gap-2 text-sm text-red-600 bg-red-50 rounded-xl px-4 py-3 mb-2">
                <AlertTriangle size={16} />
                <span>Some items have stock issues. Please update quantities or remove them.</span>
              </div>
            )}
            <Button
              className="w-full mt-6"
              onClick={handleCheckout}
              disabled={validating || hasStockIssues}
            >
              {validating ? "Checking availability..." : hasStockIssues ? "Fix Stock Issues" : "Proceed to Checkout"}
            </Button>
            <button
              onClick={clearCart}
              className="w-full text-center text-sm text-text-secondary hover:text-error mt-3 transition-colors"
            >
              Clear Cart
            </button>
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
