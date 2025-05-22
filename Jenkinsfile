pipeline {
    agent any

    environment {
        OLLAMA_URL = 'http://ollama:11434/api/generate'
        GITHUB_REPO = 'YasarKarakaya/spring' // 🔁 GitHub kullanıcı adı / repo adı
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
                    // Default branch'i otomatik bul
                    def defaultBranch = sh(
                        script: "git remote show origin | grep 'HEAD branch' | cut -d ':' -f2 | tr -d ' '",
                        returnStdout: true
                    ).trim()

                    echo "🌿 Default branch: ${defaultBranch}"

                    sh "git fetch origin ${defaultBranch}"

                    def changedFiles = sh(
                        script: "git diff --name-only origin/${defaultBranch}...HEAD",
                        returnStdout: true
                    ).trim().split('\n')

                    def javaFiles = changedFiles.findAll { it.endsWith('.java') }

                    if (javaFiles.isEmpty()) {
                        echo "🔍 Değişen Java dosyası yok. AI review yapılmayacak."
                        currentBuild.result = 'SUCCESS'
                        return
                    }

                    def allCode = javaFiles.collect { file -> readFile(file) }.join('\n\n')
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
Review the following Java code and provide feedback:
- Logical issues
- Bugs
- Code smells
- Suggestions for improvement

```java
${env.CODE_TO_REVIEW}
```"""

                    def json = groovy.json.JsonOutput.toJson([
                        model: 'codegemma', // 🔁 Kullandığın Ollama modeli
                        prompt: prompt,
                        stream: false
                    ])

                    def response = httpRequest(
                        httpMode: 'POST',
                        url: "${env.OLLAMA_URL}",
                        contentType: 'APPLICATION_JSON',
                        requestBody: json
                    )

                    env.AI_REVIEW = response.content
                    echo "\n🧠 AI Review:\n${env.AI_REVIEW}"
                }
            }
        }

        stage('Comment on PR') {
            when {
                expression { return env.CHANGE_ID && env.AI_REVIEW }
            }
            steps {
                withCredentials([string(credentialsId: 'GITHUB_TOKEN', variable: 'GH_TOKEN')]) {
                    script {
                        def reviewComment = env.AI_REVIEW
                            .replaceAll('"', '\\"')
                            .replaceAll('\r', '')
                            .replaceAll('\n', '\\n')

                        def jsonPayload = """{
                            "body": "${reviewComment}"
                        }"""

                        echo "📤 Sending GitHub Comment Payload:\n${jsonPayload}"

                        sh """
                            curl -s -H "Authorization: token ${GH_TOKEN}" \\
                                 -H "Content-Type: application/json" \\
                                 -X POST \\
                                 -d '${jsonPayload}' \\
                                 https://api.github.com/repos/${env.GITHUB_REPO}/issues/${env.CHANGE_ID}/comments
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline başarıyla tamamlandı. PR'a yorum yazıldı."
        }
        failure {
            echo "❌ Pipeline başarısız oldu. Hatalar için logu kontrol et."
        }
    }
}
