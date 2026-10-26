# HSSC — High School STEM Championship

This repository contains a production-oriented full-stack platform for the HSSC competition.

## Project structure

- `/backend` — Java 21 + Spring Boot + Spring Security + PostgreSQL API
- `/frontend` — React + TypeScript + Vite + SCSS + React Router UI

## Core platform features

- School registration and secure authentication
- School registration accepts a school-linked roster of participating students, including uniform photo, name, gender, and age
- An approved school-account holder can create one student identity for themselves using the same login, with their own student number, name, age, and gender; exam attempts and results are attributed only to that account holder, not to rostered students
- Schools can view their submitted registration and participating-student roster in a school-specific dashboard; administrators review the same records from school details
- New school accounts remain pending until administrator approval; the registration screen confirms that approval is required, and pending school login attempts receive an actionable review message
- Student registration and secure authentication
- District/zone hierarchy
- Admin dashboard with database-backed District → Zone → School review, school/student details, and school approval actions
- Exam portal controls per district
- Question-bank architecture with configurable import and validation
- Administrator loading of the official 400-question Set A and Set B banks; scored exams are blocked until every question in the chosen bank has an answer key
- Server-authoritative exam workflow with timer, answer persistence, and answer submission
- Result publication and leaderboard qualification workflow
- Persistent audit logging and database-backed notifications

## Prerequisites

- Java 21+
- Maven 3.10+
- PostgreSQL 15+
- Node.js 20+
- npm 10+

## Backend setup

1. Create a PostgreSQL database, for example:

   ```sql
   CREATE DATABASE hssc;
   CREATE USER hssc WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE hssc TO hssc;
   ```

2. Configure backend environment variables. Example:

   ```bash
   export DATABASE_URL=jdbc:postgresql://localhost:5432/hssc
   export DATABASE_USERNAME=hssc
   export DATABASE_PASSWORD=your_password
   export JWT_SECRET=<base64-encoded 32+ byte secret>
   export JWT_ISSUER=hssc-api
   export JWT_EXPIRATION_MINUTES=60
   export CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
   export HSSC_ADMIN_EMAIL=admin@example.com
   export HSSC_ADMIN_PASSWORD=ChangeMe!StrongPassword
   export HSSC_EXAM_DURATION_MINUTES=40
   export HSSC_EXAM_MARKS_PER_CORRECT_ANSWER=2.5
   export HSSC_DISTRICT_ZONES="I=District I|Agege,Alimosho,Ifako-Ijaiye;II=District II|Ikorodu,Kosofe,Somolu;III=District III|Epe,Eti-Osa,Ibeju-Lekki,Lagos Island;IV=District IV|Apapa,Mainland,Surulere;V=District V|Ajeromi-Ifelodun,Amuwo-Odofin,Badagry,Ojo;VI=District VI|Ikeja,Mushin,Oshodi-Isolo"
   export UPLOAD_DIRECTORY=./var/uploads
   ```

   Set B's supplied organiser copy currently contains answers for only questions 1–200; Set A's copy has no answer key. Both banks can be loaded for preparation, but neither can be used for a scored exam until all 400 answers for that set are available.

3. Run the backend:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   Or build and run a packaged jar:

   ```bash
   cd backend
   mvn clean package
   java -jar target/hssc-backend-1.0.0.jar
   ```

## Frontend setup

1. Install dependencies:

   ```bash
   cd frontend
   npm install
   ```

2. Copy the sample environment file:

   ```bash
   cp .env.example .env
   ```

3. Set the backend API URL in `.env`:

   ```env
   VITE_API_URL=http://localhost:8080/api
   ```

4. Start the app:

   ```bash
   npm run dev -- --host 0.0.0.0
   ```

5. Production build:

   ```bash
   npm run build
   ```

## Database and migration expectations

- Flyway runs on application startup.
- The backend validates the database schema rather than generating it blindly.
- Exam duration and marks per correct answer are configured with `HSSC_EXAM_DURATION_MINUTES` and `HSSC_EXAM_MARKS_PER_CORRECT_ANSWER`. New exams store a copy of the marking rate so published scoring remains consistent if configuration changes later.
- Flyway migration V9 converts stored attempt marks to decimal values and adds the exam marking rate. Migration V10 adds individual result release and leaderboard publication records.
- District and zone seed data may be supplied through `HSSC_DISTRICT_ZONES`.
- Actual competition question banks and official exam content are intentionally not fabricated. The system is structured to import those later without rebuilding the platform.

## Creating the initial admin account

The initial admin account is created from environment variables on startup:

- `HSSC_ADMIN_EMAIL`
- `HSSC_ADMIN_PASSWORD`

If both are set, the application creates or ensures an admin user with those credentials. If either is omitted, no bootstrap admin is created.

## Question bank import and future usage

The platform includes a question-bank architecture and import-ready form, but actual HSSC question data is intentionally not populated in code. To add a real bank later:

1. Create a question bank in the admin interface or through the API.
2. Import normal and anonymous question records in the configured format.
3. Assign the bank to a district and validate sufficient subject quotas before opening the portal.

## Local run overview

Open the frontend at:

- `http://localhost:5173`

The API listens at:

- `http://localhost:8080/api`

## Production deployment notes

- Use environment variables for secrets and config.
- Set `CORS_ALLOWED_ORIGINS` to the exact frontend origins that should be allowed to call the API (comma-separated; do not use `*` for production).
- Keep PostgreSQL and upload storage private.
- Use TLS at the edge and rotate JWT secrets.
- Restrict admin endpoints to trusted roles only.
- Keep uploads outside the public web root.
- Set production reload behavior and monitoring as appropriate for your hosting provider.

## Validation performed

- Frontend build passes with `npm run build`.
- Backend test suite passes with `mvn test`.
