import type { Metadata } from "next";
import { UsersPage } from "@/components/admin/users/users-page";

export const metadata: Metadata = { title: "Konta" };

export default function Page() {
  return <UsersPage />;
}
