package com.qa.common.reporting.core.model;

/**
 * Resultado de un step individual de Cucumber.
 *
 * <p>Representa la ejecución de un paso (Given/When/Then/And/But) dentro de un scenario,
 * incluyendo su estado, duración y metadata relevante.</p>
 *
 * <p><b>Ejemplo:</b></p>
 * <pre>
 * StepResult step = new StepResult();
 * step.setKeyword("Given");
 * step.setName("usuario con cuenta activa");
 * step.setStatus(TestStatus.PASS);
 * step.setDurationMs(150);
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class StepResult {

    // Identificación del step
    private String keyword;           // Given, When, Then, And, But
    private String name;              // Texto completo del step
    private int line;                 // Línea en el archivo .feature

    // Resultado de ejecución
    private TestStatus status;        // PASS, FAIL, SKIP
    private long durationMs;          // Duración en milisegundos
    private long durationNanos;       // Duración en nanosegundos (precisión)

    // Información de error (si aplica)
    private String errorMessage;      // Mensaje de error si falló
    private String stackTrace;        // Stack trace si falló

    // Metadata técnica
    private String location;          // Clase.método que ejecuta el step
    private String[] arguments;       // Argumentos capturados del step

    // =================================================================================
    // CONSTRUCTORES
    // =================================================================================

    public StepResult() {
        this.status = TestStatus.TODO;
    }

    public StepResult(String keyword, String name, TestStatus status) {
        this.keyword = keyword;
        this.name = name;
        this.status = status;
    }

    // =================================================================================
    // MÉTODOS DE UTILIDAD
    // =================================================================================

    /**
     * Obtiene el texto completo del step con keyword.
     * Ejemplo: "Given usuario con cuenta activa"
     */
    public String getFullStepText() {
        return (keyword != null ? keyword + " " : "") + (name != null ? name : "");
    }

    /**
     * Obtiene la duración formateada para humanos.
     * Ejemplos: "150ms", "1.5s", "2m 30s"
     */
    public String getFormattedDuration() {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.2fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    /**
     * Obtiene el emoji del status para visualización.
     */
    public String getStatusEmoji() {
        if (status == null) {
            return "⚪";
        }
        return switch (status) {
            case PASS -> "✅";
            case FAIL -> "❌";
            case SKIP -> "⏭️";
            case TODO -> "⚪";
            case EXECUTING -> "▶️";
        };
    }

    /**
     * Verifica si el step falló.
     */
    public boolean isFailed() {
        return status == TestStatus.FAIL;
    }

    /**
     * Verifica si el step pasó exitosamente.
     */
    public boolean isPassed() {
        return status == TestStatus.PASS;
    }

    /**
     * Verifica si el step fue omitido.
     */
    public boolean isSkipped() {
        return status == TestStatus.SKIP;
    }

    // =================================================================================
    // GETTERS Y SETTERS
    // =================================================================================

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public void setDurationNanos(long durationNanos) {
        this.durationNanos = durationNanos;
        // Calcular millisegundos automáticamente
        this.durationMs = durationNanos / 1_000_000;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String[] getArguments() {
        return arguments;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return String.format("%s %s [%s] (%s)",
                getStatusEmoji(),
                keyword != null ? keyword : "",
                name != null ? name : "",
                getFormattedDuration());
    }
}

