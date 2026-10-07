#!/usr/bin/env bash
# Operational User Creation Helper for GateLMS (PHASE5-AUTH)
# Usage: ./scripts/create_user.sh <username> <password> <displayName> [STUDENT|ADMIN]

set -euo pipefail

USERNAME="${1:-}"
PASSWORD="${2:-}"
DISPLAY_NAME="${3:-}"
ROLE="${4:-STUDENT}"

if [ -z "$USERNAME" ] || [ -z "$PASSWORD" ] || [ -z "$DISPLAY_NAME" ]; then
    echo "Usage: $0 <username> <password> <displayName> [STUDENT|ADMIN]"
    exit 1
fi

# Generate BCrypt hash using python
HASH=$(python3 -c "import bcrypt; print(bcrypt.hashpw('$PASSWORD'.encode('utf-8'), bcrypt.gensalt(10)).decode('utf-8'))" 2>/dev/null || \
      python -c "import bcrypt; print(bcrypt.hashpw('$PASSWORD'.encode('utf-8'), bcrypt.gensalt(10)).decode('utf-8'))" 2>/dev/null || \
      htpasswd -nbBC 10 "" "$PASSWORD" | tr -d ':\n' | sed 's/^://')

if [ -z "$HASH" ]; then
    echo "Error: Failed to generate BCrypt hash. Ensure python with bcrypt or htpasswd is installed."
    exit 1
fi

USER_ID=$(python3 -c "import uuid; print(uuid.uuid4())" 2>/dev/null || python -c "import uuid; print(uuid.uuid4())")

echo "Generated User ID: $USER_ID"
echo "Generated Hash: $HASH"

SQL="INSERT INTO users (id, username, password_hash, display_name, role) VALUES ('$USER_ID', '$USERNAME', '$HASH', '$DISPLAY_NAME', '$ROLE');"

echo "Executing SQL: $SQL"

if command -v psql &>/dev/null; then
    PGHOST="${POSTGRES_HOST:-localhost}"
    PGPORT="${POSTGRES_PORT:-5432}"
    PGDATABASE="${POSTGRES_DB:-gatelms_db}"
    PGUSER="${POSTGRES_USER:-postgres}"
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "$SQL"
    echo "User '$USERNAME' ($ROLE) created successfully."
else
    echo "psql not found in PATH. Run the following statement in your PostgreSQL database:"
    echo "$SQL"
fi
