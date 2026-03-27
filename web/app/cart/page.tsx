"use client";

import Link from "next/link";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import Button from "@/components/ui/Button";
import { useCartStore } from "@/store/cartStore";
import { formatPrice } from "@/lib/utils";
import { Trash2, Minus, Plus } from "lucide-react";

export default function CartPage() {
  const { items, removeItem, updateQuantity, subtotal, clearCart } = useCartStore();

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
                className="bg-white rounded-2xl p-4 shadow-sm flex items-center gap-4"
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
            <Link href="/checkout">
              <Button className="w-full mt-6">Proceed to Checkout</Button>
            </Link>
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
