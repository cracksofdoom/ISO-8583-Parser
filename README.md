# Parser — ISO 8583 workbench

A Spring Boot 3 application with a browser workbench and JSON API for inspecting and comparing ISO 8583 frames. The browser site and API are served from the same origin. The existing `net.cracksofdoom.parser.field` library remains in the JAR.

## Run locally

Requires JDK 17+. The Maven wrapper downloads the configured Maven version automatically.

```sh
make test
make run
```

On Windows, use `mvnw.cmd test` and `mvnw.cmd spring-boot:run`.

Open <http://localhost:8080>. The health endpoint is `/actuator/health`.

## Deploy

```sh
make docker-config
make docker-up
```

Run `make docker-down` to stop the Compose service. Docker Compose reads optional host settings from `.env.example` after you copy it to `.env`.

Set `PORT` for a platform-assigned port when running the JAR directly. Set `APP_PORT` to change the host port in Compose. Put the app behind HTTPS at the ingress or reverse proxy. For a separately hosted frontend, set `APP_ALLOWED_ORIGINS` to comma-separated exact origins; the bundled site needs no CORS configuration. Put a shared rate limiter at the edge when running multiple instances.

The application stores neither messages nor results. The public API always masks PAN, track, PIN, ICC, MAC, security control and account ID fields. `revealSensitive: true` is rejected. Do not paste live card data into a publicly accessible deployment unless its surrounding infrastructure and operational controls meet your organization's requirements. The built-in dictionary covers common fields, but network specifications differ; verify field layouts before using decoded results operationally.

## API

The machine-readable contract is in [docs/openapi.yaml](docs/openapi.yaml).

`POST /api/v1/messages/parse` accepts:

```json
{
  "message": "0800822000000000000004000000000000001125210025123456301",
  "format": "ASCII_HEX_BITMAP",
  "headerBytes": 0
}
```

`POST /api/v1/messages/compare` accepts `{"left": {…parse request…}, "right": {…parse request…}}`. The parse response has `mti`, `bitmap`, `presentFields`, `fields`, `consumedBytes`, and `totalBytes`. Each field has a name, masked value, length, byte offset, and encoded byte length. Compare adds `mtiStatus` and a list of differences; sensitive fields have `HIDDEN` status. Invalid input returns HTTP 400 with `error` and, for parse failures, `offset`. The per-instance limit is 60 API requests per minute per remote IP; excess requests return HTTP 429 and `Retry-After`.

`ASCII_HEX_BITMAP` expects four ASCII MTI digits, then 16, 32 or 48 ASCII hex bitmap characters and field bytes. `HEX_BINARY_BITMAP` expects hex byte pairs for the whole frame: ASCII MTI bytes, binary bitmap bytes and field bytes. Field encoding comes from its definition; ICC data (DE 55), PIN data and MACs default to binary bytes. Hex input permits whitespace, colons and hyphens. `headerBytes` skips an optional header before the MTI (0–64). Message length is limited to 16,384 characters.

For a processor-specific layout, pass `fieldOverrides` using field numbers as keys:

```json
{
  "message": "…",
  "format": "HEX_BINARY_BITMAP",
  "fieldOverrides": {
    "2": {"name": "PAN", "length": 0, "maxLength": 19, "type": "LLVAR", "encoding": "BCD", "lengthEncoding": "BCD"},
    "39": {"name": "Response code", "length": 3, "maxLength": 3, "type": "FIXED"}
  }
}
```

Types are `FIXED`, `LVAR`, `LLVAR`, `LLLVAR`. Value encodings are `ASCII`, `BCD`, `HEX`; length encodings are `ASCII`, `BCD`. Packed BCD fields use right padding for odd digit counts. Secondary and tertiary bitmap indicators are read automatically. Tertiary data elements (DE 130–192) require field overrides. Unsupported fields fail with an explicit error. Proprietary nested fields remain unsupported.

## Structure

- `api`: versioned HTTP endpoints, request validation and error responses
- `application`: parse and compare workflows
- `core`: stateless ISO frame decoder
- `config`: Spring Security, CORS and rate limiting
- `field`: original reusable field library
- `resources/static`: bundled browser workbench
