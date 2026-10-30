# MediaHub API
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-4169E1?logo=postgresql&logoColor=white)
![Cloudflare R2](https://img.shields.io/badge/Cloudflare-R2-F38020?logo=cloudflare&logoColor=white)

A REST backend for a Pinterest/Cosmos-style image sharing platform. Users can register, upload images directly to Cloudflare R2, publish posts, follow each other, save posts, and organize saved posts into collections.

The project focuses on production-minded backend concerns: secure token handling with refresh-token rotation, verified direct-to-storage uploads, keyset pagination on every list endpoint, and consistent RFC 7807 error responses.

## Table of Contents
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Setup](#setup)
- [Session Model](#session-model)
- [Data Model](#data-model)
- [API Reference](#api-reference)
- [Conventions](#conventions)
- [Project Structure](#project-structure)

## Tech stack

- **Language / runtime:** Java 21
- **Framework:** Spring Boot 4.1.2
- **Security:** Spring Security, OAuth2 Resource Server, JWT `HS256`, Bcrypt
- **Database:** PostgreSQL with Flyway migration
- **Object storage:** Cloudflare R2 via AWS SDK for Java
- **Persistence:** Spring Data JPA / Hibernate
- **Validation & Utilities:** Jakarta Bean Validation, Lombok
- **Build:** Maven

## Features

**Accounts & auth**
- Register / login / logout with short-lived access tokens and rotating refresh tokens
- Refresh token delivered in an `HttpOnly` cookie
- Token rotation on every refresh, with reuse detection that revokes the whole session family
- Password change with optional "sign out other devices"
- Email and username availability checks for signup forms

**Profiles & social graph**
- Partial profile updates, plus separate guarded routes for username, email and password
- Profile picture update through upload pipeline
- Public profiles with follower/following counts
- Idempotent follow / unfollow with self-follow checks
- Paged follower and following lists

**Posts**
- Create, edit, delete; title + optional description + one image
- Global feed, user feed, and a follow feed
- Case-insensitive search over title and description
- Width/height returned on every post so clients can reserve layout space before the image loads

**Saving & collections**
- Save / unsave any post; saved posts are visible only to their owner
- Named collections, private by default with unique title per author
- Collection lists include post count and a cover image
- Collections support "Which of my collections contain this post?" lookup
- Unsaving a post automatically removes it from every collection
- Private collections respond `404` to everyone but the owner, so their existence is never revealed

**Uploads**
- Presigned `PUT` direct to R2 — `Content-Type` and size are part of the signature
- Verified JPEG, PNG and WebP files; 400 KB for avatars, 10 MB for posts
- Promoted from `tmp/` to an immutable final key only after verification passes

**Scheduled work**
- Deletes `COMPLETED` images that were never attached to the post and expired `PENDING` rows
- Purges expired refresh tokens daily

**Cross-cutting**
- Keyset (cursor) pagination on every list — no `OFFSET`, each list has a matching composite index
- RFC 7807 `application/problem+json` errors with per-field validation messages
- Public reads accept an optional token, so `savedByViewer` and `isFollowing` are accurate for signed-in viewers and simply `false` for guests

## Setup

JDK 21 · Maven 3.9+ · PostgreSQL 14+ with permission to create the `pg_trgm` extension · an R2 bucket.

### 1. Environment variables

`application.yaml` — every secret comes from the environment.

```yaml
datasource:
  url: ${DB_URL}
  username: ${DB_USER}
  password: ${DB_PASSWORD}
    
jwt:
  secret: ${JWT_SECRET}
  issuer: hub-hakye
  access-ttl-minutes: 60
  refresh-ttl-days: 7
  
refresh-cookie:
  name: refresh_token
  secure: ${REFRESH_COOKIE_SECURE:false}      # Currently local dev variable
  same-site: ${REFRESH_COOKIE_SAME_SITE:Lax}  # Currently local dev variable
  path: /api/auth
  
storage:
  account-id: ${R2_ACCOUNT_ID}
  bucket: ${R2_BUCKET}
  access-key-id: ${R2_ACCESS_KEY_ID}
  secret-access-key: ${R2_SECRET_ACCESS_KEY}
  public-base-url: ${R2_PUBLIC_BASE_URL}
  presign-expiry-seconds: 300

cors:
  allowed-origin: http://localhost:3000       # Currently local dev variable
```

### 2. Database

Flyway applies `V1` → `V7` on boot. Hibernate only validates the schema, so a mismatch aborts startup instead of altering tables.

### 3. R2 bucket

- CORS on the bucket allowing `PUT` from your frontend origin
- A lifecycle rule expiring objects under `tmp/` after 1 day
- Public read access, with `R2_PUBLIC_BASE_URL` set to the public hostname

## Session model

- Access token is short-lived JWT, sent as a `Bearer` header.
- Refresh token is opaque random value, delivered as an `HttpOnly` cookie and rotated on every use.
- Every login starts a token *family*. Presenting an already-rotated token outside the grace window revokes the entire family.
- `/refresh` and `/logout` additionally check the `Origin` header against the configured frontend origin on top of `SameSite`.

## Data model

| Table | Purpose |
| --- | --- |
| `users` | Account and profile; unique lowercase email and username |
| `follows` | `(follower_id, followee_id)` composite PK, self-follow blocked |
| `uploads` | Pending/completed upload records with verified dimensions |
| `posts` | One image per post (`image_key` unique), title, description, dimensions |
| `saved_posts` | `(user_id, post_id)` |
| `collections` | Owner and title, unique `lower(title)` per user, `is_public` |
| `collection_posts` | Membership, with composite foreign keys that require the collection and the saved post to share an owner |
| `refresh_tokens` | Hashed token, `family_id` lineage, expiry and revocation timestamps |

Deletions cascade in the database, so removing a user sweeps their posts, saves, collections, uploads and sessions.

## API reference

### Auth — `/api/auth`

| Method | Path | Auth | Description |
| :---: | --- | :---: | --- |
| `POST` | `/register` | — | Create account, start session → `201` |
| `POST` | `/login` | — | Exchange credentials for tokens |
| `POST` | `/refresh` | cookie | Rotate refresh token, issue new access token |
| `POST` | `/logout` | cookie | Revoke the session family → `204` |
| `GET` | `/check-email?email=` | — | `{ "available": true }` |
| `GET` | `/check-username?username=` | — | `false` for taken, malformed or reserved names |

### Users — `/api/users` (Includes endpoints from Post and Collection)

| Method | Path | Auth | Description |
| :---: | --- | :---: | --- |
| `GET` | `/me` | Bearer | Own account |
| `PATCH` | `/me` | Bearer | `displayName`, `gender`, `bio` |
| `PATCH` | `/me/username` | Bearer | `{ username }` |
| `PATCH` | `/me/email` | Bearer | `{ newEmail, currentPassword }` |
| `PATCH` | `/me/password` | Bearer | `{ currentPassword, newPassword, logoutOtherDevices? }` → `AuthResponse` |
| `PATCH` | `/me/avatar` | Bearer | `{ uploadId }` of a completed avatar upload |
| `DELETE` | `/me/avatar` | Bearer | Unlink and delete the object |
| `GET` | `/{username}` | optional | Public profile with counts |
| `POST` | `/{username}/follow` | Bearer | `204`, idempotent |
| `DELETE` | `/{username}/follow` | Bearer | `204`, idempotent |
| `GET` | `/{username}/followers` | optional | Paged profiles |
| `GET` | `/{username}/following` | optional | Paged profiles |
| `GET` | `/{username}/posts` | optional | One author's posts |
| `GET` | `/{username}/collections` | optional | The owner sees private and public, non-owners public|
| `GET` | `/saved-posts` | Bearer | Saved posts, most recent first |

### Posts — `/api/posts`

| Method | Path | Auth | Description |
| :---: | --- | :---: | --- |
| `POST` | `/` | Bearer | `{ uploadId, title, description? }` → `201` |
| `GET` | `/` | optional | Global feed, newest first |
| `GET` | `/search?q=` | optional | Substring match on title/description |
| `GET` | `/following` | Bearer | Feed of accounts you follow |
| `GET` | `/{id}` | optional | Detail + author summary |
| `PATCH` | `/{id}` | Bearer | `title` and/or `description` |
| `DELETE` | `/{id}` | Bearer | `204` |
| `PUT` | `/{id}/save` | Bearer | `204`, idempotent |
| `DELETE` | `/{id}/save` | Bearer | `204`, also removes it from your collections |
| `GET` | `/{postId}/collections` | Bearer | Your collections holding that post |

### Collections — `/api/collections`

| Method | Path | Auth | Description |
| :---: | --- | :---: | --- |
| `POST` | `/` | Bearer | `{ title, isPublic? }` — private unless `true` → `201` |
| `GET` | `/{id}` | optional | Detail + author |
| `PATCH` | `/{id}` | Bearer | Title and/or visibility |
| `DELETE` | `/{id}` | Bearer | `204`; posts stay saved |
| `GET` | `/{id}/posts` | optional | Posts, most recently added first |
| `PUT` | `/{id}/posts/{postId}` | Bearer | `204`, idempotent |
| `DELETE` | `/{id}/posts/{postId}` | Bearer | `204`, idempotent |

### Uploads — `/api/uploads`

| Method | Path | Auth | Description |
| :---: | --- | :---: | --- |
| `POST` | `/` | Bearer | `{ kind, contentType, size }` → presigned URL, `201` |
| `POST` | `/{id}/complete` | Bearer | Verify and finalize → `200 { uploadId, key, url }` |

`kind` is either `AVATAR` (400 KB) or `POST` (10 MB); accepted types are `image/jpeg`, `image/png`, `image/webp`. An upload must be completed before it can be attached, otherwise throws `409 Upload has not been finalized`.

## Conventions

### Pagination

Every list is keyset-paginated, newest first.

```json
{ "items": [], "nextCursor": "<opaque cursor>" }
```

- `cursor` is opaque base64 of `"<createdAt>|<id>"` — omit it for the first page, a malformed one is `400 Invalid cursor`.
- `size` defaults to **20**, clamped to 1..50.
- `nextCursor` is `null` when exhausted.
- Followers, saved posts and collection members page over the edge's own timestamp, so order means "most recently added".

### Errors

RFC 7807 `application/problem+json`, mapped centrally by `ApiExceptionHandler`


| Status | When |
| :---: | --- |
| `400` | Validation, malformed body or cursor, blank search, wrong upload kind, expired upload, failed file verification, self-follow |
| `401` | Missing/invalid/expired token, bad credentials, dead refresh token, token for a deleted account |
| `403` | Untrusted `Origin` on refresh/logout, or an authenticated-only route |
| `404` | Unknown resource, or any resource you don't own |
| `409` | Email/username taken, duplicate collection title, upload already used, post not saved yet |

## Project structure

Feature-first packages — each domain owns its controller, service, repository and DTOs, and cross-feature calls go through a service into another feature's internals.

```text
com.hakye.mediahub.api
├── auth/           Register, login, JWT, refresh rotation, cookie, cleanup
├── user/           Account, public profiles, summary mapper
├── follow/         Follow functionality
├── post/           Feed, authoring, search
├── collection/     Saved posts and collections
├── upload/         Presign lifecycle, image header parsing, cleanup job
├── storage/        ObjectStorageService and the S3/R2 beans
├── config/         SecurityConfig, JwtConfig, @ConfigurationProperties
├── common/         Exception handler, Cursor Pagination, utilities
└── ApiApplication
```