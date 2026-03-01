# API Clients (Java + TypeScript + Curl)

![CI (Java)](https://github.com/Anandpp/api-client-java-homework/actions/workflows/ci.yml/badge.svg)

This repository contains multiple client implementations for the AIXIGO demo APIs.

---

## 📁 Repository Structure

- `api-client-java/` — Java client (Maven) + examples
- `api-client-typescript/` — TypeScript client + examples
- `api-client-curl/` — curl examples / scripts
- `spec.analytics.json`, `spec.openapi.json`, `spec.openapi.yaml` — API specifications

---

## ⚙️ Prerequisites

- Java 11+ (Java 17 recommended)
- Maven 3.8+
- Node.js 18+ (for the TypeScript client)

---

## 🚀 Quick Start

### ▶ Java Client

```bash
cd api-client-java

# set token (example)
export X_ID_TOKEN="YOUR_TOKEN_HERE"

mvn clean test
mvn exec:java

▶ TypeScript Client
cd api-client-typescript

npm install

# set token (example)
export X_ID_TOKEN="YOUR_TOKEN_HERE"

npm run build
npm run start

▶ Curl Examples
cd api-client-curl

# run your curl scripts / commands here

🔐 Notes
Do not commit tokens to GitHub.
Each subproject contains its own README with detailed instructions.
