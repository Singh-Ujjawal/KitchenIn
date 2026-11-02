import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Calendar, Clock, MapPin, Phone, Star } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { api, profileImageUrl } from "@/api";
import { CATEGORIES, CITIES, TIME_SLOTS, categoryLabel } from "@/constants";
import { errorMessage } from "@/lib/errors";
import { formatDate, formatMoney, todayInputValue } from "@/lib/format";
import { getUser } from "@/lib/session";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { EmptyState } from "@/components/EmptyState";
import { Pager } from "@/components/Pager";
import { StarRating } from "@/components/StarRating";
import { StatusBadge } from "@/components/StatusBadge";
import type { Booking } from "@/types";

const PAGE_SIZE = 5;

const CustomerDashboard = () => {
  const user = getUser();
  const [params] = useSearchParams();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(1);
  const [city, setCity] = useState(params.get("city") || "");
  const [category, setCategory] = useState(params.get("category") || "");
  const [date, setDate] = useState("");
  const [timeSlot, setTimeSlot] = useState("");
  const [address, setAddress] = useState(user?.address || "");
  const [description, setDescription] = useState("");
  const [cancelId, setCancelId] = useState<string | null>(null);
  const [reviewBooking, setReviewBooking] = useState<Booking | null>(null);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");

  const load = async () => {
    if (!user) return;
    const response = await api.get<Booking[]>(`/bookings/history/${user.id}`);
    setBookings(response.data);
  };

  useEffect(() => {
    const current = getUser();
    if (!current) {
      setLoading(false);
      return;
    }
    api.get<Booking[]>(`/bookings/history/${current.id}`)
      .then((response) => setBookings(response.data))
      .catch((error) => toast.error(errorMessage(error, "Could not load your bookings")))
      .finally(() => setLoading(false));
  }, []);

  const pages = Math.max(1, Math.ceil(bookings.length / PAGE_SIZE));
  const visible = useMemo(
    () => bookings.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [bookings, page]
  );

  const book = async () => {
    if (!city || !category || !date || !timeSlot || !address.trim() || !description.trim()) {
      toast.error("Fill in city, service, date, time, address, and the problem");
      return;
    }
    setSubmitting(true);
    try {
      await api.post("/bookings/book", {
        category,
        city,
        bookingDate: date,
        timeSlot,
        serviceAddress: address.trim(),
        problemDescription: description.trim(),
      });
      toast.success("Booking request sent. An admin will assign a professional.");
      setDescription("");
      setDate("");
      await load();
      setPage(1);
    } catch (error) {
      toast.error(errorMessage(error, "Booking failed"));
    } finally {
      setSubmitting(false);
    }
  };

  const cancel = async () => {
    if (!cancelId) return;
    setSubmitting(true);
    try {
      await api.post(`/bookings/cancel/${cancelId}`);
      toast.success("Booking cancelled");
      setCancelId(null);
      await load();
    } catch (error) {
      toast.error(errorMessage(error, "Could not cancel the booking"));
    } finally {
      setSubmitting(false);
    }
  };

  const submitReview = async () => {
    if (!reviewBooking) return;
    setSubmitting(true);
    try {
      await api.post("/reviews/add", { bookingId: reviewBooking.id, rating, comment: comment.trim() });
      toast.success("Review saved");
      setReviewBooking(null);
      setComment("");
      setRating(5);
      await load();
    } catch (error) {
      toast.error(errorMessage(error, "Could not save the review"));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="container mx-auto space-y-8 px-4 py-8">
      <div>
        <h1 className="text-3xl font-semibold">Hello, {user?.name}</h1>
        <p className="mt-1 text-muted-foreground">Request a visit. An admin assigns an approved professional in your city.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Book a service</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Label>City</Label>
            <Select value={city || undefined} onValueChange={setCity}>
              <SelectTrigger><SelectValue placeholder="Select city" /></SelectTrigger>
              <SelectContent>{CITIES.map((item) => <SelectItem key={item} value={item}>{item}</SelectItem>)}</SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Service</Label>
            <Select value={category || undefined} onValueChange={setCategory}>
              <SelectTrigger><SelectValue placeholder="Select service" /></SelectTrigger>
              <SelectContent>{CATEGORIES.map((item) => <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>)}</SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="date">Date</Label>
            <Input id="date" type="date" min={todayInputValue()} value={date} onChange={(event) => setDate(event.target.value)} />
          </div>
          <div className="space-y-2">
            <Label>Time</Label>
            <Select value={timeSlot || undefined} onValueChange={setTimeSlot}>
              <SelectTrigger><SelectValue placeholder="Select time" /></SelectTrigger>
              <SelectContent>{TIME_SLOTS.map((item) => <SelectItem key={item} value={item}>{item}</SelectItem>)}</SelectContent>
            </Select>
          </div>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="address">Address</Label>
            <Input id="address" value={address} onChange={(event) => setAddress(event.target.value)} placeholder="House, street, area" />
          </div>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="problem">What needs to be done?</Label>
            <Textarea id="problem" value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Describe the job" />
          </div>
          <Button className="md:col-span-2" disabled={submitting} onClick={book}>
            {submitting ? "Please wait..." : "Submit booking request"}
          </Button>
        </CardContent>
      </Card>

      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Your bookings</h2>
        {loading && <p className="text-sm text-muted-foreground">Loading bookings...</p>}
        {!loading && bookings.length === 0 && (
          <EmptyState title="No bookings yet" description="Book your first home service using the form above." />
        )}
        {visible.map((booking) => {
          const photo = profileImageUrl(booking.staffProfilePicture);
          return (
            <Card key={booking.id}>
              <CardContent className="space-y-4 p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-semibold">{categoryLabel(booking.category)}</h3>
                    <p className="text-sm text-muted-foreground">{booking.city}</p>
                  </div>
                  <StatusBadge status={booking.status} />
                </div>
                <div className="grid gap-2 text-sm text-muted-foreground md:grid-cols-2">
                  <p className="flex items-center gap-2"><Calendar className="h-4 w-4" /> {formatDate(booking.bookingDate)}</p>
                  <p className="flex items-center gap-2"><Clock className="h-4 w-4" /> {booking.timeSlot}</p>
                  <p className="flex items-center gap-2 md:col-span-2"><MapPin className="h-4 w-4" /> {booking.serviceAddress}</p>
                </div>
                <p className="text-sm">{booking.problemDescription}</p>
                <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4">
                  <div className="text-sm">
                    <p className="font-medium">{booking.staffName || "Waiting for assignment"}</p>
                    {booking.staffName && (
                      <p className="mt-1 flex items-center gap-3 text-muted-foreground">
                        {photo && <img src={photo} alt="" className="h-8 w-8 rounded-md object-cover" />}
                        <span className="flex items-center gap-1"><Star className="h-3.5 w-3.5 fill-amber-500 text-amber-500" /> {booking.staffRating || "New"}</span>
                        {booking.staffPhone && <span className="flex items-center gap-1"><Phone className="h-3.5 w-3.5" /> {booking.staffPhone}</span>}
                      </p>
                    )}
                    <p className="mt-1">Price: {formatMoney(booking.price)}</p>
                  </div>
                  <div className="flex gap-2">
                    {(booking.status === "PENDING" || booking.status === "ASSIGNED") && (
                      <Button variant="destructive" size="sm" onClick={() => setCancelId(booking.id)}>Cancel</Button>
                    )}
                    {booking.status === "COMPLETED" && !booking.reviewed && (
                      <Button size="sm" onClick={() => { setReviewBooking(booking); setRating(5); setComment(""); }}>Leave review</Button>
                    )}
                    {booking.reviewed && <span className="text-sm text-muted-foreground">Reviewed</span>}
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
        <Pager page={page} pages={pages} onPage={setPage} />
      </section>

      <ConfirmDialog
        open={Boolean(cancelId)}
        title="Cancel this booking?"
        description="Pending and assigned bookings can be cancelled. A completed booking cannot."
        confirmLabel="Cancel booking"
        destructive
        loading={submitting}
        onOpenChange={(open) => !open && setCancelId(null)}
        onConfirm={cancel}
      />

      <Dialog open={Boolean(reviewBooking)} onOpenChange={(open) => !open && setReviewBooking(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Review {reviewBooking?.staffName || "this visit"}</DialogTitle>
            <DialogDescription>One review is saved for each completed booking.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <StarRating value={rating} onChange={setRating} />
            <Textarea value={comment} maxLength={500} onChange={(event) => setComment(event.target.value)} placeholder="Optional comment" />
            <Button className="w-full" disabled={submitting} onClick={submitReview}>
              {submitting ? "Please wait..." : "Submit review"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </main>
  );
};

export default CustomerDashboard;
