pipeline {
  agent any

  parameters {
    booleanParam(name: 'DEPLOY_TO_MINIKUBE', defaultValue: false, description: 'Deploy to local Minikube using Helm')
    choice(name: 'TARGET_ENV', choices: ['auto', 'dev', 'test', 'prod'], description: 'Deployment environment (auto maps from branch)')
    booleanParam(name: 'REQUIRE_PROD_APPROVAL', defaultValue: true, description: 'Require manual approval before production deployment')
    string(name: 'HELM_RELEASE', defaultValue: 'banking-platform', description: 'Helm release name')
    string(name: 'HELM_NAMESPACE', defaultValue: 'banking', description: 'Kubernetes namespace')
    string(name: 'HELM_CHART_PATH', defaultValue: 'deploy/helm/banking-platform', description: 'Path to Helm chart in repo')
  }

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
    DEPLOY_ENV = ""
    DEPLOY_NAMESPACE = ""
    HELM_VALUES_FILE = ""
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

    stage('Resolve Deployment Environment') {
      when {
        expression { return params.DEPLOY_TO_MINIKUBE }
      }
      steps {
        script {
          def rawBranch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: "unknown"
          def branch = rawBranch.replaceFirst(/^origin\\//, '')
          def target = params.TARGET_ENV

          if (target == 'auto') {
            if (branch == 'main' || branch == 'master') {
              target = 'prod'
            } else if (branch.startsWith('release/') || branch.startsWith('qa/')) {
              target = 'test'
            } else {
              target = 'dev'
            }
          }

          env.DEPLOY_ENV = target
          env.DEPLOY_NAMESPACE = "${params.HELM_NAMESPACE}-${env.DEPLOY_ENV}"
          env.HELM_VALUES_FILE = "${params.HELM_CHART_PATH}/environments/${env.DEPLOY_ENV}-values.yaml"

          echo "Branch: ${branch}"
          echo "Resolved deployment environment: ${env.DEPLOY_ENV}"
          echo "Resolved namespace: ${env.DEPLOY_NAMESPACE}"
          echo "Helm values file: ${env.HELM_VALUES_FILE}"
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

    stage('Kubernetes Precheck') {
      when {
        expression { return params.DEPLOY_TO_MINIKUBE }
      }
      steps {
        sh '''
          set -euo pipefail
          command -v kubectl
          command -v helm
          command -v minikube
          minikube status
          kubectl config use-context minikube
          kubectl get nodes
        '''
      }
    }

    stage('Production Guardrails') {
      when {
        allOf {
          expression { return params.DEPLOY_TO_MINIKUBE }
          expression { return env.DEPLOY_ENV == 'prod' }
        }
      }
      steps {
        script {
          def rawBranch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: "unknown"
          def branch = rawBranch.replaceFirst(/^origin\\//, '')
          if (!(branch == 'main' || branch == 'master')) {
            error("Production deployments are allowed only from main/master. Current branch: ${branch}")
          }
          echo "Production branch guardrail passed on branch: ${branch}"
          echo "Code review enforcement must be configured in GitHub branch protection (required PR approvals)."
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

    stage('Helm Deploy to Minikube (Optional)') {
      when {
        allOf {
          expression { return params.DEPLOY_TO_MINIKUBE }
          expression { return fileExists(params.HELM_CHART_PATH) }
        }
      }
      steps {
        script {
          def rawBranch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: "unknown"
          def branch = rawBranch.replaceFirst(/^origin\\//, '')
          def resolvedEnv = params.TARGET_ENV
          if (resolvedEnv == 'auto') {
            if (branch == 'main' || branch == 'master') {
              resolvedEnv = 'prod'
            } else if (branch.startsWith('release/') || branch.startsWith('qa/')) {
              resolvedEnv = 'test'
            } else {
              resolvedEnv = 'dev'
            }
          }
          if (resolvedEnv == 'prod' && params.REQUIRE_PROD_APPROVAL) {
            input message: "Approve production deployment to namespace ${params.HELM_NAMESPACE}-${resolvedEnv}?", ok: 'Deploy'
          }
        }
        sh '''
          set -euo pipefail
          BRANCH="${BRANCH_NAME:-${GIT_BRANCH:-unknown}}"
          BRANCH="${BRANCH#origin/}"
          DEPLOY_ENV="${TARGET_ENV}"
          if [ "${DEPLOY_ENV}" = "auto" ]; then
            if [ "${BRANCH}" = "main" ] || [ "${BRANCH}" = "master" ]; then
              DEPLOY_ENV="prod"
            elif [[ "${BRANCH}" == release/* ]] || [[ "${BRANCH}" == qa/* ]]; then
              DEPLOY_ENV="test"
            else
              DEPLOY_ENV="dev"
            fi
          fi
          DEPLOY_NAMESPACE="${HELM_NAMESPACE}-${DEPLOY_ENV}"
          HELM_VALUES_FILE="${HELM_CHART_PATH}/environments/${DEPLOY_ENV}-values.yaml"

          kubectl config use-context minikube
          kubectl get namespace "${DEPLOY_NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${DEPLOY_NAMESPACE}"
          helm lint "${HELM_CHART_PATH}"
          HELM_EXTRA_ARGS=""
          if [ -f "${HELM_VALUES_FILE}" ]; then
            HELM_EXTRA_ARGS="-f ${HELM_VALUES_FILE}"
          fi
          if ! helm upgrade --install "${HELM_RELEASE}" "${HELM_CHART_PATH}" \
            ${HELM_EXTRA_ARGS} \
            --namespace "${DEPLOY_NAMESPACE}" \
            --wait --timeout 5m; then
            echo "Helm deploy failed. Collecting diagnostics..."
            kubectl get all -n "${DEPLOY_NAMESPACE}" || true
            kubectl describe deploy "${HELM_RELEASE}" -n "${DEPLOY_NAMESPACE}" || true
            kubectl get events -n "${DEPLOY_NAMESPACE}" --sort-by=.metadata.creationTimestamp | tail -n 40 || true
            exit 1
          fi
          kubectl get pods -n "${DEPLOY_NAMESPACE}"
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
