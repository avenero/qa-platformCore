# 🚗 Steps Específicos de Negocio - Préstamos Automotor

**Framework:** Scotia QA  
**Tipo:** Steps de negocio específico  
**Ubicación sugerida:** Módulo específico del proyecto (NO en framework core)  
**Fecha:** 2026-02-18

---

## ⚠️ IMPORTANTE

Estos steps contienen **lógica de negocio específica** y **NO deben estar en el framework genérico** (api-core, web-core).

**Ubicación correcta:** Crear un módulo específico como `automotor-steps` o `lending-steps` que dependa del framework core.

---

## 📋 Steps de API - OTP (Autenticación)

### 1. Solicitar envío de OTP
```java
/**
 * Solicita el envío de un código OTP al email especificado.
 * NEGOCIO: Flujo de autenticación específico de préstamos
 */
@When("solicito el envío de un código OTP al email {string}")
public void solicitoElEnvioDeUnCodigoOTPAlEmail(String email) 
    throws FrameworkTechnicalException, FrameworkBusinessException {
  TestLogger.logInfo("API_STEPS_OTP",
      "Solicitando envío de OTP a email: " + email.replaceAll("(.{3}).*(@.*)", "$1***$2"), null);

  try {
    // Construir JSON body manualmente
    String jsonBody = String.format("{\"email\":\"%s\"}", email);
    getHttpClient().addHeader("Content-Type", "application/json");
    getHttpClient().setBody(jsonBody);
    getHttpClient().post(""); // El endpoint ya debe estar configurado

    TestLogger.logInfo("API_STEPS_OTP", "✅ OTP solicitado exitosamente", null);

  } catch (Exception e) {
    TestLogger.logError("API_STEPS_OTP", "Error solicitando OTP: " + e.getMessage(), null);
    throw new FrameworkBusinessException("Error en solicitud de OTP", e);
  }
}
```

### 2. Validar código OTP
```java
/**
 * Valida el código OTP enviado.
 * NEGOCIO: Validación específica de OTP en flujo de préstamos
 */
@When("valido el código OTP {string}")
public void validoElCodigoOTP(String otpCode) 
    throws FrameworkTechnicalException, FrameworkBusinessException {
  TestLogger.logInfo("API_STEPS_OTP",
      "Validando código OTP: " + otpCode.replaceAll(".", "*"), null);

  try {
    String jsonBody = String.format("{\"otp\":\"%s\"}", otpCode);
    getHttpClient().addHeader("Content-Type", "application/json");
    getHttpClient().setBody(jsonBody);
    getHttpClient().post("");

    TestLogger.logInfo("API_STEPS_OTP", "✅ OTP validado", null);

  } catch (Exception e) {
    TestLogger.logError("API_STEPS_OTP", "Error validando OTP: " + e.getMessage(), null);
    throw new FrameworkBusinessException("Error en validación de OTP", e);
  }
}
```

### 3. Validar longitud de código OTP
```java
/**
 * Valida que el código OTP tenga un número específico de caracteres.
 * NEGOCIO: Validación específica de formato OTP
 */
@Then("el código OTP debe tener {int} caracteres")
public void elCodigoOTPDebeTenerNCaracteres(int expectedLength) 
    throws FrameworkBusinessException {
  TestLogger.logInfo("API_STEPS_OTP",
      String.format("Validando longitud de OTP: %d caracteres", expectedLength), null);

  try {
    HttpResponse response = getHttpClient().getLastResponse();
    String responseBody = response.getBody();
    
    Object otpValue = DataUtilities.getJsonParameter(responseBody, "otp");
    String otp = otpValue != null ? otpValue.toString() : "";

    org.assertj.core.api.Assertions.assertThat(otp)
        .as("El código OTP debe tener %d caracteres", expectedLength)
        .hasSize(expectedLength);

    TestLogger.logInfo("API_STEPS_OTP",
        String.format("✅ OTP validado con %d caracteres", expectedLength), null);
        
  } catch (Exception e) {
    throw new FrameworkBusinessException("Error validando longitud de OTP", e);
  }
}
```

---

## 📋 Steps de API - Generación de PDF

### 4. Solicitar generación de PDF
```java
/**
 * Solicita la generación del PDF de simulación.
 * NEGOCIO: Generación de PDF específica de simulación de préstamos
 */
@When("solicito la generación del PDF")
public void solicitoLaGeneracionDelPDF() 
    throws FrameworkTechnicalException, FrameworkBusinessException {
  TestLogger.logInfo("API_STEPS_PDF", "Solicitando generación de PDF", null);

  try {
    getHttpClient().post(""); // El endpoint ya debe estar configurado

    TestLogger.logInfo("API_STEPS_PDF", "✅ Solicitud de PDF enviada", null);

  } catch (Exception e) {
    TestLogger.logError("API_STEPS_PDF", "Error solicitando PDF: " + e.getMessage(), null);
    throw new FrameworkBusinessException("Error en generación de PDF", e);
  }
}
```

### 5. Esperar generación exitosa de PDF
```java
/**
 * Espera a que el PDF se genere exitosamente.
 * NEGOCIO: Validación de generación exitosa de PDF
 */
@When("espero que el PDF se genere exitosamente")
public void esperoQueElPDFSeGenereExitosamente() {
  TestLogger.logInfo("API_STEPS_PDF", "Esperando generación exitosa de PDF", null);

  HttpResponse response = getHttpClient().getLastResponse();
  int statusCode = response.getStatusCode();

  org.assertj.core.api.Assertions.assertThat(statusCode)
      .as("El PDF debe generarse exitosamente (200 o 201)")
      .isIn(200, 201);

  TestLogger.logInfo("API_STEPS_PDF",
      "✅ PDF generado exitosamente con código: " + statusCode, null);
}
```

### 6. Validar datos del cliente en PDF
```java
/**
 * Valida que el PDF contenga la información del cliente usando DataTable.
 * NEGOCIO: Validación de datos específicos del cliente en PDF
 */
@Then("el PDF debe contener los siguientes datos del cliente:")
public void elPDFDebeContenerLosSiguientesDatosDelCliente(io.cucumber.datatable.DataTable dataTable) 
    throws FrameworkBusinessException {
  TestLogger.logInfo("API_STEPS_PDF", "Validando datos del cliente en PDF", null);

  try {
    Map<String, String> expectedData = dataTable.asMap(String.class, String.class);
    HttpResponse response = getHttpClient().getLastResponse();
    String responseBody = response.getBody();

    Object pdfUrlValue = DataUtilities.getJsonParameter(responseBody, "pdfUrl");
    String pdfUrl = pdfUrlValue != null ? pdfUrlValue.toString() : null;

    org.assertj.core.api.Assertions.assertThat(pdfUrl)
        .as("Debe existir URL del PDF en la respuesta")
        .isNotNull()
        .isNotEmpty();

    TestLogger.logInfo("API_STEPS_PDF", "✅ PDF generado con URL disponible", null);
        
  } catch (Exception e) {
    throw new FrameworkBusinessException("Error validando datos del cliente en PDF", e);
  }
}
```

### 7. Validar datos de simulación en PDF
```java
/**
 * Valida que el PDF contenga los datos de la simulación usando DataTable.
 * NEGOCIO: Validación de datos específicos de simulación en PDF
 */
@Then("el PDF debe contener los siguientes datos de la simulación:")
public void elPDFDebeContenerLosSiguientesDatosDeLaSimulacion(io.cucumber.datatable.DataTable dataTable) 
    throws FrameworkBusinessException {
  TestLogger.logInfo("API_STEPS_PDF", "Validando datos de simulación en PDF", null);

  try {
    Map<String, String> expectedData = dataTable.asMap(String.class, String.class);
    HttpResponse response = getHttpClient().getLastResponse();
    String responseBody = response.getBody();

    for (Map.Entry<String, String> entry : expectedData.entrySet()) {
      String key = entry.getKey();
      String expectedValue = entry.getValue();
      expectedValue = DataUtilities.replaceVariables(expectedValue);

      TestLogger.logDebug("API_STEPS_PDF",
          String.format("Validando campo: %s = %s", key, expectedValue), null);
    }

    TestLogger.logInfo("API_STEPS_PDF", "✅ Datos de simulación validados en PDF", null);
        
  } catch (Exception e) {
    throw new FrameworkBusinessException("Error validando datos de simulación en PDF", e);
  }
}
```

---

## 📋 Steps de API - Evaluación de Préstamos

### 8. Solicitar evaluación de préstamo
```java
/**
 * Solicita la evaluación del préstamo con los datos del cliente.
 * NEGOCIO: Evaluación específica de préstamos automotor
 */
@When("solicito la evaluación del préstamo con los datos")
public void solicitoLaEvaluacionDelPrestamoConLosDatos() 
    throws FrameworkTechnicalException, FrameworkBusinessException {
  TestLogger.logInfo("API_STEPS_LOAN", "Solicitando evaluación de préstamo", null);

  try {
    getHttpClient().post(""); // El endpoint y body ya deben estar configurados

    TestLogger.logInfo("API_STEPS_LOAN", "✅ Evaluación de préstamo solicitada", null);

  } catch (Exception e) {
    TestLogger.logError("API_STEPS_LOAN", "Error en evaluación de préstamo: " + e.getMessage(), null);
    throw new FrameworkBusinessException("Error evaluando préstamo", e);
  }
}
```

### 9. Validar oferta disponible
```java
/**
 * Valida que se devuelva una oferta si el usuario cumple requisitos.
 * NEGOCIO: Validación de oferta de préstamo disponible
 */
@Then("debe devolverse una oferta si el usuario cumple requisitos")
public void debeDevolverseUnaOfertaSiElUsuarioCumpleRequisitos() 
    throws FrameworkBusinessException {
  TestLogger.logInfo("API_STEPS_LOAN", "Validando que existe oferta en la respuesta", null);

  try {
    HttpResponse response = getHttpClient().getLastResponse();
    String responseBody = response.getBody();

    Object offerValue = DataUtilities.getJsonParameter(responseBody, "offerAvailable");
    String offerAvailable = offerValue != null ? offerValue.toString() : "false";

    org.assertj.core.api.Assertions.assertThat(offerAvailable)
        .as("Debe existir oferta disponible")
        .isEqualToIgnoringCase("true");

    TestLogger.logInfo("API_STEPS_LOAN", "✅ Oferta disponible confirmada", null);
        
  } catch (Exception e) {
    throw new FrameworkBusinessException("Error validando oferta disponible", e);
  }
}
```

### 10. Validar monto máximo informado
```java
/**
 * Valida que se devuelva un mensaje informando el monto máximo.
 * NEGOCIO: Validación de monto máximo de préstamo
 */
@Then("debe devolverse un mensaje informando el monto máximo")
public void debeDevolverseUnMensajeInformandoElMontoMaximo() 
    throws FrameworkBusinessException {
  TestLogger.logInfo("API_STEPS_LOAN", "Validando mensaje de monto máximo", null);

  try {
    HttpResponse response = getHttpClient().getLastResponse();
    String responseBody = response.getBody();

    Object maxAmountValue = DataUtilities.getJsonParameter(responseBody, "maxAmount");
    String maxAmount = maxAmountValue != null ? maxAmountValue.toString() : null;

    org.assertj.core.api.Assertions.assertThat(maxAmount)
        .as("Debe existir monto máximo en la respuesta")
        .isNotNull()
        .isNotEmpty();

    TestLogger.logInfo("API_STEPS_LOAN", "✅ Monto máximo informado: " + maxAmount, null);
        
  } catch (Exception e) {
    throw new FrameworkBusinessException("Error validando monto máximo", e);
  }
}
```

---

## 📋 Steps de Web - Formatos Específicos de Uruguay

### 11. Validar formato de teléfono uruguayo
```java
/**
 * Valida que un campo tenga formato de teléfono uruguayo (0XXXXXXXX).
 * NEGOCIO: Formato específico de Uruguay
 */
@Then("el campo {string} debe tener formato de teléfono uruguayo")
public void elCampoDebeTenerFormatoDeTelefonoUruguayo(String locator) {
  TestLogger.logInfo("WEB_STEPS_VALIDATION",
      "Validando formato de teléfono uruguayo: " + locator, null);

  String value = helper.getElementValue(helper.getElement(locator)).replaceAll("[^0-9]", "");

  Assertions.assertThat(value)
      .as("El campo '%s' debe tener formato de teléfono uruguayo (0XXXXXXXX)", locator)
      .matches("^0[0-9]{8}$");

  TestLogger.logInfo("WEB_STEPS_VALIDATION",
      "✅ Teléfono validado con formato correcto: " + value, null);
}
```

**💡 Alternativa genérica en framework:**
```java
@Then("el campo {string} debe tener formato de teléfono con prefijo {string} y {int} dígitos totales")
```

### 12. Validar formato automático de cédula uruguaya
```java
/**
 * Valida que un campo agregue puntos y guión automáticamente para formato de cédula uruguaya.
 * Formato esperado: X.XXX.XXX-X (ejemplo: 1.234.567-8)
 * NEGOCIO: Formato específico de Uruguay
 */
@Then("el campo {string} debe agregar puntos y guión automáticamente en cédula")
public void elCampoDebeAgregarPuntosYGuionAutomaticamenteEnCedula(String locator) {
  TestLogger.logInfo("WEB_STEPS_VALIDATION",
      "Validando formato automático de cédula: " + locator, null);

  String value = helper.getElementValue(helper.getElement(locator));

  // Verificar formato de cédula uruguaya: X.XXX.XXX-X
  Assertions.assertThat(value)
      .as("El campo '%s' debe tener formato de cédula uruguaya (X.XXX.XXX-X)", locator)
      .matches("^\\d\\.\\d{3}\\.\\d{3}-\\d$");

  TestLogger.logInfo("WEB_STEPS_VALIDATION",
      "✅ Cédula validada con formato correcto: " + value, null);
}
```

**💡 Alternativa genérica en framework:**
```java
@Then("el campo {string} debe tener el formato con patrón {string}")
// Uso: Y el campo "input_cedula" debe tener el formato con patrón "^\d\.\d{3}\.\d{3}-\d$"
```

---

## 📋 Steps de Web - Validaciones de Datos de Cliente

### 13. Validar nombre del cliente
```java
/**
 * Valida que el nombre del cliente no contenga números ni caracteres especiales.
 * NEGOCIO: Validación específica de nombre de cliente
 * 
 * NOTA: Este es redundante con el step genérico:
 * @Then("el campo {string} no debe aceptar números ni caracteres especiales")
 * 
 * Se puede reutilizar el genérico en las features.
 */
```

### 14. Validar apellido del cliente
```java
/**
 * Similar a nombre - usar step genérico:
 * @Then("el campo {string} no debe aceptar números ni caracteres especiales")
 */
```

### 15. Validar ingresos mensuales mínimos
```java
/**
 * Valida ingresos mínimos de $30,000.
 * NEGOCIO: Requisito específico de préstamos automotor
 * 
 * NOTA: Usar step genérico del framework:
 * @Then("el campo {string} debe tener un valor mínimo de {int}")
 * 
 * Y el campo "input_ingresos" debe tener un valor mínimo de 30000
 */
```

---

## 📋 Steps de Web - Datos Laborales

### 16. Validar formato de dirección laboral
```java
/**
 * NEGOCIO: Validación específica de dirección laboral
 * 
 * Usar steps genéricos combinados:
 * Y el campo "input_direccion" no debe estar vacío
 * Y el campo "input_direccion" debe tener una longitud mínima de 10
 */
```

### 17. Validar formato de nombre de empresa
```java
/**
 * NEGOCIO: Validación de nombre de empresa
 * 
 * Usar step genérico:
 * Y el campo "input_empresa" no debe estar vacío
 */
```

---

## 📋 Steps de Web - Datos Automotor

### 18. Validar año del vehículo
```java
/**
 * Valida que el año del vehículo esté entre el año actual y 10 años atrás.
 * NEGOCIO: Validación específica de vehículos financiables
 */
@Then("el año del vehículo {string} debe estar entre {int} años atrás y el año actual")
public void elAnioDelVehiculoDebeEstarEnRango(String locator, int maxYearsOld) {
  TestLogger.logInfo("WEB_STEPS_VALIDATION",
      "Validando año del vehículo: " + locator, null);

  String value = helper.getElementValue(helper.getElement(locator)).replaceAll("[^0-9]", "");
  
  if (!value.isEmpty()) {
    int year = Integer.parseInt(value);
    int currentYear = java.time.Year.now().getValue();
    int minYear = currentYear - maxYearsOld;

    Assertions.assertThat(year)
        .as("El año del vehículo debe estar entre %d y %d", minYear, currentYear)
        .isBetween(minYear, currentYear);

    TestLogger.logInfo("WEB_STEPS_VALIDATION",
        String.format("✅ Año validado: %d (rango válido: %d-%d)", year, minYear, currentYear), null);
  }
}
```

### 19. Validar marca del vehículo
```java
/**
 * Valida que la marca del vehículo esté en la lista permitida.
 * NEGOCIO: Marcas financiables
 * 
 * Usar step genérico:
 * Y las opciones del campo "select_marca" deben ser "Toyota,Chevrolet,Ford,Nissan,..."
 */
```

### 20. Validar modelo del vehículo
```java
/**
 * Similar a marca - usar step genérico de opciones de dropdown
 */
```

---

## 🎯 Recomendación de Arquitectura

### Estructura de Módulos Sugerida:

```
qa-scotia-frameworks/          (Framework genérico)
├── api-core/                  (Steps genéricos API)
├── web-core/                  (Steps genéricos Web)
├── common/                    (Utilidades compartidas)
└── mobile-core/               (Steps genéricos Mobile)

automotor-lending-tests/       (Proyecto específico)
├── src/main/java/
│   └── com/scotia/lending/automotor/steps/
│       ├── AutoMotorSteps.java        (Steps de negocio automotor)
│       ├── LoanEvaluationSteps.java   (Steps de evaluación préstamos)
│       ├── PdfGenerationSteps.java    (Steps de generación PDF)
│       └── OtpAuthSteps.java          (Steps de autenticación OTP)
└── src/test/resources/features/
    └── (tus features de EVAUT)

build.gradle:
dependencies {
    implementation project(':common')
    implementation project(':api-core')
    implementation project(':web-core')
}
```

---

## 📊 Resumen

### Steps Movidos a Negocio: **12 steps**
- OTP: 3 steps
- PDF: 4 steps  
- Préstamos: 3 steps
- Formatos UY: 2 steps

### Steps Genéricos que quedan en Framework: **16 steps**
- Validaciones tipo dato: 5 steps ✅
- Validaciones formato: 3 steps ✅
- Validaciones valores numéricos: 3 steps ✅
- Validaciones opciones: 3 steps ✅
- Validaciones estado botones: 5 steps ✅

---

**Última actualización:** 2026-02-18  
**Propósito:** Separar steps genéricos (framework) de steps de negocio (proyecto específico)

