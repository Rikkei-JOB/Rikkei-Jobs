# UT-02 role alignment

The role matrix in `resource/Rikkei Jobs — Rikkei Project-role.pdf` extends UT-01/UT-02 with invited mentor scoring, administrator read access, and applicant phone visibility. Existing HR and STAFF behavior remains.

Recruitment resolves a mentor's current email through identity's internal user lookup using the JWT user ID. A mentor sees only rounds whose `interviewer_email` equals that verified email, and can update only those rounds while the application is `INTERVIEW`. Mentor responses omit applicant contact, CV, match, and private workflow history. HR still schedules and can record results as required by UT-02. Round revisions record the acting user ID.

ADMIN receives the same application list/detail/CV read access as STAFF, with no transition or round mutation. Candidate and HR ownership rules remain.

Candidate phone is owned by profile-service. A narrow internal endpoint returns only phone for one candidate, requires both an authorized HR/STAFF/ADMIN JWT and a configured internal key, and is called only after recruitment authorizes application detail. Missing phone returns null. Recruitment treats an unavailable profile service as a dependency error rather than silently inventing contact data.

Frontend adds ADMIN application list/detail and MENTOR assigned-round list/editor. Existing detail shows phone when present. OpenAPI and role documentation describe every new permission and response.
