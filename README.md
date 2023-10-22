# Welcome to MyZappi!

## Next Features
* Support multiple tanks with Eddi (API changes, Alexa command changes, front end schedule changes)
* Remind me when my car finishes charging
* Better metrics
* Notify if the charger is offline 
* If the car charge mode is changed or if boost was set, notify if the car is not plugged in

## Premium features
* sync history
* Query history between 2 times 
* Add PIN so that only those with a PIN can control the Zappi
* Add guest mode where the host authorizes guests can use the charger and can get information from it for their sessions.
** Guests are issued a guest key. The guests enter it into the website to unlock the charger.
** Guests enter key whenever they are charging
** Guest have a prorated session that the owner gets reported of 
** May be useful in AirBnBs 

## What is MyZappi?
MyZappi is an Alexa skill that can be used to control your myenergi Zappi device all through an Amazon Echo or Alexa device.
It offers functionality such as:
* Get the current status of the Zappi, including import, export, solar generation, charge add to E.V., etc.
* Set the Zappi charge mode to Eco, Eco+, Fast or Stop
* Enable boost for a specific duration or kilowatt amount
* Get energy usage for a specific day
* Check if your E.V. is plugged in

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
  --table-name session \
  --attribute-definitions AttributeName=session-id,AttributeType=S \
  --key-schema AttributeName=session-id,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1

aws dynamodb update-time-to-live \
    --table-name session \
    --time-to-live-specification "Enabled=true, AttributeName=ttl"

aws dynamodb create-table \
  --table-name tariff \
  --attribute-definitions AttributeName=user-id,AttributeType=S \
  --key-schema AttributeName=user-id,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1

aws dynamodb create-table \
  --table-name alexa-to-lwa-users-lookup \
  --attribute-definitions AttributeName=alexa-user-id,AttributeType=S \
  --key-schema AttributeName=alexa-user-id,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1
```

Contains all the schedule information for a user as a json blob
```
aws dynamodb create-table \
--table-name schedule \
--attribute-definitions AttributeName=user-id,AttributeType=S \
--key-schema AttributeName=user-id,KeyType=HASH \
--provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1
```

Stores the schedule information for a schedule ID
```
aws dynamodb create-table \
--table-name schedule-details \
--attribute-definitions AttributeName=schedule-id,AttributeType=S \
--key-schema AttributeName=schedule-id,KeyType=HASH \
--provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1
```

## Manually create role for Lambda

### Create role for myzappi Lambda
Create role `MyZappiLambdaPermissions` with the following:
* AWSLambdaBasicExecutionRole
Create inline policies for
* `dynamodb:PutItem`, `dynamodb:DeleteItem`, `dynamodb:GetItem` for `zappi-login-creds` DynamoDB table
* `kms:Decrypt` for `myZappiApiKey` created above

### Create role for myzappi-login-lambda
Create role with `MyZappiLoginLambdaPermissions` with the following:
* `AWSLambdaBasicExecutionRole`
Create inline policies for
* `kms:Encrypt` for `myZappiApiKey` created above

## Create Lambdas
### Create Lambda for MyZappi Alexa skill
```
aws lambda create-function --function-name myzappi --runtime java11 --handler com.amcglynn.myenergi.aws.MySkillStreamHandler::handleRequest --role {arnFromRoleCreatedAbove}  --code S3Bucket={myzappiBuilds},S3Key={pathToJarFile} --memory-size 512 --timeout 15
```
### Create Lambda for MyZappi Login page
```
aws lambda create-function --function-name myzappi-login --runtime java11 --handler com.amcglynn.myzappi.api.CompleteLoginHandler::handleRequest --role {arnFromRoleCreatedAbove}  --code S3Bucket={myzappiBuilds},S3Key={pathToJarFile} --memory-size 512 --timeout 30
```

# Configure environment variables
* Create `skillId` environment variable for `myzappi` lambda. The skill ID needs to be taken from the Alexa development console
* Create `kmsKeyArn` environment variable and `awsRegion` for both lambdas

## Manually configure API gateway for myzappi-login
* Click Add Trigger, API Gateway
* Create new REST API with Open security
* Create POST method for the API and deploy it

* Create `loginUrl` environment variable with the API endpoint for myzappi-login lambda

## Add Alexa trigger to lambda manually
* Copy skillID from Alexa Skill and copy into new Alexa Trigger in Lambda
* Add Lambda Function ARN to endpoints in Alexa skill configuration
