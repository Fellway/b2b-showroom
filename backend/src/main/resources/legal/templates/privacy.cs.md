# Zásady ochrany osobních údajů

> **Vzor k právní kontrole.** Text popisuje skutečné fungování showroomu, ale před ostrým provozem jej musí zkontrolovat právník značky. Doplňte údaje v hranatých závorkách a tuto poznámku odstraňte.

## 1. Správce údajů

Správcem osobních údajů je [PLNÝ NÁZEV FIRMY], [ADRESA SÍDLA], DIČ (NIP) [NIP], [ZÁPIS V REJSTŘÍKU] — vlastník značky {brand} („my“).

Kontakt ve věcech osobních údajů: [E-MAIL PRO OCHRANU OSOBNÍCH ÚDAJŮ]. [Pokud byl jmenován pověřenec pro ochranu osobních údajů — jeho kontaktní údaje.]

## 2. Na co se zásady vztahují

Velkoobchodní showroom {brand} je služba pro firemní zákazníky (B2B): katalog kolekce, účty odběratelů, PDF přehledy a kontakt s obchodním zástupcem. V showroomu neprobíhá prodej ani platby a neposíláme obchodní sdělení e-mailem — zasíláme pouze zprávy týkající se účtu.

## 3. Jaké údaje zpracováváme, proč a na jakém právním základě

### Účet odběratele

- **Údaje:** e-mailová adresa, heslo (pouze jako kryptografický otisk — vaše heslo neznáme), název firmy, země, DIČ / DIČ EU, jméno a příjmení kontaktní osoby, telefon, jazyk, stav účtu, data registrace, potvrzení adresy, rozhodnutí o účtu a posledního přihlášení, počítadlo neúspěšných přihlášení.
- **Účel:** založení a vedení účtu, ověření, že žadatel je velkoobchodní zákazník, zpřístupnění velkoobchodních cen a přehledů, kontakt ohledně spolupráce.
- **Právní základ:** čl. 6 odst. 1 písm. b) GDPR (smlouva — obchodní podmínky showroomu) a u údajů kontaktní osoby jednající za firmu čl. 6 odst. 1 písm. f) GDPR (náš oprávněný zájem na ověření obchodních partnerů a obchodní komunikaci).

### PDF přehledy

- **Údaje:** údaje o firmě a kontaktní osobě z účtu v okamžiku vytvoření přehledu (ukládají se spolu s přehledem), vybrané produkty a orientační množství, přidělený obchodní zástupce, soubor PDF.
- **Účel:** příprava přehledu na vaši žádost a vyřízení poptávky obchodním zástupcem.
- **Právní základ:** čl. 6 odst. 1 písm. b) GDPR.

### Záznam o přijetí dokumentů

- **Údaje:** který dokument (obchodní podmínky, zásady ochrany osobních údajů), v jaké verzi, kdy, v jakém jazyce a v jaké situaci (registrace, nová verze dokumentu) byl přijat nebo vzat na vědomí. Neukládáme IP adresu ani údaje o prohlížeči.
- **Účel:** prokázání, za jakých podmínek byla smlouva uzavřena a že jsme splnili informační povinnost.
- **Právní základ:** čl. 6 odst. 1 písm. c) GDPR ve spojení s čl. 5 odst. 2 GDPR (odpovědnost) a čl. 6 odst. 1 písm. f) GDPR.

### Zabezpečení a relace

- Cookie relace `showroom_buyer_refresh` (nepřístupná skriptům, platná až 30 dní) udržuje přihlášení; v databázi uchováváme pouze její otisk. Krátkodobý přístupový token (15 minut) existuje jen v paměti karty prohlížeče.
- Omezujeme počet požadavků z jedné adresy (např. pokusů o přihlášení). IP adresa se přitom používá pouze v paměti serveru a neukládá se do databáze ani do statistik.
- **Právní základ:** čl. 6 odst. 1 písm. f) GDPR (zabezpečení služby a účtů).

### Statistiky návštěv

- **Bez vašeho souhlasu** počítáme pouze anonymní události (zobrazení stránky nebo produktu, přidání produktu do přehledu, kliknutí na kontaktní údaje) — bez identifikátorů, IP adresy a údajů o prohlížeči. Do vašeho zařízení nic neukládáme.
- **S vaším souhlasem** (lišta ochrany soukromí) ukládáme v prohlížeči náhodný identifikátor návštěvníka (`fs-visitor`, obnovuje se po 13 měsících) a identifikátor návštěvy (`fs-session`, do zavření karty), díky kterým počítáme unikátní návštěvníky.
- Souhlas můžete kdykoli odvolat v „Nastavení soukromí“ v patičce stránky — identifikátory se pak z prohlížeče odstraní.
- **Právní základ:** čl. 6 odst. 1 písm. a) GDPR (souhlas) a předpisy o ukládání informací do zařízení uživatele [právní kontrola: příslušné vnitrostátní ustanovení].

### Kontakt s obchodním zástupcem

Když obchodnímu zástupci napíšete nebo zavoláte, zpracováváme údaje z korespondence, abychom mohli odpovědět a vést obchodní jednání (čl. 6 odst. 1 písm. f) GDPR).

## 4. Informace ukládané v prohlížeči

- `showroom_buyer_refresh` — cookie relace přihlášeného odběratele (nezbytná).
- `fs-buyer-session` — značka, že je na tomto zařízení přihlášen odběratel (nezbytná).
- `fs-line-sheet` — váš výběr produktů do přehledu, bez cen (nezbytný pro funkci, kterou používáte).
- `fs-consent` — vaše rozhodnutí o statistikách, platné 12 měsíců (nezbytné).
- `fs-visitor`, `fs-session` — identifikátory statistik, pouze s vaším souhlasem.

Nepoužíváme reklamní cookies ani sledovací nástroje třetích stran.

## 5. Příjemci údajů

- poskytovatel serveru: [NÁZEV POSKYTOVATELE HOSTINGU, ZEMĚ] — uchovává databázi, soubory a zálohy;
- Brevo (Sendinblue SAS, Francie) — doručuje zprávy týkající se účtu: potvrzení adresy, rozhodnutí o účtu, obnovení hesla, potvrzení smazání účtu;
- [další příjemci, např. účetní kancelář, advokátní kancelář — pokud se uplatní];
- orgány veřejné moci, pokud to vyžadují předpisy.

Zpracovatelé jednající naším jménem tak činí na základě smluv o zpracování (čl. 28 GDPR). Údaje nepředáváme mimo Evropský hospodářský prostor [právní kontrola: umístění serverů poskytovatelů].

## 6. Jak dlouho údaje uchováváme

- účet a záznam o přijetí dokumentů — do smazání účtu (vámi, nebo námi po ukončení spolupráce); nepotvrzené a zamítnuté účty — [DOBA];
- přehledy — [DOBA]. Po smazání účtu přehled zůstává jako záznam nabídky (číslo, datum, produkty, název firmy a DIČ), ale odstraníme z něj údaje kontaktní osoby a soubor PDF;
- relace — až 30 dní; odkazy v e-mailech — 48 hodin (potvrzení adresy) a 1 hodinu (nové heslo);
- statistiky — [DOBA];
- zálohy — až 14 dní;
- déle jen tehdy, vyžadují-li to předpisy nebo je-li to nutné k určení, výkonu nebo obhajobě právních nároků.

## 7. Vaše práva

Máte právo na přístup ke svým údajům, jejich opravu, výmaz, omezení zpracování a přenositelnost, právo vznést námitku proti zpracování založenému na oprávněném zájmu a právo kdykoli odvolat souhlas (aniž by tím byla dotčena zákonnost zpracování před jeho odvoláním).

V sekci „Můj účet“ můžete sami:

- stáhnout své údaje (soubor JSON: údaje účtu, historie přijatých dokumentů, seznam přehledů),
- zobrazit historii přijatých dokumentů,
- opravit kontaktní údaje (změnu názvu firmy nebo DIČ nahlaste obchodnímu zástupci),
- smazat účet.

V ostatních věcech pište na [E-MAIL PRO OCHRANU OSOBNÍCH ÚDAJŮ]. Máte také právo podat stížnost u polského dozorového úřadu (Prezes Urzędu Ochrony Danych Osobowych, ul. Stawki 2, 00-193 Varšava) nebo u dozorového úřadu ve své zemi (v České republice Úřad pro ochranu osobních údajů).

## 8. Poskytnutí údajů

Poskytnutí údajů v registračním formuláři je dobrovolné, ale bez nich účet nezaložíme. Nerozhodujeme výhradně automatizovaně — o schválení účtu rozhoduje pracovník značky.

## 9. Změny zásad

Každá verze zásad má číslo a datum zveřejnění a předchozí verze jsou dostupné na této stránce. O nové verzi budeme přihlášené odběratele informovat v showroomu.
