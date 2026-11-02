import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { MapPin, Star } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { api, profileImageUrl } from "@/api";
import { categoryLabel } from "@/constants";
import { errorMessage } from "@/lib/errors";
import { getUser } from "@/lib/session";
import { EmptyState } from "@/components/EmptyState";
import { Pager } from "@/components/Pager";
import { ReviewsDialog } from "@/components/ReviewsDialog";
import type { StaffPublic } from "@/types";

const PAGE_SIZE = 6;

const SearchResults = () => {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const city = params.get("city") || "";
  const category = params.get("category") || "";
  const [staff, setStaff] = useState<StaffPublic[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(1);
  const [reviewStaff, setReviewStaff] = useState<StaffPublic | null>(null);

  useEffect(() => {
    const load = async () => {
      if (!city || !category) {
        setLoading(false);
        setError("Choose a city and a service on the home page.");
        return;
      }
      setLoading(true);
      setError("");
      try {
        const response = await api.get<StaffPublic[]>(`/staff/search/${encodeURIComponent(city)}/${encodeURIComponent(category)}`);
        setStaff(response.data);
        setPage(1);
      } catch (err) {
        setError(errorMessage(err, "Search failed"));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [city, category]);

  const book = () => {
    const user = getUser();
    if (!user) {
      toast.error("Sign in as a customer to request this service");
      navigate("/auth");
      return;
    }
    if (user.role !== "CUSTOMER") {
      toast.error("Only customer accounts can request a booking");
      return;
    }
    navigate(`/customer?city=${encodeURIComponent(city)}&category=${encodeURIComponent(category)}`);
    toast.info("Your request is sent to an admin, who assigns an approved professional in this city.");
  };

  const pages = Math.max(1, Math.ceil(staff.length / PAGE_SIZE));
  const visible = staff.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  return (
    <main className="container mx-auto space-y-6 px-4 py-8">
      <div>
        <h1 className="text-3xl font-semibold">Available professionals</h1>
        <p className="mt-1 text-muted-foreground">
          {categoryLabel(category)} in {city || "your city"}. Booking does not pick a person directly. An admin assigns someone approved for this city and service.
        </p>
      </div>
      {loading && <p className="text-sm text-muted-foreground">Searching...</p>}
      {error && <p className="text-sm text-destructive">{error}</p>}
      {!loading && !error && staff.length === 0 && (
        <EmptyState title="No professionals found" description="Try another city or service. Only approved staff are listed." />
      )}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {visible.map((person) => {
          const photo = profileImageUrl(person.profilePicture);
          return (
            <Card key={person.id}>
              {photo && <img src={photo} alt="" className="h-44 w-full object-cover" />}
              <CardContent className="space-y-3 p-5">
                <div className="flex items-start justify-between gap-3">
                  <h2 className="text-lg font-semibold">{person.name}</h2>
                  <p className="flex items-center gap-1 text-sm">
                    <Star className="h-4 w-4 fill-amber-500 text-amber-500" />
                    {person.rating || "New"} ({person.reviewCount || 0})
                  </p>
                </div>
                <p className="flex items-center gap-2 text-sm text-muted-foreground">
                  <MapPin className="h-4 w-4" /> {person.city} · {categoryLabel(person.category)}
                </p>
                <div className="flex gap-2">
                  <Button variant="outline" className="flex-1" onClick={() => setReviewStaff(person)}>Reviews</Button>
                  <Button className="flex-1" onClick={book}>Request booking</Button>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>
      <Pager page={page} pages={pages} onPage={setPage} />
      <ReviewsDialog
        open={Boolean(reviewStaff)}
        staffId={reviewStaff?.id || null}
        staffName={reviewStaff?.name}
        onOpenChange={(open) => !open && setReviewStaff(null)}
      />
    </main>
  );
};

export default SearchResults;
