CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Backfill field_hash to canonical SHA-256 for legacy MetaSetVersion rows.
UPDATE core_meta_set_version
SET field_hash = encode(digest(field_data::text, 'sha256'), 'hex')
WHERE field_data IS NOT NULL
  AND (field_hash IS NULL OR field_hash !~ '^[0-9a-f]{64}$');

-- Remove legacy columns no longer used by the code model.
ALTER TABLE core_meta_set_version
    DROP COLUMN IF EXISTS api_config;

ALTER TABLE core_meta_set_api_setting
    DROP COLUMN IF EXISTS meta_set_id,
    DROP COLUMN IF EXISTS auth_type,
    DROP COLUMN IF EXISTS username,
    DROP COLUMN IF EXISTS password,
    DROP COLUMN IF EXISTS bearer_token,
    DROP COLUMN IF EXISTS api_key_name,
    DROP COLUMN IF EXISTS api_key_value,
    DROP COLUMN IF EXISTS api_key_placement,
    DROP COLUMN IF EXISTS headers_json,
    DROP COLUMN IF EXISTS timeout_ms;

ALTER TABLE core_meta_set_operation
    DROP COLUMN IF EXISTS meta_set_id,
    DROP COLUMN IF EXISTS method,
    DROP COLUMN IF EXISTS path,
    DROP COLUMN IF EXISTS response_mode,
    DROP COLUMN IF EXISTS description,
    DROP COLUMN IF EXISTS enabled;
