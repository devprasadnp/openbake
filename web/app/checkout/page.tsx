"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import { useCartStore } from "@/store/cartStore";
import { useAuthStore } from "@/store/authStore";
import { formatPrice } from "@/lib/utils";
import api from "@/lib/api";
import toast from "react-hot-toast";
import { MapPin, Loader2, Truck, AlertCircle } from "lucide-react";
import type { Address, DeliveryEstimate, RazorpayOrderResponse } from "@/types";

declare global {
  interface Window {
    Razorpay: new (options: Record<string, unknown>) => { open: () => void };
  }
}

const paymentMethods = [
  { id: "upi", label: "UPI", icon: "📱" },
  { id: "card", label: "Card", icon: "💳" },
  { id: "cod", label: "Cash on Delivery", icon: "💵" },
];

const timeSlots = [
  "10:00 AM - 12:00 PM",
  "12:00 PM - 2:00 PM",
  "2:00 PM - 4:00 PM",
  "4:00 PM - 6:00 PM",
  "6:00 PM - 8:00 PM",
];

export default function CheckoutPage() {
  const router = useRouter();
  const { items, subtotal, clearCart } = useCartStore();
  const { isAuthenticated, user } = useAuthStore();

  const [step, setStep] = useState(1);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedAddress, setSelectedAddress] = useState("");
  const [orderType, setOrderType] = useState<"delivery" | "pickup">("delivery");
  const [paymentMethod, setPaymentMethod] = useState("cod");
  const [selectedSlot, setSelectedSlot] = useState(timeSlots[0]);
  const [specialNote, setSpecialNote] = useState("");
  const [couponCode, setCouponCode] = useState("");
  const [discount, setDiscount] = useState(0);
  const [couponMessage, setCouponMessage] = useState("");
  const [placing, setPlacing] = useState(false);
  const [newAddress, setNewAddress] = useState({
    full_address: "",
    landmark: "",
    city: "",
    state: "",
    pincode: "",
    label: "Home",
    phone: "",
    lat: undefined as number | undefined,
    lng: undefined as number | undefined,
  });
  const [showNewAddress, setShowNewAddress] = useState(false);
  const [locating, setLocating] = useState(false);
  const [addrErrors, setAddrErrors] = useState<Record<string, string>>({});

  // ── Delivery estimate state ──────────────────────────────────────────────────
  const [deliveryEstimate, setDeliveryEstimate] = useState<DeliveryEstimate | null>(null);
  const [estimateLoading, setEstimateLoading] = useState(false);

  const deliveryFee =
    orderType === "pickup"
      ? 0
      : deliveryEstimate
        ? deliveryEstimate.delivery_fee
        : 40; // fallback while loading
  const isDeliverable = orderType === "pickup" || !deliveryEstimate || deliveryEstimate.is_deliverable;
  const total = subtotal() - discount + deliveryFee;

  // Fetch delivery estimate when selected address changes
  const fetchDeliveryEstimate = useCallback(async (lat: number, lng: number) => {
    setEstimateLoading(true);
    try {
      const res = await api.get<DeliveryEstimate>("/delivery/estimate", { params: { lat, lng } });
      setDeliveryEstimate(res.data);
    } catch {
      setDeliveryEstimate(null);
    } finally {
      setEstimateLoading(false);
    }
  }, []);

  useEffect(() => {
    if (orderType !== "delivery" || !selectedAddress) {
      setDeliveryEstimate(null);
      return;
    }
    const addr = addresses.find((a) => a.id === selectedAddress);
    if (addr?.lat && addr?.lng) {
      fetchDeliveryEstimate(addr.lat, addr.lng);
    } else {
      setDeliveryEstimate(null);
    }
  }, [selectedAddress, orderType, addresses, fetchDeliveryEstimate]);

  useEffect(() => {
    if (!isAuthenticated) {
      toast.error("Please login to checkout");
      router.push("/login");
      return;
    }
    if (items.length === 0) {
      router.push("/cart");
      return;
    }

    // Validate stock before allowing checkout
    api.post<{ valid: boolean; errors: string[] }>("/cart/validate", {
      items: items.map((i) => ({ product_id: i.product.id, quantity: i.quantity })),
    }).then((res) => {
      if (!res.data.valid) {
        toast.error(res.data.errors?.[0] || "Some items are out of stock");
        router.push("/cart");
      }
    }).catch(() => {});

    api.get<Address[]>("/addresses").then((res) => {
      setAddresses(res.data);
      const defaultAddr = res.data.find((a) => a.is_default);
      if (defaultAddr) setSelectedAddress(defaultAddr.id);
      else if (res.data.length > 0) setSelectedAddress(res.data[0].id);
    }).catch(() => {});
  }, [isAuthenticated, items.length, router]);

  const applyCoupon = async () => {
    if (!couponCode.trim()) return;
    try {
      const res = await api.post("/coupons/apply", { code: couponCode, subtotal: subtotal() });
      if (res.data.valid) {
        setDiscount(res.data.discount);
        setCouponMessage(res.data.message);
        toast.success(res.data.message);
      } else {
        setDiscount(0);
        setCouponMessage(res.data.message);
        toast.error(res.data.message);
      }
    } catch {
      toast.error("Failed to apply coupon");
    }
  };

  const validateAddress = (): boolean => {
    const errors: Record<string, string> = {};
    if (!newAddress.full_address.trim()) errors.full_address = "Address is required";
    else if (newAddress.full_address.trim().length < 10) errors.full_address = "Please enter a complete address";
    if (!newAddress.city.trim()) errors.city = "City is required";
    if (!newAddress.pincode.trim()) errors.pincode = "Pincode is required";
    else if (!/^\d{6}$/.test(newAddress.pincode.trim())) errors.pincode = "Enter a valid 6-digit pincode";
    if (!newAddress.label.trim()) errors.label = "Label is required";
    if (newAddress.phone && !/^[6-9]\d{9}$/.test(newAddress.phone.trim())) errors.phone = "Enter a valid 10-digit mobile number";
    if (!newAddress.lat || !newAddress.lng) errors.location = "Please use 'Current Location' or enter a valid address so we can calculate delivery";

    setAddrErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const addNewAddress = async () => {
    if (!validateAddress()) return;
    try {
      // Combine landmark into full_address for the backend
      const fullAddr = newAddress.landmark
        ? `${newAddress.full_address.trim()}, Near ${newAddress.landmark.trim()}, ${newAddress.state || ""}`.trim().replace(/,\s*$/, "")
        : `${newAddress.full_address.trim()}${newAddress.state ? ", " + newAddress.state : ""}`;
      const res = await api.post<Address>("/addresses", {
        full_address: fullAddr,
        city: newAddress.city.trim(),
        pincode: newAddress.pincode.trim(),
        label: newAddress.label.trim(),
        lat: newAddress.lat,
        lng: newAddress.lng,
        is_default: addresses.length === 0, // first address is default
      });
      setAddresses([...addresses, res.data]);
      setSelectedAddress(res.data.id);
      setShowNewAddress(false);
      setAddrErrors({});
      toast.success("Address added");
    } catch {
      toast.error("Failed to add address");
    }
  };

  const getCurrentLocation = () => {
    if (!navigator.geolocation) {
      toast.error("Geolocation is not supported by your browser");
      return;
    }

    // Check if we're in a secure context (HTTPS or localhost)
    if (window.location.protocol !== "https:" && window.location.hostname !== "localhost" && window.location.hostname !== "127.0.0.1") {
      toast.error("Location requires HTTPS. Try accessing via localhost or enable HTTPS.");
      return;
    }

    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { latitude, longitude, accuracy } = position.coords;
        try {
          const res = await fetch(
            `https://nominatim.openstreetmap.org/reverse?lat=${latitude}&lon=${longitude}&format=json&addressdetails=1&zoom=18`,
            { headers: { "Accept-Language": "en", "User-Agent": "OpenBake/1.0" } }
          );
          const data = await res.json();
          const addr = data.address || {};
          const road = addr.road || addr.suburb || addr.neighbourhood || "";
          const houseNumber = addr.house_number ? `${addr.house_number}, ` : "";
          const area = addr.suburb || addr.neighbourhood || addr.quarter || "";
          const fullAddress = `${houseNumber}${road}${area && !road.includes(area) ? ", " + area : ""}`.trim().replace(/^,\s*/, "");
          setNewAddress((prev) => ({
            ...prev,
            full_address: fullAddress || data.display_name?.split(",").slice(0, 3).join(",").trim() || "",
            city: addr.city || addr.town || addr.village || addr.county || "",
            state: addr.state || "",
            pincode: addr.postcode || "",
            lat: latitude,
            lng: longitude,
          }));
          setAddrErrors((prev) => {
            const next = { ...prev };
            delete next.location;
            return next;
          });
          toast.success(`Location detected! (±${Math.round(accuracy)}m accuracy)`);
        } catch {
          // Even if geocoding fails, save the coordinates
          setNewAddress((prev) => ({ ...prev, lat: latitude, lng: longitude }));
          setAddrErrors((prev) => {
            const next = { ...prev };
            delete next.location;
            return next;
          });
          toast.error("Got coordinates but couldn't fetch address. Please type it manually.");
        } finally {
          setLocating(false);
        }
      },
      (err) => {
        setLocating(false);
        if (err.code === 1) {
          toast.error("Location permission denied. Please allow access in browser settings, then refresh.");
        } else if (err.code === 2) {
          toast.error("Location unavailable. Make sure GPS / Location Services are turned on.");
        } else if (err.code === 3) {
          toast.error("Location request timed out. Try again or enter address manually.");
        } else {
          toast.error("Could not get your location. Try again.");
        }
      },
      { timeout: 20000, enableHighAccuracy: true, maximumAge: 30000 }
    );
  };

  // ── Razorpay payment flow ────────────────────────────────────────────────────
  const launchRazorpay = async (orderId: string) => {
    try {
      const res = await api.post<RazorpayOrderResponse>("/payments/create-order", { order_id: orderId });
      const { razorpay_order_id, razorpay_key_id, amount, currency, order_id: backendOrderId } = res.data;

      // ── Dev mock mode ── when backend returns a mock order (no valid Razorpay creds),
      // skip the SDK entirely and directly verify with placeholder values.
      if (razorpay_order_id.startsWith("order_dev_")) {
        try {
          await api.post("/payments/verify", {
            order_id: backendOrderId || orderId,
            razorpay_order_id,
            razorpay_payment_id: `pay_dev_${Date.now()}`,
            razorpay_signature: "dev_mock_signature",
          });
          clearCart();
          toast.success("(Dev) Payment simulated — order confirmed!");
          router.push("/orders");
        } catch {
          toast.error("Dev payment verification failed. Check backend logs.");
          router.push("/orders");
        }
        return;
      }

      if (typeof window.Razorpay === "undefined") {
        toast.error("Razorpay SDK not loaded. Please refresh the page.");
        return;
      }

      const options: Record<string, unknown> = {
        key: razorpay_key_id || process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || "rzp_test_placeholder",
        amount,
        currency,
        name: "OpenBake",
        description: `Order #${orderId.slice(0, 8)}`,
        image: "/logo.png",
        order_id: razorpay_order_id,
        // Let Razorpay show all enabled payment methods (UPI, Cards, etc.)
        handler: async (response: { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature: string }) => {
          try {
            await api.post("/payments/verify", {
              order_id: backendOrderId || orderId,
              razorpay_order_id: response.razorpay_order_id,
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_signature: response.razorpay_signature,
            });
            clearCart();
            toast.success("Payment successful! Order confirmed.");
            router.push("/orders");
          } catch {
            toast.error("Payment verification failed. Please contact support.");
            router.push("/orders");
          }
        },
        prefill: {
          name: user?.name || "",
          email: user?.email || "",
          contact: user?.phone || "",
        },
        theme: { color: "#8B4513" },
        modal: {
          ondismiss: () => {
            toast("Payment cancelled. Your order is saved — you can pay later.", { icon: "ℹ️" });
            router.push("/orders");
          },
        },
      };

      const rzp = new window.Razorpay(options);
      rzp.open();
    } catch {
      toast.error("Failed to initiate payment. Please retry.");
    }
  };

  const placeOrder = async () => {
    if (orderType === "delivery" && !isDeliverable) {
      toast.error("Selected address is outside our delivery area.");
      return;
    }

    setPlacing(true);
    try {
      const orderItems = items.map((item) => ({
        product_id: item.product.id,
        quantity: item.quantity,
        customization: item.customization || null,
      }));

      const res = await api.post("/orders", {
        address_id: orderType === "delivery" ? selectedAddress : null,
        order_type: orderType,
        items: orderItems,
        coupon_code: discount > 0 ? couponCode : null,
        payment_method: paymentMethod,
        time_slot: selectedSlot,
        special_note: specialNote || null,
      });

      const orderId: string = res.data.id;

      // For UPI / card, open Razorpay. For COD, go straight to orders.
      if (paymentMethod === "upi" || paymentMethod === "card") {
        await launchRazorpay(orderId);
      } else {
        clearCart();
        toast.success("Order placed successfully!");
        router.push("/orders");
      }
    } catch {
      toast.error("Failed to place order");
    } finally {
      setPlacing(false);
    }
  };

  if (!isAuthenticated || items.length === 0) return null;

  return (
    <>
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="font-playfair text-2xl font-bold mb-8">Checkout</h1>

        {/* Progress Steps */}
        <div className="flex items-center justify-center gap-4 mb-12">
          {["Address", "Delivery", "Payment", "Review"].map((s, i) => (
            <div key={s} className="flex items-center gap-2">
              <button
                onClick={() => setStep(i + 1)}
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-colors ${
                  i + 1 <= step ? "bg-primary text-white" : "bg-border/30 text-text-secondary"
                }`}
              >
                {i + 1}
              </button>
              <span className="text-sm hidden sm:inline">{s}</span>
              {i < 3 && <div className="w-8 h-px bg-border hidden sm:block" />}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            {/* Step 1: Address */}
            {step === 1 && (
              <div className="bg-white rounded-2xl p-6 shadow-sm">
                <h2 className="font-semibold text-lg mb-4">Delivery Method</h2>
                <div className="flex gap-4 mb-6">
                  {(["delivery", "pickup"] as const).map((type) => (
                    <button
                      key={type}
                      onClick={() => setOrderType(type)}
                      className={`flex-1 py-3 rounded-xl border-2 text-sm font-semibold transition-all ${
                        orderType === type ? "border-primary bg-primary/5 text-primary" : "border-border text-text-secondary"
                      }`}
                    >
                      {type === "delivery" ? "🚗 Delivery" : "🏪 Pickup"}
                    </button>
                  ))}
                </div>

                {orderType === "delivery" && (
                  <>
                    <h3 className="font-medium mb-3">Select Address</h3>
                    <div className="space-y-3">
                      {addresses.map((addr) => (
                        <label
                          key={addr.id}
                          className={`flex items-start gap-3 p-4 rounded-xl border-2 cursor-pointer transition-all ${
                            selectedAddress === addr.id ? "border-primary bg-primary/5" : "border-border"
                          }`}
                        >
                          <input
                            type="radio"
                            name="address"
                            checked={selectedAddress === addr.id}
                            onChange={() => setSelectedAddress(addr.id)}
                            className="mt-1"
                          />
                          <div>
                            <span className="font-medium">{addr.label}</span>
                            <p className="text-sm text-text-secondary">{addr.full_address}, {addr.city} - {addr.pincode}</p>
                          </div>
                        </label>
                      ))}
                    </div>

                    {/* Delivery estimate info badge */}
                    {selectedAddress && (
                      <div className="mt-4">
                        {estimateLoading ? (
                          <div className="flex items-center gap-2 text-sm text-text-secondary bg-cream rounded-xl px-4 py-3">
                            <Loader2 size={16} className="animate-spin" />
                            Calculating delivery estimate…
                          </div>
                        ) : deliveryEstimate ? (
                          deliveryEstimate.is_deliverable ? (
                            <div className="flex items-center gap-3 text-sm bg-green-50 text-green-800 rounded-xl px-4 py-3">
                              <Truck size={18} />
                              <div>
                                <span className="font-semibold">
                                  {deliveryEstimate.distance_km.toFixed(1)} km away
                                </span>
                                {" · "}
                                ETA ~{deliveryEstimate.estimated_time_minutes} min
                                {" · "}
                                {deliveryEstimate.delivery_fee === 0 ? (
                                  <span className="text-green-600 font-semibold">Free delivery!</span>
                                ) : (
                                  <span>Delivery fee: {formatPrice(deliveryEstimate.delivery_fee)}</span>
                                )}
                              </div>
                            </div>
                          ) : (
                            <div className="flex items-center gap-3 text-sm bg-red-50 text-red-700 rounded-xl px-4 py-3">
                              <MapPin size={18} />
                              <span>This address is outside our delivery area ({deliveryEstimate.distance_km.toFixed(1)} km). Please choose a different address or select Pickup.</span>
                            </div>
                          )
                        ) : null}
                      </div>
                    )}

                    {showNewAddress ? (
                      <div className="mt-4 space-y-3 border border-border rounded-xl p-4">
                        <button
                          type="button"
                          onClick={getCurrentLocation}
                          disabled={locating}
                          className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl border-2 border-dashed border-primary/40 text-primary text-sm font-medium hover:bg-primary/5 transition-colors disabled:opacity-50"
                        >
                          {locating ? <Loader2 size={15} className="animate-spin" /> : <MapPin size={15} />}
                          {locating ? "Detecting location..." : "📍 Use My Current Location"}
                        </button>
                        {addrErrors.location && (
                          <div className="flex items-center gap-2 text-xs text-amber-600 bg-amber-50 px-3 py-2 rounded-lg">
                            <AlertCircle size={14} />
                            {addrErrors.location}
                          </div>
                        )}
                        {newAddress.lat && newAddress.lng && (
                          <div className="flex items-center gap-2 text-xs text-green-600 bg-green-50 px-3 py-2 rounded-lg">
                            <MapPin size={14} />
                            Location set ({newAddress.lat.toFixed(4)}, {newAddress.lng.toFixed(4)})
                          </div>
                        )}
                        <div className="grid grid-cols-2 gap-3">
                          <div>
                            <label className="block text-xs font-medium text-text-primary mb-1">
                              Label <span className="text-error">*</span>
                            </label>
                            <div className="flex gap-2">
                              {["Home", "Work", "Other"].map((l) => (
                                <button
                                  key={l}
                                  type="button"
                                  onClick={() => { setNewAddress({ ...newAddress, label: l }); setAddrErrors((p) => { const n = {...p}; delete n.label; return n; }); }}
                                  className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
                                    newAddress.label === l
                                      ? "border-primary bg-primary/10 text-primary"
                                      : "border-border text-text-secondary hover:border-primary/30"
                                  }`}
                                >
                                  {l === "Home" ? "🏠" : l === "Work" ? "💼" : "📍"} {l}
                                </button>
                              ))}
                            </div>
                          </div>
                          <Input
                            id="phone"
                            label="Contact Phone"
                            value={newAddress.phone}
                            onChange={(e) => { setNewAddress({ ...newAddress, phone: e.target.value }); setAddrErrors((p) => { const n = {...p}; delete n.phone; return n; }); }}
                            placeholder="9876543210"
                            error={addrErrors.phone}
                          />
                        </div>
                        <Input
                          id="addr"
                          label="House / Flat / Floor, Building Name, Street *"
                          value={newAddress.full_address}
                          onChange={(e) => { setNewAddress({ ...newAddress, full_address: e.target.value }); setAddrErrors((p) => { const n = {...p}; delete n.full_address; return n; }); }}
                          placeholder="e.g. Flat 302, Sai Residency, MG Road"
                          error={addrErrors.full_address}
                        />
                        <Input
                          id="landmark"
                          label="Landmark (optional)"
                          value={newAddress.landmark}
                          onChange={(e) => setNewAddress({ ...newAddress, landmark: e.target.value })}
                          placeholder="e.g. Near City Mall, Opposite SBI Bank"
                        />
                        <div className="grid grid-cols-3 gap-3">
                          <Input
                            id="city"
                            label="City *"
                            value={newAddress.city}
                            onChange={(e) => { setNewAddress({ ...newAddress, city: e.target.value }); setAddrErrors((p) => { const n = {...p}; delete n.city; return n; }); }}
                            error={addrErrors.city}
                          />
                          <Input
                            id="state"
                            label="State"
                            value={newAddress.state}
                            onChange={(e) => setNewAddress({ ...newAddress, state: e.target.value })}
                            placeholder="e.g. Maharashtra"
                          />
                          <Input
                            id="pin"
                            label="Pincode *"
                            value={newAddress.pincode}
                            onChange={(e) => { setNewAddress({ ...newAddress, pincode: e.target.value.replace(/\D/g, "").slice(0, 6) }); setAddrErrors((p) => { const n = {...p}; delete n.pincode; return n; }); }}
                            placeholder="560001"
                            error={addrErrors.pincode}
                          />
                        </div>
                        <div className="flex gap-2 pt-1">
                          <Button size="sm" onClick={addNewAddress}>Save Address</Button>
                          <Button size="sm" variant="ghost" onClick={() => { setShowNewAddress(false); setAddrErrors({}); }}>Cancel</Button>
                        </div>
                      </div>
                    ) : (
                      <button onClick={() => setShowNewAddress(true)} className="mt-3 text-sm text-primary font-medium hover:underline">
                        + Add New Address
                      </button>
                    )}
                  </>
                )}

                <div className="mt-6 flex justify-end">
                  <Button
                    onClick={() => {
                      if (orderType === "delivery" && !selectedAddress) {
                        toast.error("Please select or add a delivery address");
                        return;
                      }
                      setStep(2);
                    }}
                    disabled={orderType === "delivery" && !isDeliverable}
                  >
                    Continue
                  </Button>
                </div>
              </div>
            )}

            {/* Step 2: Delivery Slot */}
            {step === 2 && (
              <div className="bg-white rounded-2xl p-6 shadow-sm">
                <h2 className="font-semibold text-lg mb-4">Delivery Time Slot</h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {timeSlots.map((slot) => (
                    <button
                      key={slot}
                      onClick={() => setSelectedSlot(slot)}
                      className={`py-3 px-4 rounded-xl border-2 text-sm font-medium transition-all ${
                        selectedSlot === slot ? "border-primary bg-primary/5 text-primary" : "border-border text-text-secondary"
                      }`}
                    >
                      {slot}
                    </button>
                  ))}
                </div>

                <div className="mt-6">
                  <label className="text-sm font-medium text-text-primary mb-2 block">Special Note (optional)</label>
                  <textarea
                    value={specialNote}
                    onChange={(e) => setSpecialNote(e.target.value)}
                    placeholder="Any special instructions..."
                    className="w-full border border-border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                    rows={3}
                  />
                </div>

                <div className="mt-6 flex justify-between">
                  <Button variant="ghost" onClick={() => setStep(1)}>Back</Button>
                  <Button onClick={() => setStep(3)}>Continue</Button>
                </div>
              </div>
            )}

            {/* Step 3: Payment */}
            {step === 3 && (
              <div className="bg-white rounded-2xl p-6 shadow-sm">
                <h2 className="font-semibold text-lg mb-4">Payment Method</h2>
                <div className="space-y-3">
                  {paymentMethods.map((pm) => (
                    <div key={pm.id}>
                      <label
                        className={`flex items-center gap-3 p-4 rounded-xl border-2 cursor-pointer transition-all ${
                          paymentMethod === pm.id ? "border-primary bg-primary/5" : "border-border"
                        }`}
                      >
                        <input
                          type="radio"
                          name="payment"
                          checked={paymentMethod === pm.id}
                          onChange={() => setPaymentMethod(pm.id)}
                        />
                        <span className="text-xl">{pm.icon}</span>
                        <div className="flex-1">
                          <span className="font-medium">{pm.label}</span>
                          {pm.id === "cod" && (
                            <p className="text-xs text-text-secondary mt-0.5">Pay when your order is delivered</p>
                          )}
                          {pm.id === "upi" && (
                            <p className="text-xs text-text-secondary mt-0.5">Google Pay, PhonePe, Paytm & more</p>
                          )}
                          {pm.id === "card" && (
                            <p className="text-xs text-text-secondary mt-0.5">Visa, Mastercard, RuPay — secured by Razorpay</p>
                          )}
                        </div>
                      </label>

                      {/* UPI details — expanded when selected */}
                      {pm.id === "upi" && paymentMethod === "upi" && (
                        <div className="mt-2 ml-11 p-4 bg-blue-50/50 rounded-xl border border-blue-100 space-y-3">
                          <p className="text-sm font-medium text-text-primary">How UPI payment works:</p>
                          <div className="flex items-start gap-3 text-sm text-text-secondary">
                            <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold">1</span>
                            <p>Click &quot;Place Order &amp; Pay&quot; on the next step</p>
                          </div>
                          <div className="flex items-start gap-3 text-sm text-text-secondary">
                            <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold">2</span>
                            <p>Razorpay checkout opens — choose your UPI app or enter your UPI ID (e.g. name@upi, name@ybl)</p>
                          </div>
                          <div className="flex items-start gap-3 text-sm text-text-secondary">
                            <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold">3</span>
                            <p>Approve the payment in your UPI app — your order is confirmed instantly!</p>
                          </div>
                          <div className="flex items-center gap-2 mt-1">
                            <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/UPI-Logo-vector.svg/120px-UPI-Logo-vector.svg.png" alt="UPI" className="h-5" />
                            <span className="text-xs text-text-secondary">Secure UPI payment via Razorpay</span>
                          </div>
                        </div>
                      )}

                      {/* Card details — expanded when selected */}
                      {pm.id === "card" && paymentMethod === "card" && (
                        <div className="mt-2 ml-11 p-4 bg-purple-50/30 rounded-xl border border-purple-100 space-y-3">
                          <p className="text-sm font-medium text-text-primary">How card payment works:</p>
                          <div className="flex items-start gap-3 text-sm text-text-secondary">
                            <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold">1</span>
                            <p>Click &quot;Place Order &amp; Pay&quot; on the next step</p>
                          </div>
                          <div className="flex items-start gap-3 text-sm text-text-secondary">
                            <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold">2</span>
                            <p>Enter your card details (number, expiry, CVV) securely in the Razorpay window</p>
                          </div>
                          <div className="flex items-start gap-3 text-sm text-text-secondary">
                            <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold">3</span>
                            <p>Complete 3D Secure / OTP verification — your order is confirmed!</p>
                          </div>
                          <div className="flex items-center gap-3 mt-1">
                            <div className="flex gap-1">
                              <span className="px-2 py-0.5 bg-white rounded text-[10px] font-bold border text-blue-700">VISA</span>
                              <span className="px-2 py-0.5 bg-white rounded text-[10px] font-bold border text-red-600">MC</span>
                              <span className="px-2 py-0.5 bg-white rounded text-[10px] font-bold border text-green-700">RuPay</span>
                            </div>
                            <span className="text-xs text-text-secondary">PCI-DSS compliant via Razorpay</span>
                          </div>
                        </div>
                      )}

                      {/* COD details — expanded when selected */}
                      {pm.id === "cod" && paymentMethod === "cod" && (
                        <div className="mt-2 ml-11 p-4 bg-green-50/50 rounded-xl border border-green-100">
                          <p className="text-sm text-text-secondary">Pay with cash or UPI QR when your order arrives. No advance payment needed.</p>
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                <div className="mt-6 flex justify-between">
                  <Button variant="ghost" onClick={() => setStep(2)}>Back</Button>
                  <Button onClick={() => setStep(4)}>Review Order</Button>
                </div>
              </div>
            )}

            {/* Step 4: Review */}
            {step === 4 && (
              <div className="bg-white rounded-2xl p-6 shadow-sm">
                <h2 className="font-semibold text-lg mb-4">Review Your Order</h2>
                <div className="space-y-3 mb-6">
                  {items.map((item) => (
                    <div key={item.product.id} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                      <div>
                        <p className="font-medium">{item.product.name}</p>
                        <p className="text-sm text-text-secondary">Qty: {item.quantity}</p>
                      </div>
                      <p className="font-semibold">{formatPrice(item.product.price * item.quantity)}</p>
                    </div>
                  ))}
                </div>

                <div className="text-sm space-y-1 mb-4">
                  <p><span className="text-text-secondary">Type:</span> {orderType === "delivery" ? "Delivery" : "Pickup"}</p>
                  <p><span className="text-text-secondary">Time:</span> {selectedSlot}</p>
                  <p><span className="text-text-secondary">Payment:</span> {paymentMethods.find((p) => p.id === paymentMethod)?.label}</p>
                  {deliveryEstimate && orderType === "delivery" && (
                    <p><span className="text-text-secondary">ETA:</span> ~{deliveryEstimate.estimated_time_minutes} min</p>
                  )}
                  {specialNote && <p><span className="text-text-secondary">Note:</span> {specialNote}</p>}
                </div>

                <div className="mt-6 flex justify-between">
                  <Button variant="ghost" onClick={() => setStep(3)}>Back</Button>
                  <Button onClick={placeOrder} disabled={placing}>
                    {placing
                      ? "Processing…"
                      : paymentMethod === "cod"
                        ? "Place Order"
                        : "Place Order & Pay"}
                  </Button>
                </div>
              </div>
            )}
          </div>

          {/* Order Summary Sidebar */}
          <div className="bg-white rounded-2xl p-6 shadow-sm h-fit sticky top-24">
            <h2 className="font-semibold text-lg mb-4">Order Summary</h2>
            <div className="space-y-2 text-sm">
              {items.map((item) => (
                <div key={item.product.id} className="flex justify-between">
                  <span className="text-text-secondary">{item.product.name} x{item.quantity}</span>
                  <span>{formatPrice(item.product.price * item.quantity)}</span>
                </div>
              ))}
              <div className="border-t border-border pt-2 mt-2">
                <div className="flex justify-between">
                  <span className="text-text-secondary">Subtotal</span>
                  <span>{formatPrice(subtotal())}</span>
                </div>
                {discount > 0 && (
                  <div className="flex justify-between text-success">
                    <span>Discount</span>
                    <span>-{formatPrice(discount)}</span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-text-secondary">Delivery</span>
                  <span>
                    {estimateLoading
                      ? "Calculating…"
                      : deliveryFee > 0
                        ? formatPrice(deliveryFee)
                        : "Free"}
                  </span>
                </div>
              </div>
              <div className="border-t border-border pt-2 mt-2 flex justify-between font-bold text-lg">
                <span>Total</span>
                <span className="text-primary">{formatPrice(total)}</span>
              </div>
            </div>

            {/* Coupon */}
            <div className="mt-4">
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="Coupon code"
                  value={couponCode}
                  onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
                  className="flex-1 border border-border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
                <Button size="sm" variant="outline" onClick={applyCoupon}>Apply</Button>
              </div>
              {couponMessage && (
                <p className={`text-xs mt-1 ${discount > 0 ? "text-success" : "text-error"}`}>{couponMessage}</p>
              )}
            </div>
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
