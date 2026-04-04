ALTER TABLE placement_profiles
    ADD COLUMN activity_scores_json TEXT NULL,
    ADD COLUMN placement_rounds_json TEXT NULL;
