pipeline {
    agent any

    environment {
        OLLAMA_URL = 'http://ollama:11434/api/generate'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Detect Changed Java Files') {
            steps {
                script {
                    sh 'git fetch origin main'

                    def changedFiles = sh(script: 'git diff --name-only origin/main...HEAD', returnStdout: true).trim().split('\n')
                    def javaFiles = changedFiles.findAll { it.endsWith('.java') }

                    if (javaFiles.isEmpty()) {
                        echo "🔍 Değişen Java dosyası yok, AI review yapılmayacak."
                        currentBuild.result = 'SUCCESS'
                        return
                    }

                    def allCode = javaFiles.collect { file ->
                        return readFile(file)
                    }.join('\n\n')

                    env.CODE_TO_REVIEW = allCode.take(5000)
                }
            }
        }

        stage('AI Code Review') {
            when {
                expression { return env.CODE_TO_REVIEW?.trim() }
            }
            steps {
                script {
                    def prompt = """
You are a senior Java code reviewer.
Review the following changed Java code and provide:

- Bug detection
- Code smell identification
- Performance feedback
- Suggestions

```java
${env.CODE_TO_REVIEW}
```"""

                    def json = groovy.json.JsonOutput.toJson([
                        model: 'codellama',
                        prompt: prompt,
                        stream: false
                    ])

                    def response = httpRequest(
                        httpMode: 'POST',
                        url: "${env.OLLAMA_URL}",
                        contentType: 'APPLICATION_JSON',
                        requestBody: json
                    )

                    echo "\n🧠 AI Review:\n${response.content}"
                }
            }
        }
    }

    post {
        success {
            echo "✅ AI review tamamlandı."
        }
        failure {
            echo "❌ Pipeline başarısız oldu."
        }
    }
}
