import { useEffect, useMemo, useState } from "react";
import { Calendar, Clock, MapPin, Phone } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, profileImageUrl } from "@/api";
import { categoryLabel } from "@/constants";
import { errorMessage } from "@/lib/errors";
import { formatDate, formatMoney } from "@/lib/format";
import { getToken, getUser, updateStoredUser } from "@/lib/session";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { EmptyState } from "@/components/EmptyState";
import { Pager } from "@/components/Pager";
import { StatusBadge } from "@/components/StatusBadge";
import type { Booking, UserProfile } from "@/types";

const PAGE_SIZE = 5;

const StaffDashboard = () => {
  const [user, setUser] = useState<UserProfile | null>(getUser());
  const [jobs, setJobs] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(1);
  const [completeId, setCompleteId] = useState<string | null>(null);
  const [profileFile, setProfileFile] = useState<File | null>(null);
  const [identityFile, setIdentityFile] = useState<File | null>(null);

  const load = async (staffId: string) => {
    const response = await api.get<Booking[]>(`/bookings/staff/${staffId}`);
    setJobs(response.data);
  };

  useEffect(() => {
    const current = getUser();
    if (!current) {
      setLoading(false);
      return;
    }
    api.get<Booking[]>(`/bookings/staff/${current.id}`)
      .then((response) => setJobs(response.data))
      .catch((error) => toast.error(errorMessage(error, "Could not load your jobs")))
      .finally(() => setLoading(false));
  }, []);

  const pages = Math.max(1, Math.ceil(jobs.length / PAGE_SIZE));
  const visible = useMemo(() => jobs.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE), [jobs, page]);
  const photo = profileImageUrl(user?.profilePicture);

  const upload = async () => {
    if (!user || (!profileFile && !identityFile)) {
      toast.error("Choose a profile photo or an identity document");
      return;
    }
    setSubmitting(true);
    try {
      const form = new FormData();
      if (profileFile) form.append("profilePic", profileFile);
      if (identityFile) form.append("identityProof", identityFile);
      const response = await api.post<UserProfile>(`/staff/docs/upload/${user.id}`, form);
      updateStoredUser(response.data);
      setUser(response.data);
      setProfileFile(null);
      setIdentityFile(null);
      toast.success("Documents saved");
    } catch (error) {
      toast.error(errorMessage(error, "Upload failed"));
    } finally {
      setSubmitting(false);
    }
  };

  const complete = async () => {
    if (!completeId || !user) return;
    setSubmitting(true);
    try {
      await api.post(`/bookings/complete/${completeId}`);
      toast.success("Job marked completed");
      setCompleteId(null);
      await load(user.id);
    } catch (error) {
      toast.error(errorMessage(error, "Could not complete the job"));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="container mx-auto space-y-8 px-4 py-8">
      <Card>
        <CardContent className="flex flex-col gap-6 p-6 md:flex-row md:items-center">
          {photo ? (
            <img src={photo} alt="" className="h-20 w-20 rounded-md object-cover" />
          ) : (
            <div className="flex h-20 w-20 items-center justify-center rounded-md bg-muted text-2xl font-semibold">
              {user?.name?.slice(0, 1)}
            </div>
          )}
          <div className="flex-1">
            <h1 className="text-2xl font-semibold">{user?.name}</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {categoryLabel(user?.category)} · {user?.city} · {user?.approved ? "Approved" : "Pending approval"}
            </p>
            <p className="mt-1 text-sm">Rating {user?.rating || "New"} · {user?.reviewCount || 0} reviews</p>
            <p className="mt-1 text-sm text-muted-foreground">{user?.identityProof ? "Identity document uploaded" : "Identity document not uploaded yet"}</p>
          </div>
          <div className="grid w-full gap-3 md:w-80">
            <div className="space-y-1">
              <Label htmlFor="profile">Profile photo</Label>
              <Input id="profile" type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => setProfileFile(event.target.files?.[0] || null)} />
            </div>
            <div className="space-y-1">
              <Label htmlFor="identity">Identity document</Label>
              <Input id="identity" type="file" accept="image/jpeg,image/png,application/pdf" onChange={(event) => setIdentityFile(event.target.files?.[0] || null)} />
            </div>
            <Button disabled={submitting || !getToken()} onClick={upload}>{submitting ? "Please wait..." : "Upload documents"}</Button>
          </div>
        </CardContent>
      </Card>

      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Assigned jobs</h2>
        {loading && <p className="text-sm text-muted-foreground">Loading jobs...</p>}
        {!loading && jobs.length === 0 && (
          <EmptyState title="No jobs assigned" description="When an admin assigns you a booking in your city and category, it will show up here." />
        )}
        {visible.map((job) => (
          <Card key={job.id}>
            <CardContent className="space-y-4 p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h3 className="text-lg font-semibold">{categoryLabel(job.category)}</h3>
                  <p className="text-sm text-muted-foreground">{job.customerName || "Customer"}</p>
                </div>
                <StatusBadge status={job.status} />
              </div>
              <p className="text-sm">{job.problemDescription}</p>
              <div className="grid gap-2 text-sm text-muted-foreground md:grid-cols-2">
                <p className="flex items-center gap-2"><Calendar className="h-4 w-4" /> {formatDate(job.bookingDate)}</p>
                <p className="flex items-center gap-2"><Clock className="h-4 w-4" /> {job.timeSlot}</p>
                <p className="flex items-center gap-2"><MapPin className="h-4 w-4" /> {job.serviceAddress}{job.city ? `, ${job.city}` : ""}</p>
                {job.customerPhone && <p className="flex items-center gap-2"><Phone className="h-4 w-4" /> {job.customerPhone}</p>}
              </div>
              <div className="flex items-center justify-between gap-3 border-t border-border pt-4">
                <p className="text-sm font-medium">Agreed price: {formatMoney(job.price)}</p>
                {job.status === "ASSIGNED" && (
                  <Button size="sm" onClick={() => setCompleteId(job.id)}>Mark completed</Button>
                )}
              </div>
            </CardContent>
          </Card>
        ))}
        <Pager page={page} pages={pages} onPage={setPage} />
      </section>

      <ConfirmDialog
        open={Boolean(completeId)}
        title="Mark this job completed?"
        description="Only the assigned staff member can complete an assigned booking. This cannot be cancelled afterwards."
        confirmLabel="Mark completed"
        loading={submitting}
        onOpenChange={(open) => !open && setCompleteId(null)}
        onConfirm={complete}
      />
    </main>
  );
};

export default StaffDashboard;
