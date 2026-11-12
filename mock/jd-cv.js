const express = require('express');
const { createHmac, timingSafeEqual } = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const ALGORITHMS = { HS256: ['sha256', 32], HS384: ['sha384', 48], HS512: ['sha512', 64] };
const PDF_FILE = path.join(__dirname, 'fixtures', 'cvs', 'sample.pdf');
const error = (res, status, code, message) => res.status(status).json({ code, message });

function verifyCandidateToken(req, res, next) {
  const secret = process.env.JWT_SECRET || '';
  if (Buffer.byteLength(secret) < 32) {
    return error(res, 503, 'DEPENDENCY_NOT_CONFIGURED', 'Chưa cấu hình xác thực hợp lệ cho dữ liệu ứng viên mẫu.');
  }
  let claims;
  try {
    const match = /^Bearer ([A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+)$/i.exec(req.get('Authorization') || '');
    if (!match) throw new Error('Missing bearer JWT');
    const [headerPart, payloadPart, signaturePart] = match[1].split('.');
    const header = JSON.parse(Buffer.from(headerPart, 'base64url').toString('utf8'));
    if (!header || !Object.hasOwn(ALGORITHMS, header.alg)) throw new Error('Unsupported algorithm');
    const [hash, minimumBytes] = ALGORITHMS[header.alg];
    if (Buffer.byteLength(secret) < minimumBytes) throw new Error('Weak signing key');
    const expected = createHmac(hash, secret).update(`${headerPart}.${payloadPart}`).digest();
    const signature = Buffer.from(signaturePart, 'base64url');
    if (signature.length !== expected.length || !timingSafeEqual(signature, expected)) throw new Error('Invalid signature');
    claims = JSON.parse(Buffer.from(payloadPart, 'base64url').toString('utf8'));
    const now = Date.now() / 1000;
    if (!claims || typeof claims.sub !== 'string' || !UUID.test(claims.sub) || !Number.isFinite(claims.exp) || claims.exp <= now ||
      (claims.nbf !== undefined && (!Number.isFinite(claims.nbf) || claims.nbf > now))) throw new Error('Invalid claims');
  } catch {
    return error(res, 401, 'UNAUTHORIZED', 'Thiếu JWT hợp lệ, token sai chữ ký hoặc đã hết hạn.');
  }
  if (claims.mustChangePassword === true) {
    return error(res, 403, 'MUST_CHANGE_PASSWORD', 'Bạn cần đổi mật khẩu trước khi tiếp tục.');
  }
  if (claims.role !== 'CANDIDATE') return error(res, 403, 'FORBIDDEN', 'Chỉ ứng viên được truy cập dữ liệu này.');
  req.candidateId = claims.sub.toLowerCase();
  next();
}

function allowCandidateBrowser(req, res, next) {
  const origin = process.env.MOCK_WEB_ORIGIN || process.env.WEB_BASE_URL || 'http://localhost:3000';
  res.vary('Origin');
  if (req.get('Origin') === origin) {
    res.set('Access-Control-Allow-Origin', origin);
    res.set('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Authorization, Content-Type');
  }
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
}

function candidateJob(job) {
  return {
    jobId: job.jobId,
    companyId: job.companyId,
    jobTitle: job.jobTitle,
    companyName: job.companyName,
    deadline: job.deadline,
    acceptingApplications: job.status === 'APPROVED' && !job.companyLocked && Date.parse(job.deadline) > Date.now(),
  };
}

function mountJdCv(app, getData) {
  const jobs = express.Router();
  jobs.use((req, res, next) => {
    const expected = process.env.RECRUITMENT_COMPANY_SERVICE_KEY || process.env.INTERNAL_SERVICE_KEY || 'dev-internal-key';
    if (!expected.trim()) return error(res, 503, 'DEPENDENCY_NOT_CONFIGURED', 'Chưa cấu hình internal key của mock JD.');
    const actualBytes = Buffer.from(req.get('X-Internal-Key') || '');
    const expectedBytes = Buffer.from(expected);
    if (actualBytes.length !== expectedBytes.length || !timingSafeEqual(actualBytes, expectedBytes)) {
      return error(res, 401, 'INVALID_INTERNAL_KEY', 'Thiếu hoặc sai X-Internal-Key.');
    }
    next();
  });
  jobs.get('/:jobId/application-context', (req, res) => {
    if (!UUID.test(req.params.jobId)) return error(res, 400, 'VALIDATION_ERROR', 'jobId phải là UUID.');
    const job = (getData().jobs || []).find(item => item.jobId.toLowerCase() === req.params.jobId.toLowerCase());
    if (!job) return error(res, 404, 'NOT_FOUND', 'Không tìm thấy JD.');
    res.json(job);
  });
  jobs.use((req, res) => error(res, 404, 'ROUTE_NOT_FOUND', 'Không có endpoint mock JD này.'));
  app.use('/internal/jobs', jobs);

  const candidateJobs = express.Router();
  candidateJobs.use(allowCandidateBrowser, verifyCandidateToken);
  candidateJobs.get('/', (req, res) => {
    res.set('Cache-Control', 'private, no-store').json(
      (getData().jobs || []).map(candidateJob).filter(job => job.acceptingApplications)
    );
  });
  candidateJobs.get('/:jobId', (req, res) => {
    if (!UUID.test(req.params.jobId)) return error(res, 400, 'VALIDATION_ERROR', 'jobId phải là UUID.');
    const job = (getData().jobs || []).find(item => item.jobId.toLowerCase() === req.params.jobId.toLowerCase());
    if (!job || job.status !== 'APPROVED' || job.companyLocked) {
      return error(res, 404, 'NOT_FOUND', 'Không tìm thấy việc làm.');
    }
    res.set('Cache-Control', 'private, no-store').json(candidateJob(job));
  });
  candidateJobs.use((req, res) => error(res, 404, 'ROUTE_NOT_FOUND', 'Không có endpoint việc làm này.'));
  app.use('/jobs', candidateJobs);

  const cvs = express.Router();
  cvs.use(allowCandidateBrowser, verifyCandidateToken);
  cvs.get('/', (req, res) => {
    const choices = (getData().cvs || []).filter(cv => cv.ownerId.toLowerCase() === req.candidateId)
      .map(({ id, name, targetPosition }) => ({ id, name, targetPosition }));
    res.set('Cache-Control', 'private, no-store').json(choices);
  });
  cvs.get('/:cvId/pdf', (req, res) => {
    if (!UUID.test(req.params.cvId)) return error(res, 400, 'VALIDATION_ERROR', 'cvId phải là UUID.');
    const cv = (getData().cvs || []).find(item => item.id.toLowerCase() === req.params.cvId.toLowerCase());
    if (!cv) return error(res, 404, 'NOT_FOUND', 'Không tìm thấy CV.');
    if (cv.ownerId.toLowerCase() !== req.candidateId) return error(res, 403, 'FORBIDDEN', 'CV không thuộc ứng viên này.');
    let pdf;
    try { pdf = fs.readFileSync(PDF_FILE); }
    catch { return error(res, 503, 'CV_STORAGE_UNAVAILABLE', 'Không đọc được PDF fixture.'); }
    res.type('application/pdf').set('Cache-Control', 'private, no-store')
      .set('Content-Disposition', `attachment; filename="cv-${cv.id}.pdf"`).send(pdf);
  });
  cvs.use((req, res) => error(res, 404, 'ROUTE_NOT_FOUND', 'Không có endpoint mock CV này.'));
  app.use(['/profile/cvs', '/api/v1/profile/cvs'], cvs);
}

module.exports = { mountJdCv };
