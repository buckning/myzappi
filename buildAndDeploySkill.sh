#!/bin/zsh
mvn clean verify
aws s3 rm s3://myzappi-builds/myzappi-alexa-lambda-1.0-SNAPSHOT.jar
aws s3 cp ./myzappi-alexa-lambda/target/myzappi-alexa-lambda-1.0-SNAPSHOT.jar s3://myzappi-builds/myzappi-alexa-lambda-1.0-SNAPSHOT.jar
aws lambda update-function-code --function-name myzappi --s3-bucket myzappi-builds --s3-key myzappi-alexa-lambda-1.0-SNAPSHOT.jar
