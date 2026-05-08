package com.qa.mobilecore.steps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MobileLocatorResolver — resolución semántica 3 niveles")
class MobileLocatorResolverTest {

    // =========================================================================
    // Nivel 1 — byA11y()
    // =========================================================================

    @Nested
    @DisplayName("Nivel 1 — byA11y()")
    class ByA11yTests {

        @Test
        @DisplayName("genera prefijo ~ para accessibility id")
        void byA11y_generatesTildePrefix() {
            assertThat(MobileLocatorResolver.byA11y("Submit")).isEqualTo("~Submit");
        }

        @Test
        @DisplayName("preserva espacios y caracteres especiales")
        void byA11y_preservesSpecialChars() {
            assertThat(MobileLocatorResolver.byA11y("Save & Continue"))
                    .isEqualTo("~Save & Continue");
        }

        @Test
        @DisplayName("prefijo vacío produce ~ (ElementLocatorHelper lo maneja)")
        void byA11y_emptyLabel_producesTildeOnly() {
            assertThat(MobileLocatorResolver.byA11y("")).isEqualTo("~");
        }
    }

    // =========================================================================
    // Nivel 1 — byText()
    // =========================================================================

    @Nested
    @DisplayName("Nivel 1 — byText()")
    class ByTextTests {

        @Test
        @DisplayName("genera prefijo text: para texto visible")
        void byText_generatesTextPrefix() {
            assertThat(MobileLocatorResolver.byText("Welcome")).isEqualTo("text:Welcome");
        }

        @Test
        @DisplayName("preserva espacios en el texto")
        void byText_preservesSpaces() {
            assertThat(MobileLocatorResolver.byText("Hello World"))
                    .isEqualTo("text:Hello World");
        }
    }

    // =========================================================================
    // Nivel 2 — byResourceId()
    // =========================================================================

    @Nested
    @DisplayName("Nivel 2 — byResourceId()")
    class ByResourceIdTests {

        @Test
        @DisplayName("genera prefijo id: para resource id")
        void byResourceId_generatesIdPrefix() {
            assertThat(MobileLocatorResolver.byResourceId("com.app:id/btn_login"))
                    .isEqualTo("id:com.app:id/btn_login");
        }

        @Test
        @DisplayName("soporta resource ids con puntos y barras")
        void byResourceId_supportsComplexIds() {
            assertThat(MobileLocatorResolver.byResourceId("com.myapp.ui:id/submit_button"))
                    .isEqualTo("id:com.myapp.ui:id/submit_button");
        }
    }

    // =========================================================================
    // Nivel 2 — byPredicate()
    // =========================================================================

    @Nested
    @DisplayName("Nivel 2 — byPredicate()")
    class ByPredicateTests {

        @Test
        @DisplayName("genera prefijo pred: para iOS NSPredicate")
        void byPredicate_generatesPredPrefix() {
            assertThat(MobileLocatorResolver.byPredicate("label == 'Submit'"))
                    .isEqualTo("pred:label == 'Submit'");
        }

        @Test
        @DisplayName("preserva expresiones de predicado complejas")
        void byPredicate_preservesComplexExpression() {
            assertThat(MobileLocatorResolver.byPredicate("label == 'Login' AND enabled == true"))
                    .isEqualTo("pred:label == 'Login' AND enabled == true");
        }
    }

    // =========================================================================
    // Nivel 3 — byXPath()
    // =========================================================================

    @Nested
    @DisplayName("Nivel 3 — byXPath()")
    class ByXPathTests {

        @Test
        @DisplayName("genera prefijo xpath: (+ FRAGILE log)")
        void byXPath_generatesXpathPrefix() {
            assertThat(MobileLocatorResolver.byXPath("//android.widget.Button[@text='OK']"))
                    .isEqualTo("xpath://android.widget.Button[@text='OK']");
        }

        @Test
        @DisplayName("soporta XPath con atributos iOS")
        void byXPath_iosXPath() {
            assertThat(MobileLocatorResolver.byXPath("//XCUIElementTypeButton[@name='Submit']"))
                    .isEqualTo("xpath://XCUIElementTypeButton[@name='Submit']");
        }
    }

    // =========================================================================
    // Nivel 3 — byUiAutomator()
    // =========================================================================

    @Nested
    @DisplayName("Nivel 3 — byUiAutomator()")
    class ByUiAutomatorTests {

        @Test
        @DisplayName("genera prefijo uia: (+ FRAGILE log)")
        void byUiAutomator_generatesUiaPrefix() {
            assertThat(MobileLocatorResolver.byUiAutomator("new UiSelector().text(\"Login\")"))
                    .isEqualTo("uia:new UiSelector().text(\"Login\")");
        }
    }

    // =========================================================================
    // Nivel 3 — byClassChain()
    // =========================================================================

    @Nested
    @DisplayName("Nivel 3 — byClassChain()")
    class ByClassChainTests {

        @Test
        @DisplayName("genera prefijo chain: (+ FRAGILE log)")
        void byClassChain_generatesChainPrefix() {
            assertThat(MobileLocatorResolver.byClassChain("**/XCUIElementTypeButton[`label == 'Submit'`]"))
                    .isEqualTo("chain:**/XCUIElementTypeButton[`label == 'Submit'`]");
        }
    }

    // =========================================================================
    // Nivel 1+ — nthOf()
    // =========================================================================

    @Nested
    @DisplayName("Nivel 1+ — nthOf()")
    class NthOfTests {

        @Test
        @DisplayName("n=1 produce XPath [1] (XPath es 1-indexed)")
        void nthOf_firstElement_isXPathIndexOne() {
            assertThat(MobileLocatorResolver.nthOf(MobileLocatorResolver.byA11y("Delete"), 1))
                    .isEqualTo("xpath:(//*[@content-desc='Delete' or @name='Delete'])[1]");
        }

        @Test
        @DisplayName("n=3 produce XPath [3]")
        void nthOf_thirdElement_isXPathIndexThree() {
            assertThat(MobileLocatorResolver.nthOf(MobileLocatorResolver.byA11y("Delete"), 3))
                    .isEqualTo("xpath:(//*[@content-desc='Delete' or @name='Delete'])[3]");
        }

        @Test
        @DisplayName("funciona con byText() como expresión base")
        void nthOf_withByText() {
            assertThat(MobileLocatorResolver.nthOf(MobileLocatorResolver.byText("Item"), 2))
                    .isEqualTo("xpath:(//*[@text='Item' or @label='Item' or @value='Item'])[2]");
        }

        @Test
        @DisplayName("funciona con byResourceId() como expresión base")
        void nthOf_withByResourceId() {
            assertThat(MobileLocatorResolver.nthOf(MobileLocatorResolver.byResourceId("com.app:id/row"), 1))
                    .isEqualTo("xpath:(//*[@resource-id='com.app:id/row'])[1]");
        }

        @Test
        @DisplayName("n=0 lanza IllegalArgumentException")
        void nthOf_zeroIndex_throwsIllegalArgument() {
            assertThatThrownBy(() -> MobileLocatorResolver.nthOf("~X", 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("n negativo lanza IllegalArgumentException")
        void nthOf_negativeIndex_throwsIllegalArgument() {
            assertThatThrownBy(() -> MobileLocatorResolver.nthOf("~X", -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // Nivel 1+ — inContainer()
    // =========================================================================

    @Nested
    @DisplayName("Nivel 1+ — inContainer()")
    class InContainerTests {

        @Test
        @DisplayName("compone dos byA11y con descendiente XPath")
        void inContainer_twoA11y() {
            String result = MobileLocatorResolver.inContainer(
                MobileLocatorResolver.byA11y("Delete"),
                MobileLocatorResolver.byA11y("List item"));
            assertThat(result).isEqualTo(
                "xpath://*[@content-desc='List item' or @name='List item']" +
                "//*[@content-desc='Delete' or @name='Delete']");
        }

        @Test
        @DisplayName("compone byText como inner con byA11y como container")
        void inContainer_textInsideA11y() {
            String result = MobileLocatorResolver.inContainer(
                MobileLocatorResolver.byText("Submit"),
                MobileLocatorResolver.byA11y("modal"));
            assertThat(result).isEqualTo(
                "xpath://*[@content-desc='modal' or @name='modal']" +
                "//*[@text='Submit' or @label='Submit' or @value='Submit']");
        }

        @Test
        @DisplayName("compone byResourceId como container")
        void inContainer_resourceIdContainer() {
            String result = MobileLocatorResolver.inContainer(
                MobileLocatorResolver.byA11y("OK"),
                MobileLocatorResolver.byResourceId("com.app:id/dialog"));
            assertThat(result).isEqualTo(
                "xpath://*[@resource-id='com.app:id/dialog']" +
                "//*[@content-desc='OK' or @name='OK']");
        }
    }

    // =========================================================================
    // toXPath() — conversión interna
    // =========================================================================

    @Nested
    @DisplayName("toXPath() — conversión interna de prefijos a XPath puro")
    class ToXPathTests {

        @Test
        @DisplayName("~ produce XPath por content-desc y name")
        void toXPath_tildePrefix() {
            assertThat(MobileLocatorResolver.toXPath("~Login"))
                    .isEqualTo("//*[@content-desc='Login' or @name='Login']");
        }

        @Test
        @DisplayName("text: produce XPath por text, label y value")
        void toXPath_textPrefix() {
            assertThat(MobileLocatorResolver.toXPath("text:Submit"))
                    .isEqualTo("//*[@text='Submit' or @label='Submit' or @value='Submit']");
        }

        @Test
        @DisplayName("id: produce XPath por resource-id")
        void toXPath_idPrefix() {
            assertThat(MobileLocatorResolver.toXPath("id:com.app:id/btn"))
                    .isEqualTo("//*[@resource-id='com.app:id/btn']");
        }

        @Test
        @DisplayName("xpath: extrae el XPath puro sin prefijo")
        void toXPath_xpathPrefix() {
            assertThat(MobileLocatorResolver.toXPath("xpath://button[@id='x']"))
                    .isEqualTo("//button[@id='x']");
        }

        @Test
        @DisplayName("sin prefijo usa content-desc/name por default")
        void toXPath_noPrefix_usesA11yDefault() {
            assertThat(MobileLocatorResolver.toXPath("Submit"))
                    .isEqualTo("//*[@content-desc='Submit' or @name='Submit']");
        }

        @Test
        @DisplayName("null lanza IllegalArgumentException")
        void toXPath_null_throwsIllegalArgument() {
            assertThatThrownBy(() -> MobileLocatorResolver.toXPath(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blank lanza IllegalArgumentException")
        void toXPath_blank_throwsIllegalArgument() {
            assertThatThrownBy(() -> MobileLocatorResolver.toXPath("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // Casos compuestos — combinaciones realistas
    // =========================================================================

    @Nested
    @DisplayName("Casos compuestos")
    class CompoundCases {

        @Test
        @DisplayName("segundo botón Delete en lista: nthOf(byA11y)")
        void compound_secondDeleteButton() {
            String selector = MobileLocatorResolver.nthOf(
                MobileLocatorResolver.byA11y("Delete"), 2);
            assertThat(selector)
                    .isEqualTo("xpath:(//*[@content-desc='Delete' or @name='Delete'])[2]");
        }

        @Test
        @DisplayName("texto dentro de contenedor a11y: inContainer(byText, byA11y)")
        void compound_textInA11yContainer() {
            String selector = MobileLocatorResolver.inContainer(
                MobileLocatorResolver.byText("Confirm"),
                MobileLocatorResolver.byA11y("alert-dialog"));
            assertThat(selector).startsWith("xpath:");
            assertThat(selector).contains("content-desc='alert-dialog'");
            assertThat(selector).contains("@text='Confirm'");
        }

        @Test
        @DisplayName("tercer resource-id en pantalla: nthOf(byResourceId)")
        void compound_thirdResourceId() {
            String selector = MobileLocatorResolver.nthOf(
                MobileLocatorResolver.byResourceId("com.app:id/list_item"), 3);
            assertThat(selector)
                    .isEqualTo("xpath:(//*[@resource-id='com.app:id/list_item'])[3]");
        }
    }

    // =========================================================================
    // FRAGILE MARKER — constante pública
    // =========================================================================

    @Test
    @DisplayName("FRAGILE_MARKER es la cadena esperada")
    void fragileMarker_hasExpectedValue() {
        assertThat(MobileLocatorResolver.FRAGILE_MARKER).isEqualTo("[FRAGILE LOCATOR]");
    }

    // =========================================================================
    // Parametrized — múltiples a11y labels
    // =========================================================================

    @ParameterizedTest(name = "a11y={0}")
    @ValueSource(strings = {"Login", "Sign Up", "Forgot Password?", "Continue"})
    @DisplayName("byA11y soporta labels comunes de pantallas de login")
    void byA11y_commonLoginLabels(String label) {
        assertThat(MobileLocatorResolver.byA11y(label)).isEqualTo("~" + label);
    }
}
