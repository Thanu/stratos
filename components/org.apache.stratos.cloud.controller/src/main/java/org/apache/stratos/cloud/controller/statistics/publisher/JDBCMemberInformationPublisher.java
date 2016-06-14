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
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.InstanceMetadata;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.common.db.AnalyticsJDBCManager;
import org.apache.stratos.common.threading.StratosThreadPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import static org.apache.stratos.common.constants.StratosConstants.ANALYTICS_LOGGER_NAME;

class JDBCMemberInformationPublisher extends MemberInformationPublisher {
    private static final String STATS_PUBLISHER_THREAD_POOL_ID = "member.information.jdbc.stats.publisher.thread.pool";
    private static final int STATS_PUBLISHER_THREAD_POOL_SIZE = 10;
    private static final Log analyticsLog = LogFactory.getLog(ANALYTICS_LOGGER_NAME);
    private static final Log log = LogFactory.getLog(JDBCMemberInformationPublisher.class);
    private static final String MEMBER_INFORMATION_INSERT_QUERY_SQL = "INSERT INTO MEMBER_INFORMATION " +
            "(MemberId, InstanceType, ImageId, HostName, PrivateIPAddresses, PublicIPAddresses, " +
            "Hypervisor, CPU, RAM, OSName, OSVersion)" +
            " VALUES(?,?,?,?,?,?,?,?,?,?,?)";
    private static volatile JDBCMemberInformationPublisher jdbcMemberInformationPublisher;
    private ExecutorService executorService;

    private JDBCMemberInformationPublisher() {
        executorService = StratosThreadPool.getExecutorService(STATS_PUBLISHER_THREAD_POOL_ID,
                STATS_PUBLISHER_THREAD_POOL_SIZE);
    }

    public static JDBCMemberInformationPublisher getInstance() {
        if (jdbcMemberInformationPublisher == null) {
            synchronized (JDBCMemberInformationPublisher.class) {
                if (jdbcMemberInformationPublisher == null) {
                    jdbcMemberInformationPublisher = new JDBCMemberInformationPublisher();
                }
            }
        }
        return jdbcMemberInformationPublisher;
    }

    @Override
    public void publish(final String memberId, String scalingDecisionId, final InstanceMetadata metadata) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final MemberContext memberContext = CloudControllerContext.getInstance().getMemberContextOfMemberId
                        (memberId);
                String cartridgeType = memberContext.getCartridgeType();
                Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
                IaasProvider iaasProvider = CloudControllerContext.getInstance().getIaasProviderOfPartition(
                        cartridge.getType(), memberContext.getPartition().getId());
                final String instanceType = iaasProvider.getProperty(CloudControllerConstants.INSTANCE_TYPE);
                ArrayList dataList = null;
                if (metadata == null) {
                    dataList = new ArrayList() {{
                        add(memberId);
                        add(instanceType);
                        add(null);
                        add(null);
                        add(Arrays.toString(memberContext.getPrivateIPs()));
                        add(Arrays.toString(memberContext.getPublicIPs()));
                        add(null);
                        add(null);
                        add(null);
                        add(null);
                        add(null);
                    }};
                } else {
                    dataList = new ArrayList() {{
                        add(memberId);
                        add(instanceType);
                        add(getDefaultIfNotDefined(metadata.getImageId()));
                        add(getDefaultIfNotDefined(metadata.getHostname()));
                        add(Arrays.toString(memberContext.getPrivateIPs()));
                        add(Arrays.toString(memberContext.getPublicIPs()));
                        add(getDefaultIfNotDefined(metadata.getHypervisor()));
                        add(getDefaultIfNotDefined(metadata.getCpu()));
                        add(getDefaultIfNotDefined(metadata.getRam()));
                        add(getDefaultIfNotDefined(metadata.getOperatingSystemName()));
                        add(getDefaultIfNotDefined(metadata.getOperatingSystemVersion()));
                    }};
                }
                try {
                    AnalyticsJDBCManager.insert(MEMBER_INFORMATION_INSERT_QUERY_SQL, dataList.toArray());
                } catch (Exception e) {
                    analyticsLog.error(String.format("PUBLISH FAILED [event-type] %s, [data] %s",
                            "MemberInformation", dataList));
                    log.error("Failed to publish MemberInfo analytics event data.", e);
                }
            }
        };
        executorService.execute(runnable);
    }

    private String getDefaultIfNotDefined (String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    public boolean isEnabled() {
        return AnalyticsJDBCManager.isJDBCAnalyticsPublisherEnabled();
    }
}
