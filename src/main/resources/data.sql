-- Wildcard sec_permission seed for development/MVP.
-- Per starter's PermissionMatrix:
--   ENTITY     uses target='*' for cross-entity wildcard
--   ATTRIBUTE  uses target='ENTITY_NAME.*' per entity (no single global wildcard)
--
-- Idempotent via ON CONFLICT — Spring Boot data.sql may re-run on devtools restart.

-- Entity-level wildcards
INSERT INTO sec_permission (id, authority_name, action, target, target_type, effect) VALUES
  (1000, 'ROLE_ADMIN', 'READ',   '*', 'ENTITY', 'ALLOW'),
  (1001, 'ROLE_ADMIN', 'CREATE', '*', 'ENTITY', 'ALLOW'),
  (1002, 'ROLE_ADMIN', 'UPDATE', '*', 'ENTITY', 'ALLOW'),
  (1003, 'ROLE_ADMIN', 'DELETE', '*', 'ENTITY', 'ALLOW'),
  (1004, 'ROLE_USER',  'READ',   '*', 'ENTITY', 'ALLOW')
ON CONFLICT (id) DO NOTHING;

-- Attribute-level wildcards per @SecuredEntity (entity_name uppercase + '.*').
-- Covers both starter entities (USER, AUTHORITY, ORGANIZATION, DEPARTMENT, EMPLOYEE)
-- and consumer entities (DOMAIN, TAG, METAPACK, METASET, METASOURCE, METASYNC, ...).
INSERT INTO sec_permission (id, authority_name, action, target, target_type, effect)
SELECT
  3000 + ROW_NUMBER() OVER ()::INT AS id,
  role,
  action,
  entity || '.*' AS target,
  'ATTRIBUTE'    AS target_type,
  'ALLOW'        AS effect
FROM (
  SELECT unnest(ARRAY[
    'ORGANIZATION','DOMAIN','TAG',
    'METAPACK','METAPACKVERSION','METAPACKREGISTRATION',
    'METASET','METASETVERSION',
    'METASOURCE','METASYNC',
    'USER','AUTHORITY','DEPARTMENT','EMPLOYEE'
  ]) AS entity
) e
CROSS JOIN (VALUES ('ROLE_ADMIN','EDIT'), ('ROLE_ADMIN','VIEW'), ('ROLE_USER','VIEW')) AS r(role, action)
ON CONFLICT (id) DO NOTHING;
