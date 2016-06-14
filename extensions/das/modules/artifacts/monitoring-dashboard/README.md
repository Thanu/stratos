# Apache Stratos Monitoring Dashboard

## This directory contains following artifacts:
(1) capps - Includes stratos-monitoring-service car file which bundles all Event Stream, Event receiver, Even Store,
Gadgets, SparkScripts and Dashboard artifacts.

(2) jaggery-files

## Follow the below steps to generate the monitoring dashboard:

1. Add jaggery files needed for your database type which can be found in
`<Stratos-DAS-Distribution>/monitoring-dashboard/jaggery-files/<db>/` to DAS server path
`<DAS_HOME/repository/deployment/server/jaggeryapps/portal/controllers/apis/`.

2. Create MySQL/Oracle database and tables using queries in
`<Stratos-DAS-Distribution>/monitoring-dashboard/database-scripts/<db>/monitoring-script.sql` manually.

3. Add stratos-monitoring-service car file in `<Stratos-DAS-Distribution>/monitoring-dashboard/` to
`<DAS-HOME>/repository/deployment/server/carbonapps/` to generate the monitoring dashboard.
