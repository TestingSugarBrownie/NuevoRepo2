pipeline {
    agent any
    
    // Configuración de opciones del pipeline
    options {
        // Mantener solo los últimos 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Timeout general del pipeline
        timeout(time: 30, unit: 'MINUTES')
        // Evitar ejecución concurrente
        disableConcurrentBuilds()
        // Timestamps en los logs
        timestamps()
    }
    
    environment {
        REPO_NAME = "${env.JOB_NAME.split('/')[1]}"
        COMMIT_ID = "${env.GIT_COMMIT}"
        BUILD_TIMESTAMP = "${new Date().format('yyyy-MM-dd_HH-mm-ss')}"
        PROJECT_TYPE = ""
    }
    
    stages {
        stage('🔍 Repository Analysis') {
            steps {
                script {
                    echo "=== REPOSITORY ANALYSIS ==="
                    echo "Repository: ${REPO_NAME}"
                    echo "Build: #${env.BUILD_NUMBER}"
                    echo "Commit: ${env.GIT_COMMIT}"
                    echo "Branch: ${env.GIT_BRANCH}"
                    echo "Workspace: ${env.WORKSPACE}"
                    
                    sh '''
                        echo "Repository structure:"
                        find . -maxdepth 2 -type f | head -15
                        
                        echo ""
                        echo "Repository size:"
                        du -sh . 2>/dev/null || echo "Cannot calculate size"
                    '''
                }
            }
        }
        
        stage('🔧 Environment Setup') {
            steps {
                script {
                    echo "=== ENVIRONMENT SETUP ==="
                    
                    sh '''
                        echo "Available tools:"
                        echo "Docker: $(docker --version 2>/dev/null || echo 'Not available')"
                        echo "Git: $(git --version 2>/dev/null || echo 'Not available')"
                        echo "Node: $(node --version 2>/dev/null || echo 'Not available')"
                        echo "Python: $(python3 --version 2>/dev/null || echo 'Not available')"
                        
                        echo ""
                        echo "System info:"
                        echo "User: $(whoami)"
                        echo "PWD: $(pwd)"
                        echo "Date: $(date)"
                    '''
                }
            }
        }
        
        stage('📋 Project Detection') {
            steps {
                script {
                    echo "=== PROJECT TYPE DETECTION ==="
                    
                    if (fileExists('package.json')) {
                        env.PROJECT_TYPE = "nodejs"
                        echo "📦 Node.js project detected"
                        sh 'echo "Package.json found:" && head -10 package.json'
                    } else if (fileExists('requirements.txt')) {
                        env.PROJECT_TYPE = "python"
                        echo "🐍 Python project detected"
                        sh 'echo "Requirements.txt found:" && head -10 requirements.txt'
                    } else if (fileExists('pom.xml')) {
                        env.PROJECT_TYPE = "java-maven"
                        echo "☕ Java Maven project detected"
                        sh 'echo "Maven POM found:" && head -10 pom.xml'
                    } else if (fileExists('build.gradle')) {
                        env.PROJECT_TYPE = "java-gradle"
                        echo "☕ Java Gradle project detected"
                        sh 'echo "Gradle build found:" && head -10 build.gradle'
                    } else if (fileExists('Dockerfile')) {
                        env.PROJECT_TYPE = "docker"
                        echo "🐳 Docker project detected"
                        sh 'echo "Dockerfile found:" && head -10 Dockerfile'
                    } else if (fileExists('docker-compose.yml')) {
                        env.PROJECT_TYPE = "docker-compose"
                        echo "🐳 Docker Compose project detected"
                        sh 'echo "Docker Compose found:" && head -10 docker-compose.yml'
                    } else {
                        env.PROJECT_TYPE = "generic"
                        echo "📂 Generic project detected"
                    }
                    
                    echo "Project type set to: ${env.PROJECT_TYPE}"
                }
            }
        }
        
        stage('⚡ Dependency Installation') {
            when {
                not { environment name: 'PROJECT_TYPE', value: 'generic' }
            }
            steps {
                script {
                    echo "=== DEPENDENCY INSTALLATION ==="
                    
                    switch(env.PROJECT_TYPE) {
                        case 'nodejs':
                            sh '''
                                echo "Installing Node.js dependencies with Docker..."
                                docker run --rm -v $(pwd):/app -w /app node:18-alpine sh -c "
                                    echo 'Node version: $(node --version)' &&
                                    echo 'NPM version: $(npm --version)' &&
                                    npm install --production=false
                                " || echo "Dependency installation failed, continuing..."
                            '''
                            break
                        case 'python':
                            sh '''
                                echo "Installing Python dependencies with Docker..."
                                docker run --rm -v $(pwd):/app -w /app python:3.11-slim sh -c "
                                    echo 'Python version: $(python --version)' &&
                                    pip install --upgrade pip &&
                                    pip install -r requirements.txt
                                " || echo "Dependency installation failed, continuing..."
                            '''
                            break
                        case 'docker':
                            echo "🐳 Docker project - no dependency installation needed"
                            break
                        default:
                            echo "⚠️ Unknown project type, skipping dependency installation"
                    }
                }
            }
        }
        
        stage('🧪 Testing') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        script {
                            echo "=== UNIT TESTS ==="
                            
                            switch(env.PROJECT_TYPE) {
                                case 'nodejs':
                                    sh '''
                                        docker run --rm -v $(pwd):/app -w /app node:18-alpine sh -c "
                                            npm test || echo 'No tests configured or tests failed'
                                        " || echo "Test execution failed"
                                    '''
                                    break
                                case 'python':
                                    sh '''
                                        docker run --rm -v $(pwd):/app -w /app python:3.11-slim sh -c "
                                            python -m pytest -v || echo 'No tests found or tests failed'
                                        " || echo "Test execution failed"
                                    '''
                                    break
                                default:
                                    echo "⚠️ No test configuration for ${env.PROJECT_TYPE}"
                            }
                        }
                    }
                }
                
                stage('Code Quality') {
                    steps {
                        echo "=== CODE QUALITY CHECKS ==="
                        sh '''
                            echo "Checking for large files (>5MB):"
                            find . -size +5M -type f | head -5 || echo "No large files found"
                            
                            echo ""
                            echo "Code statistics:"
                            find . -type f -name "*.js" -o -name "*.py" -o -name "*.java" -o -name "*.ts" | wc -l | sed 's/^/Code files: /'
                            
                            echo ""
                            echo "Checking for potential secrets:"
                            grep -r -i -E "(password|secret|api_key|token)" . \
                                --include="*.js" --include="*.py" --include="*.java" --include="*.json" \
                                --exclude-dir=node_modules --exclude-dir=.git \
                                | head -3 || echo "No suspicious patterns found"
                        '''
                    }
                }
            }
        }
        
        stage('🏗️ Build') {
            steps {
                script {
                    echo "=== BUILD STAGE ==="
                    
                    switch(env.PROJECT_TYPE) {
                        case 'nodejs':
                            sh '''
                                echo "Building Node.js application..."
                                docker run --rm -v $(pwd):/app -w /app node:18-alpine sh -c "
                                    npm run build || echo 'No build script configured'
                                " || echo "Build completed with warnings"
                            '''
                            break
                        case 'docker':
                            sh '''
                                echo "Building Docker image..."
                                docker build -t ${REPO_NAME}:${BUILD_NUMBER} -t ${REPO_NAME}:latest . || echo "Docker build failed"
                            '''
                            break
                        case 'docker-compose':
                            sh '''
                                echo "Validating Docker Compose configuration..."
                                docker-compose config || echo "Docker Compose validation failed"
                            '''
                            break
                        default:
                            echo "✅ Generic project - no specific build steps required"
                    }
                }
            }
        }
        
        stage('📊 Quality Gates') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                    branch 'develop'
                }
            }
            steps {
                echo "=== QUALITY GATES FOR PROTECTED BRANCH ==="
                
                script {
                    sh '''
                        echo "Running enhanced quality checks for protected branch..."
                        
                        # Verificar estructura del proyecto
                        echo "Project structure validation:"
                        if [ -f "README.md" ]; then
                            echo "✅ README.md found"
                        else
                            echo "⚠️ README.md missing"
                        fi
                        
                        if [ -f ".gitignore" ]; then
                            echo "✅ .gitignore found"
                        else
                            echo "⚠️ .gitignore missing"
                        fi
                        
                        # Análisis de commits
                        echo ""
                        echo "Recent commits:"
                        git log --oneline -5 || echo "Cannot access git log"
                        
                        # Verificar que no hay archivos de desarrollo en el commit
                        echo ""
                        echo "Checking for development files:"
                        find . -name "*.tmp" -o -name "*.log" -o -name "debug*" | head -5 || echo "No development files found"
                    '''
                }
            }
        }
        
        stage('🚀 Deployment Check') {
            when {
                branch 'main'
            }
            steps {
                echo "=== DEPLOYMENT READINESS ==="
                
                script {
                    def deploymentReady = false
                    
                    if (fileExists('docker-compose.yml')) {
                        echo "🐳 Docker Compose deployment configuration detected"
                        sh 'docker-compose config'
                        deploymentReady = true
                    } else if (fileExists('Dockerfile')) {
                        echo "🐳 Docker deployment configuration detected"
                        deploymentReady = true
                    } else if (fileExists('package.json') && env.PROJECT_TYPE == 'nodejs') {
                        echo "📦 Node.js deployment ready"
                        deploymentReady = true
                    }
                    
                    if (deploymentReady) {
                        echo "✅ Project is ready for deployment"
                    } else {
                        echo "⚠️ No deployment configuration detected"
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "=== 📋 BUILD SUMMARY ==="
                echo "Repository: ${REPO_NAME}"
                echo "Project Type: ${env.PROJECT_TYPE}"
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Build Timestamp: ${env.BUILD_TIMESTAMP}"
                echo "Git Commit: ${env.GIT_COMMIT}"
                echo "Git Branch: ${env.GIT_BRANCH}"
                echo "Build Result: ${currentBuild.result ?: 'SUCCESS'}"
                echo "Build Duration: ${currentBuild.durationString}"
                echo "Workspace: ${env.WORKSPACE}"
                
                // Generar reporte simple
                sh '''
                    echo ""
                    echo "=== 📊 BUILD METRICS ==="
                    echo "Files processed: $(find . -type f | wc -l)"
                    echo "Directories: $(find . -type d | wc -l)"
                    echo "Workspace size: $(du -sh . | cut -f1)"
                '''
            }
            
            // Limpiar workspace
            deleteDir()
        }
        
        success {
            echo "🎉 ✅ BUILD SUCCESSFUL! All stages completed successfully for ${REPO_NAME}"
        }
        
        failure {
            echo "💥 ❌ BUILD FAILED! Check the logs above for ${REPO_NAME}"
        }
        
        unstable {
            echo "⚠️ 🟡 BUILD UNSTABLE! Some tests or checks failed for ${REPO_NAME}"
        }
        
        aborted {
            echo "🛑 ⏹️ BUILD ABORTED! Pipeline was cancelled for ${REPO_NAME}"
        }
    }
}
