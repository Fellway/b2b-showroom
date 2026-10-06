package pl.wizytowka.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Strażnik architektury heksagonalnej: domena jest czystą Javą, aplikacja nie zna adapterów,
 * a moduły komunikują się wyłącznie przez swoje porty wejściowe (+ widoki i typy domenowe).
 */
class HexagonalArchitectureTest {

    private static final String ROOT = "pl.wizytowka";
    private static final List<String> MODULES = List.of("catalog", "linesheet", "analytics", "contact", "identity");
    private static final Pattern MODULE_PACKAGE = Pattern.compile("^pl\\.wizytowka\\.([a-z]+)(\\..*)?$");

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void domainIsFrameworkFree() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "org.hibernate..", "com.fasterxml..", "org.slf4j..")
                .check(classes);
    }

    @Test
    void domainDoesNotDependOnOuterLayers() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..", "..config..")
                .check(classes);
    }

    @Test
    void applicationDoesNotDependOnAdaptersOrInfrastructure() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapter..", "..config..", "jakarta.persistence..", "org.hibernate..",
                        "org.springframework.web..", "org.springframework.data..", "jakarta.servlet..")
                .check(classes);
    }

    @Test
    void webAdaptersUseOnlyInboundPorts() {
        noClasses().that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAnyPackage("..application.port.out..", "..adapter.out..", "..application.service..")
                .check(classes);
    }

    @Test
    void sharedKernelDoesNotDependOnModules() {
        noClasses().that().resideInAPackage(ROOT + ".shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        MODULES.stream().map(m -> ROOT + "." + m + "..").toArray(String[]::new))
                .check(classes);
    }

    @Test
    void modulesTalkOnlyThroughPublicApi() {
        classes().that().resideInAnyPackage(MODULES.stream().map(m -> ROOT + "." + m + "..").toArray(String[]::new))
                .should(onlyUsePublicApiOfOtherModules())
                .check(classes);
    }

    private static ArchCondition<JavaClass> onlyUsePublicApiOfOtherModules() {
        return new ArchCondition<>("access other modules only via application.port.in, application.view or domain") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String originModule = moduleOf(origin.getPackageName()).orElse(null);
                origin.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    moduleOf(target.getPackageName())
                            .filter(targetModule -> MODULES.contains(targetModule) && !targetModule.equals(originModule))
                            .filter(targetModule -> !isPublicApi(target.getPackageName(), targetModule))
                            .ifPresent(targetModule -> events.add(SimpleConditionEvent.violated(dependency,
                                    dependency.getDescription() + " — module '" + originModule
                                            + "' must use the public API of '" + targetModule + "'")));
                });
            }
        };
    }

    private static boolean isPublicApi(String packageName, String module) {
        String base = ROOT + "." + module + ".";
        return packageName.startsWith(base + "application.port.in")
                || packageName.startsWith(base + "application.view")
                || packageName.startsWith(base + "domain");
    }

    private static Optional<String> moduleOf(String packageName) {
        Matcher matcher = MODULE_PACKAGE.matcher(packageName);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
