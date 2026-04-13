-- Unified PostgreSQL initialization entrypoint.
-- Recommended usage:
--   PGPASSWORD='postgres' psql -h 127.0.0.1 -U postgres -d platformdb -f database/init.sql

\set ON_ERROR_STOP on

BEGIN;
\ir schema.sql
\ir seed.sql
COMMIT;
