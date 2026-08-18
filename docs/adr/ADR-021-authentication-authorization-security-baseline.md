# ADR-021 — Authentication & Authorization Security Baseline

- **Projet :** AutoRent Pro
- **Statut :** Accepted
- **Phase :** Phase 2 — Authentification & Sécurité
- **Décision :** Security Baseline V1

## 1. Contexte

AutoRent Pro est une plateforme professionnelle de location et de gestion de véhicules construite sous la forme d'un monolithe modulaire.

Le frontend est une SPA React / TypeScript et le backend est une API Spring Boot.

La sécurité doit servir de fondation aux futurs modules :

- customer ;
- agency ;
- fleet ;
- availability ;
- reservation ;
- rental ;
- maintenance ;
- finance ;
- document ;
- notification ;
- analytics ;
- audit.

La sécurité ne doit pas reposer uniquement sur des rôles.

Le modèle d'autorisation retenu est :

**Role + Permission + Scope + Business Rule**

Le principe général est :

**DENY BY DEFAULT**

---

## 2. Décision d'authentification

AutoRent Pro V1 utilise une authentification **stateful basée sur une session HTTP Spring Security**.

Le navigateur reçoit un cookie de session.

AutoRent Pro V1 n'utilise pas de JWT d'authentification stocké dans :

- localStorage ;
- sessionStorage.

Le choix JWT / OAuth2 pourra être réévalué ultérieurement si AutoRent Pro expose une API destinée à :

- une application mobile indépendante ;
- des partenaires ;
- des clients externes ;
- des services distribués.

---

## 3. Session HTTP

La session est créée uniquement après une authentification réussie.

Le cookie de session doit être :

- HttpOnly ;
- Secure en production ;
- configuré avec une politique SameSite adaptée au déploiement.

La durée d'inactivité de la session est configurable.

Valeur initiale proposée :

**30 minutes**

Cette durée ne doit pas être codée en dur dans le code Java.

Le logout doit :

- invalider la session ;
- supprimer le SecurityContext ;
- rendre la session précédente inutilisable.

---

## 4. CSRF

La protection CSRF reste activée.

L'authentification étant basée sur un cookie automatiquement envoyé par le navigateur, les opérations HTTP modifiant l'état doivent être protégées.

La SPA React doit pouvoir récupérer un token CSRF puis le renvoyer pour les méthodes concernées.

Sont notamment concernées :

- POST ;
- PUT ;
- PATCH ;
- DELETE.

Le mécanisme exact sera configuré pendant l'implémentation Spring Security.

---

## 5. CORS

Le frontend et le backend peuvent être exposés sur des origines différentes selon l'environnement.

En développement, le proxy Vite doit être privilégié lorsque cela simplifie les échanges.

Si CORS est nécessaire, il doit être configuré explicitement.

Aucune politique CORS permissive globale du type `*` ne doit être utilisée pour les endpoints authentifiés.

---

## 6. Identité utilisateur

`User` représente une identité capable de se connecter.

`Customer` représente une personne cliente du domaine métier.

Ces deux concepts restent distincts.

Un Customer pourra ultérieurement être associé à un User lorsqu'un compte client existe.

Il ne faut jamais fusionner automatiquement `User` et `Customer`.

---

## 7. Identifiant utilisateur

Les utilisateurs utilisent un identifiant technique de type UUID.

L'adresse email sert d'identifiant de connexion.

L'email doit être :

- normalisé ;
- stocké de manière cohérente ;
- unique.

La normalisation doit notamment éviter les doublons liés à la casse.

---

## 8. Cycle de vie du compte

Les états persistants initiaux sont :

- ACTIVE ;
- DISABLED.

Le verrouillage lié aux tentatives de connexion n'est pas représenté comme un statut permanent.

Les attributs prévus sont :

- failedLoginAttempts ;
- lockedUntil.

Un compte est temporairement verrouillé lorsque `lockedUntil` est dans le futur.

Une désactivation administrative conserve l'utilisateur et son historique.

La suppression physique n'est pas utilisée pour désactiver un compte.

---

## 9. Protection contre le brute force

Baseline initiale :

- 5 échecs consécutifs ;
- verrouillage temporaire de 15 minutes.

Ces valeurs doivent être configurables.

Après une authentification réussie :

- failedLoginAttempts revient à zéro ;
- lockedUntil est supprimé ;
- lastLoginAt est mis à jour.

Les réponses publiques d'authentification ne doivent pas permettre de déterminer facilement :

- si un email existe ;
- si le mot de passe est incorrect ;
- si le compte est temporairement verrouillé.

Les détails nécessaires peuvent être journalisés côté serveur sans inclure de secret.

---

## 10. Mot de passe

Les mots de passe ne sont jamais stockés en clair.

La base de données conserve uniquement un hash.

Le backend utilise l'abstraction `PasswordEncoder` de Spring Security.

La stratégie doit permettre l'évolution future de l'algorithme de hachage.

Politique initiale :

- minimum 15 caractères ;
- prise en charge des phrases de passe ;
- pas d'obligation artificielle systématique de majuscule, chiffre ou symbole ;
- rejet de mots de passe manifestement trop courants ou interdits.

Les mots de passe ne doivent jamais apparaître :

- dans les logs ;
- dans les réponses API ;
- dans les traces d'erreur ;
- dans Git.

---

## 11. Changement de mot de passe

Un utilisateur authentifié peut changer son mot de passe.

L'opération exige :

- le mot de passe actuel ;
- le nouveau mot de passe.

Le backend doit :

1. vérifier le mot de passe actuel ;
2. valider le nouveau mot de passe ;
3. générer le nouveau hash ;
4. mettre à jour passwordHash ;
5. mettre à jour passwordChangedAt.

Les autres sessions de l'utilisateur pourront être invalidées lorsque cela est nécessaire pour des raisons de sécurité.

---

## 12. Réinitialisation de mot de passe

Le workflow public complet « mot de passe oublié » n'est pas implémenté tant qu'un véritable mécanisme sécurisé de remise de token n'est pas disponible.

Le futur workflow devra utiliser :

- un token aléatoire ;
- une durée de validité limitée ;
- un usage unique ;
- un canal de notification sécurisé.

Un reset administratif peut utiliser un mécanisme de mot de passe temporaire avec obligation de changement via `mustChangePassword`, sous réserve de son implémentation sécurisée.

---

## 13. Rôles

Les rôles initiaux sont :

- CLIENT ;
- AGENCY_AGENT ;
- AGENCY_MANAGER ;
- FLEET_MANAGER ;
- MANAGER ;
- ADMIN.

Un utilisateur peut posséder plusieurs rôles.

La relation ne doit donc pas être implémentée avec une simple colonne `role` dans `users`.

Une relation `UserRole` est utilisée.

---

## 14. Permissions de la Phase 2

Permissions liées au compte personnel :

- ACCOUNT_READ ;
- ACCOUNT_CHANGE_PASSWORD.

Permissions d'administration des utilisateurs :

- USER_READ ;
- USER_CREATE ;
- USER_UPDATE ;
- USER_ENABLE ;
- USER_DISABLE ;
- USER_ROLE_ASSIGN ;
- USER_AGENCY_ASSIGN.

Les permissions des futurs modules métier ne doivent pas être créées prématurément.

Elles seront introduites avec leurs modules respectifs.

---

## 15. Scopes

Les scopes initiaux sont :

- GLOBAL ;
- AGENCY ;
- SELF.

### GLOBAL

Accès à l'échelle de l'entreprise sous réserve des Business Rules.

### AGENCY

Accès limité aux agences autorisées pour l'utilisateur.

### SELF

Accès uniquement à l'identité ou aux ressources appartenant à l'utilisateur courant.

Un scope n'est jamais suffisant à lui seul pour accorder une autorisation.

---

## 16. RolePermission

Le scope est associé à une permission obtenue par un rôle.

Modèle conceptuel :

Role → RolePermission → Permission

`RolePermission` contient notamment :

- roleId ;
- permissionId ;
- scope.

Une permission effective dépend donc du couple :

**Permission + Scope**

---

## 17. Affectation des employés aux agences

La relation entre User et Agency est plusieurs-à-plusieurs.

Le concept retenu est :

`UserAgencyAssignment`

Attributs prévus :

- userId ;
- agencyId ;
- primary ;
- validFrom ;
- validUntil ;
- createdAt.

Un utilisateur interne peut être affecté à plusieurs agences.

Une seule affectation principale peut exister simultanément.

L'agence principale est une information fonctionnelle et d'interface.

Elle ne constitue jamais une règle d'autorisation.

---

## 18. Affectation active

Une affectation est considérée comme active lorsque :

- validFrom est antérieur ou égal au moment courant ;
- validUntil est absent ou postérieur au moment courant.

La fin d'une affectation doit être historisée via `validUntil` plutôt que par suppression automatique de la ligne.

---

## 19. Règles de rattachement par rôle

### CLIENT

Aucune affectation à une agence n'est nécessaire.

### AGENCY_AGENT

Au moins une affectation active est nécessaire pour utiliser les permissions de scope AGENCY.

### AGENCY_MANAGER

Au moins une affectation active est nécessaire pour utiliser les permissions de scope AGENCY.

### FLEET_MANAGER

Les affectations dépendent de son périmètre fonctionnel réel.

### MANAGER

Peut disposer de permissions GLOBAL.

Les affectations agence sont facultatives.

### ADMIN

Peut disposer de permissions GLOBAL.

Une affectation à une agence n'est pas nécessaire pour exercer un scope GLOBAL.

---

## 20. Une affectation agence n'accorde aucune permission

Le fait qu'un utilisateur soit affecté à une agence ne lui accorde aucun droit par lui-même.

Une autorisation de scope agence exige au minimum :

**Permission + Scope AGENCY + affectation active + Business Rule**

---

## 21. Protection anti-IDOR / BOLA

Un identifiant fourni par le frontend n'accorde jamais l'accès à une ressource.

UUID n'est pas synonyme d'autorisation.

Pour chaque ressource protégée, l'application doit déterminer :

1. l'utilisateur courant ;
2. l'opération demandée ;
3. la permission nécessaire ;
4. le scope effectif ;
5. la ressource cible ;
6. la relation entre l'utilisateur et la ressource ;
7. les Business Rules applicables.

---

## 22. SELF

Pour les opérations SELF, l'identité doit être dérivée du contexte de sécurité lorsque cela est possible.

Exemple recommandé :

`GET /api/auth/me`

ou des endpoints de type :

`/api/account/...`

Il faut éviter de demander au frontend de transmettre son propre `userId` lorsqu'il peut être obtenu directement depuis la session.

---

## 23. AGENCY

Pour une permission de scope AGENCY, la ressource cible doit appartenir au périmètre d'une affectation active de l'utilisateur.

Le frontend ne doit jamais effectuer le filtrage de sécurité.

Les listes doivent être filtrées côté serveur.

Une requête listant des ressources ne doit jamais charger toutes les données puis demander à React de cacher les éléments interdits.

---

## 24. Recherche des ressources

Lorsque cela est pertinent, les repositories doivent privilégier des recherches déjà contraintes par le périmètre autorisé.

Une ressource hors scope ne doit pas être retournée simplement parce que son UUID existe.

Les repositories du module restent privés au module.

Les autres modules doivent passer par les contrats publics du module concerné.

---

## 25. DTO et commandes explicites

Les entités JPA ne doivent jamais être exposées directement dans l'API.

Les entrées et sorties utilisent des DTO ou records dédiés.

Les opérations sensibles utilisent des commandes explicites.

Exemples :

- ChangePasswordRequest ;
- CreateUserRequest ;
- UpdateUserProfileRequest ;
- AssignRoleRequest ;
- DisableUserRequest ;
- AssignUserToAgencyRequest.

Il faut éviter les PATCH génériques permettant de modifier arbitrairement :

- rôle ;
- permissions ;
- statut ;
- passwordHash ;
- affectations agence ;
- données internes de sécurité.

---

## 26. ADMIN

ADMIN n'est pas un bypass universel de toutes les Business Rules.

Même un utilisateur possédant un scope GLOBAL doit respecter les invariants métier.

Exemple :

l'application peut interdire la désactivation du dernier ADMIN actif.

---

## 27. Autorisation HTTP et applicative

La sécurité fonctionne sur plusieurs niveaux.

### Niveau HTTP

Les routes sont protégées par Spring Security.

### Niveau application

Les services applicatifs contrôlent les permissions, scopes et règles liées aux ressources.

Les contrôles simples peuvent utiliser la sécurité par méthode.

Les contrôles complexes doivent rester dans du Java explicite, lisible et testable.

Il faut éviter les expressions d'autorisation complexes et difficiles à maintenir lorsque la décision dépend de plusieurs règles métier.

---

## 28. Endpoints d'authentification

Baseline de la Phase 2 :

### Public

- POST `/api/auth/login`
- GET `/api/auth/csrf`
- GET `/actuator/health`

### Authentifié

- GET `/api/auth/me`
- POST `/api/auth/logout`
- POST `/api/account/change-password`

### Administration

Les endpoints `/api/users/**` utilisent les permissions et scopes appropriés.

Tout endpoint non explicitement autorisé reste protégé ou refusé.

---

## 29. Login

Endpoint :

`POST /api/auth/login`

Le body contient uniquement les données nécessaires à l'authentification.

Après succès :

- l'Authentication est créée ;
- le SecurityContext est associé à la session ;
- les compteurs d'échec sont réinitialisés ;
- lastLoginAt est mis à jour.

Aucun hash ni détail interne de sécurité n'est retourné.

---

## 30. Utilisateur courant

Endpoint cible :

`GET /api/auth/me`

Il sert notamment à restaurer l'état d'authentification du frontend après un rechargement de page.

Le frontend ne doit pas considérer localStorage comme source de vérité de l'identité authentifiée.

La session serveur reste la source de vérité.

---

## 31. Logout

Endpoint :

`POST /api/auth/logout`

Le logout :

- invalide la session ;
- supprime le SecurityContext ;
- rend l'ancienne session inutilisable.

Réponse cible :

`204 No Content`

---

## 32. Réponses 401 et 403

### 401 Unauthorized

Utilisé lorsqu'aucune authentification valide n'est présente.

Exemple de code applicatif :

`UNAUTHENTICATED`

### 403 Forbidden

Utilisé lorsqu'un utilisateur authentifié ne dispose pas de l'autorisation nécessaire.

Exemple de code applicatif :

`ACCESS_DENIED`

Les réponses publiques ne doivent pas contenir de stack trace ni de détails internes.

---

## 33. Ressource hors périmètre

Pour certaines lectures par identifiant, une ressource inexistante et une ressource existante mais inaccessible peuvent toutes deux produire une réponse 404 lorsque cela réduit utilement la fuite d'information.

Cela ne remplace jamais le contrôle réel d'autorisation.

---

## 34. Audit et logs de sécurité

Les événements importants devront pouvoir être audités.

Exemples :

- authentification réussie ;
- authentification échouée ;
- verrouillage temporaire ;
- désactivation utilisateur ;
- réactivation utilisateur ;
- changement de rôle ;
- changement d'affectation agence ;
- changement ou reset de mot de passe.

Les logs ne doivent jamais contenir :

- mot de passe ;
- hash complet inutile ;
- token secret ;
- cookie de session.

La conception détaillée du module `audit` reste séparée.

---

## 35. Modèle conceptuel cible

Le modèle d'identité doit évoluer vers :

User
↕
UserRole
↕
Role
↕
RolePermission
↕
Permission

et :

User
↕
UserAgencyAssignment
↕
Agency

Le module `identity` ne doit pas dépendre directement des repositories internes du futur module `agency`.

---

## 36. Limite Phase 2 / Phase 3

Le vrai module métier `agency` appartient à la Phase 3.

La Phase 2 définit le concept de scope agence et les contrats nécessaires.

La persistance complète de `UserAgencyAssignment` ne doit pas provoquer la création prématurée de tout le domaine `agency`.

La stratégie exacte de migration sera décidée avant la création des tables afin de conserver des clés étrangères cohérentes.

---

## 37. Tests obligatoires

La Phase 2 devra au minimum démontrer :

- endpoint public accessible ;
- endpoint protégé inaccessible sans session ;
- authentification réussie ;
- authentification refusée ;
- mot de passe invalide ;
- compte désactivé ;
- compte temporairement verrouillé ;
- logout ;
- session expirée ou absente ;
- réponse 401 ;
- réponse 403 ;
- permission autorisée ;
- permission interdite ;
- scope SELF ;
- scope AGENCY ;
- scope GLOBAL ;
- utilisateur d'une agence A refusé sur une agence B ;
- accès SELF refusé à une ressource appartenant à un autre utilisateur ;
- manipulation d'UUID refusée ;
- listes filtrées côté serveur ;
- migrations Flyway valides ;
- PostgreSQL réel via Testcontainers.

Les tests ne doivent pas dépendre du PostgreSQL local du développeur.

---

## 38. Frontend Phase 2

L'intégration frontend reste volontairement limitée à :

- écran de connexion ;
- état de l'utilisateur courant ;
- logout ;
- gestion 401 ;
- gestion 403 ;
- routes protégées ;
- affichage conditionnel selon les autorisations.

Le dashboard métier complet ne fait pas partie de cette phase.

---

## 39. Technologies explicitement non requises

La Phase 2 n'introduit pas sans besoin démontré :

- Redis ;
- Kafka ;
- RabbitMQ ;
- Kubernetes ;
- microservices ;
- Machine Learning ;
- OAuth social ;
- MFA complexe.

---

## 40. Conséquences positives

Cette décision apporte :

- révocation simple des sessions ;
- logout simple ;
- absence de JWT stocké dans le navigateur ;
- modèle d'autorisation extensible ;
- séparation Role / Permission / Scope ;
- support du multi-agence ;
- protection anti-IDOR structurée ;
- base réutilisable par les futurs modules métier ;
- possibilité d'évolution vers d'autres mécanismes d'authentification ultérieurement.

---

## 41. Coûts et contraintes

Cette décision implique :

- gestion de session côté serveur ;
- protection CSRF ;
- gestion explicite des scopes ;
- contrôles d'autorisation au niveau des ressources ;
- tests de sécurité plus complets ;
- prise en compte future de la stratégie de session lors d'un déploiement multi-instance.

Ces contraintes sont acceptées car elles correspondent aux besoins actuels d'AutoRent Pro.

---

## 42. Décisions différées

Sont volontairement différés :

- OAuth2 / OpenID Connect ;
- connexion sociale ;
- MFA ;
- API publique partenaire ;
- authentification mobile native ;
- reset de mot de passe par email complet ;
- stockage distribué des sessions ;
- stratégie Redis éventuelle ;
- SSO entreprise.

Ces éléments devront faire l'objet de nouveaux ADR si un besoin réel apparaît.

---

## 43. Décision finale

AutoRent Pro V1 adopte :

**Session Spring Security + Cookie HttpOnly + CSRF**

avec un modèle d'autorisation :

**Role + Permission + Scope + Business Rule**

et une stratégie de sécurité :

**DENY BY DEFAULT + contrôle d'autorisation au niveau des ressources**

Cette ADR constitue la baseline de sécurité de la Phase 2.
