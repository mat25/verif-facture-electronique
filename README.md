# 📄 Vérificateur de Facture Électronique

Ce projet permet de **valider la conformité structurelle et métier des factures électroniques** au format XML (notamment UBL 2.1), conformément aux normes européennes (EN16931) et aux spécificités françaises (EXTENDED, BR-FR).

## 🚀 Architecture du projet

Le projet est pensé pour être découpé en deux grandes parties : un moteur de validation (Backend) et une interface utilisateur (Frontend).

### 1. Backend (Disponible) ⚙️
Une API REST développée en **Java / Spring Boot**. C'est le cœur métier de l'application qui se charge de vérifier les factures entrantes.
- **Dossier :** `/backend`
- **Validation XSD** : Vérifie que la structure du fichier XML respecte strictement le dictionnaire et la syntaxe UBL 2.1.
- **Validation Schematron** : Utilise la librairie `ph-schematron` pour appliquer un ensemble de règles métier conditionnelles selon le format demandé (EN16931 standard européen, ou EXTENDED avec règles françaises).

### 2. Frontend (À venir) 🖥️
Une interface utilisateur (UI) viendra prochainement compléter l'API. Elle permettra :
- Le téléchargement (drag & drop) des factures XML par l'utilisateur.
- La sélection du profil de vérification souhaité.
- Un affichage clair et lisible des rapports d'erreurs (formatage des retours XSD et assertions échouées issues de SVRL).

---

## 🛠️ Trépied technique & Prérequis

**Backend**
- Java 17 (ou supérieur)
- Maven
- Framework : Spring Boot

## 🏃 Comment lancer le projet en local

### Démarrer l'API (Backend)
1. Ouvrez un terminal à la racine du projet.
2. Naviguez dans le répertoire backend :
   ```bash
   cd backend
   ```
3. Démarrez l'application via Maven :
   ```bash
   mvn spring-boot:run
   ```
4. L'API sera accessible localement (ex: sur `http://localhost:8080`).

### Démarrer le Frontend
*(Partie en cours de construction - Les instructions d'installation et de lancement arriveront avec l'initialisation du front-end !)*

---

## 📌 Formats supportés

Actuellement, le validateur embarque les Schematrons pour :
- **EN16931** (Norme Européenne de base)
- **EXTENDED** + **BR-FR** (Règles spécifiques métiers françaises / Chorus Pro etc.)
