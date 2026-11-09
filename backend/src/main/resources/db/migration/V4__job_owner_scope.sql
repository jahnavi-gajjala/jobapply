ALTER TABLE jobs ADD COLUMN owner_user_id BIGINT NULL;
ALTER TABLE jobs ADD CONSTRAINT fk_job_owner FOREIGN KEY(owner_user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX idx_jobs_owner_status ON jobs(owner_user_id,status);
