import type { UserProfile } from "@/types";

const TOKEN_KEY = "token";
const USER_KEY = "user";
const ROLE_KEY = "role";

export function saveSession(token: string, user: UserProfile) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  localStorage.setItem(ROLE_KEY, user.role);
}

export function updateStoredUser(user: UserProfile) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  localStorage.setItem(ROLE_KEY, user.role);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(ROLE_KEY);
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getUser(): UserProfile | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    const user = JSON.parse(raw) as UserProfile;
    if (!user?.id || !user.role) return null;
    return user;
  } catch {
    clearSession();
    return null;
  }
}

export function dashboardPath(role?: string) {
  if (role === "ADMIN") return "/admin";
  if (role === "WORKER") return "/staff";
  return "/customer";
}
