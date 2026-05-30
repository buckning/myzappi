# automation-processor

AWS Lambda module that evaluates MyZappi Automations. It is invoked by a manually configured EventBridge schedule, expected every five minutes.

Handler: `com.amcglynn.automation.AutomationProcessorHandler`

Required DynamoDB tables:

- `automation` with partition key `user-id` (String)
- `automation-state` with partition key `user-id` (String)
- `automation-processor-lock` with partition key `lock-id` (String)

Required permissions:

- DynamoDB read/write/delete/scan for the three automation tables
- Existing credential, device, KMS, and myenergi access used by `core`
- SQS send for reconciliation queue
- Lambda invoke permission for self-continuation

The EventBridge schedule is not managed by Gradle. Configure it manually with a five minute rate expression.
