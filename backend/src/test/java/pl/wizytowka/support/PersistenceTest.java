package pl.wizytowka.support;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Slice persystencji na prawdziwym PostgreSQL: schemat z migracji Flyway, Hibernate tylko go waliduje.
 * Dodatkowo skanuje adaptery persystencji ({@code ..adapter.out.persistence..}), żeby testować je przez porty.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, PersistenceAdaptersConfiguration.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
public @interface PersistenceTest {
}
