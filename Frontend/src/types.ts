export type UserRole = "CUSTOMER" | "WORKER" | "ADMIN";

export type StaffCategory = "MAID" | "MECHANIC" | "WORKER" | "OTHER";

export type BookingStatus = "PENDING" | "ASSIGNED" | "COMPLETED" | "CANCELLED" | "CONFIRMED";

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  phone?: string;
  role: UserRole;
  address?: string;
  city?: string;
  category?: StaffCategory | string;
  approved?: boolean;
  rating?: number;
  reviewCount?: number;
  profilePicture?: string;
  identityProof?: string;
}

export interface AuthResponse {
  token: string;
  user: UserProfile;
}

export interface StaffPublic {
  id: string;
  name: string;
  category: string;
  city: string;
  rating?: number;
  reviewCount?: number;
  profilePicture?: string;
}

export interface Booking {
  id: string;
  customerId: string;
  customerName?: string;
  customerPhone?: string;
  staffId?: string;
  staffName?: string;
  staffPhone?: string;
  staffRating?: number;
  staffReviewCount?: number;
  staffProfilePicture?: string;
  category: string;
  city?: string;
  bookingDate: string;
  timeSlot: string;
  problemDescription: string;
  serviceAddress: string;
  price?: number;
  status: BookingStatus;
  createdAt?: string;
  reviewed?: boolean;
}

export interface Review {
  id: string;
  bookingId: string;
  customerId: string;
  customerName?: string;
  staffId: string;
  rating: number;
  comment?: string;
  createdAt?: string;
}

export interface DashboardStats {
  totalCustomers: number;
  totalStaff: number;
  activeStaff: number;
  pendingApprovals: number;
  totalBookings: number;
  pendingBookings: number;
  assignedBookings: number;
  completedBookings: number;
  cancelledBookings: number;
  totalRevenue: number;
}
