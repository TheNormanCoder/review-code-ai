package com.reviewcode.ai.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class LayeredArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.reviewcode.ai");

    @Test
    void layeredArchitectureShouldBeRespected() {
        Architectures.LayeredArchitecture architecture = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                
                // Define layers
                .layer("Controllers").definedBy("..controller..")
                .layer("Services").definedBy("..service..")
                .layer("Repositories").definedBy("..repository..")
                .layer("Models").definedBy("..model..")
                .layer("Config").definedBy("..config..")
                
                // Define access rules
                .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers", "Config")
                .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services", "Config")
                .whereLayer("Models").mayOnlyBeAccessedByLayers("Controllers", "Services", "Repositories", "Config")
                .whereLayer("Config").mayOnlyBeAccessedByLayers("Controllers", "Services");

        architecture.check(classes);
    }

    @Test
    void servicesShouldNotDependOnControllers() {
        com.tngtech.archunit.lang.ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");

        rule.check(classes);
    }

    @Test
    void repositoriesShouldNotDependOnServices() {
        com.tngtech.archunit.lang.ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..repository..")
                .should().dependOnClassesThat().resideInAPackage("..service..");

        rule.check(classes);
    }

    @Test
    void modelsShouldNotDependOnAnyOtherLayer() {
        com.tngtech.archunit.lang.ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..model..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..controller..", "..service..", "..repository..", "..config..");

        rule.check(classes);
    }
}