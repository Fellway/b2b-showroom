"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Field, Select, TextInput } from "@/components/admin/ui/field";
import { useFlash } from "@/components/admin/ui/flash";
import { PageBody, PageHeader } from "@/components/admin/ui/page-header";
import { EmptyState, ErrorState, LoadingRows } from "@/components/admin/ui/states";
import { Table, Td, Th, Tr } from "@/components/admin/ui/table";
import { ActiveLabel } from "@/components/admin/ui/status-label";
import { FormSheet } from "@/components/admin/ui/form-sheet";
import { adminApi, ApiError } from "@/lib/admin/api";
import { errorMessage } from "@/lib/admin/errors";
import { formatDateTime } from "@/lib/admin/line-sheets";
import type { AdminRole } from "@/lib/admin/session";
import { useSession } from "@/lib/admin/use-session";
import { PASSWORD_MAX, PASSWORD_MIN, ROLE_HINT, ROLE_LABEL, useAdminUsers, userError, usersKey, type AdminUser } from "@/lib/admin/users";

export function UsersPage() {
  const state = useSession();
  const isAdmin = state.status === "authenticated" && state.admin.role === "ADMIN";
  const selfId = state.status === "authenticated" ? state.admin.id : null;
  const { data, isPending, isError, error, refetch, dataUpdatedAt } = useAdminUsers(isAdmin);
  const [editing, setEditing] = useState<AdminUser | "new" | null>(null);

  if (!isAdmin) {
    return (
      <>
        <PageHeader title="Konta" />
        <PageBody>
          <EmptyState title="Brak dostępu">Kontami panelu zarządza administrator. Twoja rola (redaktor) obejmuje katalog, słowniki, zestawienia i statystyki.</EmptyState>
        </PageBody>
      </>
    );
  }

  // blokada liczona względem chwili pobrania listy (render musi być czysty)
  const now = dataUpdatedAt;
  return (
    <>
      <PageHeader
        title="Konta panelu"
        meta={data ? `${data.length}` : undefined}
        actions={
          <Button onClick={() => setEditing("new")}>
            <Plus aria-hidden strokeWidth={1.5} />
            Dodaj konto
          </Button>
        }
      />
      <PageBody>
        {isPending ? (
          <LoadingRows rows={3} />
        ) : isError ? (
          <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>Osoba</Th>
                <Th>Rola</Th>
                <Th>Status</Th>
                <Th>Ostatnie logowanie</Th>
              </tr>
            </thead>
            <tbody>
              {data.map((user) => {
                const locked = user.lockedUntil && Date.parse(user.lockedUntil) > now;
                return (
                  <Tr key={user.id} interactive onClick={() => setEditing(user)}>
                    <Td>
                      <button type="button" className="text-left font-medium underline-offset-4 hover:underline" onClick={() => setEditing(user)}>
                        {user.displayName}
                        {user.id === selfId && <span className="font-normal text-ink-3"> (Ty)</span>}
                      </button>
                      <span className="block break-all text-meta text-ink-3">{user.email}</span>
                    </Td>
                    <Td>{ROLE_LABEL[user.role]}</Td>
                    <Td>
                      <ActiveLabel active={user.enabled} activeText="Aktywne" inactiveText="Wyłączone" />
                      {locked && <span className="block text-meta text-ink-3">Zablokowane po nieudanych logowaniach do {formatDateTime(user.lockedUntil)}</span>}
                    </Td>
                    <Td className="tabular text-ink-2">{user.lastLoginAt ? formatDateTime(user.lastLoginAt) : "Nigdy"}</Td>
                  </Tr>
                );
              })}
            </tbody>
          </Table>
        )}
      </PageBody>
      {editing === "new" ? (
        <CreateUserSheet onClose={() => setEditing(null)} />
      ) : (
        <EditUserSheet key={editing?.id ?? "closed"} user={editing} self={editing?.id === selfId} onClose={() => setEditing(null)} />
      )}
    </>
  );
}

function RoleField({ value, onChange, disabled, hint }: { value: AdminRole; onChange: (role: AdminRole) => void; disabled?: boolean; hint?: string }) {
  return (
    <Field label="Rola" hint={hint ?? ROLE_HINT[value]}>
      <Select value={value} disabled={disabled} onChange={(e) => onChange(e.target.value as AdminRole)}>
        <option value="EDITOR">{ROLE_LABEL.EDITOR}</option>
        <option value="ADMIN">{ROLE_LABEL.ADMIN}</option>
      </Select>
    </Field>
  );
}

function CreateUserSheet({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient();
  const flash = useFlash();
  const [form, setForm] = useState({ email: "", displayName: "", role: "EDITOR" as AdminRole, password: "" });
  const [error, setError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: () => adminApi.post("/users", { ...form, email: form.email.trim(), displayName: form.displayName.trim() }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: usersKey });
      flash("Dodano konto");
      onClose();
    },
    onError: (e) => setError(userError(e)),
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (form.password.length < PASSWORD_MIN || form.password.length > PASSWORD_MAX) {
      setError(`Hasło musi mieć od ${PASSWORD_MIN} do ${PASSWORD_MAX} znaków.`);
      return;
    }
    create.mutate();
  }

  return (
    <FormSheet
      open
      onOpenChange={(open) => !open && onClose()}
      title="Nowe konto"
      description="Przekaż hasło startowe osobie bezpiecznym kanałem — nie wysyłamy go e-mailem."
      onSubmit={submit}
      footer={
        <>
          <Button type="submit" disabled={create.isPending}>
            {create.isPending ? "Zapisywanie…" : "Dodaj konto"}
          </Button>
          <Button type="button" variant="text" onClick={onClose}>
            Anuluj
          </Button>
        </>
      }
    >
      <Field label="E-mail (login)">
        <TextInput type="email" required autoComplete="off" maxLength={254} value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
      </Field>
      <Field label="Nazwa wyświetlana">
        <TextInput required maxLength={120} value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} />
      </Field>
      <RoleField value={form.role} onChange={(role) => setForm({ ...form, role })} />
      <Field label="Hasło startowe" hint={`Od ${PASSWORD_MIN} do ${PASSWORD_MAX} znaków. Długa fraza jest lepsza niż znaki specjalne.`}>
        <TextInput
          type="password"
          required
          autoComplete="new-password"
          minLength={PASSWORD_MIN}
          maxLength={PASSWORD_MAX}
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
      </Field>
      {error && (
        <p role="alert" className="text-danger">
          {error}
        </p>
      )}
    </FormSheet>
  );
}

function EditUserSheet({ user, self, onClose }: { user: AdminUser | null; self: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const flash = useFlash();
  const [form, setForm] = useState({ displayName: user?.displayName ?? "", role: user?.role ?? ("EDITOR" as AdminRole) });
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const refresh = () => queryClient.invalidateQueries({ queryKey: usersKey });
  const fail = (e: unknown) => {
    setError(userError(e));
    if (e instanceof ApiError && e.status === 409) void refresh();
  };

  const update = useMutation({
    mutationFn: () => adminApi.put(`/users/${user!.id}`, { displayName: form.displayName.trim(), role: form.role, version: user!.version }),
    onSuccess: async () => {
      await refresh();
      flash("Zapisano zmiany");
      onClose();
    },
    onError: fail,
  });

  const toggle = useMutation({
    mutationFn: () => adminApi.post(`/users/${user!.id}/${user!.enabled ? "disable" : "enable"}`),
    onSuccess: async () => {
      await refresh();
      flash(user!.enabled ? "Konto wyłączone — sesje tej osoby zostały zakończone" : "Konto włączone");
      onClose();
    },
    onError: fail,
  });

  const reset = useMutation({
    mutationFn: () => adminApi.post(`/users/${user!.id}/password`, { password }),
    onSuccess: async () => {
      setPassword("");
      await refresh();
      flash("Hasło zmienione — dotychczasowe sesje zakończone");
    },
    onError: fail,
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    update.mutate();
  }

  function resetPassword() {
    setError(null);
    if (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
      setError(`Hasło musi mieć od ${PASSWORD_MIN} do ${PASSWORD_MAX} znaków.`);
      return;
    }
    reset.mutate();
  }

  return (
    <FormSheet
      open={user !== null}
      onOpenChange={(open) => !open && onClose()}
      title={user ? `${user.displayName}${self ? " (Ty)" : ""}` : ""}
      description={user?.email}
      onSubmit={submit}
      footer={
        <>
          <Button type="submit" disabled={update.isPending}>
            {update.isPending ? "Zapisywanie…" : "Zapisz zmiany"}
          </Button>
          <Button type="button" variant="text" onClick={onClose}>
            Anuluj
          </Button>
        </>
      }
    >
      <Field label="Nazwa wyświetlana">
        <TextInput required maxLength={120} value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} />
      </Field>
      <RoleField
        value={form.role}
        onChange={(role) => setForm({ ...form, role })}
        disabled={self}
        hint={self ? "Nie możesz zmienić własnej roli." : undefined}
      />

      <section className="mt-2 flex flex-col gap-3 border-t border-line pt-5">
        <h3 className="font-medium">Dostęp</h3>
        <p className="text-ink-2">
          {user?.enabled
            ? "Wyłączenie konta natychmiast kończy wszystkie sesje tej osoby. Konto można później włączyć ponownie."
            : "Konto jest wyłączone — ta osoba nie może się zalogować."}
        </p>
        <div>
          <Button type="button" variant="secondary" disabled={self || toggle.isPending} onClick={() => toggle.mutate()}>
            {user?.enabled ? "Wyłącz konto" : "Włącz konto"}
          </Button>
          {self && <p className="mt-2 text-meta text-ink-3">Nie możesz wyłączyć własnego konta.</p>}
        </div>
      </section>

      <section className="flex flex-col gap-3 border-t border-line pt-5">
        <h3 className="font-medium">Nowe hasło</h3>
        <Field label="Hasło" hint={`Od ${PASSWORD_MIN} do ${PASSWORD_MAX} znaków. Zmiana kończy dotychczasowe sesje tej osoby.`}>
          <TextInput type="password" autoComplete="new-password" maxLength={PASSWORD_MAX} value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <div>
          <Button type="button" variant="secondary" disabled={!password || reset.isPending} onClick={resetPassword}>
            {reset.isPending ? "Zmienianie…" : "Ustaw hasło"}
          </Button>
        </div>
      </section>

      {error && (
        <p role="alert" className="text-danger">
          {error}
        </p>
      )}
    </FormSheet>
  );
}
