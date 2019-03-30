#!/usr/bin/env bash

cd ../

export HOME_FOLDER=`pwd`

export CI_DEPLOY_TAG=${BUILD_NUMBER}

echo "Create docker config"
mkdir -p /root/.docker

set -e

# deploy dockers
echo $DOCKER_AUTH_CONFIG > /root/.docker/config.json

echo $MAVEN_CONFIG > /opt/maven_settings_yad.xml

MVN_PARAMS='/opt/buildagent/tools/maven3/bin/mvn -s /opt/maven_settings_yad.xml -B'

function getDockerRepository {
   echo 'docker-local.artifactory.corp.nbakaev.com/'
}

function getDockerArtifactName {
    ${MVN_PARAMS} org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=docker.image.name 2>/dev/null | grep -Ev '(^\[|Download\w+:)'
}

function getArtifactFinalName {
    ${MVN_PARAMS} org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.build.finalName 2>/dev/null | grep -Ev '(^\[|Download\w+:)'
}

function buildSpringBoot {

    ${MVN_PARAMS} clean package -DskipTests=true

    curl -L https://yt-dl.org/downloads/latest/youtube-dl -o youtube-dl
    chmod + x youtube-dl

    DOCKER_IMAGE=`getDockerRepository``getDockerArtifactName`
    DOCKER_JOB_IMAGE_TAG=${DOCKER_IMAGE}:${BUILD_NUMBER}
    echo "TAG: ${DOCKER_JOB_IMAGE_TAG}"

    echo "Tag pipeline job tag"
    docker build -t $DOCKER_JOB_IMAGE_TAG --build-arg JAR_FILE=target/`getArtifactFinalName`.jar .
    docker push $DOCKER_JOB_IMAGE_TAG

    if [[ "${CI_COMMIT_REF_NAME}" == "master" ]];then
        echo "Label latest docker tag & push"
        docker tag $DOCKER_JOB_IMAGE_TAG $DOCKER_IMAGE:latest
        docker push $DOCKER_IMAGE:latest
    fi
}

echo "Building YD"
cd $HOME_FOLDER && buildSpringBoot
