# jobapply architecture

## Initial architecture
A modular monolith: React/Vite client -> Spring Boot REST API -> MySQL. Private document storage and external AI/job-provider integrations are added behind service interfaces.

## Modules
- auth: registration, login, token lifecycle
- user: profile and preferences
- resume: uploads, parsing, master profile and versions
- job: job ingestion, search, deduplication and expiry
- matching: deterministic relevance and eligibility scoring
- ats: explainable resume/job compatibility reports
- ai: provider-neutral LLM orchestration and output validation
- coverletter: tailored letters
- application: application records and status history
- automation: permissioned provider adapters and approval workflow
- notification: reminders and updates
- admin: source health, usage and moderation

## Security boundaries
- Derive user identity from authenticated principal, never request-supplied user IDs.
- Keep resumes private; use short-lived signed download links.
- Store secrets in environment variables/secret manager.
- Require explicit user approval before an external application is submitted.
- Treat job descriptions and uploaded documents as untrusted input to AI.
- Record consent, approval, provider response and submission result in an audit trail.
