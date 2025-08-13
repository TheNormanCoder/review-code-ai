# 🔄 Diagramma di Funzionamento - AI Code Review System

## 📊 **Flusso Completo del Sistema**

```mermaid
graph TD
    %% Trigger iniziale
    A[👨‍💻 Developer crea PR] --> B[📝 GitHub PR Template]
    B --> C[⚡ GitHub Actions Triggered]
    
    %% Analisi file
    C --> D[📂 Get Changed Files]
    D --> E{🔍 Files Supported?}
    E -->|Yes| F[📖 Read File Contents]
    E -->|No| Z[❌ Skip Analysis]
    
    %% Analisi parallele
    F --> G[🔀 Parallel Analysis]
    
    %% Branch 1: Analisi automatica locale
    G --> H1[🤖 Local Static Analysis]
    H1 --> I1[📝 PMD Rules Check]
    I1 --> J1[🏗️ Architecture Validation]
    J1 --> K1[🔒 Security Scan]
    K1 --> L1[⚡ Performance Check]
    L1 --> M1[📋 Generate Local Findings]
    
    %% Branch 2: Quality Gates
    G --> H2[🛡️ Quality Gates]
    H2 --> I2[✅ Checkstyle]
    I2 --> J2[🐛 SpotBugs]
    J2 --> K2[🧪 JaCoCo Coverage]
    K2 --> L2[🔐 OWASP Security]
    L2 --> M2[📈 SonarCloud]
    M2 --> N2[📊 Quality Report]
    
    %% Branch 3: AI Analysis
    G --> H3[🧠 AI Analysis Service]
    H3 --> I3[📤 HTTP Request to MCP]
    I3 --> J3{🌐 AI Service Available?}
    J3 -->|Yes| K3[🤖 LLM Code Analysis]
    J3 -->|No| L3[⚠️ Fallback to Local Only]
    K3 --> M3[📊 AI Findings Response]
    L3 --> M3
    
    %% Aggregazione risultati
    M1 --> O[🔄 Aggregate All Results]
    N2 --> O
    M3 --> O
    
    %% Decisione automatica
    O --> P[🎯 Calculate Overall Score]
    P --> Q{📊 Auto Decision Logic}
    Q -->|Score < 30| R1[❌ AUTO REJECT]
    Q -->|Score 30-70| R2[⚠️ CHANGES REQUESTED]  
    Q -->|Score > 70| R3[✅ AUTO APPROVE]
    
    %% Notifiche e human review
    R1 --> S[📧 Notify Team]
    R2 --> S
    R3 --> S
    S --> T[👥 Human Reviewer Assigned]
    
    %% Human review process
    T --> U{🤔 Human Review}
    U -->|Approve| V1[✅ Human Approval]
    U -->|Request Changes| V2[🔄 Human Changes Request]
    U -->|Reject| V3[❌ Human Rejection]
    
    %% Final decision logic
    V1 --> W[⚖️ Final Decision Matrix]
    V2 --> W
    V3 --> W
    
    W --> X{🎯 Final Status}
    X -->|Any Rejection| Y1[❌ PR BLOCKED]
    X -->|Changes Requested| Y2[🔄 PR NEEDS WORK]
    X -->|All Approved| Y3[✅ PR READY TO MERGE]
    
    %% Metrics e learning
    Y1 --> AA[📊 Update Metrics]
    Y2 --> AA
    Y3 --> AA
    AA --> BB[📈 Team Adoption Tracking]
    BB --> CC[🎓 Generate Training Recommendations]
```

## 🔧 **Dettaglio Componenti Tecnici**

### 1. **📂 File Detection & Content Reading**
```mermaid
sequenceDiagram
    participant GHA as GitHub Actions
    participant API as GitHub API
    participant SB as Spring Boot App
    
    GHA->>API: Get changed files in PR
    API-->>GHA: List of modified files
    GHA->>API: Read content of each file
    API-->>GHA: File contents (raw code)
    GHA->>SB: POST /api/reviews/pull-requests/{id}/ai-review
    Note over SB: Files content included in request
```

### 2. **🤖 Local Static Analysis Engine**
```mermaid
flowchart LR
    A[📄 Code Input] --> B[🔍 Regex Patterns]
    A --> C[📏 Metrics Calculation]
    A --> D[🏗️ Architecture Rules]
    
    B --> E[Field Injection Detection]
    B --> F[Hardcoded Secrets Scan]
    B --> G[SQL Injection Patterns]
    
    C --> H[Method Length Check]
    C --> I[Class Size Analysis]
    C --> J[Complexity Calculation]
    
    D --> K[Layer Violation Check]
    D --> L[SOLID Principles Verify]
    D --> M[DDD Patterns Check]
    
    E --> N[📋 Findings List]
    F --> N
    G --> N
    H --> N
    I --> N
    J --> N
    K --> N
    L --> N
    M --> N
```

### 3. **🧠 AI Service Integration**
```mermaid
sequenceDiagram
    participant SB as Spring Boot
    participant AI as External AI Service
    participant LLM as Language Model
    
    SB->>AI: POST /api/review
    Note over SB,AI: {files: {...}, context: {...}}
    
    AI->>LLM: Analyze code for principles
    Note over AI,LLM: Prompt with SOLID, Clean Code rules
    
    LLM-->>AI: Structured analysis response
    Note over AI,LLM: JSON with findings, severity, suggestions
    
    AI-->>SB: Review response
    Note over SB,AI: {decision, score, findings[]}
    
    SB->>SB: Map to internal model
    SB->>SB: Save to database
```

## 📊 **Decision Matrix Logic**

```mermaid
flowchart TD
    A[🎯 All Review Results] --> B{📊 Critical Issues?}
    
    B -->|Yes| C[❌ AUTO REJECT]
    B -->|No| D{⚠️ High Severity > 3?}
    
    D -->|Yes| E[🔄 CHANGES REQUESTED]
    D -->|No| F{✅ Coverage > 80%?}
    
    F -->|No| G[⚠️ NEEDS TESTS]
    F -->|Yes| H{🏗️ Architecture OK?}
    
    H -->|No| I[🔄 ARCHITECTURE REVIEW]
    H -->|Yes| J{👥 Human Review?}
    
    J -->|Pending| K[⏳ AWAITING REVIEW]
    J -->|Approved| L[✅ READY TO MERGE]
    J -->|Rejected| M[❌ HUMAN REJECTED]
    
    %% Priority rules
    C --> N[🚨 BLOCKED - Priority 1]
    M --> N
    E --> O[⚠️ NEEDS WORK - Priority 2]
    G --> O
    I --> O
    K --> P[⏳ IN PROGRESS - Priority 3]
    L --> Q[🎉 MERGEABLE - Priority 4]
```

## 🔧 **Esempio Pratico di Analisi**

### **Input: Codice Java**
```java
@Service
public class UserService {
    @Autowired  // ⚠️ Field injection
    private UserRepository userRepository;
    
    public User createUser(String name, String email, String password) {  // ⚠️ Troppi parametri
        if (name == null) return null;  // ⚠️ Null check debole
        
        User user = new User();
        user.setPassword(password);  // 🚨 Password in chiaro!
        userRepository.save(user);
        
        // ⚠️ Metodo troppo lungo, duplicazione logica
        System.out.println("User created: " + name);
        log.info("User created: " + name);
        
        return user;
    }
}
```

### **Output: Findings Generati**
```mermaid
graph LR
    A[📄 UserService.java] --> B[🔍 Static Analysis]
    A --> C[🧠 AI Analysis]
    
    B --> D1[🚨 SECURITY: Plain password]
    B --> D2[⚠️ DEPENDENCY_INJECTION: Field injection]
    B --> D3[⚠️ BEST_PRACTICE: Too many parameters]
    B --> D4[ℹ️ DRY_VIOLATION: Duplicate logging]
    
    C --> E1[🚨 SECURITY: Password vulnerability]
    C --> E2[⚠️ SOLID: SRP violation]
    C --> E3[ℹ️ ERROR_HANDLING: Weak validation]
    
    D1 --> F[📊 Aggregate Findings]
    D2 --> F
    D3 --> F
    D4 --> F
    E1 --> F
    E2 --> F
    E3 --> F
    
    F --> G[📈 Score: 45/100]
    G --> H[🚨 Decision: CHANGES_REQUESTED]
```

## 🎯 **Configurazione e Deployment**

```mermaid
graph TD
    A[🏗️ System Setup] --> B[📝 Configure application.yml]
    B --> C[🔧 Setup AI Endpoint]
    C --> D[🔑 Configure API Keys]
    D --> E[📊 Setup Quality Gates]
    E --> F[⚡ Deploy GitHub Actions]
    F --> G[🚀 System Ready]
    
    %% Configuration details
    B --> B1[Database Connection]
    B --> B2[Security Settings]
    B --> B3[Logging Configuration]
    
    C --> C1[MCP Service URL]
    C --> C2[Timeout Settings]
    C --> C3[Fallback Configuration]
    
    E --> E1[PMD Custom Rules]
    E --> E2[Checkstyle Config]
    E --> E3[Coverage Thresholds]
    
    F --> F1[Workflow Triggers]
    F --> F2[Secret Management]
    F --> F3[Notification Setup]
```

Questo diagramma mostra l'**intero ecosistema** di come il sistema analizza il codice, combina risultati automatici e AI, e prende decisioni intelligenti per il code review! 🚀