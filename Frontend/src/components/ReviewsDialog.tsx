import { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { api } from "@/api";
import { errorMessage } from "@/lib/errors";
import { formatDate } from "@/lib/format";
import { StarRating } from "@/components/StarRating";
import type { Review } from "@/types";

interface ReviewsDialogProps {
  staffId: string | null;
  staffName?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ReviewsDialog({ staffId, staffName, open, onOpenChange }: ReviewsDialogProps) {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open || !staffId) return;
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const response = await api.get<Review[]>(`/reviews/${staffId}`);
        setReviews(response.data);
      } catch (err) {
        setError(errorMessage(err, "Could not load reviews"));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [open, staffId]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Reviews {staffName ? `for ${staffName}` : ""}</DialogTitle>
          <DialogDescription>Comments from customers after completed bookings.</DialogDescription>
        </DialogHeader>
        {loading && <p className="text-sm text-muted-foreground">Loading reviews...</p>}
        {error && <p className="text-sm text-destructive">{error}</p>}
        {!loading && !error && reviews.length === 0 && (
          <p className="text-sm text-muted-foreground">No reviews yet.</p>
        )}
        <div className="space-y-4">
          {reviews.map((review) => (
            <div key={review.id} className="rounded-md border border-border p-3">
              <div className="flex items-center justify-between gap-3">
                <p className="font-medium">{review.customerName || "Customer"}</p>
                <StarRating value={review.rating} readOnly />
              </div>
              {review.comment && <p className="mt-2 text-sm text-muted-foreground">{review.comment}</p>}
              <p className="mt-2 text-xs text-muted-foreground">{formatDate(review.createdAt)}</p>
            </div>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}
