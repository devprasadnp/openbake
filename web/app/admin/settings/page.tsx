"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import api from "@/lib/api";
import toast from "react-hot-toast";
import { MapPin, Truck, Save, Loader2 } from "lucide-react";

interface DeliveryConfig {
  bakery_lat: number;
  bakery_lng: number;
  free_delivery_radius_km: number;
  delivery_fee_default: number;
  speed_min_per_km: number;
}

export default function AdminSettingsPage() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuthStore();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [config, setConfig] = useState<DeliveryConfig>({
    bakery_lat: 12.9716,
    bakery_lng: 77.5946,
    free_delivery_radius_km: 5,
    delivery_fee_default: 40,
    speed_min_per_km: 3,
  });

  useEffect(() => {
    if (!isAuthenticated || user?.role !== "admin") {
      router.push("/login");
      return;
    }
    fetchConfig();
  }, [isAuthenticated, user, router]);

  const fetchConfig = async () => {
    try {
      const res = await api.get<DeliveryConfig>("/admin/delivery-config");
      setConfig(res.data);
    } catch {
      toast.error("Failed to load delivery config");
    } finally {
      setLoading(false);
    }
  };

  const saveConfig = async () => {
    setSaving(true);
    try {
      await api.patch("/admin/delivery-config", config);
      toast.success("Delivery settings saved!");
    } catch {
      toast.error("Failed to save settings");
    } finally {
      setSaving(false);
    }
  };

  const detectBakeryLocation = () => {
    if (!navigator.geolocation) {
      toast.error("Geolocation not available in this browser");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setConfig({
          ...config,
          bakery_lat: parseFloat(pos.coords.latitude.toFixed(6)),
          bakery_lng: parseFloat(pos.coords.longitude.toFixed(6)),
        });
        toast.success("Location detected!");
      },
      () => toast.error("Could not detect location"),
      { enableHighAccuracy: true, timeout: 15000 }
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <Loader2 className="animate-spin text-primary" size={24} />
      </div>
    );
  }

  return (
    <div>
      <h1 className="font-playfair text-2xl font-bold mb-6">Settings</h1>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Bakery Location */}
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <h2 className="font-semibold text-lg mb-4 flex items-center gap-2">
            <MapPin size={18} className="text-primary" /> Bakery Location
          </h2>
          <p className="text-sm text-text-secondary mb-4">
            Set your bakery&apos;s location. This is used to calculate delivery distance and fees for every order.
          </p>

          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <Input
                id="bakery_lat"
                label="Latitude"
                type="number"
                value={config.bakery_lat.toString()}
                onChange={(e) => setConfig({ ...config, bakery_lat: parseFloat(e.target.value) || 0 })}
                placeholder="12.9716"
              />
              <Input
                id="bakery_lng"
                label="Longitude"
                type="number"
                value={config.bakery_lng.toString()}
                onChange={(e) => setConfig({ ...config, bakery_lng: parseFloat(e.target.value) || 0 })}
                placeholder="77.5946"
              />
            </div>

            <div className="flex gap-2">
              <Button size="sm" variant="ghost" onClick={detectBakeryLocation}>
                <MapPin size={14} className="mr-1" /> Detect My Location
              </Button>
              {config.bakery_lat && config.bakery_lng && (
                <a
                  href={`https://maps.google.com/?q=${config.bakery_lat},${config.bakery_lng}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm text-primary hover:underline flex items-center gap-1"
                >
                  View on Map →
                </a>
              )}
            </div>
          </div>
        </div>

        {/* Delivery Settings */}
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <h2 className="font-semibold text-lg mb-4 flex items-center gap-2">
            <Truck size={18} className="text-primary" /> Delivery Charges
          </h2>
          <p className="text-sm text-text-secondary mb-4">
            Configure free delivery radius and base delivery fee. Orders beyond the free radius will be charged the delivery fee.
          </p>

          <div className="space-y-4">
            <Input
              id="free_radius"
              label="Free Delivery Radius (km)"
              type="number"
              value={config.free_delivery_radius_km.toString()}
              onChange={(e) => setConfig({ ...config, free_delivery_radius_km: parseFloat(e.target.value) || 0 })}
              placeholder="5"
            />
            <Input
              id="delivery_fee"
              label="Delivery Fee (₹)"
              type="number"
              value={config.delivery_fee_default.toString()}
              onChange={(e) => setConfig({ ...config, delivery_fee_default: parseFloat(e.target.value) || 0 })}
              placeholder="40"
            />
            <Input
              id="speed"
              label="Speed (min per km)"
              type="number"
              value={config.speed_min_per_km.toString()}
              onChange={(e) => setConfig({ ...config, speed_min_per_km: parseFloat(e.target.value) || 0 })}
              placeholder="3"
            />

            <div className="bg-cream rounded-xl p-4 text-sm space-y-1">
              <p className="font-medium">How delivery fee works:</p>
              <ul className="list-disc list-inside text-text-secondary space-y-0.5">
                <li>Orders within <strong>{config.free_delivery_radius_km} km</strong> — Free delivery</li>
                <li>Orders beyond that — <strong>₹{config.delivery_fee_default}</strong> delivery charge</li>
                <li>Maximum delivery distance — <strong>25 km</strong></li>
                <li>ETA = 15 min prep + distance × {config.speed_min_per_km} min/km</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Save Button */}
      <div className="mt-6 flex justify-end">
        <Button onClick={saveConfig} disabled={saving}>
          {saving ? (
            <>
              <Loader2 size={16} className="animate-spin mr-2" /> Saving...
            </>
          ) : (
            <>
              <Save size={16} className="mr-2" /> Save Settings
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
