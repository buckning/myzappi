# MyZappi infrastructure

This directory contains the AWS SAM/CloudFormation resources for the isolated MyZappi staging account. The foundation template creates the DynamoDB tables and KMS key required by the application. The compute template creates the Lambda functions, IAM roles, log groups, and automation timer. Production resources are not managed by these templates.

## Target account

Deploy this stack only with credentials for the new staging AWS account in `eu-west-1`. Before any deployment, verify the account identity:

```bash
aws sts get-caller-identity --region eu-west-1
```

The returned account ID must be the staging account. Do not proceed if it is the production account.

## Validate and deploy

The AWS SAM CLI is required for these commands:

```bash
sam validate --template-file infra/template.yaml
sam build --template-file infra/template.yaml
sam deploy \
  --template-file .aws-sam/build/template.yaml \
  --stack-name myzappi-staging-foundation \
  --region eu-west-1 \
  --parameter-overrides EnvironmentName=staging \
  --resolve-s3
```

The stack creates or manages:

- `session`, using `session-id` as its String partition key.
- `zappi-login-creds`, using `amazon-user-id` as its String partition key.
- `myenergi-creds`, using `amazon-user-id` as its String partition key.
- `tariff`, using `user-id` as its String partition key.
- `alexa-to-lwa-users-lookup`, using `alexa-user-id` as its String partition key.
- `device-state-reconcile-requests`, using `device-state-change-key` as its String partition key.
- `automation`, using `user-id` as its String partition key.
- `automation-state`, using `user-id` as its String partition key.
- `automation-processor-lock`, using `lock-id` as its String partition key.
- `schedule`, using `user-id` as its String partition key.
- `schedule-details`, using `schedule-id` as its String partition key.
- `devices`, using `user-id` as its String partition key.
- TTL enabled on `session.ttl`, `device-state-reconcile-requests.ttl`, and `automation-processor-lock.expiresAt`.
- On-demand DynamoDB billing for the low-traffic learning environment.
- Server-side encryption for each DynamoDB table.
- A customer-managed KMS key with automatic rotation.
- The alias `alias/myZappiApiKey`.
- Retention policies and DynamoDB deletion protection for stateful resources.

## Verify the deployment

```bash
aws cloudformation describe-stacks \
  --stack-name myzappi-staging-foundation \
  --region eu-west-1

aws dynamodb describe-table \
  --table-name session \
  --region eu-west-1

aws dynamodb describe-time-to-live \
  --table-name session \
  --region eu-west-1
```

All application tables are persistent staging infrastructure. Their CloudFormation deletion and replacement policies are `Retain`, and DynamoDB deletion protection is enabled, so stack deletion or resource replacement does not delete table data. To intentionally remove a table, first disable deletion protection and then remove it through an explicit, reviewed cleanup operation.

The previous staging table named `myzappi-staging-session` is intentionally not removed by the rename to `session`; the retention policy leaves it available for later inspection or cleanup.

## Lambda compute stack

`compute.yaml` defines five Java 21 Lambda functions:

- `api`, using `com.amcglynn.myzappi.api.ApiRequestHandler::handleRequest`.
- `myzappi-alexa-lambda`, using `com.amcglynn.myzappi.MyZappiSkillStreamHandler`.
- `sqs-handler`, using `com.amcglynn.sqs.EventBridgeHandler::handleRequest`.
- `state-reconciler`, using `com.amcglynn.myzappi.reconciler.StateReconcilerHandler::handleRequest`.
- `automation-processor`, using `com.amcglynn.automation.AutomationProcessorHandler::handleRequest`.

The template expects the shaded JAR files to already exist in a private S3 artifact bucket. The default object names match the repository build output and deployment scripts. The artifact bucket is deliberately not created in this stack because Lambda creation requires the JAR objects to exist first.

The state reconciler event source mapping is disabled by default. Deploy the SQS queue first, then redeploy with `EnableStateReconcilerTrigger=true` and the queue URL. This prevents a partially configured queue consumer from starting unexpectedly.

The automation timer is created disabled. Enable it only after the Lambda environment variables, credentials, and downstream resources have been verified.

Example deployment after uploading all five JAR files and creating the reconciliation queue:

```bash
aws cloudformation deploy \
  --template-file infra/compute.yaml \
  --stack-name myzappi-staging-compute \
  --region eu-west-1 \
  --profile myzappi-staging \
  --parameter-overrides \
    FoundationStackName=myzappi-staging-foundation \
    ArtifactBucketName=myzappi-staging-builds \
    ReconciliationQueueUrl=<queue-url> \
    EnableStateReconcilerTrigger=false \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset
```

Do not put the Alexa client secret in this file or in source control. Pass it through a protected deployment mechanism for now; SSM Parameter Store SecureString is the planned low-cost improvement.
