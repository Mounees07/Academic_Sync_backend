-- Direct SQL fix to set ADMIN role for sankavi8881@gmail.com
-- Run this in MySQL Workbench or command line

USE academic_management;

-- Update the role to ADMIN for the specific email
UPDATE users 
SET role = 'ADMIN' 
WHERE email = 'sankavi8881@gmail.com';

-- Verify the change
SELECT id, firebase_uid, email, full_name, role 
FROM users 
WHERE email = 'sankavi8881@gmail.com';
