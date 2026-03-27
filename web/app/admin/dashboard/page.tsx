"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuthStore } from "@/store/authStore";
import { formatPrice } from "@/lib/utils";
import api from "@/lib/api";
import type { Order } from "@/types";
import Badge from "@/components/ui/Badge";

interface DashboardData {
  today_orders: number;
  today_revenue: number;
  week_orders: number;
  week_revenue: number;
  month_orders: number;
  month_revenue: number;
  pending_orders: number;
}

export default function AdminDashboardPage() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuthStore();
  const [data, setData] = useState<DashboardData | null>(null);
  const [recentOrders, setRecentOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated || user?.role !== "admin") {
      router.push("/login");
      return;
    }
    Promise.all([
      api.get<DashboardData>("/admin/dashboard"),
      api.get<Order[]>("/admin/orders"),
    ]).then(([dashRes, ordersRes]) => {
      setData(dashRes.data);
      setRecentOrders(ordersRes.data.slice(0, 10));
    }).catch(() => {}).finally(() => setLoading(false));
  }, [isAuthenticated, user, router]);

  if (loading) {
    return (
      <div>
        <h1 className="font-playfair text-2xl font-bold mb-6">Dashboard</h1>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-white rounded-2xl p-6 shadow-sm animate-pulse">
              <div className="h-3 bg-border/30 rounded w-1/2 mb-3" />
              <div className="h-8 bg-border/30 rounded w-1/3" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  const kpiCards = [
    { label: "Today's Orders", value: data?.today_orders ?? 0, color: "text-primary" },
    { label: "Today's Revenue", value: formatPrice(data?.today_revenue ?? 0), color: "text-success" },
    { label: "This Month", value: data?.month_orders ?? 0, color: "text-secondary" },
    { label: "Pending Orders", value: data?.pending_orders ?? 0, color: "text-accent" },
  ];

  return (
    <div>
      <h1 className="font-playfair text-2xl font-bold mb-6">Dashboard</h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {kpiCards.map((card) => (
          <div key={card.label} className="bg-white rounded-2xl p-6 shadow-sm">
            <p className="text-sm text-text-secondary">{card.label}</p>
            <p className={`text-3xl font-bold mt-1 ${card.color}`}>{card.value}</p>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-2xl shadow-sm p-6">
        <h2 className="font-semibold text-lg mb-4">Recent Orders</h2>
        {recentOrders.length === 0 ? (
          <div className="text-center py-12 text-text-secondary">
            <p>No orders yet.</p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-cream">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Order ID</th>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Total</th>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Status</th>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Payment</th>
              </tr>
            </thead>
            <tbody>
              {recentOrders.map((order) => (
                <tr key={order.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3">
                    <Link href={`/admin/orders`} className="text-primary hover:underline">
                      #{order.id.slice(0, 8)}
                    </Link>
                  </td>
                  <td className="px-4 py-3 font-semibold">{formatPrice(order.total)}</td>
                  <td className="px-4 py-3">
                    <Badge variant={order.status === "delivered" ? "success" : order.status === "cancelled" ? "error" : "info"}>
                      {order.status}
                    </Badge>
                  </td>
                  <td className="px-4 py-3">{order.payment_status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
