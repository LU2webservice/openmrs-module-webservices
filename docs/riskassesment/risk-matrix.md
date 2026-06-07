# Risk matrix � openmrs-module-webservices.rest

Dit is de risk matrix (kans x impact) om een beeld te krijgen van de risisco's die een dreiging vormen voor onze module.

---

# Risicomatrix

| ID      | Risk                      | Kans           | Impact         | Risk (Lik x Imp) |
| :------ | :------------------------ | :------------- | :------------- | :--------------- |
| **S-1** | Basic Auth onderschepping | Waarschijnlijk | Zeer serieus   | **D4**           |
| **T-1** | Mass assignment           | Waarschijnlijk | Zeer serieus   | **D4**           |
| **S-2** | Sessie-hijacking          | Mogelijk       | Serieus        | **C3**           |
| **T-2** | Inputvalidatie / injectie | Mogelijk       | Serieus        | **C3**           |
| **R-1** | Incomplete auditlogging   | Mogelijk       | Serieus        | **C3**           |
| **I-1** | Stack traces in responses | Mogelijk       | Minder ernstig | **B3**           |

---

## Evaluatiemethodiek

- **Risico (Lik x Imp):** Gebaseerd op de matrix: Kans (1-5) x Impact (1-5).
- **Kans:** De kans dat het risico voorkomt. Van zeer onwaarschijnlijk en heeft weinig prioriteit naar zeer waarschijnlijk en heeft hoge prioriteit.
- **Impact:** De schade die een risico meebrengt. Van onbelangrijk en heeft weinig prioriteit naar catastrofaal met een hoge prioriteit.
