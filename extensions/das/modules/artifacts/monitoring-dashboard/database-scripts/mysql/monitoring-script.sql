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
CREATE TABLE IF NOT EXISTS MEMBER_LIST(ClusterId VARCHAR(150), MemberId VARCHAR(150), MemberStatus VARCHAR(50));
CREATE TABLE IF NOT EXISTS AVERAGE_MEMORY_CONSUMPTION_STATS(Time BIGINT, ClusterId VARCHAR(150), ClusterInstanceId VARCHAR(150), NetworkPartitionId VARCHAR(150), Value DOUBLE, INDEX cluster_avg_mc_idx(ClusterId, Time)) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS MEMBER_AVERAGE_MEMORY_CONSUMPTION_STATS(Time BIGINT, MemberId VARCHAR(150), ClusterId VARCHAR(150), ClusterInstanceId VARCHAR(150), NetworkPartitionId VARCHAR(150), Value DOUBLE, INDEX member_avg_mc_idx(ClusterId, MemberId, Time)) DEFAULT CHARACTER SET utf8  DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS AVERAGE_LOAD_AVERAGE_STATS(Time BIGINT, ClusterId VARCHAR(150), ClusterInstanceId VARCHAR(150), NetworkPartitionId VARCHAR(150), Value DOUBLE, INDEX cluster_avg_la_idx (ClusterId, Time)) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS MEMBER_AVERAGE_LOAD_AVERAGE_STATS(Time BIGINT, MemberId VARCHAR(150), ClusterId VARCHAR(150), ClusterInstanceId VARCHAR(150), NetworkPartitionId VARCHAR(150), Value DOUBLE, INDEX member_avg_la_idx(ClusterId, MemberId, Time)) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS AVERAGE_IN_FLIGHT_REQUESTS(Time BIGINT, ClusterId VARCHAR(150), ClusterInstanceId VARCHAR(150), NetworkPartitionId VARCHAR(150), Count DOUBLE, INDEX cluster_avg_rif_idx (ClusterId, Time)) DEFAULT CHARACTER SET utf8  DEFAULT COLLATE utf8_general_ci ENGINE = MyISAM;
CREATE TABLE IF NOT EXISTS SCALING_DETAILS(Time VARCHAR(50), ScalingDecisionId VARCHAR(150), ClusterId VARCHAR(150), MinInstanceCount INT, MaxInstanceCount INT, RIFPredicted INT, RIFThreshold INT ,RIFRequiredInstances INT, MCPredicted INT, MCThreshold INT, MCRequiredInstances INT ,LAPredicted INT, LAThreshold INT,LARequiredInstances INT,RequiredInstanceCount INT ,ActiveInstanceCount INT, AdditionalInstanceCount INT, ScalingReason VARCHAR(150));
