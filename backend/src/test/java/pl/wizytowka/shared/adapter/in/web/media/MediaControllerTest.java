package pl.wizytowka.shared.adapter.in.web.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.wizytowka.shared.adapter.in.web.ApiExceptionHandler;
import pl.wizytowka.shared.adapter.in.web.WebConfig;
import pl.wizytowka.shared.application.port.in.PublicMediaQuery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebConfig.class, ApiExceptionHandler.class})
class MediaControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PublicMediaQuery storage;

    @Test
    void streamsProductImageWithImmutableCachingAndClosesStream() throws Exception {
        byte[] bytes = {1, 2, 3, 4};
        AtomicBoolean closed = new AtomicBoolean();
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        String key = "products/0199b0c2-aaaa-7bbb-8ccc-000000000001/0199b0c2-aaaa-7bbb-8ccc-000000000002.webp";
        when(storage.open(key)).thenReturn(Optional.of(new PublicMediaQuery.MediaFile(stream, bytes.length, "image/webp")));

        mvc.perform(get("/api/v1/media/" + key))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(bytes))
                .andExpect(header().longValue("Content-Length", bytes.length))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", "inline"));
        assertThat(closed).isTrue();
    }

    @Test
    void returns404WhenFileMissing() throws Exception {
        when(storage.open("products/p/missing.jpg")).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/media/products/p/missing.jpg")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/media/line-sheets/2026/abc.pdf",
            "/api/v1/media/products/../line-sheets/abc.pdf",
            "/api/v1/media/products/%2e%2e/line-sheets/abc.pdf",
            "/api/v1/media/products/..%2fline-sheets%2fabc.pdf",
            "/api/v1/media/products/.hidden",
            "/api/v1/media/products/a%5c..%5cb.jpg",
            "/api/v1/media/products",
            "/api/v1/media/",
            "/api/v1/media/products/a b.jpg"
    })
    void neverTouchesStorageForKeysOutsideProductImages(String path) throws Exception {
        int statusCode = mvc.perform(get(path)).andReturn().getResponse().getStatus();

        assertThat(statusCode).isIn(400, 404);
        verify(storage, never()).open(anyString());
    }
}
