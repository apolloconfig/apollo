#!/bin/bash
#
# Copyright 2024 Apollo Authors
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
# Chinese reference website https://www.apolloconfig.com/#/zh/portal/apollo-open-api-platform
# English reference website https://www.apolloconfig.com/#/en/portal/apollo-open-api-platform

# export global variables
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
temp_namespace_format=yaml
echo "create namespace: namespace name = '${temp_namespace_name}', app id = '${APOLLO_APP_ID}', format = '${temp_namespace_format}'"
namespace_create ${APOLLO_APP_ID} ${temp_namespace_name} ${temp_format} false 'create by openapi, bash scripts' ${APOLLO_USER}
printf "\n\n"
####################################### end of namespace #######################################

####################################### item #######################################
# create an item, i.e a key value pair
temp_item_key="openapi-usage-create-item-key-$(date +%s)"
temp_item_value="openapi-usage-create-item-value-$(date +%s)"
echo -e "create item: app id = ${APOLLO_APP_ID} env = ${APOLLO_ENV} key = ${temp_item_key} value = ${temp_item_value}"
item_create ${APOLLO_ENV} ${APOLLO_APP_ID} default application ${temp_item_key} ${temp_item_value} "openapi-create-item" ${APOLLO_USER}
printf "\n\n"

# update an item
echo "show update failed when item key not exists"
sleep 1
temp_item_key="openapi-usage-update-item-key-$(date +%s)"
temp_item_value="openapi-usage-update-item-value-$(date +%s)"
item_update ${APOLLO_ENV} ${APOLLO_APP_ID} default application ${temp_item_key} ${temp_item_value} "openapi-update-item" ${APOLLO_USER}
printf "\n\n"

echo "show after created, update success"
item_create ${APOLLO_ENV} ${APOLLO_APP_ID} default application ${temp_item_key} ${temp_item_value} "openapi-create-item" ${APOLLO_USER}
temp_item_value="item-update-success"
printf "\n"
item_update ${APOLLO_ENV} ${APOLLO_APP_ID} default application ${temp_item_key} ${temp_item_value} "openapi-update-item" ${APOLLO_USER}
printf "\n\n"

echo "show Update an item of a namespace, if item doesn't exist, create it"
sleep 1
temp_item_key="openapi-usage-item_update_create_if_not_exists-key-$(date +%s)"
temp_item_value="openapi-usage-item_update_create_if_not_exists-value-$(date +%s)"
echo "create it, key = '${temp_item_key}' value = '${temp_item_value}'"
item_update_create_if_not_exists ${APOLLO_ENV} ${APOLLO_APP_ID} default application ${temp_item_key} ${temp_item_value} "openapi-update-item" ${APOLLO_USER} ${APOLLO_USER}
temp_item_value="openapi-value-of-item_update_create_if_not_exists"
echo "update it, key = '${temp_item_key}' value = '${temp_item_value}'"
item_update_create_if_not_exists ${APOLLO_ENV} ${APOLLO_APP_ID} default application ${temp_item_key} ${temp_item_value} "openapi-update-item" ${APOLLO_USER} ${APOLLO_USER}
printf "\n\n"

echo "show delete item failed"
item_delete ${APOLLO_ENV} ${APOLLO_APP_ID} default application "key-be-deleted" ${APOLLO_USER}
printf "\nshow delete item success\n"
item_delete ${APOLLO_ENV} ${APOLLO_APP_ID} default application ${temp_item_key} ${APOLLO_USER}
printf "\n\n"
####################################### end of item #######################################

####################################### namespace release #######################################
temp_namespace_name="application-$(date +%s)"
temp_namespace_format=properties
echo -e "create namespace: namespace name = '${temp_namespace_name}', app id = '${APOLLO_APP_ID}', format = '${temp_namespace_format}'"
namespace_create ${APOLLO_APP_ID} ${temp_namespace_name} ${temp_namespace_format} false 'create by openapi, bash scripts for release' ${APOLLO_USER}
echo -e "\ncreate or update an item '${temp_item_key}'='${temp_item_value}'"
item_update_create_if_not_exists ${APOLLO_ENV} ${APOLLO_APP_ID} default ${temp_namespace_name} ${temp_item_key} ${temp_item_value} "openapi-update-item" ${APOLLO_USER} ${APOLLO_USER}
echo -e "\nrelease namespace: '${temp_namespace_name}'"
namespace_release ${APOLLO_ENV} ${APOLLO_APP_ID} ${APOLLO_CLUSTER} ${temp_namespace_name} 'releaseTitle-openapi-2021-01-01' 'releaseComment-openapi' ${APOLLO_USER}
printf "\n\n"
####################################### end of namespace release #######################################