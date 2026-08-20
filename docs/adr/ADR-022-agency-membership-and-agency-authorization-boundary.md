# ADR-022 — Agency Membership and AGENCY Authorization Boundary

- **Status:** Accepted
- **Date:** 2026-08-21
- **Phase:** 3 — Agencies & Fleet

## Context

AutoRent Pro is a modular monolith for a single rental company operating through multiple physical agencies.

Phase 2 introduced the Identity & Security foundation with:

- users;
- roles;
- permissions;
- scopes `SELF`, `AGENCY` and `GLOBAL`;
- stateful HTTP session authentication;
- fresh server-side authorization decisions;
- deny-by-default behavior;
- anti-IDOR protections.

During Phase 2, the `AGENCY` scope was intentionally left fail-closed because no real user-to-agency membership model existed yet.

The current rule is:

```text
permission @ AGENCY
+
no real agency assignment
=
DENY
```

Phase 3 must introduce real agency membership without creating cyclic dependencies between the `identity` and `agency` modules.

## Decision

### 1. Users may belong to multiple agencies

AutoRent Pro adopts a many-to-many relationship between users and agencies.

A user may be actively assigned to zero, one or several agencies.

This supports cases such as:

- an employee assigned to one agency;
- a manager supervising several agencies;
- a temporary employee working in multiple agencies;
- a regional employee operating across several locations.

The model must not impose:

```text
1 user = 1 agency
```

### 2. Membership is represented by UserAgencyAssignment

The agency module owns the user-to-agency assignment concept.

Conceptual model:

```text
UserAgencyAssignment

id
userId
agencyId
active
assignedAt
endedAt
createdAt
updatedAt
```

The following combination is unique:

```text
(userId, agencyId)
```

An assignment is not physically deleted when it is revoked.

Instead:

```text
active = false
endedAt = timestamp
```

This preserves historical information and supports future auditing.

### 3. No cross-module JPA entity association

The `agency` module must not use a JPA association such as:

```text
@ManyToOne
UserAccount user
```

It stores:

```text
UUID userId
```

Likewise, modules referencing an agency should normally persist:

```text
UUID agencyId
```

rather than importing the `Agency` JPA entity into their persistence model.

SQL foreign keys remain allowed and encouraged where appropriate.

This rule protects modular boundaries while preserving relational integrity.

### 4. Identity owns authorization semantics

The `identity` module remains responsible for:

- permissions;
- scopes;
- roles;
- authorization decisions;
- authenticated actor resolution.

The `agency` module is responsible for answering factual questions about agency membership.

It must not reimplement the permission model.

### 5. Agency membership is resolved through a narrow contract

Identity depends only on a small abstraction representing agency membership.

Conceptually:

```text
AgencyScopeMembershipResolver
```

The contract answers questions such as:

```text
is user U actively assigned to agency A?
```

The implementation lives on the agency side and uses `UserAgencyAssignment`.

The authorization layer must not directly query an Agency repository or a UserAgencyAssignment repository.

### 6. The AGENCY scope requires active membership

An authorization decision for an agency-scoped resource requires:

```text
required permission
+
AGENCY scope grant
+
active membership in the target agency
+
business rule
=
ALLOW
```

If any condition fails:

```text
DENY
```

Example:

```text
VEHICLE_READ @ AGENCY
+
active assignment to Agency A
+
vehicle belongs to Agency A
=
ALLOW
```

Another example:

```text
VEHICLE_READ @ AGENCY
+
active assignment to Agency A
+
vehicle belongs to Agency B
=
DENY
```

### 7. GLOBAL scope does not require agency membership

A valid global permission may authorize access independently of local agency assignments.

Example:

```text
VEHICLE_READ @ GLOBAL
+
target vehicle belongs to any agency
=
ALLOW
```

Business rules may still deny an operation even when permission scope is global.

### 8. AGENCY remains fail-closed

Any ambiguous or incomplete agency context must deny access.

```text
AGENCY permission + missing agencyId
→ DENY
```

```text
AGENCY permission + missing assignment
→ DENY
```

```text
AGENCY permission + inactive assignment
→ DENY
```

```text
AGENCY permission + unknown agency
→ DENY
```

No fallback to GLOBAL or another agency is allowed unless the actor actually owns the corresponding GLOBAL grant.

### 9. Membership is resolved from current state

Agency memberships used in sensitive authorization decisions must reflect the current persistence state.

The application must not rely only on an agency list copied into the HTTP session at login.

Therefore:

```text
assignment revoked
→ access must disappear without requiring a new login
```

and:

```text
assignment added
→ access may become available without rebuilding the authentication session
```

subject to the actor's current permissions and business rules.

This follows the authorization freshness principle already adopted in Phase 2.

### 10. Resource agency is derived server-side

The client must not be trusted to declare the agency used for authorization when that information can be derived from the target resource.

Example:

```text
GET /api/vehicles/{vehicleId}
```

must conceptually perform:

```text
load vehicle
→ obtain vehicle.currentAgencyId
→ authorize against that agency
→ return resource
```

It must not authorize solely from a client-provided agency identifier.

This is required for anti-IDOR protection.

### 11. Moving a resource changes its authorization boundary

When an agency-owned resource moves between agencies, authorization must immediately use the new current agency.

Example:

```text
Vehicle V:
Agency A → Agency B
```

After the transfer:

```text
user assigned only to Agency A
→ access to V denied
```

```text
user assigned to Agency B
→ access evaluated normally
```

No stale agency membership or session snapshot may preserve access.

### 12. Agency roles do not replace membership

Having a role such as:

```text
AGENCY_AGENT
AGENCY_MANAGER
```

does not by itself grant access to every agency.

Roles provide permission grants.

Membership determines the agency boundary in which an `AGENCY` grant can be used.

Therefore:

```text
AGENCY_MANAGER
+
no active agency assignment
=
no agency-scoped access
```

### 13. Assignment management is explicitly authorized

Managing user-to-agency assignments requires a dedicated permission:

```text
USER_AGENCY_ASSIGN
```

Initial policy:

```text
USER_AGENCY_ASSIGN @ GLOBAL
```

for trusted administrative roles.

Agency-scoped users must not be able to arbitrarily assign users to other agencies unless a future business requirement explicitly introduces such delegation.

### 14. Deny-by-default remains mandatory

No module may bypass the authorization decision model by directly checking:

- a role name;
- a UUID;
- an agency code;
- a frontend-visible flag;
- a session-cached membership list.

Authorization continues to follow:

```text
Permission
+
Scope
+
Resource context
+
Membership
+
Business Rule
```

## Dependency Direction

The conceptual dependency is:

```text
identity
   |
   | defines narrow authorization membership contract
   v
AgencyScopeMembershipResolver
   ^
   |
   | implemented using agency persistence
   |
agency
```

The intent is:

```text
identity does not import agency domain entities
agency does not reimplement identity authorization
```

The exact Java package placement may be adapted during implementation if necessary to preserve this modular direction.

## Consequences

### Positive

- real `AGENCY` authorization becomes possible;
- users can work across multiple agencies;
- permissions remain centralized in Identity;
- agency membership stays owned by the Agency domain;
- no cross-module JPA graph is introduced;
- authorization remains fresh;
- anti-IDOR behavior is consistent for agency-owned resources;
- future fleet, reservation and rental modules can reuse the same boundary.

### Negative

- agency-scoped authorization may require additional database lookups;
- membership revocation must be handled transactionally;
- the implementation needs a carefully defined inter-module contract;
- authorization tests become more extensive.

These costs are accepted because correctness and isolation are more important than premature optimization.

Caching may be considered later only if profiling demonstrates a real need, and any cache must preserve authorization freshness.

## Alternatives Rejected

### Store one agencyId directly on UserAccount

Rejected because it would prevent legitimate multi-agency assignments and couple Identity to the organizational structure.

### Store Agency as a JPA relation inside UserAccount

Rejected because it creates strong persistence coupling between modules and encourages bidirectional dependencies.

### Store agency IDs inside the HTTP session

Rejected because membership changes would become stale until session renewal.

### Authorize only from role names

Rejected because roles such as `AGENCY_MANAGER` do not identify the agencies the actor is allowed to manage.

### Let every module query UserAgencyAssignment directly

Rejected because agency-membership authorization rules would become duplicated across modules.

## Security Invariants

The following invariants are mandatory:

```text
UUID != authorization
```

```text
role != agency membership
```

```text
AGENCY without active assignment = DENY
```

```text
inactive assignment = DENY
```

```text
wrong agency = DENY
```

```text
GLOBAL requires a real GLOBAL permission grant
```

```text
resource agency context is derived server-side whenever possible
```

```text
membership revocation must affect subsequent sensitive requests
```

## Testing Requirements

Phase 3 must explicitly verify:

1. AGENCY permission without assignment → DENY.
2. AGENCY permission with active assignment to Agency A → access to A evaluated normally.
3. User assigned to Agency A accessing Agency B → DENY.
4. GLOBAL permission → access across agencies according to business rules.
5. Disabled assignment → subsequent access denied.
6. Reactivated assignment → authorization reflects current state.
7. Revoked role/permission → current authorization reflects the change.
8. Vehicle moved from Agency A to Agency B → former Agency A user loses agency-scoped access.
9. Knowledge of a resource UUID alone never bypasses agency authorization.
10. Agency role without agency membership never grants local access.

## Deferred Decisions

This ADR does not define:

- hierarchical regions above agencies;
- temporary assignment approval workflows;
- assignment scheduling;
- cross-company tenancy;
- agency groups;
- fine-grained delegation between agency managers;
- authorization caching.

These concerns will be introduced only when justified by concrete business requirements.

## Final Decision

AutoRent Pro adopts a many-to-many, persistent and dynamically resolved user-to-agency membership model.

The `AGENCY` authorization scope is defined as:

```text
Permission
+
AGENCY scope
+
active UserAgencyAssignment
+
resource agency context
+
business rule
=
authorization decision
```

This boundary becomes the standard authorization model for future agency-owned resources in AutoRent Pro.
