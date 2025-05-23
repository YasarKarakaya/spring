pipeline {
    agent any

    environment {
        OLLAMA_URL = 'http://ollama:11434/api/generate'
        GITHUB_REPO = 'YasarKarakaya/spring' // GitHub kullanıcı adı / repo adı
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
- Suggestions for improvement.
- Write the line number where the necessary improvements will be made.
- Please write the missing Junit tests as well.
```java
${env.CODE_TO_REVIEW}
```"""

                    def json = groovy.json.JsonOutput.toJson([
                        model: 'codegemma',
                        prompt: prompt,
                        stream: false
                    ])

                    def response = httpRequest(
                        httpMode: 'POST',
                        url: "${env.OLLAMA_URL}",
                        contentType: 'APPLICATION_JSON',
                        requestBody: json
                    )

                    def rawJson = new groovy.json.JsonSlurper().parseText(response.content)
                    env.AI_REVIEW = rawJson.response
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
                        // AI yanıtını JSON escape formatına getir
                        def reviewComment = env.AI_REVIEW
                            .replaceAll('\\\\', '\\\\\\\\') // \ → \\
                            .replaceAll('"', '\\"')         // " → \"
                            .replaceAll('\\{', '\\\\{')     // { → \{
                            .replaceAll('\\}', '\\\\}')     // } → \}
                            .replaceAll('\r', '')
                            .replaceAll('\n', '\\\\n')      // \n → \\n



                        def jsonPayload = """{
                            "body": "${reviewComment}"
                        }"""

                        echo "📤 GitHub yorum payload:\n${jsonPayload}"

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
            echo "✅ Pipeline başarıyla tamamlandı. AI yorumu PR'a yazıldı."
        }
        failure {
            echo "❌ Pipeline başarısız oldu. Logları kontrol et."
        }
    }
}
