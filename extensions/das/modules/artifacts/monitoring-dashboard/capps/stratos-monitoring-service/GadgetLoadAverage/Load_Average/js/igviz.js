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
(function () {

    var igviz = window.igviz || {};

    igviz.version = '1.0.0';

    igviz.val = 0;
    window.igviz = igviz;
    var persistedData = [];
    var maxValueForUpdate;
    var singleNumSvg;
    var singleNumCurveSvg;
    var mapChart;
    var mapSVG;
    var worldMapCodes;
    var usaMapCodes;

    /*************************************************** Initializtion functions ***************************************************************************************************/


    igviz.draw = function (canvas, config, dataTable) {
        var chart = new Chart(canvas, config, dataTable);

        if (config.chartType == "singleNumber") {
            this.drawSingleNumberDiagram(chart);
        } else if (config.chartType == "map") {
            this.drawMap(canvas, config, dataTable);
        } else if (config.chartType == "tabular") {
            this.drawTable(canvas, config, dataTable);
        } else if (config.chartType == "arc") {
            this.drawArc(canvas, config, dataTable);
        } else if (config.chartType == "drill") {
            this.drillDown(0, canvas, config, dataTable, dataTable);
        }
        return chart;
        //return
    };

    igviz.setUp = function (canvas, config, dataTable) {
        var chartObject = new Chart(canvas, config, dataTable);

        if (config.chartType == "bar") {
            this.drawBarChart(chartObject, canvas, config, dataTable);
        } else if (config.chartType == "scatter") {
            this.drawScatterPlot(chartObject);
        } else if (config.chartType == "line") {
            this.drawLineChart(chartObject);
        } else if (config.chartType == "area") {
            this.drawAreaChart(chartObject);
        }
        return chartObject;
    };


    /*************************************************** Line chart ***************************************************************************************************/

    igviz.drawLineChart = function (chartObj) {
        divId = chartObj.canvas;
        chartConfig = chartObj.config;
        dataTable = chartObj.dataTable;

        xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis])
        yStrings = [];
        for (i = 0; i < chartConfig.yAxis.length; i++) {
            yStrings[i] = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis[i]])

        }


        var xScaleConfig = {
            "index": chartConfig.xAxis,
            "schema": dataTable.metadata,
            "name": "x",
            "range": "width",
            "zero": false,
            "clamp": false,
            "field": xString,
            "round": true
        };

        var yDomain = [];
        chartConfig.yAxis.forEach(function (columnIndex, i) {
            dataTable.data.forEach(function (row, j) {
                if (yDomain.indexOf(row[columnIndex]) == -1) {
                    yDomain.push(row[columnIndex]);
                }
            });
        });

        yDomain.sort(function (a, b) {
            return a - b;
        });

        var yScale = {
            name: "y",
            type: "linear",
            range: "height",
            zero: true,
            domain: [yDomain[0], yDomain[yDomain.length - 1]]
        };

        var xScale = setScale(xScaleConfig);
        var yScale = yScale;

        var timeInterval = chartConfig.timeInterval;

        var TIME_INTERVAL_1 = '30 Minutes';
        var TIME_INTERVAL_2 = '1 Hour';
        var TIME_INTERVAL_3 = '6 Hours';
        var TIME_INTERVAL_4 = '1 Day';
        var TIME_INTERVAL_5 = '1 Week';
        var TIME_INTERVAL_6 = '1 Month';
        var TIME_INTERVAL_7 = '6 Months';
        var TIME_INTERVAL_8 = '1 Year';
        var timeFormat;

        if (timeInterval === TIME_INTERVAL_1) {
            timeFormat = '%H:%M:%S';
        } else if (timeInterval === TIME_INTERVAL_2) {
            timeFormat = '%H:%M:%S';
        } else if (timeInterval === TIME_INTERVAL_3) {
            timeFormat = '%H:%M:%S';
        } else if (timeInterval === TIME_INTERVAL_4) {
            timeFormat = '%Y-%m-%d %H:%M';
        } else if (timeInterval === TIME_INTERVAL_5) {
            timeFormat = '%Y-%m-%d %H:%M';
        } else if (timeInterval === TIME_INTERVAL_6) {
            timeFormat = '%Y-%m-%d %H:%M';
        } else if (timeInterval === TIME_INTERVAL_7) {
            timeFormat = '%Y-%m-%d %H:%M';
        } else if (timeInterval === TIME_INTERVAL_8) {
            timeFormat = '%Y-%m-%d %H:%M';
        }

        var xAxisConfig = {
            "type": "x",
            "scale": "x",
            "angle": -35,
            "title": dataTable.metadata.names[chartConfig.xAxis] + " (" + timeInterval + ")",
            "grid": true,
            "dx": -10,
            "dy": 10,
            "align": "right",
            "titleDy": 10,
            "titleDx": 0,
            "format": timeFormat,
            "formatType": 'time'
        };
        var yAxisConfig = {
            "type": "y",
            "scale": "y",
            "angle": 0,
            "title": "Load Average (%)",
            "grid": true,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -10,
            "titleDx": 0
        };
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);

        if (chartConfig.interpolationMode == undefined) {
            chartConfig.interpolationMode = "monotone";
        }
        var spec = {
            "width": chartConfig.width - 100,
            "height": chartConfig.height,
            //  "padding":{"top":40,"bottom":60,'left':90,"right":150},
            "data": [
                {
                    "name": "table"

                }
            ],
            "scales": [
                xScale, yScale,
                {
                    "name": "color", "type": "ordinal", "range": "category20"
                }
            ],
            "axes": [xAxis, yAxis
            ],
            "legends": [
                //{
                //
                //    "orient": "right",
                //    "fill": "color",
                //    "title": "Load Averge",
                //    "values": [],
                //    "properties": {
                //        "title": {
                //            "fontSize": {"value": 14}
                //        },
                //        "labels": {
                //            "fontSize": {"value": 12}
                //        },
                //        "symbols": {
                //            "stroke": {"value": "transparent"}
                //        },
                //        "legend": {
                //            "stroke": {"value": "steelblue"},
                //            "strokeWidth": {"value": 1.5}
                //
                //        }
                //    }
                //}
            ],

            "marks": []
        };

        for (i = 0; i < chartConfig.yAxis.length; i++) {
            markObj = {
                "type": "line",
                "key": xString,
                "from": {"data": "table"},
                "properties": {
                    "enter": {
                        "x": {"value": 400},
                        "interpolate": {"value": chartConfig.interpolationMode},
                        "y": {"scale": "y:prev", "field": yStrings[i]},
                        "stroke": {"scale": "color", "value": dataTable.metadata.names[chartConfig.yAxis[i]]},
                        "strokeWidth": {"value": 1.0}
                    },
                    "update": {
                        "x": {"scale": "x", "field": xString},
                        "y": {"scale": "y", "field": yStrings[i]}
                    },
                    "exit": {
                        "x": {"value": -20},
                        "y": {"scale": "y", "field": yStrings[i]}
                    }
                }
            };
            pointObj = {
                "type": "symbol",
                "from": {"data": "table"},
                "properties": {
                    "enter": {
                        "x": {"scale": "x", "field": xString},
                        "y": {"scale": "y", "field": yStrings[i]},
                        "size": {"value": 50},
                        "fill": {
                            "scale": "color", "value": dataTable.metadata.names[chartConfig.yAxis[i]]
                            //"fillOpacity": {"value": 0.5}
                        },
                        "update": {
                            //"size": {"scale":"r","field":rString},
                            // "stroke": {"value": "transparent"}
                        },
                        "hover": {
                            "size": {"value": 300},
                            "stroke": {"value": "white"}
                        }
                    }
                }
            };


            spec.marks.push(markObj);
            spec.marks.push(pointObj);
            // spec.legends[0].values.push(dataTable.metadata.names[chartConfig.yAxis[i]])

        }


        chartObj.toolTipFunction = [];
        chartObj.toolTipFunction[0] = function (event, item) {
            var format = d3.time.format(timeFormat);
            //console.log(tool,event,item);
            if (item.mark.marktype == 'symbol') {
                xVar = dataTable.metadata.names[chartConfig.xAxis];
                yVar = dataTable.metadata.names[chartConfig.yAxis];

                contentString = '<table><tr><td> ' + xVar + ' </td><td>' + format(item.datum.data[xVar]) + '</td></tr>' + '<tr><td> ' + yVar + ' </td><td>' + item.datum.data[yVar] + '</td></tr></table>';


                tool.html(contentString).style({
                    'left': event.pageX + 10 + 'px',
                    'top': event.pageY + 10 + 'px',
                    'opacity': 1
                });
                tool.selectAll('tr td').style('padding', "3px");
            }
        };

        chartObj.toolTipFunction[1] = function (event, item) {

            tool.html("").style({'left': event.pageX + 10 + 'px', 'top': event.pageY + 10 + 'px', 'opacity': 0})

        };

        chartObj.spec = spec;
        chartObj.toolTip = true;

    };


    /*************************************************** Specification Generation method ***************************************************************************************************/


    function setScale(scaleConfig) {
        var scale = {"name": scaleConfig.name};

        //console.log(scaleConfig.schema,scaleConfig.index);

        dataFrom = "table";

        scale.range = scaleConfig.range;


        switch (scaleConfig.schema.types[scaleConfig.index]) {
            case 'T':
                scale["type"] = 'time'

                break;

            case 'C':
                scale["type"] = 'ordinal'
                if (scale.name === "c") {
                    scale.range = "category20";
                }

                break;
            case 'N':
                scale["type"] = 'linear'

                break;
        }
        if (scaleConfig.hasOwnProperty("dataFrom")) {
            dataFrom = scaleConfig.dataFrom;
        }

        scale.range = scaleConfig.range;
        scale.domain = {"data": dataFrom, "field": scaleConfig.field}

        //optional attributes
        if (scaleConfig.hasOwnProperty("round")) {
            scale["round"] = scaleConfig.round;
        }

        if (scaleConfig.hasOwnProperty("nice")) {
            scale["nice"] = scaleConfig.nice;
        }

        if (scaleConfig.hasOwnProperty("padding")) {
            scale["padding"] = scaleConfig.padding;
        }

        if (scaleConfig.hasOwnProperty("reverse")) {
            scale["reverse"] = scaleConfig.reverse;
        }

        if (scaleConfig.hasOwnProperty("sort")) {
            scale["sort"] = scaleConfig.sort;
        }

        if (scale.name == 'x' && scale.type == 'linear') {
            scale.sort = true;
        }
        if (scaleConfig.hasOwnProperty("clamp")) {
            scale["clamp"] = scaleConfig.clamp;
        }


        if (scaleConfig.hasOwnProperty("zero")) {
            scale["zero"] = scaleConfig.zero;
        }
        return scale;

    }

    function setAxis(axisConfig) {

        //console.log("Axis",axisConfig);

        axis = {
            "type": axisConfig.type,
            "scale": axisConfig.scale,
            'title': axisConfig.title,
            "grid": axisConfig.grid,

            "properties": {
                "ticks": {
                    // "stroke": {"value": "steelblue"}
                },
                "majorTicks": {
                    "strokeWidth": {"value": 2}
                },
                "labels": {
                    // "fill": {"value": "steelblue"},
                    "angle": {"value": axisConfig.angle},
                    // "fontSize": {"value": 14},
                    "align": {"value": axisConfig.align},
                    "baseline": {"value": "middle"},
                    "dx": {"value": axisConfig.dx},
                    "dy": {"value": axisConfig.dy}
                },
                "title": {
                    "fontSize": {"value": 16},

                    "dx": {'value': axisConfig.titleDx},
                    "dy": {'value': axisConfig.titleDy}
                },
                "axis": {
                    "stroke": {"value": "#333"},
                    "strokeWidth": {"value": 1.5}
                }

            }

        };

        if (axisConfig.hasOwnProperty("tickSize")) {
            axis["tickSize"] = axisConfig.tickSize;
        }


        if (axisConfig.hasOwnProperty("tickPadding")) {
            axis["tickPadding"] = axisConfig.tickPadding;
        }

        if (axisConfig.hasOwnProperty("ticks")) {
            axis["ticks"] = axisConfig.ticks;
        }

        if (axisConfig.hasOwnProperty("format")) {
            axis["format"] = axisConfig.format;
        }

        if (axisConfig.hasOwnProperty("formatType")) {
            axis["formatType"] = axisConfig.formatType;
        }
        //console.log("SpecAxis",axis);
        return axis;
    }

    function setLegends(chartConfig, schema) {

    }

    function setData(dataTableObj, chartConfig, schema) {

        var table = [];
        for (i = 0; i < dataTableObj.length; i++) {
            var ptObj = {};
            namesArray = schema.names;
            for (j = 0; j < namesArray.length; j++) {
                if (schema.types[j] == 'T') {
                    ptObj[createAttributeNames(namesArray[j])] = new Date(parseInt(dataTableObj[i][j]));
                } else if (schema.types[j] == 'N') {
                    ptObj[createAttributeNames(namesArray[j])] = parseFloat(dataTableObj[i][j]);
                } else {
                    ptObj[createAttributeNames(namesArray[j])] = dataTableObj[i][j];
                }
            }

            table[i] = ptObj;
        }

        return table;
    }

    function createAttributeNames(str) {
        return str.replace(' ', '_');
    }

    function setGenericAxis(axisConfig, spec) {
        MappingObj = {};
        MappingObj["tickSize"] = "tickSize";
        MappingObj["tickPadding"] = "tickPadding";
        MappingObj["title"] = "title";
        MappingObj["grid"] = "grid";
        MappingObj["offset"] = "offset";
        MappingObj["ticks"] = "ticks";

        MappingObj["labelColor"] = "fill";
        MappingObj["labelAngle"] = "angle";
        MappingObj["labelAlign"] = "align";
        MappingObj["labelFontSize"] = "fontSize";
        MappingObj["labelDx"] = "dx";
        MappingObj["labelDy"] = "dy";
        MappingObj["labelBaseLine"] = "baseline";

        MappingObj["titleDx"] = "dx";
        MappingObj["titleDy"] = "dy";
        MappingObj["titleFontSize"] = "fontSize";

        MappingObj["axisColor"] = "stroke";
        MappingObj["axisWidth"] = "strokeWidth";

        MappingObj["tickColor"] = "ticks.stroke";
        MappingObj["tickWidth"] = "ticks.strokeWidth";


        //console.log("previous Axis",spec)
        for (var propt in axisConfig) {

            if (propt == "tickSize" || propt == "tickPadding")
                continue;

            if (axisConfig.hasOwnProperty(propt)) {

                if (propt.indexOf("label") == 0)
                    spec.properties.labels[MappingObj[propt]].value = axisConfig[propt];
                else if (propt.indexOf("ticks") == 0)
                    spec.properties.ticks[MappingObj[propt]].value = axisConfig[propt];
                else if (propt.indexOf("title") == 0)
                    spec.properties.title[MappingObj[propt]].value = axisConfig[propt];
                else if (propt.indexOf("axis") == 0)
                    spec.properties.axis[MappingObj[propt]].value = axisConfig[propt];
                else
                    spec[MappingObj[propt]] = axisConfig[propt];
            }
        }

        //console.log("NEW SPEC",spec);
    }

    function createScales(dataset, chartConfig, dataTable) {
        //Create scale functions

        var xScale;
        var yScale;
        var colorScale;
        if (dataTable.metadata.types[chartConfig.xAxis] == 'N') {
            xScale = d3.scale.linear()
                .domain([0, d3.max(dataset, function (d) {
                    return d.data[d.config.xAxis];
                })])
                .range([chartConfig.padding, chartConfig.width - chartConfig.padding]);
        } else {
            xScale = d3.scale.ordinal()
                .domain(dataset.map(function (d) {
                    return d.data[chartConfig.xAxis];
                }))
                .rangeRoundBands([chartConfig.padding, chartConfig.width - chartConfig.padding], .1)
        }

        //TODO hanle case r and color are missing

        if (dataTable.metadata.types[chartConfig.yAxis] == 'N') {
            yScale = d3.scale.linear()
                .domain([0, d3.max(dataset, function (d) {
                    return d.data[d.config.yAxis];
                })])
                .range([chartConfig.height - chartConfig.padding, chartConfig.padding]);
            //var yScale = d3.scale.linear()
            //    .range([height, 0])
            //    .domain([0, d3.max(dataset, function(d) { return d.data[d.config.yAxis]; })])
        } else {
            yScale = d3.scale.ordinal()
                .rangeRoundBands([0, chartConfig.width], .1)
                .domain(dataset.map(function (d) {
                    return d.data[chartConfig.yAxis];
                }))
        }


        //this is used to scale the size of the point, it will value between 0-20
        var rScale = d3.scale.linear()
            .domain([0, d3.max(dataset, function (d) {
                return d.config.pointSize ? d.data[d.config.pointSize] : 20;
            })])
            .range([0, 20]);

        //TODO have to handle the case color scale is categorical : Done
        //http://synthesis.sbecker.net/articles/2012/07/16/learning-d3-part-6-scales-colors
        // add color to circles see https://www.dashingd3js.com/svg-basic-shapes-and-d3js
        //add legend http://zeroviscosity.com/d3-js-step-by-step/step-3-adding-a-legend
        if (dataTable.metadata.types[chartConfig.pointColor] == 'N') {
            colorScale = d3.scale.linear()
                .domain([-1, d3.max(dataset, function (d) {
                    return d.config.pointColor ? d.data[d.config.pointColor] : 20;
                })])
                .range([chartConfig.minColor, chartConfig.maxColor]);
        } else {
            colorScale = d3.scale.category20c();
        }

        //TODO add legend


        return {
            "xScale": xScale,
            "yScale": yScale,
            "rScale": rScale,
            "colorScale": colorScale
        }
    }


    /*************************************************** Util  functions ***************************************************************************************************/


    /**
     * Get the average of a numeric array
     * @param data
     * @returns average
     */
    function getAvg(data) {

        var sum = 0;

        for (var i = 0; i < data.length; i++) {
            sum = sum + data[i];
        }

        var average = (sum / data.length).toFixed(4);
        return average;
    }

    /**
     * Function to calculate the standard deviation
     * @param values
     * @returns sigma(standard deviation)
     */
    function standardDeviation(values) {
        var avg = getAvg(values);

        var squareDiffs = values.map(function (value) {
            var diff = value - avg;
            var sqrDiff = diff * diff;
            return sqrDiff;
        });

        var avgSquareDiff = getAvg(squareDiffs);

        var stdDev = Math.sqrt(avgSquareDiff);
        return stdDev;
    }

    /**
     * Get the p(x) : Helper function for the standard deviation
     * @param x
     * @param sigma
     * @param u
     * @returns {number|*}
     */
    function pX(x, sigma, u) {

        p = (1 / Math.sqrt(2 * Math.PI * sigma * sigma)) * Math.exp((-(x - u) * (x - u)) / (2 * sigma * sigma));

        return p;
    }


    /**
     * Get the normalized values for a list of elements
     * @param xVals
     * @returns {Array} of normalized values
     *
     */
    function NormalizationCoordinates(xVals) {

        var coordinates = [];

        var u = getAvg(xVals);
        var sigma = standardDeviation(xVals);

        for (var i = 0; i < xVals.length; i++) {

            coordinates[i] = {
                x: xVals[i],
                y: pX(xVals[i], sigma, u)
            };
        }

        return coordinates;
    }

    /**
     * This function will extract a column from a multi dimensional array
     * @param 2D array
     * @param index of column to be extracted
     * @return array of values
     */

    function parseColumnFrom2DArray(dataset, index) {

        var array = [];

        //console.log(dataset.length);
        //console.log(dataset[0].data);
        //console.log(dataset[1].data);

        for (var i = 0; i < dataset.length; i++) {
            array.push(dataset[i][index])
        }

        return array;
    }


    /*************************************************** Data Table Generation class ***************************************************************************************************/


        //DataTable that holds data in a tabular format
        //E.g var dataTable = new igviz.DataTable();
        //dataTable.addColumn("OrderId","C");
        //dataTable.addColumn("Amount","N");
        //dataTable.addRow(["12SS",1234.56]);
    igviz.DataTable = function (data) {
        this.metadata = {};
        this.metadata.names = [];
        this.metadata.types = [];
        this.data = [];
    };

    igviz.DataTable.prototype.addColumn = function (name, type) {
        this.metadata.names.push(name);
        this.metadata.types.push(type);
    };

    igviz.DataTable.prototype.addRow = function (row) {
        this.data.push(row);
    };

    igviz.DataTable.prototype.addRows = function (rows) {
        for (var i = 0; i < rows.length; i++) {
            this.data.push(rows[i]);
        }
        ;
    };

    igviz.DataTable.prototype.getColumnNames = function () {
        return this.metadata.names;
    };

    igviz.DataTable.prototype.getColumnByName = function (name) {
        var column = {};
        for (var i = 0; i < this.metadata.names.length; i++) {
            //TODO Need to check for case sensitiveness
            if (this.metadata.names[i] == name) {
                column.name = this.metadata.names[i];
                column.type = this.metadata.types[i];
                return column;
            }
        }
        ;
    };

    igviz.DataTable.prototype.getColumnByIndex = function (index) {
        var column = this.metadata.names[index];
        if (column) {
            column.name = column;
            column.type = this.metadata.types[index];
            return column;
        }

    };

    igviz.DataTable.prototype.getColumnData = function (columnIndex) {
        var data = [];
        this.data.map(function (d) {
            data.push(d[columnIndex]);
        });
        return data;
    };

    igviz.DataTable.prototype.toJSON = function () {
        //console.log(this);
    };


    /*************************************************** Chart Class And API ***************************************************************************************************/


    function Chart(canvas, config, dataTable) {
        //this.chart=chart;
        this.dataTable = dataTable;
        this.config = config;
        this.canvas = canvas;
    }

    Chart.prototype.setXAxis = function (xAxisConfig) {

        /*
         *         axis=  {
         "type": axisConfig.type,
         "scale": axisConfig.scale,
         'title': axisConfig.title,
         "grid":axisConfig.grid,

         "properties": {
         "ticks": {
         // "stroke": {"value": "steelblue"}
         },
         "majorTicks": {
         "strokeWidth": {"value": 2}
         },
         "labels": {
         // "fill": {"value": "steelblue"},
         "angle": {"value": axisConfig.angle},
         // "fontSize": {"value": 14},
         "align": {"value": axisConfig.align},
         "baseline": {"value": "middle"},
         "dx": {"value": axisConfig.dx},
         "dy": {"value": axisConfig.dy}
         },
         "title": {
         "fontSize": {"value": 16},

         "dx":{'value':axisConfig.titleDx},
         "dy":{'value':axisConfig.titleDy}
         },
         "axis": {
         "stroke": {"value": "#333"},
         "strokeWidth": {"value": 1.5}
         }

         }

         }

         if (axisConfig.hasOwnProperty("tickSize")) {
         axis["tickSize"] = axisConfig.tickSize;
         }


         if (axisConfig.hasOwnProperty("tickPadding")) {
         axis["tickPadding"] = axisConfig.tickPadding;
         }
         */
        var xAxisSpec = this.spec.axes[0];
        setGenericAxis(xAxisConfig, xAxisSpec);
        /*xAxisConfig.tickSize
         xAxisConfig.tickPadding
         xAxisConfig.title;
         xAxisConfig.grid;
         xAxisConfig.offset
         xAxisConfig.ticks


         xAxisConfig.labelFill
         xAxisConfig.labelFontSize
         xAxisConfig.labelAngle
         xAxisConfig.labelAlign
         xAxisConfig.labelDx
         xAxisConfig.labelDy
         xAxisConfig.labelBaseLine;

         xAxisConfig.titleDx;
         xAxisConfig.titleDy
         xAxisConfig.titleFontSize;

         xAxisConfig.axisColor;
         xAxisConfig.axisWidth;

         xAxisConfig.tickColor;
         xAxisConfig.tickWidth;
         */


        return this;
    };

    Chart.prototype.setYAxis = function (yAxisConfig) {

        var yAxisSpec = this.spec.axes[1];
        setGenericAxis(yAxisConfig, yAxisSpec);

        return this;
    };

    Chart.prototype.setPadding = function (paddingConfig) {

        if (this.spec.padding == undefined) {
            this.spec.padding = {};
            this.spec.padding.top = 0;
            this.spec.padding.bottom = 0;
            this.spec.padding.left = 0;
            this.spec.padding.right = 0;
        }
        for (var propt in paddingConfig) {
            if (paddingConfig.hasOwnProperty(propt)) {

                this.spec.padding[propt] = paddingConfig[propt];
            }
        }

        this.spec.width = this.originalWidth - this.spec.padding.left - this.spec.padding.right;
        this.spec.height = this.originalHeight - this.spec.padding.top - this.spec.padding.bottom;

        return this;
    };

    Chart.prototype.unsetPadding = function () {
        delete this.spec.padding;
        this.spec.width = this.originalWidth;
        this.spec.height = this.originalHeight;
        return this;
    };

    Chart.prototype.setDimension = function (dimensionConfig) {

        if (dimensionConfig.width != undefined) {

            this.spec.width = dimensionConfig.width;
            this.originalWidth = dimensionConfig.width;

        }

        if (dimensionConfig.height != undefined) {

            this.spec.height = dimensionConfig.height;
            this.originalHeight = dimensionConfig.height;

        }

    };

    Chart.prototype.update = function (pointObj) {
        //console.log("+++ Inside update");

        if (this.config.chartType == "map") {
            config = this.config;
            $.each(mapSVG[0][0].__data__, function (i, val) {
                if (mapSVG[0][0].__data__[i][config.xAxis] == "DEF") {
                    mapSVG[0][0].__data__.splice(i, 1);
                }
            });

            $.each(pointObj, function (i, val) {
                pointObj[i][config.xAxis] = getMapCode(pointObj[i][config.xAxis], config.region);
                mapSVG[0][0].__data__.push(pointObj[i]);
            });

            $(this.canvas).empty();
            d3.select(this.canvas).datum(mapSVG[0][0].__data__).call(mapChart.draw, mapChart);
        } else {

            if (persistedData.length >= maxValueForUpdate) {

                var newTable = setData([pointObj], this.config, this.dataTable.metadata);
                var point = this.table.shift();
                this.dataTable.data.shift();
                this.dataTable.data.push(pointObj);
                this.table.push(newTable[0]);

                if (this.config.chartType == "tabular" || this.config.chartType == "singleNumber") {
                    this.plot(persistedData, maxValueForUpdate);
                } else {
                    this.chart.data(this.data).update({"duration": 500});
                }
            } else {
                persistedData.push(pointObj);
                this.plot(persistedData, null);
            }
        }
    };

    Chart.prototype.updateList = function (dataList, callback) {
        console.log("+++ Inside updateList");

        for (i = 0; i < dataList.length; i++) {
            this.dataTable.data.shift();
            this.dataTable.data.push(dataList[i]);
        }

        var newTable = setData(dataList, this.config, this.dataTable.metadata);

        for (i = 0; i < dataList.length; i++) {
            var point = this.table.shift();
            this.table.push(newTable[i]);
        }

        //     console.log(point,this.chart,this.data);
        this.chart.data(this.data).update();

    };

    Chart.prototype.resize = function () {
        var ref = this;
        var newH = document.getElementById(ref.canvas.replace('#', '')).offsetHeight;
        var newW = document.getElementById(ref.canvas.replace('#', '')).offsetWidth;
        console.log("Resized", newH, newW, ref);

        var left = 0, top = 0, right = 0, bottom = 0;

        var w = ref.spec.width;
        var h = ref.spec.height;
        //if(ref.spec.padding==undefined)
        //{
        //    w=newW;
        //    h=newH;
        //
        //}
        // else {
        //
        //    if (ref.spec.padding.left!=undefined){
        //        left=ref.spec.padding.left;
        //
        //    }
        //
        //    if (ref.spec.padding.bottom!=undefined){
        //        bottom=ref.spec.padding.bottom;
        //
        //    }
        //    if (ref.spec.padding.top!=undefined){
        //        top=ref.spec.padding.top;
        //
        //    }
        //    if (ref.spec.padding.right!=undefined){
        //        right=ref.spec.padding.right;
        //
        //    }
        //    w=newW-left-right;
        //    h=newH-top-bottom;
        //
        //}
        //console.log(w,h);
        ref.chart.width(w).height(h).renderer('svg').update({props: 'enter'}).update();

    };

    Chart.prototype.plot = function (dataset, callback, maxValue) {

        var config = this.config;

        if (config.chartType == "singleNumber") {

            //configure font sizes
            var MAX_FONT_SIZE = config.width * config.height * 0.0002;
            var AVG_FONT_SIZE = config.width * config.height * 0.0004;
            var MIN_FONT_SIZE = config.width * config.height * 0.0002;

            //div elements to append single number diagram components
            var minDiv = "minValue";
            var maxDiv = "maxValue";
            var avgDiv = "avgValue";

            //removing if already exist group element
            singleNumSvg.select("#groupid").remove();
            //appending a group to the diagram
            var SingleNumberDiagram = singleNumSvg
                .append("g").attr("id", "groupid");

            if (maxValue !== undefined) {

                if (dataset.length >= maxValue) {
                    var allowedDataSet = [];
                    var startingPoint = dataset.length - maxValue;
                    for (var i = startingPoint; i < dataset.length; i++) {
                        allowedDataSet.push(dataset[i]);
                    }
                    dataset = allowedDataSet;
                } else {
                    maxValueForUpdate = maxValue;
                    persistedData = dataset;
                }
            }

            //  getting a reference to the data
            var tableData = dataset;
            var table = setData(dataset, this.config, this.dataTable.metadata);
            var data = {table: table}
            this.data = data;
            this.table = table;


            var datamap = tableData.map(function (d) {
                return {
                    "data": d,
                    "config": config
                }
            });

            //parse a column to calculate the data for the single number diagram
            var selectedColumn = parseColumnFrom2DArray(tableData, config.xAxis);


            //Minimum value goes here

            SingleNumberDiagram.append("text")
                .attr("id", minDiv)
                .text("Max: " + d3.max(selectedColumn))
                //.text(50)
                .attr("font-size", MIN_FONT_SIZE)
                .attr("x", 3 * config.width / 4)
                .attr("y", config.height / 4)
                .style("fill", "black")
                .style("text-anchor", "middle")
                .style("lignment-baseline", "middle")
            ;

            //Average value goes here
            SingleNumberDiagram.append("text")
                .attr("id", avgDiv)
                .text(getAvg(selectedColumn))
                .attr("font-size", AVG_FONT_SIZE)
                .attr("x", config.width / 2)
                .attr("y", config.height / 2 + d3.select("#" + avgDiv).attr("font-size") / 5)
                .style("fill", "black")
                .style("text-anchor", "middle")
                .style("lignment-baseline", "middle")
            ;

            //Maximum value goes here
            SingleNumberDiagram.append("text")
                .attr("id", maxDiv)
                .text("Min: " + d3.min(selectedColumn))
                .attr("font-size", MAX_FONT_SIZE)
                .attr("x", 3 * config.width / 4)
                .attr("y", 3 * config.height / 4)
                .style("fill", "black")
                .style("text-anchor", "middle")
                .style("lignment-baseline", "middle")
            ;

            //constructing curve

            var margin = {top: 10, right: 10, bottom: 10, left: 0};
            var width = config.width * 0.305 - margin.left - margin.right;
            var height = config.height * 0.5 - margin.top - margin.bottom;

            singleNumSvg.append("rect")
                .attr("id", "rectCurve")
                .attr("x", 3)
                .attr("y", config.height * 0.5)
                .attr("width", config.width * 0.305)
                .attr("height", config.height * 0.5);

            var normalizedCoordinates = NormalizationCoordinates(selectedColumn.sort(function (a, b) {
                return a - b
            }));
            //console.log(normalizedCoordinates);


            // Set the ranges
            var x = d3.time.scale().range([0, config.width * 0.305]);
            var y = d3.scale.linear().range([config.height * 0.5, 0]);

            // Define the x axis
            var xAxis = d3.svg.axis().scale(x)
                .orient("bottom").ticks(0);


            // Define the line
            var valueLines = d3.svg.line()
                .x(function (d) {
                    return x(d.x);
                })
                .y(function (d) {
                    return y(d.y);
                });

            //removing if already exist group element
            singleNumSvg.select("#curvegroupid").remove();
            // Adds the svg canvas
            var normalizationCurve = singleNumSvg
                .append("g").attr("id", "curvegroupid")
                .attr("transform", "translate(" + 2 + "," + ((config.height * 0.5) + 4) + ")");

            // Scale the range of the data
            x.domain(d3.extent(normalizedCoordinates, function (d) {
                return d.x;
            }));
            y.domain([0, d3.max(normalizedCoordinates, function (d) {
                return d.y;
            })]);

            // Add the valueLines path.
            normalizationCurve.append("path")
                .attr("class", "line")
                .transition()
                .attr("d", valueLines(normalizedCoordinates))
                .delay(function (d, i) {
                    return i * 100;
                })
                .duration(10000)
                .ease('linear');
            ;

            // Add the X Axis
            normalizationCurve.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(1," + ((config.height * 0.5) - 8) + ")")
                .call(xAxis)
            ;

        } else if (config.chartType == "map") {

            $.each(dataset, function (i, val) {
                dataset[i][config.xAxis] = getMapCode(dataset[i][config.xAxis], config.region);
            });

            var defaultRow = jQuery.extend({}, dataset[0]);
            defaultRow[config.xAxis] = "DEF";
            defaultRow[config.yAxis] = 0;

            dataset.push(defaultRow);
            mapSVG = d3.select(this.canvas).datum(dataset).call(mapChart.draw, mapChart);

        } else if (config.chartType == "tabular") {

            var isColorBasedSet = this.config.colorBasedStyle;
            var isFontBasedSet = this.config.fontBasedStyle;

            if (maxValue !== undefined) {

                if (dataset.length >= maxValue) {
                    var allowedDataSet = [];
                    var startingPoint = dataset.length - maxValue;
                    for (var i = startingPoint; i < dataset.length; i++) {
                        allowedDataSet.push(dataset[i]);
                    }
                    dataset = allowedDataSet;
                } else {
                    maxValueForUpdate = maxValue;
                    persistedData = dataset;
                }
            }

            var tableData = dataset;
            tableData.reverse();

            var table = setData(dataset, this.config, this.dataTable.metadata);
            var data = {table: table};
            this.data = data;
            this.table = table;

            //Using RGB color code to represent colors
            //Because the alpha() function use these property change the contrast of the color
            var colors = [{
                r: 255,
                g: 0,
                b: 0
            }, {
                r: 0,
                g: 255,
                b: 0
            }, {
                r: 200,
                g: 100,
                b: 100
            }, {
                r: 200,
                g: 255,
                b: 250
            }, {
                r: 255,
                g: 140,
                b: 100
            }, {
                r: 230,
                g: 100,
                b: 250
            }, {
                r: 0,
                g: 138,
                b: 230
            }, {
                r: 165,
                g: 42,
                b: 42
            }, {
                r: 127,
                g: 0,
                b: 255
            }, {
                r: 0,
                g: 255,
                b: 255
            }];

            //function to change the color depth
            //default domain is set to [0, 100], but it can be changed according to the dataset
            var alpha = d3.scale.linear().domain([0, 100]).range([0, 1]);

            var colorRows = d3.scale.linear()
                .domain([2.5, 4])
                .range(['#F5BFE8', '#E305AF']);

            var fontSize = d3.scale.linear()
                .domain([0, 100])
                .range([15, 20]);


            var rows = tbody.selectAll("tr")
                .data(tableData);

            rows.enter()
                .append("tr");
            rows.exit().remove();

            rows.order();

            var cells;

            if (isColorBasedSet == true && isFontBasedSet == true) {

                //adding the  data to the table rows
                cells = rows.selectAll("td")
                    .data(function (d, i) {

                        return d;
                    });

                cells.enter()
                    .append("td");

                cells.text(function (d, i) {
                        return d;
                    })
                    .style("font-size", function (d, i) {
                        fontSize.domain([
                            d3.min(parseColumnFrom2DArray(tableData, i)),
                            d3.max(parseColumnFrom2DArray(tableData, i))
                        ]);
                        return fontSize(d) + "px";
                    })
                    .style('background-color', function (d, i) {

                        //This is where the color is decided for the cell
                        //The domain set according to the data set we have now
                        //Minimum & maximum values for the particular data column is used as the domain
                        alpha.domain([d3.min(parseColumnFrom2DArray(tableData, i)), d3.max(parseColumnFrom2DArray(tableData, i))]);

                        //return the color for the cell
                        return 'rgba(' + colors[i].r + ',' + colors[i].g + ',' + colors[i].b + ',' + alpha(d) + ')';

                    });

            } else if (isColorBasedSet && !isFontBasedSet) {
                //adding the  data to the table rows
                cells = rows.selectAll("td")
                    .data(function (d, i) {

                        return d;
                    });

                cells.enter()
                    .append("td");

                cells.text(function (d, i) {
                        return d;
                    })
                    .style('background-color', function (d, i) {

                        //This is where the color is decided for the cell
                        //The domain set according to the data set we have now
                        //Minimum & maximum values for the particular data column is used as the domain
                        alpha.domain([
                            d3.min(parseColumnFrom2DArray(tableData, i)),
                            d3.max(parseColumnFrom2DArray(tableData, i))
                        ]);

                        //return the color for the cell
                        return 'rgba(' + colors[i].r + ',' + colors[i].g + ',' + colors[i].b + ',' + alpha(d) + ')';

                    });

            } else if (!isColorBasedSet && isFontBasedSet) {

                //adding the  data to the table rows
                cells = rows.selectAll("td")
                    .data(function (d, i) {

                        return d;
                    });

                cells.enter()
                    .append("td");

                cells.text(function (d, i) {
                        return d;
                    })
                    .style("font-size", function (d, i) {
                        fontSize.domain([
                            d3.min(parseColumnFrom2DArray(tableData, i)),
                            d3.max(parseColumnFrom2DArray(tableData, i))
                        ]);
                        return fontSize(d) + "px";
                    });

            } else {
                //appending the rows inside the table body
                rows.style('background-color', function (d, i) {

                        colorRows.domain([
                            d3.min(parseColumnFrom2DArray(tableData, config.xAxis)),
                            d3.max(parseColumnFrom2DArray(tableData, config.xAxis))
                        ]);
                        return colorRows(d[config.xAxis]);
                    })
                    .style("font-size", function (d, i) {

                        fontSize.domain([
                            d3.min(parseColumnFrom2DArray(tableData, i)),
                            d3.max(parseColumnFrom2DArray(tableData, i))
                        ]);
                        return fontSize(d) + "px";
                    });

                //adding the  data to the table rows
                cells = rows.selectAll("td")
                    .data(function (d, i) {

                        return d;
                    });

                cells.enter()
                    .append("td");

                cells.text(function (d, i) {
                    return d;
                });
            }
            tableData.reverse();
        } else {
            if (maxValue !== undefined) {
                if (dataset.length >= maxValue) {
                    var allowedDataSet = [];
                    var startingPoint = dataset.length - maxValue;
                    for (var i = startingPoint; i < dataset.length; i++) {
                        allowedDataSet.push(dataset[i]);
                    }
                    dataset = allowedDataSet;
                } else {
                    maxValueForUpdate = maxValue;
                    persistedData = dataset;
                }
            }

            var table = setData(dataset, this.config, this.dataTable.metadata);
            var data = {table: table};

            var divId = this.canvas;
            this.data = data;
            this.table = table;


            if (this.legend) {
                legendsList = [];
                for (i = 0; i < dataset.length; i++) {
                    a = dataset[i][this.legendIndex];
                    isfound = false;
                    for (j = 0; j < legendsList.length; j++) {
                        if (a == legendsList[j]) {
                            isfound = true;
                            break;
                        }
                    }

                    if (!isfound) {
                        legendsList.push(a);
                    }
                }

                this.spec.legends[0].values = legendsList;
            }

            var specification = this.spec;
            var isTool = this.toolTip;
            var toolTipFunction = this.toolTipFunction;

            var ref = this;

            vg.parse.spec(specification, function (chart) {
                ref.chart = chart({
                    el: divId,
                    renderer: 'svg',
                    data: data
                }).update();

                if (isTool) {

                    tool = d3.select('body').append('div').style({
                        'position': 'absolute',
                        'opacity': 0,
                        'padding': "4px",
                        'border': "2px solid ",
                        'background': 'white'
                    });
                    ref.chart.on('mouseover', toolTipFunction[0]);
                    ref.chart.on('mouseout', toolTipFunction[1]);
                }

                if (callback) {
                    callback.call(ref);
                }
            });
            console.log(this);
        }

    }

})();
