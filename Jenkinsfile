pipeline {
  agent any

  parameters {
    booleanParam(name: 'DEPLOY_TO_MINIKUBE', defaultValue: false, description: 'Deploy to local Minikube using Helm')
    choice(name: 'TARGET_ENV', choices: ['auto', 'dev', 'test', 'prod'], description: 'Deployment environment (auto maps from branch)')
    booleanParam(name: 'REQUIRE_PROD_APPROVAL', defaultValue: true, description: 'Require manual approval before production deployment')
    booleanParam(name: 'ENABLE_TRIVY_SCAN', defaultValue: true, description: 'Run Trivy image scan for deployed workloads (if Trivy is installed)')
    booleanParam(name: 'CREATE_RELEASE_TAG', defaultValue: false, description: 'Create and push a git release tag after successful deployment')
    string(name: 'RELEASE_TAG_PREFIX', defaultValue: 'release', description: 'Prefix for release tag (example: release)')
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
            target = 'dev,test'
          }

          env.DEPLOY_ENV = target
          env.DEPLOY_NAMESPACE = "n/a"
          env.HELM_VALUES_FILE = "n/a"

          echo "Branch: ${branch}"
          echo "Resolved deployment target(s): ${env.DEPLOY_ENV}"
          echo "Namespace/values are resolved per target environment during deploy stage."
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
          expression { return params.TARGET_ENV == 'prod' }
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
          if (params.TARGET_ENV == 'prod' && params.REQUIRE_PROD_APPROVAL) {
            input message: "Approve production deployment to namespace ${params.HELM_NAMESPACE}-prod?", ok: 'Deploy'
          }
        }
        sh '''
          set -euo pipefail
          kubectl config use-context minikube
          helm lint "${HELM_CHART_PATH}"
          if [ "${TARGET_ENV}" = "auto" ]; then
            DEPLOY_TARGETS="dev test"
          else
            DEPLOY_TARGETS="${TARGET_ENV}"
          fi

          for DEPLOY_ENV in ${DEPLOY_TARGETS}; do
            DEPLOY_NAMESPACE="${HELM_NAMESPACE}-${DEPLOY_ENV}"
            HELM_VALUES_FILE="${HELM_CHART_PATH}/environments/${DEPLOY_ENV}-values.yaml"
            HELM_EXTRA_ARGS=""
            if [ -f "${HELM_VALUES_FILE}" ]; then
              HELM_EXTRA_ARGS="-f ${HELM_VALUES_FILE}"
            fi

            echo "Deploying ${HELM_RELEASE} to ${DEPLOY_NAMESPACE}"
            kubectl get namespace "${DEPLOY_NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${DEPLOY_NAMESPACE}"
            if ! helm upgrade --install "${HELM_RELEASE}" "${HELM_CHART_PATH}" \
              ${HELM_EXTRA_ARGS} \
              --namespace "${DEPLOY_NAMESPACE}" \
              --wait --timeout 5m; then
              echo "Helm deploy failed in ${DEPLOY_NAMESPACE}. Collecting diagnostics..."
              kubectl get all -n "${DEPLOY_NAMESPACE}" || true
              kubectl describe deploy "${HELM_RELEASE}" -n "${DEPLOY_NAMESPACE}" || true
              kubectl get events -n "${DEPLOY_NAMESPACE}" --sort-by=.metadata.creationTimestamp | tail -n 40 || true
              exit 1
            fi
            kubectl get pods -n "${DEPLOY_NAMESPACE}"
          done
        '''
      }
    }

    stage('Kubernetes Smoke Test (Optional)') {
      when {
        allOf {
          expression { return params.DEPLOY_TO_MINIKUBE }
          expression { return fileExists(params.HELM_CHART_PATH) }
        }
      }
      steps {
        sh '''
          set -euo pipefail
          kubectl config use-context minikube
          if [ "${TARGET_ENV}" = "auto" ]; then
            DEPLOY_TARGETS="dev test"
          else
            DEPLOY_TARGETS="${TARGET_ENV}"
          fi

          for DEPLOY_ENV in ${DEPLOY_TARGETS}; do
            DEPLOY_NAMESPACE="${HELM_NAMESPACE}-${DEPLOY_ENV}"
            echo "Running smoke test in ${DEPLOY_NAMESPACE}"
            kubectl rollout status deployment/"${HELM_RELEASE}" -n "${DEPLOY_NAMESPACE}" --timeout=120s

            ENDPOINT_IP="$(kubectl get endpoints "${HELM_RELEASE}" -n "${DEPLOY_NAMESPACE}" -o jsonpath='{.subsets[0].addresses[0].ip}' || true)"
            test -n "${ENDPOINT_IP}"

            APP_POD="$(kubectl get pods -n "${DEPLOY_NAMESPACE}" -l app.kubernetes.io/instance="${HELM_RELEASE}" -o jsonpath='{.items[0].metadata.name}')"
            test -n "${APP_POD}"
            kubectl exec -n "${DEPLOY_NAMESPACE}" "${APP_POD}" -- sh -c '
              if command -v wget >/dev/null 2>&1; then
                wget -q -O- http://127.0.0.1/ >/dev/null
              elif command -v curl >/dev/null 2>&1; then
                curl -fsS http://127.0.0.1/ >/dev/null
              else
                echo "No curl/wget in container; endpoint check already passed."
              fi
            '
          done
        '''
      }
    }

    stage('Image Security Scan (Trivy Optional)') {
      when {
        allOf {
          expression { return params.DEPLOY_TO_MINIKUBE }
          expression { return params.ENABLE_TRIVY_SCAN }
        }
      }
      steps {
        sh '''
          set -euo pipefail
          if ! command -v trivy >/dev/null 2>&1; then
            echo "Trivy not installed on Jenkins agent. Skipping image scan."
            exit 0
          fi

          if [ "${TARGET_ENV}" = "auto" ]; then
            DEPLOY_TARGETS="dev test"
          else
            DEPLOY_TARGETS="${TARGET_ENV}"
          fi

          kubectl config use-context minikube
          IMAGES_FILE="$(mktemp)"
          for DEPLOY_ENV in ${DEPLOY_TARGETS}; do
            DEPLOY_NAMESPACE="${HELM_NAMESPACE}-${DEPLOY_ENV}"
            kubectl get pods \
              -n "${DEPLOY_NAMESPACE}" \
              -l app.kubernetes.io/instance="${HELM_RELEASE}" \
              -o jsonpath='{range .items[*].spec.containers[*]}{.image}{"\n"}{end}' 2>/dev/null >> "${IMAGES_FILE}" || true
          done

          sort -u "${IMAGES_FILE}" | sed '/^$/d' > "${IMAGES_FILE}.uniq"
          if [ ! -s "${IMAGES_FILE}.uniq" ]; then
            echo "No deployed container images found for Trivy scan."
            exit 0
          fi

          while IFS= read -r image; do
            echo "Scanning image with Trivy: ${image}"
            trivy image --severity HIGH,CRITICAL --exit-code 1 --no-progress "${image}"
          done < "${IMAGES_FILE}.uniq"
        '''
      }
    }

    stage('Create Release Tag (Optional)') {
      when {
        allOf {
          expression { return params.CREATE_RELEASE_TAG }
          expression { return params.DEPLOY_TO_MINIKUBE }
          expression { return params.TARGET_ENV == 'prod' }
        }
      }
      steps {
        sh '''
          set -euo pipefail
          COMMIT_SHA="$(git rev-parse --short HEAD)"
          TAG_NAME="${RELEASE_TAG_PREFIX}-${BUILD_NUMBER}-${COMMIT_SHA}"
          git tag -a "${TAG_NAME}" -m "Release ${TAG_NAME}"
          git push origin "${TAG_NAME}"
          echo "Created and pushed tag: ${TAG_NAME}"
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
