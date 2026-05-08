package com.qa.webcore.steps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocatorResolver — resolución semántica 3 niveles")
class LocatorResolverTest {

    // =========================================================================
    // Nivel 1 — Semántico puro
    // =========================================================================

    @Nested
    @DisplayName("Nivel 1 — byText()")
    class ByTextTests {

        @Test
        @DisplayName("genera selector text= para texto visible")
        void byText_generatesTextSelector() {
            assertThat(LocatorResolver.byText("Submit")).isEqualTo("text=Submit");
        }

        @Test
        @DisplayName("preserva espacios y caracteres especiales")
        void byText_preservesSpecialChars() {
            assertThat(LocatorResolver.byText("Save & Continue"))
                    .isEqualTo("text=Save & Continue");
        }

        @Test
        @DisplayName("selector vacío produce text= (el motor de Playwright lo maneja)")
        void byText_emptyText_producesSelector() {
            assertThat(LocatorResolver.byText("")).isEqualTo("text=");
        }
    }

    @Nested
    @DisplayName("Nivel 1 — byRole()")
    class ByRoleTests {

        @Test
        @DisplayName("genera role=button[name=\"...\"] con nombre")
        void byRole_withName_generatesRoleSelector() {
            assertThat(LocatorResolver.byRole("button", "Submit"))
                    .isEqualTo("role=button[name=\"Submit\"]");
        }

        @Test
        @DisplayName("normaliza el rol a minúsculas")
        void byRole_uppercaseRole_normalizesToLowercase() {
            assertThat(LocatorResolver.byRole("BUTTON", "Save"))
                    .isEqualTo("role=button[name=\"Save\"]");
        }

        @Test
        @DisplayName("genera role sin nombre para aserciones de existencia")
        void byRole_withoutName_generatesRoleOnlySelector() {
            assertThat(LocatorResolver.byRole("heading"))
                    .isEqualTo("role=heading");
        }

        @ParameterizedTest(name = "role={0}")
        @ValueSource(strings = {"button", "link", "heading", "textbox", "checkbox", "combobox"})
        @DisplayName("soporta todos los roles ARIA estándar")
        void byRole_standardAriaRoles(String role) {
            assertThat(LocatorResolver.byRole(role, "Label"))
                    .startsWith("role=" + role);
        }
    }

    @Nested
    @DisplayName("Nivel 1 — byLabel()")
    class ByLabelTests {

        @Test
        @DisplayName("genera selector aria-label para campos de formulario")
        void byLabel_generatesAriaLabelSelector() {
            assertThat(LocatorResolver.byLabel("Email address"))
                    .isEqualTo("[aria-label=\"Email address\"]");
        }

        @Test
        @DisplayName("preserva espacios en el label")
        void byLabel_preservesSpaces() {
            assertThat(LocatorResolver.byLabel("First name"))
                    .isEqualTo("[aria-label=\"First name\"]");
        }
    }

    @Nested
    @DisplayName("Nivel 1 — byPlaceholder()")
    class ByPlaceholderTests {

        @Test
        @DisplayName("genera selector placeholder como CSS attribute")
        void byPlaceholder_generatesPlaceholderSelector() {
            assertThat(LocatorResolver.byPlaceholder("Enter email"))
                    .isEqualTo("[placeholder=\"Enter email\"]");
        }
    }

    // =========================================================================
    // Nivel 1+ — Scope e índice
    // =========================================================================

    @Nested
    @DisplayName("Nivel 1+ — inContainer()")
    class InContainerTests {

        @Test
        @DisplayName("encadena con >> para scoping en Playwright")
        void inContainer_chainsWithArrow() {
            assertThat(LocatorResolver.inContainer("text=Submit", ".modal"))
                    .isEqualTo(".modal >> text=Submit");
        }

        @Test
        @DisplayName("funciona con selectores semánticos como contenedor")
        void inContainer_semanticContainer() {
            String inner = LocatorResolver.byText("Delete");
            String container = LocatorResolver.byRole("dialog");
            assertThat(LocatorResolver.inContainer(inner, container))
                    .isEqualTo("role=dialog >> text=Delete");
        }
    }

    @Nested
    @DisplayName("Nivel 1+ — nthOf()")
    class NthOfTests {

        @Test
        @DisplayName("n=1 produce nth=0 (Playwright es 0-indexed)")
        void nthOf_firstElement_isNthZero() {
            assertThat(LocatorResolver.nthOf("text=Delete", 1))
                    .isEqualTo("text=Delete >> nth=0");
        }

        @Test
        @DisplayName("n=3 produce nth=2")
        void nthOf_thirdElement_isNthTwo() {
            assertThat(LocatorResolver.nthOf("text=Delete", 3))
                    .isEqualTo("text=Delete >> nth=2");
        }

        @Test
        @DisplayName("n=0 lanza IllegalArgumentException")
        void nthOf_zeroIndex_throwsIllegalArgument() {
            assertThatThrownBy(() -> LocatorResolver.nthOf("text=X", 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("n negativo lanza IllegalArgumentException")
        void nthOf_negativeIndex_throwsIllegalArgument() {
            assertThatThrownBy(() -> LocatorResolver.nthOf("text=X", -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // Nivel 2 — data-testid
    // =========================================================================

    @Nested
    @DisplayName("Nivel 2 — byTestId()")
    class ByTestIdTests {

        @Test
        @DisplayName("genera selector data-testid como CSS attribute")
        void byTestId_generatesDataTestIdSelector() {
            assertThat(LocatorResolver.byTestId("submit-button"))
                    .isEqualTo("[data-testid=\"submit-button\"]");
        }

        @Test
        @DisplayName("soporta testIds con guiones y underscores")
        void byTestId_withSpecialCharsInId() {
            assertThat(LocatorResolver.byTestId("user-profile_avatar"))
                    .isEqualTo("[data-testid=\"user-profile_avatar\"]");
        }
    }

    @Nested
    @DisplayName("Nivel 2 — byTestIdInContainer()")
    class ByTestIdInContainerTests {

        @Test
        @DisplayName("encadena testId dentro de contenedor")
        void byTestIdInContainer_chainsCorrectly() {
            assertThat(LocatorResolver.byTestIdInContainer("save-btn", ".form"))
                    .isEqualTo(".form >> [data-testid=\"save-btn\"]");
        }
    }

    // =========================================================================
    // Nivel 3 — CSS/XPath escape hatch
    // =========================================================================

    @Nested
    @DisplayName("Nivel 3 — byCss()")
    class ByCssTests {

        @Test
        @DisplayName("retorna el selector CSS sin modificar")
        void byCss_returnsUnchanged() {
            assertThat(LocatorResolver.byCss(".my-class > button"))
                    .isEqualTo(".my-class > button");
        }

        @Test
        @DisplayName("soporta selectores de ID")
        void byCss_idSelector() {
            assertThat(LocatorResolver.byCss("#submit-btn"))
                    .isEqualTo("#submit-btn");
        }

        @Test
        @DisplayName("soporta selectores de atributo")
        void byCss_attributeSelector() {
            assertThat(LocatorResolver.byCss("[type='submit']"))
                    .isEqualTo("[type='submit']");
        }
    }

    @Nested
    @DisplayName("Nivel 3 — byXPath()")
    class ByXPathTests {

        @Test
        @DisplayName("añade prefijo xpath= si no está presente")
        void byXPath_addsPrefixIfAbsent() {
            assertThat(LocatorResolver.byXPath("//button[@id='submit']"))
                    .isEqualTo("xpath=//button[@id='submit']");
        }

        @Test
        @DisplayName("no duplica xpath= si ya está presente")
        void byXPath_doesNotDuplicatePrefix() {
            assertThat(LocatorResolver.byXPath("xpath=//button"))
                    .isEqualTo("xpath=//button");
        }

        @Test
        @DisplayName("soporta XPath relativo")
        void byXPath_relativeXPath() {
            assertThat(LocatorResolver.byXPath("(//table//tr)[3]/td[2]"))
                    .isEqualTo("xpath=(//table//tr)[3]/td[2]");
        }
    }

    // =========================================================================
    // Casos compuestos — combinaciones realistas
    // =========================================================================

    @Nested
    @DisplayName("Casos compuestos")
    class CompoundCases {

        @Test
        @DisplayName("botón semántico dentro de modal: inContainer(byRole, byRole)")
        void compound_buttonInModal() {
            String selector = LocatorResolver.inContainer(
                LocatorResolver.byRole("button", "Confirm"),
                LocatorResolver.byRole("dialog"));

            assertThat(selector).isEqualTo("role=dialog >> role=button[name=\"Confirm\"]");
        }

        @Test
        @DisplayName("segundo enlace Delete en tabla: nthOf(byRole)")
        void compound_secondDeleteLink() {
            String selector = LocatorResolver.nthOf(
                LocatorResolver.byRole("button", "Delete"), 2);

            assertThat(selector).isEqualTo("role=button[name=\"Delete\"] >> nth=1");
        }

        @Test
        @DisplayName("testId dentro de contenedor semántico")
        void compound_testIdInSemanticContainer() {
            String selector = LocatorResolver.byTestIdInContainer(
                "close-icon", LocatorResolver.byRole("dialog"));

            assertThat(selector).isEqualTo("role=dialog >> [data-testid=\"close-icon\"]");
        }
    }
}
