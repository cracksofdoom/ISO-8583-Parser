# Build and deployment commands

This project uses Maven Wrapper 3.3.4 with Maven 3.10.1. Run `./mvnw` on macOS/Linux or `mvnw.cmd` on Windows. The [makefile](makefile) provides shortcuts such as `make test`, `make package`, and `make docker-up`.

The application is a stateless ISO 8583 parser. Compose starts only the site and API; it does not need PostgreSQL, JWT secrets, or Swagger settings. See [README.md](README.md) for usage and [docs/openapi.yaml](docs/openapi.yaml) for the API contract.
