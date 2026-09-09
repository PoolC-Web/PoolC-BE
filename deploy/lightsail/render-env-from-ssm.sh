#!/usr/bin/env bash

set -euo pipefail

readonly AWS_BIN="${AWS_BIN:-/usr/local/bin/aws}"
readonly AWS_PROFILE="${AWS_PROFILE:-default}"
readonly AWS_REGION="${AWS_REGION:-ap-northeast-2}"
readonly PARAMETER_PATH="${PARAMETER_PATH:-/poolc/prod/backend/}"
readonly OUTPUT_FILE="${OUTPUT_FILE:-/home/ubuntu/backend/.env.production}"

required_keys=(
  AWS_REGION
  CLUB_WIFI_ALLOWED_IPS
  DB_HOST
  DB_NAME
  DB_PASSWORD
  DB_USER_NAME
  EMAIL_ADDRESS
  EMAIL_PASSWORD
  EXPIRE_LENGTH_IN_MILLISECONDS
  FILE_DIR
  FILE_S3_BUCKET
  FILE_STORAGE
  KUBERNETES_API_KEY
  KAKAO_BOOK_REST_API_KEY
  PROJECT_NAME_HERE_SECRET_KEY
  TEST_DB_NAME
)

command -v "$AWS_BIN" >/dev/null
command -v jq >/dev/null

umask 077
temp_file="$(mktemp "${OUTPUT_FILE}.XXXXXX")"
trap 'rm -f "$temp_file"' EXIT

"$AWS_BIN" --profile "$AWS_PROFILE" ssm get-parameters-by-path \
  --region "$AWS_REGION" \
  --path "$PARAMETER_PATH" \
  --recursive \
  --with-decryption \
  --output json \
  | jq -r '.Parameters[] | "\(.Name | split("/")[-1])=\(.Value)"' \
  | LC_ALL=C sort > "$temp_file"

for key in "${required_keys[@]}"; do
  grep -q "^${key}=" "$temp_file" || {
    echo "missing required SSM parameter: ${PARAMETER_PATH}${key}" >&2
    exit 1
  }
done

install -m 600 "$temp_file" "$OUTPUT_FILE"
