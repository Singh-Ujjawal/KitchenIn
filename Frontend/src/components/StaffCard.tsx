import { Link } from "react-router-dom";
import { MapPin, Star } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { profileImageUrl } from "@/api";
import { categoryLabel } from "@/constants";
import type { StaffPublic } from "@/types";

export const StaffCard = ({ staff }: { staff: StaffPublic }) => {
  const photo = profileImageUrl(staff.profilePicture);
  return (
    <Card className="lift h-full overflow-hidden rounded-2xl">
      <CardContent className="flex gap-4 p-5">
        {photo ? (
          <img src={photo} alt="" className="h-16 w-16 rounded-xl object-cover" />
        ) : (
          <div className="flex h-16 w-16 items-center justify-center rounded-xl bg-primary/10 font-display text-xl font-semibold text-primary">
            {staff.name.slice(0, 1)}
          </div>
        )}
        <div className="min-w-0">
          <h3 className="truncate font-semibold">{staff.name}</h3>
          <p className="mt-1 text-sm text-muted-foreground">{categoryLabel(staff.category)}</p>
          <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
            <MapPin className="h-3.5 w-3.5" /> {staff.city}
          </p>
          <p className="mt-2 flex items-center gap-1 text-sm">
            <Star className="h-4 w-4 fill-amber-500 text-amber-500" />
            <span className="font-medium">{staff.rating || "New"}</span>
            <span className="text-muted-foreground">({staff.reviewCount || 0})</span>
          </p>
          <Link
            className="mt-3 inline-block text-sm font-medium text-primary hover:underline"
            to={`/search?city=${encodeURIComponent(staff.city)}&category=${encodeURIComponent(staff.category)}`}
          >
            View in {staff.city}
          </Link>
        </div>
      </CardContent>
    </Card>
  );
};
