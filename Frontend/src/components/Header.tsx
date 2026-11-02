import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Home, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CATEGORIES } from "@/constants";
import { clearSession, dashboardPath, getUser } from "@/lib/session";

export const Header = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [name, setName] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);

  useEffect(() => {
    const user = getUser();
    setName(user?.name || null);
    setRole(user?.role || null);
  }, [location.pathname]);

  const logout = () => {
    clearSession();
    setName(null);
    setRole(null);
    navigate("/");
  };

  return (
    <header className="sticky top-0 z-50 border-b border-border/60 bg-background/80 backdrop-blur-md">
      <div className="container mx-auto flex items-center justify-between gap-4 px-4 py-3.5">
        <Link to="/" className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-md">
            <Home className="h-5 w-5" />
          </span>
          <span className="font-display text-xl font-semibold tracking-tight text-foreground">Home Service Hub</span>
        </Link>

        <nav className="flex items-center gap-2">
          <div className="hidden items-center gap-1 md:flex">
            {CATEGORIES.map((category) => (
              <Button key={category.value} asChild variant="ghost" size="sm" className="rounded-full text-muted-foreground hover:text-foreground">
                <Link to={`/?category=${category.value}#search`}>{category.label}</Link>
              </Button>
            ))}
          </div>
          {name ? (
            <>
              <Button asChild variant="outline" size="sm" className="rounded-full">
                <Link to={dashboardPath(role || undefined)}>Dashboard</Link>
              </Button>
              <span className="hidden text-sm text-muted-foreground sm:inline">{name}</span>
              <Button type="button" variant="ghost" size="sm" onClick={logout}>
                <LogOut className="mr-2 h-4 w-4" />
                Logout
              </Button>
            </>
          ) : (
            <Button asChild size="sm" className="rounded-full px-5 shadow-md">
              <Link to="/auth">Sign in</Link>
            </Button>
          )}
        </nav>
      </div>
    </header>
  );
};
