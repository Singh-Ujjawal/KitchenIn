import { Navigate } from "react-router-dom";
import { getToken, getUser } from "@/lib/session";
import type { UserRole } from "@/types";

export function RequireAuth({ role, children }: { role: UserRole; children: React.ReactNode }) {
  const token = getToken();
  const user = getUser();
  if (!token || !user) return <Navigate to="/auth" replace />;
  if (user.role !== role) return <Navigate to="/unauthorized" replace />;
  return <>{children}</>;
}
