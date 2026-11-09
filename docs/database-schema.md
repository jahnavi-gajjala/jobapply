# jobapply database schema (initial foundation)

V1 creates `users`. Subsequent Flyway migrations will add:
- resumes, resume_versions
- jobs, job_skills, saved_jobs
- job_profiles, user_skills
- ats_reports, cover_letters
- applications, application_events
- automation_tasks, consents, audit_logs
- notifications, refresh_tokens, subscriptions

All user-owned records must be scoped by authenticated user ID. Add foreign keys, indexes, uniqueness constraints and retention policies with each migration.
