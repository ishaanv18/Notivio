# Notivio Backend — Production-Ready AI Gmail Reminder System

## Project Overview

Notivio is a production-ready Spring Boot 3 + Java 21 backend that:
- Connects to Gmail via OAuth2
- Uses AI (Groq LLaMA3 + OpenRouter fallback) to extract deadlines from emails
- Auto-creates reminders at 1 day, 6 hours, 1 hour, and 15 minutes before deadlines
- Sends push notifications via Firebase Cloud Messaging (FCM)
- Syncs with Google Calendar
- Stays alive 24/7 on free hosting tiers

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2.5, Java 21 |
| Database | PostgreSQL 16 (Supabase / Docker) |
| Auth | Google OAuth2 + JWT (JJWT 0.12.5) |
| AI Primary | Groq API (LLaMA3-8b) |
| AI Fallback | OpenRouter API |
| Notifications | Firebase Cloud Messaging (FCM) |
| Email Sync | Gmail API v1 |
| Calendar | Google Calendar API v3 |
| Build | Maven 3.9 |
| Deployment | Docker + Docker Compose |

---

## Quick Start

### 1. Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Google Cloud Project (for OAuth2 + Gmail + Calendar APIs)
- Groq API key (free at console.groq.com)

### 2. Clone & Configure
```bash
git clone <repo-url>
cd notivio

# Copy and fill in your environment variables
cp .env.example .env
nano .env
```

### 3. Google Cloud Setup
1. Go to https://console.cloud.google.com
2. Create a new project called "Notivio"
3. Enable these APIs:
   - Gmail API
   - Google Calendar API
   - Google People API
4. Create OAuth 2.0 Credentials:
   - Type: Web Application
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
5. Copy Client ID and Client Secret to `.env`

### 4. Run with Docker Compose
```bash
docker-compose up -d
```

### 5. Run locally with Maven
```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

---

## API Endpoints

### Authentication
```
GET  /oauth2/authorization/google   → Initiate Google OAuth2 login
GET  /api/auth/me                   → Get current user profile
GET  /api/auth/login                → Get login URL info
```

### Gmail
```
GET  /api/gmail/status              → Gmail connection status
POST /api/gmail/sync                → Manually trigger email sync
DELETE /api/gmail/disconnect        → Disconnect Gmail
```

### Tasks
```
GET    /api/tasks                   → All tasks (paginated)
GET    /api/tasks/upcoming?days=7   → Upcoming deadlines
GET    /api/tasks/overdue           → Overdue tasks
GET    /api/tasks/completed         → Completed tasks
GET    /api/tasks/stats             → Task statistics
GET    /api/tasks/{id}              → Get task by ID
PATCH  /api/tasks/{id}/complete     → Mark as complete
PUT    /api/tasks/{id}              → Update task
DELETE /api/tasks/{id}              → Delete task
```

### Notifications
```
POST   /api/notifications/device-token    → Register FCM device token
DELETE /api/notifications/device-token   → Remove device token
GET    /api/notifications                → Notification history
POST   /api/notifications/mark-read      → Mark all as read
GET    /api/notifications/unread-count   → Unread count
```

### Health (Public)
```
GET /health           → Overall health
GET /health/db        → Database health
GET /health/ai        → AI provider health
GET /health/gmail     → Gmail API health
```

### Admin
```
GET   /api/admin/api-usage      → API usage statistics
GET   /api/admin/api-health     → Active alerts
GET   /api/admin/quota-status   → Quota usage percentages
PATCH /api/admin/alerts/{id}/acknowledge → Acknowledge alert
```

---

## Swagger UI

Access interactive API docs at:
```
http://localhost:8080/swagger-ui.html
```

---

## Email Processing Flow

```
User connects Gmail
    ↓
EmailSyncScheduler (every 15 min)
    ↓
GmailService.syncEmails()
    ↓
EmailFilter (remove spam/promotions)
    ↓
Email saved to DB
    ↓
AiAnalysisService.analyzeEmail() [async]
    ↓
GroqClient → prompt → LLaMA3
    (if fails → OpenRouterClient)
    ↓
AiResponseParser → ExtractionResult
    ↓
ExtractedTask saved to DB
    ↓
ReminderService.createRemindersForTask()
    (1 day, 6h, 1h, 15min before deadline)
    ↓
CalendarService.createCalendarEvent() [async]
    ↓
WebSocket broadcast → client
    ↓
ReminderDispatchScheduler (every 60s)
    ↓
NotificationService.sendReminderNotification()
    ↓
FcmNotificationSender → Firebase → Device
```

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | ✅ | PostgreSQL JDBC URL |
| `DB_USERNAME` | ✅ | Database user |
| `DB_PASSWORD` | ✅ | Database password |
| `GOOGLE_CLIENT_ID` | ✅ | Google OAuth2 Client ID |
| `GOOGLE_CLIENT_SECRET` | ✅ | Google OAuth2 Client Secret |
| `JWT_SECRET` | ✅ | Min 32 chars JWT signing secret |
| `ENCRYPTION_KEY` | ✅ | 32 char AES-256 key |
| `GROQ_API_KEY` | ✅ | Groq API key (free tier) |
| `OPENROUTER_API_KEY` | ⭕ | OpenRouter fallback key |
| `FIREBASE_SERVICE_ACCOUNT_BASE64` | ⭕ | Firebase FCM config (Base64) |
| `APP_BASE_URL` | ⭕ | Backend URL for OAuth redirect |
| `FRONTEND_URL` | ⭕ | Frontend URL for CORS |

---

## Deployment — Free Tier

### Option A: Render.com (Recommended)
```bash
# Push to GitHub, then connect to Render
# Uses render.yaml for configuration
# Set env vars in Render dashboard
```

### Option B: Railway
```bash
# Push to GitHub, connect Railway
# Railway auto-detects Dockerfile
```

### Option C: Fly.io
```bash
fly launch
fly secrets set GOOGLE_CLIENT_ID=xxx ...
fly deploy
```

### Database (Free Options)
- **Supabase** (recommended): 500MB free PostgreSQL
- **Railway**: 1GB PostgreSQL free tier
- **Render PostgreSQL**: 1GB free

---

## AI Configuration

### Groq (Primary — Free)
1. Sign up at https://console.groq.com
2. Create API key
3. Set `GROQ_API_KEY` in `.env`
4. Free tier: 14,400 req/day, ~500K tokens/day

### OpenRouter (Fallback — Free)
1. Sign up at https://openrouter.ai
2. Create API key
3. Set `OPENROUTER_API_KEY` in `.env`
4. Use model: `meta-llama/llama-3-8b-instruct:free`

---

## 24/7 Uptime

The backend includes:
1. **KeepAliveScheduler** — pings `/health` every 5 minutes
2. **HikariCP keepalive** — 60s TCP keepalive for DB connections
3. **Health checks** — Docker and Render health check at `/health`
4. **Auto-recovery** — Spring retry + Resilience4j circuit breaker

---

## Security

- AES-256-GCM encryption for Gmail tokens
- JWT HS256 with configurable expiry
- CORS restricted to configured frontend URL
- Rate limiting via Bucket4j
- Input validation on all endpoints
- Non-root Docker user

---

## Project Structure

```
src/main/java/com/notivio/
├── NotivioApplication.java
├── ai/                    ← Groq + OpenRouter clients
├── config/                ← Security, Swagger, Firebase, WebSocket
├── controller/            ← REST API endpoints
├── entity/                ← JPA entities (11 tables)
├── exception/             ← Global error handling
├── gmail/                 ← Gmail API client + email filter
├── notification/          ← FCM notification sender
├── repository/            ← Spring Data JPA repos
├── scheduler/             ← Cron jobs (email sync, reminders, keepalive)
├── security/              ← JWT, OAuth2 handler
├── service/               ← Business logic
└── util/                  ← Encryption, date utilities
```

---

## License
MIT
