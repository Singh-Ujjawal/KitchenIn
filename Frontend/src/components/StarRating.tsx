import { Star } from "lucide-react";

interface StarRatingProps {
  value: number;
  onChange?: (value: number) => void;
  readOnly?: boolean;
}

export function StarRating({ value, onChange, readOnly = false }: StarRatingProps) {
  return (
    <div className="flex items-center gap-1" role={readOnly ? undefined : "radiogroup"} aria-label="Rating">
      {[1, 2, 3, 4, 5].map((star) => {
        const active = star <= value;
        return (
          <button
            key={star}
            type="button"
            disabled={readOnly}
            onClick={() => onChange?.(star)}
            className="text-amber-500 disabled:cursor-default"
            aria-label={`${star} star${star > 1 ? "s" : ""}`}
          >
            <Star className={`h-5 w-5 ${active ? "fill-current" : ""}`} />
          </button>
        );
      })}
    </div>
  );
}
