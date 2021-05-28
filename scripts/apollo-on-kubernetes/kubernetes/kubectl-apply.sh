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

# fat-env(test-alpha-env)
kubectl apply -f apollo-env-test-alpha/service-mysql-for-apollo-test-alpha-env.yaml --record && \
kubectl apply -f apollo-env-test-alpha/service-apollo-config-server-test-alpha.yaml --record && \
kubectl apply -f apollo-env-test-alpha/service-apollo-admin-server-test-alpha.yaml --record

# uat-env(test-beta-env)
kubectl apply -f apollo-env-test-beta/service-mysql-for-apollo-test-beta-env.yaml --record && \
kubectl apply -f apollo-env-test-beta/service-apollo-config-server-test-beta.yaml --record && \
kubectl apply -f apollo-env-test-beta/service-apollo-admin-server-test-beta.yaml --record

# prod-env
kubectl apply -f apollo-env-prod/service-mysql-for-apollo-prod-env.yaml --record && \
kubectl apply -f apollo-env-prod/service-apollo-config-server-prod.yaml --record && \
kubectl apply -f apollo-env-prod/service-apollo-admin-server-prod.yaml --record

# portal
kubectl apply -f service-apollo-portal-server.yaml --record
