--
-- Copyright 2023 Apollo Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
/* # CREATE apolloconfigdb; */
/*  # ------------------------------------------------------------ */

CREATE DATABASE "ApolloConfigDB";

\c "ApolloConfigDB"

CREATE SCHEMA apolloconfig;

GRANT ALL PRIVILEGES ON DATABASE "ApolloConfigDB" to postgres;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA apolloconfig to postgres;

GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA apolloconfig to postgres;

GRANT ALL PRIVILEGES ON SCHEMA apolloconfig TO postgres;


/* # CREATE apolloconfigdb; */
/* # ------------------------------------------------------------ */

CREATE DATABASE "ApolloPortalDB";

\c "ApolloPortalDB"

CREATE SCHEMA apolloportal;

GRANT ALL PRIVILEGES ON DATABASE "ApolloPortalDB" to postgres;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA apolloportal to postgres;

GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA apolloportal to postgres;

GRANT ALL PRIVILEGES ON SCHEMA apolloportal TO postgres;
