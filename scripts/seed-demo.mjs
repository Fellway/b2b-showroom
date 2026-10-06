#!/usr/bin/env node
/**
 * DANE DEMONSTRACYJNE katalogu Fabian's Style.
 *
 * Nazwy modeli, opisy, ceny, SKU i kody EAN są PRZYKŁADOWE i muszą zostać zastąpione importem prawdziwych danych
 * marki. Opisy mówią wyłącznie o tym, co widać na zdjęciach (krój, rękaw, długość, print) — bez deklaracji
 * o składzie, certyfikatach czy nagrodach. EAN-y leżą w puli GS1 „restricted circulation” (prefiks 29), więc nie mogą
 * kolidować z prawdziwymi produktami. Zdjęcia pochodzą z brand-assets/ (poza repozytorium).
 *
 * Skrypt rozmawia wyłącznie z publicznym REST API backendu i jest idempotentny (można go uruchamiać wielokrotnie).
 *
 *   SHOWROOM_API=http://localhost:8080 SHOWROOM_ADMIN_EMAIL=... SHOWROOM_ADMIN_PASSWORD=... node scripts/seed-demo.mjs
 */
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const API = (process.env.SHOWROOM_API ?? 'http://localhost:8080').replace(/\/$/, '');
const EMAIL = process.env.SHOWROOM_ADMIN_EMAIL;
const PASSWORD = process.env.SHOWROOM_ADMIN_PASSWORD;
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const ASSETS = path.join(ROOT, 'brand-assets');

if (!EMAIL || !PASSWORD) {
  console.error('Set SHOWROOM_ADMIN_EMAIL and SHOWROOM_ADMIN_PASSWORD');
  process.exit(1);
}

// ---------------------------------------------------------------------------------------------- dane

const SIZES = ['36', '38', '40', '42', '44', '46'];

const CATEGORIES = [
  { slug: 'sukienki-midi', sortOrder: 0, translations: {
    pl: { name: 'Sukienki midi' }, en: { name: 'Midi dresses' }, cs: { name: 'Midi šaty' }, uk: { name: 'Сукні міді' } } },
  { slug: 'sukienki-koszulowe', sortOrder: 1, translations: {
    pl: { name: 'Sukienki koszulowe' }, en: { name: 'Shirt dresses' }, cs: { name: 'Košilové šaty' }, uk: { name: 'Сукні-сорочки' } } },
  { slug: 'sukienki-dopasowane', sortOrder: 2, translations: {
    pl: { name: 'Sukienki dopasowane' }, en: { name: 'Fitted dresses' }, cs: { name: 'Přiléhavé šaty' }, uk: { name: 'Приталені сукні' } } },
];

/** short = fragment SKU koloru */
const COLORS = [
  { code: 'MULTI-PIORA', short: 'MUL', hex: '#5A2D4A', names: { pl: 'Wielobarwne pióra', en: 'Multicolour feathers', cs: 'Pestrobarevná pera', uk: 'Різнокольорове пір’я' } },
  { code: 'BRAZ-LISCIE', short: 'BRL', hex: '#5B3226', names: { pl: 'Brąz w liście', en: 'Brown leaf print', cs: 'Hnědá s listy', uk: 'Коричневий з листям' } },
  { code: 'SLIWKA-ROZE', short: 'SLR', hex: '#5E1F3A', names: { pl: 'Śliwkowe róże', en: 'Plum roses', cs: 'Švestkové růže', uk: 'Сливові троянди' } },
  { code: 'KOBALT-KWIATY', short: 'KOB', hex: '#1F6FB2', names: { pl: 'Kobalt w kwiaty', en: 'Cobalt floral', cs: 'Kobaltová s květy', uk: 'Кобальт у квіти' } },
  { code: 'GRAFIT-ABSTRAKCJA', short: 'GRA', hex: '#1E2230', names: { pl: 'Grafitowa abstrakcja', en: 'Graphite abstract', cs: 'Grafitová abstrakce', uk: 'Графітова абстракція' } },
  { code: 'MORSKI-MARMUR', short: 'MOR', hex: '#1F5C5A', names: { pl: 'Morski marmur', en: 'Teal marble', cs: 'Mořský mramor', uk: 'Морський мармур' } },
  { code: 'BORDO-MARMUR', short: 'BMA', hex: '#5A1A22', names: { pl: 'Bordowy marmur', en: 'Burgundy marble', cs: 'Bordó mramor', uk: 'Бордовий мармур' } },
  { code: 'GRANAT-PAISLEY', short: 'GPA', hex: '#24304F', names: { pl: 'Granatowy paisley', en: 'Navy paisley', cs: 'Tmavomodrý paisley', uk: 'Темно-синій пейслі' } },
  { code: 'CZARNY-PLASTER', short: 'CZP', hex: '#2A2A2A', names: { pl: 'Czarny plaster miodu', en: 'Black honeycomb', cs: 'Černá plástev', uk: 'Чорні стільники' } },
  { code: 'CZEKOLADA', short: 'CZE', hex: '#4A2C20', names: { pl: 'Czekoladowy', en: 'Chocolate', cs: 'Čokoládová', uk: 'Шоколадний' } },
  { code: 'BORDO', short: 'BOR', hex: '#5C1A2A', names: { pl: 'Bordowy', en: 'Burgundy', cs: 'Bordó', uk: 'Бордовий' } },
  { code: 'ZIELEN-PIORA', short: 'ZIP', hex: '#3F6B5A', names: { pl: 'Zieleń w pióra', en: 'Green feather print', cs: 'Zelená s pery', uk: 'Зелений у пір’я' } },
  { code: 'BEZ-BATIK', short: 'BEB', hex: '#C9A887', names: { pl: 'Beżowy batik', en: 'Beige batik', cs: 'Béžová batika', uk: 'Бежевий батик' } },
  { code: 'GRANAT-MIEDZ', short: 'GMI', hex: '#1C2440', names: { pl: 'Granat z miedzią', en: 'Navy with copper', cs: 'Tmavomodrá s mědí', uk: 'Темно-синій з міддю' } },
  { code: 'BEZ-PIORA', short: 'BEP', hex: '#D8C3AA', names: { pl: 'Beż w pióra', en: 'Beige feather print', cs: 'Béžová s pery', uk: 'Бежевий у пір’я' } },
];

const S = 'zdj-sesja/';
const AI = 'modelki-ai/';

/** Pierwsze zdjęcie każdego koloru jest okładką. status: PUBLISHED | HIDDEN | DRAFT */
const PRODUCTS = [
  {
    styleCode: 'FS-2601', slug: 'sukienka-midi-klara', category: 'sukienki-midi', wholesale: '159.00', retail: '379.00',
    featured: true, sortOrder: 0, status: 'PUBLISHED',
    names: { pl: 'Sukienka midi Klara', en: 'Klara midi dress', cs: 'Midi šaty Klara', uk: 'Сукня міді Клара' },
    descriptions: {
      pl: 'Rozkloszowana sukienka midi z kopertowym dekoltem w szpic, odcięciem pod biustem i rękawem 3/4. Wielobarwny print przypominający pióra.',
      en: 'Flared midi dress with a wrap V-neckline, empire seam and 3/4 sleeves. Multicolour feather-like print.',
      cs: 'Rozšířené midi šaty s překládaným výstřihem do V, sedlem pod prsy a 3/4 rukávem. Pestrobarevný potisk připomínající pera.',
      uk: 'Розкльошена сукня міді із запахом і V-подібним вирізом, відрізною лінією під грудьми та рукавом 3/4. Різнокольоровий принт, схожий на пір’я.',
    },
    colors: [{ code: 'MULTI-PIORA', images: [S + '825278528_4549590355367787_7024475099642768538_n.jpeg', S + '825292262_1865814264796255_8786258421017956336_n.jpeg', S + '825273959_1434355681978583_5146643861417901554_n.jpeg'] }],
  },
  {
    styleCode: 'FS-2602', slug: 'sukienka-koszulowa-wiktoria', category: 'sukienki-koszulowe', wholesale: '169.00', retail: '399.00',
    featured: true, sortOrder: 1, status: 'PUBLISHED',
    names: { pl: 'Sukienka koszulowa Wiktoria', en: 'Wiktoria shirt dress', cs: 'Košilové šaty Wiktoria', uk: 'Сукня-сорочка Вікторія' },
    descriptions: {
      pl: 'Zapinana na guziki sukienka koszulowa midi z kołnierzykiem, wiązanym paskiem i podwijanymi rękawami na patce.',
      en: 'Button-through midi shirt dress with a collar, tie belt and roll-up tab sleeves.',
      cs: 'Propínací košilové midi šaty s límečkem, vázacím páskem a ohrnovacími rukávy s poutkem.',
      uk: 'Сукня-сорочка міді на ґудзиках з комірцем, поясом на зав’язках і рукавами, що підкочуються на хлястик.',
    },
    colors: [
      { code: 'BRAZ-LISCIE', images: [S + '822286650_2097859557512536_717236879680542930_n.jpeg', S + '822076629_2859318627773998_7921491144345487795_n.jpeg'] },
      { code: 'SLIWKA-ROZE', images: [S + '825267809_2468936360299796_625644863711734028_n.jpeg', S + '824654748_1660639715594633_5153632572631924779_n.jpeg', S + '825206487_1713040640477662_3525577371911096673_n.jpeg'] },
    ],
  },
  {
    styleCode: 'FS-2603', slug: 'sukienka-midi-lena', category: 'sukienki-midi', wholesale: '149.00', retail: '349.00',
    featured: false, sortOrder: 2, status: 'PUBLISHED', omitSizes: ['46'],
    names: { pl: 'Sukienka midi Lena', en: 'Lena midi dress', cs: 'Midi šaty Lena', uk: 'Сукня міді Лена' },
    descriptions: {
      pl: 'Zwiewna sukienka midi z wiązaniem przy dekolcie, gumką w talii, falbankami przy rękawach i falbaną na dole.',
      en: 'Airy midi dress with a tie neckline, elasticated waist, ruffled cuffs and a ruffle hem.',
      cs: 'Vzdušné midi šaty s vázáním u výstřihu, gumou v pase, volánky na rukávech a volánem na spodním okraji.',
      uk: 'Легка сукня міді із зав’язками біля вирізу, гумкою на талії, воланами на рукавах і воланом унизу.',
    },
    colors: [{ code: 'KOBALT-KWIATY', images: [S + '823154175_1482179507071220_7143974582879534125_n.jpeg', S + '820381548_1097754352744012_683884574669880362_n.jpeg', S + '822077757_2148306792772122_4406965447473376640_n.jpeg'] }],
  },
  {
    styleCode: 'FS-2604', slug: 'sukienka-nina', category: 'sukienki-midi', wholesale: '139.00', retail: '329.00',
    featured: false, sortOrder: 3, status: 'HIDDEN',
    names: { pl: 'Sukienka Nina', en: 'Nina dress', cs: 'Šaty Nina', uk: 'Сукня Ніна' },
    descriptions: {
      pl: 'Sukienka do kolan z okrągłym dekoltem, marszczeniem pod biustem, rozkloszowanym dołem i rękawem 3/4. Abstrakcyjny print na ciemnym tle.',
      en: 'Knee-length dress with a round neckline, gathered empire waist, flared skirt and 3/4 sleeves. Abstract print on a dark ground.',
      cs: 'Šaty ke kolenům s kulatým výstřihem, nařasením pod prsy, rozšířenou sukní a 3/4 rukávem. Abstraktní potisk na tmavém podkladu.',
      uk: 'Сукня до колін з круглим вирізом, збираннями під грудьми, розкльошеним низом і рукавом 3/4. Абстрактний принт на темному тлі.',
    },
    colors: [{ code: 'GRAFIT-ABSTRAKCJA', images: [S + '825329197_2008624156497935_1299546619021708573_n.jpeg', S + '825279603_1140121891673639_8944365580202601028_n.jpeg'] }],
  },
  {
    styleCode: 'FS-2605', slug: 'sukienka-dopasowana-iga', category: 'sukienki-dopasowane', wholesale: '129.00', retail: '299.00',
    featured: true, sortOrder: 4, status: 'PUBLISHED',
    names: { pl: 'Sukienka dopasowana Iga', en: 'Iga fitted dress', cs: 'Přiléhavé šaty Iga', uk: 'Приталена сукня Іга' },
    descriptions: {
      pl: 'Dopasowana sukienka do kolan z okrągłym dekoltem i rękawem 3/4. Dostępna w printach marmurkowych i paisley.',
      en: 'Fitted knee-length dress with a round neckline and 3/4 sleeves. Available in marble and paisley prints.',
      cs: 'Přiléhavé šaty ke kolenům s kulatým výstřihem a 3/4 rukávem. K dispozici v mramorovém a paisley potisku.',
      uk: 'Приталена сукня до колін з круглим вирізом і рукавом 3/4. Доступна в мармурових принтах і пейслі.',
    },
    colors: [
      { code: 'MORSKI-MARMUR', images: [AI + '830977903_3630063807150579_6249702151230893437_n.jpeg'] },
      { code: 'BORDO-MARMUR', images: [AI + '834286499_4007390972901691_6366017940278401661_n.jpeg'] },
      { code: 'GRANAT-PAISLEY', images: [AI + '829910077_1387107916913917_5239437122828130041_n.jpeg'] },
    ],
  },
  {
    styleCode: 'FS-2606', slug: 'sukienka-olowkowa-mira', category: 'sukienki-dopasowane', wholesale: '119.00', retail: '279.00',
    featured: false, sortOrder: 5, status: 'DRAFT',
    names: { pl: 'Sukienka ołówkowa Mira', en: 'Mira pencil dress', cs: 'Pouzdrové šaty Mira', uk: 'Сукня-олівець Міра' },
    descriptions: {
      pl: 'Ołówkowa sukienka do kolan z okrągłym dekoltem i rękawem 3/4, we wzór plastra miodu.',
      en: 'Knee-length pencil dress with a round neckline and 3/4 sleeves in a honeycomb pattern.',
      cs: 'Pouzdrové šaty ke kolenům s kulatým výstřihem a 3/4 rukávem se vzorem včelí plástve.',
      uk: 'Сукня-олівець до колін з круглим вирізом і рукавом 3/4 у візерунок стільників.',
    },
    colors: [{ code: 'CZARNY-PLASTER', images: [AI + '797188353_1068259106004779_7549502093814094890_n.jpeg'] }],
  },
  {
    styleCode: 'FS-2607', slug: 'sukienka-z-falbana-ada', category: 'sukienki-dopasowane', wholesale: '139.00', retail: '319.00',
    featured: false, sortOrder: 6, status: 'PUBLISHED', omitSizes: ['36'],
    names: { pl: 'Sukienka z falbaną Ada', en: 'Ada flounce dress', cs: 'Šaty s volánem Ada', uk: 'Сукня з воланом Ада' },
    descriptions: {
      pl: 'Gładka, dopasowana sukienka za kolano z okrągłym dekoltem, długim rękawem i falbaną na dole.',
      en: 'Plain fitted below-the-knee dress with a round neckline, long sleeves and a flounce hem.',
      cs: 'Jednobarevné přiléhavé šaty pod kolena s kulatým výstřihem, dlouhým rukávem a volánem na spodním okraji.',
      uk: 'Однотонна приталена сукня нижче колін з круглим вирізом, довгим рукавом і воланом унизу.',
    },
    colors: [
      { code: 'CZEKOLADA', images: [AI + '814428290_1049928174346903_1255577126030399663_n.jpeg'] },
      { code: 'BORDO', images: [AI + '814624356_28648799151411998_3786180424875630331_n.jpeg'] },
    ],
  },
  {
    styleCode: 'FS-2608', slug: 'sukienka-kopertowa-ewa', category: 'sukienki-midi', wholesale: '159.00', retail: '369.00',
    featured: false, sortOrder: 7, status: 'PUBLISHED',
    names: { pl: 'Sukienka kopertowa Ewa', en: 'Ewa wrap dress', cs: 'Zavinovací šaty Ewa', uk: 'Сукня на запах Єва' },
    descriptions: {
      pl: 'Kopertowa sukienka midi z dekoltem w szpic, bufiastym długim rękawem na gumce i rozkloszowanym dołem.',
      en: 'Wrap midi dress with a V-neckline, long puffed sleeves with elasticated cuffs and a flared skirt.',
      cs: 'Zavinovací midi šaty s výstřihem do V, dlouhým nabíraným rukávem s gumou a rozšířenou sukní.',
      uk: 'Сукня міді на запах з V-подібним вирізом, довгим пишним рукавом на гумці та розкльошеним низом.',
    },
    colors: [{ code: 'ZIELEN-PIORA', images: [AI + '826878636_1070890229036678_3450560418896989688_n.jpeg'] }],
  },
  {
    styleCode: 'FS-2609', slug: 'sukienka-midi-julia', category: 'sukienki-midi', wholesale: '149.00', retail: '349.00',
    featured: false, sortOrder: 8, status: 'PUBLISHED',
    names: { pl: 'Sukienka midi Julia', en: 'Julia midi dress', cs: 'Midi šaty Julia', uk: 'Сукня міді Юлія' },
    descriptions: {
      pl: 'Rozkloszowana sukienka midi z okrągłym dekoltem, zakładkami w talii i rękawem 3/4. Trzy printy w ciepłych i ciemnych tonacjach.',
      en: 'Flared midi dress with a round neckline, waist pleats and 3/4 sleeves. Three prints in warm and dark tones.',
      cs: 'Rozšířené midi šaty s kulatým výstřihem, sklady v pase a 3/4 rukávem. Tři potisky v teplých a tmavých tónech.',
      uk: 'Розкльошена сукня міді з круглим вирізом, складками на талії та рукавом 3/4. Три принти в теплих і темних тонах.',
    },
    colors: [
      { code: 'BEZ-BATIK', images: [AI + '829806353_912330511759543_559768821842293300_n.jpeg'] },
      { code: 'GRANAT-MIEDZ', images: [AI + '827693811_1095133646551597_4635502676686791982_n.jpeg'] },
      { code: 'BEZ-PIORA', images: [AI + '829651184_1113586404495310_3235373777571052705_n.jpeg'] },
    ],
  },
];

const SALES_REP = { fullName: "Fabian's Style B2B", email: 'hurt@example.com', phone: '+48 000 000 000', active: true, sortOrder: 0 };

// ---------------------------------------------------------------------------------------------- API

let token;

async function api(method, url, body, { form } = {}) {
  const headers = { Authorization: `Bearer ${token}` };
  let payload;
  if (form) {
    payload = form;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const res = await fetch(API + url, { method, headers, body: payload });
  const text = await res.text();
  const json = text ? JSON.parse(text) : null;
  if (!res.ok) {
    throw new Error(`${method} ${url} → ${res.status} ${json?.code ?? ''} ${json?.detail ?? text}`);
  }
  return json;
}

async function login() {
  const res = await fetch(`${API}/api/v1/admin/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
    body: JSON.stringify({ email: EMAIL, password: PASSWORD }),
  });
  if (!res.ok) throw new Error(`Login failed: ${res.status} ${await res.text()}`);
  token = (await res.json()).accessToken;
}

/** EAN-13 z puli restricted circulation (prefiks 29) z poprawną cyfrą kontrolną. */
function ean13(styleCode, colorIndex, size) {
  const base = `29${styleCode.replace(/\D/g, '').padStart(4, '0')}${String(colorIndex).padStart(2, '0')}${size}00`;
  const sum = [...base].reduce((acc, d, i) => acc + Number(d) * (i % 2 === 0 ? 1 : 3), 0);
  return base + ((10 - (sum % 10)) % 10);
}

// ---------------------------------------------------------------------------------------------- seed

async function main() {
  await login();
  const created = { categories: 0, colors: 0, sizes: 0, products: 0, variants: 0, images: 0, statusChanges: 0, reps: 0 };

  const categories = new Map((await api('GET', '/api/v1/admin/categories')).map((c) => [c.slug, c.id]));
  for (const c of CATEGORIES) {
    if (!categories.has(c.slug)) {
      const { id } = await api('POST', '/api/v1/admin/categories', { ...c, visible: true });
      categories.set(c.slug, id);
      created.categories++;
    }
  }

  const colors = new Map((await api('GET', '/api/v1/admin/colors')).map((c) => [c.code, c.id]));
  for (const [i, c] of COLORS.entries()) {
    if (!colors.has(c.code)) {
      const { id } = await api('POST', '/api/v1/admin/colors', { code: c.code, hex: c.hex, sortOrder: i, names: c.names });
      colors.set(c.code, id);
      created.colors++;
    }
  }

  const sizes = new Map((await api('GET', '/api/v1/admin/sizes')).map((s) => [s.code, s.id]));
  for (const [i, code] of SIZES.entries()) {
    if (!sizes.has(code)) {
      const { id } = await api('POST', '/api/v1/admin/sizes', { code, sortOrder: (i + 1) * 10 });
      sizes.set(code, id);
      created.sizes++;
    }
  }

  const summary = [];
  for (const p of PRODUCTS) {
    const search = await api('GET', `/api/v1/admin/products?q=${encodeURIComponent(p.styleCode)}&size=100`);
    let id = search.items.find((item) => item.styleCode === p.styleCode)?.id;
    if (!id) {
      const translations = Object.fromEntries(Object.keys(p.names).map((lang) => [lang, {
        name: p.names[lang], description: p.descriptions[lang],
      }]));
      ({ id } = await api('POST', '/api/v1/admin/products', {
        categoryId: categories.get(p.category), slug: p.slug, styleCode: p.styleCode,
        wholesalePrice: p.wholesale, retailPrice: p.retail, currency: 'PLN',
        featured: p.featured, sortOrder: p.sortOrder, translations,
      }));
      created.products++;
    }

    let product = await api('GET', `/api/v1/admin/products/${id}`);
    const existingSkus = new Set(product.variants.map((v) => v.sku));
    for (const color of p.colors) {
      const colorDef = COLORS.find((c) => c.code === color.code);
      const colorIndex = COLORS.indexOf(colorDef) + 1;
      for (const size of SIZES.filter((s) => !(p.omitSizes ?? []).includes(s))) {
        const sku = `${p.styleCode.replace('-', '')}-${colorDef.short}-${size}`;
        if (existingSkus.has(sku)) continue;
        await api('POST', `/api/v1/admin/products/${id}/variants`, {
          colorId: colors.get(color.code), sizeId: sizes.get(size), sku, ean: ean13(p.styleCode, colorIndex, size),
        });
        created.variants++;
      }
    }

    product = await api('GET', `/api/v1/admin/products/${id}`);
    for (const color of p.colors) {
      const colorId = colors.get(color.code);
      if (product.images.some((image) => image.colorId === colorId)) continue;
      for (const file of color.images) {
        const bytes = await readFile(path.join(ASSETS, file));
        const form = new FormData();
        form.append('file', new Blob([bytes], { type: 'image/jpeg' }), path.basename(file));
        form.append('colorId', colorId);
        await api('POST', `/api/v1/admin/products/${id}/images`, undefined, { form });
        created.images++;
      }
    }

    product = await api('GET', `/api/v1/admin/products/${id}`);
    const transitions = {
      PUBLISHED: { DRAFT: ['PUBLISH'], HIDDEN: ['PUBLISH'], PUBLISHED: [] },
      HIDDEN: { DRAFT: ['PUBLISH', 'HIDE'], PUBLISHED: ['HIDE'], HIDDEN: [] },
      DRAFT: { DRAFT: [], PUBLISHED: [], HIDDEN: [] },
    }[p.status][product.status] ?? [];
    for (const transition of transitions) {
      await api('POST', `/api/v1/admin/products/${id}/status`, { transition, version: product.version });
      product = await api('GET', `/api/v1/admin/products/${id}`);
      created.statusChanges++;
    }
    summary.push(`${p.styleCode}  ${p.names.pl.padEnd(30)} ${product.status.padEnd(9)} ${product.colors.length} kol. · ${product.variants.length} war. · ${product.images.length} zdj.`);
  }

  const reps = await api('GET', '/api/v1/admin/sales-representatives');
  if (!reps.some((r) => r.email === SALES_REP.email)) {
    await api('POST', '/api/v1/admin/sales-representatives', SALES_REP);
    created.reps++;
  }

  console.log('DANE DEMONSTRACYJNE — utworzono:', created);
  console.log(summary.join('\n'));
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
