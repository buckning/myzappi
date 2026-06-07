# Automations Design

## Purpose

MyZappi needs an Automations feature: user-configured conditional rules that
periodically evaluate live myenergi state and execute supported MyZappi actions
when a predicate becomes true. This is similar to IFTTT, but limited to
validated predicates and actions that the project explicitly supports.

The first version serves a small opt-in subset of roughly 200 users. Cost and
Lambda timeout safety matter more than high-scale throughput. The processor
will run from a manually configured EventBridge schedule, expected every five
minutes.

## Goals

- Add web-managed automations that evaluate live myenergi conditions.
- Keep user-facing functionality under the name "Automations".
- Add a new `automation-processor` Lambda/module for periodic processing.
- Store definitions and runtime state separately to avoid API/processor write
  races on JSON blobs.
- Keep DynamoDB row count low by storing per-user JSON blobs.
- Fetch current myenergi state once per user per processor run.
- Execute actions when predicates are currently true and the selected target is
  not already in the desired state, where current target state can be read.
- Support priority-based conflict resolution.
- Reuse existing action strings and state reconciliation where possible.

## Non-Goals

- Alexa will not create, edit, or manage automations.
- V1 will not expose automation runtime state in the public API or UI.
- V1 will not support arbitrary user-defined code.
- V1 will not support boost or unlock actions.
- V1 will not support editing predicate/action definitions after creation.
- V1 will not include infrastructure as code for the EventBridge trigger.
- V1 will not implement composite `AND`/`OR` predicates, but the model should
  leave room for them later.

## Architecture

Add a new backend module and Lambda named `automation-processor`.

Responsibilities:

- `api`
  - Owns automation definition management.
  - Provides create, list, delete, active toggle, priority reorder, and options
    endpoints.

- `site/myzappi`
  - Adds a separate Automations panel.
  - Provides create modal, list, active toggle, delete, and drag/drop priority
    ordering.

- `core`
  - Holds shared automation models, repositories, validators, predicate
    evaluators, action execution, state services, and processor lock support.

- `automation-processor`
  - Runs from a manually configured EventBridge schedule every five minutes.
  - Scans opt-in automation definitions in batches.
  - Evaluates active automations, executes selected actions that need target
    state changes, writes state, and handles continuation.

- Existing `StateReconcilerService`
  - Used only for automation actions with an existing registered reconciler.

This keeps condition-based automations separate from existing time-based
schedules and avoids changing `sqs-handler` behavior.

## DynamoDB Model

Use low-row-count per-user JSON blobs.

### `automation`

- Partition key: `user-id`
- Attribute: `automations`
- Contains automation definitions only.
- A row exists only when the user has at least one automation.
- Creating the first automation creates the row.
- Deleting the final automation deletes the row.

### `automation-state`

- Partition key: `user-id`
- Attribute: `states`
- JSON object keyed by `automationId`.
- Owned by the processor for normal state updates.
- The API may delete the whole user row only when deleting the user's final
  automation.
- The API must not edit individual state entries.

### `automation-processor-lock`

- Partition key: fixed processor lock id.
- Attributes: `runId`, `expiresAt`.
- Used as a lightweight global guard against overlapping processor runs.
- Expiry/TTL allows recovery when Lambda execution fails before releasing the
  lock.

## Automation Definition

Each automation has:

- `automationId`
- optional `name`
- `active`
- `priority`
- `predicate`
- `action`
- `createdAt`

Rules:

- `name` is optional and retained in the backend model for compatibility with
  manually-created or future UI-managed rules.
- If present, `name` is trimmed and limited to 80 characters.
- The V1 Angular UI does not create, edit, or display automation names. It
  always shows generated predicate/action summaries instead.
- `active` defaults to `true`.
- Priorities are unique per user and normalized to `1..N`.
- Priority `1` is the highest priority.
- Creating a new automation appends it at priority `N + 1`.
- Reordering rewrites priorities to match the full ordered list.
- Predicate/action definitions are immutable after creation.
- Only `active` and `priority` are mutable.
- Max 10 automations per user.

## Automation State

Each state entry may contain:

- `lastPredicateMatched`
- `lastEvaluatedAt`
- `lastTriggeredAt`
- `lastError`
- `lastFailedAt`
- `lastSkippedReason`

State behavior:

- Missing state is treated as "not previously matched" for action types whose
  current target state cannot be read.
- A newly created or re-enabled automation can execute on its first processor
  evaluation if the predicate is already true.
- For action types whose current target state can be read, the processor uses
  level-triggered desired-state behavior: if the predicate is true and the
  current target state differs from the action value, execute the action even
  when the predicate was also true in the previous run.
- If the predicate is true but the current target state already matches the
  action value, do not execute the action.
- For action types whose current target state cannot be read, the processor
  retains edge-trigger fallback behavior to avoid command spam.
- Disabling an automation does not write state from the API. The processor
  removes the disabled automation's state entry on its next run.
- Deleting an automation removes only the definition unless it is the final
  automation for that user.
- While a user still has automation definitions, the processor removes orphaned
  state entries for missing definition ids.
- If the user deletes the final automation, the API deletes the user's
  `automation` row and whole `automation-state` row.
- Priority changes preserve state.

## Predicates

V1 supports exactly one predicate per automation. The model should use a
type-based predicate abstraction so composite predicates can be added later.

Predicate values are strings. Each predicate type owns parsing and validation.

Operators:

- `GREATER_THAN`
- `LESS_THAN`

Account-level predicates without a target:

- `ENERGY_SOLAR_GENERATION_KW`
- `ENERGY_EXPORTING_KW`
- `ENERGY_IMPORTING_KW`
- `ENERGY_CONSUMING_KW`

Device-specific predicates:

- `ZAPPI_EV_CHARGE_RATE_KW`
- `LIBBI_STATE_OF_CHARGE_PERCENT`

Validation:

- Account-level predicates must not require a target.
- Zappi predicates require a target Zappi serial number.
- Libbi predicates require a target Libbi serial number.
- Targeted predicates must reference a device currently owned by the user.
- Numeric predicate values must parse as decimal values.

Examples:

```json
{
  "type": "ENERGY_EXPORTING_KW",
  "operator": "GREATER_THAN",
  "value": "2.0"
}
```

```json
{
  "type": "ZAPPI_EV_CHARGE_RATE_KW",
  "target": "12345678",
  "operator": "LESS_THAN",
  "value": "0.1"
}
```

## Actions

Automation actions reuse existing schedule action type strings.

V1 allowed actions:

- `setChargeMode`
- `setZappiMgl`
- `setEddiMode`
- `setLibbiEnabled`
- `setLibbiChargeFromGrid`
- `setLibbiChargeTarget`

Excluded in V1:

- boost actions
- unlock actions

Validation:

- Action target must be provided.
- Action target must belong to the user.
- Action target device class must match the action type.
- Action value must pass action-specific validation.

Predicates and actions can target different devices owned by the same user.

## Processor Flow

The manually configured EventBridge schedule invokes `automation-processor`
approximately every five minutes.

High-level flow:

1. Acquire the global processor lock.
2. Exit if another non-expired run owns the lock.
3. Scan the `automation` table in bounded batches.
4. For each user item:
   - read definitions
   - read current state
   - remove disabled or orphaned state entries
   - build `MyEnergiService` once
   - fetch one current myenergi snapshot
   - evaluate all active automations against the snapshot
   - compare predicate results with target action state where available
   - group currently matching automations by conflict key
   - sort matching automations by priority
   - select the highest-priority matching automation per conflict key
   - execute the selected action when the target state is not already satisfied
   - for action types without readable target state, execute only when the
     predicate transitions from false or missing to true
   - skip lower-priority conflicting actions without fallback
   - record per-automation failures and continue
   - write updated state once for the user
5. If the scan has more users or remaining Lambda time is below a safety
   threshold, invoke a continuation with the cursor and same `runId`.
6. Release the lock on normal completion.

The processor must remain idempotent. Locking reduces overlap but does not
replace desired-state checks, transition fallback for unreadable action state,
and deterministic conflict handling.

## Snapshot Strategy

For each user, the processor should call myenergi once and derive all predicate
fields from that snapshot where possible.

The snapshot should include:

- account energy values:
  - solar generation
  - exporting
  - importing
  - consuming
- Zappi status values needed by predicates:
  - EV charge rate
- Zappi status values needed by action desired-state checks:
  - charge mode
  - minimum green level
- Eddi status values needed by action desired-state checks:
  - mode derived from Eddi state
- Libbi status values needed by predicates:
  - state of charge
- Libbi status values needed by action desired-state checks:
  - enabled mode where available from status

If one snapshot cannot provide every required field, the design should still
centralize data fetching per user and avoid per-rule calls.

## Conflict Resolution

Automations can command the same action target/type with different values.
Priority determines precedence.

Conflict key:

- action type
- target device

When multiple automations match in one poll with the same conflict key:

1. Sort by priority.
2. Select only the highest-priority matching automation.
3. If the selected automation's target state already satisfies the action, do
   nothing and do not fall back to lower-priority automations.
4. If the selected automation needs execution and fails, do not fall back to
   lower-priority automations.
5. Record `lastSkippedReason` for lower-priority conflicting automations.
6. Record the actual predicate match state for skipped automations.

This allows a lower-priority overlapping automation to run later if the
higher-priority predicate stops matching and the lower-priority predicate is
still true.

## State Reconciliation

After a supported automation action executes, the processor should submit a
state reconciliation request only when the action type has an existing
reconciler in the current `ReconcilerRegistry`.

Expected V1 reconciler-supported actions include existing supported commands
such as:

- `setChargeMode`
- `setZappiMgl`
- `setEddiMode`

Actions without registered reconcilers execute without reconciliation.

If command execution succeeds but reconciliation enqueueing fails, record/log
the reconciliation failure separately. Do not mark the command execution itself
as failed solely because reconciliation could not be queued.

## API

### `GET /automations/options`

Returns backend-supported metadata for the Angular UI:

- predicate types
- operators
- value types
- target requirements
- action types
- action value constraints
- action device class constraints

The backend is the source of truth for valid predicates and actions.
Display labels and display units are applied by the Angular UI from known API
strings. API payloads continue to use the canonical backend values.

### `GET /automations`

Returns automation definitions only. Runtime state is not exposed in V1.

### `POST /automations`

Creates an automation.

Behavior:

- validates request
- enforces max 10 automations
- generates `automationId`
- appends priority at `N + 1`
- writes the per-user definition blob

### `PATCH /automations/{automationId}`

Updates mutable definition fields.

V1 supports:

- `active`

Disabling does not edit state directly. The processor cleans state on its next
run.

### `PUT /automations/priorities`

Accepts the full ordered list of automation ids for the user.

Behavior:

- rejects duplicate ids
- rejects missing/unknown ids
- rewrites priorities to `1..N`
- preserves state

### `DELETE /automations/{automationId}`

Deletes the automation definition.

Behavior:

- removes the definition
- compacts priorities
- if other automations remain, leaves state cleanup to the processor
- if no automations remain, deletes the user's `automation` row and whole
  `automation-state` row

## Angular UI

Add a separate Automations panel to the logged-in dashboard. It should not be
part of the schedules panel.

V1 UI capabilities:

- list automations
- show generated predicate/action summaries
- create automation in a modal/dialog
- enable/disable automation
- delete automation
- drag/drop reorder where the top item is priority `1`

The create modal should use `GET /automations/options` for supported predicate
and action metadata.

V1 UI behavior:

- Do not show automation names in the modal or list.
- Do not show explicit priority text in list rows. Priority is implied by the
  order of the rows.
- Map API values to user-facing labels in the modal and list, for example:
  - `ECO_PLUS` displays as `Eco+`
  - `GREATER_THAN` displays as `Greater than`
  - `setChargeMode` displays as `Set charge mode`
- Display units next to predicate/action values where units are known:
  - `_KW` predicate types display values in `kW`
  - `_PERCENT` predicate types display values in `%`
  - `setZappiMgl` and `setLibbiChargeTarget` display action values in `%`
  - Unitless actions such as `setChargeMode` display no unit
- Filter predicate and action options to device classes the user currently owns.
- For targeted predicates/actions with exactly one matching user device, show
  the target device as read-only text instead of a dropdown.
- For targeted predicates/actions with multiple matching devices, show a device
  dropdown.
- Numeric predicate values use numeric inputs and submit string values to the
  API.
- Non-numeric predicate values use text inputs.
- Invalid form values show inline validation messages.
- Save is disabled while required fields are missing or invalid.
- Submitting an invalid form must not close the modal.
- The modal uses the footer Cancel button as the only cancel control.
- If the API rejects automation creation, the panel shows a visible error
  message instead of failing silently.
- After deleting an automation, the visible list updates immediately after the
  delete call succeeds, including the final-row case.

V1 UI does not show runtime processor state, rule execution errors, skipped
reasons, or timestamps.

## Manual Infrastructure

Infrastructure is manually configured, consistent with the existing project.

Manual setup required:

- create `automation` DynamoDB table
- create `automation-state` DynamoDB table
- create `automation-processor-lock` DynamoDB table
- create/deploy `automation-processor` Lambda
- configure EventBridge to invoke `automation-processor` every five minutes
- configure Lambda permissions for DynamoDB, myenergi credential reads, KMS
  decrypt, SQS reconciliation enqueue, and self-invocation for continuations

Deployment automation can be modeled after existing Lambda build/deploy scripts,
but running deployment remains explicit/manual.

## Error Handling

- Malformed automation definitions should be logged and skipped without stopping
  the user batch.
- Missing or invalid targets at processing time should skip that automation and
  record state error context.
- A failed myenergi snapshot for one user should skip that user and continue
  with other users.
- A failed action should record failure for that automation and continue
  processing non-conflicting automations.
- A failed highest-priority action should not fall back to lower-priority
  conflicting automations.
- Lock acquisition failure should exit cleanly.
- Continuation failure should be recoverable through lock expiry and the next
  scheduled run.

## Acceptance Criteria

- Users can create up to 10 automations through the Angular UI.
- Users can list, delete, enable/disable, and reorder automations.
- Automation names are optional and limited to 80 characters when present, but
  the V1 Angular UI does not expose names.
- The Angular UI lists generated predicate/action summaries with friendly labels
  and known units.
- The Angular UI does not show visible priority text in automation rows.
- The Angular UI filters predicate/action choices by the user's owned device
  classes.
- The Angular UI uses read-only target display when only one matching target
  device exists.
- The Angular UI prevents saving invalid automation forms and displays inline
  validation errors.
- The Angular UI displays a visible error when automation creation is rejected
  by the API.
- Priorities are unique per user and normalized to `1..N`.
- The backend exposes automation options metadata for the UI.
- The API validates allowed predicates, actions, operators, values, device
  ownership, and device classes.
- The processor scans only users with automation rows.
- The processor acquires a global lock and exits on active lock contention.
- The processor processes users in bounded batches and supports continuation.
- The processor fetches current myenergi state once per user where possible.
- Automations with readable target state execute when their predicate is true
  and their selected action target is not already in the desired state.
- Automations without readable target state retain transition fallback and
  execute only when their predicate transitions from false or missing state to
  true.
- Create and re-enable can execute on the next processor run if the predicate is
  already true.
- Disabled automation state is cleaned by the processor.
- Orphan state entries are cleaned by the processor while user definitions
  remain.
- Deleting the final automation deletes the user's definition row and whole
  state row.
- Conflicting matching actions execute according to priority with no fallback.
- Supported automation actions enqueue existing state reconciliation requests.
- Failed rules do not stop other rules or users from being processed.
- V1 does not expose runtime automation state in the public API or UI.

## Test Plan

### Core

- Automation repository read/write/delete for per-user blobs.
- Automation state repository read/write/delete for per-user blobs.
- Processor lock acquire, reject, refresh, and release behavior.
- Automation validation for predicate/action types, operators, values, ownership,
  target requirements, max count, and name length.
- Predicate evaluation for all V1 predicate fields.
- Level-triggered desired-state behavior for readable action target state.
- Transition fallback behavior for missing, false, and true prior state when
  action target state is not readable.
- Active/disabled state cleanup.
- Orphan state cleanup.
- Priority normalization and reorder behavior.
- Conflict resolution with no fallback on failure.
- Action execution for all V1 allowed actions.
- Conditional reconciliation enqueueing only for registered reconcilers.

### API

- Route registration for all automation endpoints.
- `GET /automations/options` response shape.
- Create success and validation failures.
- Max 10 automations enforcement.
- List definitions without state.
- Active toggle only.
- Reorder rejects duplicate, missing, or unknown ids.
- Delete compacts priorities.
- Delete final automation removes definition row and whole state row.

### Automation Processor

- Empty table scan exits cleanly.
- Batch limit and continuation cursor behavior.
- Timeout-aware continuation before Lambda time is exhausted.
- Per-user snapshot is reused across multiple rules.
- User-level snapshot failure does not stop other users.
- Rule-level action failure records failure and continues.
- Lock contention exits cleanly.

### Angular

- Automations panel renders in logged-in dashboard.
- Create modal renders predicate/action controls from options metadata.
- Create modal filters options by owned device class.
- Create modal hides target dropdowns when there is only one matching device.
- Create modal uses numeric inputs for numeric predicate values.
- Create modal disables Save and shows validation messages for invalid values.
- Create modal keeps API payload values unchanged while displaying friendly
  labels.
- Create modal shows known value units in field labels.
- Automations list renders saved rules with friendly labels and known value
  units.
- Automations list does not show names or priority labels.
- Create failure renders a visible panel error.
- Deleting the final automation removes the row from the visible list.
- Create, delete, active toggle, and reorder call the expected endpoints.
- Drag/drop reorder produces normalized ordered ids.
- Build with `ng build`.

## Open Extension Points

- Composite predicates with `AND`/`OR`.
- Runtime state display in API/UI.
- Additional predicate value types such as booleans and enums.
- Re-enabling automation names in the Angular UI.
- Additional action types after safety review.
- Infrastructure as code for tables, Lambda, and EventBridge.
