"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense } from "react";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import { useAuthStore } from "@/store/authStore";
import { useWishlistStore } from "@/store/wishlistStore";
import ProductCard from "@/components/product/ProductCard";
import api from "@/lib/api";
import toast from "react-hot-toast";
import type { Address } from "@/types";

type Tab = "profile" | "addresses" | "wishlist";

export default function ProfilePage() {
  return (
    <Suspense fallback={<div className="flex items-center justify-center min-h-screen">Loading...</div>}>
      <ProfileContent />
    </Suspense>
  );
}

function ProfileContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialTab = (searchParams.get("tab") as Tab) || "profile";
  const { isAuthenticated, user, fetchProfile, logout } = useAuthStore();
  const { items: wishlistItems, fetchWishlist, isLoading: wishlistLoading } = useWishlistStore();
  const [activeTab, setActiveTab] = useState<Tab>(initialTab);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [saving, setSaving] = useState(false);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [showNewAddr, setShowNewAddr] = useState(false);
  const [newAddr, setNewAddr] = useState({ full_address: "", city: "", pincode: "", label: "Home" });

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }
    if (user) {
      setName(user.name);
      setPhone(user.phone || "");
    }
  }, [isAuthenticated, user, router]);

  useEffect(() => {
    if (activeTab === "addresses" && isAuthenticated) {
      api.get<Address[]>("/addresses").then((res) => setAddresses(res.data)).catch(() => {});
    }
    if (activeTab === "wishlist" && isAuthenticated) {
      fetchWishlist();
    }
  }, [activeTab, isAuthenticated, fetchWishlist]);

  const updateProfile = async () => {
    setSaving(true);
    try {
      await api.patch("/profile", { name, phone: phone || null });
      await fetchProfile();
      toast.success("Profile updated");
    } catch {
      toast.error("Failed to update profile");
    } finally {
      setSaving(false);
    }
  };

  const addAddress = async () => {
    try {
      const res = await api.post<Address>("/addresses", newAddr);
      setAddresses([...addresses, res.data]);
      setShowNewAddr(false);
      setNewAddr({ full_address: "", city: "", pincode: "", label: "Home" });
      toast.success("Address added");
    } catch {
      toast.error("Failed to add address");
    }
  };

  const deleteAddress = async (id: string) => {
    try {
      await api.delete(`/addresses/${id}`);
      setAddresses(addresses.filter((a) => a.id !== id));
      toast.success("Address deleted");
    } catch {
      toast.error("Failed to delete address");
    }
  };

  const handleLogout = () => {
    logout();
    router.push("/");
  };

  if (!isAuthenticated) return null;

  const tabs: { key: Tab; label: string }[] = [
    { key: "profile", label: "Profile" },
    { key: "addresses", label: "Addresses" },
    { key: "wishlist", label: "Wishlist" },
  ];

  return (
    <>
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="font-playfair text-2xl font-bold mb-8">My Profile</h1>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <aside>
            <nav className="bg-white rounded-2xl p-4 shadow-sm space-y-1">
              {tabs.map((tab) => (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  className={`w-full text-left px-4 py-2 rounded-xl text-sm transition-colors ${
                    activeTab === tab.key
                      ? "bg-primary/10 text-primary font-semibold"
                      : "hover:bg-cream hover:text-primary"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
              <button
                onClick={handleLogout}
                className="w-full text-left px-4 py-2 rounded-xl text-sm hover:bg-error/5 hover:text-error text-text-secondary transition-colors"
              >
                Logout
              </button>
            </nav>
          </aside>

          <div className="md:col-span-3">
            {activeTab === "profile" && (
              <div className="bg-white rounded-2xl p-6 shadow-sm">
                <div className="flex items-center gap-4 mb-6">
                  <div className="w-16 h-16 rounded-full bg-primary/10 text-primary flex items-center justify-center text-2xl font-bold">
                    {user?.name?.charAt(0) || "?"}
                  </div>
                  <div>
                    <h2 className="font-semibold text-lg">{user?.name}</h2>
                    <p className="text-text-secondary text-sm">{user?.email}</p>
                  </div>
                </div>

                <div className="space-y-4 max-w-md">
                  <Input id="name" label="Full Name" value={name} onChange={(e) => setName(e.target.value)} />
                  <Input id="phone" label="Phone" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="9876543210" />
                  <Button onClick={updateProfile} disabled={saving}>
                    {saving ? "Saving..." : "Save Changes"}
                  </Button>
                </div>
              </div>
            )}

            {activeTab === "addresses" && (
              <div className="bg-white rounded-2xl p-6 shadow-sm">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="font-semibold text-lg">Saved Addresses</h2>
                  <Button size="sm" onClick={() => setShowNewAddr(!showNewAddr)}>
                    {showNewAddr ? "Cancel" : "+ Add Address"}
                  </Button>
                </div>

                {showNewAddr && (
                  <div className="border border-border rounded-xl p-4 mb-4 space-y-3">
                    <Input id="label" label="Label" value={newAddr.label} onChange={(e) => setNewAddr({ ...newAddr, label: e.target.value })} placeholder="Home / Work" />
                    <Input id="address" label="Full Address" value={newAddr.full_address} onChange={(e) => setNewAddr({ ...newAddr, full_address: e.target.value })} />
                    <div className="grid grid-cols-2 gap-3">
                      <Input id="city" label="City" value={newAddr.city} onChange={(e) => setNewAddr({ ...newAddr, city: e.target.value })} />
                      <Input id="pincode" label="Pincode" value={newAddr.pincode} onChange={(e) => setNewAddr({ ...newAddr, pincode: e.target.value })} />
                    </div>
                    <Button size="sm" onClick={addAddress}>Save Address</Button>
                  </div>
                )}

                {addresses.length === 0 ? (
                  <p className="text-text-secondary text-center py-8">No saved addresses yet.</p>
                ) : (
                  <div className="space-y-3">
                    {addresses.map((addr) => (
                      <div key={addr.id} className="flex items-start justify-between p-4 border border-border rounded-xl">
                        <div>
                          <p className="font-medium">{addr.label} {addr.is_default && <span className="text-xs text-primary">(Default)</span>}</p>
                          <p className="text-sm text-text-secondary">{addr.full_address}, {addr.city} - {addr.pincode}</p>
                        </div>
                        <button onClick={() => deleteAddress(addr.id)} className="text-sm text-error hover:underline">Delete</button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === "wishlist" && (
              <div className="bg-white rounded-2xl p-6 shadow-sm">
                <h2 className="font-semibold text-lg mb-4">My Wishlist</h2>
                {wishlistLoading ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
                    {[1, 2, 3].map((i) => (
                      <div key={i} className="bg-cream rounded-2xl overflow-hidden animate-pulse">
                        <div className="aspect-[4/3] bg-border/30" />
                        <div className="p-4 space-y-3">
                          <div className="h-4 bg-border/30 rounded w-3/4" />
                          <div className="h-3 bg-border/30 rounded w-1/2" />
                        </div>
                      </div>
                    ))}
                  </div>
                ) : wishlistItems.length === 0 ? (
                  <div className="text-center py-12">
                    <div className="text-5xl mb-3">💛</div>
                    <p className="text-text-secondary mb-4">Your wishlist is empty</p>
                    <a href="/menu" className="text-primary font-medium hover:underline">Browse products</a>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
                    {wishlistItems.map((wi) => (
                      <ProductCard key={wi.id} product={wi.product} />
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
