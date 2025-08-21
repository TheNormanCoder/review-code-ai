# 🏗️ Multi-Module Project Structure

Il progetto è stato riorganizzato in una **struttura modulare** per separare le responsabilità e migliorare la manutenibilità.

## 📁 Struttura dei Moduli

```
review-code-ai/                           # Parent Project
├── pom.xml                               # Parent POM
├── README.md                             # Documentazione principale
├── .github/workflows/                    # CI/CD pipelines
├── docker/                               # Configurazioni Docker
├── docs/                                 # Documentazione di sistema
│
├── ai-review-core/                       # 🎯 MODULO 1: Backend Core
│   ├── pom.xml                          # Dipendenze Spring Boot
│   ├── src/main/java/                   # Codice sorgente Java
│   │   └── com/reviewcode/ai/
│   │       ├── CodeReviewApplication.java
│   │       ├── config/                  # Configurazioni Spring
│   │       ├── controller/              # REST Controllers
│   │       ├── model/                   # Entità JPA
│   │       ├── repository/              # Repository layer
│   │       └── service/                 # Business logic
│   ├── src/main/resources/              # File di configurazione
│   ├── src/test/                        # Test unitari e integrazione
│   ├── custom-pmd-rules.xml             # Regole PMD
│   ├── checkstyle.xml                   # Configurazione Checkstyle
│   ├── spotbugs-exclude.xml             # Esclusioni SpotBugs
│   └── Dockerfile                       # Container Docker
│
└── ai-review-integrations/               # 🔌 MODULO 2: Integrazioni
    ├── pom.xml                          # Parent delle integrazioni
    ├── intellij-plugin/                 # Plugin IntelliJ IDEA
    │   ├── build.gradle.kts             # Build Gradle
    │   ├── src/main/kotlin/             # Codice Kotlin
    │   └── plugin.xml                   # Configurazione plugin
    ├── github-integration/              # Integrazione GitHub
    │   ├── pom.xml                      # Dipendenze Maven
    │   └── src/main/java/               # Webhook handlers
    └── web-dashboard/                   # Dashboard React
        ├── package.json                 # Dipendenze Node.js
        ├── src/                         # Codice TypeScript/React
        └── vite.config.ts               # Configurazione build
```

## 🎯 Responsabilità dei Moduli

### **ai-review-core** - Backend Services
- ✅ **API REST** per code review
- ✅ **Business Logic** di analisi AI
- ✅ **Database** e persistenza
- ✅ **Configurazioni** Spring Boot
- ✅ **Docker** containerization

### **ai-review-integrations** - UI e Integrazioni
- ✅ **Plugin IntelliJ** per IDE integration
- ✅ **GitHub Integration** per webhook e PR comments
- ✅ **Web Dashboard** per management UI
- ✅ **Estensioni future** (VS Code, altri IDE)

## 🚀 Come Buildare

### Build Completo
```bash
mvn clean package -DskipTests
```

### Build Solo Core
```bash
mvn clean package -pl ai-review-core -DskipTests
```

### Build Solo Integrazioni
```bash
mvn clean package -pl ai-review-integrations -DskipTests
```

### Build Plugin IntelliJ
```bash
cd ai-review-integrations/intellij-plugin
./gradlew buildPlugin
```

### Build Web Dashboard
```bash
cd ai-review-integrations/web-dashboard
npm install && npm run build
```

## 🐳 Docker

Il Dockerfile è ora nel modulo core:
```bash
docker build -t review-code-ai:latest -f ai-review-core/Dockerfile ai-review-core/
```

## 🔄 CI/CD

Il workflow GitHub Actions è stato aggiornato per:
- ✅ Buildare solo il modulo **ai-review-core**
- ✅ Eseguire quality checks su **ai-review-core**
- ✅ Creare immagine Docker dal modulo **ai-review-core**

## 🎁 Vantaggi della Nuova Struttura

1. **📦 Separazione delle responsabilità**
   - Backend e frontend chiaramente separati
   - Dipendenze isolate per modulo

2. **⚡ Build più veloci**
   - Posso buildare solo il modulo che serve
   - CI/CD più efficiente

3. **🔧 Estensibilità**
   - Facile aggiungere nuove integrazioni
   - Plugin per altri IDE in futuro

4. **🧹 Pulizia del codice**
   - Ogni modulo ha la sua configurazione
   - Meno confusione nella root

5. **👥 Team scalability**
   - Team frontend/backend possono lavorare separatamente
   - Release indipendenti possibili

## 📝 Prossimi Passi

1. **IntelliJ Plugin** - Implementare le funzionalità base
2. **GitHub Integration** - Webhook per PR automation
3. **Web Dashboard** - UI React per management
4. **VS Code Extension** - Supporto per altri IDE