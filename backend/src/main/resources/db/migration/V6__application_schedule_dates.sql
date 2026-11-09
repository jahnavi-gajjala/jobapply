ALTER TABLE applications ADD COLUMN interview_date DATE NULL;
ALTER TABLE applications ADD COLUMN follow_up_date DATE NULL;
CREATE INDEX idx_app_user_follow_up ON applications(user_id, follow_up_date);
CREATE INDEX idx_app_user_interview ON applications(user_id, interview_date);
