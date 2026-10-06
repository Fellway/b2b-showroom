import type { Problem } from "./types";

/** Klucz komunikatu (namespace `errors`) dla błędu API; komunikaty dla kupca nigdy nie pokazują treści technicznej. */
export function problemMessageKey(problem: Problem): { key: string; values?: Record<string, number> } {
  switch (problem.code) {
    case "catalog.variant.not-selectable":
    case "catalog.variant.duplicate":
      return { key: "notSelectable" };
    case "linesheet.privacy.consent-required":
      return { key: "privacyRequired" };
    case "linesheet.recipient.email":
      return { key: "invalidEmail" };
    case "request.invalid":
      return { key: "validation" };
    default:
      return { key: "generic" };
  }
}

export async function readProblem(response: Response): Promise<Problem> {
  try {
    const body = (await response.json()) as Problem;
    return { ...body, status: response.status };
  } catch {
    return { status: response.status };
  }
}
