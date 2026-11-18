ALTER TABLE offers DROP CONSTRAINT ck_offer_response;
ALTER TABLE offers ADD CONSTRAINT ck_offer_response CHECK (
    (status = 'SENT' AND responded_at IS NULL AND decline_reason IS NULL) OR
    (status = 'ACCEPTED' AND responded_at IS NOT NULL AND decline_reason IS NULL) OR
    (status = 'DECLINED' AND responded_at IS NOT NULL AND decline_reason IS NOT NULL
        AND length(trim(decline_reason)) > 0) OR
    (status = 'EXPIRED' AND responded_at IS NULL AND decline_reason IS NULL)
);
