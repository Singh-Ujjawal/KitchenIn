import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Shield, Clock, BadgeCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SearchBar } from "@/components/SearchBar";
import { StaffCard } from "@/components/StaffCard";
import { api } from "@/api";
import { CATEGORIES } from "@/constants";
import type { StaffPublic } from "@/types";

const Index = () => {
  const [params] = useSearchParams();
  const [staff, setStaff] = useState<StaffPublic[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const response = await api.get<StaffPublic[]>("/staff/top");
        setStaff(response.data);
      } catch {
        setError("Approved professionals could not be loaded right now.");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  return (
    <main>
      <section className="hero-glow border-b border-border">
        <div className="container mx-auto px-4 py-20 md:py-28">
          <div className="fade-up mx-auto mb-12 max-w-3xl text-center">
            <p className="mb-5 inline-block rounded-full border border-primary/20 bg-primary/10 px-4 py-1.5 text-xs font-medium uppercase tracking-widest text-primary">Home services, locally assigned</p>
            <h1 className="text-4xl font-semibold leading-tight tracking-tight text-foreground md:text-6xl">
              Book a trusted professional for your home
            </h1>
            <p className="mt-5 text-lg leading-relaxed text-muted-foreground">
              Search approved maids, mechanics, and workers in your city. An admin confirms the booking, the price, and the person who visits.
            </p>
          </div>
          <SearchBar initialCategory={params.get("category") || ""} />
        </div>
      </section>

      <section className="container mx-auto grid gap-6 px-4 py-14 md:grid-cols-3">
        {[
          { icon: Shield, title: "Approved before they work", text: "Workers can sign in only after an administrator reviews their account." },
          { icon: Clock, title: "A clear booking time", text: "Choose a date and a morning, afternoon, or evening visit." },
          { icon: BadgeCheck, title: "One review per finished job", text: "Ratings come from completed bookings, not from sample profiles." },
        ].map((item) => (
          <div key={item.title} className="lift rounded-2xl border border-border bg-card p-7">
            <span className="mb-5 flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10"><item.icon className="h-5 w-5 text-primary" /></span>
            <h2 className="font-semibold">{item.title}</h2>
            <p className="mt-2 text-sm text-muted-foreground">{item.text}</p>
          </div>
        ))}
      </section>

      <section className="container mx-auto px-4 pb-14">
        <h2 className="text-3xl font-semibold">Services</h2>
        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {CATEGORIES.map((category) => (
            <Link
              key={category.value}
              to={`/?category=${category.value}#search`}
              className="lift group rounded-2xl border border-border bg-card p-6"
            >
              <h3 className="text-lg font-semibold group-hover:text-primary">{category.label}</h3>
              <p className="mt-2 text-sm text-muted-foreground">{category.description}</p>
            </Link>
          ))}
        </div>
      </section>

      <section className="container mx-auto px-4 pb-16">
        <div className="mb-6 flex items-end justify-between gap-4">
          <div>
            <h2 className="text-3xl font-semibold">Approved professionals</h2>
            <p className="mt-1 text-sm text-muted-foreground">Shown from the live directory, ordered by rating.</p>
          </div>
          <Button asChild variant="outline" className="rounded-full"><Link to="/auth">Create an account</Link></Button>
        </div>
        {loading && <p className="text-sm text-muted-foreground">Loading professionals...</p>}
        {error && <p className="text-sm text-destructive">{error}</p>}
        {!loading && !error && staff.length === 0 && (
          <p className="rounded-2xl border border-dashed bg-card/50 border-border px-6 py-10 text-center text-sm text-muted-foreground">
            No approved professionals yet. New workers appear here after an admin approves them.
          </p>
        )}
        <div className="grid gap-4 md:grid-cols-3">
          {staff.map((person) => <StaffCard key={person.id} staff={person} />)}
        </div>
      </section>
    </main>
  );
};

export default Index;
