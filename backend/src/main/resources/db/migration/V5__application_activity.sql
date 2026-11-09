CREATE TABLE application_activity (
 id BIGINT NOT NULL AUTO_INCREMENT,
 application_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 event_type VARCHAR(40) NOT NULL,
 old_status VARCHAR(40) NULL,
 new_status VARCHAR(40) NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id),
 CONSTRAINT fk_activity_application FOREIGN KEY(application_id) REFERENCES applications(id) ON DELETE CASCADE,
 CONSTRAINT fk_activity_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX idx_activity_application_created (application_id, created_at),
 INDEX idx_activity_user (user_id)
);
