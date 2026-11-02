import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { api } from "@/api";
import { CITIES, categoryLabel, cityFromText } from "@/constants";
import { errorMessage } from "@/lib/errors";
import { formatDate, formatMoney } from "@/lib/format";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { EmptyState } from "@/components/EmptyState";
import { Pager } from "@/components/Pager";
import type { Booking, DashboardStats, StaffPublic, UserProfile } from "@/types";

const PAGE_SIZE = 5;
const emptyStats: DashboardStats = {
  totalCustomers: 0,
  totalStaff: 0,
  activeStaff: 0,
  pendingApprovals: 0,
  totalBookings: 0,
  pendingBookings: 0,
  assignedBookings: 0,
  completedBookings: 0,
  cancelledBookings: 0,
  totalRevenue: 0,
};

const AdminDashboard = () => {
  const [stats, setStats] = useState<DashboardStats>(emptyStats);
  const [pendingStaff, setPendingStaff] = useState<UserProfile[]>([]);
  const [pendingBookings, setPendingBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [staffPage, setStaffPage] = useState(1);
  const [bookingPage, setBookingPage] = useState(1);
  const [rejectId, setRejectId] = useState<string | null>(null);
  const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null);
  const [availableStaff, setAvailableStaff] = useState<StaffPublic[]>([]);
  const [selectedStaffId, setSelectedStaffId] = useState("");
  const [assignCity, setAssignCity] = useState("");
  const [price, setPrice] = useState("");

  const load = async () => {
    const [statsRes, staffRes, bookingRes] = await Promise.all([
      api.get<DashboardStats>("/admin/dashboard/stats"),
      api.get<UserProfile[]>("/staff/pending"),
      api.get<Booking[]>("/bookings/pending"),
    ]);
    setStats(statsRes.data);
    setPendingStaff(staffRes.data);
    setPendingBookings(bookingRes.data);
  };

  useEffect(() => {
    load()
      .catch((error) => toast.error(errorMessage(error, "Could not load the dashboard")))
      .finally(() => setLoading(false));
  }, []);

  const staffPages = Math.max(1, Math.ceil(pendingStaff.length / PAGE_SIZE));
  const bookingPages = Math.max(1, Math.ceil(pendingBookings.length / PAGE_SIZE));
  const visibleStaff = useMemo(
    () => pendingStaff.slice((staffPage - 1) * PAGE_SIZE, staffPage * PAGE_SIZE),
    [pendingStaff, staffPage]
  );
  const visibleBookings = useMemo(
    () => pendingBookings.slice((bookingPage - 1) * PAGE_SIZE, bookingPage * PAGE_SIZE),
    [pendingBookings, bookingPage]
  );

  const approve = async (id: string) => {
    setSubmitting(true);
    try {
      await api.post(`/staff/approve/${id}`);
      toast.success("Staff approved");
      await load();
    } catch (error) {
      toast.error(errorMessage(error, "Could not approve staff"));
    } finally {
      setSubmitting(false);
    }
  };

  const reject = async () => {
    if (!rejectId) return;
    setSubmitting(true);
    try {
      await api.delete(`/staff/remove/${rejectId}`);
      toast.success("Staff application removed");
      setRejectId(null);
      await load();
    } catch (error) {
      toast.error(errorMessage(error, "Could not reject staff"));
    } finally {
      setSubmitting(false);
    }
  };

  const openIdentity = async (filename?: string) => {
    if (!filename) {
      toast.error("No identity document uploaded");
      return;
    }
    try {
      const response = await api.get(`/staff/docs/identity/${filename}`, { responseType: "blob" });
      const url = URL.createObjectURL(response.data);
      window.open(url, "_blank", "noopener,noreferrer");
    } catch (error) {
      toast.error(errorMessage(error, "Could not open the document"));
    }
  };

  const openAssign = async (booking: Booking) => {
    const city = booking.city || cityFromText(booking.serviceAddress);
    setSelectedBooking(booking);
    setAssignCity(city);
    setSelectedStaffId("");
    setPrice("");
    setAvailableStaff([]);
    if (city) await fetchStaff(city, booking.category);
  };

  const fetchStaff = async (city: string, category: string) => {
    try {
      const response = await api.get<StaffPublic[]>(`/staff/search/${encodeURIComponent(city)}/${category}`);
      setAvailableStaff(response.data);
    } catch (error) {
      toast.error(errorMessage(error, "Could not load matching staff"));
    }
  };

  const assign = async () => {
    const amount = Number(price);
    if (!selectedBooking || !selectedStaffId || !Number.isFinite(amount) || amount <= 0) {
      toast.error("Choose a staff member and enter a price greater than 0");
      return;
    }
    setSubmitting(true);
    try {
      await api.post("/bookings/assign", {
        bookingId: selectedBooking.id,
        staffId: selectedStaffId,
        price: amount,
      });
      toast.success("Staff assigned");
      setSelectedBooking(null);
      await load();
    } catch (error) {
      toast.error(errorMessage(error, "Assignment failed"));
    } finally {
      setSubmitting(false);
    }
  };

  const cards = [
    ["Customers", stats.totalCustomers],
    ["Staff", stats.totalStaff],
    ["Approved staff", stats.activeStaff],
    ["Pending staff", stats.pendingApprovals],
    ["Bookings", stats.totalBookings],
    ["Pending", stats.pendingBookings],
    ["Assigned", stats.assignedBookings],
    ["Completed", stats.completedBookings],
    ["Cancelled", stats.cancelledBookings],
    ["Revenue", formatMoney(stats.totalRevenue)],
  ] as const;

  return (
    <main className="container mx-auto space-y-8 px-4 py-8">
      <div>
        <h1 className="text-3xl font-semibold">Admin</h1>
        <p className="mt-1 text-muted-foreground">Approve staff, then assign them only when city and category match.</p>
      </div>

      {loading ? (
        <p className="text-sm text-muted-foreground">Loading dashboard...</p>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          {cards.map(([label, value]) => (
            <Card key={label}>
              <CardContent className="p-4">
                <p className="text-sm text-muted-foreground">{label}</p>
                <p className="mt-1 text-2xl font-semibold">{value}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <section className="space-y-4">
        <h2 className="text-xl font-semibold">Staff approval</h2>
        {pendingStaff.length === 0 ? (
          <EmptyState title="No applications waiting" description="New staff registrations appear here until you approve or reject them." />
        ) : visibleStaff.map((staff) => (
          <Card key={staff.id}>
            <CardContent className="flex flex-col justify-between gap-4 p-5 md:flex-row md:items-center">
              <div>
                <h3 className="font-semibold">{staff.name}</h3>
                <p className="text-sm text-muted-foreground">
                  {categoryLabel(staff.category)} · {staff.city} · {staff.phone} · {staff.email}
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                <Button variant="outline" onClick={() => openIdentity(staff.identityProof)}>View ID</Button>
                <Button variant="destructive" disabled={submitting} onClick={() => setRejectId(staff.id)}>Reject</Button>
                <Button disabled={submitting} onClick={() => approve(staff.id)}>Approve</Button>
              </div>
            </CardContent>
          </Card>
        ))}
        <Pager page={staffPage} pages={staffPages} onPage={setStaffPage} />
      </section>

      <section className="space-y-4">
        <h2 className="text-xl font-semibold">Pending bookings</h2>
        {pendingBookings.length === 0 ? (
          <EmptyState title="No pending bookings" description="New customer requests show up here for assignment." />
        ) : visibleBookings.map((booking) => (
          <Card key={booking.id}>
            <CardContent className="flex flex-col justify-between gap-4 p-5 md:flex-row md:items-start">
              <div className="space-y-1 text-sm">
                <h3 className="text-base font-semibold">{categoryLabel(booking.category)}</h3>
                <p>{booking.customerName}{booking.customerPhone ? ` · ${booking.customerPhone}` : ""}</p>
                <p className="text-muted-foreground">{formatDate(booking.bookingDate)} · {booking.timeSlot}</p>
                <p className="text-muted-foreground">{booking.serviceAddress}{booking.city ? `, ${booking.city}` : ""}</p>
                <p>{booking.problemDescription}</p>
              </div>
              <Button onClick={() => openAssign(booking)}>Assign staff</Button>
            </CardContent>
          </Card>
        ))}
        <Pager page={bookingPage} pages={bookingPages} onPage={setBookingPage} />
      </section>

      <Dialog open={Boolean(selectedBooking)} onOpenChange={(open) => !open && setSelectedBooking(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Assign staff</DialogTitle>
            <DialogDescription>
              Only approved {categoryLabel(selectedBooking?.category).toLowerCase()} professionals in the booking city can be selected.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            {!selectedBooking?.city && (
              <div className="space-y-2">
                <Label>City</Label>
                <Select value={assignCity || undefined} onValueChange={(value) => {
                  setAssignCity(value);
                  setSelectedStaffId("");
                  if (selectedBooking) fetchStaff(value, selectedBooking.category);
                }}>
                  <SelectTrigger><SelectValue placeholder="Match a city" /></SelectTrigger>
                  <SelectContent>{CITIES.map((city) => <SelectItem key={city} value={city}>{city}</SelectItem>)}</SelectContent>
                </Select>
              </div>
            )}
            <div className="space-y-2">
              <Label>Staff</Label>
              <Select value={selectedStaffId || undefined} onValueChange={setSelectedStaffId}>
                <SelectTrigger><SelectValue placeholder="Choose a staff member" /></SelectTrigger>
                <SelectContent>
                  {availableStaff.length === 0 ? (
                    <SelectItem value="none" disabled>No approved staff in this city and category</SelectItem>
                  ) : availableStaff.map((staff) => (
                    <SelectItem key={staff.id} value={staff.id}>
                      {staff.name} · {staff.city} · {staff.rating || "New"}★
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="price">Price (₹)</Label>
              <Input id="price" type="number" min="1" step="1" value={price} onChange={(event) => setPrice(event.target.value)} />
            </div>
            <Button className="w-full" disabled={submitting} onClick={assign}>
              {submitting ? "Please wait..." : "Confirm assignment"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={Boolean(rejectId)}
        title="Reject this application?"
        description="The staff account is deleted. This is blocked if the person already has bookings."
        confirmLabel="Reject application"
        destructive
        loading={submitting}
        onOpenChange={(open) => !open && setRejectId(null)}
        onConfirm={reject}
      />
    </main>
  );
};

export default AdminDashboard;
