CREATE TABLE resumes (
 id BIGINT NOT NULL AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 title VARCHAR(180) NOT NULL,
 original_file_name VARCHAR(255) NOT NULL,
 content_type VARCHAR(120) NOT NULL,
 extracted_text LONGTEXT NOT NULL,
 is_master BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id),
 CONSTRAINT fk_resume_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX idx_resume_user_created(user_id,created_at)
);
