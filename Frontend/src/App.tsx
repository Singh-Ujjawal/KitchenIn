import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import Index from "@/pages/Index";
import Auth from "@/pages/Auth";
import NotFound from "@/pages/NotFound";
import Unauthorized from "@/pages/Unauthorized";
import CustomerDashboard from "@/pages/CustomerDashboard";
import AdminDashboard from "@/pages/AdminDashboard";
import StaffDashboard from "@/pages/StaffDashboard";
import SearchResults from "@/pages/SearchResults";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="/" element={<Index />} />
            <Route path="/auth" element={<Auth />} />
            <Route path="/search" element={<SearchResults />} />
            <Route path="/customer" element={<RequireAuth role="CUSTOMER"><CustomerDashboard /></RequireAuth>} />
            <Route path="/admin" element={<RequireAuth role="ADMIN"><AdminDashboard /></RequireAuth>} />
            <Route path="/staff" element={<RequireAuth role="WORKER"><StaffDashboard /></RequireAuth>} />
            <Route path="/unauthorized" element={<Unauthorized />} />
            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
