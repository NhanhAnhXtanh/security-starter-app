# Postman collection — security-starter-app

**100 requests across 18 folders** covering every endpoint exposed by the consumer app and the starter (`com.vn.security.core`) at `http://localhost:8081`.

## Files

| File | Purpose |
|---|---|
| `security-starter-app.postman_collection.json` | All requests, with status-code asserts on the RBAC suite. |
| `security-starter-app.postman_environment.json` | `baseUrl` + admin/test credentials. |

## Import

Postman → File → Import → drop both JSON files. Top-right env dropdown → select **"security-starter-app (local dev)"**.

Or via Newman:

```bash
npm i -g newman
cd postman
newman run security-starter-app.postman_collection.json \
       -e security-starter-app.postman_environment.json \
       --folder "✅ RBAC tests (status-code assertions)"   # only run the assertion suite
```

## Folder map

| Folder | Count | Notes |
|---|---:|---|
| 🔐 Auth | 6 | login (admin + testuser), register, refresh, change-password, logout |
| 📋 Catalog - Tags | 5 | CRUD on `/api/tags`, captures `{{tagId}}` on create |
| 📋 Catalog - Domains | 5 | CRUD on `/api/domains`, captures `{{domainId}}` |
| 📋 Catalog - Organizations | 5 | CRUD on `/api/app/organizations`, captures `{{orgId}}` |
| 📦 Meta - Packs | 10 | CRUD + versions + registrations (create / approve / revoke) |
| 📦 Meta - Sets | 10 | CRUD + by-source / by-code / by-metasync-code + publish / discontinue |
| 📦 Meta - Set Versions | 5 | CRUD + lookup by code+version |
| 📦 Meta - Sources | 12 | CRUD + schema / query / sync now / extract-to-metaset / REST proxy |
| 📦 Meta - Syncs | 7 | CRUD + by-code + extract-to-metaset |
| 📦 Meta - Public | 1 | unauthenticated pack data preview |
| 🛡 Admin - Roles | 5 | starter SecRoleAdminResource (cache evicts) |
| 🛡 Admin - Permissions | 5 | starter SecPermissionAdminResource (cache evicts) |
| 🛡 Admin - Menu Definitions | 6 | CRUD + `/sync` to reseed menu defs |
| 🛡 Admin - Menu Permissions | 3 | grant / list / revoke menu access |
| 🛡 Admin - Security catalog | 1 | list `@SecuredEntity` registrations |
| 🔍 Security info | 6 | authorities CRUD + current-user menu + entity capabilities |
| ✅ RBAC tests | 7 | status-code asserting: anon / admin / user × read / write |
| 💾 Cache eviction probe | 1 | PUT role → watch Hazelcast maps clear |

## Auto-login

A **collection-level pre-request script** auto-logs-in as `admin/admin` and stores the JWT into `{{adminToken}}` whenever the variable is empty. Net effect: pick any request in any folder and it just works — no need to manually run the Auth folder first. For the user-token folders (RBAC tests, refresh), run `🔐 Auth → Login testuser` once per session.

## Captured variables

These are populated by `Tests` scripts on create-style requests so later requests in the same folder reuse the freshly created entity ID:

| Captured by | Variable |
|---|---|
| `Login admin` | `adminToken`, `adminRefreshToken` |
| `Login testuser` | `userToken`, `userRefreshToken` |
| `Refresh access token` | rotates `userToken`, `userRefreshToken` |
| `Create tag/domain/org` | `tagId` / `domainId` / `orgId` |
| `Create pack` / `Create pack registration` | `packId` / `regId` |
| `Create meta-set` / `Create version` | `metaSetId` + `metaSetCode` / `versionId` |
| `Create source (Postgres)` | `sourceId` + `sourceCode` |
| `Create sync` | `syncId` + `syncCode` |
| `Create permission/menu definition/menu permission` | `permissionId` / `menuDefId` / `menuPermId` |

## Watching the Hazelcast cache

Start Management Center alongside the backend:

```bash
docker run -d --name hazelcast-mc -p 8090:8080 hazelcast/management-center:5.5
# http://localhost:8090
# Add cluster: name=security-core, member=host.docker.internal:5701
```

Maps worth opening (**Storage → Maps**):

| Map | Populated by | Cleared by |
|---|---|---|
| `userAuthoritiesByUsername` | `DefaultCurrentUserAuthorityResolver` on first auth per username | admin role/permission writes (folders 🛡 Admin - Roles / Permissions), `UserAuthorityCacheService.evict(...)` from consumer code |
| `sec-permission-matrix` | `RequestPermissionSnapshot.getMatrix` on first request per role-set | admin permission writes |

Run **RBAC tests** once → both maps populate. Run **Cache eviction probe** → both drop to 0. Re-run **RBAC tests** → they refill.
