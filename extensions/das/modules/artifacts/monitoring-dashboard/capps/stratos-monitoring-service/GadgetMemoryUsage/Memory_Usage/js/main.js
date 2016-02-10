/*
 *
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
var type, columns, maxUpdateValue;

var REFRESH_INTERVAL = 5000;
var dataLoaded = true;
var timeInterval = '30 Min';
var clusterId = '';
var memberId = '';

//loading gadget configuration
type = gadgetConfig.type;
var counter = 0;
maxUpdateValue = gadgetConfig.maxUpdateValue;

gadgets.HubSettings.onConnect = function () {
    gadgets.Hub.subscribe('health-stats-filter', function (topic, data) {
        clusterId = data['clusterId'];
        memberId = data['memberId'];
        timeInterval = data['timeInterval'];
        console.log("Health Stats Filter Value:" + JSON.stringify(data));
        fetchData(drawChart);
    });
};

//first, fetch datasource schema
getColumns();

//load data immediately
fetchData(drawChart);

// then start periodic polling
setInterval(function () {
    fetchData(drawChart);
}, REFRESH_INTERVAL);


function getColumns() {
    columns = gadgetConfig.columns;
}

function fetchData(callback) {
    //if previous operation is not completed, DO NOT fetch data
    if (!dataLoaded) {
        console.log("Waiting for data...");
        return;
    }

    var cluster = clusterId;
    var member = memberId;
    var time = timeInterval;

    if (cluster != "") {
        var request = {
            clusterId: cluster,
            memberId: member,
            time: time
        };

        jQuery("#placeholder").html("");
        jQuery("#placeholder").append('<div id="noChart"><table><tr><td><b><p><br/>Updating</p></b></td></tr></table></div>');

        $.ajax({
            url: "/portal/apis/memory-usage",
            method: "GET",
            data: request,
            contentType: "application/json",
            success: function (data) {
                if (callback != null) {
                    callback(makeRows(JSON.parse(data)));
                }
            }
        });
        dataLoaded = false;   //setting the latch to locked position so that we block data fetching until we receive the response from backend
    }
}

function makeDataTable(data) {
    var dataTable = new igviz.DataTable();
    if (columns.length > 0) {
        columns.forEach(function (column) {
            var type = "N";
            if (column.DATA_TYPE == "varchar" || column.DATA_TYPE == "VARCHAR") {
                type = "C";
            } else if (column.DATA_TYPE == "TIME" || column.DATA_TYPE == "time") {
                type = "T";
            }
            dataTable.addColumn(column.COLUMN_NAME, type);
        });
    }
    data.forEach(function (row, index) {
        for (var i = 0; i < row.length; i++) {
            if (dataTable.metadata.types[i] == "N") {
                data[index][i] = parseInt(data[index][i]);
            }
        }
    });
    dataTable.addRows(data);
    return dataTable;
}

function makeRows(data) {
    var rows = [];
    for (var i = 0; i < data.length; i++) {
        var record = data[i];
        var row = columns.map(function (column) {
            return record[column.COLUMN_NAME];
        });
        rows.push(row);
    }
    return rows;
}

function drawChart(data) {
    var dataTable = makeDataTable(data);
    if (dataTable.data.length != 0) {
        gadgetConfig.chartConfig.width = $("#placeholder").width();
        gadgetConfig.chartConfig.height = $("#placeholder").height() - 65;
        var chartType = gadgetConfig.chartConfig.chartType;
        var xAxis = gadgetConfig.chartConfig.xAxis;
        var chart;
        jQuery("#noChart").html("");
        if (chartType === "bar" && dataTable.metadata.types[xAxis] === "N") {
            dataTable.metadata.types[xAxis] = "C";
        }

        if (gadgetConfig.chartConfig.chartType === "tabular" || gadgetConfig.chartConfig.chartType === "singleNumber") {
            gadgetConfig.chartConfig.height = $("#placeholder").height();
            chart = igviz.draw("#placeholder", gadgetConfig.chartConfig, dataTable);
            chart.plot(dataTable.data);

        } else {
            chart = igviz.setUp("#placeholder", gadgetConfig.chartConfig, dataTable);
            chart.setXAxis({
                "labelAngle": -35,
                "labelAlign": "right",
                "labelDy": 0,
                "labelDx": 0,
                "titleDy": 25
            })
                .setYAxis({
                    "titleDy": -30
                });
            chart.plot(dataTable.data);
        }
    } else {
        jQuery("#placeholder").html("");
        jQuery("#placeholder").append('<div id="noChart"><table><tr><td style="padding:30px 20px 0px 20px"><img src="../../portal/images/noEvents.png" align="left" style="width:24;height:24"/></td><td><br/><b><p><br/>Data is not available for selected cluster, member and time interval</p></b></td></tr></table></div>');
    }
    //releasing the latch so that we can request data again from the backend.
    dataLoaded = true;
}


