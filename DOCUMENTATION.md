# AI Code Review System - Documentazione

## 🎯 Obiettivi del Progetto

Questo progetto implementa un sistema di code review automatizzato che integra:
- **Standardizzazione PR**: Template, checklist e linee guida
- **Controlli automatici**: Linting, static analysis, test, coverage, security
- **Ruoli e workflow**: Definizione chiara di responsabilità e processi
- **Integrazione AI/MCP**: Review automatica tramite AI

## 🏗️ Architettura

### Componenti Principali
- **Spring Boot API**: Backend per gestione PR e review
- **Database**: PostgreSQL/H2 per persistenza dati
- **AI/MCP Integration**: Servizio esterno per review automatica
- **GitHub Actions**: Automazione CI/CD e quality gates

### Stack Tecnologico
- Java 17 + Spring Boot 3.2
- PostgreSQL/H2 Database
- Maven per build automation
- GitHub Actions per CI/CD
- Docker per containerizzazione

## 📋 Workflow di Code Review

### 1. Creazione Pull Request
- Utilizzo template standardizzato `.github/PULL_REQUEST_TEMPLATE/`
- Compilazione checklist obbligatoria
- Applicazione label appropriate

### 2. Controlli Automatici
Scattano automaticamente su ogni PR:

#### Code Quality & Style
- **Checkstyle**: Verifica standard codifica e formattazione
- **PMD**: Regole personalizzate per principi architetturali
- **SpotBugs**: Rilevamento bug e code smell
- **SonarCloud**: Analisi qualità completa

#### Architecture & Design
- **ArchUnit**: Test architettura layered e principi SOLID
- **Custom PMD Rules**: Validazione DDD, Clean Code, API design
- **Architecture Validation Service**: Controlli runtime principi

#### Security & Performance  
- **OWASP Dependency Check**: Vulnerabilità dipendenze
- **Trivy Scanner**: Vulnerabilità container
- **Security Rules**: Hardcoded secrets, injection patterns
- **Performance Rules**: N+1 queries, SELECT *, memory leaks

#### Testing & Coverage
- **Unit Tests**: JUnit 5 + Mockito
- **Integration Tests**: Spring Boot Test
- **Architecture Tests**: ArchUnit validation
- **Coverage**: JaCoCo (min 80% line coverage)

### 3. AI Review
- Trigger automatico per file modificati
- Analisi tramite MCP (Model Context Protocol)
- Generazione findings categorizzati per severità
- Suggerimenti miglioramento automatici

### 4. Human Review
- Review obbligatoria da reviewer designato
- Validazione findings AI
- Approvazione finale

## 👥 Ruoli e Responsabilità

### Developer
- Crea PR seguendo template
- Risolve findings automatici
- Risponde a commenti reviewer
- Mantiene branch aggiornato

### Reviewer
- Review codice entro 24h
- Valida findings AI
- Fornisce feedback costruttivo
- Approva o richiede modifiche

### Tech Lead
- Definisce standard di qualità
- Configura quality gates
- Monitora metriche team
- Risolve conflitti review

### AI System
- Review automatica real-time
- Categorizzazione findings
- Suggerimenti miglioramento
- Scoring qualità codice

## 📊 Metriche e KPI

### Metriche di Qualità
- **Coverage**: Min 80% line coverage
- **Complexity**: Max 10 cyclomatic complexity
- **Duplication**: Max 3% code duplication
- **Security**: Zero vulnerabilità CRITICAL/HIGH

### Metriche di Processo
- **Review Time**: Media < 24h
- **PR Size**: Media < 400 LOC
- **Rework Rate**: < 20%
- **Approval Rate**: > 95%

## 🔧 API Endpoints

### Pull Requests
```
POST   /api/reviews/pull-requests          # Crea PR
GET    /api/reviews/pull-requests          # Lista PR
GET    /api/reviews/pull-requests/{id}     # Dettaglio PR
POST   /api/reviews/pull-requests/{id}/ai-review    # Trigger AI review
POST   /api/reviews/pull-requests/{id}/human-review # Aggiungi human review
GET    /api/reviews/pull-requests/{id}/reviews      # Lista review PR
```

### Configurazione AI/MCP
```yaml
ai:
  mcp:
    endpoint: ${MCP_ENDPOINT:http://localhost:3000}
    api-key: ${MCP_API_KEY:}
    timeout: 30000
  review:
    max-file-size: 1048576
    supported-extensions: [.java, .js, .ts, .py, .go, .rs]
```

## 🚀 Quick Start

### 1. Setup Locale
```bash
# Clone repository
git clone <repository-url>
cd review-code-ai

# Build progetto
mvn clean install

# Avvia applicazione
mvn spring-boot:run
```

### 2. Setup CI/CD
1. Configura secrets GitHub:
   - `MCP_ENDPOINT`: URL servizio AI
   - `MCP_API_KEY`: API key per autenticazione
   - `SONAR_TOKEN`: Token SonarCloud

2. Workflow automatici attivi:
   - `ci.yml`: Build, test, quality, security
   - `code-review.yml`: Review automatica PR

### 3. Setup Database
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/reviewdb
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:password}
```

## 📖 Comandi Utili

```bash
# Test con coverage
mvn clean test jacoco:report

# Quality checks
mvn checkstyle:check pmd:check spotbugs:check

# Security scan
mvn org.owasp:dependency-check-maven:check

# Build Docker image
mvn spring-boot:build-image

# Run quality gates
mvn clean verify sonar:sonar
```

## 🔒 Security Best Practices

1. **Dependency Scanning**: OWASP automatico
2. **Container Scanning**: Trivy per immagini Docker  
3. **Code Analysis**: Security rules in PMD/SpotBugs
4. **Secret Detection**: Prevenzione commit secrets
5. **Access Control**: Authentication/authorization API

## 📈 Monitoring e Alerting

- **Health Checks**: `/actuator/health`
- **Metrics**: `/actuator/metrics`, Prometheus export
- **Logs**: Structured logging con correlation ID
- **Performance**: Response time, throughput monitoring

## 🔄 Process Improvement

- **Weekly Metrics Review**: KPI e trend analysis
- **Retrospective**: Feedback processo review
- **Tool Evaluation**: Nuovi tool e integrazioni
- **Training**: Upskilling team su best practices