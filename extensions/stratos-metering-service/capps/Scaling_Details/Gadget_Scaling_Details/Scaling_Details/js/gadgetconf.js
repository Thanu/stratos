var gadgetConfig = {
    "id": "Scaling_Details",
    "title": "Scaling Details",
    "datasource": "SCALING_DETAILS",
    "type": "batch",
    "columns": [{"name": "scaling_reason", "type": "STRING"}, {
        "name": "mc_threshold",
        "type": "INTEGER"
    }, {"name": "additional_instance_count", "type": "INTEGER"}, {
        "name": "cluster_id",
        "type": "STRING"
    }, {"name": "rif_threshold", "type": "INTEGER"}, {
        "name": "rif_predicted",
        "type": "INTEGER"
    }, {"name": "mc_required_instances", "type": "INTEGER"}, {
        "name": "required_instance_count",
        "type": "INTEGER"
    }, {"name": "la_threshold", "type": "INTEGER"}, {
        "name": "timestamp",
        "type": "STRING"
    }, {"name": "min_instance_count", "type": "INTEGER"}, {
        "name": "rif_required_instances",
        "type": "INTEGER"
    }, {"name": "scaling_decision_id", "type": "STRING"}, {
        "name": "mc_predicted",
        "type": "INTEGER"
    }, {"name": "max_instance_count", "type": "INTEGER"}, {
        "name": "la_required_instances",
        "type": "INTEGER"
    }, {"name": "active_instance_count", "type": "INTEGER"}, {"name": "la_predicted", "type": "INTEGER"}],
    "maxUpdateValue": 10,
    "chartConfig": {"chartType": "table", "xAxis": 0}
};