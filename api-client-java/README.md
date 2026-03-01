# API Client Java — Homework (API101 + API102)

This project solves the homework requirements using the AIXIGO demo APIs:

- **API102 (Analytics)**:
  - Fetch asset IDs for given ISINs / currency assets
  - Create **temporary contract #1** with given positions
  - Fetch **assets-snapshot** at start/end and calculate value + total return (2024)
  - Create **temporary contract #2** by converting EU-only portfolio value into USD cash
  - Fetch snapshot start/end and calculate value + total return (2024)

- **API101 (Client Profiling)**:
  - Create a risk profile questionnaire
  - Fill answers once without justification
  - Fill again with justification (using Last-Modified header)

---

## Requirements

- Java 11+ (or Java 17 recommended)
- Maven or Gradle (depending on your project setup)
- Environment variable:
  - `X_ID_TOKEN` must be set before running

> ⚠️ Do NOT commit your token to GitHub.

---

## How to run

### 1) Set env var

**macOS / Linux**
```bash
export X_ID_TOKEN="YOUR_TOKEN_HERE"

Windows (PowerShell)

setx X_ID_TOKEN "YOUR_TOKEN_HERE"


2) Run

Run the Main class:

src/main/java/com/aixigo/sample_projects/api_client_java/Main.java

You can run from IntelliJ or from the command line using your build tool.


Inputs used (Homework)
Contract #1 (Investment Date = 2024-01-01)

Deutsche Bank (ISIN DE0005140008) — 50

Citigroup (ISIN US1729674242) — 20

Amundi (ISIN FR0004125920) — 30

EUR cash (CURRENCY:EUR.EUR) — 5000 EUR

Contract #2 (Investment Date = 2024-01-01)

Convert EU-only value at start (DB + Amundi + EUR cash, excluding Citi) into USD

Invest 100% into USD cash (CURRENCY:USD.USD)

Output / Results (from successful run)
Resolved asset IDs

dbId = 514000

citiId = A1H92V

amundiId = A2LQV6

eurId = CURRENCY:EUR.EUR

usdId = CURRENCY:USD.USD

Contract #1 (Temporary Contract)

contractId_1 = contract_7be13c89-a4fa-474c-adf3-043516cda84e

Portfolio values (EUR):

Value @ 2024-01-01: 8023.949922888506

Value @ 2025-01-01: 9036.19212628742

Total return 2024:

Decimal: 0.1261526072728183

Percent: 12.6153%

EU-only value at start (used for Contract #2)

EU_ONLY_VALUE_EUR_2024_01_01 = 7092.443073573437

EUR_PER_1_USD_2024_01_01 = 0.9071940488070398

CONTRACT2_USD_QTY = 7818.0 USD

Contract #2 (USD Cash Only)

contractId_2 = contract_4ae8d3ca-9981-4c0e-a00d-ab6be1ef4a44

Portfolio values (EUR):

Value @ 2024-01-01: 7092.443073573437

Value @ 2025-01-01: 7525.26710944268

Total return 2024:

Decimal: 0.06102608528251041

Percent: 6.1026%

----------------------------------------------
API101 - Sample Result

CREATED_RISK_PROFILE_ID=b4f76ca4-851c-440c-8330-acd746b2e1dd
LAST_MODIFIED_1=Fri, 27 Feb 2026 15:29:57 GMT
FILL_1_OK
LAST_MODIFIED_2=Fri, 27 Feb 2026 16:15:11 GMT
FILL_2_OK_WITH_JUSTIFICATION
FINAL_RISK_PROFILE_ID=b4f76ca4-851c-440c-8330-acd746b2e1dd

