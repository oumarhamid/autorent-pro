# Phase 2 — Identity & Security

**Statut : terminée**

La Phase 2 met en place le socle d'identité, d'authentification et d'autorisation d'AutoRent Pro.

Elle implémente la baseline définie dans :

- `docs/adr/ADR-021-authentication-authorization-security-baseline.md`

## Périmètre livré

- Modèle Identity persistant : utilisateurs, rôles et permissions.
- Scopes `SELF`, `AGENCY` et `GLOBAL`.
- Authentification stateful par session Spring Security.
- Protection CSRF et contre la fixation de session.
- Login, `/me`, logout et restauration de session frontend.
- Politique de mot de passe et changement obligatoire.
- Verrouillage après 5 échecs pendant 15 minutes.
- Activation et désactivation des comptes.
- Administration des utilisateurs et gestion des rôles.
- Résolution fraîche des permissions et protections anti-IDOR.
- Bootstrap contrôlé du premier administrateur.
- Routage et guards d'autorisation React.
- Tests automatisés backend et frontend.

## Architecture de sécurité

Les décisions sensibles sont centralisées autour de :

- `AuthorizationDecisionService`
- `IdentityAuthorization`
- `IdentityAccessService`
- `ResolvedIdentityAccess`

Le modèle d'autorisation est :

`Permission + Scope + Business Rule`

Le backend reste l'autorité finale. Les contrôles frontend servent uniquement à l'expérience utilisateur et ne remplacent jamais les contrôles serveur.

Le scope `AGENCY` reste fail-closed jusqu'à l'implémentation des affectations réelles utilisateur-agence en Phase 3 :

`AGENCY sans affectation réelle → DENY`

## Authentification et session

AutoRent Pro utilise :

- `HttpSession`
- `SecurityContext`
- `HttpSessionSecurityContextRepository`

Aucun JWT d'authentification n'est stocké dans le navigateur.

Aucune donnée d'authentification n'est persistée dans :

- `localStorage`
- `sessionStorage`

Le cookie de session est configuré avec :

- `HttpOnly=true`
- `SameSite=Lax`

En production HTTPS, il devra également utiliser `Secure=true`.

## CSRF et politique HTTP

Le backend utilise `HttpSessionCsrfTokenRepository`.

Le frontend récupère le token via :

`GET /api/auth/csrf`

Le token est conservé uniquement en mémoire et ajouté aux requêtes mutantes.

Les seules routes explicitement publiques sont :

- `GET /actuator/health`
- `GET /api/auth/csrf`
- `POST /api/auth/login`

Toutes les autres routes nécessitent une authentification valide.

## Comptes, mots de passe et autorisations

États de compte :

- `ACTIVE`
- `DISABLED`

Politique de mot de passe :

- minimum : 15 caractères ;
- maximum : 128 caractères ;
- mots de passe communs refusés ;
- réutilisation immédiate du mot de passe courant interdite.

Après 5 échecs de connexion, le compte est temporairement verrouillé pendant 15 minutes.

Le changement de mot de passe utilise :

`POST /api/account/change-password`

Si `mustChangePassword=true`, le frontend impose le passage par `/change-password`.

Les décisions sensibles utilisent l'état courant des permissions. Elles ne reposent pas directement sur `principal.permissions`, `principal.roles` ou `hasAuthority(...)`.

## Administration des utilisateurs

Principaux endpoints :

- `POST /api/users`
- `GET /api/users`
- `GET /api/users/{userId}`
- `POST /api/users/{userId}/enable`
- `POST /api/users/{userId}/disable`
- `PUT /api/users/{userId}/roles/{role}`
- `DELETE /api/users/{userId}/roles/{role}`

Exemples de permissions :

- `USER_READ @ GLOBAL`
- `USER_CREATE @ GLOBAL`
- `USER_ENABLE @ GLOBAL`
- `USER_DISABLE @ GLOBAL`
- `USER_ROLE_ASSIGN @ GLOBAL`

La création d'un utilisateur exige simultanément :

`USER_CREATE @ GLOBAL AND USER_ROLE_ASSIGN @ GLOBAL`

Les UUID sont uniquement des identifiants techniques :

`connaître un UUID != être autorisé à accéder à la ressource`

## Bootstrap du premier administrateur

Variables de configuration :

- `AUTORENT_BOOTSTRAP_ADMIN_ENABLED`
- `AUTORENT_BOOTSTRAP_ADMIN_EMAIL`
- `AUTORENT_BOOTSTRAP_ADMIN_PASSWORD`

Le bootstrap est désactivé par défaut :

`AUTORENT_BOOTSTRAP_ADMIN_ENABLED=false`

Aucun mot de passe réel n'est versionné dans Git.

Le bootstrap n'est exposé par aucun endpoint HTTP.

## Frontend Identity

Routes principales :

- `/login`
- `/change-password`
- `/app`
- `/app/users`
- `/forbidden`

Composants principaux :

- `AuthProvider`
- `AuthContext`
- `ProtectedRoute`
- `GuestRoute`
- `PasswordChangeRoute`
- `PermissionRoute`

Au démarrage, `GET /api/auth/me` restaure l'état réel de la session.

## Validation finale

### Backend

- 81 tests réussis.
- 0 échec.
- 0 erreur.
- `BUILD SUCCESS`.

### Frontend

- 11 tests réussis.
- `npm run build` : OK.
- `npm run lint` : 0 warning, 0 erreur.
- `npm audit` : 0 vulnérabilité.

### Audit sécurité

L'audit final confirme :

- aucune donnée d'authentification dans `localStorage` ;
- aucune donnée d'authentification dans `sessionStorage` ;
- aucune manipulation frontend de `document.cookie` ;
- absence de `hasAuthority(...)` dans les décisions sensibles ;
- résolution fraîche des permissions ;
- aucun secret ADMIN versionné.

## Limites connues

- Affectations utilisateur-agence : Phase 3.
- Workflow « mot de passe oublié » : non inclus actuellement.
- MFA : non requis pour la V1.
- Configuration HTTPS et `Secure=true` sur le cookie : à appliquer en production.

## Conclusion

**Phase 2 — Identity & Security : COMPLETED**

AutoRent Pro dispose désormais d'un socle d'identité et de sécurité cohérent, testé et réutilisable par les futurs modules métier.

La prochaine phase est :

**Phase 3 — Agencies & Fleet**

Elle introduira notamment les agences, la flotte et les affectations réelles nécessaires à l'utilisation complète du scope `AGENCY`.
