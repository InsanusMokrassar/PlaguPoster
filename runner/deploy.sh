#!/bin/bash

function send_notification() {
    echo "$1"
}

function assert_success() {
    "${@}"
    local status=${?}
    if [ ${status} -ne 0 ]; then
        send_notification "### Error ${status} at: ${BASH_LINENO[*]} ###"
        exit ${status}
    fi
}

app=plaguposter
version="`grep ../gradle.properties -e "^version=" | sed -e "s/version=\(.*\)/\1/"`"
server=docker.inmo.dev

assert_success ../gradlew build
assert_success sudo docker build -t $app:"$version" .
assert_success sudo docker tag $app:"$version" $server/$app:$version
assert_success sudo docker tag $app:"$version" $server/$app:latest
assert_success sudo docker push $server/$app:$version
assert_success sudo docker push $server/$app:latest
