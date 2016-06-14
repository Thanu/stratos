/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
CREATE DATABASE IF NOT EXISTS WSO2_PPAAS_ANALYTICS_DB;
USE WSO2_PPAAS_ANALYTICS_DB;
CREATE TABLE IF NOT EXISTS MEMBER_LIST(ClusterId VARCHAR2(150), MemberId VARCHAR2(150), MemberStatus VARCHAR2(50));
CREATE TABLE IF NOT EXISTS AVG_MEMORY_CONSUMPTION_STATS(Time NUMBER(20), ClusterId VARCHAR2(150), ClusterInstanceId VARCHAR2(150), NetworkPartitionId VARCHAR2(150), Value NUMERIC, INDEX cluster_avg_mc_idx(ClusterId, Time)) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS M_AVG_MEMORY_CONSUMPTION_STATS(Time NUMBER(20), MemberId VARCHAR2(150), ClusterId VARCHAR2(150), ClusterInstanceId VARCHAR2(150), NetworkPartitionId VARCHAR2(150), Value NUMERIC, INDEX member_avg_mc_idx(ClusterId, MemberId, Time)) DEFAULT CHARACTER SET utf8  DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS AVG_LOAD_AVERAGE_STATS(Time NUMBER(20), ClusterId VARCHAR2(150), ClusterInstanceId VARCHAR2(150), NetworkPartitionId VARCHAR2(150), Value NUMERIC, INDEX cluster_avg_la_idx (ClusterId, Time)) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS M_AVG_LOAD_AVERAGE_STATS(Time NUMBER(20), MemberId VARCHAR2(150), ClusterId VARCHAR2(150), ClusterInstanceId VARCHAR2(150), NetworkPartitionId VARCHAR2(150), Value NUMERIC, INDEX member_avg_la_idx(ClusterId, MemberId, Time)) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS AVG_IN_FLIGHT_REQUESTS(Time NUMBER(20), ClusterId VARCHAR2(150), ClusterInstanceId VARCHAR2(150), NetworkPartitionId VARCHAR2(150), Count NUMERIC, INDEX cluster_avg_rif_idx (ClusterId, Time)) DEFAULT CHARACTER SET utf8  DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS SCALING_DETAILS(Time NUMBER(20), ScalingDecisionId VARCHAR2(150), ClusterId VARCHAR2(150), MinInstanceCount INT, MaxInstanceCount INT, RIFPredicted INT, RIFThreshold INT, RIFRequiredInstances INT, MCPredicted INT, MCThreshold INT, MCRequiredInstances INT, LAPredicted INT, LAThreshold INT,LARequiredInstances INT, RequiredInstanceCount INT, ActiveInstanceCount INT, AdditionalInstanceCount INT, ScalingReason VARCHAR2(150));

