# Pull Request Template

## 📋 Descrizione
<!-- Fornisci una breve descrizione delle modifiche proposte -->

### Tipo di modifica
- [ ] 🐛 Bug fix (modifica non breaking che risolve un problema)
- [ ] ✨ Nuova feature (modifica non breaking che aggiunge funzionalità)
- [ ] 💥 Breaking change (modifica che potrebbe causare incompatibilità)
- [ ] 📚 Documentazione (solo aggiornamenti alla documentazione)
- [ ] 🧹 Refactoring (miglioramento del codice senza modifiche funzionali)
- [ ] ⚡ Performance (miglioramenti delle prestazioni)
- [ ] 🔒 Security (correzioni di sicurezza)
- [ ] 🧪 Test (aggiunta o correzione di test)

## 🎯 Obiettivo
<!-- Spiega perché questa modifica è necessaria. Collega a issue se presente -->
Risolve: #(numero issue)

## 🔄 Modifiche apportate
<!-- Lista dettagliata delle modifiche -->
- 
- 
- 

## 🧪 Come testare
<!-- Descrivi i passi per testare queste modifiche -->
1. 
2. 
3. 

## 📸 Screenshot (se applicabile)
<!-- Aggiungi screenshot per modifiche UI -->

## ✅ Checklist del Developer

### Qualità del Codice
- [ ] Il mio codice segue le linee guida di stile del progetto
- [ ] Ho eseguito una self-review del mio codice
- [ ] Ho commentato il codice, specialmente nelle parti difficili da capire
- [ ] Le mie modifiche non generano nuovi warning
- [ ] Ho aggiunto test che dimostrano che la mia correzione è efficace o che la mia feature funziona
- [ ] I test nuovi ed esistenti passano localmente con le mie modifiche

### Architettura e Design
- [ ] La soluzione rispetta i principi SOLID
- [ ] Ho seguito i design pattern appropriati per il contesto
- [ ] La separazione delle responsabilità è mantenuta (layered architecture)
- [ ] Le dipendenze seguono il principio di inversione
- [ ] Il codice è modulare e riutilizzabile
- [ ] Ho evitato duplicazione di codice (DRY principle)

### Sicurezza e Performance
- [ ] Ho verificato che non ci siano vulnerabilità di sicurezza
- [ ] Le performance sono accettabili (no N+1 queries, algoritmi efficienti)
- [ ] La gestione degli errori è appropriata
- [ ] I dati sensibili sono protetti (no hardcoded secrets)

### Documentazione e Manutenibilità
- [ ] Ho aggiornato la documentazione se necessario
- [ ] I nomi di variabili/metodi sono esplicativi
- [ ] Il codice è facilmente comprensibile da altri sviluppatori
- [ ] Eventuali PR dipendenti sono state mergiate e pubblicate

## 🔍 Checklist del Reviewer

### Code Quality & Style
- [ ] Codice leggibile e ben strutturato
- [ ] Nomenclatura consistente e significativa
- [ ] Complessità ciclomatica accettabile
- [ ] Code smells risolti

### Architecture & Design
- [ ] Rispetta i principi SOLID
- [ ] Design pattern appropriati
- [ ] Layered architecture mantenuta
- [ ] Dependency injection corretto
- [ ] Separazione delle responsabilità
- [ ] Modularity e reusability

### Business Logic
- [ ] Logica di business corretta
- [ ] Edge cases gestiti
- [ ] Validazione input appropriata
- [ ] Error handling robusto

### Testing & Quality
- [ ] Test adeguati e completi
- [ ] Coverage appropriato
- [ ] Test edge cases
- [ ] Integration test se necessari

### Security & Performance
- [ ] Vulnerabilità verificate
- [ ] Performance accettabili
- [ ] Memory leaks prevenuti
- [ ] Database queries ottimizzate

### Documentation & Maintenance
- [ ] Documentazione aggiornata
- [ ] API documentation completa
- [ ] Compatibilità verificata
- [ ] Backward compatibility mantenuta

## 📝 Note aggiuntive
<!-- Qualsiasi informazione aggiuntiva per i reviewer -->

## 🏷️ Labels da applicare
<!-- Suggerisci le label appropriate -->
- [ ] feature
- [ ] bugfix
- [ ] documentation
- [ ] breaking-change
- [ ] needs-review
- [ ] ready-for-merge