pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    skipDefaultCheckout(false)
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
    timeout(time: 60, unit: 'MINUTES')
  }

  environment {
    MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository -Djava.awt.headless=true'
    JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
    PATH = "/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/bin:/bin:${env.PATH}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        sh 'git status --short || true'
      }
    }

    stage('Toolchain Check') {
      steps {
        sh '''
          set -euo pipefail
          DOCKER_BIN="$(command -v docker || true)"
          if [ -z "$DOCKER_BIN" ] && [ -x /usr/local/bin/docker ]; then
            DOCKER_BIN=/usr/local/bin/docker
          fi
          if [ -z "$DOCKER_BIN" ] && [ -x /Applications/Docker.app/Contents/Resources/bin/docker ]; then
            DOCKER_BIN=/Applications/Docker.app/Contents/Resources/bin/docker
          fi
          test -n "$DOCKER_BIN"
          echo "Using Docker CLI at: $DOCKER_BIN"
          echo "$DOCKER_BIN" > .docker_bin_path

          command -v java
          command -v mvn
          command -v git
          "$DOCKER_BIN" compose version
          java -version
          mvn -version
        '''
      }
    }

    stage('Discover Projects') {
      steps {
        script {
          def raw = sh(
            script: "find . -name pom.xml -not -path './.m2/*' -not -path './target/*' | sed 's#^\\./##'",
            returnStdout: true
          ).trim()

          env.MAVEN_POMS = raw
          env.MAVEN_PROJECT_COUNT = raw ? raw.split('\\n').size().toString() : '0'

          if (env.MAVEN_PROJECT_COUNT == '0') {
            echo 'No Maven projects found yet. Maven stages will be skipped.'
          } else {
            echo "Detected ${env.MAVEN_PROJECT_COUNT} Maven project(s):\\n${env.MAVEN_POMS}"
          }
        }
      }
    }

    stage('Static Validation') {
      parallel {
        stage('Docker Compose Lint') {
          steps {
            sh '''
              set -euo pipefail
              DOCKER_BIN="$(cat .docker_bin_path)"
              "$DOCKER_BIN" compose config >/tmp/compose.rendered.yml
            '''
          }
        }

        stage('YAML Sanity') {
          steps {
            sh '''
              set -e
              test -f docker-compose.yml
              test -f infra/prometheus/prometheus.yml
              test -f infra/grafana/provisioning/datasources/datasource.yml
              test -f infra/grafana/provisioning/dashboards/dashboards.yml
            '''
          }
        }
      }
    }

    stage('Maven Build & Unit Tests') {
      when {
        expression { env.MAVEN_PROJECT_COUNT != '0' }
      }
      steps {
        sh '''
          set -euo pipefail

          while IFS= read -r pom; do
            [ -z "$pom" ] && continue
            dir=$(dirname "$pom")

            echo "---- Running mvn clean verify in $dir ----"
            (cd "$dir" && mvn -B -ntp clean verify)
          done <<EOF_POMS
${MAVEN_POMS}
EOF_POMS
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, keepLongStdio: true, testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml'
          archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/*.jar, **/target/*.war'
        }
      }
    }

    stage('Container Build (Optional)') {
      when {
        allOf {
          expression { env.MAVEN_PROJECT_COUNT != '0' }
          expression { sh(script: "find . -name Dockerfile | grep -q .", returnStatus: true) == 0 }
        }
      }
      steps {
        sh '''
          set -euo pipefail
          DOCKER_BIN="$(cat .docker_bin_path)"

          find . -name Dockerfile | while IFS= read -r df; do
            ctx=$(dirname "$df")
            image_name=$(echo "$ctx" | sed 's#^./##' | tr '/_' '-' | tr '[:upper:]' '[:lower:]')
            [ -z "$image_name" ] && image_name="app"

            echo "---- Building image for $ctx as local/${image_name}:${BUILD_NUMBER} ----"
            "$DOCKER_BIN" build -t "local/${image_name}:${BUILD_NUMBER}" "$ctx"
          done
        '''
      }
    }
  }

  post {
    always {
      echo "Build completed. Maven projects detected: ${env.MAVEN_PROJECT_COUNT}"
      script {
        try {
          cleanWs(deleteDirs: true, disableDeferredWipeout: true)
        } catch (Exception ignored) {
          // Fallback when Workspace Cleanup plugin is not installed.
          deleteDir()
        }
      }
    }
    success {
      echo 'Pipeline succeeded.'
    }
    failure {
      echo 'Pipeline failed. Check stage logs for details.'
    }
  }
}
