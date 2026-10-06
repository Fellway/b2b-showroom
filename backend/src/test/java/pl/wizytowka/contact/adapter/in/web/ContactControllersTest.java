package pl.wizytowka.contact.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.wizytowka.contact.application.port.in.ManageSalesRepresentativesUseCase;
import pl.wizytowka.contact.application.port.in.SalesRepresentativeQuery;
import pl.wizytowka.contact.application.view.AdminSalesRepresentativeView;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.contact.domain.SalesRepresentativeDetails;
import pl.wizytowka.contact.domain.SalesRepresentativeId;
import pl.wizytowka.shared.domain.ConcurrentModificationException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PublicContactController.class, AdminSalesRepresentativeController.class})
@AutoConfigureMockMvc(addFilters = false)
class ContactControllersTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ManageSalesRepresentativesUseCase manage;

    @MockitoBean
    SalesRepresentativeQuery query;

    @Test
    void listsActiveRepresentativesPublicly() throws Exception {
        given(query.activeRepresentatives()).willReturn(List.of(
                new SalesRepresentativeContact(UUID.randomUUID(), "Anna Nowak", "anna@brand.pl", "+48 600 100 200")));

        mvc.perform(get("/api/v1/contact/representatives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Anna Nowak"))
                .andExpect(jsonPath("$[0].version").doesNotExist());
    }

    @Test
    void createsRepresentative() throws Exception {
        SalesRepresentativeId id = SalesRepresentativeId.newId();
        given(manage.create(any(SalesRepresentativeDetails.class))).willReturn(id);
        given(query.representative(id)).willReturn(new AdminSalesRepresentativeView(id.value(), "Anna Nowak",
                "anna@brand.pl", null, true, 0, 0));

        mvc.perform(post("/api/v1/admin/sales-representatives").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Anna Nowak","email":"anna@brand.pl","active":true,"sortOrder":0}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/admin/sales-representatives/" + id))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void updateRequiresVersionAndReportsConflicts() throws Exception {
        UUID id = UUID.randomUUID();
        String body = """
                {"fullName":"Anna","email":"anna@brand.pl","active":true,"sortOrder":0%s}""";

        mvc.perform(put("/api/v1/admin/sales-representatives/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body.formatted("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.version-required"));

        willThrow(new ConcurrentModificationException("data.concurrent-modification", "stale"))
                .given(manage).update(eq(SalesRepresentativeId.of(id)), eq(3L), any());
        mvc.perform(put("/api/v1/admin/sales-representatives/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body.formatted(",\"version\":3")))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/v1/admin/sales-representatives/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        mvc.perform(post("/api/v1/admin/sales-representatives").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"","email":"x","sortOrder":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
    }
}
