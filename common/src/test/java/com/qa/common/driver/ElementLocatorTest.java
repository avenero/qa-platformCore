package com.qa.common.driver;


import com.qa.common.api.driver.ElementLocator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para {@link ElementLocator}.
 *
 * @author Abel Venero
 * @since 3.0.0
 */
@DisplayName("ElementLocator")
class ElementLocatorTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryTests {

        @Test
        @DisplayName("css() crea locator con Strategy.CSS")
        void cssCreaCss() {
            ElementLocator loc = ElementLocator.css("#submit");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.CSS);
            assertThat(loc.value()).isEqualTo("#submit");
        }

        @Test
        @DisplayName("xpath() crea locator con Strategy.XPATH")
        void xpathCreaXpath() {
            ElementLocator loc = ElementLocator.xpath("//button[@data-testid='login']");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.XPATH);
        }

        @Test
        @DisplayName("id() crea locator con Strategy.ID")
        void idCreaId() {
            ElementLocator loc = ElementLocator.id("com.app:id/btn_ok");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.ID);
        }

        @Test
        @DisplayName("accessibilityId() crea locator con Strategy.ACCESSIBILITY_ID")
        void accessibilityIdCrea() {
            ElementLocator loc = ElementLocator.accessibilityId("login_button");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.ACCESSIBILITY_ID);
            assertThat(loc.value()).isEqualTo("login_button");
        }

        @Test
        @DisplayName("className() crea locator con Strategy.CLASS_NAME")
        void classNameCrea() {
            ElementLocator loc = ElementLocator.className("android.widget.Button");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.CLASS_NAME);
        }

        @Test
        @DisplayName("text() crea locator con Strategy.TEXT")
        void textCreaText() {
            ElementLocator loc = ElementLocator.text("Iniciar sesion");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.TEXT);
            assertThat(loc.value()).isEqualTo("Iniciar sesion");
        }

        @Test
        @DisplayName("resourceId() crea locator con Strategy.RESOURCE_ID")
        void resourceIdCrea() {
            ElementLocator loc = ElementLocator.resourceId("com.app:id/btn_login");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.RESOURCE_ID);
        }

        @Test
        @DisplayName("a11y() es alias de accessibilityId con Strategy.ACCESSIBILITY_ID")
        void a11yEsAliasDeAccessibilityId() {
            ElementLocator loc = ElementLocator.a11y("login_button");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.ACCESSIBILITY_ID);
            assertThat(loc.value()).isEqualTo("login_button");
        }

        @Test
        @DisplayName("name() crea locator con Strategy.NAME")
        void nameCrea() {
            ElementLocator loc = ElementLocator.name("username");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.NAME);
        }

        @Test
        @DisplayName("role() crea locator con Strategy.ROLE")
        void roleCrea() {
            ElementLocator loc = ElementLocator.role("button[name='Submit']");
            assertThat(loc.strategy()).isEqualTo(ElementLocator.Strategy.ROLE);
        }
    }

    @Nested
    @DisplayName("Validaciones del constructor")
    class ValidationTests {

        @Test
        @DisplayName("strategy null lanza NullPointerException")
        void strategyNullLanzaException() {
            assertThatNullPointerException()
                .isThrownBy(() -> new ElementLocator(null, "#btn"));
        }

        @Test
        @DisplayName("value null lanza IllegalArgumentException")
        void valueNullLanzaException() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new ElementLocator(ElementLocator.Strategy.CSS, null));
        }

        @Test
        @DisplayName("value vacio lanza IllegalArgumentException")
        void valueVacioLanzaException() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new ElementLocator(ElementLocator.Strategy.CSS, "   "));
        }
    }

    @Nested
    @DisplayName("Igualdad y toString")
    class EqualityTests {

        @Test
        @DisplayName("Dos locators con mismo strategy+value son iguales")
        void igualesConMismoStrategyYValue() {
            ElementLocator a = ElementLocator.css("#btn");
            ElementLocator b = ElementLocator.css("#btn");
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("Locators con strategy distinta no son iguales")
        void noIgualesConStrategyDistinta() {
            ElementLocator css  = ElementLocator.css("#btn");
            ElementLocator xpath = ElementLocator.xpath("#btn");
            assertThat(css).isNotEqualTo(xpath);
        }

        @Test
        @DisplayName("toString incluye estrategia y valor")
        void toStringIncluyeEstrategiaYValor() {
            ElementLocator loc = ElementLocator.css("#login");
            assertThat(loc.toString()).contains("CSS").contains("#login");
        }
    }

    @Nested
    @DisplayName("Strategy enum")
    class StrategyEnumTests {

        @Test
        @DisplayName("Strategy tiene todas las variantes esperadas")
        void strategyContieneTodasLasVariantes() {
            assertThat(ElementLocator.Strategy.values()).containsExactlyInAnyOrder(
                ElementLocator.Strategy.CSS,
                ElementLocator.Strategy.XPATH,
                ElementLocator.Strategy.ID,
                ElementLocator.Strategy.RESOURCE_ID,
                ElementLocator.Strategy.ACCESSIBILITY_ID,
                ElementLocator.Strategy.NAME,
                ElementLocator.Strategy.CLASS_NAME,
                ElementLocator.Strategy.TEXT,
                ElementLocator.Strategy.ROLE
            );
        }
    }
}
