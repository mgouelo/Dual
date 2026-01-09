# 🦊 Dual – Application d’évaluation de biathlon scolaire  
**Android / Kotlin**

## 📘 Présentation du projet

**Dual** (Cycle de Biathlon en Éducation Physique) est une application mobile Android destinée à accompagner les **enseignants d’Éducation Physique et Sportive** lors des **séances de biathlon scolaire**, notamment en classe de 6e et 4e.

Le projet est développé dans le cadre d’une **SAE (Situation d’Apprentissage et d’Évaluation)** du **BUT Informatique de l’IUT de Vannes**, en collaboration avec le **Collège Notre-Dame-La-Blanche (Theix-Noyalo)**.

L’objectif de l’application est de proposer une solution **locale, autonome et hors connexion Internet**, permettant de faciliter la collecte des résultats et l’évaluation des performances des élèves lors des séances de biathlon combinant :
- course à pied,
- tir laser,
- calculs liés à la VMA,
- pénalités associées aux tirs manqués.

---

## 🎯 Objectifs du projet

Lors des séances de biathlon, les enseignants doivent gérer :
- le chronométrage des tours,
- le suivi des tirs réussis ou manqués,
- l’application des pénalités,
- l’adaptation des distances en fonction de la VMA.

Ces opérations sont généralement réalisées **manuellement**, ce qui peut entraîner des erreurs et une perte de temps pédagogique.

**Dual** vise à :
- centraliser la saisie des résultats des élèves ;
- automatiser les calculs nécessaires à l’évaluation ;
- offrir un retour immédiat aux élèves ;
- fonctionner **sans dépendance à Internet**, directement sur le terrain.

Un premier **prototype fonctionnel** est livré dans le cadre du **semestre 3**.

---

## 🌐 Architecture réseau

L’application est conçue pour fonctionner **en extérieur**, souvent sans accès à Internet.

La solution retenue repose sur un **réseau local ad-hoc** :
- la tablette de l’enseignant agit comme **point d’accès Wi-Fi et serveur local** ;
- les élèves se connectent directement à ce réseau ;
- aucun serveur externe n’est requis.

Les échanges reposent sur :
- **HTTP** pour les requêtes ;
- **SSE (Server-Sent Events)** pour la communication en temps réel.

Cette architecture garantit une **faible latence**, une **autonomie totale** et une **robustesse en conditions réelles**.

---

## 🛠 Technologies utilisées

### 📱 Application Android
- **Kotlin**
- **Jetpack Compose**
  - Interface utilisateur native
  - Performances élevées
  - Interface moderne et adaptée à l’usage terrain

### 🖥 Backend local
- **Ktor (Kotlin)**
  - Serveur embarqué dans l’application
  - Support HTTP et SSE
  - Léger et rapide

### 🗄 Données
- **SQLite + Room**
  - Base de données locale embarquée
  - Fonctionnement hors ligne
  - Stockage des résultats et données de séance

---

## 🧩 Fonctionnalités principales

### 👨‍🏫 Côté enseignant
- Inscription et connexion
- Gestion minimale des classes et des élèves (identification lors des séances)
- Lancement d’une séance de biathlon
- Génération d’un **QR code** pour permettre l’accès des élèves à la séance

### 🧑‍🎓 Côté élève
- Accès à la séance via le **scan du QR code**
- Saisie des résultats de tir
- Consultation immédiate des résultats calculés

### ⚙️ Aspects techniques
- Fonctionnement entièrement hors ligne
- Communication locale en temps réel
- Aucune dépendance à un service externe

---

## 📄 Documentation du projet

La documentation complète du projet (organisation, sprints, tâches, maquettes, comptes rendus et suivi du temps) est disponible sur Notion :

➡️ **Espace Notion du projet**  
https://www.notion.so/glenpotay/SAE-Biathlon-Dual-281e9d17fd43801ba251e12c31ab926d

---

## 👥 Équipe du projet

### 🎓 Client
**Thierry LE GOFF**  
Professeur d’Éducation Physique et Sportive  
Collège Notre-Dame-La-Blanche (Theix-Noyalo)  
Responsable du besoin pédagogique et validation fonctionnelle  
📧 legoff.thierry2@gmail.com

---


### 🎓 Enseignant référent
**Jean-François Kamp**  
Enseignant – IUT de Vannes  
Professeur référent du projet Biathlon

---


### 💻 Équipe de développement  
**IUT de Vannes – BUT Informatique**

- **Matthieu Gouelo**  
  Scrum Master & Développeur Full Stack

- **Marin Weis**  
  Responsable communication & Développeur Full Stack

- **Nolann Lescop**  
  Développeur Full Stack

- **Glen Potay**  
  Développeur Full Stack


