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
# create namespace
kubectl create namespace sre

# dev-env
kubectl apply -f apollo-env-dev/service-mysql-for-apollo-dev-env.yaml --record && \
kubectl apply -f apollo-env-dev/service-apollo-config-server-dev.yaml --record && \
kubectl apply -f apollo-env-dev/service-apollo-admin-server-dev.yaml --record

# fat-env
kubectl apply -f apollo-env-fat/service-mysql-for-apollo-fat-env.yaml --record && \
kubectl apply -f apollo-env-fat/service-apollo-config-server-fat.yaml --record && \
kubectl apply -f apollo-env-fat/service-apollo-admin-server-fat.yaml --record

# uat-env
kubectl apply -f apollo-env-uat/service-mysql-for-apollo-uat-env.yaml --record && \
kubectl apply -f apollo-env-uat/service-apollo-config-server-uat.yaml --record && \
kubectl apply -f apollo-env-uat/service-apollo-admin-server-uat.yaml --record

# prod-env
kubectl apply -f apollo-env-prod/service-mysql-for-apollo-prod-env.yaml --record && \
kubectl apply -f apollo-env-prod/service-apollo-config-server-prod.yaml --record && \
kubectl apply -f apollo-env-prod/service-apollo-admin-server-prod.yaml --record

# portal
kubectl apply -f service-apollo-portal-server.yaml --record
