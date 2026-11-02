import axios, { AxiosError } from "axios";
import { clearSession, getToken } from "@/lib/session";

export const API_BASE_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/$/, "");

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (typeof FormData !== "undefined" && config.data instanceof FormData) {
    delete config.headers["Content-Type"];
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;
    const url = error.config?.url || "";
    const authCall = url.includes("/login") || url.includes("/register");
    if (status === 401 && !authCall && window.location.pathname !== "/auth") {
      clearSession();
      window.location.assign("/auth");
    }
    return Promise.reject(error);
  }
);

export function profileImageUrl(filename?: string | null) {
  if (!filename) return undefined;
  if (filename.startsWith("http://") || filename.startsWith("https://")) return filename;
  return `${API_BASE_URL}/staff/docs/image/${filename}`;
}
