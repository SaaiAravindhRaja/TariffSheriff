# 🔐 TariffSheriff Authentication System Documentation

## Overview
TariffSheriff uses a comprehensive JWT-based authentication system with Spring Security. This document covers everything you need to know about integrating, using, and maintaining the authentication system.

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   (React)       │    │ (Spring Boot)   │    │  (PostgreSQL)   │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │Auth Context │ │◄──►│ │Auth         │ │◄──►│ │Users Table  │ │
│ │JWT Storage  │ │    │ │Controller   │ │    │ │Sessions     │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │ ┌─────────────┐ │    │                 │
│ ┌─────────────┐ │    │ │JWT Filter   │ │    │ ┌─────────────┐ │
│ │API Client   │ │◄──►│ │Security     │ │◄──►│ │Redis Cache  │ │
│ │Interceptor  │ │    │ │Config       │ │    │ │Token        │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ │Blacklist    │ │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📁 File Structure & Components

### Core Authentication Files:

```
apps/backend/src/main/java/com/tariffsheriff/backend/
├── config/
│   ├── SecurityConfig.java              # Main security configuration
│   ├── JwtConfig.java                   # JWT settings & secret key
│   ├── SecurityProperties.java          # Security properties
│   └── OpenApiConfig.java               # Swagger auth config
├── security/
│   └── jwt/
│       ├── JwtUtil.java                 # JWT creation & validation
│       ├── JwtAuthenticationFilter.java # Request interceptor
│       ├── JwtAuthenticationEntryPoint.java # Unauthorized handler
│       └── TokenService.java            # High-level token operations
├── user/
│   ├── controller/
│   │   └── AuthController.java          # Auth endpoints
│   ├── service/
│   │   ├── AuthenticationService.java   # Auth business logic
│   │   ├── UserService.java             # User management
│   │   └── CustomUserDetailsService.java # Spring Security integration
│   ├── model/
│   │   ├── User.java                    # User entity
│   │   ├── UserRole.java                # Role enum
│   │   └── UserStatus.java              # Status enum
│   └── dto/
│       ├── LoginRequest.java            # Login payload
│       ├── RegisterRequest.java         # Registration payload
│       ├── AuthResponse.java            # Auth response
│       └── UserDto.java                 # User data transfer
└── resources/
    ├── application.properties           # JWT secrets & config
    ├── application-dev.properties       # Dev environment config
    └── application-prod.properties      # Production config
```

## 🔑 JWT Configuration

### Environment Variables Required:
```bash
# Required for production
JWT_SECRET=your-super-secret-jwt-signing-key-here-minimum-256-bits

# Optional - defaults provided
JWT_ACCESS_TOKEN_EXPIRATION=900000      # 15 minutes (in milliseconds)
JWT_REFRESH_TOKEN_EXPIRATION=604800000  # 7 days (in milliseconds)

# Database connection
DATABASE_URL=jdbc:postgresql://localhost:5432/tariffsheriff
DATABASE_USERNAME=tariff_sheriff
DATABASE_PASSWORD=tariff_sheriff
```

### Application Properties:
```properties
# JWT Configuration
jwt.secret=${JWT_SECRET:mySecretKey}
jwt.access-token-expiration=${JWT_ACCESS_TOKEN_EXPIRATION:900000}
jwt.refresh-token-expiration=${JWT_REFRESH_TOKEN_EXPIRATION:604800000}

# CORS Configuration
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS,PATCH
app.cors.allowed-headers=*
app.cors.allow-credentials=true
```

## 🚀 Authentication Endpoints

### Base URL: `/api/auth`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/register` | User registration | ❌ |
| POST | `/login` | User login | ❌ |
| POST | `/logout` | User logout | ✅ |
| POST | `/refresh` | Refresh access token | ❌ |
| GET | `/verify?token=` | Email verification | ❌ |
| POST | `/forgot-password` | Request password reset | ❌ |
| POST | `/reset-password` | Reset password with token | ❌ |
| GET | `/me` | Get current user info | ✅ |
| POST | `/change-password` | Change password | ✅ |

### 📝 Request/Response Examples:

#### 1. User Registration
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "confirmPassword": "SecurePass123!",
    "aboutMe": "Software developer"
  }'
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration successful. Please check your email to verify your account.",
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "USER",
    "status": "PENDING",
    "emailVerified": false,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

#### 2. User Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePass123!"
  }'
```

**Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 900,
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "USER",
    "status": "ACTIVE",
    "emailVerified": true,
    "lastLogin": "2024-01-15T10:30:00"
  }
}
```

#### 3. Accessing Protected Endpoints
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 4. Token Refresh
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

## 🔒 Security Features

### 1. **JWT Token Structure**
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user@example.com",
    "userId": 1,
    "roles": "USER",
    "tokenType": "access",
    "jti": "unique-token-id",
    "iat": 1642234567,
    "exp": 1642235467
  }
}
```

### 2. **Role-Based Access Control**
```java
// Roles available in the system
public enum UserRole {
    USER,      // Standard user access
    ANALYST,   // Enhanced tariff analysis access
    ADMIN      // Full system administration
}
```

### 3. **Protected Endpoints by Role**
```java
// User management endpoints
.requestMatchers("/api/user/profile").hasAnyRole("USER", "ANALYST", "ADMIN")
.requestMatchers("/api/user/update-profile").hasAnyRole("USER", "ANALYST", "ADMIN")
.requestMatchers("/api/user/list").hasAnyRole("ANALYST", "ADMIN")
.requestMatchers("/api/user/*/role").hasRole("ADMIN")
.requestMatchers("/api/user/*/status").hasRole("ADMIN")
.requestMatchers("/api/user/admin/**").hasRole("ADMIN")
```

### 4. **Token Blacklisting**
- Tokens are blacklisted on logout
- Redis-based blacklist for performance
- Automatic cleanup of expired blacklisted tokens

### 5. **Security Headers**
```java
// Configured in SecurityConfig.java
.headers(headers -> headers
    .frameOptions().deny()
    .contentSecurityPolicy(
        "default-src 'self'; " +
        "script-src 'self' 'unsafe-inline'; " +
        "style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data: https:; " +
        "font-src 'self' data:; " +
        "connect-src 'self'; " +
        "frame-ancestors 'none'"
    )
)
```

## 🧪 Testing Authentication

### 1. **Start the Backend**
```bash
cd apps/backend
./mvnw spring-boot:run
```

### 2. **Test Endpoints**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"Test123!","confirmPassword":"Test123!"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!"}'

# Use the access_token from login response in Authorization header
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```

## 🐛 Common Issues & Troubleshooting

### Issue 1: "JWT Secret is too short"
**Problem:** JWT secret key must be at least 256 bits (32 characters)
**Solution:** 
```bash
export JWT_SECRET="your-super-secret-jwt-signing-key-here-minimum-256-bits"
```

### Issue 2: "CORS Error from Frontend"
**Problem:** CORS policy blocking frontend requests
**Solution:** Update `application.properties`:
```properties
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

### Issue 3: "Token Expired" errors
**Problem:** Short access token lifetime (15 minutes)
**Solution:** Use refresh token or extend expiry:
```properties
jwt.access-token-expiration=3600000  # 1 hour
```

### Issue 4: "Database connection refused"
**Problem:** PostgreSQL not running or wrong credentials
**Solution:**
```bash
# Start PostgreSQL with Docker
docker run -d --name postgres \
  -e POSTGRES_USER=tariff_sheriff \
  -e POSTGRES_PASSWORD=tariff_sheriff \
  -e POSTGRES_DB=tariffsheriff \
  -p 5432:5432 postgres:15

# Or update connection string
export DATABASE_URL="jdbc:postgresql://localhost:5432/tariffsheriff"
```

### Issue 5: "Multiple SecurityConfig beans"
**Problem:** Duplicate security configurations
**Solution:** There are two SecurityConfig files - one is disabled:
- `/config/SecurityConfig.java` (ACTIVE) - Main JWT security
- `/user/config/SecurityConfig.java` (DISABLED) - Test configuration

## 🔧 Frontend Integration

### 1. **Store JWT Token**
```javascript
// After successful login
const authResponse = await loginUser(credentials);
localStorage.setItem('access_token', authResponse.access_token);
localStorage.setItem('refresh_token', authResponse.refresh_token);
```

### 2. **Add Authorization Header**
```javascript
// In your API client (axios example)
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api'
});

// Request interceptor to add auth header
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Try to refresh token
      const refreshToken = localStorage.getItem('refresh_token');
      if (refreshToken) {
        try {
          const response = await axios.post('/api/auth/refresh', {
            refreshToken: refreshToken
          });
          
          localStorage.setItem('access_token', response.data.access_token);
          localStorage.setItem('refresh_token', response.data.refresh_token);
          
          // Retry original request
          return apiClient(error.config);
        } catch (refreshError) {
          // Refresh failed, redirect to login
          localStorage.clear();
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);
```

### 3. **React Auth Context**
```jsx
// AuthContext.jsx
import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check for existing token on app start
    const token = localStorage.getItem('access_token');
    if (token) {
      fetchCurrentUser();
    } else {
      setLoading(false);
    }
  }, []);

  const fetchCurrentUser = async () => {
    try {
      const response = await apiClient.get('/auth/me');
      setUser(response.data);
    } catch (error) {
      localStorage.clear();
    } finally {
      setLoading(false);
    }
  };

  const login = async (credentials) => {
    const response = await apiClient.post('/auth/login', credentials);
    localStorage.setItem('access_token', response.data.access_token);
    localStorage.setItem('refresh_token', response.data.refresh_token);
    setUser(response.data.user);
    return response.data;
  };

  const logout = async () => {
    try {
      await apiClient.post('/auth/logout');
    } catch (error) {
      // Even if logout fails on server, clear local state
    } finally {
      localStorage.clear();
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

## 📊 Database Schema

### Users Table (V4__create_users.sql):
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    about_me VARCHAR(500),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    password VARCHAR(255) NOT NULL,
    admin BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_expires TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked_until TIMESTAMP,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_verification_token ON users(verification_token);
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token);
```

## 🚀 Quick Start Guide

### 1. **Environment Setup**
```bash
# Set required environment variables
export JWT_SECRET="your-super-secret-jwt-signing-key-here-minimum-256-bits"
export DATABASE_URL="jdbc:postgresql://localhost:5432/tariffsheriff"
export DATABASE_USERNAME="tariff_sheriff"
export DATABASE_PASSWORD="tariff_sheriff"
```

### 2. **Start Database**
```bash
docker run -d --name tariffsheriff-postgres \
  -e POSTGRES_USER=tariff_sheriff \
  -e POSTGRES_PASSWORD=tariff_sheriff \
  -e POSTGRES_DB=tariffsheriff \
  -p 5432:5432 postgres:15
```

### 3. **Start Backend**
```bash
cd apps/backend
./mvnw spring-boot:run
```

### 4. **Start Frontend**
```bash
cd apps/frontend
npm run dev
```

### 5. **Test Authentication**
- Navigate to `http://localhost:3000`
- Register a new account
- Login with credentials
- Access protected pages

## 📚 API Documentation
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

## 🔗 Related Files
- [User Entity](apps/backend/src/main/java/com/tariffsheriff/backend/user/model/User.java)
- [Security Config](apps/backend/src/main/java/com/tariffsheriff/backend/config/SecurityConfig.java)
- [Auth Controller](apps/backend/src/main/java/com/tariffsheriff/backend/user/controller/AuthController.java)
- [JWT Configuration](apps/backend/src/main/java/com/tariffsheriff/backend/config/JwtConfig.java)
- [Database Migration](apps/backend/src/main/resources/db/migration/V4__create_users.sql)

---

**Need Help?** Check the troubleshooting section above or review the test files in `src/test/java/com/tariffsheriff/backend/security/` for more examples.