-- Merge legacy SUPER_ADMIN role into ADMIN
-- This migration keeps backward compatibility with old data while unifying role model.

-- Normalize user roles
UPDATE users
SET role = 'ADMIN'
WHERE UPPER(TRIM(role)) IN ('SUPER_ADMIN', 'SUPERADMIN', 'ROLE_SUPER_ADMIN');

-- Normalize message sender roles if old values exist
UPDATE messages
SET sender_role = 'ADMIN'
WHERE UPPER(TRIM(sender_role)) IN ('SUPER_ADMIN', 'SUPERADMIN', 'ROLE_SUPER_ADMIN');

-- Optional cleanup for ROLE_* prefixes from historical datasets
UPDATE users
SET role = REPLACE(UPPER(TRIM(role)), 'ROLE_', '')
WHERE UPPER(TRIM(role)) LIKE 'ROLE_%';

UPDATE messages
SET sender_role = REPLACE(UPPER(TRIM(sender_role)), 'ROLE_', '')
WHERE UPPER(TRIM(sender_role)) LIKE 'ROLE_%';
