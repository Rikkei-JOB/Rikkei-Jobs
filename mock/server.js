const express = require('express');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = process.env.PORT || 9000;
const SERVICE_KEY = process.env.SERVICE_KEY || 'dev-key';
const DATA_FILE = path.join(__dirname, 'data.json');

let db = {};                  // dữ liệu cố định từ data.json
const tickets = new Map();    // ticket -> { lmsUserId, issuedAt }
const enrollments = [];       // { lmsUserId, courseId, progress, score, status, enrolledAt }
const batchMembers = [];      // { batchId, lmsUserId, addedAt }
const meetRooms = [];         // { roomId, roomUrl, title, startAt, endAt }
const requestLog = [];        // 100 request gần nhất

const ENROLLMENT_STATUSES = ['ENROLLED', 'IN_PROGRESS', 'COMPLETED'];

const load = () => { db = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')); };
load();

const app = express();
app.use(express.json());

// Ghi nhật ký mọi request (trừ công cụ /mock/*)
app.use((req, res, next) => {
  if (!req.path.startsWith('/mock/')) {
    requestLog.unshift({
      at: new Date().toISOString(),
      method: req.method,
      path: req.originalUrl,
      hasKey: req.get('X-Service-Key') === SERVICE_KEY,
      body: req.body,
    });
    requestLog.length = Math.min(requestLog.length, 100);
  }
  next();
});

// Kiểm khoá dịch vụ (trừ công cụ /mock/*)
app.use((req, res, next) => {
  if (req.path.startsWith('/mock/')) return next();
  if (req.get('X-Service-Key') !== SERVICE_KEY) {
    return res.status(401).json({ code: 'INVALID_SERVICE_KEY', message: 'Thiếu hoặc sai X-Service-Key' });
  }
  next();
});

// ---------- Hàm hỗ trợ ----------
const findUser = (id) => (db.users || []).find((u) => u.lmsUserId === id);
const findCourse = (id) => (db.courses || []).find((c) => c.courseId === id);
const findBatch = (id) => (db.batches || []).find((b) => b.batchId === id);
const listOf = (group, id) => ((db[group] || {})[id]) || [];   // API danh sách trả [] khi không có dữ liệu

const notFound = (res, what) => res.status(404).json({ code: 'NOT_FOUND', message: `${what} không tồn tại` });
const badRequest = (res, message) => res.status(400).json({ code: 'VALIDATION_ERROR', message });
const conflict = (res, code, message) => res.status(409).json({ code, message });

const isNonEmptyString = (v) => typeof v === 'string' && v.trim() !== '';
const missingFields = (body, fields) => fields.filter((f) => !isNonEmptyString((body || {})[f]));
const isIsoDate = (v) => isNonEmptyString(v) && !Number.isNaN(Date.parse(v));

// ---------- LMS ----------

// 1. Cấp vé (giả lập bấm nút từ LMS)
app.post('/lms/sso/tickets', (req, res) => {
  const user = findUser((req.body || {}).lmsUserId);
  if (!user) return notFound(res, 'lmsUserId');
  const ticket = crypto.randomBytes(16).toString('hex');
  const issuedAt = new Date().toISOString();
  tickets.set(ticket, { lmsUserId: user.lmsUserId, issuedAt });
  res.status(201).json({ ticket, issuedAt });
});

// 2. Đổi vé lấy thông tin. KHÔNG chặn vé cũ/đã dùng: backend phải tự chặn.
app.post('/lms/sso/verify', (req, res) => {
  const t = tickets.get((req.body || {}).ticket);
  if (!t) return notFound(res, 'ticket');
  const u = findUser(t.lmsUserId);
  if (!u) return notFound(res, 'lmsUserId');   // user bị xoá khỏi data.json sau khi cấp vé
  res.json({ lmsUserId: u.lmsUserId, email: u.email, fullName: u.fullName, role: u.role, issuedAt: t.issuedAt });
});

// 3. Hồ sơ học viên (chỉ CANDIDATE)
app.get('/lms/students/:lmsUserId', (req, res) => {
  const u = findUser(req.params.lmsUserId);
  if (!u || u.role !== 'CANDIDATE') return notFound(res, 'Học viên');
  const { lmsUserId, fullName, email, program, classCode, courseName } = u;
  res.json({ lmsUserId, fullName, email, program, classCode, courseName });
});

// 4. Kết quả môn học
app.get('/lms/students/:lmsUserId/results', (req, res) => {
  res.json(listOf('lmsResults', req.params.lmsUserId));
});

// 5. Danh sách khoá học, lọc theo mã kỹ năng: ?skill=JAVA
app.get('/lms/courses', (req, res) => {
  const courses = db.courses || [];
  const skill = typeof req.query.skill === 'string' ? req.query.skill.trim().toUpperCase() : '';
  res.json(skill ? courses.filter((c) => (c.skills || []).includes(skill)) : courses);
});

// 6. Ghi danh khoá học
app.post('/lms/enrollments', (req, res) => {
  const missing = missingFields(req.body, ['lmsUserId', 'courseId']);
  if (missing.length) return badRequest(res, `Thiếu trường: ${missing.join(', ')}`);
  const { lmsUserId, courseId } = req.body;
  if (!findUser(lmsUserId)) return notFound(res, 'lmsUserId');
  if (!findCourse(courseId)) return notFound(res, 'courseId');
  if (enrollments.some((e) => e.lmsUserId === lmsUserId && e.courseId === courseId)) {
    return conflict(res, 'ALREADY_ENROLLED', 'Học viên đã ghi danh khoá học này');
  }
  const enrollment = { lmsUserId, courseId, progress: 0, score: null, status: 'ENROLLED', enrolledAt: new Date().toISOString() };
  enrollments.push(enrollment);
  res.status(201).json(enrollment);
});

// 7. Danh sách ghi danh của một học viên
app.get('/lms/enrollments/:lmsUserId', (req, res) => {
  res.json(enrollments.filter((e) => e.lmsUserId === req.params.lmsUserId));
});

// ---------- Khảo thí ----------

// 8. Điểm thi
app.get('/exam/students/:lmsUserId/results', (req, res) => {
  res.json(listOf('examResults', req.params.lmsUserId));
});

// ---------- lms-pm ----------

// 9. Lịch sử thực tập
app.get('/pm/students/:lmsUserId/internships', (req, res) => {
  res.json(listOf('internships', req.params.lmsUserId));
});

// 10. Thêm thành viên vào đợt thực tập
app.post('/pm/batches/:batchId/members', (req, res) => {
  const { batchId } = req.params;
  const missing = missingFields(req.body, ['lmsUserId']);
  if (missing.length) return badRequest(res, 'Thiếu trường: lmsUserId');
  const { lmsUserId } = req.body;
  if (!findBatch(batchId)) return notFound(res, 'batchId');
  if (!findUser(lmsUserId)) return notFound(res, 'lmsUserId');
  const inFixture = listOf('internships', lmsUserId).some((i) => i.batchId === batchId);
  const added = batchMembers.some((m) => m.batchId === batchId && m.lmsUserId === lmsUserId);
  if (inFixture || added) return conflict(res, 'ALREADY_MEMBER', 'Học viên đã thuộc đợt này');
  const member = { batchId, lmsUserId, addedAt: new Date().toISOString() };
  batchMembers.push(member);
  res.status(201).json(member);
});

// ---------- Rikkei Meet ----------

// 11. Tạo phòng họp
app.post('/meet/rooms', (req, res) => {
  const missing = missingFields(req.body, ['title', 'startAt', 'endAt']);
  if (missing.length) return badRequest(res, `Thiếu trường: ${missing.join(', ')}`);
  const { title, startAt, endAt } = req.body;
  if (!isIsoDate(startAt) || !isIsoDate(endAt)) return badRequest(res, 'startAt, endAt phải là thời gian ISO 8601');
  if (Date.parse(endAt) <= Date.parse(startAt)) return badRequest(res, 'endAt phải sau startAt');
  const roomId = `room-${crypto.randomBytes(4).toString('hex')}`;
  const room = { roomId, roomUrl: `https://meet.rikkei.test/r/${roomId}`, title, startAt, endAt };
  meetRooms.push(room);
  res.status(201).json(room);
});

// ---------- Công cụ test (không cần khoá, backend không gọi) ----------

app.post('/mock/reload', (req, res) => { load(); res.json({ ok: true }); });

app.get('/mock/requests', (req, res) => res.json(requestLog));

// Sửa progress / score / status của một lượt ghi danh: {lmsUserId, courseId, progress?, score?, status?}
app.patch('/mock/enrollments', (req, res) => {
  const body = req.body || {};
  const e = enrollments.find((x) => x.lmsUserId === body.lmsUserId && x.courseId === body.courseId);
  if (!e) return notFound(res, 'Lượt ghi danh');
  if (body.progress !== undefined && !(Number.isFinite(body.progress) && body.progress >= 0 && body.progress <= 100)) {
    return badRequest(res, 'progress phải là số từ 0 đến 100');
  }
  if (body.score !== undefined && body.score !== null && !(Number.isFinite(body.score) && body.score >= 0 && body.score <= 10)) {
    return badRequest(res, 'score phải là null hoặc số từ 0 đến 10');
  }
  if (body.status !== undefined && !ENROLLMENT_STATUSES.includes(body.status)) {
    return badRequest(res, `status phải là một trong: ${ENROLLMENT_STATUSES.join(', ')}`);
  }
  ['progress', 'score', 'status'].forEach((f) => { if (body[f] !== undefined) e[f] = body[f]; });
  res.json(e);
});

// Xoá vé, ghi danh, thành viên đợt, phòng họp và nhật ký request trong bộ nhớ
app.post('/mock/reset', (req, res) => {
  tickets.clear();
  enrollments.length = 0;
  batchMembers.length = 0;
  meetRooms.length = 0;
  requestLog.length = 0;
  res.json({ ok: true });
});

// Đường dẫn không tồn tại → 404 JSON (đặt sau mọi route)
app.use((req, res) => {
  res.status(404).json({ code: 'ROUTE_NOT_FOUND', message: `Không có endpoint ${req.method} ${req.originalUrl}` });
});

// Xử lý lỗi chung (bắt buộc đủ 4 tham số): body JSON hỏng → 400, lỗi khác → 500
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json({ code: 'INVALID_JSON', message: 'Body không phải JSON hợp lệ' });
  }
  console.error(err);
  res.status(500).json({ code: 'INTERNAL_ERROR', message: 'Lỗi không mong muốn trong mock' });
});

app.listen(PORT, () => console.log(`Mock chạy ở http://localhost:${PORT}`));
