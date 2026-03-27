"use client";

import Link from "next/link";
import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { LayoutDashboard, Package, ShoppingCart, Tag, BarChart3, Boxes, LogOut } from "lucide-react";
import { useAuthStore } from "@/store/authStore";

const sidebarLinks = [
  { href: "/admin/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/admin/orders", label: "Orders", icon: ShoppingCart },
  { href: "/admin/products", label: "Products", icon: Package },
  { href: "/admin/coupons", label: "Coupons", icon: Tag },
  { href: "/admin/inventory", label: "Inventory", icon: Boxes },
  { href: "/admin/analytics", label: "Analytics", icon: BarChart3 },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { isAuthenticated, user, initialized } = useAuthStore();

  useEffect(() => {
    if (!initialized) return; // wait for auth state to load
    if (!isAuthenticated || user?.role !== "admin") {
      router.push("/login");
    }
  }, [initialized, isAuthenticated, user, router]);

  // Show blank while auth state loads
  if (!initialized) {
    return (
      <div className="flex h-screen items-center justify-center bg-cream">
        <div className="text-text-secondary text-sm">Loading...</div>
      </div>
    );
  }

  // Not admin — redirect handled by useEffect, render nothing
  if (!isAuthenticated || user?.role !== "admin") return null;

  const handleLogout = () => {
    useAuthStore.getState().logout();
    router.push("/login");
  };

  return (
    <div className="flex h-screen bg-cream">
      {/* Sidebar */}
      <aside className="w-60 bg-white border-r border-border flex flex-col">
        <div className="px-6 py-5 border-b border-border">
          <Link href="/admin/dashboard" className="flex items-center gap-2">
            <span className="text-xl">🥐</span>
            <span className="font-playfair text-lg font-bold text-primary">
              OpenBake Admin
            </span>
          </Link>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1">
          {sidebarLinks.map((link) => {
            const isActive = pathname === link.href;
            const Icon = link.icon;
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                  isActive
                    ? "bg-primary/10 text-primary"
                    : "text-text-secondary hover:bg-cream hover:text-text-primary"
                }`}
              >
                <Icon size={18} />
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="px-3 py-4 border-t border-border">
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-text-secondary hover:text-error transition-colors"
          >
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex-1 overflow-auto">
        <header className="bg-white border-b border-border px-6 py-4 flex items-center justify-between">
          <h2 className="font-semibold text-text-primary">Admin Panel</h2>
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center text-sm font-bold">
              {user?.name?.[0]?.toUpperCase() ?? "A"}
            </div>
            <span className="text-sm">{user?.name ?? "Admin"}</span>
          </div>
        </header>
        <main className="p-6">{children}</main>
      </div>
    </div>
  );
}
