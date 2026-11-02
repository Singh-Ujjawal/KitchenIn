import { Button } from "@/components/ui/button";

interface PagerProps {
  page: number;
  pages: number;
  onPage: (page: number) => void;
}

export function Pager({ page, pages, onPage }: PagerProps) {
  if (pages <= 1) return null;
  return (
    <div className="flex items-center justify-between gap-3 pt-2">
      <p className="text-sm text-muted-foreground">Page {page} of {pages}</p>
      <div className="flex gap-2">
        <Button type="button" variant="outline" size="sm" disabled={page <= 1} onClick={() => onPage(page - 1)}>
          Previous
        </Button>
        <Button type="button" variant="outline" size="sm" disabled={page >= pages} onClick={() => onPage(page + 1)}>
          Next
        </Button>
      </div>
    </div>
  );
}
