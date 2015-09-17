var datasource, type, columns, filter, maxUpdateValue;

var REFRESH_INTERVAL = 5000;
var dataLoaded = true;
var timeInterval = '30 Min';
var applicationId = '';
var clusterId = '';

//loading gadget configuration
datasource = gadgetConfig.datasource;
filter = gadgetConfig.filter;
type = gadgetConfig.type;
var counter = 0;
maxUpdateValue = gadgetConfig.maxUpdateValue;

//if gadget type is realtime, treat it different!
if (type === "realtime") {
    columns = gadgetConfig.columns;
    //subscribe to websocket
    subscribe(datasource.split(":")[0], datasource.split(":")[1], '10', gadgetConfig.domain,
        onRealTimeEventSuccessRecieval, onRealTimeEventErrorRecieval, location.hostname, location.port,
        'WEBSOCKET', "SECURED");
} else {
    //first, fetch datasource schema
    getColumns(datasource);

    //load data immediately
    fetchData(drawChart);

    // then start periodic polling
    setInterval(function () {
        fetchData(drawChart);
    }, REFRESH_INTERVAL);
}

function getColumns(table) {
    columns = gadgetConfig.columns;
};

function parseColumns(data) {
    if (data) {
        var keys = Object.getOwnPropertyNames(data);
        var columns = keys.map(function (key, i) {
            return column = {
                name: key,
                type: data.columns[key].type
            };
        });
        return columns;
    }
};

function fetchData(callback) {
    //if previous operation is not completed, DO NOT fetch data
    if (!dataLoaded) {
        console.log("Waiting for data...");
        return;
    }

    var application = applicationId;
    var cluster = clusterId;
    var time= timeInterval;

    console.log("ApplicationId:"+application);
    console.log("ClusterId:"+cluster);
    console.log("Time interval:"+timeInterval);

    var request = {
        type: 2,
        tableName: datasource,
        applicationId: application,
        clusterId: cluster,
        time: time
    };
    $.ajax({
        url: "/portal/apis/member-count",
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
};

function makeDataTable(data) {
    var dataTable = new igviz.DataTable();
    if (columns.length > 0) {
        columns.forEach(function (column, i) {
            var type = "N";
            if (column.DATA_TYPE == "varchar" || column.DATA_TYPE == "VARCHAR") {
                type = "C";
            } else if (column.DATA_TYPE == "TIME" || column.DATA_TYPE == "time") {
                type = "T";
            }
            dataTable.addColumn(column.COLUMN_NAME,type);
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
};

function makeRows(data) {
    var rows = [];
    for (var i = 0; i < data.length; i++) {
        var record = data[i];
        var row = columns.map(function (column, i) {
            return record[column.COLUMN_NAME];
        });
        rows.push(row);
    }
    return rows;
};

function drawChart(data) {
    var dataTable = makeDataTable(data);
    gadgetConfig.chartConfig.width = $("#placeholder").width();
    gadgetConfig.chartConfig.height = $("#placeholder").height() - 65;
    var chartType = gadgetConfig.chartConfig.chartType;
    var xAxis = gadgetConfig.chartConfig.xAxis;
    jQuery("#noChart").html("");
    if (chartType === "bar" && dataTable.metadata.types[xAxis] === "N") {
        dataTable.metadata.types[xAxis] = "C";
    }

    if (gadgetConfig.chartConfig.chartType === "tabular" || gadgetConfig.chartConfig.chartType === "singleNumber") {
        gadgetConfig.chartConfig.height = $("#placeholder").height();
        var chart = igviz.draw("#placeholder", gadgetConfig.chartConfig, dataTable);
        chart.plot(dataTable.data);

    } else {
        var chart = igviz.setUp("#placeholder", gadgetConfig.chartConfig, dataTable);
        chart.setXAxis({
            "labelAngle": -35,
            "labelAlign": "right",
            "labelDy": 0,
            "labelDx": 0,
            "titleDy": 25
        })
            .setYAxis({
                "titleDy": -30
            })
        chart.plot(dataTable.data);
    }
    //releasing the latch so that we can request data again from the backend.
    dataLoaded = true;
};


