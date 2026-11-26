CREATE TABLE contact_requests (
    id UUID PRIMARY KEY, company_id UUID NOT NULL, user_id UUID NOT NULL, job_id UUID NOT NULL,
    reason VARCHAR(2000) NOT NULL CHECK (length(trim(reason)) BETWEEN 1 AND 2000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    created_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL,
    reviewed_by UUID, reviewed_at TIMESTAMPTZ, rejection_reason VARCHAR(2000),
    CHECK ((status='PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL AND rejection_reason IS NULL)
        OR (status='APPROVED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NULL)
        OR (status='REJECTED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NOT NULL AND length(trim(rejection_reason))>0))
);
CREATE UNIQUE INDEX uq_contact_request_pending ON contact_requests(company_id,user_id) WHERE status='PENDING';
CREATE INDEX ix_contact_request_company_user ON contact_requests(company_id,user_id,created_at DESC,id DESC);
CREATE INDEX ix_contact_request_status ON contact_requests(status,created_at DESC,id DESC);
CREATE INDEX ix_application_company_user ON applications(company_id,user_id);
CREATE TABLE contact_access_logs (
    id UUID PRIMARY KEY, company_id UUID NOT NULL, hr_id UUID NOT NULL, user_id UUID NOT NULL,
    via VARCHAR(20) NOT NULL CHECK(via IN ('APPLICATION','APPROVED_REQUEST')), created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_contact_log_user ON contact_access_logs(user_id,created_at DESC,id DESC);
CREATE INDEX ix_contact_log_company ON contact_access_logs(company_id,created_at DESC,id DESC);
