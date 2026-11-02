import { FormEvent, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { CATEGORIES, CITIES } from "@/constants";
import { api } from "@/api";
import { errorMessage } from "@/lib/errors";
import { dashboardPath, getToken, getUser, saveSession } from "@/lib/session";
import type { AuthResponse, UserRole } from "@/types";
import { toast } from "sonner";

const Auth = () => {
  const navigate = useNavigate();
  const existing = getToken() && getUser();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [role, setRole] = useState<UserRole>("CUSTOMER");
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [pendingOpen, setPendingOpen] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [city, setCity] = useState("");
  const [category, setCategory] = useState("");
  const [adminCode, setAdminCode] = useState("");

  if (existing) return <Navigate to={dashboardPath(getUser()?.role)} replace />;

  const switchRole = (next: string) => {
    setRole(next as UserRole);
    setMode("login");
    setPassword("");
  };

  const login = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const path = role === "WORKER" ? "/staff/login" : role === "ADMIN" ? "/admin/login" : "/customer/login";
      const response = await api.post<AuthResponse>(path, { email, password });
      saveSession(response.data.token, response.data.user);
      toast.success(`Welcome back, ${response.data.user.name}`);
      navigate(dashboardPath(response.data.user.role));
    } catch (error) {
      toast.error(errorMessage(error, "Login failed"));
    } finally {
      setSubmitting(false);
    }
  };

  const register = async (event: FormEvent) => {
    event.preventDefault();
    if (role !== "ADMIN" && !/^[0-9]{10}$/.test(phone)) {
      toast.error("Phone number must be exactly 10 digits");
      return;
    }
    setSubmitting(true);
    try {
      if (role === "CUSTOMER") {
        await api.post("/customer/register", { name, email, password, phone, address });
        toast.success("Account created. Please sign in.");
        setMode("login");
        setPassword("");
      } else if (role === "ADMIN") {
        await api.post("/admin/register", { name, email, password, adminCode: adminCode || undefined });
        toast.success("Admin account created. Please sign in.");
        setMode("login");
        setPassword("");
        setAdminCode("");
      } else {
        if (!city || !category) {
          toast.error("Choose a city and a service category");
          return;
        }
        await api.post("/staff/register", { name, email, password, phone, city, category });
        setPendingOpen(true);
        setPassword("");
      }
    } catch (error) {
      toast.error(errorMessage(error, "Registration failed"));
    } finally {
      setSubmitting(false);
    }
  };

  const passwordField = (
    <div className="space-y-2">
      <Label htmlFor="password">Password</Label>
      <div className="relative">
        <Input
          id="password"
          type={showPassword ? "text" : "password"}
          autoComplete={mode === "login" ? "current-password" : "new-password"}
          value={password}
          minLength={mode === "register" ? 6 : undefined}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
        <button
          type="button"
          className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"
          onClick={() => setShowPassword((value) => !value)}
          aria-label={showPassword ? "Hide password" : "Show password"}
        >
          {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
    </div>
  );

  return (
    <main className="hero-glow flex min-h-[calc(100vh-4rem)] items-center px-4 py-12">
      <Card className="fade-up mx-auto w-full max-w-xl rounded-3xl border-border/70 shadow-lg">
        <CardHeader className="space-y-2 pb-4 text-center">
          <CardTitle className="font-display text-3xl">{mode === "login" ? "Sign in" : "Create an account"}</CardTitle>
          <CardDescription>
            {role === "ADMIN" ? "Administrator access" : "Choose your role, then sign in or create an account."}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs value={role} onValueChange={switchRole}>
            <TabsList className="mb-6 grid h-11 w-full grid-cols-3 rounded-xl bg-muted p-1">
              <TabsTrigger value="CUSTOMER" className="rounded-lg">Customer</TabsTrigger>
              <TabsTrigger value="WORKER" className="rounded-lg">Worker</TabsTrigger>
              <TabsTrigger value="ADMIN" className="rounded-lg">Admin</TabsTrigger>
            </TabsList>

            <TabsContent value="CUSTOMER">
              <form onSubmit={mode === "login" ? login : register} className="space-y-4">
                {mode === "register" && (
                  <>
                    <Field label="Full name" value={name} onChange={setName} />
                    <Field label="Phone" value={phone} onChange={setPhone} inputMode="numeric" />
                    <Field label="Address" value={address} onChange={setAddress} />
                  </>
                )}
                <Field label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
                {passwordField}
                <Button size="lg" className="w-full rounded-xl shadow-md" disabled={submitting}>{submitting ? "Please wait..." : mode === "login" ? "Sign in" : "Create account"}</Button>
              </form>
            </TabsContent>

            <TabsContent value="WORKER">
              <form onSubmit={mode === "login" ? login : register} className="space-y-4">
                {mode === "register" && (
                  <>
                    <Field label="Full name" value={name} onChange={setName} />
                    <Field label="Phone" value={phone} onChange={setPhone} inputMode="numeric" />
                    <div className="space-y-2">
                      <Label>City</Label>
                      <Select value={city || undefined} onValueChange={setCity}>
                        <SelectTrigger><SelectValue placeholder="Select city" /></SelectTrigger>
                        <SelectContent>
                          {CITIES.map((item) => <SelectItem key={item} value={item}>{item}</SelectItem>)}
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-2">
                      <Label>Category</Label>
                      <Select value={category || undefined} onValueChange={setCategory}>
                        <SelectTrigger><SelectValue placeholder="Select category" /></SelectTrigger>
                        <SelectContent>
                          {CATEGORIES.map((item) => <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>)}
                        </SelectContent>
                      </Select>
                    </div>
                  </>
                )}
                <Field label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
                {passwordField}
                <Button size="lg" className="w-full rounded-xl shadow-md" disabled={submitting}>{submitting ? "Please wait..." : mode === "login" ? "Sign in" : "Submit application"}</Button>
              </form>
            </TabsContent>

            <TabsContent value="ADMIN">
              <form onSubmit={mode === "login" ? login : register} className="space-y-4">
                {mode === "register" && (
                  <>
                    <p className="rounded-md border border-border bg-muted px-3 py-2 text-sm text-muted-foreground">
                      The first admin account needs no code. After that, an admin invite code is required.
                    </p>
                    <Field label="Full name" value={name} onChange={setName} />
                  </>
                )}
                <Field label="Email" type="email" value={email} onChange={setEmail} autoComplete="username" />
                {passwordField}
                {mode === "register" && (
                  <Field label="Admin invite code" type="password" value={adminCode} onChange={setAdminCode} required={false} />
                )}
                <Button size="lg" className="w-full rounded-xl shadow-md" disabled={submitting}>{submitting ? "Please wait..." : mode === "login" ? "Admin sign in" : "Create admin account"}</Button>
              </form>
            </TabsContent>
          </Tabs>

          <button
            type="button"
            className="mt-6 w-full text-center text-sm text-primary hover:underline"
            onClick={() => setMode(mode === "login" ? "register" : "login")}
          >
            {mode === "login" ? "Need an account? Register" : "Already registered? Sign in"}
          </button>
        </CardContent>
      </Card>

      <Dialog open={pendingOpen} onOpenChange={setPendingOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Application received</DialogTitle>
            <DialogDescription>
              Your worker account stays pending until an administrator approves it. You can sign in after approval.
            </DialogDescription>
          </DialogHeader>
          <Button onClick={() => { setPendingOpen(false); navigate("/"); }}>Back to home</Button>
        </DialogContent>
      </Dialog>
    </main>
  );
};

function Field({
  label,
  value,
  onChange,
  type = "text",
  autoComplete,
  inputMode,
  required = true,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  autoComplete?: string;
  inputMode?: "numeric" | "text";
  required?: boolean;
}) {
  const id = label.toLowerCase().replace(/\s+/g, "-");
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        type={type}
        inputMode={inputMode}
        autoComplete={autoComplete}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required={required}
      />
    </div>
  );
}

export default Auth;
