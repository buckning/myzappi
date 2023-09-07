#!/bin/zsh
mvn clean verify
aws s3 rm s3://myzappi-builds/api-1.0-SNAPSHOT.jar
aws s3 cp ./api/target/api-1.0-SNAPSHOT.jar  s3://myzappi-builds/api-1.0-SNAPSHOT.jar
aws lambda update-function-code --function-name myzappi-login --s3-bucket myzappi-builds --s3-key api-1.0-SNAPSHOT.jar
