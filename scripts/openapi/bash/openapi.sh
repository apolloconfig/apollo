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

# title                     openapi.sh
# description               functions to call openapi through http
# author                    wxq
# date                      2021-09-12
# Chinese reference website https://www.apolloconfig.com/#/zh/portal/apollo-open-api-platform
# English reference website https://www.apolloconfig.com/#/en/portal/apollo-open-api-platform

####################################### Global variables #######################################
# portal's address, just support 1 address without suffix '/'
# Don't use http://ip:port/ with suffix '/' or multiple address http://ip1:port1,http://ip2:port2
APOLLO_PORTAL_ADDRESS=${APOLLO_PORTAL_ADDRESS:-http://ip:port}
APOLLO_OPENAPI_TOKEN=${APOLLO_OPENAPI_TOKEN:-please_change_me_by_environment_variable}
CURL_OPTIONS=${CURL_OPTIONS:-}

echo "apollo portal address: ${APOLLO_PORTAL_ADDRESS}"
echo "curl options: ${CURL_OPTIONS}"
####################################### end of Global variables #######################################

####################################### basic http call #######################################
#######################################
# Http get by curl.
# Globals:
#   APOLLO_PORTAL_ADDRESS:  portal's address
#   APOLLO_OPENAPI_TOKEN:   openapi's token
#   CURL_OPTIONS:           options in curl
# Arguments:
#   url_suffix
#######################################
function openapi_get() {
  local url_suffix=$1

  local url="${APOLLO_PORTAL_ADDRESS}/${url_suffix}"
  curl ${CURL_OPTIONS} --header "Authorization: ${APOLLO_OPENAPI_TOKEN}" --header "Content-Type: application/json;charset=UTF-8" "${url}"
}

#######################################
# Http post by curl.
# Globals:
#   APOLLO_PORTAL_ADDRESS:  portal's address
#   APOLLO_OPENAPI_TOKEN:   openapi's token
#   CURL_OPTIONS:           options in curl
# Arguments:
#   url_suffix
#   body
#######################################
function openapi_post() {
  local url_suffix=$1
  local body=$2

  local url="${APOLLO_PORTAL_ADDRESS}/${url_suffix}"
  curl ${CURL_OPTIONS} --header "Authorization: ${APOLLO_OPENAPI_TOKEN}" --header "Content-Type: application/json;charset=UTF-8" --data "${body}" "${url}"
}

#######################################
# Http put by curl.
# Globals:
#   APOLLO_PORTAL_ADDRESS:  portal's address
#   APOLLO_OPENAPI_TOKEN:   openapi's token
#   CURL_OPTIONS:           options in curl
# Arguments:
#   url_suffix
#   body
#######################################
function openapi_put() {
  local url_suffix=$1
  local body=$2

  local url="${APOLLO_PORTAL_ADDRESS}/${url_suffix}"
  curl ${CURL_OPTIONS} --header "Authorization: ${APOLLO_OPENAPI_TOKEN}" --header "Content-Type: application/json;charset=UTF-8" -X PUT --data "${body}" "${url}"
}

#######################################
# Http delete by curl.
# Globals:
#   APOLLO_PORTAL_ADDRESS:  portal's address
#   APOLLO_OPENAPI_TOKEN:   openapi's token
#   CURL_OPTIONS:           options in curl
# Arguments:
#   url_suffix
#   body
#######################################
function openapi_delete() {
  local url_suffix=$1
  local body=$2

  local url="${APOLLO_PORTAL_ADDRESS}/${url_suffix}"
  curl ${CURL_OPTIONS} --header "Authorization: ${APOLLO_OPENAPI_TOKEN}" --header "Content-Type: application/json;charset=UTF-8" -X DELETE --data "${body}" "${url}"
}
####################################### end of basic http call #######################################


####################################### cluster #######################################
#######################################
# Get cluster.
# 获取集群
# Arguments:
#   env
#   appId
#   clusterName
#######################################
function cluster_get() {
    local env=$1
    local appId=$2
    local clusterName=$3
    openapi_get "openapi/v1/envs/${env}/apps/${appId}/clusters/${clusterName}"
}

#######################################
# Create cluster in app's environment.
# 创建集群
# Arguments:
#   env
#   appId
#   clusterName
#   dataChangeCreatedBy
#######################################
function cluster_create() {
  local env=$1
  local appId=$2
  local clusterName=$3
  local dataChangeCreatedBy=$4
  openapi_post "openapi/v1/envs/${env}/apps/${appId}/clusters" "$(cat <<BODY
{
    "name":"${clusterName}",
    "appId":"${appId}",
    "dataChangeCreatedBy":"${dataChangeCreatedBy}"
}
BODY
)"
}
####################################### end of cluster #######################################

####################################### namespace #######################################
#######################################
# Create a namespace of a app.
# 创建namespace
# Arguments:
#   appId
#   name
#   format
#   isPublic
#   comment
#   dataChangeCreatedBy
#######################################
function namespace_create() {
  local appId=$1
  local name=$2
  local format=$3
  local isPublic=$4
  local comment=$5
  local dataChangeCreatedBy=$6

  openapi_post "openapi/v1/apps/${appId}/appnamespaces" "$(cat <<BODY
{
  "name": "${name}",
  "appId": "${appId}",
  "format": "${format}",
  "isPublic": ${isPublic},
  "comment": "${comment}",
  "dataChangeCreatedBy": "${dataChangeCreatedBy}"
}
BODY
)"
}

#######################################
# Release a namespace.
# 发布配置
# Arguments:
#   env
#   appId
#   clusterName
#   namespaceName
#   releaseTitle
#   releaseComment
#   releasedBy
#######################################
function namespace_release() {
  local env=$1
  local appId=$2
  local clusterName=$3
  local namespaceName=$4
  
  local releaseTitle=$5
  local releaseComment=$6
  local releasedBy=$7

  openapi_post "openapi/v1/envs/${env}/apps/${appId}/clusters/${clusterName}/namespaces/${namespaceName}/releases" "$(cat <<BODY
{
    "releaseTitle":"${releaseTitle}",
    "releaseComment":"${releaseComment}",
    "releasedBy":"${releasedBy}"
}
BODY
)"
}
####################################### end of namespace #######################################


####################################### item #######################################
#######################################
# Create an item of a namespace.
# 新增配置
# Arguments:
#   env
#   appId
#   clusterName
#   namespaceName
#   key
#   value
#   comment
#   dataChangeCreatedBy
#######################################
function item_create() {
  local env=$1
  local appId=$2
  local clusterName=$3
  local namespaceName=$4
  local key=$5
  local value=$6
  local comment=$7
  local dataChangeCreatedBy=$8

  openapi_post "openapi/v1/envs/${env}/apps/${appId}/clusters/${clusterName}/namespaces/${namespaceName}/items" "$(cat <<BODY
{
    "key":"${key}",
    "value":"${value}",
    "comment":"${comment}",
    "dataChangeCreatedBy":"${dataChangeCreatedBy}"
}
BODY
)"
}

#######################################
# Update an item of a namespace.
# 修改配置
# Arguments:
#   env
#   appId
#   clusterName
#   namespaceName
#   key
#   value
#   comment
#   dataChangeLastModifiedBy
#######################################
function item_update() {
  local env=$1
  local appId=$2
  local clusterName=$3
  local namespaceName=$4
  local key=$5
  local value=$6
  local comment=$7
  local dataChangeLastModifiedBy=$8

  openapi_put "openapi/v1/envs/${env}/apps/${appId}/clusters/${clusterName}/namespaces/${namespaceName}/items/${key}" "$(cat <<BODY
{
  "key":"${key}",
  "value":"${value}",
  "comment":"${comment}",
  "dataChangeLastModifiedBy":"${dataChangeLastModifiedBy}"
}
BODY
)"
}

#######################################
# Update an item of a namespace, if item doesn't exist, create it.
# 修改配置，当配置不存在时自动创建
# Arguments:
#   env
#   appId
#   clusterName
#   namespaceName
#   key
#   value
#   comment
#   dataChangeLastModifiedBy
#######################################
function item_update_create_if_not_exists() {
  local env=$1
  local appId=$2
  local clusterName=$3
  local namespaceName=$4
  local key=$5
  local value=$6
  local comment=$7
  local dataChangeLastModifiedBy=$8
  local dataChangeCreatedBy=$9

  openapi_put "openapi/v1/envs/${env}/apps/${appId}/clusters/${clusterName}/namespaces/${namespaceName}/items/${key}?createIfNotExists=true" "$(cat <<BODY
{
  "key":"${key}",
  "value":"${value}",
  "comment":"${comment}",
  "dataChangeLastModifiedBy":"${dataChangeLastModifiedBy}",
  "dataChangeCreatedBy":"${dataChangeCreatedBy}"
}
BODY
)"
}

#######################################
# Delete an item of a namespace.
# 删除配置
# Arguments:
#   env
#   appId
#   clusterName
#   namespaceName
#   key
#   operator
#######################################
function item_delete() {
  local env=$1
  local appId=$2
  local clusterName=$3
  local namespaceName=$4
  local key=$5
  local operator=$6

  openapi_delete "openapi/v1/envs/${env}/apps/${appId}/clusters/${clusterName}/namespaces/${namespaceName}/items/${key}?operator=${operator}"
}
####################################### end of item #######################################