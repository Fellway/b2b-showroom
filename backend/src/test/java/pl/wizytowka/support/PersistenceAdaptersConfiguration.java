package pl.wizytowka.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Rejestruje adaptery persystencji wszystkich modułów ({@code ..adapter.out.persistence..}) w slice'ie JPA. */
@TestConfiguration(proxyBeanMethods = false)
@ComponentScan(basePackages = "pl.wizytowka", useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM,
                classes = PersistenceAdaptersConfiguration.PersistenceComponentFilter.class))
public class PersistenceAdaptersConfiguration {

    /** Tylko komponenty Springa (@Component/@Repository...) z pakietów adapterów persystencji — bez encji i rekordów. */
    static class PersistenceComponentFilter implements TypeFilter {

        private static final Pattern PACKAGE = Pattern.compile("pl\\.wizytowka\\..*\\.adapter\\.out\\.persistence\\..*");

        @Override
        public boolean match(MetadataReader reader, MetadataReaderFactory factory) {
            var metadata = reader.getAnnotationMetadata();
            return PACKAGE.matcher(metadata.getClassName()).matches()
                    && (metadata.hasAnnotation(Component.class.getName())
                    || metadata.hasMetaAnnotation(Component.class.getName()));
        }
    }
}
