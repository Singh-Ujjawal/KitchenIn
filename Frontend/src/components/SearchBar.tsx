import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { CATEGORIES, CITIES } from "@/constants";

interface SearchBarProps {
  initialCity?: string;
  initialCategory?: string;
}

export const SearchBar = ({ initialCity = "", initialCategory = "" }: SearchBarProps) => {
  const [city, setCity] = useState(initialCity);
  const [category, setCategory] = useState(initialCategory);
  const navigate = useNavigate();

  useEffect(() => {
    if (initialCategory) setCategory(initialCategory);
  }, [initialCategory]);

  const search = () => {
    if (!city || !category) return;
    navigate(`/search?city=${encodeURIComponent(city)}&category=${encodeURIComponent(category)}`);
  };

  return (
    <div id="search" className="mx-auto max-w-4xl rounded-2xl border border-border bg-card p-4 shadow-lg md:p-6">
      <div className="grid gap-4 md:grid-cols-[1fr_1fr_auto]">
        <div>
          <label className="mb-2 block text-xs font-medium uppercase tracking-wider text-muted-foreground">City</label>
          <Select value={city || undefined} onValueChange={setCity}>
            <SelectTrigger><SelectValue placeholder="Choose your city" /></SelectTrigger>
            <SelectContent>
              {CITIES.map((item) => (
                <SelectItem key={item} value={item}>{item}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div>
          <label className="mb-2 block text-xs font-medium uppercase tracking-wider text-muted-foreground">Service</label>
          <Select value={category || undefined} onValueChange={setCategory}>
            <SelectTrigger><SelectValue placeholder="What do you need?" /></SelectTrigger>
            <SelectContent>
              {CATEGORIES.map((item) => (
                <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="flex items-end">
          <Button type="button" className="w-full rounded-xl md:w-auto" size="lg" disabled={!city || !category} onClick={search}>
            <Search className="mr-2 h-4 w-4" />
            Search
          </Button>
        </div>
      </div>
    </div>
  );
};
