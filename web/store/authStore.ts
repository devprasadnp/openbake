import { create } from "zustand";
import type { User, TokenResponse } from "@/types";
import api from "@/lib/api";

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  initialized: boolean;

  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  fetchProfile: () => Promise<void>;
  setTokens: (tokens: TokenResponse) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  initialized: false,

  setTokens: (tokens) => {
    localStorage.setItem("access_token", tokens.access_token);
    localStorage.setItem("refresh_token", tokens.refresh_token);
    set({ isAuthenticated: true });
  },

  login: async (email, password) => {
    set({ isLoading: true });
    try {
      const res = await api.post<TokenResponse>("/auth/login", {
        email,
        password,
      });
      localStorage.setItem("access_token", res.data.access_token);
      localStorage.setItem("refresh_token", res.data.refresh_token);
      set({ isAuthenticated: true });

      // Fetch user profile
      const profileRes = await api.get<User>("/profile");
      set({ user: profileRes.data });
    } finally {
      set({ isLoading: false });
    }
  },

  register: async (name, email, password) => {
    set({ isLoading: true });
    try {
      const res = await api.post<TokenResponse>("/auth/register", {
        name,
        email,
        password,
      });
      localStorage.setItem("access_token", res.data.access_token);
      localStorage.setItem("refresh_token", res.data.refresh_token);
      set({ isAuthenticated: true });

      const profileRes = await api.get<User>("/profile");
      set({ user: profileRes.data });
    } finally {
      set({ isLoading: false });
    }
  },

  logout: () => {
    localStorage.removeItem("access_token");
    localStorage.removeItem("refresh_token");
    set({ user: null, isAuthenticated: false });
  },

  fetchProfile: async () => {
    try {
      const res = await api.get<User>("/profile");
      set({ user: res.data, isAuthenticated: true, initialized: true });
    } catch {
      set({ user: null, isAuthenticated: false, initialized: true });
    }
  },
}));
