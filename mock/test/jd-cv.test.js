const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { createHmac } = require('node:crypto');
const { spawn, spawnSync } = require('node:child_process');
const { once } = require('node:events');
const net = require('node:net');
const path = require('node:path');

const ROOT = path.resolve(__dirname, '..');
const SECRET = 'jd-cv-http-contract-tests-secret-32-bytes';
const OWNER = '30000000-0000-0000-0000-000000000001';
const OTHER = '30000000-0000-0000-0000-000000000002';
const EMPTY = '30000000-0000-0000-0000-000000000003';
const CV = '40000000-0000-0000-0000-000000000001';
const OTHER_CV = '40000000-0000-0000-0000-000000000003';
const JOB = '10000000-0000-0000-0000-000000000001';
const MISSING = '90000000-0000-0000-0000-000000000009';

function token(claims = {}, algorithm = 'HS256', secret = SECRET) {
  const header = Buffer.from(JSON.stringify({ alg: algorithm, typ: 'JWT' })).toString('base64url');
  const payload = Buffer.from(JSON.stringify({ sub: OWNER, role: 'CANDIDATE', exp: Math.floor(Date.now() / 1000) + 600, ...claims })).toString('base64url');
  const signingInput = `${header}.${payload}`;
  const hash = { HS256: 'sha256', HS384: 'sha384', HS512: 'sha512' }[algorithm] || 'sha256';
  return `${signingInput}.${createHmac(hash, secret).update(signingInput).digest('base64url')}`;
}

async function startMock(overrides = {}) {
  const reservation = net.createServer();
  reservation.listen(0, '127.0.0.1');
  await once(reservation, 'listening');
  const port = reservation.address().port;
  await new Promise(resolve => reservation.close(resolve));
  const child = spawn(process.execPath, ['server.js'], {
    cwd: ROOT, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'],
    env: { ...process.env, PORT: String(port), SERVICE_KEY: 'legacy-key', INTERNAL_SERVICE_KEY: 'internal-key',
      RECRUITMENT_COMPANY_SERVICE_KEY: '', JWT_SECRET: SECRET, MOCK_WEB_ORIGIN: 'http://localhost:3000', MOCK_CV_OWNER_ID: '', ...overrides },
  });
  let output = '';
  try {
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error(`Mock did not start: ${output}`)), 5000);
      child.stdout.on('data', chunk => {
        output += chunk;
        if (output.includes('Mock chạy')) { clearTimeout(timeout); resolve(); }
      });
      child.stderr.on('data', chunk => { output += chunk; });
      child.once('error', error => { clearTimeout(timeout); reject(error); });
      child.once('exit', code => { clearTimeout(timeout); reject(new Error(`Mock exited ${code}: ${output}`)); });
    });
  } catch (error) { child.kill(); throw error; }
  return {
    request: (url, options = {}) => fetch(`http://127.0.0.1:${port}${url}`, { ...options, signal: AbortSignal.timeout(5000) }),
    close: async () => { if (child.exitCode === null) { const exited = once(child, 'exit'); child.kill(); await exited; } },
  };
}

let mock;
before(async () => { mock = await startMock(); });
after(async () => { if (mock) await mock.close(); });
const candidateHeaders = (claims) => ({ Authorization: `Bearer ${token(claims)}` });
const internalHeaders = { 'X-Internal-Key': 'internal-key' };

test('the documented default token CLI can list the two portable CV fixtures', async () => {
  const generated = spawnSync(process.execPath, ['candidate-token.js'], {
    cwd: ROOT, encoding: 'utf8', env: { ...process.env, JWT_SECRET: SECRET, MOCK_CV_OWNER_ID: '' },
  });
  assert.equal(generated.status, 0);
  const response = await mock.request('/profile/cvs', { headers: { Authorization: `Bearer ${generated.stdout.trim()}` } });
  assert.equal(response.status, 200);
  assert.equal((await response.json()).length, 2);
});

test('local CV owner configuration maps the primary fixtures and preserves other owners', async () => {
  const localOwner = '50000000-0000-0000-0000-000000000001';
  const configured = await startMock({ MOCK_CV_OWNER_ID: localOwner });
  try {
    const own = await configured.request('/profile/cvs', { headers: candidateHeaders({ sub: localOwner }) });
    assert.equal((await own.json()).length, 2);
    const other = await configured.request('/profile/cvs', { headers: candidateHeaders({ sub: OTHER }) });
    assert.deepEqual((await other.json()).map(cv => cv.id), [OTHER_CV]);
  } finally {
    await configured.close();
  }
});

test('candidate job list exposes only accepting jobs without internal HR contacts', async () => {
  const response = await mock.request('/jobs', { headers: candidateHeaders() });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), [{
    jobId: JOB,
    companyId: '00000000-0000-0000-0000-0000000000a1',
    jobTitle: 'Java Developer',
    companyName: 'Rikkei Software',
    deadline: '2099-12-31T23:59:59Z',
    acceptingApplications: true,
  }]);
});

test('candidate job detail authenticates callers and supports browser CORS', async () => {
  await expectError(await mock.request('/jobs'), 401, 'UNAUTHORIZED');
  await expectError(await mock.request('/jobs', { headers: candidateHeaders({ role: 'HR' }) }), 403, 'FORBIDDEN');
  await expectError(await mock.request('/jobs/not-a-uuid', { headers: candidateHeaders() }), 400, 'VALIDATION_ERROR');
  await expectError(await mock.request(`/jobs/${MISSING}`, { headers: candidateHeaders() }), 404, 'NOT_FOUND');
  const response = await mock.request(`/jobs/${JOB}`, {
    headers: { ...candidateHeaders(), Origin: 'http://localhost:3000' },
  });
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('Access-Control-Allow-Origin'), 'http://localhost:3000');
  const job = await response.json();
  assert.equal(job.jobTitle, 'Java Developer');
  assert.equal(Object.hasOwn(job, 'hrEmails'), false);
  const pending = '10000000-0000-0000-0000-000000000002';
  await expectError(await mock.request(`/jobs/${pending}`, { headers: candidateHeaders() }), 404, 'NOT_FOUND');
  const expired = await mock.request('/jobs/10000000-0000-0000-0000-000000000003', { headers: candidateHeaders() });
  assert.equal((await expired.json()).acceptingApplications, false);
});

async function expectError(response, status, code) {
  assert.equal(response.status, status);
  const body = await response.json();
  assert.equal(body.code, code);
  assert.equal(typeof body.message, 'string');
  assert.ok(body.message.length > 0);
}

test('legacy: all 11 external endpoints, state and service-key authentication still work', async () => {
  await expectError(await mock.request('/lms/courses'), 401, 'INVALID_SERVICE_KEY');
  const headers = { 'X-Service-Key': 'legacy-key', 'Content-Type': 'application/json' };
  const post = (url, body) => mock.request(url, { method: 'POST', headers, body: JSON.stringify(body) });
  const ticketResponse = await post('/lms/sso/tickets', { lmsUserId: 'lms-sv-001' });
  assert.equal(ticketResponse.status, 201);
  const { ticket } = await ticketResponse.json();
  const verified = await post('/lms/sso/verify', { ticket });
  assert.equal(verified.status, 200);
  assert.equal((await verified.json()).lmsUserId, 'lms-sv-001');
  for (const url of ['/lms/students/lms-sv-001', '/lms/students/lms-sv-001/results', '/lms/courses?skill=JAVA',
    '/exam/students/lms-sv-001/results', '/pm/students/lms-sv-001/internships']) {
    assert.equal((await mock.request(url, { headers })).status, 200, url);
  }
  const enrollment = { lmsUserId: 'lms-sv-002', courseId: 'LMS-C-JAVA-02' };
  assert.equal((await post('/lms/enrollments', enrollment)).status, 201);
  await expectError(await post('/lms/enrollments', enrollment), 409, 'ALREADY_ENROLLED');
  const enrolled = await mock.request('/lms/enrollments/lms-sv-002', { headers });
  assert.equal((await enrolled.json())[0].courseId, enrollment.courseId);
  const member = { lmsUserId: 'lms-sv-002' };
  assert.equal((await post('/pm/batches/IOC-JB-2026-12/members', member)).status, 201);
  await expectError(await post('/pm/batches/IOC-JB-2026-12/members', member), 409, 'ALREADY_MEMBER');
  assert.equal((await post('/meet/rooms', { title: 'Interview', startAt: '2030-01-01T01:00:00Z', endAt: '2030-01-01T02:00:00Z' })).status, 201);
  const patched = await mock.request('/mock/enrollments', { method: 'PATCH', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...enrollment, progress: 100, status: 'COMPLETED' }) });
  assert.equal((await patched.json()).status, 'COMPLETED');
  assert.equal((await mock.request('/mock/reload', { method: 'POST' })).status, 200);
  assert.equal((await mock.request('/mock/reset', { method: 'POST' })).status, 200);
  assert.deepEqual(await (await mock.request('/mock/requests')).json(), []);
});

test('JD context uses the internal key and exact UT-01 response fields', async () => {
  const response = await mock.request(`/internal/jobs/${JOB}/application-context`, { headers: internalHeaders });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { jobId: JOB, companyId: '00000000-0000-0000-0000-0000000000a1',
    jobTitle: 'Java Developer', companyName: 'Rikkei Software', status: 'APPROVED',
    deadline: '2099-12-31T23:59:59Z', companyLocked: false, hrEmails: ['hr1@rikkeijobs.test'] });
});

test('JD rejects missing/wrong internal keys, legacy keys and candidate JWTs', async () => {
  for (const headers of [{}, { 'X-Internal-Key': 'wrong' }, { 'X-Service-Key': 'legacy-key' }, candidateHeaders()]) {
    await expectError(await mock.request(`/internal/jobs/${JOB}/application-context`, { headers }), 401, 'INVALID_INTERNAL_KEY');
  }
});

test('JD fixtures expose unapproved, expired and locked-company scenarios without hiding them', async () => {
  const fixtures = [
    ['10000000-0000-0000-0000-000000000002', body => assert.notEqual(body.status, 'APPROVED')],
    ['10000000-0000-0000-0000-000000000003', body => assert.ok(Date.parse(body.deadline) < Date.now())],
    ['10000000-0000-0000-0000-000000000004', body => assert.equal(body.companyLocked, true)],
  ];
  for (const [id, verify] of fixtures) {
    const response = await mock.request(`/internal/jobs/${id}/application-context`, { headers: internalHeaders });
    assert.equal(response.status, 200); verify(await response.json());
  }
});

test('JD returns contract errors for missing and malformed IDs', async () => {
  await expectError(await mock.request(`/internal/jobs/${MISSING}/application-context`, { headers: internalHeaders }), 404, 'NOT_FOUND');
  await expectError(await mock.request('/internal/jobs/not-a-uuid/application-context', { headers: internalHeaders }), 400, 'VALIDATION_ERROR');
});

test('CV lists are scoped to the JWT subject and expose only consumer fields', async () => {
  const response = await mock.request('/profile/cvs', { headers: candidateHeaders() });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), [
    { id: CV, name: 'CV Backend', targetPosition: 'Java Developer' },
    { id: '40000000-0000-0000-0000-000000000002', name: 'CV Frontend', targetPosition: 'React Developer' },
  ]);
  const other = await mock.request('/profile/cvs', { headers: candidateHeaders({ sub: OTHER }) });
  assert.deepEqual((await other.json()).map(cv => cv.id), [OTHER_CV]);
  const empty = await mock.request('/profile/cvs', { headers: candidateHeaders({ sub: EMPTY }) });
  assert.deepEqual(await empty.json(), []);
});

test('CV authentication rejects absent, forged, expired and invalid JWTs', async () => {
  const headers = [{}, internalHeaders, { Authorization: 'Bearer test-token' },
    { Authorization: `Bearer ${token({}, 'HS256', 'wrong-secret')}` },
    candidateHeaders({ exp: Math.floor(Date.now() / 1000) - 1 }), candidateHeaders({ exp: null }),
    candidateHeaders({ sub: 'lms-sv-001' }), candidateHeaders({ sub: [OWNER] }),
    candidateHeaders({ nbf: Math.floor(Date.now() / 1000) + 60 }),
    candidateHeaders({ nbf: 'tomorrow' }), { Authorization: `Bearer ${token({}, 'HS512')}` },
    { Authorization: `Bearer ${token({}, 'none')}` }];
  for (const value of headers) await expectError(await mock.request('/profile/cvs', { headers: value }), 401, 'UNAUTHORIZED');
});

test('CV authentication rejects non-candidates and password-change tokens', async () => {
  await expectError(await mock.request('/profile/cvs', { headers: candidateHeaders({ role: 'HR' }) }), 403, 'FORBIDDEN');
  await expectError(await mock.request('/profile/cvs', { headers: candidateHeaders({ mustChangePassword: true }) }), 403, 'MUST_CHANGE_PASSWORD');
});

test('CV PDF is a real private binary document and cannot be downloaded by another owner', async () => {
  const response = await mock.request(`/profile/cvs/${CV}/pdf`, { headers: candidateHeaders() });
  assert.equal(response.status, 200);
  assert.match(response.headers.get('content-type'), /^application\/pdf/);
  assert.match(response.headers.get('cache-control'), /no-store/);
  const bytes = Buffer.from(await response.arrayBuffer());
  assert.equal(bytes.subarray(0, 5).toString(), '%PDF-');
  assert.match(bytes.subarray(-64).toString(), /%%EOF/);
  await expectError(await mock.request(`/profile/cvs/${OTHER_CV}/pdf`, { headers: candidateHeaders() }), 403, 'FORBIDDEN');
  await expectError(await mock.request(`/profile/cvs/${MISSING}/pdf`, { headers: candidateHeaders() }), 404, 'NOT_FOUND');
  await expectError(await mock.request('/profile/cvs/invalid/pdf', { headers: candidateHeaders() }), 400, 'VALIDATION_ERROR');
  await expectError(await mock.request(`/profile/cvs/${CV}/pdf`), 401, 'UNAUTHORIZED');
});

test('browser CV routes support scoped CORS and the optional api/v1 alias', async () => {
  const response = await mock.request('/api/v1/profile/cvs', { headers: { ...candidateHeaders(), Origin: 'http://localhost:3000' } });
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('access-control-allow-origin'), 'http://localhost:3000');
  const preflight = await mock.request('/profile/cvs', { method: 'OPTIONS', headers: { Origin: 'http://localhost:3000',
    'Access-Control-Request-Method': 'GET', 'Access-Control-Request-Headers': 'authorization,content-type' } });
  assert.equal(preflight.status, 204);
  assert.match(preflight.headers.get('access-control-allow-headers'), /authorization/i);
  const denied = await mock.request('/profile/cvs', { headers: { ...candidateHeaders(), Origin: 'https://unrelated.example' } });
  assert.equal(denied.headers.get('access-control-allow-origin'), null);
  const legacy = await mock.request('/lms/courses', { headers: { 'X-Service-Key': 'legacy-key', Origin: 'http://localhost:3000' } });
  assert.equal(legacy.headers.get('access-control-allow-origin'), null);
});

test('local token CLI produces a signed token accepted by the shared CV API', async () => {
  const result = spawnSync(process.execPath, ['candidate-token.js', OWNER], { cwd: ROOT,
    env: { ...process.env, JWT_SECRET: SECRET }, encoding: 'utf8', windowsHide: true });
  assert.equal(result.status, 0, result.stderr);
  const response = await mock.request('/profile/cvs', { headers: { Authorization: `Bearer ${result.stdout.trim()}` } });
  assert.equal(response.status, 200);
  assert.equal((await response.json()).length, 2);
});

test('missing JWT configuration fails closed without disabling the external mocks', async () => {
  const unconfigured = await startMock({ JWT_SECRET: '' });
  try {
    await expectError(await unconfigured.request('/profile/cvs', { headers: candidateHeaders() }), 503, 'DEPENDENCY_NOT_CONFIGURED');
    assert.equal((await unconfigured.request('/lms/courses', { headers: { 'X-Service-Key': 'legacy-key' } })).status, 200);
  } finally { await unconfigured.close(); }
});

test('CV accepts the HS384/HS512 algorithms selected by real identity signing keys', async () => {
  for (const [algorithm, length] of [['HS384', 48], ['HS512', 64]]) {
    const secret = 'x'.repeat(length);
    const configured = await startMock({ JWT_SECRET: secret });
    try {
      const response = await configured.request('/profile/cvs', { headers: { Authorization: `Bearer ${token({}, algorithm, secret)}` } });
      assert.equal(response.status, 200, algorithm);
      assert.equal((await response.json()).length, 2);
    } finally { await configured.close(); }
  }
});

test('recruitment internal-key override takes priority and private PDF has no public static route', async () => {
  const configured = await startMock({ RECRUITMENT_COMPANY_SERVICE_KEY: 'override-key' });
  try {
    assert.equal((await configured.request(`/internal/jobs/${JOB}/application-context`, { headers: { 'X-Internal-Key': 'override-key' } })).status, 200);
    await expectError(await configured.request(`/internal/jobs/${JOB}/application-context`, { headers: internalHeaders }), 401, 'INVALID_INTERNAL_KEY');
    await expectError(await configured.request('/fixtures/cvs/sample.pdf', { headers: { 'X-Service-Key': 'legacy-key' } }), 404, 'ROUTE_NOT_FOUND');
  } finally { await configured.close(); }
});
