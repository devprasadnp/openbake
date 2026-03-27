"use client";

import { useEffect, useState } from "react";
import api from "@/lib/api";
import { formatPrice } from "@/lib/utils";

interface DailyPoint { date: string; orders: number; revenue: number; }
interface StatusBreakdown { status: string; count: number; }
interface TypeSplit { type: string; count: number; }
interface TopProduct { name: string; units: number; revenue: number; }
interface PaymentSplit { method: string; count: number; }

interface AnalyticsData {
  daily_trend: DailyPoint[];
  status_breakdown: StatusBreakdown[];
  order_type_split: TypeSplit[];
  top_products: TopProduct[];
  payment_split: PaymentSplit[];
}

const STATUS_COLORS: Record<string, string> = {
  placed: "bg-blue-400",
  accepted: "bg-indigo-400",
  preparing: "bg-yellow-400",
  dispatched: "bg-orange-400",
  delivered: "bg-green-500",
  cancelled: "bg-red-400",
};

function BarChart({ items, colorClass = "bg-primary" }: {
  items: { label: string; value: number }[];
  colorClass?: string;
}) {
  const max = Math.max(...items.map((d) => d.value), 1);
  return (
    <div className="space-y-2">
      {items.map((d, i) => (
        <div key={i} className="flex items-center gap-3">
          <span className="text-xs text-text-secondary w-20 shrink-0 text-right truncate">
            {d.label}
          </span>
          <div className="flex-1 bg-cream rounded-full h-5 overflow-hidden">
            <div
              className={`${colorClass} h-full rounded-full flex items-center justify-end pr-2 transition-all duration-500`}
              style={{ width: `${Math.max(4, (d.value / max) * 100)}%` }}
            >
              <span className="text-xs text-white font-medium">
                {d.value.toLocaleString()}
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function DonutChart({ items }: {
  items: { label: string; value: number }[];
}) {
  const total = items.reduce((s, d) => s + d.value, 0);
  const colors = ["bg-primary", "bg-accent", "bg-success", "bg-error", "bg-indigo-400", "bg-yellow-400"];
  return (
    <div className="space-y-2">
      {items.map((d, i) => {
        const pct = total > 0 ? ((d.value / total) * 100).toFixed(1) : "0";
        return (
          <div key={i} className="flex items-center gap-3">
            <div className={`w-3 h-3 rounded-full ${colors[i % colors.length]} shrink-0`} />
            <span className="text-sm capitalize flex-1">{d.label.replace("_", " ")}</span>
            <span className="text-sm font-semibold text-text-secondary">{pct}%</span>
            <span className="text-sm font-bold">{d.value}</span>
          </div>
        );
      })}
    </div>
  );
}

export default function AdminAnalyticsPage() {
  const [data, setData] = useState<AnalyticsData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<AnalyticsData>("/admin/analytics")
      .then((res) => setData(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div>
        <h1 className="font-playfair text-2xl font-bold mb-6">Analytics</h1>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-white rounded-2xl p-6 shadow-sm animate-pulse h-48" />
          ))}
        </div>
      </div>
    );
  }

  if (!data) return <p className="text-error">Failed to load analytics.</p>;

  const maxRevenue = Math.max(...data.daily_trend.map((d) => d.revenue), 1);
  const maxOrders = Math.max(...data.daily_trend.map((d) => d.orders), 1);
  const totalOrders = data.status_breakdown.reduce((s, x) => s + x.count, 0);
  const totalRevenue = data.daily_trend.reduce((s, d) => s + d.revenue, 0);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-playfair text-2xl font-bold">Analytics</h1>
        <span className="text-sm text-text-secondary">Last 7 days</span>
      </div>

      {/* KPI summary */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {[
          { label: "Total Orders (7d)", value: totalOrders, color: "text-primary" },
          { label: "Revenue (7d, paid)", value: formatPrice(totalRevenue), color: "text-success" },
          { label: "Top Product", value: data.top_products[0]?.name ?? "—", color: "text-accent", small: true },
          { label: "Delivery vs Pickup", value: data.order_type_split.map((t) => `${t.type}: ${t.count}`).join(" / "), color: "text-text-primary", small: true },
        ].map((card) => (
          <div key={card.label} className="bg-white rounded-2xl p-5 shadow-sm">
            <p className="text-xs text-text-secondary mb-1">{card.label}</p>
            <p className={`font-bold ${card.small ? "text-base" : "text-2xl"} ${card.color}`}>
              {card.value}
            </p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Daily Revenue Chart */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="font-semibold text-lg mb-4">Daily Revenue (last 7 days)</h2>
          <div className="space-y-2">
            {data.daily_trend.map((d) => (
              <div key={d.date} className="flex items-center gap-3">
                <span className="text-xs text-text-secondary w-14 shrink-0 text-right">{d.date}</span>
                <div className="flex-1 bg-cream rounded-full h-6 overflow-hidden">
                  <div
                    className="bg-primary h-full rounded-full flex items-center justify-end pr-2 transition-all duration-500"
                    style={{ width: `${Math.max(4, (d.revenue / maxRevenue) * 100)}%` }}
                  >
                    {d.revenue > 0 && (
                      <span className="text-xs text-white font-medium whitespace-nowrap">
                        {formatPrice(d.revenue)}
                      </span>
                    )}
                  </div>
                </div>
                {d.revenue === 0 && (
                  <span className="text-xs text-text-secondary">₹0</span>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Daily Orders Chart */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="font-semibold text-lg mb-4">Daily Orders (last 7 days)</h2>
          <div className="space-y-2">
            {data.daily_trend.map((d) => (
              <div key={d.date} className="flex items-center gap-3">
                <span className="text-xs text-text-secondary w-14 shrink-0 text-right">{d.date}</span>
                <div className="flex-1 bg-cream rounded-full h-6 overflow-hidden">
                  <div
                    className="bg-accent h-full rounded-full flex items-center justify-end pr-2 transition-all duration-500"
                    style={{ width: `${Math.max(4, (d.orders / maxOrders) * 100)}%` }}
                  >
                    {d.orders > 0 && (
                      <span className="text-xs text-white font-medium">{d.orders}</span>
                    )}
                  </div>
                </div>
                {d.orders === 0 && <span className="text-xs text-text-secondary">0</span>}
              </div>
            ))}
          </div>
        </div>

        {/* Orders by status */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="font-semibold text-lg mb-4">Orders by Status</h2>
          {data.status_breakdown.length === 0 ? (
            <p className="text-text-secondary text-sm">No orders yet.</p>
          ) : (
            <div className="space-y-2">
              {data.status_breakdown.map((s) => (
                <div key={s.status} className="flex items-center gap-3">
                  <span className="text-sm capitalize w-20 shrink-0">{s.status}</span>
                  <div className="flex-1 bg-cream rounded-full h-5 overflow-hidden">
                    <div
                      className={`${STATUS_COLORS[s.status] ?? "bg-primary"} h-full rounded-full flex items-center justify-end pr-2 transition-all duration-500`}
                      style={{ width: `${Math.max(4, (s.count / totalOrders) * 100)}%` }}
                    >
                      <span className="text-xs text-white font-medium">{s.count}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Top products */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="font-semibold text-lg mb-4">Top Products by Units Sold</h2>
          {data.top_products.length === 0 ? (
            <p className="text-text-secondary text-sm">No sales data yet.</p>
          ) : (
            <BarChart
              items={data.top_products.map((p) => ({ label: p.name, value: p.units }))}
              colorClass="bg-success"
            />
          )}
        </div>

        {/* Order type split */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="font-semibold text-lg mb-4">Order Type</h2>
          {data.order_type_split.length === 0 ? (
            <p className="text-text-secondary text-sm">No orders yet.</p>
          ) : (
            <DonutChart items={data.order_type_split.map((t) => ({ label: t.type, value: t.count }))} />
          )}
        </div>

        {/* Payment method split */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="font-semibold text-lg mb-4">Payment Methods</h2>
          {data.payment_split.length === 0 ? (
            <p className="text-text-secondary text-sm">No orders yet.</p>
          ) : (
            <DonutChart items={data.payment_split.map((p) => ({ label: p.method, value: p.count }))} />
          )}
        </div>
      </div>
    </div>
  );
}
