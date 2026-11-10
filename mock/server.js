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
const requestLog = [];        // 100 request gần nhất

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

const findUser = (id) => (db.users || []).find((u) => u.lmsUserId === id);
const notFound = (res, what) => res.status(404).json({ code: 'NOT_FOUND', message: `${what} không tồn tại` });

// 1. Cấp vé (giả lập bấm nút từ LMS)
app.post('/lms/sso/tickets', (req, res) => {
  const user = findUser(req.body.lmsUserId);
  if (!user) return notFound(res, 'lmsUserId');
  const ticket = crypto.randomBytes(16).toString('hex');
  const issuedAt = new Date().toISOString();
  tickets.set(ticket, { lmsUserId: user.lmsUserId, issuedAt });
  res.status(201).json({ ticket, issuedAt });
});

// 2. Đổi vé lấy thông tin. KHÔNG chặn vé cũ/đã dùng: backend phải tự chặn.
app.post('/lms/sso/verify', (req, res) => {
  const t = tickets.get(req.body.ticket);
  if (!t) return notFound(res, 'ticket');
  const u = findUser(t.lmsUserId);
  res.json({ lmsUserId: u.lmsUserId, email: u.email, fullName: u.fullName, role: u.role, issuedAt: t.issuedAt });
});

// TODO 3–11: /lms/students, /lms/courses, /lms/enrollments, /exam, /pm, /meet (mục 3)

// Công cụ test
app.post('/mock/reload', (req, res) => { load(); res.json({ ok: true }); });
app.get('/mock/requests', (req, res) => res.json(requestLog));
// TODO: PATCH /mock/enrollments, POST /mock/reset (mục 5)

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