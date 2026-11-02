import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { dashboardPath, getUser } from "@/lib/session";

const Unauthorized = () => {
  const user = getUser();
  return (
    <main className="container mx-auto px-4 py-24 text-center">
      <h1 className="text-3xl font-semibold">You cannot open this page</h1>
      <p className="mt-2 text-muted-foreground">This area belongs to a different account type.</p>
      <div className="mt-6 flex justify-center gap-3">
        <Button asChild variant="outline"><Link to="/">Home</Link></Button>
        {user && <Button asChild><Link to={dashboardPath(user.role)}>Your dashboard</Link></Button>}
      </div>
    </main>
  );
};

export default Unauthorized;
