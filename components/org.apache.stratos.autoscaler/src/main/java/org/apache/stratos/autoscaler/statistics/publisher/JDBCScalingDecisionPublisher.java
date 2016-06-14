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
package org.apache.stratos.autoscaler.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.db.AnalyticsJDBCManager;
import org.apache.stratos.common.threading.StratosThreadPool;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import static org.apache.stratos.common.constants.StratosConstants.ANALYTICS_LOGGER_NAME;

class JDBCScalingDecisionPublisher extends ScalingDecisionPublisher {
    private static final String STATS_PUBLISHER_THREAD_POOL_ID = "scaling.decision.jdbc.stats.publisher.thread.pool";
    private static final Log analyticsLog = LogFactory.getLog(ANALYTICS_LOGGER_NAME);
    private static final Log log = LogFactory.getLog(JDBCScalingDecisionPublisher.class);
    private static final String SCALING_DECISION_INSERT_QUERY_SQL = "INSERT INTO SCALING_DETAILS " +
            "(Time, ScalingDecisionId, ClusterId, MinInstanceCount, MaxInstanceCount, RIFPredicted, " +
            "RIFThreshold, RIFRequiredInstances, MCPredicted, MCThreshold, MCRequiredInstances, " +
            "LAPredicted, LAThreshold, LARequiredInstances, RequiredInstanceCount, " +
            "ActiveInstanceCount, AdditionalInstanceCount, ScalingReason)" +
            " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final int STATS_PUBLISHER_THREAD_POOL_SIZE = 10;
    private static volatile JDBCScalingDecisionPublisher jdbcScalingDecisionPublisher;
    private ExecutorService executorService;

    private JDBCScalingDecisionPublisher() {
        executorService = StratosThreadPool
                .getExecutorService(STATS_PUBLISHER_THREAD_POOL_ID, STATS_PUBLISHER_THREAD_POOL_SIZE);
    }

    public static JDBCScalingDecisionPublisher getInstance() {
        if (jdbcScalingDecisionPublisher == null) {
            synchronized (JDBCScalingDecisionPublisher.class) {
                if (jdbcScalingDecisionPublisher == null) {
                    jdbcScalingDecisionPublisher = new JDBCScalingDecisionPublisher();
                }
            }
        }
        return jdbcScalingDecisionPublisher;
    }

    @Override public void publish(final Long timestamp, final String scalingDecisionId, final String clusterId,
            final int minInstanceCount, final int maxInstanceCount, final int rifPredicted, final int rifThreshold,
            final int rifRequiredInstances, final int mcPredicted, final int mcThreshold, final int mcRequiredInstances,
            final int laPredicted, final int laThreshold, final int laRequiredInstance, final int requiredInstanceCount,
            final int activeInstanceCount, final int additionalInstanceCount, final String scalingReason) {
        Runnable runnable = new Runnable() {
            @Override public void run() {
                ArrayList dataList = new ArrayList() {{
                    add(timestamp);
                    add(scalingDecisionId);
                    add(clusterId);
                    add(minInstanceCount);
                    add(maxInstanceCount);
                    add(rifPredicted);
                    add(rifThreshold);
                    add(rifRequiredInstances);
                    add(mcPredicted);
                    add(mcThreshold);
                    add(mcRequiredInstances);
                    add(laPredicted);
                    add(laThreshold);
                    add(laRequiredInstance);
                    add(requiredInstanceCount);
                    add(activeInstanceCount);
                    add(additionalInstanceCount);
                    add(scalingReason);
                }};
                try {
                    AnalyticsJDBCManager.insert(SCALING_DECISION_INSERT_QUERY_SQL, dataList.toArray());
                } catch (Exception e) {
                    analyticsLog.error(String
                            .format("PUBLISH FAILED [event-type] %s, [data] %s", "ScalingDecision", dataList));
                    log.error("Failed to publish ScalingDecision analytics event data.", e);
                }
            }
        };
        executorService.execute(runnable);
    }

    @Override public boolean isEnabled() {
        return AnalyticsJDBCManager.isJDBCAnalyticsPublisherEnabled();
    }
}
