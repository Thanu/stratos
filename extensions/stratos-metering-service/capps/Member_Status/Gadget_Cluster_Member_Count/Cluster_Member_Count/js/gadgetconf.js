var gadgetConfig = {
    "id": "Cluster_Member_Count",
    "title": "Cluster Member Count",
    "datasource": "CLUSTER_MEMBER_COUNT",
    "type": "batch",
    "columns": [{"name": "activated_instance_count", "type": "INTEGER"}, {
        "name": "Time",
        "type": "STRING"
    }, {"name": "terminaed_instance_count", "type": "INTEGER"}, {
        "name": "cluster_id",
        "type": "STRING"
    }, {"name": "start_time", "type": "STRING"}, {"name": "active_instance_count", "type": "INTEGER"}],
    "maxUpdateValue": 10,
    "chartConfig": {"chartType": "line", "yAxis": [5], "xAxis": 1, "interpolationMode": "line"}
};