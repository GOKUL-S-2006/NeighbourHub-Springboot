# NeighbourHub Backend

NeighbourHub is a community issue-reporting platform. Residents report local problems such as road damage,
water leaks or garbage, upvote the ones that matter, and admins track them through to resolution.
This repository contains the backend service, built with **Spring Boot 3**, **Spring Security** and **MongoDB**.

## Features

- Signup and login with JWT authentication
- Report, view, edit, upvote and delete community issues
- Duplicate detection: same category within 100 m of an open issue is rejected
- Issue feed sorted by community upvotes
- Role-based access (`user` and `admin`)
- Admin dashboard with statistics, filtering, pagination and status control
- Image upload to Cloudinary
- AI photo analysis (suggests title, description, category and severity) via Groq
- AI repair verification (compares before and after photos) via Groq
- AI-written daily community news brief via Gemini, with a built-in fallback

## Tech Stack

| Area | Technology |
|---|---|
| Language / framework | Java 17, Spring Boot 3 |
| Database | MongoDB with Spring Data MongoDB |
| Security | Spring Security, JWT (jjwt), BCrypt |
| Image hosting | Cloudinary |
| AI | Groq (vision), Google Gemini (text) |
| Build | Maven |

## Project Structure

```
src/main/java/com/neighbourhub
├── controller/   REST endpoints
├── service/      Business logic (auth, issues, admin, AI, news)
├── repository/   MongoDB access
├── model/        Issue and User documents
├── dto/          Request bodies
├── security/     JWT service, auth filter, access rules, CORS
├── config/       MongoDB, JSON, Cloudinary and HTTP client setup
├── exception/    Custom errors and global error handler
└── util/         Geo-distance helper
```

Requests flow through: **security filter → controller → service → repository → MongoDB**.

## Getting Started

**Requirements:** JDK 17+, Maven 3.9+, a MongoDB database.

```bash
# 1. Create your environment file
cp .env.example .env

# 2. Fill in the values (see table below), then run
mvn spring-boot:run
```

The server starts on `http://localhost:5000`. Opening it should show a "backend running" message.

To build a runnable jar:

```bash
mvn clean package
java -jar target/neighbourhub-backend-1.0.0.jar
```

## Configuration

Settings are read from environment variables or from a `.env` file in the project root.

| Variable | Required | Description |
|---|---|---|
| `MONGO_URI` | Yes | MongoDB connection string. Must include the database name. |
| `JWT_SECRET` | Yes | Token signing secret, at least 32 characters. |
| `PORT` | No | Server port. Default `5000`. |
| `CLOUDINARY_CLOUD_NAME` | For uploads | Cloudinary cloud name. |
| `CLOUDINARY_API_KEY` | For uploads | Cloudinary API key. |
| `CLOUDINARY_API_SECRET` | For uploads | Cloudinary API secret. |
| `CLOUDINARY_FOLDER` | No | Upload folder. Default `your_folder_name`. |
| `GROQ_API_KEY` | For AI photo features | Used by image analysis and repair verification. |
| `GEMINI_API_KEY` | No | Used for the news brief. Without it, a built-in summary is returned. |

Never commit your `.env` file. It is already listed in `.gitignore`.

## Authentication

1. `POST /api/auth/signup` creates an account (role `user`).
2. `POST /api/auth/login` returns `{ "token": "..." }`, valid for 7 days.
3. Send the token on protected requests: `Authorization: Bearer <token>`.

To make a user an admin, set their `role` to `admin` in the database, or create them through the admin-only `POST /api/users`.

## API Endpoints

### Public

| Method | Path | Description |
|---|---|---|
| GET | `/` | Health check |
| POST | `/api/auth/signup` | Create an account |
| POST | `/api/auth/login` | Log in and receive a JWT |
| GET | `/api/issues` | All issues, most upvoted first |
| GET | `/api/news/brief` | AI-generated daily community brief |
| POST | `/api/upload` | Upload an image (form field `image`) |
| POST | `/api/analyze-image` | AI analysis of a photo (form field `image`) |
| POST | `/api/verify-repair` | AI check of a repair (form field `afterImage`, plus `beforeImageUrl`, `category`, `issueTitle`) |

### Logged-in users

| Method | Path | Description |
|---|---|---|
| POST | `/api/issues` | Report an issue (`title` and `location` as `"lat,lon"` required) |
| GET | `/api/issues/my` | Issues you reported, newest first |
| PUT | `/api/issues/{id}` | Edit your own issue |
| PATCH | `/api/issues/{id}/status` | Set status: `open`, `in-progress` or `resolved` |
| PATCH | `/api/issues/{id}/upvote` | Toggle your upvote |
| DELETE | `/api/issues/{id}` | Delete your own issue (admins can delete any) |

### Admin only

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/dashboard` | Totals: issues by status, users, votes |
| GET | `/api/admin/issues` | Paginated list. Query: `page`, `limit`, `status`, `category` |
| PATCH | `/api/admin/issues/{id}/status` | Update an issue's status |
| DELETE | `/api/admin/issues/{id}` | Delete any issue |
| GET | `/api/users` | List users (passwords are never returned) |
| POST | `/api/users` | Create a user, optionally as `admin` |

## Data Model

**User** (`users`): `name`, `email` (unique), `password` (BCrypt hash), `role`, `createdAt`, `updatedAt`.

**Issue** (`issues`): `title`, `description`, `category`, `location`, `image`, `status`, `votes`, `votedBy`,
`createdBy`, `severity`, `priority`, `createdAt`, `updatedAt`.

## Error Responses

Errors are returned as JSON, with the text under `message` or `error`.

| Status | Meaning |
|---|---|
| 400 | Invalid or missing input |
| 401 | Missing, invalid or expired token |
| 403 | Not allowed (not the owner, or not an admin) |
| 404 | Issue not found |
| 409 | Duplicate issue (response includes the existing issue) |
| 500 | Unexpected server error |

## Testing

```bash
mvn test
```
