package pl.wizytowka.shared.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RequestedLanguageResolver implements HandlerMethodArgumentResolver {

    private static final List<Locale> SUPPORTED = Arrays.stream(CatalogLanguage.values())
            .map(CatalogLanguage::toLocale)
            .toList();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestedLanguage.class)
                && parameter.getParameterType() == CatalogLanguage.class;
    }

    @Override
    public CatalogLanguage resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                           NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String explicit = webRequest.getParameter("lang");
        if (explicit != null && !explicit.isBlank()) {
            return CatalogLanguage.fromCode(explicit)
                    .orElseThrow(() -> new InvalidValueException("i18n.unsupported-language",
                            "Supported languages: pl, en, cs, uk"));
        }
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String header = request == null ? null : request.getHeader("Accept-Language");
        if (header != null && !header.isBlank()) {
            try {
                Locale match = Locale.lookup(Locale.LanguageRange.parse(header), SUPPORTED);
                if (match != null) {
                    return CatalogLanguage.fromCode(match.getLanguage()).orElse(CatalogLanguage.DEFAULT);
                }
            } catch (IllegalArgumentException ignored) {
                // niepoprawny nagłówek — język bazowy
            }
        }
        return CatalogLanguage.DEFAULT;
    }
}
