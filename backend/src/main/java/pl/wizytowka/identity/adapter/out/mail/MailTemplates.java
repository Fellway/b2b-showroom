package pl.wizytowka.identity.adapter.out.mail;

import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.EnumMap;
import java.util.Map;

/**
 * Stałe treści e-maili transakcyjnych (zwykły tekst, 4 języki). Podstawiane są wyłącznie wartości wygenerowane
 * przez aplikację: nazwa marki z konfiguracji, linki z tokenem i czas ważności — nigdy dane wpisane przez
 * użytkownika (D23: formularz rejestracji nie może posłużyć do wysyłania treści spamera z domeny marki).
 */
final class MailTemplates {

    enum Kind { VERIFY_EMAIL, ACCOUNT_EXISTS, ACCOUNT_APPROVED, ACCOUNT_REJECTED, PASSWORD_RESET }

    record Message(String subject, String body) {
    }

    private record Template(String subject, String body) {
    }

    private static final Map<CatalogLanguage, Map<Kind, Template>> TEMPLATES = new EnumMap<>(CatalogLanguage.class);

    static {
        TEMPLATES.put(CatalogLanguage.PL, Map.of(
                Kind.VERIFY_EMAIL, new Template("Potwierdź adres e-mail — {brand} B2B", """
                        Dzień dobry,

                        dziękujemy za rejestrację w showroomie hurtowym {brand}. Aby potwierdzić adres e-mail, otwórz link:
                        {link}

                        Link jest ważny przez {validity}. Po potwierdzeniu adresu wniosek trafi do naszego zespołu, \
                        a o akceptacji konta poinformujemy w osobnej wiadomości.

                        Jeśli to nie Ty zakładałeś konto, zignoruj tę wiadomość."""),
                Kind.ACCOUNT_EXISTS, new Template("Twoje konto w showroomie {brand} B2B", """
                        Dzień dobry,

                        otrzymaliśmy prośbę o założenie konta na ten adres e-mail, ale konto już istnieje. Zaloguj się:
                        {link}

                        Jeśli nie pamiętasz hasła, wybierz „Nie pamiętam hasła” na stronie logowania. \
                        Jeśli to nie Ty, zignoruj tę wiadomość."""),
                Kind.ACCOUNT_APPROVED, new Template("Konto zaakceptowane — {brand} B2B", """
                        Dzień dobry,

                        Twoje konto w showroomie hurtowym {brand} zostało zaakceptowane. Po zalogowaniu zobaczysz ceny \
                        hurtowe i przygotujesz zestawienie PDF:
                        {link}

                        W razie pytań skontaktuj się ze swoim opiekunem handlowym."""),
                Kind.ACCOUNT_REJECTED, new Template("Wniosek o konto — {brand} B2B", """
                        Dzień dobry,

                        dziękujemy za zainteresowanie współpracą z {brand}. Niestety nie możemy teraz aktywować konta \
                        w showroomie hurtowym. Jeśli uważasz, że to pomyłka, skontaktuj się z nami — dane kontaktowe \
                        znajdziesz na stronie showroomu:
                        {link}"""),
                Kind.PASSWORD_RESET, new Template("Ustaw nowe hasło — {brand} B2B", """
                        Dzień dobry,

                        otrzymaliśmy prośbę o zmianę hasła do konta w showroomie {brand}. Aby ustawić nowe hasło, otwórz link:
                        {link}

                        Link jest ważny przez {validity} i można go użyć tylko raz. Jeśli to nie Ty, zignoruj tę \
                        wiadomość — hasło pozostanie bez zmian.""")));

        TEMPLATES.put(CatalogLanguage.EN, Map.of(
                Kind.VERIFY_EMAIL, new Template("Confirm your e-mail address — {brand} B2B", """
                        Hello,

                        thank you for registering with the {brand} wholesale showroom. To confirm your e-mail address, open this link:
                        {link}

                        The link is valid for {validity}. Once your address is confirmed, our team will review your \
                        application and let you know in a separate message when your account is approved.

                        If you did not create this account, please ignore this message."""),
                Kind.ACCOUNT_EXISTS, new Template("Your {brand} B2B showroom account", """
                        Hello,

                        we received a request to create an account for this e-mail address, but an account already exists. Sign in here:
                        {link}

                        If you have forgotten your password, choose “Forgot password” on the sign-in page. \
                        If this was not you, please ignore this message."""),
                Kind.ACCOUNT_APPROVED, new Template("Account approved — {brand} B2B", """
                        Hello,

                        your {brand} wholesale showroom account has been approved. Sign in to see wholesale prices \
                        and prepare a PDF line sheet:
                        {link}

                        If you have any questions, please contact your sales representative."""),
                Kind.ACCOUNT_REJECTED, new Template("Your account application — {brand} B2B", """
                        Hello,

                        thank you for your interest in working with {brand}. Unfortunately, we cannot activate a \
                        wholesale showroom account at this time. If you believe this is a mistake, please contact us — \
                        you will find our contact details in the showroom:
                        {link}"""),
                Kind.PASSWORD_RESET, new Template("Set a new password — {brand} B2B", """
                        Hello,

                        we received a request to change the password for your {brand} showroom account. To set a new password, open this link:
                        {link}

                        The link is valid for {validity} and can be used only once. If this was not you, please ignore \
                        this message — your password will stay the same.""")));

        TEMPLATES.put(CatalogLanguage.CS, Map.of(
                Kind.VERIFY_EMAIL, new Template("Potvrďte e-mailovou adresu — {brand} B2B", """
                        Dobrý den,

                        děkujeme za registraci ve velkoobchodním showroomu {brand}. Pro potvrzení e-mailové adresy otevřete odkaz:
                        {link}

                        Odkaz je platný {validity}. Po potvrzení adresy předáme vaši žádost našemu týmu a o schválení \
                        účtu vás budeme informovat samostatnou zprávou.

                        Pokud jste účet nezakládali vy, tuto zprávu ignorujte."""),
                Kind.ACCOUNT_EXISTS, new Template("Váš účet v showroomu {brand} B2B", """
                        Dobrý den,

                        obdrželi jsme žádost o založení účtu pro tuto e-mailovou adresu, ale účet již existuje. Přihlaste se:
                        {link}

                        Pokud si nepamatujete heslo, zvolte na přihlašovací stránce „Zapomenuté heslo“. \
                        Pokud jste to nebyli vy, tuto zprávu ignorujte."""),
                Kind.ACCOUNT_APPROVED, new Template("Účet schválen — {brand} B2B", """
                        Dobrý den,

                        váš účet ve velkoobchodním showroomu {brand} byl schválen. Po přihlášení uvidíte velkoobchodní \
                        ceny a připravíte si PDF přehled:
                        {link}

                        S případnými dotazy se obraťte na svého obchodního zástupce."""),
                Kind.ACCOUNT_REJECTED, new Template("Žádost o účet — {brand} B2B", """
                        Dobrý den,

                        děkujeme za zájem o spolupráci se značkou {brand}. Bohužel vám nyní nemůžeme aktivovat účet \
                        ve velkoobchodním showroomu. Pokud se domníváte, že jde o omyl, kontaktujte nás — kontaktní údaje \
                        najdete v showroomu:
                        {link}"""),
                Kind.PASSWORD_RESET, new Template("Nastavte si nové heslo — {brand} B2B", """
                        Dobrý den,

                        obdrželi jsme žádost o změnu hesla k vašemu účtu v showroomu {brand}. Nové heslo nastavíte přes odkaz:
                        {link}

                        Odkaz je platný {validity} a lze jej použít pouze jednou. Pokud jste to nebyli vy, tuto zprávu \
                        ignorujte — heslo zůstane beze změny.""")));

        TEMPLATES.put(CatalogLanguage.UK, Map.of(
                Kind.VERIFY_EMAIL, new Template("Підтвердьте адресу електронної пошти — {brand} B2B", """
                        Добрий день,

                        дякуємо за реєстрацію в оптовому шоурумі {brand}. Щоб підтвердити адресу електронної пошти, відкрийте посилання:
                        {link}

                        Посилання дійсне протягом {validity}. Після підтвердження адреси ваша заявка надійде нашій \
                        команді, а про схвалення облікового запису ми повідомимо окремим листом.

                        Якщо ви не створювали обліковий запис, просто проігноруйте цей лист."""),
                Kind.ACCOUNT_EXISTS, new Template("Ваш обліковий запис у шоурумі {brand} B2B", """
                        Добрий день,

                        ми отримали запит на створення облікового запису для цієї адреси, але обліковий запис уже існує. Увійдіть:
                        {link}

                        Якщо ви не пам’ятаєте пароль, оберіть «Забули пароль?» на сторінці входу. \
                        Якщо це були не ви, просто проігноруйте цей лист."""),
                Kind.ACCOUNT_APPROVED, new Template("Обліковий запис схвалено — {brand} B2B", """
                        Добрий день,

                        ваш обліковий запис в оптовому шоурумі {brand} схвалено. Після входу ви побачите оптові ціни \
                        та зможете підготувати PDF-добірку:
                        {link}

                        Якщо у вас виникнуть запитання, зверніться до свого менеджера."""),
                Kind.ACCOUNT_REJECTED, new Template("Заявка на обліковий запис — {brand} B2B", """
                        Добрий день,

                        дякуємо за інтерес до співпраці з {brand}. На жаль, зараз ми не можемо активувати обліковий \
                        запис в оптовому шоурумі. Якщо ви вважаєте, що це помилка, зв’яжіться з нами — контактні дані \
                        є в шоурумі:
                        {link}"""),
                Kind.PASSWORD_RESET, new Template("Встановіть новий пароль — {brand} B2B", """
                        Добрий день,

                        ми отримали запит на зміну пароля до вашого облікового запису в шоурумі {brand}. Щоб встановити новий пароль, відкрийте посилання:
                        {link}

                        Посилання дійсне протягом {validity}, і його можна використати лише один раз. Якщо це були \
                        не ви, проігноруйте цей лист — пароль залишиться без змін.""")));
    }

    /** Powiadomienie zespołu marki — zawsze po polsku (panel jest tylko po polsku). */
    static Message staffPendingBuyer(String panelLink) {
        return new Message("Nowy wniosek o konto kupca", """
                Kupiec potwierdził adres e-mail i czeka na akceptację konta w showroomie hurtowym.

                Wniosek sprawdzisz w panelu:
                %s""".formatted(panelLink));
    }

    private MailTemplates() {
    }

    static Message render(Kind kind, CatalogLanguage language, String brand, String link, String validity) {
        Template template = TEMPLATES.getOrDefault(language, TEMPLATES.get(CatalogLanguage.DEFAULT)).get(kind);
        return new Message(fill(template.subject(), brand, link, validity), fill(template.body(), brand, link, validity));
    }

    private static String fill(String text, String brand, String link, String validity) {
        return text.replace("{brand}", brand)
                .replace("{link}", link == null ? "" : link)
                .replace("{validity}", validity == null ? "" : validity);
    }
}
