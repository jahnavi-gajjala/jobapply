CREATE TABLE jobs (
 id BIGINT NOT NULL AUTO_INCREMENT,
 company_name VARCHAR(150) NOT NULL,
 job_title VARCHAR(200) NOT NULL,
 job_description TEXT,
 location VARCHAR(150),
 job_type VARCHAR(50) NOT NULL DEFAULT 'FULL_TIME',
 experience_level VARCHAR(50) NOT NULL DEFAULT 'FRESHER',
 graduation_year INT,
 application_url VARCHAR(1000),
 source VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
 external_job_id VARCHAR(150),
 status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id),
 UNIQUE KEY uk_jobs_source_external (source, external_job_id),
 INDEX idx_jobs_status_created (status, created_at),
 INDEX idx_jobs_title (job_title)
);
CREATE TABLE applications (
 id BIGINT NOT NULL AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 job_id BIGINT NOT NULL,
 status VARCHAR(40) NOT NULL DEFAULT 'SAVED',
 application_mode VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
 submitted_at TIMESTAMP NULL,
 notes TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id),
 CONSTRAINT fk_app_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_app_job FOREIGN KEY(job_id) REFERENCES jobs(id),
 CONSTRAINT uk_application_user_job UNIQUE(user_id,job_id),
 INDEX idx_app_user_status (user_id,status)
);
