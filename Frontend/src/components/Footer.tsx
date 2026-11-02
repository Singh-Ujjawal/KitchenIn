import { Link } from "react-router-dom";
import { Home } from "lucide-react";

export function Footer() {
  return (
    <footer className="mt-auto bg-foreground text-background/80">
      <div className="container mx-auto grid gap-8 px-4 py-12 md:grid-cols-[2fr_1fr_1fr]">
        <div>
          <p className="flex items-center gap-2 font-display text-xl font-semibold text-background">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent text-accent-foreground">
              <Home className="h-4 w-4" />
            </span>
            Home Service Hub
          </p>
          <p className="mt-3 max-w-sm text-sm">Verified home professionals, booked through a local admin.</p>
        </div>
        <div className="text-sm">
          <p className="mb-3 font-medium text-background">Explore</p>
          <div className="flex flex-col gap-2">
            <Link to="/" className="hover:text-background">Home</Link>
            <Link to="/auth" className="hover:text-background">Sign in</Link>
          </div>
        </div>
        <p className="text-sm md:text-right">&copy; {new Date().getFullYear()} Home Service Hub</p>
      </div>
    </footer>
  );
}
