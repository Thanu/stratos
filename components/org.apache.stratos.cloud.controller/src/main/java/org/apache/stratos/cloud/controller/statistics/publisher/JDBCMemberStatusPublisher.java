/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stratos.cloud.controller.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyHolder;
import org.apache.stratos.common.db.AnalyticsJDBCDataHolder;
import org.apache.stratos.common.db.AnalyticsJDBCManager;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

class JDBCMemberStatusPublisher extends MemberStatusPublisher {
    private static final Log log = LogFactory.getLog(JDBCMemberStatusPublisher.class);
    private static final String STATS_PUBLISHER_THREAD_POOL_ID = "member.status.jdbc.stats.publisher.thread.pool";
    private static final int STATS_PUBLISHER_THREAD_POOL_SIZE = 15;
    private static final String MEMBER_STATUS_INSERT_QUERY_SQL = "INSERT INTO MEMBER_STATUS (Time, ApplicationId, " +
            "ClusterAlias, MemberId, MemberStatus) VALUES(?,?,?,?,?)";
    private static final String MEMBER_LIST_INSERT_QUERY_SQL = "INSERT INTO MEMBER_LIST (ClusterId, MemberId, " +
            "MemberStatus) VALUES(?,?,?)";
    private static final String APPLICATION_ACTIVE_MEMBER_COUNT_INSERT_QUERY_SQL = "INSERT INTO " +
            "APPLICATION_ACTIVE_MEMBER_COUNT (timestamp, application_id, active_instances) VALUES (?,?,?)";
    private static final String CLUSTER_ACTIVE_MEMBER_COUNT_INSERT_QUERY_SQL = "INSERT INTO " +
            "CLUSTER_ACTIVE_MEMBER_COUNT (timestamp, application_id, cluster_alias, active_instances) VALUES (?,?,?,?)";
    private static volatile JDBCMemberStatusPublisher jdbcMemberStatusPublisher;
    private ExecutorService executorService;

    private JDBCMemberStatusPublisher() {
        executorService = StratosThreadPool.getExecutorService(STATS_PUBLISHER_THREAD_POOL_ID,
                STATS_PUBLISHER_THREAD_POOL_SIZE);
    }

    public static JDBCMemberStatusPublisher getInstance() {
        if (jdbcMemberStatusPublisher == null) {
            synchronized (JDBCMemberStatusPublisher.class) {
                if (jdbcMemberStatusPublisher == null) {
                    jdbcMemberStatusPublisher = new JDBCMemberStatusPublisher();
                }
            }
        }
        return jdbcMemberStatusPublisher;
    }

    private static int getClusterActiveMemberCount(Cluster cluster) {
        int activeMembersInCluster = 0;
        for (Member member : cluster.getMembers()) {
            if (member.getStatus() == MemberStatus.Active) {
                activeMembersInCluster++;
            }
        }
        return activeMembersInCluster;
    }

    public void publishActiveCountsToAnalytics() {
        Map<String, Integer> appActiveMemberCount = new HashMap<>();
        Map<Cluster, Integer> clusterActiveMemberCount = new HashMap<>();
        Long timestamp = System.currentTimeMillis();

        for (Service service : TopologyHolder.getTopology().getServices()) {
            for (Cluster cluster : service.getClusters()) {
                // count per app active member count
                Integer currentActiveCountForApp = appActiveMemberCount.get(cluster.getAppId());
                if (currentActiveCountForApp == null) {
                    currentActiveCountForApp = 0;
                }
                int activeMemberCountForThisCluster = getClusterActiveMemberCount(cluster);
                appActiveMemberCount.put(cluster.getAppId(), activeMemberCountForThisCluster +
                        currentActiveCountForApp);
                clusterActiveMemberCount.put(cluster, activeMemberCountForThisCluster);
            }
        }

        // Collect all data to process as a batch
        ArrayList<AnalyticsJDBCDataHolder> analyticsDataList = new ArrayList<>();

        // for each application id
        for (Map.Entry<String, Integer> appCount : appActiveMemberCount.entrySet()) {
            // Add query data to a list
            ArrayList<Object> queryDataList = new ArrayList<>();
            queryDataList.add(timestamp);
            queryDataList.add(appCount.getKey());
            queryDataList.add(appCount.getValue());
            analyticsDataList.add(new AnalyticsJDBCDataHolder(APPLICATION_ACTIVE_MEMBER_COUNT_INSERT_QUERY_SQL,
                    queryDataList));
        }

        for (Map.Entry<Cluster, Integer> clusterCount : clusterActiveMemberCount.entrySet()) {
            Cluster cluster = clusterCount.getKey();
            // Storing ClusterAlias for backward compatibility and usability
            // cluster id = {appid}.{clusteralias}.{cartridgetype}.{domain}
            String splitArr[] = cluster.getClusterId().split(Pattern.quote("."));
            if (splitArr.length < 4) {
                throw new RuntimeException(String.format("Invalid cluster ID. Could not derive cluster alias for " +
                        "[cluster-id] %s", cluster.getClusterId()));
            }
            String derivedClusterAlias = splitArr[1];
            ArrayList<Object> queryDataList = new ArrayList<>();
            queryDataList.add(timestamp);
            queryDataList.add(cluster.getAppId());
            queryDataList.add(derivedClusterAlias);
            queryDataList.add(clusterCount.getValue());
            analyticsDataList.add(new AnalyticsJDBCDataHolder(CLUSTER_ACTIVE_MEMBER_COUNT_INSERT_QUERY_SQL,
                    queryDataList));
        }

        // insert application and cluster active counts as a batch
        try {
            if (!analyticsDataList.isEmpty()) {
                AnalyticsJDBCManager.insertBatch(analyticsDataList);
            }
        } catch (Exception e) {
            AnalyticsJDBCManager.logBatchQueryData(analyticsDataList);
            log.error("Failed to publish member active counts analytics event data", e);
        }
    }

    @Override
    public void publish(final Long timestamp, final String applicationId, final String clusterId,
            final String clusterAlias, String clusterInstanceId, String serviceName,
            String networkPartitionId, String partitionId, final String memberId, final String status) {

        Runnable activeCountPublisherRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    publishActiveCountsToAnalytics();
                } catch (Exception e) {
                    log.error("Failed to publish current active member count", e);
                }
            }
        };
        executorService.execute(activeCountPublisherRunnable);

        Runnable memberStatusAndmemberListPublisherRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    publishMemberStatusAndMemberListInformationData(timestamp, applicationId, clusterId,
                            clusterAlias, memberId, status);
                } catch (Exception e) {
                    log.error("Failed to publish member status and member list analytics data", e);
                }
            }
        };
        executorService.execute(memberStatusAndmemberListPublisherRunnable);
    }

    private void publishMemberStatusAndMemberListInformationData(final Long timestamp, final String applicationId,
            String clusterId, final String clusterAlias,
            final String memberId,
            final String status) {
        // Collect all data to process as a batch
        ArrayList<AnalyticsJDBCDataHolder> analyticsDataList = new ArrayList<>();

        // member status
        ArrayList<Object> memberStatusQueryData = new ArrayList<>();
        memberStatusQueryData.add(timestamp);
        memberStatusQueryData.add(applicationId);
        memberStatusQueryData.add(clusterAlias);
        memberStatusQueryData.add(memberId);
        memberStatusQueryData.add(status);

        analyticsDataList.add(new AnalyticsJDBCDataHolder(MEMBER_STATUS_INSERT_QUERY_SQL, memberStatusQueryData));

        // member list
        ArrayList<Object> memberListQueryData = new ArrayList<>();
        memberListQueryData.add(clusterId);
        memberListQueryData.add(memberId);
        memberListQueryData.add(status);

        analyticsDataList.add(new AnalyticsJDBCDataHolder(MEMBER_LIST_INSERT_QUERY_SQL, memberListQueryData));

        // insert member status and member list as a batch
        try {
            if (!analyticsDataList.isEmpty()) {
                AnalyticsJDBCManager.insertBatch(analyticsDataList);
            }
        } catch (Exception e) {
            AnalyticsJDBCManager.logBatchQueryData(analyticsDataList);
            log.error("Failed to publish member status and member list analytics event data", e);
        }
    }

    public boolean isEnabled() {
        return AnalyticsJDBCManager.isJDBCAnalyticsPublisherEnabled();
    }
}
