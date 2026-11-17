-- DN-05: khoá tài khoản bởi cán bộ (locked_at khác null = khoá bởi cán bộ)
ALTER TABLE users ADD COLUMN locked_reason VARCHAR(500);
ALTER TABLE users ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE;
