"use client";

import { useEffect } from "react";
import { buyerSession } from "@/lib/buyer/session";

/** Po wejściu na stronę odtwarza sesję kupca z ciasteczka (tylko gdy kupiec logował się w tej przeglądarce). */
export function BuyerSessionBootstrap() {
  useEffect(() => {
    buyerSession.restore();
  }, []);
  return null;
}
