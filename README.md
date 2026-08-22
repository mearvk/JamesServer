# JamesServer

James Server 48.0 — based on Apache James 3.9.0

## Overview

This repository contains a selective checkout of the Apache James Mail Enterprise Server (v3.9.0), a Java-based email server supporting SMTP, IMAP, POP3, JMAP, and WebAdmin protocols.

## Project Structure

```
src/james-project/
├── core/              Core domain objects (email addresses, domains, etc.)
├── json/              JSON serialization utilities
├── mailet/            Mail processing pipeline (mailets & matchers)
│   ├── api/           Mailet API interfaces
│   ├── base/          Base mailet implementations
│   ├── standard/      Standard mailets (redirect, bounce, etc.)
│   ├── crypto/        S/MIME and OpenPGP support
│   ├── ai/            AI-assisted mail processing
│   ├── icalendar/     Calendar invitation handling
│   └── amqp/          AMQP message forwarding
├── mdn/               Message Disposition Notification (RFC 8098)
├── mpt/               Mail Protocol Tests (IMAP, SMTP, ManageSieve)
├── server/
│   ├── apps/          Runnable server applications
│   │   ├── memory-app/        In-memory backend (testing/dev)
│   │   ├── postgres-app/      PostgreSQL backend
│   │   ├── cassandra-app/     Cassandra backend
│   │   ├── distributed-app/   Distributed (Cassandra + RabbitMQ + S3)
│   │   ├── jpa-app/           JPA backend
│   │   ├── spring-app/        Legacy Spring-based server
│   │   └── ...
│   ├── blob/          Blob storage (S3, Cassandra, PostgreSQL, file, memory)
│   └── data/          Data access (JPA, LDAP, file, memory, JMAP)
└── docs/              Antora-based documentation
```

## Build Status

**Compilation: PASS** — All present modules compile cleanly.

| Module | Status |
|--------|--------|
| core | ✅ Compiles |
| json | ✅ Compiles |
| mdn | ✅ Compiles |
| mailet (10 sub-modules) | ✅ Compiles |
| mpt (15+ sub-modules) | ✅ Compiles |
| server/blob (12 sub-modules) | ✅ Compiles |
| server/data (6 sub-modules) | ✅ Compiles |
| server/apps (13 apps) | ✅ Compiles |

## Completeness

This is a **partial checkout** of the full Apache James 3.9.0 source — specifically, the domain, processing, storage, and data layers without the runtime server skeleton.

**What's fully present:**
- 100% of the mailet/mail-processing layer (API + all implementations)
- 100% of the blob storage layer (API + S3, Cassandra, Postgres, file, memory, AES)
- 100% of the data/persistence API + 4 of 7 backends (JPA, file, LDAP, memory)
- 100% of the protocol test scripts (IMAP, SMTP, ManageSieve)
- 100% of the core domain objects (MailAddress, Domain, Username, Quota)
- 100% of MDN (RFC 8098)

**What's missing (0% — the runtime server infrastructure):**

- `backends-common` — shared backend utilities
- `event-bus` — event distribution system
- `event-sourcing` — event sourcing framework
- `javax-mail-extension` — JavaMail extensions
- `mailbox` — mailbox storage layer
- `metrics` — metrics collection
- `protocols` — protocol implementations (IMAP, SMTP, POP3, LMTP, ManageSieve)
- `testing/base` — test utilities
- `third-party` — third-party integrations
- `server/container` — Guice/Spring DI containers
- `server/dns-service` — DNS resolution
- `server/mailet` — server-side mailet wiring
- `server/mailrepository` — mail repository storage
- `server/protocols` — protocol server bindings
- `server/queue` — mail queue (memory, RabbitMQ, Pulsar, ActiveMQ)
- `server/task` — task management

The present modules compile against dependencies from Maven Central. The app modules (memory-app, cassandra-app, etc.) will compile but cannot run standalone without the missing server infrastructure modules installed to the local Maven repository.

## Source Stats

- **1,314** Java source files
- **75** Maven POM files
- **8** shell scripts (provisioning, startup, environment)

## Build Instructions

### Prerequisites

- Java 11+ (tested on OpenJDK 25)
- Maven 3.6+ (tested on 3.8.7)

### Quick Build

```bash
cd src/james-project
mvn compile -DskipTests -Dcheckstyle.skip=true -Dscalafix.skip=true
```

### Install to Local Repository

```bash
cd src/james-project
mvn install -DskipTests -Dcheckstyle.skip=true -Dscalafix.skip=true \
    -Djib.skip=true -Dassembly.skipAssembly=true
```

### Notes

- `-Dcheckstyle.skip=true` — skips checkstyle (config present but project-level suppressions are minimal)
- `-Dscalafix.skip=true` — skips Scala linting
- `-Djib.skip=true` — skips Docker image creation
- `-Dassembly.skipAssembly=true` — skips zip/tar assembly packaging

## Shell Scripts

| Script | Purpose |
|--------|---------|
| `server/apps/demo/startup.sh` | Starts JPA demo server with TLS cert generation |
| `server/apps/demo/initialdata.sh` | Seeds 3 test users via james-cli |
| `server/apps/postgres-app/provision.sh` | Provisions 1000 users via WebAdmin API |
| `server/apps/spring-app/*/setenv.sh` | Classpath/environment configuration |

## License

Apache License 2.0
