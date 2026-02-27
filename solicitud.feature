# ==============================================================================
# FEATURE: Loan Application Flow - EVAUT
# Consolidation of: datos-personales.feature + datos-laborales.feature
# Version: 1.0.0
# Date: 2026-02-25
# ==============================================================================
#
# FLOW STRUCTURE (based on design screens):
#   Screen 1 - Welcome              → "Nueva Solicitud" button
#   Screen 2 - Loan Data            → Amount, Down payment, Term
#   Screen 3 - Personal Data        → Name, Surnames, ID, Phone, Email, Income
#   Screen 4 - Employment Data      → Occupation, Seniority, Marital Status, PEP, Scotia salary
#   Screen 4b - Co-debtor Data      → Conditional: shown when Marital Status = Casado / Unión libre
#
# STEPS USED:
#   Web  → WebSteps (web-core)
#   API  → ApiSteps (api-core)
#   DB   → DatabaseConnectionSteps (common)
# ==============================================================================

@web @evaut @solicitud
Feature: Solicitud de Préstamo Automotor - EVAUT
  Given estoy autenticado en el sistema evaluador de préstamos automotores
  When completo el formulario de solicitud de préstamo
  Then el sistema debe validar los datos ingresados y mostrar un resumen de la solicitud

  # ============================================================================
  # SECTION 1: WELCOME SCREEN
  # ============================================================================

  @EVAUT-98 @ui-design @bienvenida
  Scenario: Validar diseño de pantalla de Bienvenida
    Given que navego a la pantalla de bienvenida
    Then el diseño de "pantalla-bienvenida" debe coincidir con Figma
    # STEP MISSING: "el diseño de {string} debe coincidir con Figma"
    # Requires visual comparison tool integration (Percy, Applitools)

  @EVAUT-99 @button-state @bienvenida
  Scenario: Validar botón "Nueva solicitud" activo en pantalla de Bienvenida
    Given que navego a la pantalla de bienvenida
    Then el botón "btn_nueva_solicitud" debe estar activo

  @EVAUT-100 @navigation @bienvenida
  Scenario: Validar que al presionar "Nueva solicitud" se muestra la sección Datos del préstamo
    When presiono el boton "btn_nueva_solicitud"
    Then el elemento "section-datos-prestamo" debe ser visible

  # ============================================================================
  # SECTION 2: LOAN DATA (VEHICLE)
  # ============================================================================

  @EVAUT-104 @EVAUT-105 @ui-design @datos-prestamo
  Scenario Outline: Validar diseño figma de las secciones de datos del préstamo
    When presiono el boton "btn_nueva_solicitud"
    Then el diseño de "<seccion>" debe coincidir con Figma
    # STEP MISSING: "el diseño de {string} debe coincidir con Figma"

    Examples:
      | seccion                 |
      | section-datos-automotor |
      | section-datos-prestamo  |

  # ============================================================================
  # SECTION 3: PERSONAL DATA
  # ============================================================================

  @EVAUT-110 @validation @required-fields @datos-personales
  Scenario: Validar que todos los campos de Datos del Solicitante son requeridos
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    Then el elemento "error-campos-obligatorios" debe ser visible
    And el mensaje "error-campos-obligatorios" debe contener el texto "Todos los campos son obligatorios"

  # --- Campos Nombre y Apellidos: validaciones de tipo de dato ---

  @EVAUT-111 @EVAUT-112 @EVAUT-113 @EVAUT-114 @validation @datos-personales
  Scenario Outline: Validar tipo de dato aceptado en campos de texto del solicitante
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "<texto>" en el elemento "<campo>"
    Then el campo "<campo>" <validacion>

    Examples:
      | campo           | texto          | validacion                                       |
      | input_nombre    | Juan Carlos    | debe aceptar solo letras                         |
      | input_nombre    | Juan123        | no debe aceptar números ni caracteres especiales |
      | input_apellidos | Pérez González | debe aceptar solo letras                         |
      | input_apellidos | Pérez123@      | no debe aceptar números ni caracteres especiales |

  # --- Campo Cédula ---

  @EVAUT-115 @validation @positive @datos-personales @campo-cedula
  Scenario: Validar que el campo "Cédula" acepta solo números
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "12345678" en el elemento "input_cedula"
    Then el campo "input_cedula" debe aceptar solo números

  @EVAUT-116 @validation @format @datos-personales @campo-cedula
  Scenario: Validar que el campo "Cédula" formatea con puntos y guión automáticamente
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "12345678" en el elemento "input_cedula"
    Then el campo "input_cedula" debe tener el formato con patrón "\\d\\.\\d{3}\\.\\d{3}-\\d"
    And el valor formateado debe ser "1.234.567-8"

  # --- Campo Teléfono ---

  @EVAUT-119 @EVAUT-120 @validation @datos-personales @campo-telefono
  Scenario Outline: Validar campo Teléfono acepta números y formato uruguayo
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "099123456" en el elemento "input_telefono"
    Then el campo "input_telefono" <validacion>

    Examples:
      | validacion                                                          |
      | debe aceptar solo números                                           |
      | debe tener formato de teléfono con prefijo "09" y 9 dígitos totales |

  # --- Campo Ingresos Líquidos ---

  @EVAUT-124 @EVAUT-125 @validation @datos-personales @campo-ingresos
  Scenario Outline: Validar campo Ingresos acepta números y agrega separadores de miles
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "45000" en el elemento "input_ingresos"
    Then el campo "input_ingresos" <validacion>

    Examples:
      | validacion                                        |
      | debe aceptar solo números                         |
      | debe agregar separadores de miles automáticamente |

  @EVAUT-125b @validation @format @datos-personales @campo-ingresos
  Scenario: Validar valor formateado con separadores de miles en campo Ingresos
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "45000" en el elemento "input_ingresos"
    Then el valor formateado debe ser "45.000"

  @EVAUT-126 @EVAUT-127 @validation @business-rule @negative @datos-personales @campo-ingresos
  Scenario Outline: Validar mensaje de error cuando Ingresos líquidos es menor a 30.000
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "<monto>" en el elemento "input_ingresos"
    And presiono el boton "btn_continuar"
    Then el campo "input_ingresos" debe tener un valor mínimo de 30000
    And el mensaje "error-ingresos-minimos" debe contener el texto "Los ingresos mínimos del titular para solicitar un préstamo son $30.000"

    Examples:
      | monto |
      | 25000 |
      | 29999 |

  @EVAUT-130 @validation @business-rule @configuration @datos-personales @campo-ingresos
  Scenario: Validar que el mínimo de ingresos viene de configuración y no es hardcode
    Given configuro el endpoint "api.endpoint.config.parametros"
    And agrego el header "Content-Type" con valor "application/json"
    When ejecuto una petición "GET" ""
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que la respuesta contenga el texto "INGRESO_MINIMO"
    # STEP MISSING: step that validates the parameter value is dynamic and not hardcoded "30000"
    # Suggestion: "obtengo el campo {string} del objeto {string} y lo guardo como {string}"
    #             + subsequent validation of the retrieved value

  # ============================================================================
  # SECTION 4: EMPLOYMENT DATA AND MARITAL STATUS
  # ============================================================================

  @EVAUT-131 @validation @occupation @datos-laborales
  Scenario: Validar opciones del campo "Ocupación" son Dependiente e Independiente
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    Then las opciones del campo "select_ocupacion" deben ser "Dependiente,Independiente"
    And el campo "select_ocupacion" debe permitir selección única

  # --- Campo Antigüedad Laboral ---

  @EVAUT-132 @EVAUT-133 @validation @employment @datos-laborales
  Scenario: Validar visibilidad y opciones del campo "Antigüedad laboral"
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    Then el campo "select_antiguedad" debe estar habilitado
    And el campo "select_antiguedad" debe tener 4 opciones
    And las opciones del campo "select_antiguedad" deben ser "Menos de 6 meses,Entre 6 y 12 meses,Entre 12 y 24 meses,Más de 24 meses"

  @EVAUT-134 @validation @business-rule @negative @datos-laborales
  Scenario: Validar mensaje de error al seleccionar "Menos de 6 meses" en Antigüedad laboral
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Menos de 6 meses" en el combobox "select_antiguedad"
    And presiono el boton "btn_continuar"
    Then el mensaje "error-antiguedad-minima" debe contener el texto "No cumple con la antigüedad laboral mínima"

  @EVAUT-135 @EVAUT-136 @validation @business-rule @conditional @datos-laborales
  Scenario Outline: Validar comportamiento de "Entre 6 y 12 meses" según pago de sueldo en Scotiabank
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And selecciono el valor "<pago_sueldo>" en el combobox "checkbox_pago_sueldo_scotia"
    And selecciono el valor "Entre 6 y 12 meses" en el combobox "select_antiguedad"
    And presiono el boton "btn_continuar"
    Then <resultado>

    Examples:
      | pago_sueldo | resultado                                                                                              |
      | Sí          | el elemento "error-antiguedad-minima" no debe ser visible                                              |
      | No          | el mensaje "error-antiguedad-minima" debe contener el texto "No cumple con la antigüedad laboral mínima" |

  # --- Campo Estado Civil ---

  @EVAUT-137 @validation @marital-status @datos-laborales
  Scenario: Validar que las opciones del campo "Estado civil" son correctas
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    Then las opciones del campo "select_estado_civil" deben ser "Soltero,Casado,Divorciado,Viudo,Unión libre"

  @EVAUT-138 @EVAUT-140 @conditional-display @codeudor @datos-laborales
  Scenario Outline: Validar visibilidad de sección codeudor según estado civil seleccionado
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And selecciono el valor "<estado_civil>" en el combobox "select_estado_civil"
    Then el elemento "<elemento>" <visibilidad>

    Examples:
      | estado_civil | elemento                  | visibilidad          |
      | Casado       | section-datos-codeudor    | debe ser visible     |
      | Soltero      | checkbox_agregar_codeudor | no debe ser visible  |

  @EVAUT-138b @validation @marital-status @datos-laborales
  Scenario: Validar que al seleccionar "Casado" el campo codeudor está habilitado
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Casado" en el combobox "select_estado_civil"
    Then el campo "checkbox_agregar_codeudor" debe estar habilitado

  # --- Campo Codeudor ---

  @EVAUT-139 @tooltip @codeudor @datos-laborales
  Scenario: Validar tooltip del campo "¿Querés agregarlo como codeudor?"
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Casado" en el combobox "select_estado_civil"
    Then el campo "checkbox_agregar_codeudor" debe mostrar el tooltip "Si el cliente agrega un codeudor, se suman los dos salarios declarados, por lo que puede mejorar las condiciones de su préstamo"

  @EVAUT-141 @validation @codeudor @datos-laborales
  Scenario: Validar que las opciones del campo "Agregar codeudor" son Sí o No
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Casado" en el combobox "select_estado_civil"
    Then las opciones del campo "checkbox_agregar_codeudor" deben ser "Sí,No"

  @EVAUT-142 @conditional-display @codeudor @datos-laborales
  Scenario: Validar que al seleccionar "Sí" en codeudor se muestran los campos del codeudor
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Casado" en el combobox "select_estado_civil"
    And selecciono el valor "Sí" en el combobox "checkbox_agregar_codeudor"
    Then el elemento "section-datos-codeudor" debe ser visible
    And el elemento "input_nombre_codeudor" debe ser visible
    And el elemento "input_apellidos_codeudor" debe ser visible
    And el elemento "input_cedula_codeudor" debe ser visible
    And el elemento "input_telefono_codeudor" debe ser visible
    And el elemento "input_email_codeudor" debe ser visible
    And el elemento "input_ingresos_codeudor" debe ser visible

  @EVAUT-143 @validation @business-rule @codeudor @datos-laborales
  Scenario: Validar que el codeudor puede declarar ingresos menor a 30.000
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Casado" en el combobox "select_estado_civil"
    And selecciono el valor "Sí" en el combobox "checkbox_agregar_codeudor"
    And ingreso el texto "25000" en el elemento "input_ingresos_codeudor"
    And presiono el boton "btn_continuar"
    Then el elemento "error-ingresos-codeudor" no debe ser visible

  @EVAUT-144 @validation @business-rule @negative @codeudor @datos-laborales
  Scenario: Validar mensaje cuando el ingreso del codeudor es mayor al del titular
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And ingreso el texto "35000" en el elemento "input_ingresos"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Casado" en el combobox "select_estado_civil"
    And selecciono el valor "Sí" en el combobox "checkbox_agregar_codeudor"
    And ingreso el texto "40000" en el elemento "input_ingresos_codeudor"
    And presiono el boton "btn_continuar"
    Then el mensaje "error-ingresos-codeudor" debe contener el texto "El titular del préstamo debe ser el de sueldo mas alto"

  # --- Campo PEP ---

  @EVAUT-150 @EVAUT-152 @validation @pep @datos-laborales
  Scenario: Validar campo PEP visible, habilitado y con opciones correctas
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    Then el campo "checkbox_pep" debe estar habilitado
    And el elemento "checkbox_pep" debe ser visible
    And las opciones del campo "checkbox_pep" deben ser "Sí,No"
    And el campo "checkbox_pep" debe permitir selección única

  @EVAUT-151 @tooltip @pep @datos-laborales
  Scenario: Validar tooltip del campo PEP coincide con Figma
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    Then el campo "checkbox_pep" debe mostrar el tooltip "Persona que ocupa o ha ocupado en los últimos 5 años un cargo público de alto nivel"

  @EVAUT-153 @validation @pep @datos-laborales
  Scenario: Validar que el flujo avanza al seleccionar cualquier opción del campo PEP
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Sí" en el combobox "checkbox_pep"
    And presiono el boton "btn_continuar"
    Then el elemento "error-pep" no debe ser visible

  # --- Campo Pago de Sueldo en Scotiabank ---

  @EVAUT-154 @EVAUT-155 @validation @bank-payment @datos-laborales
  Scenario: Validar campo "Pago de sueldo en Scotiabank" visible y con opciones correctas
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And presiono el boton "btn_continuar"
    Then el elemento "checkbox_pago_sueldo_scotia" debe ser visible
    And las opciones del campo "checkbox_pago_sueldo_scotia" deben ser "Sí,No"

  # ============================================================================
  # SECTION 5: END-TO-END FLOWS (HAPPY PATH)
  # ============================================================================

  @EVAUT-E2E-001 @smoke @happy-path @e2e
  Scenario: Completar formulario de datos personales exitosamente
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "Juan" en el elemento "input_nombre"
    And ingreso el texto "Pérez González" en el elemento "input_apellidos"
    And ingreso el texto "12345678" en el elemento "input_cedula"
    And ingreso el texto "099123456" en el elemento "input_telefono"
    And ingreso el texto "juan.perez@test.com" en el elemento "input_email"
    And ingreso el texto "45000" en el elemento "input_ingresos"
    And presiono el boton "btn_continuar"
    Then el elemento "section-datos-laborales" debe ser visible

  @EVAUT-E2E-002 @EVAUT-E2E-003 @smoke @happy-path @e2e
  Scenario Outline: Completar formulario completo exitosamente con distintos estados civiles
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "Juan" en el elemento "input_nombre"
    And ingreso el texto "Pérez González" en el elemento "input_apellidos"
    And ingreso el texto "12345678" en el elemento "input_cedula"
    And ingreso el texto "099123456" en el elemento "input_telefono"
    And ingreso el texto "juan.perez@test.com" en el elemento "input_email"
    And ingreso el texto "45000" en el elemento "input_ingresos"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Dependiente" en el combobox "select_ocupacion"
    And selecciono el valor "Más de 24 meses" en el combobox "select_antiguedad"
    And selecciono el valor "<estado_civil>" en el combobox "select_estado_civil"
    And selecciono el valor "No" en el combobox "checkbox_pep"
    And selecciono el valor "Sí" en el combobox "checkbox_pago_sueldo_scotia"
    And presiono el boton "btn_continuar"
    Then el elemento "section-resumen-solicitud" debe ser visible

    Examples:
      | estado_civil |
      | Soltero      |
      | Casado       |

  @EVAUT-E2E-003b @smoke @happy-path @codeudor @e2e
  Scenario: Completar formulario con datos del codeudor exitosamente
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "Juan" en el elemento "input_nombre"
    And ingreso el texto "Pérez González" en el elemento "input_apellidos"
    And ingreso el texto "12345678" en el elemento "input_cedula"
    And ingreso el texto "099123456" en el elemento "input_telefono"
    And ingreso el texto "juan.perez@test.com" en el elemento "input_email"
    And ingreso el texto "45000" en el elemento "input_ingresos"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Dependiente" en el combobox "select_ocupacion"
    And selecciono el valor "Más de 24 meses" en el combobox "select_antiguedad"
    And selecciono el valor "Casado" en el combobox "select_estado_civil"
    And selecciono el valor "Sí" en el combobox "checkbox_agregar_codeudor"
    And ingreso el texto "María" en el elemento "input_nombre_codeudor"
    And ingreso el texto "López García" en el elemento "input_apellidos_codeudor"
    And ingreso el texto "87654321" en el elemento "input_cedula_codeudor"
    And ingreso el texto "098765432" en el elemento "input_telefono_codeudor"
    And ingreso el texto "maria.lopez@test.com" en el elemento "input_email_codeudor"
    And ingreso el texto "30000" en el elemento "input_ingresos_codeudor"
    And selecciono el valor "No" en el combobox "checkbox_pep"
    And selecciono el valor "No" en el combobox "checkbox_pago_sueldo_scotia"
    And presiono el boton "btn_continuar"
    Then el elemento "section-resumen-solicitud" debe ser visible

  # ============================================================================
  # SECTION 6: CROSS-PLATFORM SCENARIOS (WEB + API + DB)
  # ============================================================================

  @EVAUT-CROSS-001 @cross-platform @web @api @smoke
  Scenario: Validar que el formulario enviado en UI llega correctamente al backend
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "Juan" en el elemento "input_nombre"
    And ingreso el texto "Pérez González" en el elemento "input_apellidos"
    And ingreso el texto "12345678" en el elemento "input_cedula"
    And ingreso el texto "099123456" en el elemento "input_telefono"
    And ingreso el texto "juan.perez@test.com" en el elemento "input_email"
    And ingreso el texto "45000" en el elemento "input_ingresos"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Dependiente" en el combobox "select_ocupacion"
    And selecciono el valor "Más de 24 meses" en el combobox "select_antiguedad"
    And selecciono el valor "Soltero" en el combobox "select_estado_civil"
    And selecciono el valor "No" en el combobox "checkbox_pep"
    And selecciono el valor "Sí" en el combobox "checkbox_pago_sueldo_scotia"
    And presiono el boton "btn_continuar"
    And guardo texto del elemento "solicitud-id" en variable temporal llamada "solicitudId"
    Given configuro el endpoint "api.endpoint.solicitudes"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el queryparam "solicitudId" con el valor "{{solicitudId}}"
    When ejecuto una petición "GET" ""
    Then valido que el codigo de respuesta del servicio sea 200

  @EVAUT-CROSS-002 @cross-platform @web @database @smoke
  Scenario: Validar que la solicitud creada en UI se persiste correctamente en base de datos
    Given que navego a la pantalla de bienvenida
    When presiono el boton "btn_nueva_solicitud"
    And ingreso el texto "Juan" en el elemento "input_nombre"
    And ingreso el texto "Pérez González" en el elemento "input_apellidos"
    And ingreso el texto "12345678" en el elemento "input_cedula"
    And ingreso el texto "099123456" en el elemento "input_telefono"
    And ingreso el texto "juan.perez@test.com" en el elemento "input_email"
    And ingreso el texto "45000" en el elemento "input_ingresos"
    And presiono el boton "btn_continuar"
    And selecciono el valor "Dependiente" en el combobox "select_ocupacion"
    And selecciono el valor "Más de 24 meses" en el combobox "select_antiguedad"
    And selecciono el valor "Soltero" en el combobox "select_estado_civil"
    And selecciono el valor "No" en el combobox "checkbox_pep"
    And selecciono el valor "Sí" en el combobox "checkbox_pago_sueldo_scotia"
    And presiono el boton "btn_continuar"
    And guardo texto del elemento "solicitud-id" en variable temporal llamada "solicitudId"
    Given establezco conexion a base de datos "sqlserver"
    When ejecuto la consulta "SELECT estado, cedula FROM solicitudes WHERE id = ?" con parametros "{{solicitudId}}"
    Then valido que la consulta retorne resultados
    And valido que la columna "cedula" tenga el valor "12345678"
    And valido que la columna "estado" tenga el valor "PENDIENTE"

  @EVAUT-CROSS-003 @cross-platform @api @database
  Scenario: Validar que los parámetros de configuración del backend coinciden con los validados en UI
    Given configuro el endpoint "api.endpoint.config.parametros"
    And agrego el header "Content-Type" con valor "application/json"
    When ejecuto una petición "GET" ""
    Then valido que el codigo de respuesta del servicio sea 200
    And obtengo el campo "valor" del objeto "INGRESO_MINIMO" y lo guardo como "ingresoMinimoApi"
    Given establezco conexion a base de datos "sqlserver"
    When ejecuto la consulta "SELECT valor FROM parametros_config WHERE clave = ?" con parametros "INGRESO_MINIMO"
    Then valido que la consulta retorne resultados
    And obtengo el valor de la columna "valor" y lo almaceno en "ingresoMinimoBd"
    And valido que la variable "ingresoMinimoApi" sea igual a la variable "ingresoMinimoBd"
