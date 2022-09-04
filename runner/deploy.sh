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
version=0.0.1
server=docker.inmo.dev

assert_success ../gradlew build
# scp ./build/distributions/AutoPostTestTelegramBot-1.0.0.zip ./config.json developer@insanusmokrassar.dev:/tmp/
assert_success sudo docker build -t $app:"$version" .
assert_success sudo docker tag $app:"$version" $server/$app:$version
assert_success sudo docker tag $app:"$version" $server/$app:latest
assert_success sudo docker push $server/$app:$version
assert_success sudo docker push $server/$app:latest
