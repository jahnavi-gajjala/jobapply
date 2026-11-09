# API conventions

Base path: `/api/v1`

- `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`
- `GET/PUT /users/me`
- `POST /resumes/upload`, `GET /resumes`, `GET /resumes/{id}`, `POST /resumes/{id}/parse`
- `GET /jobs`, `GET /jobs/{id}`, `GET /jobs/recommended`, `POST /jobs/{id}/save`
- `POST /ats/analyze`, `GET /ats/reports/{id}`
- `POST /ai/tailor-resume`, `POST /ai/generate-cover-letter`
- `GET/POST /applications`, `GET /applications/{id}`, `PATCH /applications/{id}/status`
- `POST /applications/{id}/prepare`, `POST /applications/{id}/approve`, `POST /applications/{id}/submit`

External submission must be provider-specific, permissioned, idempotent where possible, and only report SUBMITTED after a verified provider response.
