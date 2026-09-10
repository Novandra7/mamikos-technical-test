package com.mamikos.kostapi.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Enforces the layering this codebase is designed around — web calls service, service calls
 * repository, entities know nothing about the web layer — as an executable rule rather than
 * a convention someone has to remember in review.
 */
class LayeredArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.mamikos.kostapi");
    }

    @Test
    void webLayerMustNotAccessRepositoriesDirectly() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..web..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..repository..");

        rule.check(classes);
    }

    @Test
    void entitiesMustNotDependOnTheWebLayer() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..entity..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..web..");

        rule.check(classes);
    }

    @Test
    void servicesMustNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..service..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Controller");

        rule.check(classes);
    }

    @Test
    void entitiesMustStoreEnumsAsStringsNeverAsOrdinals() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..entity..")
                .and()
                .areAnnotatedWith(jakarta.persistence.Entity.class)
                .should(new EnumeratedFieldsUseStringConvention());

        rule.check(classes);
    }

    private static final class EnumeratedFieldsUseStringConvention
            extends com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass> {

        private EnumeratedFieldsUseStringConvention() {
            super("store every @Enumerated field as STRING");
        }

        @Override
        public void check(
                com.tngtech.archunit.core.domain.JavaClass javaClass,
                com.tngtech.archunit.lang.ConditionEvents events) {
            javaClass.getFields().forEach(field -> field.tryGetAnnotationOfType(jakarta.persistence.Enumerated.class)
                    .ifPresent(enumerated -> {
                        if (enumerated.value() != jakarta.persistence.EnumType.STRING) {
                            events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(
                                    field,
                                    "%s.%s is @Enumerated(%s), not STRING — reordering the enum would silently remap existing rows"
                                            .formatted(
                                                    javaClass.getSimpleName(), field.getName(), enumerated.value())));
                        }
                    }));
        }
    }
}
