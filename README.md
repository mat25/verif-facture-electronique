# 📄 VerifFacture — Vérificateur de Facture Électronique

Application de **validation de factures électroniques** au format XML (UBL 2.1), conforme aux normes européennes (EN16931) et aux règles françaises de la réforme CTC (EXTENDED, BR-FR).

---

## 🏗️ Architecture

```
verif-facture-electronique/
├── backend/      → API REST Java / Spring Boot (moteur de validation)
└── frontend/     → Interface web Vue 3 + Vite
```

---

## ⚙️ Backend — API Spring Boot

**Technos :** Java 17+, Spring Boot, Maven, `ph-schematron`

### Ce qu'il fait

| Étape | Description |
|---|---|
| **1. Validation XSD** | Vérifie la structure du XML contre le schéma UBL Invoice 2.1 |
| **2. Validation Schematron** | Applique les règles métier selon le format choisi |

### Formats supportés

| Format | Schematrons appliqués |
|---|---|
| `EN16931` | Règles européennes de base (EN 16931) |
| `EXTENDED` | Règles CTC-FR EXTENDED + BR-FR (Chorus Pro / réforme française) |

### Résultats retournés

Chaque règle échouée indique :
- `🔴 [FATAL]` — erreur bloquante (la facture est non conforme)
- `🟡 [AVERTISSEMENT]` — règle non bloquante (recommandation)

### Endpoint

```
POST http://localhost:8080/api/v1/validation/ubl
Content-Type: multipart/form-data

Paramètres :
  file    → fichier XML de la facture
  format  → "EN16931" | "EXTENDED"
```

### Lancer le backend

```bash
cd backend
mvn spring-boot:run
```

L'API démarre sur **http://localhost:8080**.

---

## 🖥️ Frontend — Interface Vue 3

**Technos :** Vue 3, Vite, CSS Vanilla (thème sombre)

### Fonctionnalités

- 📋 Sélection du format de validation (EN16931 / EXTENDED)
- 📂 Glisser-déposer ou sélection du fichier XML
- 🔍 Appel à l'API backend et affichage des résultats
- Résultats color-codés : ✅ succès, ❌ échec, 🔴 fatal, 🟡 avertissement
- Bouton de réinitialisation pour relancer une validation

### Lancer le frontend

```bash
cd frontend
npm install   # première fois uniquement
npm run dev
```

L'interface est accessible sur **http://localhost:5173**.

---

## 🚀 Lancer le projet complet

Ouvrir **deux terminaux** :

```bash
# Terminal 1 — Backend
cd backend
mvn spring-boot:run

# Terminal 2 — Frontend
cd frontend
npm run dev
```

Puis ouvrir **http://localhost:5173** dans le navigateur.

---

## 📋 Prérequis

| Outil | Version minimale |
|---|---|
| Java | 17 |
| Maven | 3.8+ |
| Node.js | 18+ |
| npm | 9+ |
