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

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => setMounted(true));
    return () => window.cancelAnimationFrame(frame);
  }, []);

  useEffect(() => {
    if (searchOpen && searchRef.current) {
      searchRef.current.focus();
    }
  }, [searchOpen]);

  useEffect(() => {
    if (!mobileOpen) {
      document.body.style.overflow = "";
      return;
    }

    const onEsc = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMobileOpen(false);
      }
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onEsc);

    return () => {
      document.body.style.overflow = "";
      window.removeEventListener("keydown", onEsc);
    };
  }, [mobileOpen]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/menu?search=${encodeURIComponent(searchQuery.trim())}`);
      setSearchOpen(false);
      setSearchQuery("");
    }
  };

  return (
    <>
      <nav className="sticky top-0 z-40 border-b border-border/80 bg-surface/90 backdrop-blur-md shadow-sm">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          {/* Logo */}
          <Link href="/" className="flex shrink-0 items-center gap-2.5 rounded-full pr-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-primary to-secondary shadow-sm">
              <ChefHat size={18} className="text-white" />
            </div>
            <span className="font-playfair text-lg font-bold text-primary sm:text-xl">
              Sri Vinayaka Bakery
            </span>
          </Link>

          {/* Desktop nav links */}
          <div className="hidden items-center gap-7 md:flex">
            {navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="text-sm font-semibold text-text-secondary transition-colors hover:text-primary"
              >
                {link.label}
              </Link>
            ))}
            {isAuthenticated && user?.role === "admin" && (
              <Link
                href="/admin/dashboard"
                className="text-sm font-semibold text-text-secondary transition-colors hover:text-primary"
              >
                Admin
              </Link>
            )}
          </div>

          {/* Right actions */}
          <div className="flex items-center gap-1.5 sm:gap-2">
            {/* Search */}
            {searchOpen ? (
              <form onSubmit={handleSearch} className="flex items-center gap-2" role="search">
                <input
                  ref={searchRef}
                  type="text"
                  placeholder="Search bakery items..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  aria-label="Search bakery items"
                  className="w-36 rounded-full border border-border bg-surface-muted px-4 py-1.5 text-sm focus-visible:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/35 sm:w-56"
                />
                <button
                  type="button"
                  onClick={() => { setSearchOpen(false); setSearchQuery(""); }}
                  className="rounded-full p-2 text-text-secondary transition-colors hover:bg-surface-muted hover:text-primary"
                  aria-label="Close search"
                >
                  <X size={18} />
                </button>
              </form>
            ) : (
              <button
                onClick={() => setSearchOpen(true)}
                className="rounded-full p-2 text-text-secondary transition-colors hover:bg-surface-muted hover:text-primary"
                aria-label="Open search"
              >
                <Search size={20} />
              </button>
            )}

            <Link
              href="/profile?tab=wishlist"
              className="hidden rounded-full p-2 text-text-secondary transition-colors hover:bg-surface-muted hover:text-primary sm:flex"
              aria-label="Open wishlist"
            >
              <Heart size={20} />
            </Link>
            <Link
              href="/cart"
              className="relative rounded-full p-2 text-text-secondary transition-colors hover:bg-surface-muted hover:text-primary"
              aria-label="Open cart"
            >
              <ShoppingBag size={20} />
              {mounted && totalItems > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-accent text-[10px] font-bold text-white shadow-sm">
                  {totalItems > 99 ? "99+" : totalItems}
                </span>
              )}
            </Link>

            {isAuthenticated ? (
              <Link
                href="/profile"
                className="hidden items-center gap-2 rounded-full bg-gradient-to-r from-primary to-primary/90 px-4 py-2 text-sm font-medium text-white shadow-sm transition-all hover:shadow-md sm:flex"
              >
                <User size={16} />
                <span>{user?.name?.split(" ")[0] || "Profile"}</span>
              </Link>
            ) : (
              <Link
                href="/login"
                className="hidden rounded-full bg-gradient-to-r from-primary to-primary/90 px-5 py-2 text-sm font-medium text-white shadow-sm transition-all hover:shadow-md sm:block"
              >
                Login
              </Link>
            )}

            {/* Mobile hamburger */}
            <button
              className="rounded-full p-2 text-text-secondary transition-colors hover:bg-surface-muted hover:text-primary md:hidden"
              onClick={() => setMobileOpen(!mobileOpen)}
              aria-expanded={mobileOpen}
              aria-controls="mobile-menu-panel"
              aria-label={mobileOpen ? "Close menu" : "Open menu"}
            >
              {mobileOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>
      </nav>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 md:hidden" aria-modal="true" role="dialog">
          <button
            className="absolute inset-0 bg-black/30 backdrop-blur-[2px]"
            onClick={() => setMobileOpen(false)}
            aria-label="Close menu"
          />
          <div
            id="mobile-menu-panel"
            className="absolute right-0 top-0 flex h-full w-[min(20rem,88vw)] flex-col border-l border-border bg-surface p-6 shadow-2xl"
          >
            <div className="mb-5 flex items-center justify-between">
              <span className="font-playfair text-xl font-bold text-primary">Menu</span>
              <button
                className="rounded-full p-2 text-text-secondary transition-colors hover:bg-surface-muted hover:text-primary"
                onClick={() => setMobileOpen(false)}
                aria-label="Close menu panel"
              >
                <X size={20} />
              </button>
            </div>

            <div className="space-y-1.5">
              {navLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className="block rounded-xl px-3 py-2.5 font-semibold text-text-secondary transition-colors hover:bg-surface-muted hover:text-primary"
                  onClick={() => setMobileOpen(false)}
                >
                  {link.label}
                </Link>
              ))}
              <Link
                href="/profile?tab=wishlist"
                className="block rounded-xl px-3 py-2.5 font-semibold text-text-secondary transition-colors hover:bg-surface-muted hover:text-primary"
                onClick={() => setMobileOpen(false)}
              >
                Wishlist
              </Link>
              {isAuthenticated && user?.role === "admin" && (
                <Link
                  href="/admin/dashboard"
                  className="block rounded-xl px-3 py-2.5 font-semibold text-text-secondary transition-colors hover:bg-surface-muted hover:text-primary"
                  onClick={() => setMobileOpen(false)}
                >
                  Admin Panel
                </Link>
              )}
            </div>

            <div className="mt-auto border-t border-border pt-4">
              {isAuthenticated ? (
                <Link
                  href="/profile"
                  className="inline-flex w-full items-center justify-center rounded-full bg-gradient-to-r from-primary to-primary/90 px-4 py-2.5 text-sm font-semibold text-white shadow-sm"
                  onClick={() => setMobileOpen(false)}
                >
                  My Profile
                </Link>
              ) : (
                <Link
                  href="/login"
                  className="inline-flex w-full items-center justify-center rounded-full bg-gradient-to-r from-primary to-primary/90 px-4 py-2.5 text-sm font-semibold text-white shadow-sm"
                  onClick={() => setMobileOpen(false)}
                >
                  Login
                </Link>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
