"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { formatPrice, formatDate } from "@/lib/utils";
import Badge from "@/components/ui/Badge";
import api from "@/lib/api";
import toast from "react-hot-toast";
import type { Order, OrderStatus } from "@/types";
import { MapPin } from "lucide-react";

const STATUS_TABS = ["all", "placed", "accepted", "preparing", "dispatched", "delivered", "cancelled"];

const NEXT_STATUS: Record<string, string> = {
  placed: "accepted",
  accepted: "preparing",
  preparing: "dispatched",
  dispatched: "delivered",
};

const statusVariant: Record<OrderStatus, "success" | "warning" | "error" | "info" | "default"> = {
  placed: "info",
  accepted: "info",
  preparing: "warning",
  dispatched: "warning",
  delivered: "success",
  cancelled: "error",
};

export default function AdminOrdersPage() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuthStore();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("all");

  useEffect(() => {
    if (!isAuthenticated || user?.role !== "admin") {
      router.push("/login");
      return;
    }
    fetchOrders();
  }, [isAuthenticated, user, router]);

  const fetchOrders = async () => {
    try {
      const res = await api.get<Order[]>("/admin/orders");
      setOrders(res.data);
    } catch {
      // error
    } finally {
      setLoading(false);
    }
  };

  const updateStatus = async (orderId: string, newStatus: string) => {
    try {
      await api.patch(`/admin/orders/${orderId}`, { status: newStatus });
      toast.success(`Order updated to ${newStatus}`);
      fetchOrders();
    } catch {
      toast.error("Failed to update order");
    }
  };

  const filtered = activeTab === "all" ? orders : orders.filter((o) => o.status === activeTab);

  return (
    <div>
      <h1 className="font-playfair text-2xl font-bold mb-6">Orders Management</h1>

      <div className="flex gap-2 mb-6 flex-wrap">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 rounded-full text-sm font-medium border transition-all capitalize ${
              activeTab === tab
                ? "bg-primary text-white border-primary"
                : "border-border hover:bg-primary hover:text-white hover:border-primary"
            }`}
          >
            {tab} {tab !== "all" && `(${orders.filter((o) => o.status === tab).length})`}
          </button>
        ))}
      </div>

      <div className="bg-white rounded-2xl shadow-sm overflow-hidden mb-10">
        <table className="w-full text-sm">
          <thead className="bg-cream">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Order ID</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Date</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Type</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Delivery Address</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Items</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Total</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Payment</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Time Slot</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Status</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={10} className="text-center py-12 text-text-secondary">Loading...</td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={10} className="text-center py-12 text-text-secondary">No orders to display.</td>
              </tr>
            ) : (
              filtered.map((order) => (
                <tr
                  key={order.id}
                  className="border-b border-border last:border-0 hover:bg-cream/50 cursor-pointer"
                  onClick={() => router.push(`/admin/orders/${order.id}`)}
                >
                  <td className="px-4 py-3 font-medium">#{order.id.slice(0, 8)}</td>
                  <td className="px-4 py-3 text-text-secondary">{formatDate(order.created_at)}</td>
                  <td className="px-4 py-3">
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${order.order_type === "delivery" ? "bg-blue-50 text-blue-600" : "bg-orange-50 text-orange-600"}`}>
                      {order.order_type}
                    </span>
                  </td>
                  <td className="px-4 py-3 max-w-[200px]">
                    {order.order_type === "delivery" ? (
                      order.address ? (
                        <div className="flex items-start gap-1">
                          <MapPin size={12} className="text-primary mt-0.5 shrink-0" />
                          <span className="text-xs text-text-secondary truncate" title={`${order.address.full_address}, ${order.address.city} - ${order.address.pincode}`}>
                            {order.address.full_address}, {order.address.city}
                          </span>
                        </div>
                      ) : (
                        <span className="text-xs text-text-secondary italic">No address</span>
                      )
                    ) : (
                      <span className="text-xs text-text-secondary">Pickup</span>
                    )}
                  </td>
                  <td className="px-4 py-3">{order.items.length}</td>
                  <td className="px-4 py-3 font-semibold">{formatPrice(order.total)}</td>
                  <td className="px-4 py-3">
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                      order.payment_status === "paid" ? "bg-green-50 text-green-600" :
                      order.payment_status === "failed" ? "bg-red-50 text-red-600" :
                      "bg-yellow-50 text-yellow-600"
                    }`}>
                      {order.payment_status || "pending"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-text-secondary">
                    {order.time_slot || "—"}
                  </td>
                  <td className="px-4 py-3">
                    <Badge variant={statusVariant[order.status]}>{order.status}</Badge>
                  </td>
                  <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                    {NEXT_STATUS[order.status] ? (
                      <button
                        onClick={() => updateStatus(order.id, NEXT_STATUS[order.status])}
                        className="text-primary text-sm font-medium hover:underline"
                      >
                        Mark {NEXT_STATUS[order.status]}
                      </button>
                    ) : (
                      <span className="text-text-secondary text-sm">—</span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Delivery Locations Summary */}
      {!loading && (
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <h2 className="font-playfair text-lg font-bold mb-4 flex items-center gap-2">
            <MapPin size={18} className="text-primary" /> Active Delivery Locations
          </h2>
          {(() => {
            const deliveryOrders = orders.filter(
              (o) => o.order_type === "delivery" && o.address && ["placed", "accepted", "preparing", "dispatched"].includes(o.status)
            );
            if (deliveryOrders.length === 0) {
              return <p className="text-text-secondary text-sm">No active deliveries right now.</p>;
            }
            return (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {deliveryOrders.map((order) => (
                  <div key={order.id} className="border border-border rounded-xl p-4 hover:border-primary transition-colors">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-medium">#{order.id.slice(0, 8)}</span>
                      <Badge variant={statusVariant[order.status]}>{order.status}</Badge>
                    </div>
                    {order.address && (
                      <div className="flex items-start gap-2">
                        <MapPin size={14} className="text-primary mt-0.5 shrink-0" />
                        <div>
                          <p className="text-sm font-medium">{order.address.label}</p>
                          <p className="text-xs text-text-secondary">{order.address.full_address}</p>
                          <p className="text-xs text-text-secondary">{order.address.city} — {order.address.pincode}</p>
                          {order.address.lat && order.address.lng && (
                            <a
                              href={`https://maps.google.com/?q=${order.address.lat},${order.address.lng}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-xs text-primary hover:underline mt-1 inline-block"
                            >
                              Open in Maps →
                            </a>
                          )}
                        </div>
                      </div>
                    )}
                    <p className="text-xs text-text-secondary mt-2">
                      {formatPrice(order.total)} · {order.items.length} item{order.items.length !== 1 ? "s" : ""}
                      {order.time_slot && ` · ${order.time_slot}`}
                    </p>
                  </div>
                ))}
              </div>
            );
          })()}
        </div>
      )}
    </div>
  );
}
