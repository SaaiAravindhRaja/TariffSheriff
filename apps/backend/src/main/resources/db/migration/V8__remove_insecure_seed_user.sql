-- Remove insecure seeded admin user with plaintext password
DELETE FROM users WHERE email = 'admin@example.com';

