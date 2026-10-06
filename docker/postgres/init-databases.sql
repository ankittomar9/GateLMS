-- GateLMS: Database Provisioning Script
-- Note: gatelms_db is already initialized by the POSTGRES_DB environment variable.
-- The official postgres entrypoint executes this script connected directly to gatelms_db.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
