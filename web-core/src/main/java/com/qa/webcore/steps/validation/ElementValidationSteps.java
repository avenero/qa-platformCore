package com.qa.webcore.steps.validation;

import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.annotation.StepDef;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * Steps de validacion de elementos: visibilidad, texto, atributos, estado, formato.
 * Migrado de WebSteps.java (secciones THEN).
 * @author Abel Venero
 * @since 2.0.0
 */
public class ElementValidationSteps {

    private static final String STEP_VERIFY_TEXT_DOUBLE_SHADOW =
        "verifico que el texto en el elemento {string} sea {string}"
        + " dentro del doble contenedor shadow {string} {string}";

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @StepDef(value = "web.element-validation.legacy-verifico-existe",
             deprecated = true, replacedBy = "web.element-validation#elElementoDebeSerVisible")
    @Then("verifico si existe el elemento {string}")
    public void verificoSiExisteElElemento(String locator) {
        Assertions.assertThat(engine().isPresent(locator)).
            as("Elemento " + locator + " no encontrado").isTrue();
        helper.captureScreen(scenario);
    }

    @Then("verifico que el texto en {string} sea {string}")
    public void verificoQueElTextoEnSea(String locator, String expectedText) {
        Assertions.assertThat(engine().find(locator).getText()).
            as("Texto no coincide").isEqualTo(helper.resolveVariables(expectedText));
        helper.captureScreen(scenario);
    }

    @Then("verifico que no exista el elemento {string}")
    public void verificoQueNoExistaElElemento(String locator) {
        Assertions.assertThat(engine().isPresent(locator)).as("Elemento " + locator + " encontrado!").isFalse();
        helper.captureScreen(scenario);
    }

    @StepDef(value = "web.element-validation.legacy-desactivado",
             deprecated = true, replacedBy = "web.element-validation#verificoQueElElementoEsteDeshabilitado")
    @Then("verifico que el elemento {string} este desactivado")
    public void verificoQueElElementoEsteDesactivado(String locator) {
        Assertions.assertThat(helper.isActive(locator)).as("El estado de " + locator + " es Activo!").isFalse();
        helper.captureScreen(scenario);
    }

    @Then("verifico que el elemento {string} este deshabilitado")
    public void verificoQueElElementoEsteDeshabilitado(String locator) {
        Assertions.assertThat(engine().find(locator).isEnabled()).
            as("El elemento " + locator + " esta habilitado!").isFalse();
        helper.captureScreen(scenario);
    }

    @Then("verifico que el elemento {string} este habilitado")
    public void verificoQueElElementoEstehabilitado(String locator) {
        Assertions.assertThat(engine().find(locator).isEnabled()).
            as("El elemento " + locator + " esta deshabilitado!").isTrue();
        helper.captureScreen(scenario);
    }

    @StepDef(value = "web.element-validation.legacy-activo",
             deprecated = true, replacedBy = "web.element-validation#verificoQueElElementoEstehabilitado")
    @Then("verifico que el elemento {string} este activo")
    public void verificoQueElElementoEsteActivo(String locator) {
        Assertions.assertThat(helper.isActive(locator)).as("El estado de " + locator + " es Desactivado!").isTrue();
        helper.captureScreen(scenario);
    }

    @Then("verifico si existe el elemento {string} y valido que el texto sea {string}")
    public void verificoSiExisteElElementoYValidoQueElTextoSea(String locator, String expectedText) {
        if (engine().isPresent(locator)) {
            Assertions.assertThat(engine().find(locator).getText()).
                as("Texto no coincide").isEqualTo(helper.resolveVariables(expectedText));
        } else {
            Assertions.fail("Elemento " + locator + " no aparecio o no es visible");
        }
    }

    @Then("verifico si existe el elemento {string} y valido que el texto contenga {string}")
    public void verificoSiExisteElElementoYValidoQueElTextoContenga(String locator, String expectedSubtext) {
        if (engine().isPresent(locator)) {
            Assertions.assertThat(engine().find(locator).getText()).
                contains(helper.resolveVariables(expectedSubtext));
        } else {
            Assertions.fail("Elemento " + locator + " no aparecio");
        }
    }

    @Then("verifico que el texto en {string} contenga el texto de la variable temporal {string}")
    public void verificoTextoEnContengaElTextoVariableTemporal(String locator, String variableName) {
        Assertions.assertThat(engine().find(locator).getText()).
            as("No contiene variable: " + variableName).contains(helper.getTextVariableTemp(variableName));
    }

    @Then("verifico que el texto en {string} sea igual al de la variable temporal {string}")
    public void verificoQueElTextoEnSeaIgualAlDeLaVariableTemporal(String locator, String variableName) {
        Assertions.assertThat(engine().find(locator).getText()).
            as("Error, valores no iguales").isEqualTo(helper.getTextVariableTemp(variableName));
    }

    @Then("verifico que el texto en {string} contenga el texto {string}")
    public void verificoTextoEnContengaElTexto(String locator, String expectedSubtext) {
        Assertions.assertThat(engine().find(locator).getText()).
            as("Elemento no contiene: " + expectedSubtext).
            contains(helper.resolveVariables(expectedSubtext));
    }

    @Then(STEP_VERIFY_TEXT_DOUBLE_SHADOW)
    public void verificoTextoDentroDeDobleShadow(String el, String text, String h1, String h2) {
        helper.verifyTextInNestedShadow(el, text, h1, h2);
    }

    @Then("verifico que el texto en el elemento {string} sea {string} dentro del shadow {string}")
    public void verificoTextoDentroDeShadow(String el, String text, String host) {
        helper.verifyTextInShadow(el, text, host);
    }

    @Then("verifico que el valor del elemento {string} no sea cero")
    public void verificoQueElValorDelElementoNoSeaCero(String locator) {
        Assertions.assertThat(helper.valueElementNotZero(locator)).as("El valor de " + locator + " es cero!").isTrue();
    }

    @Then("valido que el texto de elemento {string} no contenga el texto {string}")
    public void validoQueElValorDelElementoNoSeaVacio(String locator, String texto) {
        Assertions.assertThat(helper.valueElementNotContains(locator, texto)).
            as("Contiene el texto: " + texto).isTrue();
    }

    @Then("valido que el valor de elemento {string} no sea vacio")
    public void validoQueElValorDelElementoNoSeaVacio2(String locator) {
        Assertions.assertThat(helper.valueElementNotEmpty(locator)).as("El valor es vacio!").isTrue();
    }

    @Then("valido que el valor almacenado en el campo {string} sea {string}")
    public void validoQueElValorAlmacenadoEnElCampoSea(String campo, String expectedValue) {
        helper.isFieldEquals(campo, expectedValue);
    }

    @And("verifico si existe el elemento {string} con host {string}")
    public void verificoSiExisteElementoConShadow(String elemento, String host) {
        helper.isElementShadowPresent(elemento, host);
        helper.captureScreen(scenario);
    }

    @And("busco dentro de el shadow con host {string} el elemento {string}")
    public void buscoElementoEnShadow(String host, String elemento) {
        helper.findElementInShadow(host, elemento);
        helper.captureScreen(scenario);
    }

    // --- Validaciones de formato y tipo ---

    @Then("el campo {string} debe aceptar solo numeros")
    public void elCampoDebeAceptarSoloNumeros(String locator) {
        helper.validateFieldAcceptsOnlyNumbers(locator);
    }

    @Then("el campo {string} debe aceptar solo letras")
    public void elCampoDebeAceptarSoloLetras(String locator) {
        helper.validateFieldAcceptsOnlyLetters(locator);
    }

    @Then("el campo {string} no debe aceptar numeros ni caracteres especiales")
    public void elCampoNoDebeAceptarNumerosNiCaracteresEspeciales(String locator) {
        helper.validateFieldNoNumbersNoSpecialChars(locator);
    }

    @Then("el campo {string} debe tener formato de email valido")
    public void elCampoDebeTenerFormatoDeEmailValido(String locator) {
        helper.validateEmailFormat(locator);
    }

    @Then("el campo {string} debe tener formato de telefono con prefijo {string} y {int} digitos totales")
    public void elCampoDebeTenerFormatoDeTelefonoConPrefijoYDigitos(String locator, String prefix, int totalDigits) {
        helper.validatePhoneFormat(locator, prefix, totalDigits);
    }

    @Then("el campo {string} no debe contener espacios en blanco")
    public void elCampoNoDebeContenerEspaciosEnBlanco(String locator) {
        helper.validateFieldNoSpaces(locator);
    }

    @Then("el campo {string} debe agregar separadores de miles automaticamente")
    public void elCampoDebeAgregarSeparadoresDeMilesAutomaticamente(String locator) {
        helper.validateThousandsSeparators(locator);
    }

    @Then("el campo {string} debe tener el formato con patron {string}")
    public void elCampoDebeTenerElFormatoConPatron(String locator, String regexPattern) {
        helper.validateFieldMatchesPattern(locator, regexPattern);
    }

    @Then("el valor formateado debe ser {string}")
    public void elValorFormateadoDebeSer(String expectedValue) {
        helper.validateFormattedValue(expectedValue);
    }

    @Then("el campo {string} debe tener un valor minimo de {int}")
    public void elCampoDebeTenerUnValorMinimoDe(String locator, int minValue) {
        helper.validateMinValue(locator, minValue);
    }

    @Then("el campo {string} debe tener un valor maximo de {int}")
    public void elCampoDebeTenerUnValorMaximoDe(String locator, int maxValue) {
        helper.validateMaxValue(locator, maxValue);
    }

    @Then("el campo {string} debe estar en modo solo lectura")
    public void elCampoDebeEstarEnModoSoloLectura(String locator) {
        helper.validateFieldIsReadonly(locator);
    }

    @Then("el campo {string} debe mostrar el placeholder {string}")
    public void elCampoDebeMostrarElPlaceholder(String locator, String expectedPlaceholder) {
        helper.validatePlaceholder(locator, expectedPlaceholder);
    }

    @Then("el campo {string} debe mostrar el tooltip {string}")
    public void elCampoDebeMostrarElTooltip(String locator, String expectedTooltip) {
        helper.validateTooltip(locator, expectedTooltip);
    }

    @StepDef(value = "web.element-validation.legacy-mensaje-visible",
             deprecated = true, replacedBy = "web.element-validation#elElementoDebeSerVisible")
    @Then("el mensaje {string} debe estar visible")
    public void elMensajeDebeEstarVisible(String locator) {
        helper.validateMessageIsVisible(locator);
    }

    @Then("el mensaje {string} no debe estar visible")
    public void elMensajeNoDebeEstarVisible(String locator) {
        helper.validateMessageIsNotVisible(locator);
    }

    @Then("el mensaje {string} debe contener el texto {string}")
    public void elMensajeDebeContenerElTexto(String locator, String expectedText) {
        helper.validateMessageContainsText(locator, expectedText);
    }

    @Then("el elemento {string} debe ser visible")
    public void elElementoDebeSerVisible(String locator) {
        Assertions.assertThat(engine().isPresent(locator))
            .as("Elemento %s no visible", locator)
            .isTrue();
    }

    @Then("el elemento {string} no debe ser visible")
    public void elElementoNoDebeSerVisible(String locator) {
        Assertions.assertThat(engine().isPresent(locator))
            .as("Elemento %s visible cuando no debería", locator)
            .isFalse();
    }

    @Then("el campo {string} no debe estar vacio")
    public void elCampoNoDebeEstarVacio(String locator) {
        helper.validateFieldNotEmpty(locator);
    }

    @Then("el campo {string} debe estar vacio")
    public void elCampoDebeEstarVacio(String locator) {
        helper.validateFieldIsEmpty(locator);
    }

    @StepDef(value = "web.element-validation.legacy-campo-habilitado",
             deprecated = true, replacedBy = "web.element-validation#verificoQueElElementoEstehabilitado")
    @Then("el campo {string} debe estar habilitado")
    public void elCampoDebeEstarHabilitado(String locator) {
        helper.validateButtonIsEnabled(locator);
    }

    @StepDef(value = "web.element-validation.legacy-boton-activo",
             deprecated = true, replacedBy = "web.element-validation#verificoQueElElementoEstehabilitado")
    @Then("el boton {string} debe estar activo")
    public void elBotonDebeEstarActivo(String locator) {
        helper.validateButtonIsEnabled(locator);
    }

    @StepDef(value = "web.element-validation.legacy-boton-inactivo",
             deprecated = true, replacedBy = "web.element-validation#verificoQueElElementoEsteDeshabilitado")
    @Then("el boton {string} debe estar inactivo")
    public void elBotonDebeEstarInactivo(String locator) {
        helper.validateButtonIsDisabled(locator);
    }

    @Then("el boton {string} debe cambiar de {string} a {string}")
    public void elBotonDebeCambiarDeTexto(String locator, String initialText, String finalText) {
        helper.validateButtonTextChange(locator, initialText, finalText);
    }

    @Then("el campo {string} debe tener una longitud minima de {int}")
    public void elCampoDebeTenerUnaLongitudMinimaDe(String locator, int minLength) {
        helper.validateMinLength(locator, minLength);
    }

    @Then("el campo {string} debe tener una longitud maxima de {int}")
    public void elCampoDebeTenerUnaLongitudMaximaDe(String locator, int maxLength) {
        helper.validateMaxLength(locator, maxLength);
    }

    @Then("el campo {string} debe tener exactamente {int} caracteres")
    public void elCampoDebeTenerExactamenteNCaracteres(String locator, int expectedLength) {
        helper.validateExactLength(locator, expectedLength);
    }

    @Then("el mensaje {string} debe contener el texto de la variable {string}")
    public void elMensajeDebeContenerElTextoDeLaVariable(String locator, String varName) {
        helper.validateMessageContainsText(locator, helper.getTextVariableTemp(varName));
    }

    // =========================================================================
    // NUEVOS STEPS GENÉRICOS — Validación v2.2.1
    // =========================================================================

    /**
     * Verifica la cantidad de elementos que coinciden con un locator.
     */
    @Then("verifico que la cantidad de elementos {string} sea {int}")
    public void verificoCantidadDeElementos(String locator, int expectedCount) {
        int actual = helper.countElements(locator);
        Assertions.assertThat(actual).as("Cantidad de elementos '%s'", locator).isEqualTo(expectedCount);
    }

    /**
     * Verifica que un elemento tenga un atributo con un valor específico.
     */
    @Then("verifico que el elemento {string} tenga el atributo {string} con valor {string}")
    public void verificoAtributoConValor(String locator, String attribute, String expectedValue) {
        helper.validateAttributeValue(locator, attribute, expectedValue);
    }

    /**
     * Verifica que un elemento tenga una clase CSS específica.
     */
    @Then("verifico que el elemento {string} tenga la clase CSS {string}")
    public void verificoClaseCss(String locator, String cssClass) {
        helper.validateHasCssClass(locator, cssClass);
    }

    /**
     * Verifica que un checkbox esté seleccionado.
     */
    @Then("verifico que el checkbox {string} este seleccionado")
    public void verificoCheckboxSeleccionado(String locator) {
        helper.validateCheckboxSelected(locator, true);
    }

    /**
     * Verifica que un checkbox NO esté seleccionado.
     */
    @Then("verifico que el checkbox {string} no este seleccionado")
    public void verificoCheckboxNoSeleccionado(String locator) {
        helper.validateCheckboxSelected(locator, false);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
