#!/bin/bash
./gradlew build shadowJar
aws s3 rm s3://myzappi-builds/myzappi-alexa-lambda-all.jar
aws s3 cp ./myzappi-alexa-lambda/target/libs/myzappi-alexa-lambda-all.jar s3://myzappi-builds/myzappi-alexa-lambda-all.jar
aws lambda update-function-code --function-name myzappi --s3-bucket myzappi-builds --s3-key myzappi-alexa-lambda-all.jar
