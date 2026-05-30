# AGENTS.md

## Project Overview

MyZappi is a non-profit, cost-conscious serverless application for controlling
myenergi Zappi, Libbi and Eddi devices through Alexa and a web app. Users can check device
status, change charge mode, enable or stop boost, view energy summaries, manage
schedules, and receive reminders.

The project is deployed mainly as AWS Lambda functions plus an Angular web app.
Cold starts matter. Keep startup paths small, avoid unnecessary dependencies in
Lambda modules, and do not add eager initialization unless there is a clear
reason.

MyZappi stores user, account, device, schedule, and reconciliation data in
DynamoDB. myenergi credentials and API keys are sensitive and must remain
encrypted with AWS KMS where persisted. The service stores data in AWS Ireland
according to the project documentation.

## Architecture

The backend is a Java 21 Gradle multi-module project. The frontend is an Angular
application in `site/myzappi`.

- `api`: REST APIs used by the Angular application. These APIs handle account
  registration, device discovery, device commands, energy summary, status, and
  schedule operations.
- `myzappi-alexa-lambda`: Alexa skill Lambda for voice control, reminders, and
  account-linked user flows.
- `sqs-handler`: EventBridge Scheduler target for scheduled MyZappi actions and
  Alexa reminder callbacks. The name is historical; do not assume it only
  handles SQS.
- `state-reconciler`: Lambda that verifies delayed device state changes and
  retries commands when myenergi accepts a command but the physical device has
  not yet applied the desired state.
- `core`: Shared application code used across Lambda modules, including domain,
  persistence, scheduling, and cross-cutting logic.
- `myenergi-client`: Client for myenergi APIs.
- `login-with-amazon`: Login with Amazon and related Amazon integration code.
- `alexa`: Alexa Skill Kit CLI support and skill-management notes.
- `site/myzappi`: Angular 17 web application used by customers to register and
  manage their MyZappi setup.

Runtime integrations include AWS Lambda, API Gateway, DynamoDB, KMS, EventBridge
Scheduler, SQS-style delayed reconciliation, Login with Amazon, Alexa Skill
Messaging, and upstream myenergi APIs.

## Important Runtime Flows

The web app calls the `api` Lambda through REST APIs. Alexa requests are handled
by `myzappi-alexa-lambda`. Both rely on shared code in `core` and call myenergi
through `myenergi-client`.

Scheduled actions are created through EventBridge Scheduler and handled by
`sqs-handler`. Alexa reminders use a callback flow where a scheduled invocation
posts a skill message shortly before the reminder time, allowing the Alexa skill
to check the latest device state and decide what to do next.

Commands invoked through `myenergi-client` may return success before the
physical myenergi device has actually applied the requested state. Code must not
treat a successful myenergi API response as proof that device state changed
immediately. For state-sensitive commands, prefer the existing reconciliation
pattern: persist desired state, wait before checking actual state, compare actual
state with desired state, retry up to the existing attempt limit, and stop once
the device matches the desired state.

Keep scheduled and retried work idempotent. Duplicate EventBridge or delayed
retry invocations should not create unsafe repeated actions.

## Development Rules

Before changing behavior, read the relevant README files and existing code
patterns. Feature work must have acceptance criteria and tests. Use the
project's spec/design location for feature design; if no suitable spec location
exists for the work, create one deliberately rather than burying design decisions
inside implementation.

Keep changes simple, testable, and aligned with existing module boundaries.
Prefer existing dependencies, frameworks, and local helper APIs. Do not introduce
new technologies or libraries without a strong reason.

Apply SOLID principles pragmatically:

- Put behavior in focused units with clear responsibilities.
- Depend on interfaces or small collaborators where that makes code easier to
  test.
- Avoid coupling Lambda handlers directly to business logic when a controller,
  service, or domain object boundary already exists.
- Keep AWS, myenergi, and Alexa integration details at the edges where possible.

Write comments only when they explain why a decision was made or why a structure
exists. Do not add comments that restate obvious code.

## Data, Security, and Infrastructure Cautions

Treat myenergi API keys, myenergi account credentials, Amazon user IDs, tokens,
and encrypted credential records as sensitive. Do not log secrets or add test
fixtures containing real credentials.

The root README notes that this project does not yet have full infrastructure as
code. Several AWS resources, Lambda roles, API Gateway settings, tables, KMS
keys, and deployment steps are documented as manual setup. Do not change
deployment assumptions or run deployment scripts unless the user explicitly asks
for it.

DynamoDB tables documented by the project include credential storage, sessions,
tariffs, Alexa-to-LWA user lookup, schedules, schedule details, devices, and
device state reconciliation requests. Preserve existing key shapes and TTL
semantics unless a design explicitly changes them.

## Build and Test Commands

Backend:

```sh
./gradlew clean build
./gradlew :api:test
./gradlew :core:test
./gradlew :login-with-amazon:test
./gradlew :myenergi-client:test
./gradlew :myzappi-alexa-lambda:test
./gradlew :sqs-handler:test
./gradlew :state-reconciler:test
```

Frontend:

```sh
cd site/myzappi
ng build
ng test
ng serve
```

`ng serve` runs the Angular dev server at `http://localhost:4200/`.
Do not run `npm install` as a routine build step. Only install or update
frontend dependencies when dependency changes are part of the task.

The root README currently notes that local website login requires changing the
Amazon authorize URL to `http://localhost:4200` in
`site/myzappi/src/app/logged-out-content/logged-out-content.component.ts`.
Preserve or improve this intentionally; do not make incidental auth callback
changes.

Deployment scripts exist, but run them only when explicitly requested:

```sh
./buildAndDeployApi.sh
./buildAndDeploySkill.sh
./buildAndDeploySqs.sh
```

Website deployment is documented as manual S3 upload to
`www.myzappiunofficial.com` followed by a CloudFront invalidation for `/*`.

## Verification Expectations

Choose the narrowest meaningful verification for the files touched, then broaden
when changes cross module boundaries. For backend changes, prefer module-scoped
Gradle tests first and run `./gradlew clean build` when shared behavior,
packaging, or multiple modules are affected. For frontend changes, run the
Angular build and relevant tests from `site/myzappi`.

When working on schedules, reminders, retries, or reconciliation, include tests
for idempotency, retry limits, and the distinction between accepted commands and
observed device state.

If a test or build cannot be run, explain why and state the remaining risk.

## Documentation Sources

Use these files as primary local context:

- `README.md`
- `Instructions.md`
- `api/README.md`
- `alexa/README.md`
- `sqs-handler/README.md`
- `state-reconciler/README.md`
- `site/myzappi/README.md`
- `docs/Architecture.png`
