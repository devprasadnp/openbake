"use client";

import Image from "next/image";
import Link from "next/link";
import { Star, Plus, Heart, ChefHat } from "lucide-react";
import type { Product } from "@/types";
import { formatPrice } from "@/lib/utils";
import { useCartStore } from "@/store/cartStore";
import { useWishlistStore } from "@/store/wishlistStore";
import { useAuthStore } from "@/store/authStore";
import toast from "react-hot-toast";
import Badge from "@/components/ui/Badge";

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
    <div className="group overflow-hidden rounded-2xl border border-border/80 bg-white shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:border-primary/20 hover:shadow-lg">
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
            <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-surface-muted to-secondary/20">
              <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white/80 shadow-sm">
                <ChefHat size={30} className="text-primary" />
              </div>
            </div>
          )}
          {/* Wishlist heart */}
          <button
            onClick={handleWishlist}
            aria-label={wishlisted ? `Remove ${product.name} from wishlist` : `Add ${product.name} to wishlist`}
            className={`absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full shadow-md transition-all ${
              wishlisted
                ? "bg-error/90 text-white"
                : "bg-white/90 text-text-secondary hover:bg-error/10 hover:text-error"
            }`}
          >
            <Heart size={16} className={wishlisted ? "fill-current" : ""} />
          </button>
          {/* Badges */}
          <div className="absolute left-3 top-3 flex flex-col gap-1.5">
            {product.is_eggless_available && (
              <Badge variant="success" className="border-0 bg-success/90 text-white">
                EGGLESS
              </Badge>
            )}
            {(!product.is_available || product.stock_count <= 0) && (
              <Badge variant="error" className="border-0 bg-error/90 text-white">
                SOLD OUT
              </Badge>
            )}
          </div>
        </div>
      </Link>

      <div className="p-4">
        <Link href={`/menu/${product.id}`}>
          <h3 className="line-clamp-1 text-[15px] font-semibold text-text-primary transition-colors hover:text-primary">
            {product.name}
          </h3>
        </Link>
        {product.description && (
          <p className="text-text-secondary text-xs mt-1 line-clamp-1">
            {product.description}
          </p>
        )}

        <div className="mt-3 flex items-center justify-between">
          <div>
            <p className="text-lg font-bold leading-tight text-primary">
              {formatPrice(product.price)}
            </p>
            <div className="mt-0.5 flex items-center gap-1">
              <Star size={12} className="fill-secondary text-secondary" />
              <span className="text-xs text-text-secondary font-medium">{product.rating.toFixed(1)}</span>
            </div>
          </div>

          <button
            onClick={handleAddToCart}
            disabled={!product.is_available || product.stock_count <= 0}
            aria-label={`Add ${product.name} to cart`}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-secondary to-primary text-white transition-all hover:shadow-md disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Plus size={20} />
          </button>
        </div>
      </div>
    </div>
  );
}
