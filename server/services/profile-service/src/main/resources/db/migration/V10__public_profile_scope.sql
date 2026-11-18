-- Legacy PUBLIC rows without recorded consent stay hidden until submitted and approved again.
CREATE VIEW public_candidate_ids AS
SELECT user_id FROM candidate_profiles
WHERE visibility = 'PUBLIC' AND consent_at IS NOT NULL AND consent_withdrawn_at IS NULL;
