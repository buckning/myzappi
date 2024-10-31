# Welcome to MyZappi!

Task list:
Launch screen
Add QR code: https://s3.eu-west-1.amazonaws.com/www.myzappiunofficial.com/assets/images/qrcode.jpg


SetChargeMode
1. Prompt the user to confirm if they want to unlock the charge mode if it is locked

Create validation interceptor for slots

Show the energy usage graph as a background image for getting the status. This should be done in a separate thread.

The most popular commands from about 10 months are:
1. Set charge mode
2. Go green
3. Set eddi mode to stopped
4. Charge my car
5. Set eddi mode to normal
6. Status summary
7. Get plug status
8. Get charge rate - show graph of just the charge rate
9. Get solar report - show graph of solar generation

These commands should be as good as possible, offering value to the user over the myenergi app

## What is MyZappi?
MyZappi is an Alexa skill that can be used to control your myenergi Zappi device all through an Amazon Echo or Alexa device.
It offers functionality such as:
* Get the current status of the Zappi, including import, export, solar generation, charge add to E.V., etc.
* Set the Zappi charge mode to Eco, Eco+, Fast or Stop
* Enable boost for a specific duration or kilowatt amount
* Get energy usage for a specific day
* Check if your E.V. is plugged in

## Architecture
There are 3 AWS lambda functions that are used by the application and an Angular application.
The 3 lambda functions are:
* `api`: contains all the REST APIs that are used by the Angular application
* `myzappi-alexa-lambda`: contains the Alexa skill that is used to control the Zappi
* `sqs-handler`: consumes schedule events, which were initially supposed to be from SQS, but are now from a Eventbridge scheduler

![alt text](https://github.com/buckning/myzappi/blob/main/docs/Architecture.png?raw=true)

This project is broken up into a number of modules and an Angular application and are contained in the following directories:
* `api`: REST APIs that is used by the Angular application
* `core`: The core of the application and contains common code between the API and the Alexa skill
* `login-with-amazon`: Client to interact with Login with Amazon and other Amazon services
* `myenergi-client`: Client to interact with the myenergi APIs
* `myzappi-alexa-lambda`: Contains the Alexa skill
* `sqs-handler`: Contains the code that is used to handle schedule events
* `site`: Contains the Angular application


## Build
Build the project with the following command
```
gradle clean build
```

### Build and deploy lambdas
Build the APIs, which are used by the website
```
./buildAndDeployApi.sh
```
Build and deploy the Alexa skill
```
./buildAndDeploySkill.sh
```
Build and deploy the schedule handler
```
./buildAndDeploySqs.sh
```

### Build and deploy website
Build the website
```
cd site/myzappi
ng build
```
Resulting files are found in `site/myzappi/dist/myzappi`

There is no automation to deploy the website, it is all manual.
1. Upload the contents of `site/myzappi/dist/myzappi` to the S3 bucket `www.myzappiunofficial.com`
2. Invalidate the CDN cache in CloudFront for `www.myzappiunofficial.com`. This is done by creating an invalidation for `/*`

### Running the website locally
To login to the website locally, the Amazon authorize URL needs to change to http://localhost:4200 in
`site/myzappi/src/app/logged-out-content/logged-out-content.component.ts`

Run the website locally with the following commands
```
cd site/myzappi
ng serve
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

aws dynamodb create-table \
  --table-name myenergi-creds \
  --attribute-definitions AttributeName=amazon-user-id,AttributeType=S \
  --key-schema AttributeName=amazon-user-id,KeyType=HASH \
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

Contains all the myenergi device information for a user as a json blob
```
aws dynamodb create-table \
--table-name devices \
--attribute-definitions AttributeName=user-id,AttributeType=S \
--key-schema AttributeName=user-id,KeyType=HASH \
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
