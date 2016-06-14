/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.common.db;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.constants.StratosConstants;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.stratos.common.constants.StratosConstants.ANALYTICS_DB_JNDI_NAME_KEY;
import static org.apache.stratos.common.constants.StratosConstants.ANALYTICS_LOGGER_NAME;

/**
 * Publishes given events to stats databases
 */
public class AnalyticsJDBCManager {
    private static final Log analyticsLog = LogFactory.getLog(ANALYTICS_LOGGER_NAME);
    private static final Log log = LogFactory.getLog(AnalyticsJDBCManager.class);
    private static volatile DataSource analyticsDataSource;

    private static DataSource getAnalyticsDatasource() {
        if (analyticsDataSource != null) {
            return analyticsDataSource;
        }
        try {
            String analyticsDSJNDIName = System.getProperty(ANALYTICS_DB_JNDI_NAME_KEY, StratosConstants
                    .ANALYTICS_DB_JNDI_NAME);
            analyticsDataSource = InitialContext.doLookup(analyticsDSJNDIName);
            return analyticsDataSource;
        } catch (NameNotFoundException ignored) {
        } catch (Exception e) {
            log.error("Error when looking up analytics data source", e);
        }
        return null;
    }

    public static void insert(String insertQueryPrepatedStmtString, Object... data) {
        if (data == null) {
            log.warn("Data to be inserted is null. Skipping insert operation");
            return;
        }
        DataSource analyticsDataSource = getAnalyticsDatasource();
        PreparedStatement preparedStatement = null;
        Connection conn = null;
        if (analyticsDataSource == null) {
            String errMsg = String.format("Could not find analytics data source [jndi-name] %s. Analytics publishing " +
                    "to metering and monitoring dashboard will be disabled.", StratosConstants.ANALYTICS_DB_JNDI_NAME);
            log.error(errMsg);
            analyticsLog.error(String.format("INSERT FAILED [prepared-statement] %s, [data] %s",
                    insertQueryPrepatedStmtString, Arrays.asList(data)));
            return;
        }

        try {
            conn = analyticsDataSource.getConnection();
            preparedStatement = conn.prepareStatement(insertQueryPrepatedStmtString);
            for (int i = 0; i < data.length; i++) {
                preparedStatement.setObject(i + 1, data[i]);
            }
            preparedStatement.executeUpdate();
            // if autocommit = false, commit explicitly
            if (!conn.isClosed() && !conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (Exception e) {
            log.error("Could not execute INSERT statement", e);
            analyticsLog.error(String.format("INSERT FAILED [prepared-statement] %s, [data] %s",
                    insertQueryPrepatedStmtString, Arrays.asList(data)));
        } finally {
            // Closing statement
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    log.error("Error when closing prepared statement", e);
                }
            }

            // Closing connection
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("Error when closing JDBC connection", e);
                }
            }
        }
    }

    public static void insertBatch(final ArrayList<AnalyticsJDBCDataHolder> dataHolderList) {
        if (dataHolderList == null) {
            log.warn("Batch data to be inserted is null. Skipping batch insert operation");
            return;
        }
        DataSource analyticsDataSource = getAnalyticsDatasource();
        Connection conn = null;
        List<PreparedStatement> preparedStatements = new ArrayList<>();
        if (analyticsDataSource == null) {
            String errMsg = String.format("Could not find analytics data source [jndi-name] %s. Analytics publishing " +
                    "to metering and monitoring dashboard will be disabled.", StratosConstants.ANALYTICS_DB_JNDI_NAME);
            log.error(errMsg);
            logBatchQueryData(dataHolderList);
            return;
        }

        try {
            conn = analyticsDataSource.getConnection();
            // set auto commit to false
            conn.setAutoCommit(false);
            for (AnalyticsJDBCDataHolder dataHolder : dataHolderList) {
                PreparedStatement preparedStatement = conn.prepareStatement(dataHolder
                        .getInsertQueryPrepatedStmtString());
                // add to list
                preparedStatements.add(preparedStatement);
                for (int i = 0; i < dataHolder.getQueryData().size(); i++) {
                    preparedStatement.setObject(i + 1, dataHolder.getQueryData().get(i));
                }
                preparedStatement.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            rollback(conn);
            log.error("Could not execute BATCH INSERT statement", e);
            logBatchQueryData(dataHolderList);
        } finally {
            // Closing statements
            for (PreparedStatement stmt : preparedStatements) {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        log.error("Error when closing prepared statement", e);
                    }
                }
            }
            // Closing connection
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("Error when closing JDBC connection", e);
                }
            }
        }
    }

    private static void rollback(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            log.error("Error when rolling back", e);
        }
    }

    public static void logBatchQueryData(ArrayList<AnalyticsJDBCDataHolder> dataHolderList) {
        if (dataHolderList == null) {
            return;
        }
        for (AnalyticsJDBCDataHolder jdbcDataHolder : dataHolderList) {
            analyticsLog.error(String.format("BATCH INSERT FAILED [prepared-statement] %s, [data] %s",
                    jdbcDataHolder.getInsertQueryPrepatedStmtString(), jdbcDataHolder.getQueryData()));
        }
    }

    public static boolean isJDBCAnalyticsPublisherEnabled() {
        return getAnalyticsDatasource() != null;
    }
}
