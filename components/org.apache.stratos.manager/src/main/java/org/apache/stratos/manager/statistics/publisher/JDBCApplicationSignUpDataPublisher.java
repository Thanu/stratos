package org.apache.stratos.manager.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.db.AnalyticsJDBCManager;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.manager.utils.StratosManagerConstants;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import static org.apache.stratos.common.constants.StratosConstants.ANALYTICS_LOGGER_NAME;

class JDBCApplicationSignUpDataPublisher extends ApplicationSignUpDataPublisher {
    private static final Log analyticsLog = LogFactory.getLog(ANALYTICS_LOGGER_NAME);
    private static final Log log = LogFactory.getLog(JDBCApplicationSignUpDataPublisher.class);
    private static final String APPLICATION_SIGN_UP_DATA_INSERT_QUERY_SQL = "INSERT INTO APPLICATION_USAGE "
            + "(ApplicationId, TenantId, TenantDomain, StartTime, EndTime, Duration) VALUES(?,?,?,?,?,?)";
    private static volatile JDBCApplicationSignUpDataPublisher jdbcApplicationSignUpDataPublisher;
    private ExecutorService executorService;

    private JDBCApplicationSignUpDataPublisher() {
        executorService = StratosThreadPool.getExecutorService(StratosManagerConstants.STATS_PUBLISHER_THREAD_POOL_ID,
                StratosManagerConstants.STATS_PUBLISHER_THREAD_POOL_SIZE);
    }

    public static JDBCApplicationSignUpDataPublisher getInstance() {
        if (jdbcApplicationSignUpDataPublisher == null) {
            synchronized (JDBCApplicationSignUpDataPublisher.class) {
                if (jdbcApplicationSignUpDataPublisher == null) {
                    jdbcApplicationSignUpDataPublisher = new JDBCApplicationSignUpDataPublisher();
                }
            }
        }
        return jdbcApplicationSignUpDataPublisher;
    }

    @Override
    public void publish(final String applicationId, final int tenantId, final String tenantDomain,
            final long startTime, final long endTime, final long duration) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ArrayList dataList = null;

                dataList = new ArrayList() {{
                    add(applicationId);
                    add(tenantId);
                    add(tenantDomain);
                    add(startTime);
                    add(endTime);
                    add(duration);
                }};
                try {
                    AnalyticsJDBCManager.insert(APPLICATION_SIGN_UP_DATA_INSERT_QUERY_SQL, dataList.toArray());
                } catch (Exception e) {
                    analyticsLog.error(String
                            .format("PUBLISH FAILED [event-type] %s, [data] %s", "MemberInformation", dataList));
                    log.error("Failed to publish MemberInfo analytics event data.", e);
                }
            }
        };
        executorService.execute(runnable);
    }

    private String getDefaultIfNotDefined(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    public boolean isEnabled() {
        return AnalyticsJDBCManager.isJDBCAnalyticsPublisherEnabled();
    }
}
