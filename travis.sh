#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v46 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

# see https://github.com/SonarSource/travis-utils/bin/...
installJDK8

# NOTE: see shell source for expected environment variables
# deploy does SQ install, not yet needed
# regular_mvn_build_deploy_analyze
echo '======= Build, no analysis, no deploy'
mvn --version
mvn package \
   -Dmaven.test.redirectTestOutputToFile=false \
   -B -e -V $*
