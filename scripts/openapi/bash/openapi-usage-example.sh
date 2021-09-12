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
# load functions
source openapi.sh

###################
# get cluster
cluster_get openapi DEV default
# create cluster. To forbid cluster xxx already exists, add timestamp to suffix
cluster_create openapi DEV "cluster-$(date +%s)" apollo
