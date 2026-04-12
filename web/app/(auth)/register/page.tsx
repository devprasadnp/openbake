"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import api from "@/lib/api";
import toast from "react-hot-toast";

export default function RegisterPage() {
  const router = useRouter();
  const { register, isLoading } = useAuthStore();

  // Shared state
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [authMode, setAuthMode] = useState<"email" | "phone">("email");

  // Email registration state
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");

  // OTP registration state
  const [otpName, setOtpName] = useState("");
  const [otpPhone, setOtpPhone] = useState("");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [otpSending, setOtpSending] = useState(false);
  const [otpVerifying, setOtpVerifying] = useState(false);

  // --- Email registration validation ---
  const validate = (): boolean => {
    const errors: Record<string, string> = {};
    if (!name.trim()) {
      errors.name = "Name is required";
    } else if (name.trim().length < 2) {
      errors.name = "Name must be at least 2 characters";
    }
    if (!email.trim()) {
      errors.email = "Email is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = "Enter a valid email address";
    }
    if (phone && !/^[6-9]\d{9}$/.test(phone.replace(/\s+/g, ""))) {
      errors.phone = "Enter a valid 10-digit mobile number";
    }
    if (!password) {
      errors.password = "Password is required";
    } else if (password.length < 8) {
      errors.password = "Password must be at least 8 characters";
    } else if (!/(?=.*[a-z])(?=.*[A-Z0-9])/.test(password)) {
      errors.password = "Include at least one uppercase letter or number";
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (!validate()) return;
    try {
      await register(name, email, password, phone.replace(/\s+/g, "") || undefined);
      router.push("/");
    } catch {
      setError("Registration failed. Email may already be in use.");
    }
  };

  // --- OTP flow ---
  const sendOtp = async () => {
    const cleanPhone = otpPhone.replace(/\s+/g, "");
    if (!cleanPhone || !/^[6-9]\d{9}$/.test(cleanPhone)) {
      setError("Enter a valid 10-digit phone number");
      return;
    }
    setOtpSending(true);
    setError("");
    try {
      await api.post("/auth/otp/send", { phone: cleanPhone });
      setOtpSent(true);
      toast.success("OTP sent to your phone");
    } catch {
      setError("Failed to send OTP. Please try again.");
    } finally {
      setOtpSending(false);
    }
  };

  const verifyOtp = async () => {
    if (!otp || otp.length !== 6) {
      setError("Enter the 6-digit OTP");
      return;
    }
    if (!otpName.trim()) {
      setError("Name is required");
      return;
    }
    setOtpVerifying(true);
    setError("");
    try {
      const res = await api.post("/auth/otp/verify", {
        phone: otpPhone.replace(/\s+/g, ""),
        otp,
        name: otpName.trim() || undefined,
      });
      useAuthStore.getState().setTokens(res.data);
      await useAuthStore.getState().fetchProfile();
      toast.success("Account created successfully!");
      router.push("/");
    } catch {
      setError("Invalid OTP. Please try again.");
    } finally {
      setOtpVerifying(false);
    }
  };

  return (
    <div className="min-h-screen overflow-y-auto bg-cream px-4 py-8">
      <div className="flex items-center justify-center min-h-[calc(100vh-4rem)]">
        <div className="w-full max-w-md">
          <div className="text-center mb-8">
            <Link href="/" className="inline-flex items-center gap-2">
              <span className="text-3xl">&#x1F370;</span>
              <span className="font-playfair text-2xl font-bold text-primary">
                Sri Vinayaka Bakery
              </span>
            </Link>
          </div>

          <div className="bg-white rounded-2xl shadow-sm p-8">
            <h1 className="font-playfair text-2xl font-bold text-center mb-6">
              Create Account
            </h1>

            {/* Tab Switcher */}
            <div className="flex rounded-xl bg-cream p-1 mb-6">
              <button
                type="button"
                onClick={() => {
                  setAuthMode("email");
                  setError("");
                  setFieldErrors({});
                }}
                className={`flex-1 py-2 text-sm font-semibold rounded-lg transition-colors ${
                  authMode === "email"
                    ? "bg-white text-primary shadow-sm"
                    : "text-text-secondary hover:text-text"
                }`}
              >
                Email
              </button>
              <button
                type="button"
                onClick={() => {
                  setAuthMode("phone");
                  setError("");
                  setFieldErrors({});
                }}
                className={`flex-1 py-2 text-sm font-semibold rounded-lg transition-colors ${
                  authMode === "phone"
                    ? "bg-white text-primary shadow-sm"
                    : "text-text-secondary hover:text-text"
                }`}
              >
                Phone (OTP)
              </button>
            </div>

            {error && (
              <div className="bg-error/10 text-error text-sm px-4 py-2 rounded-xl mb-4">
                {error}
              </div>
            )}

            {/* Email Registration Form */}
            {authMode === "email" && (
              <form onSubmit={handleSubmit} className="space-y-4">
                <Input
                  id="name"
                  label="Full Name"
                  placeholder="John Doe"
                  value={name}
                  onChange={(e) => {
                    setName(e.target.value);
                    setFieldErrors((p) => { const n = {...p}; delete n.name; return n; });
                  }}
                  error={fieldErrors.name}
                />
                <Input
                  id="email"
                  label="Email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setFieldErrors((p) => { const n = {...p}; delete n.email; return n; });
                  }}
                  error={fieldErrors.email}
                />
                <Input
                  id="phone"
                  label="Phone Number"
                  type="tel"
                  placeholder="9876543210"
                  value={phone}
                  onChange={(e) => {
                    setPhone(e.target.value);
                    setFieldErrors((p) => { const n = {...p}; delete n.phone; return n; });
                  }}
                  error={fieldErrors.phone}
                />
                <div>
                  <Input
                    id="password"
                    label="Password"
                    type="password"
                    placeholder="Min 8 characters"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      setFieldErrors((p) => { const n = {...p}; delete n.password; return n; });
                    }}
                    error={fieldErrors.password}
                  />
                  {password && !fieldErrors.password && (
                    <div className="mt-1.5 flex gap-1">
                      {[
                        password.length >= 8,
                        /[A-Z]/.test(password),
                        /[0-9]/.test(password),
                      ].map((met, i) => (
                        <div
                          key={i}
                          className={`h-1 flex-1 rounded-full transition-colors ${met ? "bg-success" : "bg-border/40"}`}
                        />
                      ))}
                    </div>
                  )}
                </div>
                <Button type="submit" className="w-full" disabled={isLoading}>
                  {isLoading ? "Creating account..." : "Create Account"}
                </Button>
              </form>
            )}

            {/* Phone OTP Registration Form */}
            {authMode === "phone" && (
              <div className="space-y-4">
                <Input
                  id="otp-name"
                  label="Full Name"
                  placeholder="John Doe"
                  value={otpName}
                  onChange={(e) => {
                    setOtpName(e.target.value);
                    setError("");
                  }}
                />
                <div>
                  <Input
                    id="otp-phone"
                    label="Phone Number"
                    type="tel"
                    placeholder="9876543210"
                    value={otpPhone}
                    onChange={(e) => {
                      setOtpPhone(e.target.value);
                      setError("");
                      if (otpSent) {
                        setOtpSent(false);
                        setOtp("");
                      }
                    }}
                  />
                  {!otpSent && (
                    <Button
                      type="button"
                      onClick={sendOtp}
                      className="w-full mt-3"
                      disabled={otpSending || !otpPhone}
                    >
                      {otpSending ? "Sending OTP..." : "Send OTP"}
                    </Button>
                  )}
                </div>

                {otpSent && (
                  <>
                    <div>
                      <Input
                        id="otp-code"
                        label="Enter OTP"
                        type="text"
                        inputMode="numeric"
                        placeholder="6-digit OTP"
                        maxLength={6}
                        value={otp}
                        onChange={(e) => {
                          const val = e.target.value.replace(/\D/g, "");
                          setOtp(val);
                          setError("");
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => {
                          setOtpSent(false);
                          setOtp("");
                          sendOtp();
                        }}
                        className="text-xs text-primary font-semibold mt-1 hover:underline"
                      >
                        Resend OTP
                      </button>
                    </div>
                    <Button
                      type="button"
                      onClick={verifyOtp}
                      className="w-full"
                      disabled={otpVerifying || otp.length !== 6}
                    >
                      {otpVerifying ? "Verifying..." : "Verify & Create Account"}
                    </Button>
                  </>
                )}
              </div>
            )}

            <p className="text-center text-sm text-text-secondary mt-6">
              Already have an account?{" "}
              <Link href="/login" className="text-primary font-semibold hover:underline">
                Sign In
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
