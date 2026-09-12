#!/usr/bin/env bash

set -euo pipefail

readonly BACKUP_BUCKET="${BACKUP_BUCKET:-poolc-db-backup-s3}"
readonly AWS_REGION="${AWS_REGION:-ap-northeast-2}"
readonly AWS_PROFILE="${AWS_PROFILE:-default}"
readonly AWS_BIN="${AWS_BIN:-/usr/local/bin/aws}"
readonly DATABASE_NAME="${DATABASE_NAME:-postgres_kr}"
readonly LOCK_DIRECTORY="${LOCK_DIRECTORY:-/var/lock/poolc}"
readonly LOCK_FILE="${LOCK_FILE:-${LOCK_DIRECTORY}/postgres-backup.lock}"

if [[ "$EUID" -ne 0 ]]; then
  echo "This backup script must run as root." >&2
  exit 1
fi

# Keep the lock outside sticky /tmp. A lock file created by another account in
# /tmp can prevent root's cron job from reopening it on hardened Linux hosts.
install -d -m 700 -o root -g root "$LOCK_DIRECTORY"

exec 9>"$LOCK_FILE"
flock -n 9 || {
  logger -t poolc-db-backup "backup already running; skipped"
  exit 0
}

for command in "$AWS_BIN" pg_dump pg_restore sha256sum; do
  command -v "$command" >/dev/null || {
    logger -t poolc-db-backup "required command is missing: $command"
    exit 1
  }
done

umask 077
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
date_path="$(date -u +%Y/%m/%d)"
dump_file="$(mktemp /tmp/poolc-postgres-${timestamp}-XXXXXX.dump)"
checksum_file="${dump_file}.sha256"

cleanup() {
  rm -f "$dump_file" "$checksum_file"
}
trap cleanup EXIT

sudo -n -u postgres pg_dump \
  --format=custom \
  --compress=6 \
  --dbname="$DATABASE_NAME" > "$dump_file"

pg_restore --list "$dump_file" >/dev/null
sha256sum "$dump_file" > "$checksum_file"

upload_backup() {
  local prefix="$1"
  local base_name="poolc-postgres-${timestamp}"

  "$AWS_BIN" --profile "$AWS_PROFILE" --region "$AWS_REGION" s3 cp \
    "$dump_file" "s3://${BACKUP_BUCKET}/${prefix}/${base_name}.dump"
  "$AWS_BIN" --profile "$AWS_PROFILE" --region "$AWS_REGION" s3 cp \
    "$checksum_file" "s3://${BACKUP_BUCKET}/${prefix}/${base_name}.dump.sha256"
  "$AWS_BIN" --profile "$AWS_PROFILE" --region "$AWS_REGION" s3api head-object \
    --bucket "$BACKUP_BUCKET" \
    --key "${prefix}/${base_name}.dump" >/dev/null
}

upload_backup "postgres/daily/${date_path}"

if [[ "$(date -u +%d)" == "01" ]]; then
  upload_backup "postgres/monthly/$(date -u +%Y/%m)"
fi

logger -t poolc-db-backup "completed backup ${timestamp}"
