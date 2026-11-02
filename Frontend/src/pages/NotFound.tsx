import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

const NotFound = () => (
  <main className="container mx-auto px-4 py-24 text-center">
    <p className="text-sm font-medium text-primary">404</p>
    <h1 className="mt-2 text-3xl font-semibold">This page does not exist</h1>
    <p className="mt-2 text-muted-foreground">The link may be old. Head back to the home page to search for a service.</p>
    <Button asChild className="mt-6"><Link to="/">Back to home</Link></Button>
  </main>
);

export default NotFound;
