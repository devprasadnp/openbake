"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import Badge from "@/components/ui/Badge";
import Button from "@/components/ui/Button";
import { useAuthStore } from "@/store/authStore";
import { formatPrice, formatDate } from "@/lib/utils";
import api from "@/lib/api";
import { RefreshCw } from "lucide-react";
import type { Order, OrderStatus } from "@/types";

const statusVariant: Record<OrderStatus, "success" | "warning" | "error" | "info" | "default"> = {
  placed: "info",
  accepted: "info",
  preparing: "warning",
  dispatched: "warning",
  delivered: "success",
  cancelled: "error",
};

export default function OrdersPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchOrders = useCallback(async (silent = false) => {
    if (!silent) setRefreshing(true);
    try {
      const res = await api.get<Order[]>("/orders");
      setOrders(res.data);
    } catch {
      // silently ignore
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }
    fetchOrders(true);
  }, [isAuthenticated, router, fetchOrders]);

  return (
    <>
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="font-playfair text-2xl font-bold">My Orders</h1>
          {orders.length > 0 && (
            <button
              onClick={() => fetchOrders()}
              disabled={refreshing}
              className="flex items-center gap-1.5 text-sm text-primary hover:underline disabled:opacity-50"
            >
              <RefreshCw size={15} className={refreshing ? "animate-spin" : ""} />
              {refreshing ? "Refreshing..." : "Refresh"}
            </button>
          )}
        </div>

        {loading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-white rounded-2xl p-6 shadow-sm animate-pulse">
                <div className="h-4 bg-border/30 rounded w-1/4 mb-3" />
                <div className="h-3 bg-border/30 rounded w-1/2 mb-2" />
                <div className="h-3 bg-border/30 rounded w-1/3" />
              </div>
            ))}
          </div>
        ) : orders.length === 0 ? (
          <div className="bg-white rounded-2xl p-12 shadow-sm text-center">
            <div className="text-5xl mb-4">📦</div>
            <h2 className="font-semibold text-lg mb-2">No orders yet</h2>
            <p className="text-text-secondary mb-6">
              Your order history will appear here once you place your first order.
            </p>
            <Link href="/menu"><Button>Browse Menu</Button></Link>
          </div>
        ) : (
          <div className="space-y-4">
            {orders.map((order) => (
              <Link
                key={order.id}
                href={`/orders/${order.id}`}
                className="block bg-white rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow"
              >
                {/* Header row */}
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <p className="font-semibold">Order #{order.id.slice(0, 8).toUpperCase()}</p>
                    <p className="text-xs text-text-secondary mt-0.5">{formatDate(order.created_at)}</p>
                  </div>
                  <Badge variant={statusVariant[order.status]}>
                    {order.status.charAt(0).toUpperCase() + order.status.slice(1)}
                  </Badge>
                </div>

                {/* Items preview */}
                {order.items.length > 0 && (
                  <p className="text-sm text-text-secondary mb-3 line-clamp-1">
                    {order.items
                      .slice(0, 3)
                      .map((it) => it.product_name ?? `Item ×${it.quantity}`)
                      .join(", ")}
                    {order.items.length > 3 && ` +${order.items.length - 3} more`}
                  </p>
                )}

                {/* Metadata chips */}
                <div className="flex flex-wrap gap-2 mb-3 text-xs">
                  <span className="bg-cream px-2 py-1 rounded-full capitalize">
                    {order.order_type === "delivery" ? "🚚 Delivery" : "🏪 Pickup"}
                  </span>
                  {order.payment_method && (
                    <span className="bg-cream px-2 py-1 rounded-full capitalize">
                      💳 {order.payment_method.replace("_", " ")}
                    </span>
                  )}
                  {order.time_slot && (
                    <span className="bg-cream px-2 py-1 rounded-full">
                      🕐 {order.time_slot}
                    </span>
                  )}
                  {order.address && (
                    <span className="bg-cream px-2 py-1 rounded-full line-clamp-1 max-w-[200px]">
                      📍 {order.address.city || order.address.full_address}
                    </span>
                  )}
                </div>

                {/* Footer row */}
                <div className="flex items-center justify-between border-t border-border pt-3">
                  <p className="text-sm text-text-secondary">
                    {order.items.length} {order.items.length === 1 ? "item" : "items"}
                    {order.coupon_code && (
                      <span className="ml-2 text-success font-medium">🏷 {order.coupon_code}</span>
                    )}
                  </p>
                  <p className="font-bold text-primary">{formatPrice(order.total)}</p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
      <Footer />
    </>
  );
}
