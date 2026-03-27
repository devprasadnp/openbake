"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { formatPrice } from "@/lib/utils";
import Badge from "@/components/ui/Badge";
import api from "@/lib/api";
import toast from "react-hot-toast";
import type { Product } from "@/types";

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingPrice, setEditingPrice] = useState<{ id: string; value: string } | null>(null);

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      const res = await api.get<Product[]>("/admin/products");
      setProducts(res.data);
    } catch {
      // error
    } finally {
      setLoading(false);
    }
  };

  const toggleAvailability = async (product: Product) => {
    try {
      await api.patch(`/admin/products/${product.id}`, {
        is_available: !product.is_available,
      });
      toast.success(`Product ${!product.is_available ? "enabled" : "disabled"}`);
      fetchProducts();
    } catch {
      toast.error("Failed to update product");
    }
  };

  const deleteProduct = async (id: string) => {
    if (!confirm("Delete this product?")) return;
    try {
      await api.delete(`/admin/products/${id}`);
      toast.success("Product deleted");
      fetchProducts();
    } catch {
      toast.error("Failed to delete product");
    }
  };

  const startEditPrice = (product: Product) => {
    setEditingPrice({ id: product.id, value: String(product.price) });
  };

  const savePrice = async (product: Product) => {
    const newPrice = parseFloat(editingPrice?.value ?? "");
    if (isNaN(newPrice) || newPrice <= 0) {
      toast.error("Enter a valid price");
      return;
    }
    try {
      await api.patch(`/admin/products/${product.id}`, { price: newPrice });
      toast.success("Price updated");
      setEditingPrice(null);
      fetchProducts();
    } catch {
      toast.error("Failed to update price");
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-playfair text-2xl font-bold">Products ({products.length})</h1>
      </div>

      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-cream">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Image</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Name</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Price</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Stock</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Rating</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Status</th>
              <th className="text-left px-4 py-3 font-medium text-text-secondary">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="text-center py-12 text-text-secondary">Loading...</td>
              </tr>
            ) : products.length === 0 ? (
              <tr>
                <td colSpan={7} className="text-center py-12 text-text-secondary">No products.</td>
              </tr>
            ) : (
              products.map((p) => (
                <tr key={p.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3">
                    <div className="w-12 h-12 rounded-xl overflow-hidden bg-cream">
                      {p.images?.[0] ? (
                        <Image src={p.images[0]} alt={p.name} width={48} height={48} className="object-cover w-full h-full" />
                      ) : (
                        <span className="flex items-center justify-center w-full h-full text-lg">🧁</span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 font-medium">{p.name}</td>
                  <td className="px-4 py-3">
                    {editingPrice?.id === p.id ? (
                      <div className="flex items-center gap-1">
                        <span className="text-text-secondary">₹</span>
                        <input
                          type="number"
                          min="0"
                          step="0.01"
                          className="w-20 border border-border rounded-lg px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                          value={editingPrice.value}
                          onChange={(e) => setEditingPrice({ id: p.id, value: e.target.value })}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") savePrice(p);
                            if (e.key === "Escape") setEditingPrice(null);
                          }}
                          autoFocus
                        />
                        <button onClick={() => savePrice(p)} className="text-xs text-success hover:underline">Save</button>
                        <button onClick={() => setEditingPrice(null)} className="text-xs text-text-secondary hover:underline">✕</button>
                      </div>
                    ) : (
                      <button
                        onClick={() => startEditPrice(p)}
                        className="text-left hover:text-primary hover:underline transition-colors"
                        title="Click to edit price"
                      >
                        {formatPrice(p.price)}
                      </button>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span className={p.stock_count <= 5 ? "text-error font-semibold" : ""}>
                      {p.stock_count}
                    </span>
                  </td>
                  <td className="px-4 py-3">⭐ {p.rating.toFixed(1)}</td>
                  <td className="px-4 py-3">
                    <Badge variant={p.is_available ? "success" : "error"}>
                      {p.is_available ? "Active" : "Inactive"}
                    </Badge>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button
                        onClick={() => startEditPrice(p)}
                        className="text-xs text-primary hover:underline"
                      >
                        Edit Price
                      </button>
                      <button
                        onClick={() => toggleAvailability(p)}
                        className="text-xs text-primary hover:underline"
                      >
                        {p.is_available ? "Disable" : "Enable"}
                      </button>
                      <button
                        onClick={() => deleteProduct(p.id)}
                        className="text-xs text-error hover:underline"
                      >
                        Delete
                      </button>
                    </div>
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