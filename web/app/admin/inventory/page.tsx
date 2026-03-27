"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Badge from "@/components/ui/Badge";
import api from "@/lib/api";
import toast from "react-hot-toast";
import type { Product } from "@/types";

type Filter = "all" | "low" | "out";

export default function AdminInventoryPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<Filter>("all");
  const [editingStock, setEditingStock] = useState<{ id: string; value: string } | null>(null);

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      const res = await api.get<Product[]>("/admin/products");
      setProducts(res.data);
    } catch {
      toast.error("Failed to load products");
    } finally {
      setLoading(false);
    }
  };

  const saveStock = async (product: Product) => {
    const newStock = parseInt(editingStock?.value ?? "", 10);
    if (isNaN(newStock) || newStock < 0) {
      toast.error("Enter a valid stock number");
      return;
    }
    try {
      await api.patch(`/admin/products/${product.id}`, { stock_count: newStock });
      toast.success("Stock updated");
      setEditingStock(null);
      fetchProducts();
    } catch {
      toast.error("Failed to update stock");
    }
  };

  const filtered = products.filter((p) => {
    if (filter === "low") return p.stock_count > 0 && p.stock_count <= 10;
    if (filter === "out") return p.stock_count === 0;
    return true;
  });

  const lowCount = products.filter((p) => p.stock_count > 0 && p.stock_count <= 10).length;
  const outCount = products.filter((p) => p.stock_count === 0).length;

  const stockColor = (n: number) => {
    if (n === 0) return "text-error font-bold";
    if (n <= 5) return "text-error font-semibold";
    if (n <= 10) return "text-accent font-semibold";
    return "";
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-playfair text-2xl font-bold">Inventory</h1>
      </div>

      {/* Summary chips */}
      <div className="flex gap-3 mb-6 flex-wrap">
        {(
          [
            { id: "all", label: "All Products", count: products.length, cls: "bg-primary/10 text-primary" },
            { id: "low", label: "Low Stock (≤10)", count: lowCount, cls: "bg-accent/15 text-accent" },
            { id: "out", label: "Out of Stock", count: outCount, cls: "bg-error/10 text-error" },
          ] as { id: Filter; label: string; count: number; cls: string }[]
        ).map(({ id, label, count, cls }) => (
          <button
            key={id}
            onClick={() => setFilter(id)}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${cls} ${
              filter === id ? "ring-2 ring-current" : "opacity-70 hover:opacity-100"
            }`}
          >
            {label}: <span className="font-bold">{count}</span>
          </button>
        ))}
      </div>

      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-cream">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Product</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Stock</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Status</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={4} className="text-center py-12 text-text-secondary">Loading…</td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={4} className="text-center py-12 text-text-secondary">No products match this filter.</td>
              </tr>
            ) : (
              filtered.map((p) => (
                <tr
                  key={p.id}
                  className={`border-b border-border last:border-0 ${p.stock_count === 0 ? "bg-error/5" : p.stock_count <= 5 ? "bg-accent/5" : ""}`}
                >
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl overflow-hidden bg-cream shrink-0">
                        {p.images?.[0] ? (
                          <Image src={p.images[0]} alt={p.name} width={40} height={40} className="object-cover w-full h-full" />
                        ) : (
                          <span className="flex items-center justify-center w-full h-full text-base">🧁</span>
                        )}
                      </div>
                      <span className="font-medium">{p.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    {editingStock?.id === p.id ? (
                      <div className="flex items-center gap-1">
                        <input
                          type="number"
                          min="0"
                          className="w-20 border border-border rounded-lg px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                          value={editingStock.value}
                          onChange={(e) => setEditingStock({ id: p.id, value: e.target.value })}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") saveStock(p);
                            if (e.key === "Escape") setEditingStock(null);
                          }}
                          autoFocus
                        />
                        <button onClick={() => saveStock(p)} className="text-xs text-success hover:underline">Save</button>
                        <button onClick={() => setEditingStock(null)} className="text-xs text-text-secondary hover:underline">✕</button>
                      </div>
                    ) : (
                      <button
                        onClick={() => setEditingStock({ id: p.id, value: String(p.stock_count) })}
                        className={`hover:underline ${stockColor(p.stock_count)}`}
                        title="Click to edit stock"
                      >
                        {p.stock_count === 0 ? "Out of stock" : `${p.stock_count} units`}
                      </button>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <Badge variant={p.is_available ? "success" : "error"}>
                      {p.is_available ? "Active" : "Inactive"}
                    </Badge>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => setEditingStock({ id: p.id, value: String(p.stock_count) })}
                      className="text-xs text-primary hover:underline"
                    >
                      Update Stock
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
