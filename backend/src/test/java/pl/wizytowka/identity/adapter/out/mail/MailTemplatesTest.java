package pl.wizytowka.identity.adapter.out.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.wizytowka.shared.domain.CatalogLanguage;

import static org.assertj.core.api.Assertions.assertThat;

class MailTemplatesTest {

    @ParameterizedTest
    @EnumSource(CatalogLanguage.class)
    void everyTemplateExistsInEveryLanguageAndFillsAllPlaceholders(CatalogLanguage language) {
        for (MailTemplates.Kind kind : MailTemplates.Kind.values()) {
            MailTemplates.Message message = MailTemplates.render(kind, language, "Fabian's Style",
                    "https://b2b.example.com/x?token=abc", "48 h");

            assertThat(message.subject()).as("%s/%s", kind, language).contains("Fabian's Style").doesNotContain("{");
            assertThat(message.body()).as("%s/%s", kind, language)
                    .contains("https://b2b.example.com/x?token=abc")
                    .doesNotContain("{brand}", "{link}", "{validity}");
        }
    }

    @Test
    void linkBearingTemplatesMentionValidity() {
        assertThat(MailTemplates.render(MailTemplates.Kind.VERIFY_EMAIL, CatalogLanguage.PL, "B", "L", "48 h").body())
                .contains("48 h");
        assertThat(MailTemplates.render(MailTemplates.Kind.PASSWORD_RESET, CatalogLanguage.UK, "B", "L", "1 h").body())
                .contains("1 h");
    }

    @Test
    void staffNotificationContainsOnlyPanelLink() {
        MailTemplates.Message message = MailTemplates.staffPendingBuyer("https://b2b.example.com/admin/buyers");

        assertThat(message.body()).contains("https://b2b.example.com/admin/buyers");
    }
}
