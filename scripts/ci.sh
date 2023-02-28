#!/bin/bash

set -e

DIR=$(cd `dirname $0` && pwd)

pushd $DIR
cd ..

(
  cd cdk
  npm ci
  npm run lint
  npm test
  npm run synth
)

sbt -java-home $JAVA_HOME clean riffRaffUpload

popd