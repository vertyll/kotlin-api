CREATE TABLE translation (
    message_key VARCHAR(200) PRIMARY KEY,
    message_pl TEXT NOT NULL,
    message_en TEXT NOT NULL,
    default_pl TEXT NOT NULL,
    default_en TEXT NOT NULL,
    customized BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
