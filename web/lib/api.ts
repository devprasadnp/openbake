import axios from "axios";
import toast from "react-hot-toast";
import type { Product } from "@/types";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8000/api";

const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
    // Skip ngrok browser-warning interstitial when tunnelling locally
    "ngrok-skip-browser-warning": "true",
  },
  timeout: 15000, // 15s — prevent hanging requests
});

// Attach JWT token to every request
api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("access_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// Shared refresh promise to prevent concurrent refresh token rotation races
let refreshPromise: Promise<{ access_token: string; refresh_token: string }> | null = null;

// Auto-refresh on 401
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const originalRequest = error.config;

    // Network / timeout errors
    if (!error.response) {
      toast.error("Network error — please check your connection");
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // If a refresh is already in-flight, reuse the same promise
        if (!refreshPromise) {
          const refreshToken = localStorage.getItem("refresh_token");
          if (!refreshToken) throw new Error("No refresh token");

          refreshPromise = axios
            .post(`${BASE_URL}/auth/refresh`, { refresh_token: refreshToken })
            .then((res) => res.data)
            .finally(() => {
              refreshPromise = null;
            });
        }

        const { access_token, refresh_token } = await refreshPromise;
        localStorage.setItem("access_token", access_token);
        localStorage.setItem("refresh_token", refresh_token);

        originalRequest.headers.Authorization = `Bearer ${access_token}`;
        return api(originalRequest);
      } catch {
        localStorage.removeItem("access_token");
        localStorage.removeItem("refresh_token");
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
      }
    }

    // Don't toast on 401 (handled above) or cancelled requests
    if (error.response?.status !== 401 && !axios.isCancel(error)) {
      const raw = error.response?.data?.detail || "Something went wrong";
      // Sanitize backend errors — never show raw exception details to users
      let message = raw;
      if (typeof raw === "string" && (raw.includes("sqlalchemy") || raw.includes("Traceback") || raw.includes("IntegrityError"))) {
        message = "Something went wrong. Please try again.";
      }
      toast.error(message);
    }

    return Promise.reject(error);
  }
);

// ── Typed helpers ──────────────────────────────────────────────────────────────

export interface PaginatedProducts {
  items: Product[];
  total: number;
  page: number;
  page_size: number;
  pages: number;
  has_next: boolean;
  has_prev: boolean;
}

export async function fetchProducts(params: Record<string, unknown> = {}): Promise<PaginatedProducts> {
  const res = await api.get<PaginatedProducts>("/products", { params });
  return res.data;
}

export default api;
