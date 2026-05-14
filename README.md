# User Management Service

Servizio REST in Java/Spring Boot per la gestione anagrafica utenti, sviluppato come progetto tecnico.

## Indice

- [Stack tecnologico](#stack-tecnologico)
- [Requisiti implementati](#requisiti-implementati)
- [Avvio locale](#avvio-locale)
- [Autenticazione](#autenticazione)
- [RBAC e permessi](#rbac-e-permessi)
- [Filtering dei campi](#filtering-dei-campi)
- [API Reference](#api-reference)
- [Evento asincrono](#evento-asincrono)
- [Schema del database](#schema-del-database)
- [Logging](#logging)
- [Test](#test)
- [Postman Collection](#postman-collection)

---

## Stack tecnologico

| Componente | Tecnologia |
|---|---|
| Linguaggio | Java 21 (LTS) |
| Framework | Spring Boot 3.3.5 |
| Persistenza | Spring Data JPA + Hibernate |
| Database | H2 in-memory (compatibile con MySQL / PostgreSQL / Oracle) |
| Migrazioni DB | Flyway |
| Sicurezza | Spring Security OAuth2 Resource Server (JWT) |
| IAM | Keycloak |
| API Docs | Springdoc OpenAPI / Swagger UI |
| Logging | SLF4J + Logback |
| Test | JUnit 5 + MockMvc + spring-security-test |

---

## Requisiti implementati

### Funzionali base (§2.1)
- [x] Lista utenti — `GET /api/users`
- [x] Dettaglio utente — `GET /api/users/{id}`
- [x] Creazione utente con ruoli — `POST /api/users`
- [x] Modifica utente e ruoli — `PUT /api/users/{id}`
- [x] Cancellazione utente — `DELETE /api/users/{id}`
- [x] Email unica e non modificabile (§1.1)

### Funzionali opzionali (§2.2)
- [x] Autenticazione JWT tramite Keycloak (§2.2.1)
- [x] RBAC con permessi granulari (§2.2.2)
- [x] Filtering dei campi in base al ruolo (§2.2.3)
- [x] Evento asincrono interno alla creazione utente (§2.2.4)

---

## Avvio locale

### Prerequisiti

- Java 21+
- Maven 3.8+

### Avvio

```bash
mvn spring-boot:run
```

L'applicazione si avvia su `http://localhost:8080`.

| Risorsa | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| H2 Console | http://localhost:8080/h2-console |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

**Credenziali H2:**

```
JDBC URL : jdbc:h2:mem:userdb
Username : sa
Password : password
```

---

## Autenticazione

Il servizio è configurato come **OAuth2 Resource Server** e valida i JWT emessi da Keycloak.
Ogni richiesta alle API deve includere un Bearer token nell'header `Authorization`.

### Parametri Keycloak

| Proprietà | Valore |
|---|---|
| Issuer URI | fornito dal recruiter |
| Token endpoint | fornito dal recruiter |
| Client ID | fornito dal recruiter |
| Client Secret | fornito dal recruiter |

### Ottenere un token (Resource Owner Password)

```bash
curl -s -X POST \
  "$KEYCLOAK_TOKEN_URL" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$KEYCLOAK_CLIENT_ID" \
  -d "client_secret=$KEYCLOAK_CLIENT_SECRET" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD"
```

---

## RBAC e permessi

Keycloak è preconfigurato con tre utenze. Il `KeycloakJwtAuthenticationConverter` legge il ruolo
dal claim `realm_access.roles` del JWT e lo espande nei permessi applicativi:

| Username | Ruolo Keycloak | Permessi |
|---|---|---|
| `admin_user` | ADMIN | `read_user` `create_user` `update_user` `delete_user` |
| `creator_user` | OPERATOR | `read_user` `create_user` `update_user` |
| `reader_user` | USER | `read_user` |

I permessi vengono verificati tramite `@PreAuthorize` su ogni metodo del controller.
Le richieste non autorizzate restituiscono `403 Forbidden` (loggato a `WARN`).
Le richieste senza token restituiscono `401 Unauthorized` (loggato a `WARN`).

---

## Filtering dei campi

Le risposte vengono filtrate dinamicamente in base al ruolo dell'utente autenticato (§2.2.3):

| Ruolo | `taxCode` | `roles` | altri campi |
|---|---|---|---|
| ADMIN | visibile | visibile | visibili |
| OPERATOR | nascosto | visibile | visibili |
| USER | nascosto | nascosto | visibili |

I campi nascosti sono impostati a `null` nel mapper e omessi dal JSON grazie a
`@JsonInclude(NON_NULL)` su `UserResponse`, senza DTO separati per ruolo.

---

## API Reference

Tutti gli endpoint richiedono `Authorization: Bearer <token>`.

### GET /api/users — Lista utenti

```http
GET /api/users HTTP/1.1
Authorization: Bearer <token>
```

**Risposta `200 OK`:**
```json
[
  {
    "id": "43bedb70-4300-4e42-9eeb-ca10b1a06814",
    "username": "mario.rossi",
    "email": "mario.rossi@example.com",
    "taxCode": "RSSMRA80A01H501Z",
    "name": "Mario",
    "surname": "Rossi",
    "roles": ["DEVELOPER", "REPORTER"]
  }
]
```

---

### GET /api/users/{id} — Dettaglio utente

```http
GET /api/users/43bedb70-4300-4e42-9eeb-ca10b1a06814 HTTP/1.1
Authorization: Bearer <token>
```

**Risposta `200 OK`:** (stessa struttura della lista)

**Risposta `404 Not Found`:**
```json
{
  "status": 404,
  "detail": "User not found with id: 43bedb70-4300-4e42-9eeb-ca10b1a06814"
}
```

---

### POST /api/users — Crea utente

```http
POST /api/users HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "username": "mario.rossi",
  "email": "mario.rossi@example.com",
  "taxCode": "RSSMRA80A01H501Z",
  "name": "Mario",
  "surname": "Rossi",
  "roles": ["DEVELOPER", "REPORTER"]
}
```

**Risposta `201 Created`:** body con l'utente creato + header `Location: /api/users/{id}`

**Risposta `409 Conflict`** se l'email è già presente:
```json
{
  "status": 409,
  "detail": "A user with email 'mario.rossi@example.com' already exists"
}
```

**Risposta `400 Bad Request`** in caso di validazione fallita:
```json
{
  "status": 400,
  "detail": "Validation error",
  "errors": {
    "taxCode": "taxCode must contain 16 alphanumeric characters",
    "email": "must be a well-formed email address"
  }
}
```

> Ruoli applicativi accettati: `OWNER` `OPERATOR` `MAINTAINER` `DEVELOPER` `REPORTER`

---

### PUT /api/users/{id} — Modifica utente

L'email è assente dal body perché non modificabile (§1.1).

```http
PUT /api/users/43bedb70-4300-4e42-9eeb-ca10b1a06814 HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "username": "mario.rossi.updated",
  "taxCode": "RSSMRA80A01H501Z",
  "name": "Mario",
  "surname": "Rossi Aggiornato",
  "roles": ["OWNER", "MAINTAINER"]
}
```

**Risposta `200 OK`:** body con l'utente aggiornato

---

### DELETE /api/users/{id} — Elimina utente

```http
DELETE /api/users/43bedb70-4300-4e42-9eeb-ca10b1a06814 HTTP/1.1
Authorization: Bearer <token>
```

**Risposta `204 No Content`**

---

## Evento asincrono

Alla creazione di ogni utente viene pubblicato un `UserCreatedEvent` tramite il bus interno di
Spring (`ApplicationEventPublisher`). Il listener lo consuma su un thread separato grazie a
`@Async` + `@EnableAsync`, garantendo che la risposta HTTP non sia bloccata dall'elaborazione
dell'evento.

`@TransactionalEventListener(phase = AFTER_COMMIT)` assicura che l'evento parta solo se la
transazione su DB è committata con successo: in caso di rollback l'evento non viene mai consegnato.

Tecnologia scelta: **evento interno applicativo** (nessun broker esterno richiesto), come da §2.2.4.

---

## Schema del database

```sql
CREATE TABLE users (
    id         UUID         PRIMARY KEY,
    username   VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    tax_code   VARCHAR(16)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    surname    VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE roles (
    id        BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id   UUID         NOT NULL,
    role_type VARCHAR(30)  NOT NULL,
    CONSTRAINT fk_roles_user        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_roles_user_role_type UNIQUE (user_id, role_type)
);
```

Le migrazioni sono gestite da **Flyway** (`src/main/resources/db/migration/V1__create_users_and_roles.sql`).

`created_by` e `updated_by` tracciano lo `preferred_username` Keycloak dell'utente che ha
eseguito l'operazione (§2.2.1 — tracciamento applicativo dell'utente loggato).

---

## Logging

Il logging è configurato in `src/main/resources/logback-spring.xml` con comportamento diverso
per profilo:

| Profilo | Output | Formato |
|---|---|---|
| `dev` (default) | Console | Pattern colorato leggibile + query SQL Hibernate |
| `prod` | Console + File rolling | JSON strutturato (indicizzabile da ELK/Datadog) |

**File rolling (profilo `prod`):**
- Un file per giorno, compressi in `.gz`
- Max 50 MB per file, 30 giorni di retention, tetto totale 500 MB
- Path: `logs/user-management-service.log`

**Avvio in modalità prod:**
```bash
java -jar target/user-management-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Test

```bash
mvn test
```

I test di integrazione (`UserControllerIntegrationTest`) usano `MockMvc` e `spring-security-test`:
non richiedono una connessione reale a Keycloak — le authority vengono iniettate direttamente
nel `SecurityContext` tramite `@WithMockUser` / `SecurityMockMvcRequestPostProcessors`.

---

## Postman Collection

Il file `postman_collection.json` nella root del progetto contiene:

- **Auth** — tre request per ottenere il token JWT di `admin_user`, `creator_user` e `reader_user`;
  il token viene salvato automaticamente nella variabile di collezione `{{token}}`
- **Users** — CRUD completo; la POST salva automaticamente l'`id` dell'utente creato in `{{user_id}}`
- **Error Cases** — scenari di errore (409, 404, 400, 403, 401)

**Come importare:** Postman → Import → seleziona `postman_collection.json`
