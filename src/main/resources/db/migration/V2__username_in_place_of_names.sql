ALTER TABLE "user" ADD COLUMN username VARCHAR(255);
UPDATE "user" SET username = email;
ALTER TABLE "user" ALTER COLUMN username SET NOT NULL;
ALTER TABLE "user" ADD CONSTRAINT uk_user_username UNIQUE (username);
ALTER TABLE "user" DROP COLUMN first_name;
ALTER TABLE "user" DROP COLUMN last_name;
