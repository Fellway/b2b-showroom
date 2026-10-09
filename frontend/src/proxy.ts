import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";

export default createMiddleware(routing);

export const config = {
  // wszystko poza API, zasobami Next.js i plikami statycznymi
  matcher: "/((?!api|admin|_next|_vercel|.*\\..*).*)",
};
