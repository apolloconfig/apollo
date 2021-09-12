#!/bin/bash
# title                     openapi-usage-example.sh
# description               show how to use openapi.sh
# author                    wxq
# date                      2021-09-12
# Chinese reference website https://www.apolloconfig.com/#/zh/usage/apollo-open-api-platform
# English reference website https://www.apolloconfig.com/#/en/usage/apollo-open-api-platform

# export global varialbes
export APOLLO_PORTAL_ADDRESS=http://106.54.227.205
export APOLLO_OPENAPI_TOKEN=284fe833cbaeecf2764801aa73965080b184fc88
# load functions
source openapi.sh

###################
# get cluster
cluster_get openapi DEV default
# create cluster. To forbid cluster xxx already exists, add timestamp to suffix
cluster_create openapi DEV "cluster-$(date +%s)" apollo
