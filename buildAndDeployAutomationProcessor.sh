#!/bin/bash
./gradlew build shadowJar
aws s3 rm s3://myzappi-builds/automation-processor-all.jar
aws s3 cp ./automation-processor/target/libs/automation-processor-all.jar s3://myzappi-builds/automation-processor-all.jar
aws lambda update-function-code --function-name automation-processor --s3-bucket myzappi-builds --s3-key automation-processor-all.jar
