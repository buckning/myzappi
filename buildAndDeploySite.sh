#!/bin/zsh
mvn clean verify -DskipTests
aws s3 rm s3://myzappi-builds/login-lambda-1.0-SNAPSHOT.jar
aws s3 cp ./login-lambda/target/login-lambda-1.0-SNAPSHOT.jar  s3://myzappi-builds/login-lambda-1.0-SNAPSHOT.jar
aws lambda update-function-code --function-name myzappi-login --s3-bucket myzappi-builds --s3-key login-lambda-1.0-SNAPSHOT.jar
