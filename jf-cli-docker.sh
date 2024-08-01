# SET meta-data to differentiate application category, such as application or internal-library
# export PACKAGE_CATEGORIES=(WEBAPP, SERVICE, LIBRARY, BASEIMAGE)

# clean data
echo "\n**** CLEAN ****"
docker image prune --all --force --filter "until=72h" && docker system prune --all --force --filter "until=72h" && docker builder prune --all --force && docker image ls

# Config - Artifactory info# https://docs.jfrog-applications.jfrog.io/jfrog-applications/jfrog-cli/configurations/jfrog-platform-configuration 

export JF_RT_URL="https://psazuse.jfrog.io" JFROG_NAME="psazuse" JFROG_RT_USER="krishnam" JF_BEARER_TOKEN="<GET YOUR KEY>" JFROG_CLI_LOG_LEVEL=DEBUG

echo " JFROG_NAME: $JFROG_NAME \n JF_RT_URL: $JF_RT_URL \n JFROG_RT_USER: $JFROG_RT_USER \n JFROG_CLI_LOG_LEVEL: $JFROG_CLI_LOG_LEVEL \n "

## Health check
jf rt ping --url=${JF_RT_URL}/artifactory

# MVN 
## Config - project
### CLI
export BUILD_NAME="spring-petclinic" BUILD_ID="cmd.$(date '+%Y-%m-%d-%H-%M')" PACKAGE_CATEGORY="WEBAPP-CONTAINER"

### Jenkins
# export BUILD_NAME=${env.JOB_NAME} BUILD_ID=${env.BUILD_ID} PACKAGE_CATEGORY="WEBAPP-CONTAINER"

# References: 
# https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#using-environment-variables 
# https://wiki.jenkins.io/JENKINS/Building+a+software+project 

### CMD
export RT_PROJECT_REPO="krishnam-mvn" RT_DOCKER_REPO="krishnam-docker"

echo " BUILD_NAME: $BUILD_NAME \n BUILD_ID: $BUILD_ID \n JFROG_CLI_LOG_LEVEL: $JFROG_CLI_LOG_LEVEL  \n RT_PROJECT_REPO: $RT_PROJECT_REPO \n RT_DOCKER_REPO: $RT_DOCKER_REPO \n "

jf mvnc --server-id-resolve ${JFROG_NAME} --server-id-deploy ${JFROG_NAME} --repo-resolve-releases ${RT_PROJECT_REPO}-virtual --repo-resolve-snapshots ${RT_PROJECT_REPO}-virtual --repo-deploy-releases ${RT_PROJECT_REPO}-local --repo-deploy-snapshots ${RT_PROJECT_REPO}-dev-local

## Audit
echo "\n\n**** MVN: Audit ****"
jf audit --mvn --extended-table=true

## Create Build
echo "\n\n**** MVN: clean install ****"
jf mvn clean install -DskipTests=true --build-name=${BUILD_NAME} --build-number=${BUILD_ID} --detailed-summary=true --scan=true

## scan packages
echo "\n\n**** JF: scan ****"
jf scan . --extended-table=true --format=simple-json --server-id=${JFROG_NAME}

## Docker
### config
export DOCKER_PWD="<GET_YOUR_OWN_KEY>" && echo "\n DOCKER_PWD: $DOCKER_PWD \n "
docker login psazuse.jfrog.io -u krishnam -p ${DOCKER_PWD}

### Create image and push
echo "\n\n**** Docker: build image ****"
docker image build -f Dockerfile-cli --platform linux/amd64,linux/arm64 -t psazuse.jfrog.io/${RT_DOCKER_REPO}-virtual/${BUILD_NAME}:${BUILD_ID} --output=type=image .

docker inspect psazuse.jfrog.io/${RT_DOCKER_REPO}-virtual/${BUILD_NAME}:${BUILD_ID} --format='{{.Id}}'

echo "\n BUILD_NAME: $BUILD_NAME \n BUILD_ID: $BUILD_ID \n JFROG_CLI_LOG_LEVEL: $JFROG_CLI_LOG_LEVEL  \n RT_PROJECT_REPO: $RT_PROJECT_REPO \n RT_DOCKER_REPO: $RT_DOCKER_REPO \n "


#### Tag with latest also
# docker tag psazuse.jfrog.io/krishnam-docker-virtual/${BUILD_NAME}:${BUILD_ID} psazuse.jfrog.io/krishnam-docker-virtual/${BUILD_NAME}:latest 

### Docker Push image
echo "\n\n**** Docker: jf push ****"
jf docker push psazuse.jfrog.io/${RT_DOCKER_REPO}-virtual/${BUILD_NAME}:${BUILD_ID} --build-name=${BUILD_NAME} --build-number=${BUILD_ID} --detailed-summary=true

# docker builder prune --all --force

### Scan image
echo "\n\n**** Docker: jf scan ****"
jf docker scan psazuse.jfrog.io/${RT_DOCKER_REPO}-virtual/${BUILD_NAME}:${BUILD_ID} 

## bdc: build-docker-create, Adding Published Docker Images to the Build-Info 
echo "\n\n**** Docker: build create ****"
# export imageSha256=$(jf rt curl -XGET "/api/storage/krishnam-docker-virtual/spring-petclinic/cmd.2024-07-31-18-35/list.manifest.json" | jq -r '.originalChecksums.sha256')

export imageSha256=$(jf rt curl -XGET "/api/storage/${RT_DOCKER_REPO}-virtual/${BUILD_NAME}/${BUILD_ID}/list.manifest.json" | jq -r '.originalChecksums.sha256')

echo psazuse.jfrog.io/krishnam-docker-virtual/${BUILD_NAME}:${BUILD_ID}@sha256:${imageSha256} > image-file-details

jf rt bdc krishnam-docker-virtual --image-file image-file-details --build-name ${BUILD_NAME}  --build-number ${BUILD_ID} 


## bp:build-publish - Publish build info
echo "\n\n**** Docker: build publish ****"
jf rt bce ${BUILD_NAME} ${BUILD_ID}
jf rt bag ${BUILD_NAME} ${BUILD_ID}
jf rt bp ${BUILD_NAME} ${BUILD_ID} --detailed-summary=true

# bs: Build-Scan
echo "\n\n**** build scan ****"
jf bs ${BUILD_NAME} ${BUILD_ID} --rescan=true 


## bdc: build-docker-promote,
echo "\n\n**** Docker: build promote ****"
jf rt docker-promote psazuse.jfrog.io/krishnam-docker-virtual/${BUILD_NAME}:${BUILD_ID} krishnam-docker-dev-local krishnam-docker-qa-local



rm -rf image-file-details


<<comment

## RBv2: release bundle - create
echo " BUILD_NAME: $BUILD_NAME \n BUILD_ID: $BUILD_ID \n RT_PROJECT_REPO: $RT_PROJECT_REPO  \n RT_PROJECT_RB_SIGNING_KEY: $RT_PROJECT_RB_SIGNING_KEY  \n "

echo "{\"builds\": [{\"name\": \"${BUILD_NAME}\", \"number\": \"${BUILD_ID}\"}]}" > build-spec.json && jf rbc --sync=true --url="${JF_RT_URL}" --access-token="${JF_BEARER_TOKEN}" --signing-key="${RT_PROJECT_RB_SIGNING_KEY}" --builds=build-spec.json ${BUILD_NAME} ${BUILD_ID} 

## RBv2: release bundle - DEV promote
jf rbp --sync=true --url="${JF_RT_URL}" --access-token="${JF_BEARER_TOKEN}" --signing-key="${RT_PROJECT_RB_SIGNING_KEY}" ${BUILD_NAME} ${BUILD_ID} DEV

## RBv2: release bundle - QA promote
jf rbp --sync=true --url="${JF_RT_URL}" --access-token="${JF_BEARER_TOKEN}" --signing-key="${RT_PROJECT_RB_SIGNING_KEY}" ${BUILD_NAME} ${BUILD_ID} QA


comment