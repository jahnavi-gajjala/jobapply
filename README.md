# jobapply — AI-assisted job application tracker

A multi-user MVP built with React + Vite, Java 21 + Spring Boot, Spring Security/JWT and MySQL/Flyway.

## Working features in this build
- Registration/login with BCrypt password hashing and JWT access tokens.
- Per-user profile and application records.
- Browse/search public job records and add private job descriptions to your own workspace.
- Duplicate application prevention for the same user and job.
- Application statuses, private per-application notes, and per-user dashboard counts.
- PDF/DOCX resume text extraction (up to 5 MB; scanned-PDF OCR is not included), master-resume selection, and safe deletion fallback.
- Heuristic resume/job-description compatibility report.
- Keyword-prioritized resume draft that reorders existing lines; it does not invent credentials.
- Cover-letter starter template.

## Run with Docker Compose
1. Copy `.env.example` to `.env`.
2. Replace the example database and JWT secrets in `.env`.
3. Run `docker compose up --build`.
4. Open `http://localhost:3000`.

The frontend is served by Nginx, which proxies `/api/` requests to Spring Boot. MySQL data is stored in a Docker volume.

## Run locally for development
Requirements: Java 21, Maven 3.9+, Node.js 20+, and MySQL 8.
1. Start MySQL: `docker compose up -d mysql`.
2. Backend: `cd backend && mvn spring-boot:run`.
3. Frontend: `cd frontend && npm install && npm run dev`.
4. Open `http://localhost:5173` and register.

Backend health: `http://localhost:8080/api/v1/health`

## API endpoints
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`
- `GET /api/v1/jobs` and `GET /api/v1/jobs?q=java`
- `POST /api/v1/jobs` (authenticated; creates a private job record)
- `POST /api/v1/resumes/upload` (multipart field `file`)
- `GET /api/v1/resumes`, `GET /api/v1/resumes/{id}`, `PUT /api/v1/resumes/{id}/master`, `DELETE /api/v1/resumes/{id}`
- `POST /api/v1/ats/analyze` (`resumeId`, `jobId`)
- `POST /api/v1/ai/tailor-resume` (`resumeId`, `jobId`)
- `POST /api/v1/ai/generate-cover-letter` (`resumeId`, `jobId`)
- `POST /api/v1/applications` (`jobId`)
- `GET /api/v1/applications`
- `PATCH /api/v1/applications/{id}/status` (`status`)
- `PUT /api/v1/applications/{id}/notes` (`notes`, private to the signed-in user)
- `GET /api/v1/applications/stats`

Application totals count statuses `SUBMITTED`, `UNDER_REVIEW`, `INTERVIEW`, `REJECTED`, and `OFFER`. Saved/preparation statuses are not counted as applied. A user changes the status after applying externally; this build does not verify external submissions.

## Important limitations
The ATS result is an explainable heuristic compatibility estimate, not an employer ATS score or hiring prediction. Resume tailoring currently reorders existing text only. The original resume file is not retained; extracted text and file metadata are stored. The job list is not yet connected to live job boards; users can paste job descriptions manually. Automatic external application submission is not implemented.

## Remaining work before a public SaaS launch
- Live job-source integrations and verified current job ingestion.
- LLM-backed resume rewriting and formatted PDF/DOCX export.
- Provider-authorized application submission adapters and end-to-end verification.
- Email verification, password reset, refresh-token rotation/revocation, rate limiting and admin provisioning.
- File malware scanning, private object storage, privacy/retention controls, audit logs and production deployment hardening.
- Automated integration tests and full build verification in an environment with Maven and npm dependencies available.

Local compose defaults are for development only. Never deploy with the example credentials.
