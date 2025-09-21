

````markdown
# TariffSheriff – User API (Test Setup)

This is a **test setup** for experimenting with the User API in the TariffSheriff project.  
It includes database setup, migration, security configuration, and example `curl` commands.

---

##  Database Setup (Docker)

If you don’t have PostgreSQL running yet, start a container:

```bash
docker run -d \
  --name tariffsheriff-postgres \
  -e POSTGRES_USER=tariff_sheriff \
  -e POSTGRES_PASSWORD=tariff_sheriff \
  -e POSTGRES_DB=tariffsheriff \
  -p 5432:5432 \
  postgres:16
````

---

##  Database Migration

Inside `src/main/resources/db/migration`, create a file:

**`V4__create_users.sql`**

```sql
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
    '$2a$12$0jS8DjLqfkBmxRonGT2vd.gWPYzyxnhbMoebvcWQsOLqGJ6ckqrYy',
    true
);
```

> The password here is **`secret123`** (hashed with BCrypt).

---

##  Security Configuration

In `user/config/SecurityConfig.java`:

* Currently, **security is disabled** for all endpoints.
* To enable **Basic Authentication**, uncomment the method that configures `HttpSecurity`.

When enabled:

* Username: `admin@example.com`
* Password: `secret123`

---

## Example API Calls

### 1. Create a User

```bash
curl -X POST http://localhost:8080/api/users \
  -u admin@example.com:secret123 \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Beanny",
        "email": "beanny@example.com",
        "aboutMe": "test account",
        "role": "USER",
        "password": "mypassword",
        "admin": false
      }'
```

### 2. Get All Users

```bash
curl -u admin@example.com:secret123 http://localhost:8080/api/users
```

### 3. Get User by ID

```bash
curl -u admin@example.com:secret123 http://localhost:8080/api/users/1
```

---

## Notes

* The `-u admin@example.com:secret123` flag is **only required if security is enabled**.
* With security disabled, you can call the endpoints without authentication.
* This setup is for **local testing only** (not production-ready).

```

---

