CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    about_me VARCHAR(500),
    role VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    admin BOOLEAN NOT NULL
);
INSERT INTO users (name, email, about_me, role, password, admin)
VALUES (
    'Admin User',
    'admin@example.com',
    'Administrator account',
    'ADMIN',
    '123',
    true
);