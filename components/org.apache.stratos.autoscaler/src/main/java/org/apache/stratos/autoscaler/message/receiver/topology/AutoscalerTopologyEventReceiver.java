/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.message.receiver.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.topic.ApplicationsEventPublisher;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.grouping.topic.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.grouping.topic.InstanceNotificationPublisher;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitorFactory;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.VMClusterMonitor;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.applications.Applications;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.applications.ApplicationUndeployedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterCreatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterInActivateEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterRemovedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterResetEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterTerminatedEventListener;
import org.apache.stratos.messaging.listener.topology.ClusterTerminatingEventListener;
import org.apache.stratos.messaging.listener.topology.CompleteTopologyEventListener;
import org.apache.stratos.messaging.listener.topology.MemberActivatedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberMaintenanceListener;
import org.apache.stratos.messaging.listener.topology.MemberReadyToShutdownEventListener;
import org.apache.stratos.messaging.listener.topology.MemberStartedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberTerminatedEventListener;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

import java.util.Set;

/**
 * Autoscaler topology receiver.
 */
public class AutoscalerTopologyEventReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(AutoscalerTopologyEventReceiver.class);

    private TopologyEventReceiver topologyEventReceiver;
    private boolean terminated;
    private boolean topologyInitialized;

    public AutoscalerTopologyEventReceiver() {
        this.topologyEventReceiver = new TopologyEventReceiver();
        addEventListeners();
    }

    @Override
    public void run() {
        //FIXME this activated before autoscaler deployer activated.
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ignore) {
        }
        Thread thread = new Thread(topologyEventReceiver);
        thread.start();
        if (log.isInfoEnabled()) {
            log.info("Autoscaler topology receiver thread started");
        }

        // Keep the thread live until terminated
        while (!terminated) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        if (log.isInfoEnabled()) {
            log.info("Autoscaler topology receiver thread terminated");
        }
    }

    private boolean allClustersInitialized(Application application) {
        boolean allClustersInitialized = false;
        for (ClusterDataHolder holder : application.getClusterDataMap().values()) {
            TopologyManager.acquireReadLockForCluster(holder.getServiceType(),
                    holder.getClusterId());

            try {
                Topology topology = TopologyManager.getTopology();
                if (topology != null) {
                    Service service = topology.getService(holder.getServiceType());
                    if (service != null) {
                        if (service.clusterExists(holder.getClusterId())) {
                            allClustersInitialized = true;
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("[Cluster] " + holder.getClusterId() + " is not found in " +
                                        "the Topology");
                            }
                            allClustersInitialized = false;
                            return allClustersInitialized;
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Service is null in the CompleteTopologyEvent");
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Topology is null in the CompleteTopologyEvent");
                    }
                }
            } finally {
                TopologyManager.releaseReadLockForCluster(holder.getServiceType(),
                        holder.getClusterId());
            }
        }
        return allClustersInitialized;
    }


    private void addEventListeners() {
        // Listen to topology events that affect clusters
        topologyEventReceiver.addEventListener(new CompleteTopologyEventListener() {
            @Override
            protected void onEvent(Event event) {
                if (!topologyInitialized) {
                    log.info("[CompleteTopologyEvent] Received: " + event.getClass());
                    ApplicationHolder.acquireReadLock();
                    try {
                        Applications applications = ApplicationHolder.getApplications();
                        if (applications != null) {
                            for (Application application : applications.getApplications().values()) {
                                if (allClustersInitialized(application)) {
                                    startApplicationMonitor(application.getUniqueIdentifier());
                                } else {
                                    log.error("Complete Topology is not consistent with the applications " +
                                            "which got persisted");
                                }
                            }
                            topologyInitialized = true;
                        } else {
                            log.info("No applications found in the complete topology");
                        }
                    } catch (Exception e) {
                        log.error("Error processing event", e);
                    } finally {
                        ApplicationHolder.releaseReadLock();
                    }
                }
            }
        });


        topologyEventReceiver.addEventListener(new ApplicationClustersCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    log.info("[ApplicationClustersCreatedEvent] Received: " + event.getClass());
                    ApplicationClustersCreatedEvent applicationClustersCreatedEvent =
                            (ApplicationClustersCreatedEvent) event;
                    String appId = applicationClustersCreatedEvent.getAppId();
                    try {
                        //acquire read lock
                        ApplicationHolder.acquireReadLock();
                        //start the application monitor
                        startApplicationMonitor(appId);
                    } catch (Exception e) {
                        String msg = "Error processing event " + e.getLocalizedMessage();
                        log.error(msg, e);
                    } finally {
                        //release read lock
                        ApplicationHolder.releaseReadLock();

                    }
                } catch (ClassCastException e) {
                    String msg = "Error while casting the event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }

            }
        });

        topologyEventReceiver.addEventListener(new ClusterActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterActivatedEvent] Received: " + event.getClass());

                ClusterActivatedEvent clusterActivatedEvent = (ClusterActivatedEvent) event;
                String clusterId = clusterActivatedEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                if (clusterMonitor != null) {
                    clusterMonitor.setStatus(ClusterStatus.Active);
                }

            }
        });

        topologyEventReceiver.addEventListener(new ClusterResetEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterCreatedEvent] Received: " + event.getClass());

                ClusterCreatedEvent clusterCreatedEvent = (ClusterCreatedEvent) event;
                String clusterId = clusterCreatedEvent.getCluster().getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                clusterMonitor.setStop(true);
                clusterMonitor.setStatus(ClusterStatus.Created);

            }
        });

        topologyEventReceiver.addEventListener(new ClusterCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterCreatedEvent] Received: " + event.getClass());
            }
        });

        topologyEventReceiver.addEventListener(new ClusterInActivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterInActivateEvent] Received: " + event.getClass());

                ClusterInactivateEvent clusterInactivateEvent = (ClusterInactivateEvent) event;
                String clusterId = clusterInactivateEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                if (clusterMonitor != null) {
                    clusterMonitor.setStatus(ClusterStatus.Inactive);
                }

            }
        });

        topologyEventReceiver.addEventListener(new ClusterTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ClusterTerminatingEvent] Received: " + event.getClass());

                ClusterTerminatingEvent clusterTerminatingEvent = (ClusterTerminatingEvent) event;
                String clusterId = clusterTerminatingEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                if (clusterMonitor != null) {
                    if (clusterMonitor.getStatus() == ClusterStatus.Active) {
                        // terminated gracefully
                        clusterMonitor.setStatus(ClusterStatus.Terminating);
                        InstanceNotificationPublisher.sendInstanceCleanupEventForCluster(clusterId);
                    } else {
                        clusterMonitor.setStatus(ClusterStatus.Terminating);
                        clusterMonitor.terminateAllMembers();
                    }

                } else {
                    log.warn("No Cluster Monitor found for cluster id " + clusterId);
                }
            }
        });

        topologyEventReceiver.addEventListener(new ClusterTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                log.info("[ClusterTerminatedEvent] Received: " + event.getClass());

                ClusterTerminatedEvent clusterTerminatedEvent = (ClusterTerminatedEvent) event;
                String clusterId = clusterTerminatedEvent.getClusterId();
                AbstractClusterMonitor clusterMonitor =
                        AutoscalerContext.getInstance().getClusterMonitor(clusterId);

                //changing the status in the monitor, will notify its parent monitor
                if (clusterMonitor != null) {
                    clusterMonitor.setStatus(ClusterStatus.Terminated);
                }
            }
        });

        topologyEventReceiver.addEventListener(new ApplicationUndeployedEventListener() {
            @Override
            protected void onEvent(Event event) {

                log.info("[ApplicationUndeployedEvent] Received: " + event.getClass());

                ApplicationUndeployedEvent applicationUndeployedEvent = (ApplicationUndeployedEvent) event;

                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().
                        getAppMonitor(applicationUndeployedEvent.getApplicationId());

                // if any of Cluster Monitors are not added yet, should send the
                // Cluster Terminated event for those clusters
                Set<ClusterDataHolder> clusterDataHolders = applicationUndeployedEvent.getClusterData();
                if (clusterDataHolders != null) {
                    for (ClusterDataHolder clusterDataHolder : clusterDataHolders) {
                        VMClusterMonitor clusterMonitor =
                                ((VMClusterMonitor) AutoscalerContext.getInstance().getClusterMonitor(clusterDataHolder.getClusterId()));
                        if (clusterMonitor == null) {
                            // Cluster Monitor not found; send Cluster Terminated event to cleanup
                            ClusterStatusEventPublisher.sendClusterTerminatedEvent(
                                    applicationUndeployedEvent.getApplicationId(),
                                    clusterDataHolder.getServiceType(),
                                    clusterDataHolder.getClusterId());
                        } else {
                            // if the Cluster Monitor exists, mark it as destroyed to stop it from spawning
                            // more instances
                            clusterMonitor.setDestroyed(true);
                        }
                    }
                }

                if (appMonitor != null) {
                    // set Application Monitor state to 'Terminating'
                    appMonitor.setStatus(ApplicationStatus.Terminating);

                } else {
                    // ApplicationMonitor is not found, send Terminating event to clean up
                    ApplicationsEventPublisher.sendApplicationTerminatedEvent(
                            applicationUndeployedEvent.getApplicationId(), applicationUndeployedEvent.getClusterData());
                }

//                ApplicationUndeployedEvent applicationUndeployedEvent = (ApplicationUndeployedEvent) event;
//
//                // acquire reead locks for application and relevant clusters
//                TopologyManager.acquireReadLockForApplication(applicationUndeployedEvent.getApplicationId());
//                Set<ClusterDataHolder> clusterDataHolders = applicationUndeployedEvent.getClusterData();
//                if (clusterDataHolders != null) {
//                    for (ClusterDataHolder clusterData : clusterDataHolders) {
//                        TopologyManager.acquireReadLockForCluster(clusterData.getServiceType(),
//                                clusterData.getClusterId());
//                    }
//                }
//
//                try {
//                    ApplicationMonitor appMonitor = AutoscalerContext.getInstance().
//                            getAppMonitor(applicationUndeployedEvent.getApplicationId());
//
//                    if (appMonitor != null) {
//                        // update the status as Terminating
//                        appMonitor.setStatus(ApplicationStatus.Terminating);
//
////                        List<String> clusters = appMonitor.
////                                findClustersOfApplication(applicationUndeployedEvent.getApplicationId());
//
//                        boolean clusterMonitorsFound = false;
//                        for (ClusterDataHolder clusterData : clusterDataHolders) {
//                            //stopping the cluster monitor and remove it from the AS
//                            ClusterMonitor clusterMonitor =
//                                    ((ClusterMonitor) AutoscalerContext.getInstance().getMonitor(clusterData.getClusterId()));
//                            if (clusterMonitor != null) {
//                                clusterMonitorsFound = true;
//                                clusterMonitor.setDestroyed(true);
//                                //clusterMonitor.terminateAllMembers();
//                                if (clusterMonitor.getStatus() == ClusterStatus.Active) {
//                                    // terminated gracefully
//                                    clusterMonitor.setStatus(ClusterStatus.Terminating);
//                                    InstanceNotificationPublisher.sendInstanceCleanupEventForCluster(clusterData.getClusterId());
//                                } else {
//                                    // if not active, forcefully terminate
//                                    clusterMonitor.setStatus(ClusterStatus.Terminating);
//                                    clusterMonitor.terminateAllMembers();
////                                    try {
////                                        // TODO: introduce a task to do this cleanup
////                                        CloudControllerClient.getInstance().terminateAllInstances(clusterData.getClusterId());
////                                    } catch (TerminationException e) {
////                                        log.error("Unable to terminate instances for [ cluster id ] " +
////                                                clusterData.getClusterId(), e);
////                                    }
//                                }
//                            } else {
//                                log.warn("No Cluster Monitor found for cluster id " + clusterData.getClusterId());
//                                // if Cluster Monitor is not found, still the Cluster Terminated
//                                // should be sent to update the parent Monitor
//                                StatusEventPublisher.sendClusterTerminatedEvent(
//                                        applicationUndeployedEvent.getApplicationId(),
//                                        clusterData.getServiceType(), clusterData.getClusterId());
//                            }
//                        }
//
//                        // if by any chance, the cluster monitors have failed, we still need to undeploy this application
//                        // hence, check if the Cluster Monitors are not found and send the Application Terminated event
//                        if (!clusterMonitorsFound) {
//                            StatusEventPublisher.sendApplicationTerminatedEvent(
//                                    applicationUndeployedEvent.getApplicationId(), clusterDataHolders);
//                        }
//
//                    } else {
//                        log.warn("Application Monitor cannot be found for the undeployed [application] "
//                                + applicationUndeployedEvent.getApplicationId());
//                        // send the App Terminated event to cleanup
//                        StatusEventPublisher.sendApplicationTerminatedEvent(
//                                applicationUndeployedEvent.getApplicationId(), clusterDataHolders);
//                    }
//
//                } finally {
//                    if (clusterDataHolders != null) {
//                        for (ClusterDataHolder clusterData : clusterDataHolders) {
//                            TopologyManager.releaseReadLockForCluster(clusterData.getServiceType(),
//                                    clusterData.getClusterId());
//                        }
//                    }
//                    TopologyManager.
//                            releaseReadLockForApplication(applicationUndeployedEvent.getApplicationId());
//                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberReadyToShutdownEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    MemberReadyToShutdownEvent memberReadyToShutdownEvent = (MemberReadyToShutdownEvent) event;
                    String clusterId = memberReadyToShutdownEvent.getClusterId();
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    AbstractClusterMonitor monitor;
                    monitor = asCtx.getClusterMonitor(clusterId);
                    if (null == monitor) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                    + "[cluster] %s", clusterId));
                        }
                        return;
                    }
                    monitor.handleMemberReadyToShutdownEvent(memberReadyToShutdownEvent);
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }
            }
        });
//TODO delete this if we don't want this
//        topologyEventReceiver.addEventListener(new ClusterRemovedEventListener() {
//            @Override
//            protected void onEvent(Event event) {
//
//                ClusterRemovedEvent clusterRemovedEvent = null;
//                try {
//                    clusterRemovedEvent = (ClusterRemovedEvent) event;
//                    //TopologyManager.acquireReadLock();
//                    TopologyManager.acquireReadLockForCluster(clusterRemovedEvent.getServiceName(),
//                            clusterRemovedEvent.getClusterId());
//
//                    String clusterId = clusterRemovedEvent.getClusterId();
//                    String deploymentPolicy = clusterRemovedEvent.getDeploymentPolicy();
//
//                    AbstractClusterMonitor monitor;
//
//                    if (clusterRemovedEvent.isLbCluster()) {
//                        DeploymentPolicy depPolicy = PolicyManager.getInstance().
//                                getDeploymentPolicy(deploymentPolicy);
//                        if (depPolicy != null) {
//                            List<NetworkPartitionLbHolder> lbHolders = PartitionManager.getInstance()
//                                    .getNetworkPartitionLbHolders(depPolicy);
//
//                            for (NetworkPartitionLbHolder networkPartitionLbHolder : lbHolders) {
//                                // removes lb cluster ids
//                                boolean isRemoved = networkPartitionLbHolder.removeLbClusterId(clusterId);
//                                if (isRemoved) {
//                                    log.info("Removed the lb cluster [id]:"
//                                            + clusterId
//                                            + " reference from Network Partition [id]: "
//                                            + networkPartitionLbHolder
//                                            .getNetworkPartitionId());
//
//                                }
//                                if (log.isDebugEnabled()) {
//                                    log.debug(networkPartitionLbHolder);
//                                }
//
//                            }
//                        }
//                        monitor = AutoscalerContext.getInstance()
//                                .removeLbMonitor(clusterId);
//
//                    } else {
//                        monitor = (AbstractClusterMonitor) AutoscalerContext.getInstance()
//                                .removeMonitor(clusterId);
//                    }
//
//                    // runTerminateAllRule(monitor);
//                    if (monitor != null) {
//                        monitor.destroy();
//                        log.info(String.format("Cluster monitor has been removed successfully: [cluster] %s ",
//                                clusterId));
//                    }
//                } catch (Exception e) {
//                    log.error("Error processing event", e);
//                } finally {
//                    //TopologyManager.releaseReadLock();
//                    TopologyManager.releaseReadLockForCluster(clusterRemovedEvent.getServiceName(),
//                            clusterRemovedEvent.getClusterId());
//                }
//            }
//
//        });

        topologyEventReceiver.addEventListener(new MemberStartedEventListener() {
            @Override
            protected void onEvent(Event event) {

            }

        });

        topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;
                    String clusterId = memberTerminatedEvent.getClusterId();
                    AbstractClusterMonitor monitor;
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    monitor = asCtx.getClusterMonitor(clusterId);
                    if (null == monitor) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                    + "[cluster] %s", clusterId));
                        }
                        return;
                    }
                    monitor.handleMemberTerminatedEvent(memberTerminatedEvent);
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;
                    String clusterId = memberActivatedEvent.getClusterId();
                    AbstractClusterMonitor monitor;
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    monitor = asCtx.getClusterMonitor(clusterId);
                    if (null == monitor) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                    + "[cluster] %s", clusterId));
                        }
                        return;
                    }
                    monitor.handleMemberActivatedEvent(memberActivatedEvent);
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }
            }
        });

        topologyEventReceiver.addEventListener(new MemberMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {
                try {
                    MemberMaintenanceModeEvent maintenanceModeEvent = (MemberMaintenanceModeEvent) event;
                    String clusterId = maintenanceModeEvent.getClusterId();
                    AbstractClusterMonitor monitor;
                    AutoscalerContext asCtx = AutoscalerContext.getInstance();
                    monitor = asCtx.getClusterMonitor(clusterId);
                    if (null == monitor) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                    + "[cluster] %s", clusterId));
                        }
                        return;
                    }
                    monitor.handleMemberMaintenanceModeEvent(maintenanceModeEvent);
                } catch (Exception e) {
                    String msg = "Error processing event " + e.getLocalizedMessage();
                    log.error(msg, e);
                }
            }
        });
    }

    @SuppressWarnings("unused")
    private void runTerminateAllRule(VMClusterMonitor monitor) {

        FactHandle terminateAllFactHandle = null;

        StatefulKnowledgeSession terminateAllKnowledgeSession = null;

        for (NetworkPartitionContext networkPartitionContext : monitor.getNetworkPartitionCtxts().values()) {
            terminateAllFactHandle = AutoscalerRuleEvaluator.evaluateTerminateAll(terminateAllKnowledgeSession
                    , terminateAllFactHandle, networkPartitionContext);
        }

    }

    /**
     * Terminate load balancer topology receiver thread.
     */
    public void terminate() {
        topologyEventReceiver.terminate();
        terminated = true;
    }

    protected synchronized void startApplicationMonitor(String applicationId) {
        Thread th = null;
        if (!AutoscalerContext.getInstance().appMonitorExist(applicationId)) {
            th = new Thread(
                    new ApplicationMonitorAdder(applicationId));
        }

        if (th != null) {
            th.start();
            //    try {
            //        th.join();
            //    } catch (InterruptedException ignore) {

            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Application monitor thread has been started successfully: " +
                                "[application] %s ", applicationId));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Application monitor thread already exists: " +
                                "[application] %s ", applicationId));
            }
        }
    }

    private class ApplicationMonitorAdder implements Runnable {
        private String appId;

        public ApplicationMonitorAdder(String appId) {
            this.appId = appId;
        }

        public void run() {
            ApplicationMonitor applicationMonitor = null;
            int retries = 5;
            boolean success = false;
            do {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
                try {
                    long start = System.currentTimeMillis();
                    if (log.isDebugEnabled()) {
                        log.debug("application monitor is going to be started for [application] " +
                                appId);
                    }
                    applicationMonitor = ApplicationMonitorFactory.getApplicationMonitor(appId);

                    long end = System.currentTimeMillis();
                    log.info("Time taken to start app monitor: " + (end - start) / 1000);
                    success = true;
                } catch (DependencyBuilderException e) {
                    String msg = "Application monitor creation failed for Application: ";
                    log.warn(msg, e);
                    retries--;
                } catch (TopologyInConsistentException e) {
                    String msg = "Application monitor creation failed for Application: ";
                    log.warn(msg, e);
                    retries--;
                }
            } while (!success && retries != 0);

            if (applicationMonitor == null) {
                String msg = "Application monitor creation failed, even after retrying for 5 times, "
                        + "for Application: " + appId;
                log.error(msg);
                throw new RuntimeException(msg);
            }

            AutoscalerContext.getInstance().addAppMonitor(applicationMonitor);

            if (log.isInfoEnabled()) {
                log.info(String.format("Application monitor has been added successfully: " +
                        "[application] %s", applicationMonitor.getId()));
            }
        }
    }


}
