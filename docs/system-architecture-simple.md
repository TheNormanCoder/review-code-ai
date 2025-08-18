# 🔄 Diagramma Semplificato - AI Code Review System

## 📊 **Flusso Principale del Sistema**

```mermaid
graph TD
    A[👨‍💻 Developer apre PR] --> B[⚡ GitHub Actions]
    B --> C[📂 Analizza file modificati]
    C --> D[🔀 Analisi Parallela]
    
    D --> E[🤖 Analisi Locale]
    D --> F[🧠 Analisi AI]
    D --> G[🛡️ Quality Gates]
    
    E --> H[🔄 Combina Risultati]
    F --> H
    G --> H
    
    H --> I{📊 Score Finale}
    I -->|< 30| J[❌ Rifiuta]
    I -->|30-70| K[⚠️ Richiedi Modifiche]
    I -->|> 70| L[✅ Approva]
    
    J --> M[📧 Notifica Team]
    K --> M
    L --> M
```

## 🧩 **Componenti Principali**

1. **🤖 Analisi Locale**: PMD, Checkstyle, Security scan
2. **🧠 Analisi AI**: LLM per principi SOLID e Clean Code
3. **🛡️ Quality Gates**: Coverage, SonarCloud, OWASP
4. **📊 Decision Engine**: Calcola score e decide automaticamente

## 💡 **In Sintesi**

Il sistema prende una PR, la analizza con 3 motori diversi, combina i risultati e decide automaticamente se approvarla, richiedere modifiche o rifiutarla basandosi su uno score calcolato.