import { Fragment, type ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Treść dokumentów prawnych (D25) — Markdown w ograniczonym zakresie: nagłówki `##`/`###`, akapity, listy `-` i `1.`,
 * cytat `>` (uwagi), `**pogrubienie**`, `` `kod` `` i linki `[tekst](adres)`. Renderowany do elementów React, nigdy
 * jako HTML — treść z panelu nie może wstrzyknąć skryptu, a linki przyjmują tylko http(s), mailto i ścieżki względne.
 */

type Block =
  | { kind: "heading"; level: 2 | 3; text: string }
  | { kind: "paragraph"; text: string }
  | { kind: "quote"; text: string }
  | { kind: "list"; ordered: boolean; items: string[] };

export function parseLegalMarkdown(source: string): Block[] {
  const blocks: Block[] = [];
  let paragraph: string[] = [];
  let quote: string[] = [];
  let list: { ordered: boolean; items: string[] } | null = null;

  const flush = () => {
    if (paragraph.length) blocks.push({ kind: "paragraph", text: paragraph.join(" ") });
    if (quote.length) blocks.push({ kind: "quote", text: quote.join(" ") });
    if (list) blocks.push({ kind: "list", ...list });
    paragraph = [];
    quote = [];
    list = null;
  };

  for (const raw of source.replace(/\r\n/g, "\n").split("\n")) {
    const line = raw.trim();
    if (!line) {
      flush();
      continue;
    }
    const heading = /^(#{1,3})\s+(.*)$/.exec(line);
    if (heading) {
      flush();
      blocks.push({ kind: "heading", level: heading[1].length >= 3 ? 3 : 2, text: heading[2] });
      continue;
    }
    if (line.startsWith(">")) {
      if (paragraph.length || list) flush();
      quote.push(line.replace(/^>\s?/, ""));
      continue;
    }
    const bullet = /^[-*]\s+(.*)$/.exec(line);
    const numbered = /^\d+[.)]\s+(.*)$/.exec(line);
    if (bullet || numbered) {
      const ordered = Boolean(numbered);
      const current = list as { ordered: boolean; items: string[] } | null;
      if (paragraph.length || quote.length || (current && current.ordered !== ordered)) flush();
      list ??= { ordered, items: [] };
      list.items.push((bullet ?? numbered)![1]);
      continue;
    }
    if (list) {
      // kontynuacja punktu listy w kolejnej linii
      const items = (list as { items: string[] }).items;
      items[items.length - 1] += ` ${line}`;
      continue;
    }
    if (quote.length) flush();
    paragraph.push(line);
  }
  flush();
  return blocks;
}

const SAFE_HREF = /^(https?:\/\/|mailto:|\/(?!\/)|#)/i;

/** `**pogrubienie**`, `` `kod` `` i `[tekst](adres)` w jednej linii tekstu. */
function inline(text: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  const pattern = /\*\*(.+?)\*\*|`([^`]+)`|\[([^\]]+)\]\(([^)\s]+)\)/g;
  let last = 0;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(text))) {
    if (match.index > last) nodes.push(text.slice(last, match.index));
    const key = nodes.length;
    if (match[1] !== undefined) {
      nodes.push(<strong key={key} className="font-medium text-foreground">{inline(match[1])}</strong>);
    } else if (match[2] !== undefined) {
      nodes.push(<code key={key} className="bg-well px-1 text-[0.92em]">{match[2]}</code>);
    } else if (SAFE_HREF.test(match[4])) {
      const external = /^https?:/i.test(match[4]);
      nodes.push(
        <a
          key={key}
          href={match[4]}
          className="underline underline-offset-4 hover:text-foreground"
          {...(external ? { target: "_blank", rel: "noopener noreferrer" } : {})}
        >
          {match[3]}
        </a>,
      );
    } else {
      nodes.push(match[3]);
    }
    last = match.index + match[0].length;
  }
  if (last < text.length) nodes.push(text.slice(last));
  return nodes;
}

export function LegalMarkdown({ source, className }: { source: string; className?: string }) {
  const blocks = parseLegalMarkdown(source);
  return (
    <div className={cn("grid gap-4 text-ink-2", className)}>
      {blocks.map((block, index) => {
        switch (block.kind) {
          case "heading":
            return block.level === 2 ? (
              <h2 key={index} className="mt-6 text-body font-medium text-foreground first:mt-0">
                {inline(block.text)}
              </h2>
            ) : (
              <h3 key={index} className="mt-2 text-small font-medium text-foreground">
                {inline(block.text)}
              </h3>
            );
          case "quote":
            return (
              <p key={index} className="border-l-2 border-foreground bg-well px-4 py-3 text-small text-foreground">
                {inline(block.text)}
              </p>
            );
          case "list": {
            const Tag = block.ordered ? "ol" : "ul";
            return (
              <Tag key={index} className={cn("grid gap-1.5 pl-5", block.ordered ? "list-decimal" : "list-disc")}>
                {block.items.map((item, i) => (
                  <li key={i} className="pl-1">
                    {inline(item)}
                  </li>
                ))}
              </Tag>
            );
          }
          default:
            return <p key={index}>{inline(block.text).map((node, i) => <Fragment key={i}>{node}</Fragment>)}</p>;
        }
      })}
    </div>
  );
}
