// TT-04: giả lập lms-pm gọi ngược sang backend Rikkei Jobs để lấy link phiếu đánh giá cho mentor.
// Cách chạy: node send-evaluation-links.js <batchId>      (hoặc: npm run evaluation-links -- <batchId>)
// Biến môi trường: BACKEND_URL (mặc định http://localhost:8080), SERVICE_KEY (mặc định dev-key)
const fs = require('fs');
const path = require('path');

const BACKEND_URL = (process.env.BACKEND_URL || 'http://localhost:8080').replace(/\/+$/, '');
const SERVICE_KEY = process.env.SERVICE_KEY || 'dev-key';
const ENDPOINT = `${BACKEND_URL}/api/v1/integrations/pm/evaluation-links`;

async function main() {
  const batchId = process.argv[2];
  if (!batchId) {
    console.error('Thiếu batchId. Ví dụ: node send-evaluation-links.js IOC-JB-2026-09');
    process.exit(1);
  }

  const db = JSON.parse(fs.readFileSync(path.join(__dirname, 'data.json'), 'utf8'));
  if (!(db.batches || []).some((b) => b.batchId === batchId)) {
    console.error(`Đợt ${batchId} không có trong data.json`);
    process.exit(1);
  }

  // Thực tập sinh của đợt: những ai có bản ghi thực tập với batchId này
  const interns = Object.entries(db.internships || {}).flatMap(([lmsUserId, list]) =>
    (list || []).filter((i) => i.batchId === batchId).map((i) => ({ lmsUserId, mentorEmail: i.mentorEmail, batchId })));

  if (interns.length === 0) {
    console.log(`Đợt ${batchId} chưa có thực tập sinh trong data.json, không có gì để gửi.`);
    return;
  }

  console.log(`Gửi ${interns.length} yêu cầu tới ${ENDPOINT}\n`);
  let failed = 0;
  for (const body of interns) {
    try {
      const res = await fetch(ENDPOINT, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Service-Key': SERVICE_KEY },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(5000),
      });
      const text = await res.text();
      let data;
      try { data = JSON.parse(text); } catch { data = text; }
      const link = data && typeof data === 'object' ? (data.link || data.url || data.evaluationLink) : undefined;
      if (!res.ok) failed++;
      console.log(`${body.lmsUserId} (mentor ${body.mentorEmail}) -> HTTP ${res.status}`);
      console.log(link ? `  Link: ${link}` : `  Phản hồi: ${typeof data === 'string' ? data : JSON.stringify(data)}`);
    } catch (err) {
      failed++;
      console.log(`${body.lmsUserId} -> không gọi được backend (${err.cause?.code || err.name}: ${err.message})`);
    }
  }
  if (failed) process.exitCode = 1;
}

main();
