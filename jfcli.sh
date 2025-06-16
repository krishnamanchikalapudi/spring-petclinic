# SET meta-data to differentiate application category, such as application or internal-library
# export PACKAGE_CATEGORIES=(WEBAPP, SERVICE, LIBRARY, BASEIMAGE)
clear
# TOKEN SETUP
# jf c add --user=krishnam --interactive=true --url=https://psazuse.jfrog.io --overwrite=true 

# Config - Artifactory info
export JF_HOST="psrbinstance2.jfrog.io" JFROG_NAME="psrbinstance2" JFROG_RT_USER="krishnam@jfrog.com" JFROG_CLI_LOG_LEVEL="DEBUG" # JF_ACCESS_TOKEN="<GET_YOUR_OWN_KEY>"
export JF_RT_URL="https://${JF_HOST}" RBv2_SIGNIN_KEY="varunm" 

# clean data
echo "\n**** CLEAN ****"
# docker image prune --all --force --filter "until=72h" && docker system prune --all --force --filter "until=72h" && docker builder prune --all --force && docker image ls

# MVN 
## Config - project
### CLI
export BUILD_NAME="spring-petclinic" BUILD_ID="cmd.$(date '+%Y-%m-%d-%H-%M')" 
export DKR_MANIFEST="list-manifest-${BUILD_ID}.json" SPEC_BP_DOCKER="dockerimage-file-details-${BUILD_ID}" SPEC_RBv2="rb2-spec-${BUILD_ID}.json"


# References: 
# https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#using-environment-variables 
# https://wiki.jenkins.io/JENKINS/Building+a+software+project 

### CMD
export RT_PROJECT_REPO="rbv2merative-mvn" RT_REPO_DOCKER="rbv2merative-docker"

echo " BUILD_NAME: $BUILD_NAME \n BUILD_ID: $BUILD_ID \n JFROG_CLI_LOG_LEVEL: $JFROG_CLI_LOG_LEVEL  \n RT_PROJECT_REPO: $RT_PROJECT_REPO \n RT_REPO_DOCKER: $RT_REPO_DOCKER \n "

jf mvnc --server-id-resolve ${JFROG_NAME} --server-id-deploy ${JFROG_NAME} --repo-resolve-releases ${RT_PROJECT_REPO}-virtual --repo-resolve-snapshots ${RT_PROJECT_REPO}-virtual --repo-deploy-releases ${RT_PROJECT_REPO}-local --repo-deploy-snapshots ${RT_PROJECT_REPO}-dev-local


## Create Build
echo "\n\n**** MVN: clean install ****"
jf mvn clean install -DskipTests=true --build-name=${BUILD_NAME} --build-number=${BUILD_ID} --detailed-summary=true --scan=true


## Docker
### config
# export DOCKER_PWD="<GET_YOUR_OWN_KEY>" 
echo "\n DOCKER_PWD: $DOCKER_PWD \n "
docker login psazuse.jfrog.io -u krishnam -p ${DOCKER_PWD}

### Create image and push
echo "\n\n**** Docker: build image ****"
docker image build -f my-files/Dockerfile-cli-mvn --platform linux/amd64,linux/arm64 -t psazuse.jfrog.io/${RT_REPO_DOCKER}-virtual/${BUILD_NAME}:${BUILD_ID} --output=type=image .

docker inspect psazuse.jfrog.io/${RT_REPO_DOCKER}-virtual/${BUILD_NAME}:${BUILD_ID} --format='{{.Id}}'

echo "\n BUILD_NAME: $BUILD_NAME \n BUILD_ID: $BUILD_ID \n JFROG_CLI_LOG_LEVEL: $JFROG_CLI_LOG_LEVEL  \n RT_PROJECT_REPO: $RT_PROJECT_REPO \n RT_REPO_DOCKER: $RT_REPO_DOCKER \n "

#### Tag with latest also
# docker tag psazuse.jfrog.io/krishnam-docker-virtual/${BUILD_NAME}:${BUILD_ID} psazuse.jfrog.io/krishnam-docker-virtual/${BUILD_NAME}:latest 

### Docker Push image
echo "\n\n**** Docker: jf push ****"
jf docker push psazuse.jfrog.io/${RT_REPO_DOCKER}-virtual/${BUILD_NAME}:${BUILD_ID} --build-name=${BUILD_NAME} --build-number=${BUILD_ID} --detailed-summary=true

# docker builder prune --all --force


## bdc: build-docker-create, Adding Published Docker Images to the Build-Info 
echo "\n\n**** Docker: build create ****"
# export imageSha256=$(jf rt curl -XGET "/api/storage/krishnam-docker-virtual/spring-petclinic/cmd.2024-07-31-18-35/list.manifest.json" | jq -r '.originalChecksums.sha256')
# curl -XGET 'https://psazuse.jfrog.io/artifactory/api/storage/krishnam-docker-virtual/spring-petclinic/cmd.2024-09-11-13-56/list.manifest.json' --header 'Content-Type:  application/json' --header "Authorization: Bearer ${JF_ACCESS_TOKEN}"
# jf rt curl -XGET "/api/storage/krishnam-docker-virtual/spring-petclinic/cmd.2024-09-11-13-56/list.manifest.json" -H "Authorization: Bearer ${JF_ACCESS_TOKEN}"

jf rt curl -XGET "/api/storage/krishnam-docker-virtual/spring-petclinic/cmd.2024-09-11-13-56/list.manifest.json" -H "Authorization: Bearer ${JF_ACCESS_TOKEN}"

export imageSha256=$(jf rt curl -XGET "/api/storage/${RT_REPO_DOCKER}-virtual/${BUILD_NAME}/${BUILD_ID}/list.manifest.json" | jq -r '.originalChecksums.sha256')
jf rt curl -XGET "/api/storage/${RT_REPO_DOCKER}-virtual/${BUILD_NAME}/${BUILD_ID}/list.manifest.json" -H "Authorization: Bearer ${JF_ACCESS_TOKEN}" -o "${DKR_MANIFEST}"
imageSha256=`cat ${DKR_MANIFEST} | jq -r '.originalChecksums.sha256'`

echo ${imageSha256}
echo ${JF_RT_HOST}/${RT_REPO_DOCKER}-virtual/${BUILD_NAME}:${BUILD_ID}@sha256:${imageSha256} > ${SPEC_BP_DOCKER}
jf rt bdc ${RT_REPO_DOCKER}-virtual --image-file ${SPEC_BP_DOCKER} --build-name ${BUILD_NAME} --build-number ${BUILD_ID} 


## bp:build-publish - Publish build info
echo "\n\n**** Docker: build publish ****"
jf rt bce ${BUILD_NAME} ${BUILD_ID}
jf rt bag ${BUILD_NAME} ${BUILD_ID}
jf rt bp ${BUILD_NAME} ${BUILD_ID} --detailed-summary=true


## RBv2: release bundle - create
# ref: https://docs.jfrog-applications.jfrog.io/jfrog-applications/jfrog-cli/cli-for-jfrog-artifactory/release-lifecycle-management
echo "\n\n**** RBv2: Create ****\n\n"
echo " BUILD_NAME: $BUILD_NAME \n BUILD_ID: $BUILD_ID \n RT_PROJECT_REPO: $RT_PROJECT_REPO  \n RT_PROJECT_RB_SIGNING_KEY: $RT_PROJECT_RB_SIGNING_KEY  \n "

  # create spec
echo "{ \"files\": [ {\"build\": \"${BUILD_NAME}/${BUILD_ID}\", \"includeDeps\": \"false\" } ] }"  > ${SPEC_RBv2}
echo "\n" && cat ${SPEC_RBv2} && echo "\n"

  # create RB to state=NEW
jf rbc ${BUILD_NAME} ${BUILD_ID} --sync="true" --url="${JF_RT_URL}" --signing-key="${RBv2_SIGNIN_KEY}" --spec="${SPEC_RBv2} 

## RBv2: release bundle - DEV promote
echo "\n\n**** RBv2: Promoted to DEV ****\n\n"
jf rbp ${BUILD_NAME} ${BUILD_ID} DEV --sync="true" --url="${JF_RT_URL}" --signing-key="${RBv2_SIGNIN_KEY}" 


## RBv2: release bundle - Prod promote
echo "\n\n**** RBv2: Promoted to DEV ****\n\n"
# jf rt dpr ${BUILD_NAME} ${RT_REPO_DOCKER}-dev-local ${RT_REPO_DOCKER}-qa-local
jf rbp ${BUILD_NAME} ${BUILD_ID} PROD --sync="true" --url="${JF_RT_URL}" --signing-key="${RBv2_SIGNIN_KEY}" 


echo "\n\n**** CLEAN UP ****\n\n"
rm -rf ${DKR_MANIFEST}
rm -rf ${SPEC_BP_DOCKER}
rm -rf ${SPEC_RBv2}

docker image prune -a --force --filter "until=24h"
docker system prune -a --force --filter "until=24h"


echo "\n\n**** DONE ****\n\n"