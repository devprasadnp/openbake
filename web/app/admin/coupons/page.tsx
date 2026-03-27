"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { formatPrice, formatDate } from "@/lib/utils";
import Button from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";
import api from "@/lib/api";
import toast from "react-hot-toast";
import type { Coupon } from "@/types";

interface CouponForm {
  code: string;
  discount_type: "flat" | "percent";
  discount_value: number;
  min_order_value: number;
  max_uses: number;
  valid_from: string;
  valid_until: string;
  is_active: boolean;
}

const emptyCoupon: CouponForm = {
  code: "",
  discount_type: "percent",
  discount_value: 10,
  min_order_value: 0,
  max_uses: 100,
  valid_from: new Date().toISOString().split("T")[0],
  valid_until: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
  is_active: true,
};

export default function AdminCouponsPage() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuthStore();
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<CouponForm>(emptyCoupon);
  const [editId, setEditId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isAuthenticated || user?.role !== "admin") {
      router.push("/login");
      return;
    }
    fetchCoupons();
  }, [isAuthenticated, user, router]);

  const fetchCoupons = async () => {
    try {
      const res = await api.get<Coupon[]>("/admin/coupons");
      setCoupons(res.data);
    } catch {
      // error
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => {
    setForm(emptyCoupon);
    setEditId(null);
    setShowForm(true);
  };

  const openEdit = (c: Coupon) => {
    setForm({
      code: c.code,
      discount_type: c.discount_type,
      discount_value: c.discount_value,
      min_order_value: c.min_order_value,
      max_uses: c.max_uses,
      valid_from: c.valid_from?.split("T")[0] || "",
      valid_until: c.valid_until?.split("T")[0] || "",
      is_active: c.is_active,
    });
    setEditId(c.id);
    setShowForm(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const payload = {
        ...form,
        valid_from: new Date(form.valid_from).toISOString(),
        valid_until: new Date(form.valid_until).toISOString(),
      };
      if (editId) {
        await api.patch(`/admin/coupons/${editId}`, payload);
        toast.success("Coupon updated");
      } else {
        await api.post("/admin/coupons", payload);
        toast.success("Coupon created");
      }
      setShowForm(false);
      fetchCoupons();
    } catch {
      toast.error("Failed to save coupon");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-playfair text-2xl font-bold">Coupons ({coupons.length})</h1>
        <Button onClick={openCreate}>+ Add Coupon</Button>
      </div>

      {/* Create/Edit form */}
      {showForm && (
        <div className="bg-white rounded-2xl shadow-sm p-6 mb-6">
          <h2 className="font-semibold text-lg mb-4">{editId ? "Edit Coupon" : "New Coupon"}</h2>
          <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Code</label>
              <input
                className="w-full border border-border rounded-xl px-3 py-2 text-sm"
                value={form.code}
                onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Discount Type</label>
              <select
                className="w-full border border-border rounded-xl px-3 py-2 text-sm"
                value={form.discount_type}
                onChange={(e) => setForm({ ...form, discount_type: e.target.value as "flat" | "percent" })}
              >
                <option value="percent">Percentage (%)</option>
                <option value="flat">Flat (₹)</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Discount Value</label>
              <input
                type="number"
                className="w-full border border-border rounded-xl px-3 py-2 text-sm"
                value={form.discount_value}
                onChange={(e) => setForm({ ...form, discount_value: Number(e.target.value) })}
                min={1}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Min Order Value</label>
              <input
                type="number"
                className="w-full border border-border rounded-xl px-3 py-2 text-sm"
                value={form.min_order_value}
                onChange={(e) => setForm({ ...form, min_order_value: Number(e.target.value) })}
                min={0}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Max Uses</label>
              <input
                type="number"
                className="w-full border border-border rounded-xl px-3 py-2 text-sm"
                value={form.max_uses}
                onChange={(e) => setForm({ ...form, max_uses: Number(e.target.value) })}
                min={1}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Active</label>
              <select
                className="w-full border border-border rounded-xl px-3 py-2 text-sm"
                value={form.is_active ? "true" : "false"}
                onChange={(e) => setForm({ ...form, is_active: e.target.value === "true" })}
              >
                <option value="true">Yes</option>
                <option value="false">No</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Valid From</label>
              <input
                type="date"
                className="w-full border border-border rounded-xl px-3 py-2 text-sm"
                value={form.valid_from}
                onChange={(e) => setForm({ ...form, valid_from: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Valid Until</label>
              <input
                type="date"
                className="w-full border border-border rounded-xl px-3 py-2 text-sm"
                value={form.valid_until}
                onChange={(e) => setForm({ ...form, valid_until: e.target.value })}
                required
              />
            </div>
            <div className="md:col-span-2 flex gap-3 mt-2">
              <Button type="submit" disabled={submitting}>
                {submitting ? "Saving..." : editId ? "Update" : "Create"}
              </Button>
              <button
                type="button"
                className="px-4 py-2 text-sm rounded-xl border border-border text-text-secondary hover:bg-cream"
                onClick={() => setShowForm(false)}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Coupons table */}
      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-cream">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Code</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Discount</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Min Order</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Usage</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Valid Until</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Status</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="text-center py-12 text-text-secondary">Loading...</td>
              </tr>
            ) : coupons.length === 0 ? (
              <tr>
                <td colSpan={7} className="text-center py-12 text-text-secondary">No coupons yet.</td>
              </tr>
            ) : (
              coupons.map((c) => (
                <tr key={c.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 font-mono font-semibold">{c.code}</td>
                  <td className="px-4 py-3">
                    {c.discount_type === "percent"
                      ? `${c.discount_value}%`
                      : formatPrice(c.discount_value)}
                  </td>
                  <td className="px-4 py-3">{formatPrice(c.min_order_value)}</td>
                  <td className="px-4 py-3">{c.used_count} / {c.max_uses}</td>
                  <td className="px-4 py-3">{formatDate(c.valid_until)}</td>
                  <td className="px-4 py-3">
                    <Badge variant={c.is_active ? "success" : "error"}>
                      {c.is_active ? "Active" : "Inactive"}
                    </Badge>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => openEdit(c)}
                      className="text-xs text-primary hover:underline"
                    >
                      Edit
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
