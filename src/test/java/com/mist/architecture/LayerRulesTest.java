package com.mist.architecture;

import com.mist.exception.MistException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Locks the layer rules of MIST: {@code ui -> service -> dao -> model}, with persistence APIs kept in the data layer
 * and JavaFX kept in the UI.
 */
class LayerRulesTest {

    private static final JavaClasses MIST = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.mist");

    @Test
    void uiDoesNotTouchDataAccess() {
        noClasses().that().resideInAPackage("com.mist.ui..")
                .should().dependOnClassesThat().resideInAPackage("com.mist.dao..")
                .check(MIST);
    }

    @Test
    void servicesStayIndependentOfUiAndHibernate() {
        noClasses().that().resideInAPackage("com.mist.service..")
                .should().dependOnClassesThat().resideInAnyPackage("com.mist.ui..", "com.mist.dao.hibernate..")
                .check(MIST);
    }

    @Test
    void daoInterfacesStayPure() {
        noClasses().that().resideInAPackage("com.mist.dao")
                .should().dependOnClassesThat().resideInAnyPackage("com.mist.service..", "com.mist.ui..")
                .check(MIST);
    }

    @Test
    void persistenceApisStayInDataLayer() {
        noClasses().that().resideOutsideOfPackages("com.mist.dao.hibernate..", "com.mist.config..", "com.mist.model..")
                .should().dependOnClassesThat().resideInAnyPackage("org.hibernate..", "com.mongodb..")
                .check(MIST);
    }

    @Test
    void javafxStaysInUi() {
        noClasses().that().resideOutsideOfPackages("com.mist", "com.mist.ui..")
                .should().dependOnClassesThat().resideInAPackage("javafx..")
                .check(MIST);
    }

    @Test
    void exceptionsExtendMistException() {
        classes().that().resideInAPackage("com.mist.exception").and().doNotHaveSimpleName("package-info")
                .should().beAssignableTo(MistException.class)
                .check(MIST);
    }
}
