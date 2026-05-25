# Postman collection — security-starter-app

End-to-end RBAC + auth flow against the backend on `http://localhost:8081`.

## Import

1. **Postman** → File → Import → drop both JSON files:
   - `security-starter-app.postman_collection.json` (requests + tests)
   - `security-starter-app.postman_environment.json` (host + credentials)
2. Top-right env dropdown → select **"security-starter-app (local dev)"**.

## Run order

The collection assumes the four folders run top to bottom because tokens captured by earlier requests are read by later ones via collection variables:

| Folder | Captures | Reads |
|---|---|---|
| `Auth - admin` | `adminToken`, `adminRefreshToken` | — |
| `Auth - user` | `userToken`, `userRefreshToken` | — |
| `RBAC checks` | — | `adminToken`, `userToken` |
| `Token lifecycle` | rotates `userToken` / `userRefreshToken` | `userRefreshToken`, `adminToken` |
| `Cache eviction probe` | — | `adminToken` |

Use **Collection Runner** (▶︎ next to the collection name) or the CLI:

```bash
# requires npm i -g newman
newman run security-starter-app.postman_collection.json \
       -e security-starter-app.postman_environment.json
```

## What each folder verifies

- **Auth - admin**: `POST /api/auth/login` returns `accessToken` + `refreshToken`, status 200.
- **Auth - user**: register (`201` first run / `409` on reruns — both accepted), then login.
- **RBAC checks** — the core of the suite:
  - anonymous request → `401`,
  - admin can read tags and list roles,
  - `ROLE_USER` can read tags (sec_permission seed grants READ),
  - `ROLE_USER` is denied at the URL gate on `/api/admin/**` (`403`),
  - `ROLE_USER` is denied at the data layer (`SecureDataManager`) on `POST /api/tags` (`403`).
- **Token lifecycle**: refresh rotation (new refresh token must differ from the previous one), reuse-after-rotation must fail (single-use semantics), change password (revokes all refresh tokens), logout.
- **Cache eviction probe**: `PUT /api/admin/sec/roles/ROLE_USER` triggers `@CacheEvict` on both `userAuthoritiesByUsername` and `sec-permission-matrix`. Pair this with Hazelcast Management Center (see below) to watch the maps drop and refill.

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
| `userAuthoritiesByUsername` | `DefaultCurrentUserAuthorityResolver` on first auth per username | admin role/permission writes, `UserAuthorityCacheService.evict(...)` |
| `sec-permission-matrix` | `RequestPermissionSnapshot.getMatrix` on first request per role-set | admin permission writes |

Run `RBAC checks` once → both maps populate. Run `Cache eviction probe` → both drop to 0. Re-run `RBAC checks` → they refill.
