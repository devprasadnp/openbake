"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ShoppingBag, Heart, Search, Menu, X, User, ChefHat } from "lucide-react";
import { useState, useRef, useEffect } from "react";
import { useCartStore } from "@/store/cartStore";
import { useAuthStore } from "@/store/authStore";

const navLinks = [
  { href: "/", label: "Home" },
  { href: "/menu", label: "Menu" },
  { href: "/orders", label: "My Orders" },
  { href: "/about", label: "About" },
];

export default function Navbar() {
  const router = useRouter();
  const [mounted, setMounted] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const searchRef = useRef<HTMLInputElement>(null);
  const totalItems = useCartStore((s) => s.totalItems());
  const { isAuthenticated, user } = useAuthStore();

  useEffect(() => { setMounted(true); }, []);

  useEffect(() => {
    if (searchOpen && searchRef.current) {
      searchRef.current.focus();
    }
  }, [searchOpen]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/menu?search=${encodeURIComponent(searchQuery.trim())}`);
      setSearchOpen(false);
      setSearchQuery("");
    }
  };

  return (
    <nav className="sticky top-0 z-50 bg-white/90 backdrop-blur-md border-b border-border shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2 shrink-0">
            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-primary to-secondary flex items-center justify-center">
              <ChefHat size={18} className="text-white" />
            </div>
            <span className="font-playfair text-xl font-bold text-primary">
              Sri Vinayaka Bakery
            </span>
          </Link>

          {/* Desktop nav links */}
          <div className="hidden md:flex items-center gap-8">
            {navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="text-text-secondary hover:text-primary transition-colors font-medium text-sm"
              >
                {link.label}
              </Link>
            ))}
            {isAuthenticated && user?.role === "admin" && (
              <Link
                href="/admin/dashboard"
                className="text-text-secondary hover:text-primary transition-colors font-medium text-sm"
              >
                Admin
              </Link>
            )}
          </div>

          {/* Right actions */}
          <div className="flex items-center gap-3">
            {/* Search */}
            {searchOpen ? (
              <form onSubmit={handleSearch} className="flex items-center gap-2">
                <input
                  ref={searchRef}
                  type="text"
                  placeholder="Search bakery items..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-40 sm:w-56 border border-border rounded-full px-4 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 bg-cream"
                />
                <button
                  type="button"
                  onClick={() => { setSearchOpen(false); setSearchQuery(""); }}
                  className="text-text-secondary hover:text-primary"
                >
                  <X size={18} />
                </button>
              </form>
            ) : (
              <button
                onClick={() => setSearchOpen(true)}
                className="text-text-secondary hover:text-primary transition-colors p-2 rounded-full hover:bg-cream"
              >
                <Search size={20} />
              </button>
            )}

            <Link
              href="/profile?tab=wishlist"
              className="text-text-secondary hover:text-primary transition-colors p-2 rounded-full hover:bg-cream hidden sm:flex"
            >
              <Heart size={20} />
            </Link>
            <Link
              href="/cart"
              className="relative text-text-secondary hover:text-primary transition-colors p-2 rounded-full hover:bg-cream"
            >
              <ShoppingBag size={20} />
              {mounted && totalItems > 0 && (
                <span className="absolute -top-0.5 -right-0.5 bg-accent text-white text-[10px] w-5 h-5 rounded-full flex items-center justify-center font-bold shadow-sm">
                  {totalItems > 99 ? "99+" : totalItems}
                </span>
              )}
            </Link>

            {isAuthenticated ? (
              <Link
                href="/profile"
                className="flex items-center gap-2 bg-gradient-to-r from-primary to-primary/90 text-white px-4 py-2 rounded-full text-sm font-medium hover:shadow-md transition-all"
              >
                <User size={16} />
                <span className="hidden sm:inline">
                  {user?.name?.split(" ")[0] || "Profile"}
                </span>
              </Link>
            ) : (
              <Link
                href="/login"
                className="bg-gradient-to-r from-primary to-primary/90 text-white px-5 py-2 rounded-full text-sm font-medium hover:shadow-md transition-all"
              >
                Login
              </Link>
            )}

            {/* Mobile hamburger */}
            <button
              className="md:hidden text-text-secondary p-2"
              onClick={() => setMobileOpen(!mobileOpen)}
            >
              {mobileOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {mobileOpen && (
          <div className="md:hidden pb-4 border-t border-border mt-2 pt-4 space-y-1">
            {navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="block py-2.5 px-3 rounded-xl text-text-secondary hover:text-primary hover:bg-cream font-medium transition-colors"
                onClick={() => setMobileOpen(false)}
              >
                {link.label}
              </Link>
            ))}
            <Link
              href="/profile?tab=wishlist"
              className="block py-2.5 px-3 rounded-xl text-text-secondary hover:text-primary hover:bg-cream font-medium transition-colors"
              onClick={() => setMobileOpen(false)}
            >
              Wishlist
            </Link>
            {isAuthenticated && user?.role === "admin" && (
              <Link
                href="/admin/dashboard"
                className="block py-2.5 px-3 rounded-xl text-text-secondary hover:text-primary hover:bg-cream font-medium transition-colors"
                onClick={() => setMobileOpen(false)}
              >
                Admin Panel
              </Link>
            )}
          </div>
        )}
      </div>
    </nav>
  );
}
