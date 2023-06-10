# Welcome to MyZappi!

## Build
Build the project with the following command
```
mvn clean package
```

# Configuration needed before running the application
This project does not have IaC yet. The following section describes what is required to build the application and
all its dependencies.
## Create KMS Key
```
aws kms create-key --origin AWS_KMS --tags TagKey=Alias,TagValue=myZappiApiKey --region eu-west-1
aws kms create-alias --alias-name alias/myZappiApiKey --target-key-id {KeyId} --region eu-west-1
```

## Create DB tables
```
aws dynamodb create-table \
  --table-name zappi-login-creds \
  --attribute-definitions AttributeName=amazon-user-id,AttributeType=S \
  --key-schema AttributeName=amazon-user-id,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1

aws dynamodb create-table \
  --table-name zappi-otp \
  --attribute-definitions AttributeName=otp,AttributeType=S \
  --key-schema AttributeName=otp,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1

```

## Manually create role for Lambda

### Create role for myzappi Lambda
Create role `MyZappiLambdaPermissions` with the following:
* AWSLambdaBasicExecutionRole
Create inline policies for
* `dynamodb:PutItem`, `dynamodb:DeleteItem`, `dynamodb:GetItem` for `zappi-login-creds` DynamoDB table
* `dynamodb:PutItem`, `dynamodb:DeleteItem`, `dynamodb:GetItem`, `dynamodb:UpdateItem` for `zappi-otp` DynamodDB table
* `kms:Decrypt` for `myZappiApiKey` created above

### Create role for myzappi-login-lambda
Create role with `MyZappiLoginLambdaPermissions` with the following:
* `AWSLambdaBasicExecutionRole`
Create inline policies for
* `kms:Encrypt` for `myZappiApiKey` created above
* `dynamodb:PutItem`, `dynamodb:DeleteItem`, `dynamodb:GetItem` for `zappi-login-creds` and `zappi-otp`  DynamodDB tables

## Create Lambdas
### Create Lambda for MyZappi Alexa skill
```
aws lambda create-function --function-name myzappi --runtime java11 --handler com.amcglynn.myenergi.aws.MySkillStreamHandler::handleRequest --role {arnFromRoleCreatedAbove}  --code S3Bucket={myzappiBuilds},S3Key={pathToJarFile} --memory-size 512 --timeout 15
```
### Create Lambda for MyZappi Login page
```
aws lambda create-function --function-name myzappi-login --runtime java11 --handler com.amcglynn.myzappi.login.CompleteLoginHandler::handleRequest --role {arnFromRoleCreatedAbove}  --code S3Bucket={myzappiBuilds},S3Key={pathToJarFile} --memory-size 512 --timeout 30
```

# Configure environment variables
* Create `kmsKeyArn` environment variable and `awsRegion` for both lambdas

## Manually configure API gateway for myzappi-login
* Click Add Trigger, API Gateway
* Create new REST API with Open security
* Create POST method for the API and deploy it

* Create `loginUrl` environment variable with the API endpoint for myzappi-login lambda

## Add Alexa trigger to lambda manually
* Copy skillID from Alexa Skill and copy into new Alexa Trigger in Lambda
* Add Lambda Function ARN to endpoints in Alexa skill configuration
