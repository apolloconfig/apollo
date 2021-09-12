#!/bin/bash
# title                     openapi.sh
# description               functions to call openapi through http
# author                    wxq
# date                      2021-09-12
# Chinese reference website https://www.apolloconfig.com/#/zh/usage/apollo-open-api-platform
# English reference website https://www.apolloconfig.com/#/en/usage/apollo-open-api-platform

# Global variables
# portal's address, just support 1 without suffix '/'
# Don't use http://ip:port/ with suffix '/'
# or multiple address http://ip1:port1,http://ip2:port2
APOLLO_PORTAL_ADDRESS=${APOLLO_PORTAL_ADDRESS:-http://106.54.227.205}
APOLLO_OPENAPI_TOKEN=${APOLLO_OPENAPI_TOKEN:-284fe833cbaeecf2764801aa73965080b184fc88}

#######################################
# Http get by curl.
# Globals:
#   APOLLO_PORTAL_ADDRESS portal's address
#   APOLLO_OPENAPI_TOKEN openapi's token
# Arguments:
#   url_suffix
#######################################
function openapi_get() {
  local url_suffix=$1

  local url="${APOLLO_PORTAL_ADDRESS}/${url_suffix}"
  curl --header "Authorization: ${APOLLO_OPENAPI_TOKEN}" --header "Content-Type: application/json;charset=UTF-8" "${url}"
}


#######################################
# Http post by curl.
# Globals:
#   APOLLO_PORTAL_ADDRESS portal's address
#   APOLLO_OPENAPI_TOKEN openapi's token
# Arguments:
#   url_suffix
#   body
#######################################
function openapi_post() {
  local url_suffix=$1
  local body=$2

  local url="${APOLLO_PORTAL_ADDRESS}/${url_suffix}"
  curl --header "Authorization: ${APOLLO_OPENAPI_TOKEN}" --header "Content-Type: application/json;charset=UTF-8" --data "${body}" "${url}"
}

#######################################
# Create cluster in app's environment.
# Arguments:
#   app_id
#   env
#   cluster_name
# Outputs:
#   Writes location to stdout
#######################################
function cluster_get() {
    local app_id=$1
    local env=$2
    local cluster_name=$3
    openapi_get "openapi/v1/envs/${env}/apps/${app_id}/clusters/${cluster_name}"
}

#######################################
# Create cluster in app's environment.
# Arguments:
#   app_id
#   env
#   cluster_name
#   data_change_created_by
# Outputs:
#   Writes location to stdout
#######################################
function cluster_create() {
  local app_id=$1
  local env=$2
  local cluster_name=$3
  local data_change_created_by=$4
  openapi_post "openapi/v1/envs/${env}/apps/${app_id}/clusters" "$(cat <<BODY
{
    "name":"${cluster_name}",
    "appId":"${app_id}",
    "dataChangeCreatedBy":"${data_change_created_by}"
}
BODY
)"
}