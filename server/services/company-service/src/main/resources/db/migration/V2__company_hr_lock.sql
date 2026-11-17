-- Bản sao thời điểm cán bộ khoá tài khoản HR (nguồn sự thật ở identity); khác null nghĩa là bị khoá.
ALTER TABLE company_hr ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE;
