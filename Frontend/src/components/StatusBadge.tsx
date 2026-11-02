import { Badge } from "@/components/ui/badge";
import type { BookingStatus } from "@/types";

const styles: Record<string, string> = {
  PENDING: "bg-amber-100 text-amber-900 hover:bg-amber-100",
  ASSIGNED: "bg-sky-100 text-sky-900 hover:bg-sky-100",
  COMPLETED: "bg-emerald-100 text-emerald-900 hover:bg-emerald-100",
  CANCELLED: "bg-red-100 text-red-800 hover:bg-red-100",
  CONFIRMED: "bg-sky-100 text-sky-900 hover:bg-sky-100",
};

export const StatusBadge = ({ status }: { status: BookingStatus | string }) => (
  <Badge className={styles[status] || "bg-muted text-muted-foreground"}>{status}</Badge>
);
