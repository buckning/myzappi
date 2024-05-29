#!/bin/bash
./gradlew build shadowJar
aws s3 rm s3://myzappi-builds/sqs-handler-all.jar
aws s3 cp sqs-handler/target/libs/sqs-handler-all.jar  s3://myzappi-builds/sqs-handler-all.jar
aws lambda update-function-code --function-name sqs-handler --s3-bucket myzappi-builds --s3-key sqs-handler-all.jar
