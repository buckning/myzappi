#!/bin/zsh
gradle build shadowJar
aws s3 rm s3://myzappi-builds/api-all.jar
aws s3 cp ./api/target/libs/api-all.jar  s3://myzappi-builds/api-all.jar
aws lambda update-function-code --function-name myzappi-login --s3-bucket myzappi-builds --s3-key api-all.jar
