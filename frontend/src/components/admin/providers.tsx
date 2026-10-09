"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import { ApiError } from "@/lib/admin/api";
import { session } from "@/lib/admin/session";
import { FlashProvider } from "./ui/flash";

export function AdminProviders({ children }: { children: ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 15_000,
            refetchOnWindowFocus: false,
            retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 2,
          },
          mutations: { retry: false },
        },
      }),
  );

  // po przeładowaniu strony sesję odtwarza refresh token z ciasteczka (access token nie przetrwa przeładowania)
  useEffect(() => {
    if (session.snapshot().status === "loading") void session.refresh();
  }, []);

  return (
    <QueryClientProvider client={client}>
      <FlashProvider>{children}</FlashProvider>
    </QueryClientProvider>
  );
}
