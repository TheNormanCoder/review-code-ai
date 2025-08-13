# Checklist Verifica Principi di Sviluppo

## 🏗️ Principi SOLID

### Single Responsibility Principle (SRP)
- [ ] Ogni classe ha una sola ragione per cambiare
- [ ] La classe non supera le 300 righe di codice
- [ ] I metodi sono focalizzati su un singolo compito
- [ ] Non ci sono commenti che indicano "questa classe fa anche..."

**Violazioni comuni:**
- Classi che gestiscono sia business logic che persistenza
- Utility classes con metodi non correlati
- Controller che contengono business logic

### Open/Closed Principle (OCP)
- [ ] Il codice è estendibile senza modifiche
- [ ] Uso di interfacce per l'astrazione
- [ ] Pattern Strategy/Template Method implementati correttamente
- [ ] Nessuna modifica diretta di classi esistenti per nuove funzionalità

**Violazioni comuni:**
- Switch/if statements che richiedono modifica per nuovi casi
- Classi concrete invece di interfacce
- Logica hard-coded senza possibilità di estensione

### Liskov Substitution Principle (LSP)
- [ ] Le sottoclassi sono sostituibili alle superclassi
- [ ] Nessuna violazione di precondizioni/postcondizioni
- [ ] Comportamento coerente in tutta la gerarchia
- [ ] Nessun lancio di eccezioni non previste nella superclasse

**Violazioni comuni:**
- Override che cambia completamente il comportamento
- Sottoclassi che restringono eccessivamente le precondizioni
- Implementazioni che lanciano NotSupportedException

### Interface Segregation Principle (ISP)
- [ ] Interfacce piccole e coese
- [ ] Client non dipendono da metodi che non usano
- [ ] Interfacce specifiche per ruoli specifici
- [ ] Nessuna "fat interface" con troppi metodi

**Violazioni comuni:**
- Interfacce monolitiche con molti metodi
- Implementazioni che lasciano metodi vuoti
- Client costretti a implementare metodi non necessari

### Dependency Inversion Principle (DIP)
- [ ] Dipendenze verso astrazioni, non implementazioni concrete
- [ ] Constructor injection utilizzato (non field injection)
- [ ] Nessun uso di `new` per dipendenze esterne
- [ ] Configurazione delle dipendenze esternalizzata

**Violazioni comuni:**
- `@Autowired` su field invece che constructor
- Dipendenze dirette verso classi concrete
- Hard-coding di nomi di classi implementative

## 🧹 Clean Code Principles

### DRY (Don't Repeat Yourself)
- [ ] Nessuna duplicazione di codice logico
- [ ] Costanti estratte per valori magici
- [ ] Utility methods per logica ripetuta
- [ ] Configurazione centralizzata

**Violazioni comuni:**
- Copy-paste di blocchi di codice
- Validation logic duplicata
- String literals ripetute
- Logica di conversione duplicata

### KISS (Keep It Simple, Stupid)
- [ ] Soluzioni semplici per problemi semplici
- [ ] Complessità ciclomatica < 10 per metodo
- [ ] Nesting massimo di 3 livelli
- [ ] Nomi di metodi/variabili chiari e diretti

**Violazioni comuni:**
- Over-engineering di soluzioni semplici
- Pattern complessi per problemi banali
- Metodi con troppi parametri (>5)
- Logica eccessivamente annidata

### YAGNI (You Aren't Gonna Need It)
- [ ] Nessuna funzionalità "just in case"
- [ ] Codice implementato solo quando necessario
- [ ] Nessuna generalizzazione prematura
- [ ] Rimozione di codice non utilizzato

**Violazioni comuni:**
- Framework custom per un solo use case
- Configurabilità eccessiva non richiesta
- Codice "preparatorio" per future feature
- Astrazioni non giustificate

## 🎯 Domain-Driven Design

### Entities
- [ ] Identità unica e persistente (`@Id`)
- [ ] Logica di business incapsulata
- [ ] Invarianti di dominio protette
- [ ] Nessun anemic model (solo getter/setter)

### Value Objects
- [ ] Immutabilità garantita
- [ ] Equality basata su valore, non identità
- [ ] Validazione nel costruttore
- [ ] Nessun settore pubblico

### Aggregates
- [ ] Root entity chiaramente identificata
- [ ] Consistenza transazionale mantenuta
- [ ] Accesso esterno solo tramite root
- [ ] Dimensione ragionevole dell'aggregato

### Domain Services
- [ ] Stateless (nessuno stato interno)
- [ ] Logica che non appartiene a entity/value object
- [ ] Interfaccia espressa in linguaggio di dominio
- [ ] Nessuna dipendenza da infrastruttura

### Bounded Context
- [ ] Confini di contesto rispettati
- [ ] Linguaggio ubiquo utilizzato
- [ ] Nessun leakage tra contesti
- [ ] Integration patterns appropriati

## 🧪 Testing Principles

### Test Coverage
- [ ] Coverage minimo 80% per nuove linee
- [ ] Branch coverage > 70%
- [ ] Test per edge cases critici
- [ ] Nessun test vuoto o commented

### Test Quality
- [ ] Test isolati e indipendenti
- [ ] Arrange-Act-Assert pattern
- [ ] Nomi di test descrittivi
- [ ] Un solo assert per concetto

### TDD Compliance
- [ ] Test scritti prima dell'implementazione
- [ ] Red-Green-Refactor cycle seguito
- [ ] Test falliscono per la ragione corretta
- [ ] Refactoring preserva funzionalità

### Mocking Strategy
- [ ] Mock solo dipendenze esterne
- [ ] Verify interaction solo quando necessario
- [ ] Nessun mock di value objects
- [ ] Test data builders per setup complesso

## 🔒 Security Principles

### Input Validation
- [ ] Validazione di tutti gli input esterni
- [ ] Sanitizzazione appropriata
- [ ] Validation sia client che server-side
- [ ] Error messages non rivelano dettagli interni

### Authentication & Authorization
- [ ] Principio del least privilege
- [ ] Nessun hardcoded credentials
- [ ] Session management sicuro
- [ ] Authorization checks su tutti gli endpoint

### Data Protection
- [ ] Dati sensibili criptati
- [ ] Nessun logging di informazioni sensibili
- [ ] SQL injection prevenuta (prepared statements)
- [ ] XSS protection implementata

## ⚡ Performance Principles

### Database Access
- [ ] Lazy loading appropriato
- [ ] Nessuna query N+1
- [ ] Indici appropriati utilizzati
- [ ] Connection pooling configurato

### Caching Strategy
- [ ] Cache a livelli appropriati
- [ ] Cache invalidation gestita
- [ ] TTL configurato appropriatamente
- [ ] Cache warming quando necessario

### Resource Management
- [ ] Streams e resources chiusi appropriatamente
- [ ] Nessun memory leak
- [ ] Garbage collection considerata
- [ ] Connection timeout configurati

## 📋 Checklist di Review

### Pre-Review (Reviewer)
- [ ] Ho compreso i requirements della feature?
- [ ] Ho familiarità con il contesto di dominio?
- [ ] Ho tempo sufficiente per una review approfondita?

### Durante Review
- [ ] Architecture principles rispettati?
- [ ] Design patterns appropriati utilizzati?
- [ ] Clean code principles seguiti?
- [ ] Security considerations indirizzate?
- [ ] Performance implications valutate?
- [ ] Test coverage e qualità adeguati?

### Post-Review
- [ ] Feedback costruttivo fornito?
- [ ] Alternativeutions proposte quando appropriato?
- [ ] Documentation aggiornata se necessario?
- [ ] Follow-up actions identificate?

## 🎯 Grading System

### Scoring
- **Excellent (90-100%)**: Tutti i principi seguiti correttamente
- **Good (80-89%)**: Principi principalmente seguiti, miglioramenti minori
- **Needs Improvement (70-79%)**: Alcune violazioni significative
- **Critical (< 70%)**: Violazioni multiple, training necessario

### Action Based on Score
- **Excellent**: Complimenti, consider for mentoring others
- **Good**: Minor adjustments, approve after fixes
- **Needs Improvement**: Request changes, schedule pairing session
- **Critical**: Reject, schedule training, assign mentor