package com.qa.webcore.steps.validation;

import com.qa.common.api.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Steps de validacion y navegacion de tablas HTML.
 * Migrado de WebSteps.java.
 * @author Abel Venero
 * @since 2.0.0
 */
public class TableValidationSteps {

    private static final String STEP_SELECT_TABLE_ROW =
        "recorro tabla {string} y selecciono la fila"
        + " que tenga el valor {string} en la columna que tenga el valor {string}";

    private static final String STEP_VALIDATE_SUM =
        "verifico que la suma de las variables temporales {string} y {string}"
        + " sea igual al valor de la variable temporal {string}";

    private final WebHelper helper = new WebHelper();

    @When(STEP_SELECT_TABLE_ROW)
    public void recorroTablaYSeleccionoLaFilaQueTengaElValorEnLaColumna(
            String tabla, String valor, String columna) {
        Object result = engine().evaluate(
            "const tableLocator = arguments[0];"
                + "const targetValue = arguments[1];"
                + "const targetHeader = arguments[2];"
                + "const resolveEl = (locator) => {"
                + "  if (!locator) return null;"
                + "  const normalized = locator.trim();"
                + "  if (normalized.startsWith('xpath=')) {"
                + "    return document.evaluate(normalized.substring(6),"
                + " document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                + "  }"
                + "  if (normalized.startsWith('//') || normalized.startsWith('(//')) {"
                + "    return document.evaluate(normalized,"
                + " document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                + "  }"
                + "  return document.querySelector(normalized);"
                + "};"
                + "const table = resolveEl(tableLocator);"
                + "if (!table) return 'TABLE_NOT_FOUND';"
                + "const headers = Array.from(table.querySelectorAll('thead th, tr th'))"
                + ".map(h => h.textContent.trim());"
                + "const idx = headers.findIndex(h => h === targetHeader);"
                + "if (idx < 0) return 'COLUMN_NOT_FOUND';"
                + "const rows = Array.from(table.querySelectorAll('tbody tr, tr'))"
                + ".filter(r => r.querySelectorAll('td').length > idx);"
                + "for (const row of rows) {"
                + "  const cells = row.querySelectorAll('td');"
                + "  if (cells[idx] && cells[idx].textContent.trim() === targetValue) {"
                + "    row.click();"
                + "    return 'OK';"
                + "  }"
                + "}"
                + "return 'ROW_NOT_FOUND';",
            tabla, valor, columna
        );
        Assertions.assertThat(String.valueOf(result))
            .as("Error buscando el valor '%s' en la columna '%s' de la tabla '%s'", valor, columna, tabla)
            .isEqualTo("OK");
    }

    @Then("valido que las cabeceras de la tabla {string} sean {string}")
    public void validarQueLasCabecerasDeLaTablaSean(String tabla, String expectedHeaders) {
        Object result = engine().evaluate(
            "const tableLocator = arguments[0];"
                + "const resolveEl = (locator) => {"
                + "  if (!locator) return null;"
                + "  const normalized = locator.trim();"
                + "  if (normalized.startsWith('xpath=')) {"
                + "    return document.evaluate(normalized.substring(6),"
                + " document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                + "  }"
                + "  if (normalized.startsWith('//') || normalized.startsWith('(//')) {"
                + "    return document.evaluate(normalized,"
                + " document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                + "  }"
                + "  return document.querySelector(normalized);"
                + "};"
                + "const table = resolveEl(tableLocator);"
                + "if (!table) return '__TABLE_NOT_FOUND__';"
                + "const headers = Array.from(table.querySelectorAll('thead th, tr th'))"
                + ".map(h => h.textContent.trim());"
                + "return headers.join('|');",
            tabla
        );
        Assertions.assertThat(String.valueOf(result))
            .as("Tabla no encontrada para validar cabeceras: %s", tabla)
            .isNotEqualTo("__TABLE_NOT_FOUND__");

        List<String> actualHeaders = Arrays.stream(String.valueOf(result).split("\\|"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        List<String> expected = Arrays.stream(expectedHeaders.split("[,;|]"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        Assertions.assertThat(actualHeaders)
            .as("Cabeceras de tabla no coinciden")
            .isEqualTo(expected);
    }

    @Then(STEP_VALIDATE_SUM)
    public void validarSumaVariablesTemporales(String var1, String var2, String varResult) {
        Assertions.assertThat(helper.validateAdditionVariables(var1, var2, varResult)).
            as("La suma no es igual al resultado esperado").isTrue();
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
