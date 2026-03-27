"use client";

import Image from "next/image";
import Link from "next/link";
import { Star, Plus, Heart } from "lucide-react";
import type { Product } from "@/types";
import { formatPrice } from "@/lib/utils";
import { useCartStore } from "@/store/cartStore";
import { useWishlistStore } from "@/store/wishlistStore";
import { useAuthStore } from "@/store/authStore";
import toast from "react-hot-toast";

interface ProductCardProps {
  product: Product;
}

export default function ProductCard({ product }: ProductCardProps) {
  const addItem = useCartStore((s) => s.addItem);
  const { isAuthenticated } = useAuthStore();
  const { isInWishlist, addToWishlist, removeFromWishlist } = useWishlistStore();
  const wishlisted = isInWishlist(product.id);

  const handleWishlist = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isAuthenticated) {
      toast.error("Please login to add to wishlist");
      return;
    }
    if (wishlisted) {
      removeFromWishlist(product.id);
    } else {
      addToWishlist(product);
    }
  };

  const handleAddToCart = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    addItem(product);
  };

  return (
    <div className="group bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-lg transition-all duration-300 border border-transparent hover:border-primary/10">
      <Link href={`/menu/${product.id}`}>
        <div className="relative aspect-[4/3] overflow-hidden">
          {product.images?.[0] ? (
            <Image
              src={product.images[0]}
              alt={product.name}
              fill
              className="object-cover group-hover:scale-105 transition-transform duration-500"
            />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-cream to-secondary/10 flex items-center justify-center text-5xl">
              🧁
            </div>
          )}
          {/* Wishlist heart */}
          <button
            onClick={handleWishlist}
            className={`absolute top-3 right-3 w-9 h-9 rounded-full flex items-center justify-center transition-all shadow-md ${
              wishlisted
                ? "bg-error/90 text-white"
                : "bg-white/90 text-text-secondary hover:bg-error/10 hover:text-error"
            }`}
          >
            <Heart size={16} className={wishlisted ? "fill-current" : ""} />
          </button>
          {/* Badges */}
          <div className="absolute top-3 left-3 flex flex-col gap-1">
            {product.is_eggless_available && (
              <span className="bg-success/90 text-white text-[10px] font-bold px-2 py-0.5 rounded-full">
                EGGLESS
              </span>
            )}
            {!product.is_available && (
              <span className="bg-error/90 text-white text-[10px] font-bold px-2 py-0.5 rounded-full">
                SOLD OUT
              </span>
            )}
          </div>
        </div>
      </Link>

      <div className="p-4">
        <Link href={`/menu/${product.id}`}>
          <h3 className="font-semibold text-text-primary hover:text-primary transition-colors line-clamp-1 text-[15px]">
            {product.name}
          </h3>
        </Link>
        {product.description && (
          <p className="text-text-secondary text-xs mt-1 line-clamp-1">
            {product.description}
          </p>
        )}

        <div className="flex items-center justify-between mt-3">
          <div>
            <p className="font-bold text-primary text-lg leading-tight">
              {formatPrice(product.price)}
            </p>
            <div className="flex items-center gap-1 mt-0.5">
              <Star size={12} className="fill-secondary text-secondary" />
              <span className="text-xs text-text-secondary font-medium">{product.rating.toFixed(1)}</span>
            </div>
          </div>

          <button
            onClick={handleAddToCart}
            disabled={!product.is_available}
            className="w-10 h-10 rounded-full bg-gradient-to-br from-secondary to-primary text-white flex items-center justify-center hover:shadow-md transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Plus size={20} />
          </button>
        </div>
      </div>
    </div>
  );
}
