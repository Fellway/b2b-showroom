package pl.wizytowka.catalog.adapter.in.web.dto;

import pl.wizytowka.catalog.application.view.ColorSwatchView;
import pl.wizytowka.catalog.application.view.ImageView;
import pl.wizytowka.catalog.application.view.PriceView;
import pl.wizytowka.shared.application.PageResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Odpowiedzi współdzielone przez publiczne i administracyjne API katalogu. */
public final class CommonResponses {

    public static final String MEDIA_PATH = "/api/v1/media/";

    private CommonResponses() {
    }

    public record IdResponse(UUID id) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

        public static <V, T> PageResponse<T> of(PageResult<V> result, Function<? super V, ? extends T> mapper) {
            return new PageResponse<>(result.items().stream().<T>map(mapper).toList(),
                    result.page(), result.size(), result.totalItems(), result.totalPages());
        }
    }

    public record PriceResponse(BigDecimal wholesale, BigDecimal retail, String currency, BigDecimal markup) {

        public static PriceResponse of(PriceView view) {
            return new PriceResponse(view.wholesale(), view.retail(), view.currency(), view.markup());
        }
    }

    /** @param url ścieżka względna do {@code MediaController}; frontend serwuje ją z tej samej domeny */
    public record ImageResponse(UUID id, UUID colorId, String url, Integer width, Integer height) {

        public static ImageResponse of(ImageView view) {
            if (view == null) {
                return null;
            }
            return new ImageResponse(view.id(), view.colorId(), mediaUrl(view.storageKey()), view.widthPx(), view.heightPx());
        }
    }

    public record ColorSwatchResponse(UUID id, String code, String name, String hex) {

        public static ColorSwatchResponse of(ColorSwatchView view) {
            return new ColorSwatchResponse(view.colorId(), view.code(), view.name(), view.hex());
        }
    }

    public static String mediaUrl(String storageKey) {
        return storageKey == null ? null : MEDIA_PATH + storageKey;
    }
}
