# Backend services

The team keeps backend code in `server/` and frontend code in `web/`.
Create feature branches from `test` and target pull requests at `test`,
with titles such as `[NT-01] Add login`. See the shared
[repository conventions](../../docs/conventions.md#1-repository-phân-công-và-git).

All eight business services use the same package layout. The API gateway is
a separate module at `server/api-gateway/`.

```text
<name>-service/
  src/main/java/com/rikkei/jobs/<name>/
    <Name>Application.java
    controller/
    service/
    repository/
    entity/
    dto/
    mapper/
    security/
    client/
    exception/
  src/main/resources/
    application.yml
    db/migration/
  src/test/
    java/com/rikkei/jobs/<name>/
    resources/
  Dockerfile
  pom.xml
```

Empty directories contain `.gitkeep` placeholders. The `security` and
`client` packages do not configure authentication or service calls yet.
The existing Spring Security dependency on identity-service still applies
its default security until explicit configuration is added.

| Service | Application class | Default port |
| --- | --- | --- |
| identity-service | IdentityApplication | 8081 |
| profile-service | ProfileApplication | 8082 |
| company-service | CompanyApplication | 8085 |
| internship-service | InternshipApplication | 8086 |
| assessment-service | AssessmentApplication | 8087 |
| learning-service | LearningApplication | 8084 |
| recruitment-service | RecruitmentApplication | 8083 |
| ai-service | AiApplication | 8088 |

Responsibilities and data ownership are documented in the root [README](../../README.md#phạm-vi-các-service).

## Local configuration

Spring reads configuration from the process environment; copying the root
`.env.example` to `.env` does not automatically export it to Java.
Each service uses its corresponding `<NAME>_SERVICE_PORT` variable.

The seven JPA services use PostgreSQL and `ddl-auto: validate`.
Local defaults match the existing Compose database: `localhost:5432/rikkei_jobs`
with username/password `postgres`. Each JPA service supports its own
`<NAME>_DB_URL`, `<NAME>_DB_USER`, and `<NAME>_DB_PASSWORD`, where `<NAME>` is
`IDENTITY`, `PROFILE`, `COMPANY`, `INTERNSHIP`, `ASSESSMENT`, `LEARNING`, or
`RECRUITMENT`. Provision independent service databases before using their URLs.
Shared local fallback variables are `POSTGRES_HOST`, `POSTGRES_PORT`,
`POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`.

The migration directories are placeholders. No Flyway dependency or SQL
migrations are enabled yet. AI-service has no JPA dependency or datasource
configuration; its Gemini client and API key configuration are not implemented.

From `server/`, build an individual service and its shared library:

```sh
mvn -pl services/profile-service -am clean package
java -jar services/profile-service/target/profile-service-1.0.0-SNAPSHOT.jar
```

## Docker

Run from the repository root. The build context must be `server/` so Maven
can resolve the parent POM and common-observability module:

```sh
docker build -f server/services/profile-service/Dockerfile -t rikkei/profile-service:local server
docker run --rm -p 8082:8082 -e POSTGRES_HOST=host.docker.internal rikkei/profile-service:local
```

Replace the service name and port to build another service. Inside Docker,
`localhost` refers to the application container; use the database container's
DNS name on a shared network, or `host.docker.internal` with Docker Desktop
when PostgreSQL is published on the host.

