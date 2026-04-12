#!/bin/sh
set -eu

MODE="${MYSQL_INIT_MODE:-full}"
MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-}"

if [ -z "$MYSQL_PWD" ]; then
  echo "[init] MYSQL_ROOT_PASSWORD is empty, aborting import" >&2
  exit 1
fi

import_full() {
  db="$1"
  sql_file="$2"
  echo "[init] importing full data into ${db} from ${sql_file}"
  mysql -uroot "$db" < "$sql_file"
}

import_schema_only() {
  db="$1"
  sql_file="$2"
  echo "[init] importing schema only into ${db} from ${sql_file}"
  # Strip INSERT statements while preserving table/routine definitions.
  sed '/^[[:space:]]*INSERT[[:space:]]\+INTO[[:space:]]/Id' "$sql_file" | mysql -uroot "$db"
}

import_one() {
  db="$1"
  sql_file="$2"

  if [ "$MODE" = "schema" ]; then
    import_schema_only "$db" "$sql_file"
  else
    import_full "$db" "$sql_file"
  fi
}

import_one cozy_user /docker-entrypoint-initdb.d/raw/cozy_user.sql
import_one cozy_member /docker-entrypoint-initdb.d/raw/cozy_member.sql
import_one cozy_order /docker-entrypoint-initdb.d/raw/cozy_order.sql
import_one cozy_mall /docker-entrypoint-initdb.d/raw/cozy_mall.sql

echo "[init] mysql initialization finished, mode=${MODE}"
