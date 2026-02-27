package com.scotia.qa.webcore.steps;

import com.scotia.qa.common.config.ConfigManager;
import com.scotia.qa.common.cucumber.context.ScenarioContext;
import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.webcore.driver.DriverManager;
import com.scotia.qa.webcore.driver.WebDriverFactory;
import com.scotia.qa.webcore.driver.WebDriverFactory.BrowserType;
import com.scotia.qa.webcore.utils.WebHelper;
import com.scotia.qa.webcore.utils.WaitUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.WebDriver;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Steps de Cucumber para automatización Web UI - Framework Scotia QA.
 *
 * <p>Estos steps mantienen compatibilidad con la implementación anterior pero
 * están adaptados a la nueva arquitectura sin Spring.</p>
 *
 * <p><b>⚠️ IMPORTANTE:</b> Los contratos (nombres y parámetros) de los steps
 * se mantienen idénticos para no romper features existentes.</p>
 *
 * <p><b>Separación de responsabilidades:</b></p>
 * <ul>
 *   <li><b>WebSteps:</b> Solo definiciones de steps de Cucumber</li>
 *   <li><b>WebHelper:</b> Lógica de interacción con elementos y utilidades</li>
 * </ul>
 *
 * @author Abel Venero
 * @version 1.1.0
 */
public class WebSteps {

    private Scenario scenario;
    private final WebHelper helper;

    public WebSteps() {
        this.helper = new WebHelper();
    }

    // =========================================================================
    // HOOKS - BEFORE & AFTER
    // =========================================================================

    /**
     * Hook que se ejecuta SOLO si el escenario tiene tags relacionados con Web.
     * Esto evita inicializar WebDriver innecesariamente en tests API puros o Database puros.
     *
     * Tags soportados: @web, @ui, @selenium, @browser
     */
    @Before(value = "@web or @ui or @selenium or @browser", order = 100)
    public void beforeScenario(Scenario scenario) throws FrameworkBusinessException {
        this.scenario = scenario;

        com.scotia.qa.common.cucumber.validators.HookValidator.validateWebScenario(scenario);

        String moduleName = com.scotia.qa.common.logging.ModuleDetector.detectModuleName();
        TestLogger.setFramework(moduleName);

        // Inicializar driver si no existe
        if (!DriverManager.isDriverInitialized()) {
            BrowserType browser = getBrowserForScenario();
            boolean headless = getHeadlessModeForScenario();

            WebDriver driver = WebDriverFactory.createDriver(browser, headless);
            DriverManager.setDriver(driver);

            TestLogger.logInfo("WEB_STEPS",
                "Driver inicializado",
                java.util.Map.of("browser", browser.name(), "headless", headless));
        }

        WebDriver driver = DriverManager.getDriver();

        String host = helper.getConfigProperty("host", "about:blank");
        driver.navigate().to(host);
        driver.manage().window().maximize();

        WaitUtils.setPageLoadTimeout(90);

        TestLogger.logInfo("WEB_STEPS",
            "🚀 Escenario iniciado: " + scenario.getName(), null);
    }

    /**
     * Hook de limpieza que se ejecuta SOLO si el @Before se ejecutó.
     */
    @After(value = "@web or @ui or @selenium or @browser", order = 100)
    public void afterScenario(Scenario scenario) {
        TestLogger.logInfo("WEB_STEPS", "🏁 Finalizando escenario", null);

        try {
            WebDriver driver = DriverManager.getDriver();

            // Si el escenario falló, capturar screenshot
            if (scenario.isFailed()) {
                helper.captureScreenOnFailure(scenario);
            }

            // Limpiar cookies y cerrar driver
            driver.manage().deleteAllCookies();
            DriverManager.quitDriver();

            TestLogger.logInfo("WEB_STEPS", "✅ Escenario finalizado correctamente", null);

        } catch (Exception e) {
            TestLogger.logError("WEB_STEPS",
                "Error en afterScenario: " + e.getMessage(), null);
            DriverManager.quitDriverSafely();
        }
    }

    // =========================================================================
    // GIVEN STEPS - CONFIGURACIÓN
    // =========================================================================

    /**
     * Step: Configura el navegador y modo headless para el scenario.
     *
     * <p><b>⭐ NUEVO (v1.2.0):</b> Permite configurar navegador y headless desde Gherkin</p>
     *
     * <p><b>Navegadores soportados:</b></p>
     * <ul>
     *   <li>"chrome" - Google Chrome</li>
     *   <li>"firefox" - Mozilla Firefox</li>
     *   <li>"edge" - Microsoft Edge</li>
     *   <li>"safari" - Safari (solo Mac)</li>
     * </ul>
     *
     * <p><b>Modo headless:</b></p>
     * <ul>
     *   <li>"true" / "yes" / "si" / "1" - Sin UI (para CI/CD)</li>
     *   <li>"false" / "no" / "0" - Con UI (para desarrollo)</li>
     * </ul>
     *
     * <p><b>Versión y estrategia:</b> Se leen desde config-{env}.properties</p>
     * <pre>
     * driver.chrome.version=143.0.7499.41
     * driver.strategy=artifactory
     * </pre>
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * Given configuro el driver del navegador "firefox" en modo headless "false"
     * When navego a la URL "https://google.com"
     * </pre>
     *
     * <p><b>💡 TIP:</b> Si no usas este step, el navegador por defecto es Chrome.</p>
     *
     * @param browserName Nombre del navegador: chrome, firefox, edge, safari
     * @param headlessStr Modo headless: true/false, yes/no, si/no, 1/0
     */
    @Given("configuro el driver del navegador {string} en modo headless {string}")
    public void configurarDriverDelNavegador(String browserName, String headlessStr)
            throws FrameworkBusinessException {
        BrowserType browser = helper.parseBrowserType(browserName);
        boolean headless = helper.parseBoolean(headlessStr);

        ScenarioContext.set("web.browser.type", browser);
        ScenarioContext.set("web.headless.override", headless);

        TestLogger.logInfo("WEB_STEPS",
            "Navegador configurado para este scenario",
            java.util.Map.of("browser", browser.name(), "headless", headless));
    }

    // =========================================================================
    // GIVEN STEPS - NAVEGACIÓN
    // =========================================================================

    @Given("actualizo URL en el navegador {string}")
    public void actualizo_url_en_el_navegador(String url) {
        WebDriver driver = DriverManager.getDriver();
        driver.navigate().to(url);
        WaitUtils.waitForPageReady();
        TestLogger.logInfo("WEB_STEPS", "🌐 URL actualizada: " + url, null);
    }


    // =========================================================================
    // WHEN STEPS - ACCIONES
    // =========================================================================


    @When("ingreso el texto {string} en el elemento {string}")
    public void ingresoElTextoEnElElemento(String texto, String locator) {
        helper.setTextWithWait(texto, locator);
        helper.captureScreen(scenario);
    }

    @When("ingreso texto de la variable temporal {string} en elemento {string}")
    public void setTextoVariableTemporalEnElemento(String variableName, String locator) {
        String texto = helper.getTextVariableTemp(variableName);
        helper.setText(locator, texto);
        // Nota: El logging ya se hace en WebHelper.setText() con masking de datos sensibles
    }

    @When("capturo una imagen de la pantalla")
    public void capturoUnaImgaenDeLaPantalla() {
        helper.captureScreen(scenario);
    }

    @When("selecciono el valor {string} en el combobox {string}")
    public void seleccionoElTextoEnElCombobox(String valor, String locator) {
        helper.selectOptionComboBox(locator, valor);
        helper.captureScreen(scenario);
        TestLogger.logInfo("WEB_STEPS",
            "🔽 Combobox seleccionado: " + locator + " = " + valor, null);
    }

    @When("selecciono el valor de la variable {string} en el combobox {string}")
    public void seleccionoElTextoDeVariableEnElCombobox(String variableName, String locator) {
        String valor = helper.getTextVariableTemp(variableName);
        helper.selectOptionComboBox(locator, valor);
        helper.captureScreen(scenario);
    }

    @When("espero hasta que elemento {string} este visible")
    public void esperarHastaQueElementoEsteVisible(String locator) {
        helper.waitForVisibleElement(locator, 60);
    }

    @When("espero hasta que elemento {string} este habilitado")
    public void esperarHastaQueElementoEsteHabilitado(String locator) {
        helper.waitAndValidateEnabled(locator, 60);
    }

    @When("espero hasta que elemento {string} no este visible")
    public void esperarHastaQueElementoNoEsteVisible(String locator) {
        helper.waitFromElementNoVisible(locator);
        helper.captureScreen(scenario);
    }

    @When("espero {string} segundos")
    public void esperarUnTiempo(String segundos) {
        int seconds = Integer.parseInt(segundos);
        WaitUtils.waitMilliseconds(seconds * 1000L);
        helper.captureScreen(scenario);
        TestLogger.logWarning("WEB_STEPS",
            "⏱️ Wait fijo usado: " + segundos + " segundos", null);
    }

    @When("cambio al IFrame path {string}")
    public void cambioIFramePath(String path) {
        helper.changeIFrame(path, "");
        helper.captureScreen(scenario);
    }

    @When("cambio de Iframe nombre {string}")
    public void cambioIframeNombre(String name) {
        helper.changeIFrame("", name);
        helper.captureScreen(scenario);
    }

    @When("inicializo Iframe principal")
    public void inicializarIframePrincipal() {
        helper.leaveIFrame();
    }

    @When("hago scroll hasta el elemento {string}")
    public void irAlElemento(String locator) {
        helper.scroll(locator);
        helper.captureScreen(scenario);
    }

    @When("hago scroll hacia {string}")
    public void scrollDirection(String direction) {
        helper.scrollByDirection(direction);
        helper.captureScreen(scenario);
    }

    @When("presiono el boton {string}")
    public void presionoElBoton(String locator) {
        helper.clicButton(locator);
        helper.captureScreen(scenario);
    }

    @When("presiono tab en el elemento {string}")
    public void realizarClick(String locator) {
        helper.tabAction(locator);
    }

    @When("presiono el boton al doble contenedor {string} {string} con shadow elemento {string}")
    public void presionoElBotonAlDobleContenedorConShadowElemento(String shadowHost1, String shadowHost2, String elemento) {
        helper.clickNestedShadow(shadowHost1, shadowHost2, elemento);
    }

    @When("presiono el boton {string} que se encuentra dentro del shadow {string}")
    public void presionoElBotonDentroDelShadow(String elemento, String shadowHost) {
        helper.clickElementInShadow(elemento, shadowHost);
    }

    @When("adjunto a jira el archivo de texto llamado {string}")
    public void adjuntoAJiraElArchivoQueEstaEnElDirectorio(String fileName) throws IOException {
        helper.attachInReport(scenario, fileName);
    }

    @When("vuelvo a la ventana principal")
    public void vuelvoALaVentanaPrincipal() {
        helper.backToPrincipalWindow();
    }

    @When("hago click de js al elemento {string}")
    public void hagoClickDeJsAlElemento(String locator) {
        helper.clickJs(locator);
    }

    @When("cierro la ventana")
    public void cierroLaVentana() {
        helper.closeWindow();
    }

    @When("selecciono el RadioButon con Valor {string}")
    public void seleccionRadioButonValor(String valor) {
        TestLogger.logInfo("WEB_STEPS", "📻 Radio button seleccionado: " + valor, null);
    }

    @When("realizo click derecho en elemento {string}")
    public void realizar_click_derecho_en_elemento(String locator) {
        helper.rightClick(locator);
    }

    @When("cambio foco a la ventana nueva")
    public void cambiarPagina() {
        String response = helper.changeWindowNew();
        Assertions.assertThat(response)
            .as("Error cambiando a la nueva ventana")
            .isEqualTo("OK");
    }

    @When("guardo texto del elemento {string} en variable temporal llamada {string}")
    public void guardarTextoDelElementoEnVariableTemporalLlamada(String locator, String variableName) {
        String texto = helper.getTextOf(locator);
        helper.saveVariableTemp(texto, variableName);
    }

    @When("guardo texto {string} en variable temporal llamada {string}")
    public void guardarTextoVariableTemporalLlamada(String texto, String variableName) {
        helper.saveVariableTemp(texto, variableName);
    }

    @When("espero hasta que la seccion termine de cargar")
    public void esperoHastaQueLaSeccionTermineDeCargar() {
        helper.captureScreen(scenario);
        helper.waitForPageLoad(90);
    }

    @When("genero archivo de texto llamado {string} con las variables temporales")
    public void generoArchivoDeTextoConLasVariablesTemporales(String fileName) {
        helper.generateFileTxt(fileName);
    }

    @When("recorro tabla {string} y selecciono la fila que tenga el valor {string} en la columna que tenga el valor {string}")
    public void recorroTablaYSeleccionoLaFilaQueTengaElValorEnLaColumna(String tabla, String valorBuscado, String columna) {
        String response = helper.selectRowTable(tabla, valorBuscado, columna);
        Assertions.assertThat(response)
            .as("Error buscando el valor en la tabla")
            .isEqualTo("OK");
    }

    @When("espero que el checkbox {string} salga seleccionado")
    public void checkboxwait(String locator) {
        helper.waitCheckBox(locator);
    }

    @When("valido si hay dos ventanas y cierro la ultima que se abrio")
    public void validoSiHayDosVentanasYCierroLaUltimaQueSeAbrio() {
        helper.closedLastWindows();
    }

    @When("situo el cursor del mouse sobre el elemento {string}")
    public void situoElCursorDelMouseSobreElElemento(String locator) {
        helper.moveToElement(locator);
    }

    @When("Selecciono el iframe con atributo css {string}")
    public void seleccionoElIframeConAtributoCss(String cssSelector) {
        helper.selectIframeByCssSelector(cssSelector);
    }

    @When("consulto la base de datos en {string}")
    public void consultoLaBaseDeDatosEn(String db, String query) {
        TestLogger.logInfo("WEB_STEPS", "🗄️ Consulta BD: " + db, null);
    }

    @When("genero RUT y guardo en variable {string}")
    public void generateRut(String variableName) {
        String rut = helper.generateRutUy();
        helper.saveVariableTemp(rut, variableName);
    }

    @When("adjunto un archivo al scenario con la data")
    public void adjuntoUnArchivoAlScenarioConLaData(String data) {
        helper.attachScenario(data, scenario);
    }

    @When("recargo pagina")
    public void recargoPagina() {
        helper.refreshPage();
    }

    @When("busco un documento que no exista en la bbdd de homebanking y guardo en {string}")
    public void buscoDocumentoValidoHb(String variableName) {
        TestLogger.logInfo("WEB_STEPS",
            "🔍 Buscando documento válido en HB", null);
    }

    @When("cambio de ventana")
    public void cambiodeVentana() {
        helper.handleTabs();
    }

    @When("Ingreso nombre aleatorio en el elemento {string}")
    public void nombreAleatorio(String locator) {
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String fechaHoraFormateada = ahora.format(formato);
        ingresoElTextoEnElElemento(fechaHoraFormateada, locator);
    }

    // =========================================================================
    // AND STEPS
    // =========================================================================

    @And("seteo cookie: {string} con valor {string}")
    public void seteoCookie(String nombre, String valor) {
        helper.setCookie(nombre, valor);
    }

    @And("esperar que se muestre home")
    public void esperarQueSeMuestreHome() {
        helper.waitForHome();
        helper.captureScreen(scenario);
    }

    @And("cerrar banner lateral")
    public void cerrarBannerShadowHome() {
        helper.doClicTeclaESC();
    }

    @And("verifico si existe el elemento {string} con host {string} y hago clic")
    public void verificoSiExisteElementoShadowYHagoClic(String elemento, String host) {
        helper.clickAndGoToElementInShadow(elemento, host);
        helper.captureScreen(scenario);
    }

    @And("verifico si existe el elemento {string} con host {string}")
    public void verificoSiExisteElementoConShadow(String elemento, String host) {
        helper.isElementShadowPresent(elemento, host);
        helper.captureScreen(scenario);
    }

    @And("verifico si existe el elemento {string} con host principal {string}, host secundario {string} y hago clic")
    public void verificoSiExisteElementoConHostDentroDeOtroHostYHagoClic(String elemento, String host1, String host2) {
        helper.clickAndGoToElementInShadowInNode(elemento, host1, host2);
        helper.captureScreen(scenario);
    }


    @And("busco dentro de el shadow con host {string} el elemento {string}")
    public void buscoElementoEnShadow(String host, String elemento) {
        helper.findElementInShadow(host, elemento);
        helper.captureScreen(scenario);
    }

    @And("selecciono la opcion con el valor {string} en el combobox {string}")
    public void seleccionoLaOpcionConElValorEnElCombobox(String valor, String locator) {
        helper.selectOptionComboBoxByValue(locator, valor);
        helper.captureScreen(scenario);
    }

    // =========================================================================
    // THEN STEPS - VALIDACIONES
    // =========================================================================

    @Then("verifico si existe el elemento {string}")
    public void verificoSiExisteElElemento(String locator) {
        // Esperar a que el elemento sea visible usando timeout configurado
        boolean exists = helper.waitForVisibleElement(locator);
        Assertions.assertThat(exists)
            .as("Elemento " + locator + " no encontrado o no visible")
            .isTrue();
        helper.captureScreen(scenario);
    }

    @Then("verifico que el texto en {string} sea {string}")
    public void verificoQueElTextoEnSea(String locator, String expectedText) {
        String actualText = helper.getTextOf(locator);
        // Resolver variables temporales como {full_name}
        String resolvedExpectedText = helper.resolveVariables(expectedText);
        Assertions.assertThat(actualText)
            .as("Texto no coincide")
            .isEqualTo(resolvedExpectedText);
        helper.captureScreen(scenario);
    }

    @Then("verifico que no exista el elemento {string}")
    public void verificoQueNoExistaElElemento(String locator) {
        boolean exists = helper.isPresent(locator);
        Assertions.assertThat(exists)
            .as("Elemento " + locator + " encontrado!")
            .isFalse();
        helper.captureScreen(scenario);
    }

    @Then("verifico que el elemento {string} este desactivado")
    public void verificoQueElElementoEsteDesactivado(String locator) {
        boolean active = helper.isActive(locator);
        Assertions.assertThat(active)
            .as("El estado de " + locator + " es Activo!")
            .isFalse();
        helper.captureScreen(scenario);
    }

    @Then("verifico que el elemento {string} este deshabilitado")
    public void verificoQueElElementoEsteDeshabilitado(String locator) {
        boolean disabled = helper.isDisabled(locator);
        Assertions.assertThat(disabled)
            .as("El elemento " + locator + " está habilitado!")
            .isTrue();
        helper.captureScreen(scenario);
    }

    @Then("verifico que el elemento {string} este habilitado")
    public void verificoQueElElementoEstehabilitado(String locator) {
        boolean disabled = helper.isDisabled(locator);
        Assertions.assertThat(disabled)
            .as("El elemento " + locator + " está deshabilitado!")
            .isFalse();
        helper.captureScreen(scenario);
    }

    @Then("verifico que la lista de opciones para el combobox {string} sea")
    public void verificoQueLaListaDeOpcionesParaElComboboxSea(String locator, List<String> expectedOptions) {
        List<String> actualOptions = helper.getListComboBox(locator);
        Assertions.assertThat(actualOptions)
            .as("Lista de opciones no coincide")
            .isEqualTo(expectedOptions);
        helper.captureScreen(scenario);
    }

    @Then("verifico que el elemento {string} este activo")
    public void verificoQueElElementoEsteActivo(String locator) {
        boolean active = helper.isActive(locator);
        Assertions.assertThat(active)
            .as("El estado de " + locator + " es Desactivado!")
            .isTrue();
        helper.captureScreen(scenario);
    }

    @Then("verifico si existe el elemento {string} y valido que el texto sea {string}")
    public void verificoSiExisteElElementoYValidoQueElTextoSea(String locator, String expectedText) {
        // Esperar a que el elemento sea visible antes de validar texto
        if (helper.waitForVisibleElement(locator)) {
            String actualText = helper.getTextOf(locator);
            // Resolver variables temporales como {full_name}
            String resolvedExpectedText = helper.resolveVariables(expectedText);
            Assertions.assertThat(actualText)
                .as("Texto obtenido '" + actualText + "', no coincide con '" + resolvedExpectedText + "'")
                .isEqualTo(resolvedExpectedText);
        } else {
            Assertions.fail("Elemento " + locator + " no apareció o no es visible");
        }
    }

    @Then("verifico si existe el elemento {string} y valido que el texto contenga {string}")
    public void verificoSiExisteElElementoYValidoQueElTextoContenga(String locator, String expectedSubtext) {
        // Esperar a que el elemento sea visible antes de validar
        if (helper.waitForVisibleElement(locator)) {
            // Resolver variables temporales como {full_name}
            String resolvedSubtext = helper.resolveVariables(expectedSubtext);
            boolean contains = helper.getTextOfContainsText(locator, resolvedSubtext);
            Assertions.assertThat(contains)
                .as("El texto del elemento " + locator + " no contiene '" + resolvedSubtext + "'")
                .isTrue();
        } else {
            Assertions.fail("Elemento " + locator + " no apareció o no es visible");
        }
    }

    @Then("verifico que el texto en {string} contenga el texto de la variable temporal {string}")
    public void verificoTextoEnContengaElTextoVariableTemporal(String locator, String variableName) {
        boolean contains = helper.getTextOfContainsVariable(locator, variableName);
        Assertions.assertThat(contains)
            .as("El texto del elemento " + locator + " no contiene el texto de la variable " + variableName)
            .isTrue();
    }

    @Then("verifico que el texto en {string} sea igual al de la variable temporal {string}")
    public void verificoQueElTextoEnSeaIgualAlDeLaVariableTemporal(String locator, String variableName) {
        String expectedText = helper.getTextVariableTemp(variableName);
        String actualText = helper.getTextOf(locator);
        Assertions.assertThat(actualText)
            .as("Error, los valores no son iguales")
            .isEqualTo(expectedText);
    }

    @Then("verifico que el texto en {string} contenga el texto {string}")
    public void verificoTextoEnContengaElTexto(String locator, String expectedSubtext) {
        // Resolver variables temporales como {full_name}
        String resolvedSubtext = helper.resolveVariables(expectedSubtext);
        boolean contains = helper.getTextOfContainsText(locator, resolvedSubtext);
        Assertions.assertThat(contains)
            .as("El texto del elemento " + locator + " no es igual al texto esperado " + resolvedSubtext)
            .isTrue();
    }

    @Then("verifico si existe el combobox {string} y selecciono el valor {string}")
    public void verificoSiExisteElElementoYSeleccionoOpcion(String locator, String valor) {
        // Esperar a que el combobox sea visible antes de seleccionar
        if (helper.waitForVisibleElement(locator)) {
            helper.selectOptionComboBox(locator, valor);
        }
    }

    @Then("verifico si existe la alerta y selecciono aceptar")
    public void verificoSiExisteAlertaYSeleccionoAceptar() {
        helper.captureScreen(scenario);
        helper.acceptAlert();
    }

    @Then("verifico que el check {string} este seleccionado")
    public void verificoQueElCheckEsteSeleccionado(String locator) {
        boolean selected = helper.radioBtnIsSelect(locator);
        Assertions.assertThat(selected)
            .as("El elemento " + locator + " no está seleccionado!")
            .isTrue();
    }

    @Then("valido que el texto de elemento {string} no contenga el texto {string}")
    public void validoQueElValorDelElementoNoSeaVacio(String locator, String texto) {
        boolean notContains = helper.valueElementNotContains(locator, texto);
        Assertions.assertThat(notContains)
            .as("El valor del elemento " + locator + " contiene el texto: " + texto)
            .isTrue();
    }

    @Then("valido que se despliegue la nueva ventana {string}")
    public void validoQueSeDespliegueLaNuevaVentana(String expectedWindowName) {
        String actualWindowName = helper.getWindowName();
        Assertions.assertThat(actualWindowName)
            .as("Error, los valores no son iguales")
            .isEqualTo(expectedWindowName);
        DriverManager.quitDriver();
    }

    @Then("verifico si existe el elemento {string} y hago clic")
    public void verificoSiExisteElElementoYHagoClic(String locator) {
        // Esperar a que el elemento sea visible antes de hacer click
        if (helper.waitForVisibleElement(locator)) {
            presionoElBoton(locator);
        }
    }

    @Then("verifico si existe el texto {string} y hago clic")
    public void verificoSiExisteElTextoYHagoClic(String texto) {
        helper.checkTextAndClic(texto);
    }

    @Then("verifico que el texto en el elemento {string} sea {string} dentro del doble contenedor shadow {string} {string}")
    public void verificoTextoDentroDeDobleShadow(String elemento, String expectedText, String shadowHost1, String shadowHost2) {
        helper.verifyTextInNestedShadow(elemento, expectedText, shadowHost1, shadowHost2);
    }

    @Then("verifico que el texto en el elemento {string} sea {string} dentro del shadow {string}")
    public void verificoTextoDentroDeShadow(String elemento, String expectedText, String shadowHost) {
        helper.verifyTextInShadow(elemento, expectedText, shadowHost);
    }

    @Then("verifico el texto del combobox {string} sea {string}")
    public void verificoElTextoDelComboboxSea(String locator, String expectedText) {
        String actualText = helper.getTextOfCombobox(locator);
        Assertions.assertThat(actualText)
            .as("Texto no coincide")
            .isEqualTo(expectedText);
    }

    @Then("valido que las cabeceras de la tabla {string} sean {string}")
    public void validarQueLasCabecerasDeLaTablaSean(String tabla, String expectedHeaders) {
        String result = helper.validateHeadTable(tabla, expectedHeaders);
        Assertions.assertThat(result).isEqualTo("OK");
    }

    @Then("verifico que el valor del elemento {string} no sea cero")
    public void verificoQueElValorDelElementoNoSeaCero(String locator) {
        boolean notZero = helper.valueElementNotZero(locator);
        Assertions.assertThat(notZero)
            .as("El valor del elemento " + locator + " es cero!")
            .isTrue();
    }

    @Then("verifico que la suma de las variables temporales {string} y {string} sea igual al valor de la variable temporal {string}")
    public void validarSumaVariablesTemporales(String var1, String var2, String varResult) {
        boolean valid = helper.validateAdditionVariables(var1, var2, varResult);
        Assertions.assertThat(valid)
            .as("La suma de las variables: " + var1 + " y " + var2 + " no es igual al de la variable: " + varResult)
            .isTrue();
    }

    @Then("valido que el valor de elemento {string} no sea vacio")
    public void validoQueElValorDelElementoNoSeaVacio2(String locator) {
        boolean notEmpty = helper.valueElementNotEmpty(locator);
        Assertions.assertThat(notEmpty)
            .as("El valor del elemento " + locator + " es vacío!")
            .isTrue();
    }

    @Then("verifico si existe el elemento {string} e ingreso el texto {string}")
    public void verificoSiExisteElElementoYIngresoTexto(String locator, String texto) {
        // Esperar a que el elemento sea visible antes de ingresar texto
        if (helper.waitForVisibleElement(locator)) {
            helper.setText(locator, texto);
        }
    }

    @Then("valido que el valor almacenado en el campo {string} sea {string}")
    public void validoQueElValorAlmacenadoEnElCampoSea(String campo, String expectedValue) {
        helper.isFieldEquals(campo, expectedValue);
    }

    @Then("valido que exista el texto")
    public void validoQueExistaElTexto(String texto) {
        boolean exists = helper.existText(texto);
        Assertions.assertThat(exists)
            .as("El texto: " + texto + " no existe")
            .isTrue();
    }

    @Then("valido que la url abierta sea {string}")
    public void validoUrl(String expectedUrl) {
        helper.urlValid(expectedUrl);
    }

    // =========================================================================
    // NUEVOS STEPS - VALIDACIONES DE FORMATO Y TIPO DE DATO
    // =========================================================================

    @Then("el campo {string} debe aceptar solo números")
    public void elCampoDebeAceptarSoloNumeros(String locator) {
        helper.validateFieldAcceptsOnlyNumbers(locator);
    }

    @Then("el campo {string} debe aceptar solo letras")
    public void elCampoDebeAceptarSoloLetras(String locator) {
        helper.validateFieldAcceptsOnlyLetters(locator);
    }

    @Then("el campo {string} no debe aceptar números ni caracteres especiales")
    public void elCampoNoDebeAceptarNumerosNiCaracteresEspeciales(String locator) {
        helper.validateFieldNoNumbersNoSpecialChars(locator);
    }

    @Then("el campo {string} debe tener formato de email válido")
    public void elCampoDebeTenerFormatoDeEmailValido(String locator) {
        helper.validateEmailFormat(locator);
    }

    @Then("el campo {string} debe tener formato de teléfono con prefijo {string} y {int} dígitos totales")
    public void elCampoDebeTenerFormatoDeTelefonoConPrefijoYDigitos(String locator, String prefix, int totalDigits) {
        helper.validatePhoneFormat(locator, prefix, totalDigits);
    }

    @Then("el campo {string} no debe contener espacios en blanco")
    public void elCampoNoDebeContenerEspaciosEnBlanco(String locator) {
        helper.validateFieldNoSpaces(locator);
    }

    @Then("el campo {string} debe agregar separadores de miles automáticamente")
    public void elCampoDebeAgregarSeparadoresDeMilesAutomaticamente(String locator) {
        helper.validateThousandsSeparators(locator);
    }

    @Then("el campo {string} debe tener el formato con patrón {string}")
    public void elCampoDebeTenerElFormatoConPatron(String locator, String regexPattern) {
        helper.validateFieldMatchesPattern(locator, regexPattern);
    }

    @Then("el valor formateado debe ser {string}")
    public void elValorFormateadoDebeSer(String expectedValue) {
        helper.validateFormattedValue(expectedValue);
    }

    @Then("el campo {string} debe tener un valor mínimo de {int}")
    public void elCampoDebeTenerUnValorMinimoDe(String locator, int minValue) {
        helper.validateMinValue(locator, minValue);
    }

    @Then("el campo {string} debe tener un valor máximo de {int}")
    public void elCampoDebeTenerUnValorMaximoDe(String locator, int maxValue) {
        helper.validateMaxValue(locator, maxValue);
    }

    @Then("el campo {string} debe estar en modo solo lectura")
    public void elCampoDebeEstarEnModoSoloLectura(String locator) {
        helper.validateFieldIsReadonly(locator);
    }

    // =========================================================================
    // VALIDACIONES DE OPCIONES (DROPDOWNS, RADIO BUTTONS)
    // =========================================================================

    @Then("las opciones del campo {string} deben ser {string}")
    public void lasOpcionesDelCampoDebenSer(String locator, String expectedOptions) {
        helper.validateDropdownOptions(locator, expectedOptions);
    }

    @Then("el campo {string} debe tener {int} opciones")
    public void elCampoDebeTenerNOpciones(String locator, int expectedCount) {
        helper.validateDropdownOptionCount(locator, expectedCount);
    }

    @Then("el campo {string} debe permitir selección única")
    public void elCampoDebePermitirSeleccionUnica(String locator) {
        helper.validateSingleSelection(locator);
    }

    // =========================================================================
    // VALIDACIONES DE ESTADO DE BOTONES
    // =========================================================================

    @Then("el botón {string} debe estar activo")
    public void elBotonDebeEstarActivo(String locator) {
        helper.validateButtonIsEnabled(locator);
    }

    @Then("el botón {string} debe estar inactivo")
    public void elBotonDebeEstarInactivo(String locator) {
        helper.validateButtonIsDisabled(locator);
    }

    @Then("el campo {string} debe estar habilitado")
    public void elCampoDebeEstarHabilitado(String locator) {
        helper.validateButtonIsEnabled(locator);
    }

    @Then("el botón {string} debe cambiar de {string} a {string}")
    public void elBotonDebeCambiarDeTexto(String locator, String initialText, String finalText) {
        helper.validateButtonTextChange(locator, initialText, finalText);
    }

    // =========================================================================
    // VALIDACIONES DE LONGITUD DE TEXTO
    // =========================================================================

    @Then("el campo {string} debe tener una longitud mínima de {int}")
    public void elCampoDebeTenerUnaLongitudMinimaDe(String locator, int minLength) {
        helper.validateMinLength(locator, minLength);
    }

    @Then("el campo {string} debe tener una longitud máxima de {int}")
    public void elCampoDebeTenerUnaLongitudMaximaDe(String locator, int maxLength) {
        helper.validateMaxLength(locator, maxLength);
    }

    @Then("el campo {string} debe tener exactamente {int} caracteres")
    public void elCampoDebeTenerExactamenteNCaracteres(String locator, int expectedLength) {
        helper.validateExactLength(locator, expectedLength);
    }

    // =========================================================================
    // VALIDACIONES DE PLACEHOLDERS Y TOOLTIPS
    // =========================================================================

    @Then("el campo {string} debe mostrar el placeholder {string}")
    public void elCampoDebeMostrarElPlaceholder(String locator, String expectedPlaceholder) {
        helper.validatePlaceholder(locator, expectedPlaceholder);
    }

    @Then("el campo {string} debe mostrar el tooltip {string}")
    public void elCampoDebeMostrarElTooltip(String locator, String expectedTooltip) {
        helper.validateTooltip(locator, expectedTooltip);
    }

    // =========================================================================
    // VALIDACIONES DE MENSAJES
    // =========================================================================

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

    // =========================================================================
    // VALIDACIONES DE VISIBILIDAD Y EXISTENCIA
    // =========================================================================

    @Then("el elemento {string} debe ser visible")
    public void elElementoDebeSerVisible(String locator) {
        helper.validateElementIsVisible(locator);
    }

    @Then("el elemento {string} no debe ser visible")
    public void elElementoNoDebeSerVisible(String locator) {
        helper.validateElementIsNotVisible(locator);
    }

    @Then("el campo {string} no debe estar vacío")
    public void elCampoNoDebeEstarVacio(String locator) {
        helper.validateFieldNotEmpty(locator);
    }

    @Then("el campo {string} debe estar vacío")
    public void elCampoDebeEstarVacio(String locator) {
        helper.validateFieldIsEmpty(locator);
    }

    @Then("valido que la variable {string} sea igual a la variable {string}")
    public void validoQueLaVariableSeaIgualALaVariable(String varName1, String varName2) {
        String value1 = helper.getTextVariableTemp(varName1);
        String value2 = helper.getTextVariableTemp(varName2);
        Assertions.assertThat(value1)
            .as("La variable '" + varName1 + "' (" + value1 + ") no es igual a '"
                + varName2 + "' (" + value2 + ")")
            .isEqualTo(value2);
    }

    @Then("el mensaje {string} debe contener el texto de la variable {string}")
    public void elMensajeDebeContenerElTextoDeLaVariable(String locator, String varName) {
        String expectedText = helper.getTextVariableTemp(varName);
        helper.validateMessageContainsText(locator, expectedText);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES - CONFIGURACIÓN DE DRIVER
    // =========================================================================

    /**
     * Determina qué navegador usar para este scenario.
     * Prioridad:
     * 1. ScenarioContext (step "configuro el driver del navegador")
     * 2. System Property (-Dweb.browser=firefox)
     * 3. ConfigManager (config-{env}.properties)
     * 4. Default (Chrome)
     */
    private BrowserType getBrowserForScenario() throws FrameworkBusinessException {
        BrowserType browserFromContext = (BrowserType) ScenarioContext.get("web.browser.type");
        if (browserFromContext != null) {
            return browserFromContext;
        }

        ConfigManager config = ConfigManager.getInstance();
        String browserStr = config.getWithPriority("web.browser", "chrome");

        return helper.parseBrowserType(browserStr);
    }

    /**
     * Determina modo headless.
     * Prioridad:
     * 1. ScenarioContext (step con headless específico)
     * 2. System Property (-Dweb.headless=true)
     * 3. ConfigManager (web.headless del properties)
     * 4. false (default)
     */
    private boolean getHeadlessModeForScenario() throws FrameworkBusinessException {
        Boolean headlessOverride = (Boolean) ScenarioContext.get("web.headless.override");
        if (headlessOverride != null) {
            return headlessOverride;
        }

        ConfigManager config = ConfigManager.getInstance();
        return config.getBoolean("web.headless", false);
    }
}
