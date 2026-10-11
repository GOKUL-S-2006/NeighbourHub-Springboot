# NeighbourHub Backend (Spring Boot)

Spring Boot 3 / Java 17 port of the Node + Express + MongoDB backend. It uses the **same MongoDB
collections** (`users`, `issues`) and the **same REST routes and JSON shapes**, so the existing
frontend and data should work unchanged.

## Run

```bash
cp .env.example .env      # fill in the values
mvn spring-boot:run       # or: mvn clean package && java -jar target/neighbourhub-backend-1.0.0.jar
```

Requirements: JDK 17+, Maven 3.9+.

| Variable | Notes |
|---|---|
| `MONGO_URI` | Same URI as before. It must include the database name in the path. |
| `JWT_SECRET` | At least 32 characters. Keep the old value to keep existing tokens valid (only if it is already 32+ chars). |
| `PORT` | Defaults to 5000. |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Moved out of source code. |
| `GROQ_API_KEY`, `GEMINI_API_KEY` | Optional. Without Gemini the news brief uses the built-in fallback. |

## Node -> Spring mapping

| Node | Spring |
|---|---|
| `server.js` | `NeighbourHubApplication`, `SecurityConfig` (CORS), `application.properties` |
| `routes/*` + `controllers/*` | `controller/*` (HTTP) + `service/*` (logic) |
| `models/*.js` (Mongoose) | `model/*` (`@Document`) + `repository/*` |
| `middleware/auth.js` | `security/JwtAuthFilter` + `SecurityConfig` |
| `jsonwebtoken`, `bcryptjs` | jjwt, `BCryptPasswordEncoder` |
| `multer` + `cloudinary` | `MultipartFile` + Cloudinary Java SDK (`UploadController`) |
| `groq-sdk` | `GroqClient` (REST) |
| `node-fetch` -> Gemini | `NewsService` (`RestClient`) |

## Endpoints (unchanged)

`GET /` , `POST /api/auth/signup|login`, `GET|POST /api/issues`, `GET /api/issues/my`,
`PUT|DELETE /api/issues/{id}`, `PATCH /api/issues/{id}/status|upvote`, `GET /api/admin/dashboard|issues`,
`PATCH|DELETE /api/admin/issues/{id}`, `GET|POST /api/users`, `GET /api/news/brief`, `POST /api/upload`,
`POST /api/analyze-image`, `POST /api/verify-repair`.

## Intentional behaviour changes

1. **Admin routes now check the role.** In Node, `middleware/admin.js` was a copy of `auth.js`, so any logged-in user could call `/api/admin/*`.
2. **`/api/users` (GET/POST) is admin-only and never returns password hashes.** Before, it was public, returned hashes, and let anyone create an admin.
3. **Cloudinary credentials come from env vars.** They were hard-coded in `cloudinary.js`.
4. `PATCH /api/issues/{id}/status` now rejects statuses other than open / in-progress / resolved (Node accepted any string).
5. Response bodies no longer contain Mongoose's `__v` field.

## Not carried over

The README of the Node repo describes Firebase Auth and a priority score (upvotes, age, image, severity).
Neither exists in the code: auth is JWT + bcrypt, and `GET /api/issues` simply sorts by votes. The port
matches the code, not the README.
