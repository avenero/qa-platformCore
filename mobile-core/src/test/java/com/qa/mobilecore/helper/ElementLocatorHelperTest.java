package com.qa.mobilecore.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests unitarios para ElementLocatorHelper.
 *
 * <p>Verifica la resolucion de expresiones de localizacion a objetos {@link By}
 * de Selenium/Appium sin necesitar un driver real. Se prueba la logica de
 * {@code resolveBy()} de forma indirecta a traves del toString() del By resultante.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("ElementLocatorHelper — resolucion de locators por prefijo")
class ElementLocatorHelperTest {

    // =========================================================================

    @Nested
    @DisplayName("Clase de utilidad — no instanciable")
    class InstanciaTest {

        @Test
        @DisplayName("Constructor privado existe y no es accesible publicamente")
        void testConstructorPrivado() throws Exception {
            Constructor<ElementLocatorHelper> c = ElementLocatorHelper.class.getDeclaredConstructor();
            // El constructor es privado — no puede accederse sin reflexion
            assertThat(c.canAccess(null)).isFalse();
            // Se puede forzar el acceso via reflexion (patron utility class standard)
            c.setAccessible(true);
            assertThat(c.canAccess(null)).isTrue();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — prefijo '~' (Accessibility ID)")
    class AccessibilityIdTest {

        @Test
        @DisplayName("'~login_button' debe resolver a accessibilityId('login_button')")
        void testAccessibilityIdConTilde() {
            By by = invokeResolveBy("~login_button");
            assertThat(by.toString()).containsIgnoringCase("login_button");
        }

        @Test
        @DisplayName("'~' con valor vacio lanza IllegalArgumentException (Appium rechaza cadena vacia)")
        void testAccessibilityIdVacio() {
            // "~" pasa la validacion de blanco del helper (la expresion completa no es blank),
            // pero Appium rechaza accessibilityId("") con IllegalArgumentException.
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> invokeResolveBy("~"));
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — prefijo 'id:'")
    class ResourceIdTest {

        @Test
        @DisplayName("'id:com.example:id/btn' debe resolver a By.id")
        void testResourceId() {
            By by = invokeResolveBy("id:com.example:id/btn");
            assertThat(by.toString()).containsIgnoringCase("com.example:id/btn");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — prefijo 'xpath:'")
    class XpathTest {

        @Test
        @DisplayName("'xpath://android.widget.Button' debe resolver a By.xpath")
        void testXpath() {
            By by = invokeResolveBy("xpath://android.widget.Button[@text='Login']");
            assertThat(by.toString()).containsIgnoringCase("android.widget.Button");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — prefijo 'class:'")
    class ClassNameTest {

        @Test
        @DisplayName("'class:android.widget.Button' debe resolver a By.className")
        void testClassName() {
            By by = invokeResolveBy("class:android.widget.Button");
            assertThat(by.toString()).containsIgnoringCase("android.widget.Button");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — prefijo 'text:'")
    class TextTest {

        @Test
        @DisplayName("'text:Iniciar sesion' debe resolver a XPath con @text / @label / @value")
        void testTextXpath() {
            By by = invokeResolveBy("text:Iniciar sesion");
            String str = by.toString();
            assertThat(str).contains("Iniciar sesion");
            // Verifica que usa XPath con los tres atributos para compatibilidad cross-platform
            assertThat(str).containsAnyOf("@text", "@label", "@value");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — prefijo 'pred:' (iOS predicate)")
    class IosPredicateTest {

        @Test
        @DisplayName("'pred:name == \"Login\"' debe resolver a iOSNsPredicateString")
        void testIosPredicate() {
            By by = invokeResolveBy("pred:name == \"Login\"");
            assertThat(by).isNotNull();
            assertThat(by.toString()).containsIgnoringCase("Login");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — prefijo 'chain:' (iOS class chain)")
    class IosClassChainTest {

        @Test
        @DisplayName("'chain:XCUIElementTypeButton[1]' debe resolver a iOSClassChain")
        void testIosClassChain() {
            By by = invokeResolveBy("chain:XCUIElementTypeButton[1]");
            assertThat(by).isNotNull();
            assertThat(by.toString()).containsIgnoringCase("XCUIElementTypeButton");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — prefijo 'uia:' (Android UIAutomator)")
    class UiAutomatorTest {

        @Test
        @DisplayName("'uia:new UiSelector().text(\"Login\")' debe resolver a androidUIAutomator")
        void testAndroidUiAutomator() {
            By by = invokeResolveBy("uia:new UiSelector().text(\"Login\")");
            assertThat(by).isNotNull();
            assertThat(by.toString()).containsIgnoringCase("Login");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — sin prefijo (fallback accessibilityId)")
    class SinPrefijoTest {

        @Test
        @DisplayName("Expresion sin prefijo usa accessibilityId por defecto")
        void testSinPrefijo() {
            By by = invokeResolveBy("loginButton");
            assertThat(by).isNotNull();
            assertThat(by.toString()).containsIgnoringCase("loginButton");
        }

        @Test
        @DisplayName("Expresion sin prefijo con espacios usa accessibilityId")
        void testSinPrefijoConEspacios() {
            By by = invokeResolveBy("Mi elemento con espacios");
            assertThat(by).isNotNull();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveBy() — validacion de entradas invalidas")
    class ValidacionTest {

        @ParameterizedTest(name = "expresion=''{0}'' debe lanzar IllegalArgumentException")
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("Expresiones nulas o en blanco lanzan IllegalArgumentException")
        void testExpresionNulaOVacia(String expresion) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> invokeResolveBy(expresion))
                    .withMessageContaining("nula o vacía");
        }
    }

    // =========================================================================
    // Helper privado — invoca resolveBy() via reflexion para poder testearla
    // sin necesitar un AppiumDriver real
    // =========================================================================

    private By invokeResolveBy(String expression) {
        try {
            var method = ElementLocatorHelper.class.getDeclaredMethod("resolveBy", String.class);
            method.setAccessible(true);
            return (By) method.invoke(null, expression);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException iae) throw iae;
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("Error inesperado en resolveBy", cause);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo invocar resolveBy", e);
        }
    }
}
