# 🐳 Docker Deployment Guide - AI Code Review System

This guide explains how to deploy the AI Code Review System using Docker and Docker Compose.

## 📋 Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ RAM available
- 10GB+ disk space

## 🚀 Quick Start

### 1. Clone and Setup
```bash
git clone <repository-url>
cd review-code-ai
cp .env.example .env
# Edit .env with your configuration
```

### 2. Development Environment
```bash
# Start development environment with hot reload
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Access services:
# - Application: http://localhost:8080
# - pgAdmin: http://localhost:5050
# - Redis Commander: http://localhost:8082
# - MailHog: http://localhost:8025
# - Mock AI Service: http://localhost:8081
```

### 3. Production Environment
```bash
# Start production environment
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# With monitoring:
docker-compose -f docker-compose.yml -f docker-compose.prod.yml --profile monitoring up -d

# With logging (ELK):
docker-compose -f docker-compose.yml -f docker-compose.prod.yml --profile logging up -d
```

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Nginx       │    │   Review App    │    │   PostgreSQL    │
│  (Reverse Proxy)│◄──►│  (Spring Boot)  │◄──►│   (Database)    │
│     Port 80     │    │    Port 8080    │    │    Port 5432    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       ▼                       │
         │              ┌─────────────────┐              │
         │              │      Redis      │              │
         │              │    (Caching)    │              │
         │              │    Port 6379    │              │
         │              └─────────────────┘              │
         │                                                │
         ▼                                                ▼
┌─────────────────┐                              ┌─────────────────┐
│   Prometheus    │                              │      Backup     │
│  (Monitoring)   │                              │    Service      │
│    Port 9090    │                              │   (Scheduled)   │
└─────────────────┘                              └─────────────────┘
         │
         ▼
┌─────────────────┐
│     Grafana     │
│ (Visualization) │
│    Port 3000    │
└─────────────────┘
```

## 🔧 Configuration

### Environment Variables
Key environment variables in `.env`:

```bash
# Database
DB_PASSWORD=secure_password_here

# AI Service
AI_SERVICE_URL=http://your-ai-service:8080
AI_API_KEY=your_api_key_here

# GitHub
GITHUB_TOKEN=ghp_your_token_here
GITHUB_WEBHOOK_SECRET=webhook_secret

# Security
JWT_SECRET=your_jwt_secret_256_bits_minimum
```

### Application Configuration
Override Spring Boot configuration by mounting `application.yml`:

```bash
# Create custom config
mkdir -p config
cp src/main/resources/application-example.yml config/application.yml
# Edit config/application.yml

# Mount in docker-compose:
volumes:
  - ./config:/app/config:ro
```

## 📊 Service Details

### Main Services

| Service | Description | Port | Health Check |
|---------|-------------|------|--------------|
| review-app | Spring Boot Application | 8080 | `/actuator/health` |
| postgres | PostgreSQL Database | 5432 | `pg_isready` |
| redis | Redis Cache | 6379 | `redis-cli ping` |
| nginx | Reverse Proxy | 80/443 | nginx status |

### Development Services

| Service | Description | Port | Access |
|---------|-------------|------|--------|
| pgadmin | Database Admin | 5050 | admin@example.com / admin |
| redis-commander | Redis Admin | 8082 | Web UI |
| mailhog | Email Testing | 8025 | Web UI |
| ai-service-mock | Mock AI Service | 8081 | WireMock |

### Monitoring Services

| Service | Description | Port | Access |
|---------|-------------|------|--------|
| prometheus | Metrics Collection | 9090 | Web UI |
| grafana | Metrics Visualization | 3000 | admin / admin |

## 🔍 Common Commands

### Application Management
```bash
# View logs
docker-compose logs -f review-app

# Restart application
docker-compose restart review-app

# Scale application
docker-compose up -d --scale review-app=3

# Execute command in container
docker-compose exec review-app bash
```

### Database Management
```bash
# Connect to database
docker-compose exec postgres psql -U review_user -d reviewdb

# Create backup
docker-compose exec postgres pg_dump -U review_user reviewdb > backup.sql

# Restore backup
docker-compose exec -T postgres psql -U review_user reviewdb < backup.sql
```

### Monitoring
```bash
# View application metrics
curl http://localhost:8080/actuator/metrics

# Check health
curl http://localhost:8080/actuator/health

# View Prometheus targets
curl http://localhost:9090/api/v1/targets
```

## 🐛 Troubleshooting

### Common Issues

#### Application Won't Start
```bash
# Check logs
docker-compose logs review-app

# Check database connection
docker-compose exec review-app curl -f http://localhost:8080/actuator/health

# Verify environment variables
docker-compose exec review-app env | grep -E "(DB_|AI_|GITHUB_)"
```

#### Database Connection Issues
```bash
# Test database connectivity
docker-compose exec review-app pg_isready -h postgres -p 5432

# Check database logs
docker-compose logs postgres

# Reset database
docker-compose down -v
docker-compose up -d postgres
```

#### Performance Issues
```bash
# Check resource usage
docker stats

# Check application metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# View slow queries (if enabled)
docker-compose exec postgres psql -U review_user -d reviewdb -c "SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;"
```

#### Network Issues
```bash
# Check network connectivity
docker-compose exec review-app nslookup postgres
docker-compose exec review-app nslookup redis

# List networks
docker network ls

# Inspect network
docker network inspect review-network
```

### Logs Location
```bash
# Application logs
docker-compose logs review-app

# All services logs
docker-compose logs

# Persistent logs (if mounted)
tail -f logs/code-review.log
```

## 🔒 Security Considerations

### Production Security
1. **Change Default Passwords**: Update all default passwords in `.env`
2. **Use Secrets Management**: Consider Docker Secrets or external secret management
3. **Network Security**: Use custom networks and limit exposed ports
4. **SSL/TLS**: Configure HTTPS with proper certificates
5. **Resource Limits**: Set memory and CPU limits for all services

### SSL Configuration
```bash
# Generate self-signed certificate (development only)
mkdir -p docker/nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout docker/nginx/ssl/key.pem \
  -out docker/nginx/ssl/cert.pem

# Update nginx.conf to enable HTTPS
# Uncomment HTTPS server block in docker/nginx/nginx.conf
```

## 📈 Scaling

### Horizontal Scaling
```bash
# Scale application instances
docker-compose up -d --scale review-app=3

# Load balancer will distribute traffic automatically
```

### Database Scaling
For production workloads, consider:
- Read replicas
- Connection pooling (PgBouncer)
- Database sharding

### Monitoring Scaling
```bash
# Add monitoring for scaled services
docker-compose -f docker-compose.yml -f docker-compose.prod.yml --profile monitoring up -d
```

## 🔄 Updates and Maintenance

### Application Updates
```bash
# Pull latest images
docker-compose pull

# Restart with zero downtime
docker-compose up -d --no-deps review-app

# Rollback if needed
docker-compose down
docker-compose up -d
```

### Database Maintenance
```bash
# Create backup before maintenance
docker-compose exec postgres pg_dump -U review_user reviewdb > backup-$(date +%Y%m%d).sql

# Run database maintenance
docker-compose exec postgres psql -U review_user -d reviewdb -c "VACUUM ANALYZE;"
```

### Backup and Recovery
```bash
# Manual backup
docker-compose exec backup /scripts/backup.sh

# Restore from backup
docker-compose exec backup /scripts/restore.sh reviewdb_backup_20231201_120000.sql.gz

# List available backups
docker-compose exec backup ls -la /backup/
```

This completes the comprehensive Docker setup for the AI Code Review System! 🚀