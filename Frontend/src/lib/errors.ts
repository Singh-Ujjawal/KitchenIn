import axios from "axios";

export function errorMessage(error: unknown, fallback = "Something went wrong. Please try again.") {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === "string" && message.trim()) return message;
    if (!error.response) return "Cannot reach the server. Check your connection and try again.";
  }
  return fallback;
}
