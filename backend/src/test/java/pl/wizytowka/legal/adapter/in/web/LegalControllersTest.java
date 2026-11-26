package pl.wizytowka.legal.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.wizytowka.legal.application.port.in.LegalDocumentQuery;
import pl.wizytowka.legal.application.port.in.ManageLegalDocumentsUseCase;
import pl.wizytowka.legal.application.view.AdminLegalVersionView;
import pl.wizytowka.legal.application.view.LegalDocumentView;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;
import pl.wizytowka.legal.domain.LegalDocumentVersionId;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.adapter.in.web.ApiExceptionHandler;
import pl.wizytowka.shared.adapter.in.web.WebConfig;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Role (publikacja tylko ADMIN) sprawdza konfiguracja bezpieczeństwa — tu filtry są wyłączone. */
@WebMvcTest({PublicLegalController.class, AdminLegalController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({WebConfig.class, ApiExceptionHandler.class})
class LegalControllersTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LegalDocumentQuery query;

    @MockitoBean
    ManageLegalDocumentsUseCase manage;

    @Test
    void publicDocumentInRequestedLanguageWithShortCache() throws Exception {
        UUID versionId = UUID.randomUUID();
        given(query.current(LegalDocumentType.PRIVACY_POLICY, CatalogLanguage.CS)).willReturn(java.util.Optional.of(
                new LegalDocumentView(versionId, LegalDocumentType.PRIVACY_POLICY, 2, Instant.parse("2026-11-25T10:00:00Z"),
                        false, CatalogLanguage.CS, "Zásady", "## 1. Správce", null)));

        mvc.perform(get("/api/v1/legal/privacy").header("Accept-Language", "cs-CZ"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=60")))
                .andExpect(jsonPath("$.versionId").value(versionId.toString()))
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.title").value("Zásady"));
        mvc.perform(get("/api/v1/legal/cookies")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("legal.document.not-found"));
        mvc.perform(get("/api/v1/legal/terms")).andExpect(status().isNotFound());
    }

    @Test
    void draftTextsAreMappedByLanguageCodeAndEmptyLanguagesSkipped() throws Exception {
        UUID id = UUID.randomUUID();
        given(manage.version(LegalDocumentVersionId.of(id))).willReturn(new AdminLegalVersionView(id,
                LegalDocumentType.TERMS, LegalDocumentVersion.Status.DRAFT, null, false, null, Instant.now(),
                Map.of(), List.of(CatalogLanguage.values()), 1));

        mvc.perform(put("/api/v1/admin/legal/versions/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"version":0,"texts":{"pl":{"title":"Regulamin","body":"Treść","changeSummary":"Nowe"},
                                              "en":{"title":"","body":""}}}"""))
                .andExpect(status().isOk());
        verify(manage).updateDraft(LegalDocumentVersionId.of(id), 0,
                Map.of(CatalogLanguage.PL, new LegalText("Regulamin", "Treść", "Nowe")));

        mvc.perform(put("/api/v1/admin/legal/versions/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"version":0,"texts":{"de":{"title":"AGB","body":"Text"}}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("legal.text.language"));
    }

    /** Publikację (z identyfikatorem ADMIN-a z tokenu) sprawdza ShowroomEndToEndTest. */
    @Test
    void createsDraftWithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        given(manage.createDraft(LegalDocumentType.TERMS)).willReturn(LegalDocumentVersionId.of(id));
        given(manage.version(LegalDocumentVersionId.of(id))).willReturn(new AdminLegalVersionView(id,
                LegalDocumentType.TERMS, LegalDocumentVersion.Status.DRAFT, null, false, null, Instant.now(),
                Map.of(), List.of(), 0));

        mvc.perform(post("/api/v1/admin/legal/terms/drafts"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/admin/legal/versions/" + id)))
                .andExpect(jsonPath("$.status").value("DRAFT"));
        mvc.perform(post("/api/v1/admin/legal/cookies/drafts")).andExpect(status().isNotFound());
    }
}
