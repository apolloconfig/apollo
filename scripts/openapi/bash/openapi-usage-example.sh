#!/bin/bash
#
# Copyright 2021 Apollo Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# title                     openapi-usage-example.sh
# description               show how to use openapi.sh
# author                    wxq
# date                      2021-09-12
# Chinese reference website https://www.apolloconfig.com/#/zh/usage/apollo-open-api-platform
# English reference website https://www.apolloconfig.com/#/en/usage/apollo-open-api-platform

# export global varialbes
export APOLLO_PORTAL_ADDRESS=http://106.54.227.205
export APOLLO_OPENAPI_TOKEN=284fe833cbaeecf2764801aa73965080b184fc88
export CURL_OPTIONS=""
# load functions
source openapi.sh

# set up global environment variable
APOLLO_APP_ID=openapi
APOLLO_ENV=DEV
APOLLO_CLUSTER=default
APOLLO_USER=apollo

####################################### cluster #######################################
# get cluster
printf "get cluster: env = '%s', app id = '%s', cluster = '%s'\n" ${APOLLO_ENV} ${APOLLO_APP_ID} ${APOLLO_CLUSTER}
cluster_get ${APOLLO_ENV} ${APOLLO_APP_ID} ${APOLLO_CLUSTER}
printf "\n\n"

# create cluster. To forbid cluster xxx already exists, add timestamp to suffix
temp_apollo_cluster="cluster-$(date +%s)"
printf "create cluster: env = '%s', app id = '%s', cluster = '%s'\n" ${APOLLO_ENV} ${APOLLO_APP_ID} ${temp_apollo_cluster}
cluster_create ${APOLLO_ENV} ${APOLLO_APP_ID} ${temp_apollo_cluster} ${APOLLO_USER}
printf "\n\n"
####################################### end of cluster #######################################

####################################### namespace #######################################
# create namespace
temp_namespace_name="application-123"
temp_format=yaml
printf "create namespace: namespace name = '%s', app id = '%s', format = '%s'\n" ${APOLLO_APP_ID} ${temp_namespace_name} ${temp_format}
namespace_create ${APOLLO_APP_ID} ${temp_namespace_name} ${temp_format} false 'create by openapi, bash scripts' ${APOLLO_USER}
printf "\n\n"
####################################### end of namespace #######################################

####################################### item #######################################
temp_item_key="openapi-usage-key-$(date +%s)"
temp_item_value="openapi-usage-value-$(date +%s)"
echo -e "create item: app id = ${APOLLO_APP_ID} env = ${APOLLO_ENV} key = ${temp_item_key} value = ${temp_item_value}"
item_create ${APOLLO_ENV} ${APOLLO_APP_ID} default application ${temp_item_key} ${temp_item_value} "openapi" ${APOLLO_USER}
printf "\n\n"
####################################### end of item #######################################