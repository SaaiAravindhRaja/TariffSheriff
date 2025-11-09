# --- Frontend build ---
    FROM node:18-alpine AS fe
    WORKDIR /fe
    COPY apps/frontend/package*.json ./
    RUN npm ci
    # Vite build-time envs
    ARG VITE_API_BASE_URL
    ARG VITE_AUTH0_DOMAIN
    ARG VITE_AUTH0_CLIENT_ID
    ARG VITE_AUTH0_REDIRECT_URI
    ARG VITE_AUTH0_AUDIENCE
    ARG VITE_ENABLE_ANALYTICS
    ARG VITE_ENABLE_ERROR_REPORTING
    ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
        VITE_AUTH0_DOMAIN=$VITE_AUTH0_DOMAIN \
        VITE_AUTH0_CLIENT_ID=$VITE_AUTH0_CLIENT_ID \
        VITE_AUTH0_REDIRECT_URI=$VITE_AUTH0_REDIRECT_URI \
        VITE_AUTH0_AUDIENCE=$VITE_AUTH0_AUDIENCE \
        VITE_ENABLE_ANALYTICS=$VITE_ENABLE_ANALYTICS \
        VITE_ENABLE_ERROR_REPORTING=$VITE_ENABLE_ERROR_REPORTING
    COPY apps/frontend/ ./
    RUN npm run build
    
    # --- Backend build ---
    FROM maven:3.9-eclipse-temurin-17 AS be
    WORKDIR /workspace
    COPY apps/backend/pom.xml ./pom.xml
    RUN mvn -B -q -e -DskipTests dependency:go-offline
    COPY apps/backend/src ./src
    RUN mvn -B -q -DskipTests package
    
    # --- Runtime ---
    FROM eclipse-temurin:17-jre
    WORKDIR /app
    ENV JAVA_OPTS=""
    # Serve the built frontend from /app/static
    ENV SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/static/
    COPY --from=be /workspace/target/backend-*.jar /app/app.jar
    COPY --from=fe /fe/dist /app/static
    EXPOSE 8080
    ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]