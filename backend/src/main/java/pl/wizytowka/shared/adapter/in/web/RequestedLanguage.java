package pl.wizytowka.shared.adapter.in.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Oznacza parametr {@link pl.wizytowka.shared.domain.CatalogLanguage} kontrolera rozwiązywany z żądania:
 * parametr {@code ?lang=}, potem nagłówek {@code Accept-Language}, na końcu język bazowy.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestedLanguage {
}
