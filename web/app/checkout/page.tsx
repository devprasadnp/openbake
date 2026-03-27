"use client";

import { useEffect, useState } from "react";
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
import { MapPin, Loader2 } from "lucide-react";
import type { Address } from "@/types";

const paymentMethods = [
  { id: "cod", label: "Cash on Delivery", icon: "💵" },
  { id: "upi", label: "UPI", icon: "📱" },
  { id: "card", label: "Card", icon: "💳" },
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
  const [newAddress, setNewAddress] = useState({ full_address: "", city: "", pincode: "", label: "Home", lat: undefined as number | undefined, lng: undefined as number | undefined });
  const [showNewAddress, setShowNewAddress] = useState(false);
  const [locating, setLocating] = useState(false);

  const deliveryFee = orderType === "delivery" ? 40 : 0;
  const total = subtotal() - discount + deliveryFee;

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

  const addNewAddress = async () => {
    try {
      const res = await api.post<Address>("/addresses", newAddress);
      setAddresses([...addresses, res.data]);
      setSelectedAddress(res.data.id);
      setShowNewAddress(false);
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
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { latitude, longitude } = position.coords;
        try {
          const res = await fetch(
            `https://nominatim.openstreetmap.org/reverse?lat=${latitude}&lon=${longitude}&format=json`,
            { headers: { "Accept-Language": "en" } }
          );
          const data = await res.json();
          const addr = data.address || {};
          const road = addr.road || addr.suburb || addr.neighbourhood || "";
          const houseNumber = addr.house_number ? `${addr.house_number}, ` : "";
          const fullAddress = `${houseNumber}${road}${addr.suburb ? ", " + addr.suburb : ""}`.trim();
          setNewAddress((prev) => ({
            ...prev,
            full_address: fullAddress || data.display_name?.split(",").slice(0, 2).join(",").trim() || "",
            city: addr.city || addr.town || addr.village || addr.county || "",
            pincode: addr.postcode || "",
            lat: latitude,
            lng: longitude,
          }));
          toast.success("Location detected!");
        } catch {
          toast.error("Could not fetch address for your location");
        } finally {
          setLocating(false);
        }
      },
      (err) => {
        setLocating(false);
        if (err.code === 1) {
          toast.error("Location permission denied. Please allow access in browser settings.");
        } else if (err.code === 2) {
          toast.error("Location unavailable. Make sure GPS / Location Services are on.");
        } else {
          toast.error("Could not get your location. Try again.");
        }
      },
      { timeout: 15000, enableHighAccuracy: true, maximumAge: 0 }
    );
  };

  const placeOrder = async () => {
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

      clearCart();
      toast.success("Order placed successfully!");
      router.push("/orders");
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

                    {showNewAddress ? (
                      <div className="mt-4 space-y-3 border border-border rounded-xl p-4">
                        <button
                          type="button"
                          onClick={getCurrentLocation}
                          disabled={locating}
                          className="w-full flex items-center justify-center gap-2 py-2 px-4 rounded-xl border-2 border-dashed border-primary/40 text-primary text-sm font-medium hover:bg-primary/5 transition-colors disabled:opacity-50"
                        >
                          {locating ? <Loader2 size={15} className="animate-spin" /> : <MapPin size={15} />}
                          {locating ? "Detecting location..." : "Use My Current Location"}
                        </button>
                        <Input id="addr" label="Full Address" value={newAddress.full_address} onChange={(e) => setNewAddress({ ...newAddress, full_address: e.target.value })} />
                        <div className="grid grid-cols-2 gap-3">
                          <Input id="city" label="City" value={newAddress.city} onChange={(e) => setNewAddress({ ...newAddress, city: e.target.value })} />
                          <Input id="pin" label="Pincode" value={newAddress.pincode} onChange={(e) => setNewAddress({ ...newAddress, pincode: e.target.value })} />
                        </div>
                        <div className="flex gap-2">
                          <Button size="sm" onClick={addNewAddress}>Save Address</Button>
                          <Button size="sm" variant="ghost" onClick={() => setShowNewAddress(false)}>Cancel</Button>
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
                  <Button onClick={() => setStep(2)}>Continue</Button>
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
                    <label
                      key={pm.id}
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
                      <span className="font-medium">{pm.label}</span>
                    </label>
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
                  {specialNote && <p><span className="text-text-secondary">Note:</span> {specialNote}</p>}
                </div>

                <div className="mt-6 flex justify-between">
                  <Button variant="ghost" onClick={() => setStep(3)}>Back</Button>
                  <Button onClick={placeOrder} disabled={placing}>
                    {placing ? "Placing Order..." : "Place Order"}
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
                  <span>{deliveryFee > 0 ? formatPrice(deliveryFee) : "Free"}</span>
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
