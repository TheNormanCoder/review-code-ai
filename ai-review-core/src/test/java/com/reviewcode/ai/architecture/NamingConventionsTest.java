package com.reviewcode.ai.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class NamingConventionsTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.reviewcode.ai");

    @Test
    void servicesShouldHaveServiceSuffix() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Service.class)
                .should().haveSimpleNameEndingWith("Service");

        rule.check(classes);
    }

    @Test
    void repositoriesShouldHaveRepositorySuffix() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Repository.class)
                .should().haveSimpleNameEndingWith("Repository");

        rule.check(classes);
    }

    @Test
    void controllersShouldHaveControllerSuffix() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller");

        rule.check(classes);
    }

    @Test
    void entitiesShouldNotHaveEntitySuffix() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().haveSimpleNameNotEndingWith("Entity");

        rule.check(classes);
    }

    @Test
    void dtosShouldHaveDtoSuffix() {
        ArchRule rule = classes()
                .that().resideInAPackage("..dto..")
                .should().haveSimpleNameEndingWith("Dto")
                .orShould().haveSimpleNameEndingWith("Request")
                .orShould().haveSimpleNameEndingWith("Response");

        rule.check(classes);
    }

    @Test
    void configurationClassesShouldHaveConfigurationSuffix() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("org.springframework.context.annotation.Configuration")
                .should().haveSimpleNameEndingWith("Configuration")
                .orShould().haveSimpleNameEndingWith("Config");

        rule.check(classes);
    }

    @Test
    void testClassesShouldHaveTestSuffix() {
        ArchRule rule = classes()
                .that().resideInAPackage("..test..")
                .should().haveSimpleNameEndingWith("Test")
                .orShould().haveSimpleNameEndingWith("Tests")
                .orShould().haveSimpleNameEndingWith("IT");

        rule.check(classes);
    }

    @Test
    void interfacesShouldNotHaveIPrefix() {
        ArchRule rule = classes()
                .that().areInterfaces()
                .should().haveSimpleNameNotStartingWith("I");

        rule.check(classes);
    }
}