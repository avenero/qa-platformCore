# ============================================================================
# Ejemplos de Jenkinsfile para Scotia QA Framework
# ============================================================================
#
# Este directorio contiene ejemplos de configuración para Jenkins CI/CD
# usando el framework Scotia QA.
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

## 📋 CONTENIDO

- `Jenkinsfile.simple` - Pipeline básico para un módulo
- `Jenkinsfile.multi-module` - Pipeline para múltiples módulos
- `Jenkinsfile.parameterized` - Pipeline con parámetros dinámicos
- `jenkins-vars-example.groovy` - Variables compartidas

## 🚀 USO RÁPIDO

### 1. Pipeline Simple

Copia `Jenkinsfile.simple` a la raíz de tu módulo:

```bash
cp scripts/jenkins/Jenkinsfile.simple /path/to/qa-module-banking/Jenkinsfile
```

### 2. Configurar Credenciales en Jenkins

Ve a: **Jenkins → Credentials → Add Credentials**

Crear las siguientes credenciales:

| ID | Tipo | Descripción |
|----|------|-------------|
| `db-url-qa` | Secret text | URL de BD QA |
| `db-user-qa` | Secret text | Usuario BD QA |
| `db-pass-qa` | Secret text | Password BD QA |
| `api-token-qa` | Secret text | Token API QA |

### 3. Crear Job en Jenkins

1. New Item → Pipeline
2. Configurar:
   - Pipeline script from SCM
   - Git repository URL
   - Script path: `Jenkinsfile`

## 🔧 VARIABLES DE ENTORNO REQUERIDAS

El script `test.sh` soporta las siguientes variables:

```groovy
environment {
    // Obligatorias para tests de BD
    DB_URL = credentials('db-url-qa')
    DB_USER = credentials('db-user-qa')
    DB_PASS = credentials('db-pass-qa')
    
    // Opcionales según el módulo
    TEST_ENV = 'qa'
    API_BASE_URL = 'https://api-qa.example.com'
    API_TOKEN = credentials('api-token-qa')
    WEB_BASE_URL = 'https://web-qa.example.com'
    
    // Auto-detectadas (no necesitan configuración)
    MODULE_NAME = 'banking'  // Se detecta automáticamente
}
```

## 📊 EJEMPLOS DE PIPELINES

### Pipeline Simple (Un Módulo)

```groovy
pipeline {
    agent any
    
    environment {
        TEST_ENV = 'qa'
        DB_URL = credentials('db-url-qa')
        DB_USER = credentials('db-user-qa')
        DB_PASS = credentials('db-pass-qa')
    }
    
    stages {
        stage('Test') {
            steps {
                sh '''
                    cd qa-module-banking
                    ../qa-scotia-frameworks/scripts/run-test.sh
                '''
            }
        }
    }
}
```

### Pipeline Multi-Módulo

```groovy
def modules = ['banking', 'autos', 'cards']

pipeline {
    agent any
    
    stages {
        stage('Test All Modules') {
            parallel {
                modules.each { module ->
                    stage("Test ${module}") {
                        steps {
                            sh """
                                export MODULE_NAME=${module}
                                cd qa-module-${module}
                                ../qa-scotia-frameworks/scripts/run-test.sh
                            """
                        }
                    }
                }
            }
        }
    }
}
```

### Pipeline con Tags de Cucumber

```groovy
pipeline {
    parameters {
        choice(
            name: 'TEST_SUITE',
            choices: ['all', 'smoke', 'regression'],
            description: 'Suite de tests a ejecutar'
        )
    }
    
    stages {
        stage('Test') {
            steps {
                script {
                    def tags = params.TEST_SUITE == 'all' ? '' : "--tags @${params.TEST_SUITE}"
                    sh """
                        cd qa-module-banking
                        ../qa-scotia-frameworks/scripts/run-test.sh ${tags}
                    """
                }
            }
        }
    }
}
```

## 🔒 SEGURIDAD

### ✅ BUENAS PRÁCTICAS

1. **Nunca** commitear credenciales en el código
2. **Siempre** usar Jenkins Credentials para datos sensibles
3. **Validar** que `.env*` esté en `.gitignore`
4. **Rotar** credenciales regularmente

### ⚠️ EVITAR

```groovy
// ❌ MAL - Credenciales hardcodeadas
environment {
    DB_PASS = 'password123'  // ¡NUNCA HACER ESTO!
}

// ✅ BIEN - Usar credentials binding
environment {
    DB_PASS = credentials('db-pass-qa')
}
```

## 📧 NOTIFICACIONES

### Notificar en Slack

```groovy
post {
    success {
        slackSend(
            color: 'good',
            message: "✅ Tests exitosos: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        )
    }
    failure {
        slackSend(
            color: 'danger',
            message: "❌ Tests fallidos: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        )
    }
}
```

### Notificar por Email

```groovy
post {
    always {
        emailext(
            subject: "Test Results: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: readFile('build/reports/tests/test/index.html'),
            to: 'qa-team@example.com'
        )
    }
}
```

## 🔗 RECURSOS ADICIONALES

- [Documentación Jenkins Pipeline](https://www.jenkins.io/doc/book/pipeline/)
- [Framework Guide](../../documentacion/FRAMEWORK-GUIDE.md)
- [Troubleshooting](../../TROUBLESHOOTING.md)

## 💡 TIPS

1. **Usar agentes específicos**: Define labels para agentes con Java/Gradle instalado
2. **Cachear dependencias**: Usa Gradle build cache para builds más rápidos
3. **Paralelizar**: Ejecuta múltiples módulos en paralelo cuando sea posible
4. **Archivar reportes**: Guarda reportes HTML de Cucumber para revisión

## 🐛 TROUBLESHOOTING

### Problema: "test.sh: Permission denied"

**Solución:**
```groovy
sh 'chmod +x qa-scotia-frameworks/scripts/run-test.sh'
```

### Problema: "Gradle not found"

**Solución:** Usar el wrapper de Gradle:
```groovy
sh './gradlew test'  // En vez de 'gradle test'
```

### Problema: Variables no se cargan

**Solución:** Verificar que las credenciales existen en Jenkins:
```bash
Jenkins → Manage Jenkins → Manage Credentials
```

