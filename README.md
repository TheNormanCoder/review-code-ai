# AI Code Review System

Sistema di code review automatizzato con integrazione AI tramite MCP (Model Context Protocol).

## 🎯 Obiettivi

- **Standardizzare le PR**: Template, checklist e linee guida uniformi
- **Automatizzare controlli**: Linting, static analysis, test, coverage, security  
- **Definire ruoli e workflow**: Responsabilità e processi chiari
- **Integrazione AI**: Review automatica intelligente

## 🚀 Quick Start

### Prerequisiti
- Java 21+
- Maven 3.8+
- Docker (opzionale)

### Avvio Locale
```bash
# Clone repository
git clone <repository-url>
cd review-code-ai

# Build e avvio
mvn clean install
mvn spring-boot:run
```

L'applicazione sarà disponibile su `http://localhost:8080`

### Documentazione API
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health Check: `http://localhost:8080/api/actuator/health`

## 📋 Funzionalità

### Core Features
- ✅ Gestione Pull Request
- ✅ Review AI automatica tramite MCP
- ✅ Review umana con workflow
- ✅ Metriche e reporting
- ✅ Dashboard analytics

### Quality Gates
- ✅ Checkstyle (code style)
- ✅ PMD (best practices)
- ✅ SpotBugs (bug detection)
- ✅ JaCoCo (test coverage)
- ✅ OWASP (security vulnerabilities)
- ✅ SonarCloud (code quality)

### CI/CD Integration
- ✅ GitHub Actions workflows
- ✅ Automated PR checks
- ✅ Security scanning
- ✅ Container vulnerability scan

## 🏗️ Architettura

```
├── src/main/java/com/reviewcode/ai/
│   ├── model/          # Entità JPA
│   ├── repository/     # Repository layer
│   ├── service/        # Business logic
│   ├── controller/     # REST endpoints
│   └── config/         # Configurazioni
├── .github/
│   ├── workflows/      # GitHub Actions
│   └── PULL_REQUEST_TEMPLATE/
└── docs/               # Documentazione
```

## 🔧 API Endpoints

### Pull Requests
```
POST   /api/reviews/pull-requests          # Crea PR
GET    /api/reviews/pull-requests          # Lista PR
GET    /api/reviews/pull-requests/{id}     # Dettaglio PR
POST   /api/reviews/pull-requests/{id}/ai-review    # Trigger AI review
POST   /api/reviews/pull-requests/{id}/human-review # Human review
```

### Metrics
```
GET    /api/reviews/metrics/daily          # Metriche giornaliere
GET    /api/reviews/metrics/weekly         # Metriche settimanali
GET    /api/reviews/metrics/dashboard      # Dashboard overview
POST   /api/reviews/metrics/generate       # Genera metriche
```

## ⚙️ Configurazione

### Variabili Ambiente
```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/reviewdb
DB_USER=postgres
DB_PASSWORD=password

# AI/MCP Integration
MCP_ENDPOINT=http://localhost:3000
MCP_API_KEY=your-api-key

# Security
JWT_SECRET=your-jwt-secret
```

### application.yml
```yaml
ai:
  mcp:
    endpoint: ${MCP_ENDPOINT:http://localhost:3000}
    api-key: ${MCP_API_KEY:}
    timeout: 30000
  review:
    max-file-size: 1048576
    supported-extensions: [.java, .js, .ts, .py]
```

## 🧪 Testing

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Coverage report
mvn jacoco:report

# Quality checks
mvn checkstyle:check pmd:check spotbugs:check
```

## 📊 Metriche

### KPI Principali
- **Review Time**: < 24h media
- **Coverage**: > 80% line coverage
- **Approval Rate**: > 95%
- **Security**: Zero vulnerabilità CRITICAL/HIGH

### Dashboard Metriche
- Throughput PR
- Qualità del codice
- Performance team
- Trend temporali

## 🔒 Security

- OWASP Dependency Check
- Container scanning (Trivy)
- Static analysis security rules
- JWT authentication
- HTTPS enforcement

## 📚 Documentazione

- [Workflow e Ruoli](DOCUMENTATION.md) - Processi e responsabilità
- [API Reference](docs/api.md) - Documentazione endpoint
- [Setup Guide](docs/setup.md) - Guida installazione

## 🤝 Contributing

1. Fork del repository
2. Feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### PR Guidelines
- Usa il template PR fornito
- Assicurati che tutti i check passino
- Aggiungi test per nuove funzionalità
- Mantieni coverage > 80%

## 📄 License

Questo progetto è rilasciato sotto licenza MIT. Vedi `LICENSE` per dettagli.

## 🆘 Support

- GitHub Issues per bug report
- GitHub Discussions per domande
- Wiki per documentazione dettagliata

## 🗺️ Roadmap

- [ ] Dashboard web UI
- [ ] GitLab integration  
- [ ] Slack/Teams notifications
- [ ] Advanced AI prompts
- [ ] Custom rule engine
- [ ] Multi-tenant support
