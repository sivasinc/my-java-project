#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"

if [ -f "${ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-bank-postgres}"
POSTGRES_USER="${POSTGRES_USER:-bank_admin}"
BACKUP_DIR="${PROJECT_ROOT}/backups/postgres"
mkdir -p "${BACKUP_DIR}"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="${BACKUP_DIR}/banking_platform_${TIMESTAMP}.sql.gz"

echo "Creating PostgreSQL backup: ${OUT_FILE}"
docker exec "${POSTGRES_CONTAINER}" pg_dumpall -U "${POSTGRES_USER}" | gzip > "${OUT_FILE}"

echo "Backup completed: ${OUT_FILE}"
