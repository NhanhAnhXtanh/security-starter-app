-- Wildcard sec_permission seed for development/MVP.
-- Grants ROLE_ADMIN full CRUD and ROLE_USER read access to every @SecuredEntity
-- registered with the starter (target='*' is handled in RolePermissionServiceDbImpl).
--
-- Why a single wildcard row per (role, action) instead of per-entity rows:
-- - Consumer entities (catalog, meta/*) are still in flux. Per-entity seeds
--   would require a new SQL change every time someone adds an entity.
-- - For fine-grained authorization later, delete these wildcards and replace
--   with rows scoped to FQCN targets.
--
-- Idempotent via ON CONFLICT — Spring Boot data.sql may re-run on devtools restart.
INSERT INTO sec_permission (id, authority_name, action, target, target_type, effect) VALUES
  (1000, 'ROLE_ADMIN', 'READ',   '*', 'ENTITY', 'ALLOW'),
  (1001, 'ROLE_ADMIN', 'CREATE', '*', 'ENTITY', 'ALLOW'),
  (1002, 'ROLE_ADMIN', 'UPDATE', '*', 'ENTITY', 'ALLOW'),
  (1003, 'ROLE_ADMIN', 'DELETE', '*', 'ENTITY', 'ALLOW'),
  (1004, 'ROLE_USER',  'READ',   '*', 'ENTITY', 'ALLOW')
ON CONFLICT (id) DO NOTHING;
