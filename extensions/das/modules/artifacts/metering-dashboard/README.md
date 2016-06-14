# Apache Stratos Metering Dashboard

## This directory contains following artifacts:

(1) capps - Includes stratos-metering-service car file which bundles Gadgets and dashboard artifacts.

(2) jaggery-files

(3) ues-patch

## Follow the below steps to generate the metering dashboard:

1. Add jaggery files needed for your db type which can be found in
`<Stratos-DAS-Distribution>/metering-dashboard/jaggery-files/<db>` to DAS server path
`<DAS_HOME/repository/deployment/server/jaggeryapps/portal/controllers/apis/`

2. Create MySQL/Oracle database and tables using queries in
`<Stratos-DAS-Distribution>/metering-dashboard/database-scripts/<db>/metering-script.sql` manually.

3. Apply ues-patch files in `<Stratos-DAS-Distribution>/metering-dashboard/ues-patch/` to DAS as mentioned in its
README file.

4. Add stratos-metering-service car file in `<Stratos-DAS-Distribution>/metering-dashboard/` to
`<DAS-HOME>/repository/deployment/server/carbonapps/` to generate the metering dashboard.