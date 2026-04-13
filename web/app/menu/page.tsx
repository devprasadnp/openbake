"use client";

import { Suspense, useEffect, useState, useCallback, useRef } from "react";
import { useSearchParams } from "next/navigation";
import { SearchX } from "lucide-react";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import ProductCard from "@/components/product/ProductCard";
import Skeleton from "@/components/ui/Skeleton";
import api from "@/lib/api";
import type { Product, Category } from "@/types";

export default function MenuPage() {
  return (
    <Suspense fallback={<div className="flex items-center justify-center min-h-screen">Loading...</div>}>
      <MenuContent />
    </Suspense>
  );
}

function MenuContent() {
  const searchParams = useSearchParams();
  const initialCategory = searchParams.get("category") || "";
  const initialSearch = searchParams.get("search") || "";

  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState(initialCategory);
  const [egglessOnly, setEgglessOnly] = useState(false);
  const [search, setSearch] = useState(initialSearch);
  const [searchInput, setSearchInput] = useState(initialSearch);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [sortBy, setSortBy] = useState("popularity");

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (selectedCategory) params.set("category_id", selectedCategory);
      if (egglessOnly) params.set("eggless_only", "true");
      if (search) params.set("search", search);
      params.set("page_size", "50");

      const res = await api.get<{ items: Product[] }>(`/products?${params.toString()}`);
      const sorted: Product[] = res.data.items ?? res.data;

      if (sortBy === "price_low") sorted.sort((a, b) => a.price - b.price);
      else if (sortBy === "price_high") sorted.sort((a, b) => b.price - a.price);
      else if (sortBy === "rating") sorted.sort((a, b) => b.rating - a.rating);

      setProducts(sorted);
    } catch {
      setProducts([]);
    } finally {
      setLoading(false);
    }
  }, [selectedCategory, egglessOnly, search, sortBy]);

  useEffect(() => {
    api.get<Category[]>("/categories").then((res) => setCategories(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const selectedCategoryName = categories.find((c) => c.id === selectedCategory)?.name || "All Products";

  return (
    <>
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <nav className="text-sm text-text-secondary mb-6">
          <span>Home</span> <span className="mx-2">/</span>{" "}
          <span className="text-text-primary font-medium">Menu</span>
        </nav>

        <div className="flex flex-col lg:flex-row gap-8">
          {/* Sidebar Filters */}
          <aside className="lg:w-64 shrink-0">
            <div className="bg-surface rounded-2xl border border-border/70 p-6 shadow-sm space-y-6">
              {/* Search */}
              <div>
                <input
                  type="text"
                  placeholder="Search products..."
                  value={searchInput}
                  onChange={(e) => {
                    setSearchInput(e.target.value);
                    if (debounceRef.current) clearTimeout(debounceRef.current);
                    debounceRef.current = setTimeout(() => setSearch(e.target.value), 300);
                  }}
                  aria-label="Search products"
                  className="w-full rounded-xl border border-border bg-white px-3 py-2 text-sm shadow-sm focus-visible:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/35"
                />
              </div>

              <div>
                <h3 className="font-semibold text-text-primary mb-3">Categories</h3>
                <ul className="space-y-2 text-sm text-text-secondary">
                  <li>
                    <button
                      onClick={() => setSelectedCategory("")}
                      className={`rounded-lg px-2 py-1 transition-colors hover:text-primary ${!selectedCategory ? "bg-surface-muted text-primary font-semibold" : ""}`}
                    >
                      All
                    </button>
                  </li>
                  {categories.map((c) => (
                    <li key={c.id}>
                      <button
                        onClick={() => setSelectedCategory(c.id)}
                        className={`rounded-lg px-2 py-1 transition-colors hover:text-primary ${selectedCategory === c.id ? "bg-surface-muted text-primary font-semibold" : ""}`}
                      >
                        {c.name}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>

              <div>
                <h3 className="font-semibold text-text-primary mb-3">Filters</h3>
                <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
                  <input
                    type="checkbox"
                    className="rounded"
                    checked={egglessOnly}
                    onChange={(e) => setEgglessOnly(e.target.checked)}
                  />
                  Eggless Only
                </label>
              </div>

              <div>
                <h3 className="font-semibold text-text-primary mb-3">Sort By</h3>
                <select
                  className="w-full rounded-xl border border-border bg-white px-3 py-2 text-sm shadow-sm focus-visible:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/35"
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                >
                  <option value="popularity">Popularity</option>
                  <option value="price_low">Price: Low to High</option>
                  <option value="price_high">Price: High to Low</option>
                  <option value="rating">Rating</option>
                </select>
              </div>
            </div>
          </aside>

          {/* Product Grid */}
          <div className="flex-1">
            <div className="flex items-center justify-between mb-6">
              <h1 className="font-playfair text-2xl font-bold">{selectedCategoryName}</h1>
              <span className="text-sm text-text-secondary">{products.length} items</span>
            </div>

            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
                {[1, 2, 3, 4, 5, 6].map((i) => (
                  <div key={i} className="bg-white rounded-2xl overflow-hidden shadow-sm">
                    <Skeleton className="aspect-[4/3] rounded-none" />
                    <div className="p-4 space-y-3">
                      <Skeleton className="h-4 w-3/4" />
                      <Skeleton className="h-3 w-1/2" />
                      <Skeleton className="h-5 w-1/3" />
                    </div>
                  </div>
                ))}
              </div>
            ) : products.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            ) : (
              <div className="text-center py-16">
                <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-surface-muted text-primary">
                  <SearchX size={28} />
                </div>
                <p className="text-text-secondary">No products found matching your filters.</p>
              </div>
            )}
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
