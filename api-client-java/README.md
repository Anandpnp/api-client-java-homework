API Client Java

Robust Java implementation of AIXIGO API101 (Client Profiling) and API102 (Portfolio Analytics).

This module demonstrates production-grade REST integration, financial return calculation, currency conversion logic, and concurrency-safe API updates.

Overview

This implementation focuses on:

Deterministic portfolio construction

Snapshot-based valuation

Explicit total return calculation

FX conversion using market snapshot data

HTTP header–aware conditional updates

Clean separation between orchestration and API logic

Testable and structured project layout

The goal is correctness, transparency of calculations, and proper REST semantics.

Project Structure
api-client-java/
│
├── src/main/java/com/aixigo/sample_projects/api_client_java/
│   ├── Main.java                  # Application entry point
│   ├── (API service classes)      # API101 / API102 logic
│   └── (utility classes)
│
├── src/test/java/com/aixigo/sample_projects/api_client_java/
│   └── MainTest.java              # Basic structural tests
│
├── pom.xml
└── README.md

The structure follows standard Maven conventions, separating production and test code.

Technical Stack

Java 17

Maven

Java HTTP Client

JUnit 5

Runtime Configuration

The application requires an AIXIGO authentication token:

macOS / Linux
export X_ID_TOKEN="YOUR_TOKEN_HERE"
Windows (PowerShell)
setx X_ID_TOKEN "YOUR_TOKEN_HERE"

The token is read from the environment at runtime.
It is intentionally not hardcoded to preserve security.

Build & Execution

From inside api-client-java/:

Build
mvn clean install
Run
mvn exec:java -Dexec.mainClass="com.aixigo.sample_projects.api_client_java.Main"

API102 – Portfolio Analytics
Contract #1 — Multi-Asset European Portfolio

Investment Date: 2024-01-01

Positions
Asset	ISIN	Quantity
Deutsche Bank	DE0005140008	50
Citigroup	US1729674242	20
Amundi	FR0004125920	30
EUR Cash	CURRENCY:EUR.EUR	5000
Valuation Methodology

Resolve assets via ISIN

Create temporary contract

Retrieve asset snapshots at:

2024-01-01

2025-01-01

Compute portfolio value explicitly

Calculate total return manually

Portfolio Valuation (EUR)
Date	Value
2024-01-01	8023.949922888506
2025-01-01	9036.19212628742
Total Return 2024
Decimal: 0.1261526072728183
Percent: 12.6153%
Return Formula
Total Return = (EndValue - StartValue) / StartValue

Return is calculated explicitly in Java rather than relying on derived API metrics to ensure transparency.

Contract #2 — USD Cash Reallocation Strategy

This contract simulates reallocating only the European exposure into USD.

Step 1 – Extract EU-only Portfolio Value

Excluded:

Citigroup (USD equity)

Included:

Deutsche Bank

Amundi

EUR Cash

EU_ONLY_VALUE_EUR_2024_01_01 = 7092.443073573437
Step 2 – FX Conversion

FX rate retrieved from market snapshot:

EUR_PER_1_USD_2024_01_01 = 0.9071940488070398

Converted allocation:

CONTRACT2_USD_QTY = 7818.0 USD

100% invested into:

CURRENCY:USD.USD
Portfolio Valuation (EUR)
Date	Value
2024-01-01	7092.443073573437
2025-01-01	7525.26710944268
Total Return 2024
Decimal: 0.06102608528251041
Percent: 6.1026%

This demonstrates correct FX handling and valuation consistency in base currency.

API101 – Client Profiling
Implementation Strategy

The profiling workflow follows correct HTTP concurrency semantics:

Create Risk Profile

Submit questionnaire responses

Read Last-Modified header

Re-submit with justification

Use If-Unmodified-Since header for safe update

Prevent lost-update scenarios

This ensures proper conditional update handling aligned with REST best practices.

Sample Execution Output
=============================
API102 RESULTS
=============================
Temporary Contract 1 ID:
contract_8bdc0b86-2e43-4004-bacb-4ed390d73e1b

Contract 1 (European Portfolio)
Return 2024: 12.6153%

Temporary Contract 2 ID:
contract_be4b8dc3-cfb3-4317-81a7-53a0b189fb03

Contract 2 (USD Investment)
Return 2024: 6.1026%

=============================
API101 RESULTS
=============================
Risk Profile ID:
c4cab952-5b60-47c9-84f5-fc3d0a7dbac0
Design Considerations

Clear separation between orchestration and API communication

Deterministic financial calculations

No hidden side effects

Explicit FX conversion logic

Clean console reporting

Environment-based configuration

Maven-standard project layout

Minimal but structured test coverage

What This Demonstrates

Strong understanding of REST integration

Financial domain reasoning (portfolio valuation, return math, FX logic)

HTTP header semantics and concurrency control

Clean backend project organization

Production-ready coding discipline

Author

Anand Narayana Perumal
QA Engineer
Financial Systems & API Integration
