package pl.wizytowka.linesheet.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.wizytowka.linesheet.application.port.in.AdminLineSheetQuery;
import pl.wizytowka.linesheet.application.port.in.CreateLineSheetCommand;
import pl.wizytowka.linesheet.application.port.in.CreateLineSheetUseCase;
import pl.wizytowka.linesheet.application.port.in.GetLineSheetPdfUseCase;
import pl.wizytowka.linesheet.application.view.CreatedLineSheet;
import pl.wizytowka.linesheet.application.view.LineSheetPdf;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PublicLineSheetController.class, AdminLineSheetController.class})
@AutoConfigureMockMvc(addFilters = false)
class LineSheetControllersTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CreateLineSheetUseCase createLineSheet;

    @MockitoBean
    GetLineSheetPdfUseCase getPdf;

    @MockitoBean
    AdminLineSheetQuery adminQuery;

    @Test
    void createsLineSheetInRequestedLanguage() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        given(createLineSheet.create(any())).willReturn(new CreatedLineSheet(publicId, "LS-2026-000042"));

        mvc.perform(post("/api/v1/line-sheets?lang=cs").contentType(MediaType.APPLICATION_JSON).content("""
                        {"items":[{"variantId":"%s","quantity":6}],"email":"buyer@boutique.cz",
                         "privacyAccepted":true}""".formatted(variantId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/line-sheets/" + publicId + "/pdf"))
                .andExpect(jsonPath("$.referenceNumber").value("LS-2026-000042"))
                .andExpect(jsonPath("$.pdfUrl").value("/api/v1/line-sheets/" + publicId + "/pdf"));

        ArgumentCaptor<CreateLineSheetCommand> command = ArgumentCaptor.forClass(CreateLineSheetCommand.class);
        verify(createLineSheet).create(command.capture());
        assertThat(command.getValue().language()).isEqualTo(CatalogLanguage.CS);
        assertThat(command.getValue().selections()).containsExactly(new CreateLineSheetCommand.Selection(variantId, 6));
        assertThat(command.getValue().email()).isEqualTo("buyer@boutique.cz");
        assertThat(command.getValue().privacyAccepted()).isTrue();
    }

    @Test
    void validatesRequestAndMapsDomainErrors() throws Exception {
        mvc.perform(post("/api/v1/line-sheets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"items":[],"email":"not-an-email"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalid"));

        given(createLineSheet.create(any())).willThrow(
                new BusinessRuleViolationException("catalog.variant.not-selectable", "Variant not selectable"));
        mvc.perform(post("/api/v1/line-sheets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"items":[{"variantId":"%s"}]}""".formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("catalog.variant.not-selectable"));
    }

    @Test
    void downloadsPdfWithPrivateHeaders() throws Exception {
        UUID publicId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.7".getBytes();
        given(getPdf.pdf(publicId)).willReturn(new LineSheetPdf(new ByteArrayInputStream(pdf), pdf.length, "LS-2026-000042.pdf"));

        mvc.perform(get("/api/v1/line-sheets/{id}/pdf", publicId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"LS-2026-000042.pdf\""))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"));

        UUID missing = UUID.randomUUID();
        given(getPdf.pdf(missing)).willThrow(new ResourceNotFoundException("linesheet.not-found", "Line sheet not found"));
        mvc.perform(get("/api/v1/line-sheets/{id}/pdf", missing)).andExpect(status().isNotFound());
    }

    @Test
    void listsLineSheetsForAdminAndHasNoEmailResend() throws Exception {
        UUID id = UUID.randomUUID();
        given(adminQuery.lineSheets(eq(new PageQuery(0, 20)))).willReturn(new PageResult<>(
                List.of(new LineSheetSummaryView(id, UUID.randomUUID(), "LS-2026-000001", CatalogLanguage.PL, "Firma",
                        null, "a@b.pl", 3, Instant.parse("2026-10-01T10:00:00Z"))),
                0, 20, 1));

        mvc.perform(get("/api/v1/admin/line-sheets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].referenceNumber").value("LS-2026-000001"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        // wysyłka e-mail została usunięta z aplikacji — endpoint nie istnieje
        mvc.perform(post("/api/v1/admin/line-sheets/{id}/resend-email", id))
                .andExpect(status().is4xxClientError());
    }
}
