#!/bin/zsh
mvn clean verify
aws s3 rm s3://myzappi-builds/sqs-handler-1.0-SNAPSHOT.jar
aws s3 cp sqs-handler/target/sqs-handler-1.0-SNAPSHOT.jar  s3://myzappi-builds/sqs-handler-1.0-SNAPSHOT.jar
aws lambda update-function-code --function-name sqs-handler --s3-bucket myzappi-builds --s3-key sqs-handler-1.0-SNAPSHOT.jar
