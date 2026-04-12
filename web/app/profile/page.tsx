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
import { MapPin, Loader2 } from "lucide-react";
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
  const [newAddr, setNewAddr] = useState({ recipient_name: "", recipient_phone: "", house_number: "", street: "", full_address: "", landmark: "", city: "", state: "", pincode: "", label: "Home" });
  const [newAddrLat, setNewAddrLat] = useState<number | undefined>();
  const [newAddrLng, setNewAddrLng] = useState<number | undefined>();
  const [locating, setLocating] = useState(false);
  const [addrErrors, setAddrErrors] = useState<Record<string, string>>({});

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

  const getLocation = () => {
    if (!navigator.geolocation) { toast.error("Geolocation not supported"); return; }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        try {
          const res = await fetch(`https://nominatim.openstreetmap.org/reverse?lat=${coords.latitude}&lon=${coords.longitude}&format=json&addressdetails=1&zoom=18`, { headers: { "Accept-Language": "en", "User-Agent": "OpenBake/1.0" } });
          const data = await res.json();
          const a = data.address || {};
          const road = a.road || a.suburb || a.neighbourhood || "";
          const hn = a.house_number ? `${a.house_number}, ` : "";
          const area = a.suburb || a.neighbourhood || "";
          setNewAddr((p) => ({
            ...p,
            full_address: `${hn}${road}${area && !road.includes(area) ? ", " + area : ""}`.trim().replace(/^,\s*/, "") || data.display_name?.split(",").slice(0, 3).join(",").trim() || "",
            city: a.city || a.town || a.village || a.county || "",
            state: a.state || "",
            pincode: a.postcode || "",
          }));
          setNewAddrLat(coords.latitude);
          setNewAddrLng(coords.longitude);
          toast.success(`Location detected! (±${Math.round(coords.accuracy)}m)`);
        } catch {
          setNewAddrLat(coords.latitude);
          setNewAddrLng(coords.longitude);
          toast.error("Got coordinates but couldn't fetch address details.");
        } finally { setLocating(false); }
      },
      (err) => {
        setLocating(false);
        toast.error(err.code === 1 ? "Location permission denied. Allow access in browser settings." : err.code === 3 ? "Location timed out. Try again." : "Could not get location.");
      },
      { timeout: 20000, enableHighAccuracy: true, maximumAge: 30000 }
    );
  };

  const addAddress = async () => {
    const errors: Record<string, string> = {};
    if (!newAddr.full_address.trim()) errors.full_address = "Required";
    if (!newAddr.city.trim()) errors.city = "Required";
    if (!newAddr.pincode.trim()) errors.pincode = "Required";
    else if (!/^\d{6}$/.test(newAddr.pincode.trim())) errors.pincode = "Must be 6 digits";
    setAddrErrors(errors);
    if (Object.keys(errors).length > 0) return;

    try {
      const res = await api.post<Address>("/addresses", {
        recipient_name: newAddr.recipient_name?.trim() || null,
        recipient_phone: newAddr.recipient_phone?.trim() || null,
        house_number: newAddr.house_number?.trim() || null,
        street: newAddr.street?.trim() || null,
        full_address: newAddr.full_address.trim(),
        landmark: newAddr.landmark?.trim() || null,
        city: newAddr.city.trim(),
        state: newAddr.state?.trim() || null,
        pincode: newAddr.pincode.trim(),
        label: newAddr.label.trim() || "Home",
        lat: newAddrLat,
        lng: newAddrLng,
        is_default: addresses.length === 0,
      });
      setAddresses([...addresses, res.data]);
      setShowNewAddr(false);
      setNewAddr({ recipient_name: "", recipient_phone: "", house_number: "", street: "", full_address: "", landmark: "", city: "", state: "", pincode: "", label: "Home" });
      setNewAddrLat(undefined);
      setNewAddrLng(undefined);
      setAddrErrors({});
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
                  {user?.profile_image_url ? (
                    <img
                      src={user.profile_image_url.startsWith("http") ? user.profile_image_url : `${process.env.NEXT_PUBLIC_API_URL?.replace("/api", "")}${user.profile_image_url}`}
                      alt="Profile"
                      className="w-16 h-16 rounded-full object-cover"
                    />
                  ) : (
                    <div className="w-16 h-16 rounded-full bg-primary/10 text-primary flex items-center justify-center text-2xl font-bold">
                      {user?.name?.charAt(0) || "?"}
                    </div>
                  )}
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
                    <button
                      type="button"
                      onClick={getLocation}
                      disabled={locating}
                      className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl border-2 border-dashed border-primary/40 text-primary text-sm font-medium hover:bg-primary/5 transition-colors disabled:opacity-50"
                    >
                      {locating ? <Loader2 size={15} className="animate-spin" /> : <MapPin size={15} />}
                      {locating ? "Detecting location..." : "📍 Use My Current Location"}
                    </button>
                    {newAddrLat && newAddrLng && (
                      <div className="flex items-center gap-2 text-xs text-green-600 bg-green-50 px-3 py-2 rounded-lg">
                        <MapPin size={14} />
                        Location set ({newAddrLat.toFixed(4)}, {newAddrLng.toFixed(4)})
                      </div>
                    )}
                    <div>
                      <label className="block text-xs font-medium text-text-primary mb-1">Label</label>
                      <div className="flex gap-2">
                        {["Home", "Work", "Other"].map((l) => (
                          <button
                            key={l}
                            type="button"
                            onClick={() => setNewAddr({ ...newAddr, label: l })}
                            className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
                              newAddr.label === l ? "border-primary bg-primary/10 text-primary" : "border-border text-text-secondary hover:border-primary/30"
                            }`}
                          >
                            {l === "Home" ? "🏠" : l === "Work" ? "💼" : "📍"} {l}
                          </button>
                        ))}
                      </div>
                    </div>
                    <Input id="address" label="Full Address *" value={newAddr.full_address} onChange={(e) => { setNewAddr({ ...newAddr, full_address: e.target.value }); setAddrErrors((p) => { const n = {...p}; delete n.full_address; return n; }); }} placeholder="e.g. Flat 302, Sai Residency, MG Road" error={addrErrors.full_address} />
                    <div className="grid grid-cols-2 gap-3">
                      <Input id="recipient_name" label="Recipient Name" value={newAddr.recipient_name} onChange={(e) => setNewAddr({ ...newAddr, recipient_name: e.target.value })} placeholder="e.g. Ravi Kumar" />
                      <Input id="recipient_phone" label="Recipient Phone" value={newAddr.recipient_phone} onChange={(e) => setNewAddr({ ...newAddr, recipient_phone: e.target.value })} placeholder="9876543210" />
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <Input id="house_number" label="House / Flat No." value={newAddr.house_number} onChange={(e) => setNewAddr({ ...newAddr, house_number: e.target.value })} placeholder="e.g. Flat 302, 2nd Floor" />
                      <Input id="street" label="Street / Colony" value={newAddr.street} onChange={(e) => setNewAddr({ ...newAddr, street: e.target.value })} placeholder="e.g. MG Road, Jubilee Hills" />
                    </div>
                    <Input id="landmark" label="Landmark (optional)" value={newAddr.landmark} onChange={(e) => setNewAddr({ ...newAddr, landmark: e.target.value })} placeholder="Near City Mall, Opposite SBI Bank" />
                    <div className="grid grid-cols-3 gap-3">
                      <Input id="city" label="City *" value={newAddr.city} onChange={(e) => { setNewAddr({ ...newAddr, city: e.target.value }); setAddrErrors((p) => { const n = {...p}; delete n.city; return n; }); }} error={addrErrors.city} />
                      <Input id="state" label="State" value={newAddr.state} onChange={(e) => setNewAddr({ ...newAddr, state: e.target.value })} placeholder="e.g. Karnataka" />
                      <Input id="pincode" label="Pincode *" value={newAddr.pincode} onChange={(e) => { setNewAddr({ ...newAddr, pincode: e.target.value.replace(/\D/g, "").slice(0, 6) }); setAddrErrors((p) => { const n = {...p}; delete n.pincode; return n; }); }} placeholder="560001" error={addrErrors.pincode} />
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
