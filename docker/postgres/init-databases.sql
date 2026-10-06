-- GateLMS: Database Provisioning Script
CREATE DATABASE gatelms_db;

\c gatelms_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
