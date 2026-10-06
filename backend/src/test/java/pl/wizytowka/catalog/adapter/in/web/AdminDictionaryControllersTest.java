package pl.wizytowka.catalog.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.wizytowka.catalog.application.port.in.AdminCatalogQueryUseCase;
import pl.wizytowka.catalog.application.port.in.ManageCategoriesUseCase;
import pl.wizytowka.catalog.application.port.in.ManageColorsUseCase;
import pl.wizytowka.catalog.application.port.in.ManageSizesUseCase;
import pl.wizytowka.catalog.application.view.AdminCategoryView;
import pl.wizytowka.catalog.application.view.AdminColorView;
import pl.wizytowka.catalog.application.view.AdminSizeView;
import pl.wizytowka.catalog.domain.CategoryDetails;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.CategoryTranslation;
import pl.wizytowka.catalog.domain.ColorDetails;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.shared.adapter.in.web.ApiExceptionHandler;
import pl.wizytowka.shared.adapter.in.web.WebConfig;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ResourceConflictException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminCategoryController.class, AdminColorController.class, AdminSizeController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({WebConfig.class, ApiExceptionHandler.class})
class AdminDictionaryControllersTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ManageCategoriesUseCase categories;

    @MockitoBean
    ManageColorsUseCase colors;

    @MockitoBean
    ManageSizesUseCase sizes;

    @MockitoBean
    AdminCatalogQueryUseCase queries;

    @Test
    void createsCategoryWithParentAndDefaultVisibility() throws Exception {
        UUID parent = UUID.randomUUID();
        UUID created = UUID.randomUUID();
        when(categories.create(any())).thenReturn(CategoryId.of(created));

        mvc.perform(post("/api/v1/admin/categories").contentType(MediaType.APPLICATION_JSON).content("""
                        {"parentId":"%s","slug":"koszule-lniane","sortOrder":2,
                         "translations":{"pl":{"name":"Koszule lniane"},"uk":{"name":"Лляні сорочки"}}}"""
                        .formatted(parent)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/admin/categories/" + created));

        ArgumentCaptor<CategoryDetails> details = ArgumentCaptor.forClass(CategoryDetails.class);
        verify(categories).create(details.capture());
        assertThat(details.getValue().parentId()).isEqualTo(CategoryId.of(parent));
        assertThat(details.getValue().visible()).isTrue();
        assertThat(details.getValue().translations().resolve(CatalogLanguage.UK).name()).isEqualTo("Лляні сорочки");
    }

    @Test
    void rejectsInvalidCategorySlugAndMissingVersionOnUpdate() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/api/v1/admin/categories").contentType(MediaType.APPLICATION_JSON).content("""
                        {"slug":"Koszule Lniane","translations":{"pl":{"name":"Koszule"}}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("catalog.category.slug"));

        mvc.perform(put("/api/v1/admin/categories/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"slug":"koszule","translations":{"pl":{"name":"Koszule"}}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.version-required"));
        verifyNoInteractions(categories);

        mvc.perform(put("/api/v1/admin/categories/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"slug":"koszule","visible":false,"translations":{"pl":{"name":"Koszule"}},"version":3}"""))
                .andExpect(status().isNoContent());
        ArgumentCaptor<CategoryDetails> details = ArgumentCaptor.forClass(CategoryDetails.class);
        verify(categories).update(org.mockito.ArgumentMatchers.eq(CategoryId.of(id)),
                org.mockito.ArgumentMatchers.eq(3L), details.capture());
        assertThat(details.getValue().visible()).isFalse();
    }

    @Test
    void listsCategoriesWithTranslationsKeyedByLanguageCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(queries.categories()).thenReturn(List.of(new AdminCategoryView(id, null, "koszule", 0, true,
                Map.of(CatalogLanguage.PL, new CategoryTranslation("Koszule", null)), 12, 1)));

        mvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].translations.pl.name").value("Koszule"))
                .andExpect(jsonPath("$[0].productCount").value(12));
    }

    @Test
    void mapsCategoryInUseToConflict() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceConflictException("catalog.category.in-use", "Category has products"))
                .when(categories).delete(CategoryId.of(id));

        mvc.perform(delete("/api/v1/admin/categories/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("catalog.category.in-use"));
    }

    @Test
    void managesColors() throws Exception {
        UUID id = UUID.randomUUID();
        when(colors.create(any())).thenReturn(ColorId.of(id));
        when(queries.colors()).thenReturn(List.of(new AdminColorView(id, "NAVY", "#1C2340", 1,
                Map.of(CatalogLanguage.PL, "Granatowy", CatalogLanguage.EN, "Navy"), 3, 0)));

        mvc.perform(post("/api/v1/admin/colors").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"navy","hex":"#1c2340","sortOrder":1,"names":{"pl":"Granatowy","en":"Navy"}}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
        ArgumentCaptor<ColorDetails> details = ArgumentCaptor.forClass(ColorDetails.class);
        verify(colors).create(details.capture());
        assertThat(details.getValue().code()).isEqualTo("NAVY");
        assertThat(details.getValue().hexValue()).isEqualTo("#1C2340");

        mvc.perform(post("/api/v1/admin/colors").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"RED","hex":"red","names":{"pl":"Czerwony"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("catalog.color.hex"));

        mvc.perform(post("/api/v1/admin/colors").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"RED","names":{"pl":""}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalid"));

        mvc.perform(get("/api/v1/admin/colors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].names.pl").value("Granatowy"))
                .andExpect(jsonPath("$[0].names.en").value("Navy"));

        mvc.perform(put("/api/v1/admin/colors/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"NAVY","names":{"pl":"Granat"},"version":0}"""))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/admin/colors/{id}", id)).andExpect(status().isNoContent());
        verify(colors).delete(ColorId.of(id));
    }

    @Test
    void managesSizes() throws Exception {
        UUID id = UUID.randomUUID();
        when(sizes.create("xl", 50)).thenReturn(SizeId.of(id));
        when(queries.sizes()).thenReturn(List.of(new AdminSizeView(id, "XL", 50, 8, 2)));

        mvc.perform(post("/api/v1/admin/sizes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"xl\",\"sortOrder\":50}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/admin/sizes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"XL\",\"sortOrder\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sortOrder"));

        mvc.perform(put("/api/v1/admin/sizes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"XL\",\"sortOrder\":55,\"version\":2}"))
                .andExpect(status().isNoContent());
        verify(sizes).update(SizeId.of(id), 2L, "XL", 55);

        mvc.perform(get("/api/v1/admin/sizes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].variantCount").value(8));

        mvc.perform(delete("/api/v1/admin/sizes/{id}", id)).andExpect(status().isNoContent());
        verify(sizes).delete(SizeId.of(id));
    }
}
