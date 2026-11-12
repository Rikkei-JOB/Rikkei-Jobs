// Token giả lập local, không tạo tài khoản trong identity-service.
const { createHmac } = require('node:crypto');

const secret = process.env.JWT_SECRET || '';
const subject = process.argv[2] || process.env.MOCK_CV_OWNER_ID || '30000000-0000-0000-0000-000000000001';
if (Buffer.byteLength(secret) < 32 || !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(subject)) {
  console.error('Cần JWT_SECRET dài ít nhất 32 byte và userId UUID: npm run token -- <userId>');
  process.exit(1);
}
// Giống JJWT: chọn thuật toán theo độ dài khoá HMAC.
const bytes = Buffer.byteLength(secret);
const [alg, hash] = bytes >= 64 ? ['HS512', 'sha512'] : bytes >= 48 ? ['HS384', 'sha384'] : ['HS256', 'sha256'];
const now = Math.floor(Date.now() / 1000);
const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const body = `${encode({ alg, typ: 'JWT' })}.${encode({ sub: subject, role: 'CANDIDATE', iat: now, exp: now + 3600 })}`;
console.log(`${body}.${createHmac(hash, secret).update(body).digest('base64url')}`);
