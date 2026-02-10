#!groovy
@Library('pipeline-utils') _

pipeline {
    agent { label 'jslave1'}

    tools {
        jdk 'OpenJDK 21'
        gradle 'Gradle 8.5'
    }

    parameters {
        choice(
            name: 'PUBLISH_TO_ARTIFACTORY',
            choices: ['AUTO', 'YES', 'NO'],
            description: '''Publicar a Artifactory:
• AUTO: Solo si es rama main/master
• YES: Forzar publicación ⚠️
• NO: Solo compilar y testear'''
        )
        string(
            name: 'CUSTOM_VERSION',
            defaultValue: '',
            description: 'Versión personalizada. Ej: 1.2.0'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: '⚠️ Saltar tests'
        )
    }
    // ========================================================================
    // VARIABLES DE ENTORNO
    // ========================================================================
    environment {
        // Artifactory
        ARTIFACTORY_URL = 'https://artifactory.cldevops.chl.bns/artifactory'
        ARTIFACTORY_RELEASE_REPO = 'libs-release-thirdparty'  // ⚠️ Verificar: libs-release-local o libs-release-thirdparty
        ARTIFACTORY_CREDS = credentials('Artifactory')  // ✅ Credencial existente reutilizada
        // Teams
        TEAMS_WEBHOOK = credentials('teams-webhook-qa-framework')  // ⚠️ Crear esta credencial
        // Proyecto
        PROJECT_GROUP = 'com.scotia.qa'
        PROJECT_NAME = 'qa-scotia-frameworks'
        MODULES = 'common,api-core,web-core,mobile-core'
        // Quality Gates
        MIN_CODE_COVERAGE = '70'
        // Runtime
        VERSION = ''
        WILL_PUBLISH = 'false'
    }
    // ========================================================================
    // TRIGGERS
    // ========================================================================
    triggers {
        pollSCM(env.BRANCH_NAME in ['main', 'master'] ? 'H/5 * * * *' : '')
    }
    // ========================================================================
    // OPCIONES
    // ========================================================================
    options {
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout()
        ansiColor('xterm')
    }

    // ========================================================================
    // STAGES
    // ========================================================================
    stages {
        stage('🔽 Checkout') {
            steps {
                script {
                    echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                    echo '📥 Checkout código fuente...'
                    echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                }
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.GIT_COMMIT_MSG = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    env.GIT_AUTHOR = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                    echo "📌 Branch: ${env.BRANCH_NAME}"
                    echo "📌 Commit: ${env.GIT_COMMIT_SHORT}"
                    echo "📌 Autor: ${env.GIT_AUTHOR}"
                }
            }
        }
        stage('🔢 Calcular Versión') {
            steps {
                script {
                    echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                    echo '🔢 Calculando versión...'
                    echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                    def shouldPublish = false
                    if (params.PUBLISH_TO_ARTIFACTORY == 'AUTO') {
                        shouldPublish = (env.BRANCH_NAME in ['main', 'master'])
                        echo "🤖 AUTO: ${shouldPublish ? 'Publicará' : 'NO publicará'}"
                    } else if (params.PUBLISH_TO_ARTIFACTORY == 'YES') {
                        shouldPublish = true
                    } else {
                        shouldPublish = false
                    }
                    env.WILL_PUBLISH = shouldPublish.toString()

                    if (params.CUSTOM_VERSION) {
                        env.VERSION = params.CUSTOM_VERSION
                    } else {
                        def baseVersion = '1.0.0'
                        try {
                            def propsFile = readFile('gradle.properties')
                            def matcher = (propsFile =~ /version=(.+)/)
                            if (matcher.find()) {
                                baseVersion = matcher.group(1).trim()
                            }
                        } catch (Exception e) {
                            echo "⚠️  gradle.properties no encontrado, usando: ${baseVersion}"
                        }
                        try {
                            def gitTag = sh(script: 'git describe --tags --exact-match 2>/dev/null || echo ""', returnStdout: true).trim()
                            if (gitTag) {
                                env.VERSION = gitTag.startsWith('v') ? gitTag.substring(1) : gitTag
                            } else {
                                env.VERSION = baseVersion
                            }
                        } catch (Exception e) {
                            env.VERSION = baseVersion
                        }
                    }
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "📦 VERSIÓN: ${env.VERSION}"
                    echo "🚀 PUBLICAR: ${env.WILL_PUBLISH == 'true' ? 'SÍ' : 'NO'}"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                }
            }
        }
        stage('🔍 Verificar Duplicados') {
            when {
                expression { env.WILL_PUBLISH == 'true' }
            }
            steps {
                script {
                    echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                    echo "🔍 Verificando versión ${env.VERSION} en Artifactory..."
                    echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                    def versionExists = false
                    def modulosExistentes = []
                    env.MODULES.split(',').each { module ->
                        def moduleTrimmed = module.trim()
                        def artifactPath = "${env.PROJECT_GROUP.replace('.', '/')}/${moduleTrimmed}/${env.VERSION}"
                        def checkUrl = "${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_RELEASE_REPO}/${artifactPath}/${moduleTrimmed}-${env.VERSION}.jar"
                        echo "🔎 Verificando: ${moduleTrimmed}..."
                        try {
                            def response = httpRequest(
                                httpMode: 'HEAD',
                                url: checkUrl,
                                authentication: 'Artifactory',  // ✅ ID de credencial existente
                                validResponseCodes: '100:599',
                                quiet: true
                            )
                            if (response.status == 200) {
                                versionExists = true
                                modulosExistentes.add(moduleTrimmed)
                                echo "   ⚠️  ${moduleTrimmed} v${env.VERSION} YA EXISTE"
                            } else {
                                echo "   ✅ ${moduleTrimmed} v${env.VERSION} NO existe"
                            }
                        } catch (Exception e) {
                            echo "   ✅ ${moduleTrimmed} v${env.VERSION} NO existe"
                        }
                    }
                    if (versionExists) {
                        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                        echo '❌ ERROR: VERSIÓN DUPLICADA'
                        echo ''
                        echo 'Módulos existentes:'
                        modulosExistentes.each { m -> echo "   - ${m} v${env.VERSION}" }
                        echo ''
                        echo 'Soluciones:'
                        echo '  1. Incrementar versión en gradle.properties'
                        echo '  2. Crear nuevo tag Git'
                        echo '  3. Usar CUSTOM_VERSION'
                        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                        error("❌ Versión ${env.VERSION} ya existe")
                    } else {
                        echo "✅ Versión ${env.VERSION} disponible"
                    }
                }
            }
        }
        stage('🔍 Verificar Entorno') {
            steps {
                sh '''
                    echo "☕ Java:"
                    java -version
                    echo ""
                    echo "🐘 Gradle:"
                    gradle --version || ./gradlew --version
                '''
            }
        }
        stage('🧹 Limpiar') {
            steps {
                sh 'gradle clean || ./gradlew clean'
            }
        }
        stage('🔨 Compilar') {
            steps {
                script {
                    echo "🔨 Compilando módulos: ${env.MODULES}"
                }
                sh "gradle build -x test -Pversion=${env.VERSION} || ./gradlew build -x test -Pversion=${env.VERSION}"
            }
        }
        stage('🧪 Tests') {
            when {
                expression { params.SKIP_TESTS == false }
            }
            steps {
                sh 'gradle test || ./gradlew test'
            }
            post {
                always {
                    junit(allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml')
                }
            }
        }
        stage('📊 Coverage') {
            when {
                expression { params.SKIP_TESTS == false }
            }
            steps {
                sh 'gradle jacocoTestReport || ./gradlew jacocoTestReport'
                jacoco(
                    execPattern: '**/build/jacoco/*.exec',
                    classPattern: '**/build/classes/java/main',
                    sourcePattern: '**/src/main/java',
                    minimumLineCoverage: env.MIN_CODE_COVERAGE
                )
            }
        }
        stage('🚦 Quality Gate') {
            when {
                expression { env.WILL_PUBLISH == 'true' }
            }
            steps {
                script {
                    if (currentBuild.result == 'FAILURE' || currentBuild.result == 'UNSTABLE') {
                        error('❌ Quality Gate falló')
                    }
                    echo '✅ Quality Gate: PASSED'
                }
            }
        }
        stage('📦 Artefactos') {
            steps {
                sh """
                    gradle jar javadocJar sourcesJar -Pversion=${env.VERSION} || \
                    ./gradlew jar javadocJar sourcesJar -Pversion=${env.VERSION}
                """
            }
            post {
                always {
                    archiveArtifacts(artifacts: '**/build/libs/*.jar', allowEmptyArchive: true, fingerprint: true)
                }
            }
        }
        stage('🚀 Publicar') {
            when {
                expression { env.WILL_PUBLISH == 'true' }
            }
            steps {
                script {
                    echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                    echo "🚀 Publicando versión ${env.VERSION} a Artifactory..."
                    echo "📁 Repositorio: ${env.ARTIFACTORY_RELEASE_REPO}"
                    echo "🌐 URL: ${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_RELEASE_REPO}"
                    echo '⚠️  RELEASE = INMUTABLE (no se puede sobrescribir)'
                    echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
                }
                sh """
                    gradle publish \
                        -Pversion=${env.VERSION} \
                        -PartifactoryUrl=${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_RELEASE_REPO} \
                        -PartifactoryUser=${env.ARTIFACTORY_CREDS_USR} \
                        -PartifactoryPassword=${env.ARTIFACTORY_CREDS_PSW} || \
                    ./gradlew publish \
                        -Pversion=${env.VERSION} \
                        -PartifactoryUrl=${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_RELEASE_REPO} \
                        -PartifactoryUser=${env.ARTIFACTORY_CREDS_USR} \
                        -PartifactoryPassword=${env.ARTIFACTORY_CREDS_PSW}
                """
                script {
                    echo '✅ PUBLICACIÓN EXITOSA'
                    echo "📦 Versión ${env.VERSION} en Artifactory"
                    echo '🔒 Versión INMUTABLE'
                }
            }
        }
    }
    // ========================================================================
    // POST ACTIONS
    // ========================================================================
    post {
        success {
            script {
                echo '✅ BUILD EXITOSO'
                try {
                    def publishStatus = env.WILL_PUBLISH == 'true' ? 'SÍ ✅' : 'NO ⚠️'
                    def publishedInfo = ''

                    if (env.WILL_PUBLISH == 'true') {
                        publishedInfo = "Publicado: ${env.VERSION} (INMUTABLE)"
                    } else {
                        publishedInfo = "NO publicado (rama: ${env.BRANCH_NAME})"
                    }

                    def payload = [
                        '@type': 'MessageCard',
                        '@context': 'https://schema.org/extensions',
                        summary: 'Build Exitoso',
                        themeColor: '00FF00',
                        title: "✅ Build Exitoso - ${env.PROJECT_NAME}",
                        sections: [[
                            activityTitle: "Build #${env.BUILD_NUMBER}",
                            facts: [
                                [name: '📌 Branch', value: env.BRANCH_NAME],
                                [name: '📦 Versión', value: env.VERSION],
                                [name: '🚀 Publicado', value: publishStatus],
                                [name: '👤 Autor', value: env.GIT_AUTHOR],
                                [name: '💬 Commit', value: env.GIT_COMMIT_MSG]
                            ],
                            text: publishedInfo
                        ]],
                        potentialAction: [[
                            '@type': 'OpenUri',
                            name: 'Ver Build',
                            targets: [[os: 'default', uri: env.BUILD_URL]]
                        ]]
                    ]

                    httpRequest(
                        httpMode: 'POST',
                        contentType: 'APPLICATION_JSON',
                        requestBody: groovy.json.JsonOutput.toJson(payload),
                        url: env.TEAMS_WEBHOOK,
                        validResponseCodes: '200:299'
                    )
                    echo '✅ Notificación Teams enviada'
                } catch (Exception e) {
                    echo "⚠️  Teams falló: ${e.message}"
                }
            }
        }
        failure {
            script {
                echo '❌ BUILD FALLIDO'
                try {
                    def payload = [
                        '@type': 'MessageCard',
                        '@context': 'https://schema.org/extensions',
                        summary: 'Build Fallido',
                        themeColor: 'FF0000',
                        title: "❌ Build Fallido - ${env.PROJECT_NAME}",
                        sections: [[
                            activityTitle: "Build #${env.BUILD_NUMBER}",
                            facts: [
                                [name: '📌 Branch', value: env.BRANCH_NAME],
                                [name: '📦 Versión', value: env.VERSION ?: 'N/A'],
                                [name: '👤 Autor', value: env.GIT_AUTHOR ?: 'N/A']
                            ],
                            text: '⚠️ Build falló. Revisar logs.'
                        ]],
                        potentialAction: [[
                            '@type': 'OpenUri',
                            name: 'Ver Logs',
                            targets: [[os: 'default', uri: "${env.BUILD_URL}console"]]
                        ]]
                    ]

                    httpRequest(
                        httpMode: 'POST',
                        contentType: 'APPLICATION_JSON',
                        requestBody: groovy.json.JsonOutput.toJson(payload),
                        url: env.TEAMS_WEBHOOK,
                        validResponseCodes: '200:299'
                    )
                } catch (Exception e) {
                    echo "⚠️  Teams falló: ${e.message}"
                }
            }
        }
        always {
            script {
                echo "⏱️  Duración: ${currentBuild.durationString}"
            }
            // Limpiar workspace como en tu Jenkinsfile existente
            cleanWs()
        }
    }
}
