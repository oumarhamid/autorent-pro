# AutoRent Pro

Plateforme moderne de location et de gestion de véhicules.

AutoRent Pro a pour objectif de couvrir l'ensemble du cycle de vie d'une location automobile, depuis la recherche et la disponibilité d'un véhicule jusqu'à la restitution, la facturation et l'historisation des opérations.

## Statut du projet

**Phase 1 — Fondation technique : terminée.**

La fondation technique comprend :

- backend Spring Boot ;
- frontend React / TypeScript ;
- PostgreSQL ;
- migrations Flyway ;
- Docker Compose ;
- profils de configuration dédiés ;
- tests Spring Boot ;
- tests d'intégration PostgreSQL avec Testcontainers ;
- health check Spring Boot Actuator ;
- communication frontend/backend validée ;
- gestion externalisée de la configuration locale.

## Vision fonctionnelle

AutoRent Pro doit progressivement couvrir le cycle métier complet d'une location automobile :

```text
Client
→ recherche
→ disponibilité
→ réservation
→ contrat
→ prise en charge du véhicule
→ location
→ restitution
→ inspection
→ paiement
→ facture
→ historique
```

La plateforme intégrera également :

- gestion des agences ;
- gestion des utilisateurs, rôles et permissions ;
- gestion des clients ;
- gestion de la flotte ;
- disponibilité des véhicules ;
- réservations ;
- contrats et locations ;
- cautions et paiements ;
- facturation ;
- inspections et dommages ;
- assurances ;
- maintenance ;
- documents ;
- notifications ;
- tableaux de bord et statistiques ;
- audit des opérations.

## Architecture

AutoRent Pro adopte une architecture **Monolithe Modulaire**.

Le backend sera progressivement structuré par domaines fonctionnels :

```text
identity
customer
agency
fleet
availability
reservation
rental
maintenance
finance
document
notification
analytics
audit
```

Chaque module doit conserver une responsabilité claire et limiter les dépendances directes avec les autres modules.

Les échanges inter-modules devront passer par des contrats publics, façades ou ports clairement définis.

## Stack technique

### Backend

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway
- Maven
- Spring Boot Actuator

### Tests

- JUnit 5
- Spring Boot Test
- Testcontainers
- PostgreSQL Testcontainer

### Frontend

- React
- TypeScript
- Vite
- Tailwind CSS

### Infrastructure locale

- Docker
- Docker Compose
- PostgreSQL 17

## Structure du dépôt

```text
autorent-pro/
├── backend/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── frontend/
│   ├── src/
│   ├── public/
│   └── package.json
├── docs/
├── infrastructure/
├── scripts/
├── .github/
│   └── workflows/
├── .env.example
├── .gitignore
├── docker-compose.yml
└── README.md
```

## Prérequis

Pour exécuter le projet localement :

- Java 21
- Node.js
- npm
- Docker
- Docker Compose
- Git

Maven n'a pas besoin d'être installé globalement : le projet utilise **Maven Wrapper**.

## Configuration locale

Créer un fichier `.env` à partir de `.env.example`.

Exemple :

```env
POSTGRES_DB=autorent
POSTGRES_USER=autorent
POSTGRES_PASSWORD=change_me
POSTGRES_PORT=55432

BACKEND_PORT=8080
FRONTEND_PORT=5173
```

Le fichier `.env` est volontairement ignoré par Git.

Aucun secret réel ne doit être ajouté au dépôt.

## Démarrer PostgreSQL

Depuis la racine du projet :

```powershell
docker compose --env-file .env up -d postgres
```

Vérifier son état :

```powershell
docker compose --env-file .env ps
```

PostgreSQL est exposé localement sur :

```text
localhost:55432
```

Le port `55432` est utilisé côté hôte afin d'éviter les conflits avec une éventuelle installation PostgreSQL locale utilisant déjà le port `5432`.

## Démarrer le backend

Se placer dans le dossier :

```powershell
cd .\backend
```

Puis démarrer Spring Boot :

```powershell
.\mvnw.cmd spring-boot:run
```

Le backend est accessible par défaut sur :

```text
http://localhost:8080
```

## Health check backend

Spring Boot Actuator expose :

```text
http://localhost:8080/actuator/health
```

Résultat attendu :

```json
{
  "status": "UP"
}
```

## Démarrer le frontend

Depuis la racine :

```powershell
cd .\frontend
npm install
npm run dev
```

Le frontend est accessible par défaut sur :

```text
http://localhost:5173
```

Le serveur Vite redirige les requêtes `/actuator` vers le backend local.

La communication frontend/backend a été validée avec l'affichage :

```text
Backend opérationnel
```

## Build frontend

```powershell
cd .\frontend
npm run build
```

## Tests backend

Depuis le dossier `backend` :

```powershell
.\mvnw.cmd clean test
```

La suite de tests de fondation valide :

- le chargement du contexte Spring Boot ;
- le profil de test isolé ;
- le démarrage d'un PostgreSQL réel via Testcontainers ;
- l'application des migrations Flyway ;
- la présence de la migration baseline ;
- l'exécution d'une requête SQL réelle sur PostgreSQL.

État validé à la fin de la Phase 1 :

```text
Tests run: 3
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

## Flyway

Les migrations se trouvent dans :

```text
backend/src/main/resources/db/migration/
```

Migration initiale :

```text
V1__baseline.sql
```

Cette migration initialise l'historique Flyway sans créer encore de tables métier.

Les tables métier seront ajoutées progressivement dans les phases fonctionnelles suivantes.

## Profils Spring

### dev

Utilisé pour le développement local avec PostgreSQL.

```text
backend/src/main/resources/application-dev.yml
```

### test

Utilisé pour les tests ne nécessitant pas de base de données réelle.

```text
backend/src/main/resources/application-test.yml
```

### integration-test

Utilisé pour les tests d'intégration avec Testcontainers.

```text
backend/src/test/resources/application-integration-test.yml
```

## Sécurité des secrets

Les fichiers contenant les secrets locaux ne doivent jamais être commités.

Le dépôt ignore notamment :

```text
.env
.env.local
.env.*.local
*.key
*.pem
*.p12
*.pfx
```

Le fichier `.env.example` contient uniquement des valeurs d'exemple.

## Principes d'ingénierie

Le projet suit les principes suivants :

- architecture guidée par les besoins métier ;
- monolithe modulaire avant toute éventuelle distribution ;
- séparation claire des responsabilités ;
- repositories internes aux modules ;
- pas d'exposition directe des entités JPA ;
- migrations de base de données versionnées ;
- validation automatisée ;
- sécurité par défaut ;
- configuration externalisée ;
- tests d'intégration sur PostgreSQL réel ;
- documentation progressive ;
- absence de technologies ajoutées uniquement pour complexifier l'architecture.

Redis, Kafka, RabbitMQ, Kubernetes, Machine Learning ou une architecture microservices ne seront introduits que lorsqu'un besoin concret le justifiera.

## Roadmap

```text
Phase 0  — Cadrage et architecture
Phase 1  — Fondation technique
Phase 2  — Authentification et sécurité
Phase 3  — Agences et flotte
Phase 4  — Disponibilité et réservation
Phase 5  — Location
Phase 6  — Finance
Phase 7  — Maintenance
Phase 8  — Dashboard et analytics
Phase 9  — Services complémentaires
Phase 10 — Robustesse et qualité
Phase 11 — Observabilité
Phase 12 — CI/CD
Phase 13 — Cloud
Phase 14 — Fleet Intelligence
```

## État actuel

- Phase 0 — Cadrage et architecture : **terminée**
- Phase 1 — Fondation technique : **terminée**
- Phase 2 — Authentification et sécurité : **prochaine étape**

---

**AutoRent Pro — Architecture & Domain Baseline v1.0**