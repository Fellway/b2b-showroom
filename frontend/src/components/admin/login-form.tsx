"use client";

import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { session } from "@/lib/admin/session";
import { useSession } from "@/lib/admin/use-session";
import { TextInput, Field } from "./ui/field";

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const state = useSession();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // tylko ścieżki panelu — parametr `next` nie może przekierować poza aplikację
  const next = searchParams.get("next");
  const target = next && next.startsWith("/admin") && !next.startsWith("//") ? next : "/admin/products";

  useEffect(() => {
    if (state.status === "authenticated") router.replace(target);
  }, [state.status, router, target]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    const result = await session.login(email.trim(), password).catch(() => ({ ok: false as const, status: 0 }));
    setSubmitting(false);
    if (result.ok) return;
    if (result.status === 429) {
      setError(`Za dużo prób logowania. Spróbuj ponownie za ${"retryAfter" in result && result.retryAfter ? result.retryAfter : 60} s.`);
    } else if (result.status === 401) {
      setError("Nieprawidłowy e-mail lub hasło.");
    } else {
      setError("Nie udało się połączyć z serwerem. Spróbuj ponownie.");
    }
  }

  return (
    <form onSubmit={submit} className="flex w-full max-w-sm flex-col gap-5" noValidate={false}>
      <div className="mb-4 flex flex-col items-start gap-3">
        <Image src="/brand/fabians-style-logo.png" alt="Fabian's Style" width={866} height={191} priority className="h-8 w-auto" />
        <h1 className="text-body font-medium">Panel administracyjny</h1>
      </div>
      <Field label="E-mail">
        <TextInput type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />
      </Field>
      <Field label="Hasło">
        <TextInput
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </Field>
      {error && (
        <p role="alert" className="text-small text-danger">
          {error}
        </p>
      )}
      <Button type="submit" size="lg" disabled={submitting} className="mt-2">
        {submitting ? "Logowanie…" : "Zaloguj się"}
      </Button>
    </form>
  );
}
