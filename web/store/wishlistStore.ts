import { create } from "zustand";
import { persist } from "zustand/middleware";
import toast from "react-hot-toast";
import api from "@/lib/api";
import type { Product } from "@/types";

interface WishlistItem {
  id: string;
  product_id: string;
  product: Product;
}

interface WishlistState {
  items: WishlistItem[];
  isLoading: boolean;
  fetchWishlist: () => Promise<void>;
  addToWishlist: (product: Product) => Promise<void>;
  removeFromWishlist: (productId: string) => Promise<void>;
  isInWishlist: (productId: string) => boolean;
}

export const useWishlistStore = create<WishlistState>((set, get) => ({
  items: [],
  isLoading: false,

  fetchWishlist: async () => {
    set({ isLoading: true });
    try {
      const res = await api.get<WishlistItem[]>("/wishlist");
      set({ items: res.data });
    } catch {
      // User might not be logged in
    } finally {
      set({ isLoading: false });
    }
  },

  addToWishlist: async (product: Product) => {
    try {
      await api.post(`/wishlist/${product.id}`);
      const newItem: WishlistItem = {
        id: product.id,
        product_id: product.id,
        product,
      };
      set({ items: [...get().items, newItem] });
      toast.success(`${product.name} added to wishlist`);
    } catch {
      toast.error("Failed to add to wishlist");
    }
  },

  removeFromWishlist: async (productId: string) => {
    try {
      await api.delete(`/wishlist/${productId}`);
      set({ items: get().items.filter((i) => i.product_id !== productId) });
      toast.success("Removed from wishlist");
    } catch {
      toast.error("Failed to remove from wishlist");
    }
  },

  isInWishlist: (productId: string) => {
    return get().items.some((i) => i.product_id === productId);
  },
}));
