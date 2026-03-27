"use client";

import { useEffect, useRef } from "react";
import { useAuthStore } from "@/store/authStore";
import { useWishlistStore } from "@/store/wishlistStore";

export default function AuthInitializer({ children }: { children: React.ReactNode }) {
  const fetchProfile = useAuthStore((s) => s.fetchProfile);
  const fetchWishlist = useWishlistStore((s) => s.fetchWishlist);
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;
    const token = localStorage.getItem("access_token");
    if (token) {
      fetchProfile();
      fetchWishlist();
    } else {
      // No token — mark store as initialized so auth guards don't wait
      useAuthStore.setState({ initialized: true });
    }
  }, [fetchProfile, fetchWishlist]);

  return <>{children}</>;
}
