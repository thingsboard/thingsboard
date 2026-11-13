// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.utils;

public class SolutionTemplatesConstants {

    /**
     * Water metering
     **/

    //Rule chains
    public static final String WATER_METERING_SOLUTION_MAIN_RULE_CHAIN = "Water Metering Solution Main";
    public static final String WATER_METERING_SOLUTION_ALARM_ROUTING_RULE_CHAIN = "Water Metering Solution Customer Alarm Routing";
    public static final String WATER_METERING_SOLUTION_TENANT_ALARM_ROUTING_RULE_CHAIN = "Water Metering Solution Tenant Alarm Routing";
    public static final String WATER_METERING_SOLUTION_MAIN_REMOTE_FACILITY_RULE_CHAIN = WATER_METERING_SOLUTION_MAIN_RULE_CHAIN + " (Remote Facility)";

    //Customers
    public static final String WATER_METERING_CUSTOMER_A_CUSTOMER = "Water Metering Customer A";
    public static final String WATER_METERING_CUSTOMER_B_CUSTOMER = "Water Metering Customer B";

    //Roles
    public static final String WATER_METERING_READ_ONLY_ROLES = "Water Metering Read Only";
    public static final String WATER_METERING_USER_ROLES = "Water Metering User";

    //Profiles
    public static final String WATER_METER_DEVICE_PROFILE_WM = "Water Meter";

    //Devices
    public static final String WM0000123_DEVICE = "WM0000123";
    public static final String WM0000124_DEVICE = "WM0000124";
    public static final String WM0000125_DEVICE = "WM0000125";

    //Dashboards
    public static final String WATER_METERING_USER_DASHBOARD = "Water Metering User Dashboard";
    public static final String WATER_METERING_TENANT_DASHBOARD = "Water Metering Tenant Dashboard";

    //Entity groups
    public static final String WATER_METERING_CUSTOMER_GROUP = "Water Metering";
    public static final String WATER_METERS_DEVICE_GROUP = "Water Meters";
    public static final String WATER_METERING_DASHBOARD_GROUP = "Water Metering";
    public static final String WATER_METERING_SHARED_DASHBOARD_GROUP = "Water Metering Shared";

    //Instances
    public static final String REMOTE_FACILITY_WI_EDGE = "Remote Facility W1";

    /**
     * Temperature & Humidity Sensors
     **/

    //Rule chains
    public static final String TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN = "Temperature & Humidity Sensors";
    public static final String TEMPERATURE_HUMIDITY_SENSORS_REMOTE_FACILITY_RULE_CHAIN = TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN + " (Remote Facility)";

    //Roles
    public static final String READ_ONLY_ROLES = "Read Only";

    //Customers
    public static final String CUSTOMER_D = "Customer D";

    //Profiles
    public static final String TEMPERATURE_SENSOR_DEVICE_PROFILE = "Temperature Sensor";

    //Devices
    public static final String SENSOR_C1_DEVICE = "Sensor C1";
    public static final String SENSOR_T1_DEVICE = "Sensor T1";

    //Dashboards
    public static final String TEMPERATURE_HUMIDITY_DASHBOARD = "Temperature & Humidity";

    //Entity groups
    public static final String TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP = "Temperature & Humidity sensors";
    public static final String CUSTOMER_DASHBOARD_GROUP = "Customer dashboards";

    //Instances
    public static final String REMOTE_FACILITY_RI_EDGE = "Remote Facility R1";

    /**
     * Smart Retail
     **/

    //Rule chains
    public static final String SUPERMARKET_DEVICES_RULE_CHAIN = "Supermarket Devices";
    public static final String SUPERMARKET_DEVICES_REMOTE_FACILITY_RULE_CHAIN = SUPERMARKET_DEVICES_RULE_CHAIN + " (Remote Supermarket)";

    //Roles
    public static final String SMART_RETAIL_READ_ONLY_ROLE = "Smart Retail Read Only";
    public static final String SMART_RETAIL_USER_ROLE = "Smart Retail User";
    public static final String SMART_RETAIL_ADMINISTRATOR_ROLE = "Smart Retail Administrator";

    //Customers
    public static final String RETAIL_COMPANY_A_CUSTOMER = "Retail Company A";
    public static final String RETAIL_COMPANY_B_CUSTOMER = "Retail Company B";

    //Profiles
    public static final String DOOR_SENSOR_DEVICE_PROFILE = "Door Sensor";
    public static final String SMOKE_SENSOR_DEVICE_PROFILE = "Smoke Sensor";
    public static final String SMART_SHELF_DEVICE_PROFILE = "Smart Shelf";
    public static final String CHILLER_DEVICE_PROFILE = "Chiller";
    public static final String MOTION_SENSOR_DEVICE_PROFILE = "Motion Sensor";
    public static final String FREEZER_DEVICE_PROFILE = "Freezer";
    public static final String SMART_BIN_DEVICE_PROFILE = "Smart Bin";
    public static final String OCCUPANCY_SENSOR_DEVICE_PROFILE = "Occupancy Sensor";
    public static final String LIQUID_LEVEL_SENSOR_DEVICE_PROFILE = "Liquid Level Sensor";
    public static final String SUPERMARKET_ASSET_PROFILE = "supermarket";

    //Devices
    public static final String CHILLER3_DEVICE = "Chiller 3";
    public static final String CHILLER65644_DEVICE = "Chiller 65644";
    public static final String DOOR_SENSOR_1_DEVICE = "Door Sensor 1";
    public static final String DOOR_SENSOR_2_DEVICE = "Door Sensor 2";
    public static final String DOOR_SENSOR_3_DEVICE = "Door Sensor 3";
    public static final String DOOR_SENSOR_4534_DEVICE = "Door Sensor 4534";
    public static final String FREEZER_1_DEVICE = "Freezer 1";
    public static final String FREEZER_43545_DEVICE = "Freezer 43545";
    public static final String LIQUID_LEVEL_SENSOR_1_DEVICE = "Liquid Level Sensor 1";
    public static final String LIQUID_LEVEL_SENSOR_2_DEVICE = "Liquid Level Sensor 2";
    public static final String LIQUID_LEVEL_SENSOR_3_DEVICE = "Liquid Level Sensor 3";
    public static final String LIQUID_LEVEL_SENSOR_4_DEVICE = "Liquid Level Sensor 4";
    public static final String MOTION_SENSOR_1_DEVICE = "Motion Sensor 1";
    public static final String OCCUPANCY_SENSOR_DEVICE = "Occupancy Sensor";
    public static final String SMART_BIN_1_DEVICE = "Smart Bin 1";
    public static final String SMART_BIN_2_DEVICE = "Smart Bin 2";
    public static final String SMART_BIN_3_DEVICE = "Smart Bin 3";
    public static final String SMART_BIN_4_DEVICE = "Smart Bin 4";
    public static final String SMART_SHELF_457321_DEVICE = "Smart Shelf 457321";
    public static final String SMART_SHELF_557322_DEVICE = "Smart Shelf 557322";
    public static final String SMART_SHELF_765765_DEVICE = "Smart Shelf 765765";
    public static final String SMOKE_SENSOR_1_DEVICE = "Smoke Sensor 1";
    public static final String SMOKE_SENSOR_2_DEVICE = "Smoke Sensor 2";
    public static final String SMOKE_SENSOR_3_DEVICE = "Smoke Sensor 3";
    public static final String SMOKE_SENSOR_4_DEVICE = "Smoke Sensor 4";
    public static final String SMOKE_SENSOR_5_DEVICE = "Smoke Sensor 5";
    public static final String SMOKE_SENSOR_6_DEVICE = "Smoke Sensor 6";
    public static final String SMART_SHELF_89546_DEVICE = "Smart Shelf 89546";
    public static final String CHILLER_378876_DEVICE = "Chiller 378876";
    public static final String FREEZER_67478 = "Freezer 67478";
    public static final String DOOR_SENSOR_3456_DEVICE = "Door Sensor 3456";

    //Dashboards
    public static final String SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD = "Smart Supermarket User Management";
    public static final String SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD = "Smart Supermarket Administration";
    public static final String SMART_SUPERMARKET_DASHBOARD = "Smart Supermarket";

    //Assets
    public static final String SUPERMARKETS_S1_ASSET = "Supermarket S1";
    public static final String SUPERMARKETS_S2_ASSET = "Supermarket S2";
    public static final String SUPERMARKETS_S3_ASSET = "Supermarket S3";

    //Entity groups
    public static final String SMART_RETAIL_USERS_USER_GROUP = "Smart Retail Users";
    public static final String SMART_RETAIL_ADMINISTRATORS_USER_GROUP = "Smart Retail Administrators";
    public static final String SUPERMARKET_DEVICES_DEVICE_GROUP = "Supermarket Devices";
    public static final String SUPERMARKETS_ASSET_GROUP = "Supermarkets";
    public static final String SUPERMARKET_USER_SHARED_DASHBOARD_GROUP = "Supermarket Users Shared";
    public static final String SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP = "Supermarket Administrators Shared";
    public static final String SMART_RETAIL_CUSTOMER_GROUP = "Smart Retail";

    //Instances
    public static final String REMOTE_SUPERMARKET_EDGE = "Remote Supermarket R1";

    /**
     * Smart office
     **/

    //Profiles
    public static final String SMART_SENSOR_DEVICE_PROFILE = "smart-sensor";
    public static final String HVAC_DEVICE_PROFILE = "hvac";
    public static final String ENERGY_METER_DEVICE_PROFILE = "energy-meter";
    public static final String WATER_METERING_DEVICE_PROFILE_SO = "water-meter";
    public static final String OFFICE_ASSET_PROFILE = "office";

    //Devices
    public static final String SMART_SENSOR_DEVICE = "Smart sensor";
    public static final String HVAC_DEVICE = "HVAC";
    public static final String ENERGY_METER_DEVICE = "Energy meter";
    public static final String WATER_METER_DEVICE = "Water meter";

    //Dashboards
    public static final String SMART_OFFICE_DASHBOARD = "Smart office";

    //Assets
    public static final String OFFICE_ASSET = "Office";

    //Entity groups
    public static final String OFFICE_SENSORS_DEVICE_GROUPS = "Office sensors";
    public static final String BUILDINGS_ASSET_GROUP = "Buildings";
    public static final String SMART_OFFICE_DASHBOARDS_GROUP = "Smart office dashboards";

    //Instances
    public static final String REMOTE_OFFICE_R1_EDGE = "Remote Office R1";

    /**
     * Fleet tracking
     **/

    //Profiles
    public static final String BUS_DEVICE_PROFILE = "bus";

    //Devices
    public static final String BUS_A_DEVICE = "Bus A";
    public static final String BUS_B_DEVICE = "Bus B";
    public static final String BUS_C_DEVICE = "Bus C";
    public static final String BUS_D_DEVICE = "Bus D";

    //Dashboards
    public static final String FLEET_TRACKING_DASHBOARD = "Fleet tracking";

    //Entity groups
    public static final String FLEET_TRACKING_DASHBOARD_GROUP = "Fleet tracking";
    public static final String BUS_DEVICES_DEVICE_GROUP = "Bus devices";

    //Instances
    public static final String REMOTE_BUS_STATION_R1_EDGE = "Remote Bus Station R1";

    /**
     * Air Quality Monitoring
     **/

    //Rule chains
    public static final String AQI_SENSOR_RULE_CHAIN = "AQI Sensor";
    public static final String AQI_SENSOR_REMOTE_LOCATION_RULE_CHAIN = "AQI Sensor (Remote Location)";
    public static final String AQI_CITY_RULE_CHAIN = "AQI City";
    public static final String AQI_CITY_REMOTE_LOCATION_RULE_CHAIN = "AQI City (Remote Location)";

    //Profiles
    public static final String AQI_CITY_ASSET_PROFILE = "AQI City";

    //Devices
    public static final String AIR_QUALITY_SENSOR_1_DEVICE = "Air Quality Sensor 1";
    public static final String AIR_QUALITY_SENSOR_2_DEVICE = "Air Quality Sensor 2";
    public static final String AIR_QUALITY_SENSOR_3_DEVICE = "Air Quality Sensor 3";
    public static final String AIR_QUALITY_SENSOR_4_DEVICE = "Air Quality Sensor 4";
    public static final String AIR_QUALITY_SENSOR_5_DEVICE = "Air Quality Sensor 5";

    //Assets
    public static final String LOS_ANGELES_CA_ASSET = "Los Angeles, CA";

    //Dashboards
    public static final String AIR_QUALITY_MONITORING_DASHBOARD = "Air Quality Monitoring";
    public static final String AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD = "Air Quality Monitoring Administration";

    //Entity groups
    public static final String AQI_SENSOR_DEVICE_GROUP = "AQI Sensor";
    public static final String AQI_CITY_ASSET_GROUP = "AQI City";
    public static final String AIR_QUALITY_MONITORING_DASHBOARD_GROUP = "Air Quality Monitoring Public";

    //Instances
    public static final String REMOTE_LOCATION_EDGE = "Remote Location R1";

    /**
     * Smart Irrigation
     **/

    //Rule chains
    public static final String SI_COUNT_ALARMS_RULE_CHAIN = "SI Count Alarms";
    public static final String SI_COUNT_ALARMS_REMOTE_FARM_RULE_CHAIN = SI_COUNT_ALARMS_RULE_CHAIN + " (Remote Farm)";
    public static final String SI_FIELD_RULE_CHAIN = "SI Field";
    public static final String SI_FIELD_RULE_REMOTE_FARM_CHAIN = "SI Field  (Remote Farm)";
    public static final String SI_SOIL_MOISTURE_RULE_CHAIN = "SI Soil Moisture";
    public static final String SI_SOIL_MOISTURE_REMOTE_FARM_RULE_CHAIN = SI_SOIL_MOISTURE_RULE_CHAIN + " (Remote Farm)";
    public static final String SI_WATER_METER_RULE_CHAIN = "SI Water Meter";
    public static final String SI_WATER_METER_REMOTE_FARM_RULE_CHAIN = SI_WATER_METER_RULE_CHAIN + " (Remote Farm)";
    public static final String SI_SMART_VALVE_RULE_CHAIN = "SI Smart Valve";
    public static final String SI_SMART_VALVE_REMOTE_FARM_RULE_CHAIN = SI_SMART_VALVE_RULE_CHAIN +  " (Remote Farm)";

    //Profiles
    public static final String SI_SMART_VALVE_DEVICE_PROFILE = "SI Smart Valve";
    public static final String SI_WATER_METER_DEVICE_PROFILE = "SI Water Meter";
    public static final String SI_SOIL_MOISTURE_SENSOR_DEVICE_PROFILE = "SI Soil Moisture Sensor";
    public static final String SI_FIELD_ASSET_PROFILE = "SI Field";

    //Devices
    public static final String SI_WATER_WATER_METER_1_DEVICE = "SI Water Meter 1";
    public static final String SI_WATER_WATER_METER_2_DEVICE = "SI Water Meter 2";
    public static final String SI_SMART_VALVE_1_DEVICE = "SI Smart Valve 1";
    public static final String SI_SMART_VALVE_2_DEVICE = "SI Smart Valve 2";
    public static final String SI_SOIL_MOISTURE_1_DEVICE = "SI Soil Moisture 1";
    public static final String SI_SOIL_MOISTURE_2_DEVICE = "SI Soil Moisture 2";
    public static final String SI_SOIL_MOISTURE_3_DEVICE = "SI Soil Moisture 3";
    public static final String SI_SOIL_MOISTURE_4_DEVICE = "SI Soil Moisture 4";
    public static final String SI_SOIL_MOISTURE_5_DEVICE = "SI Soil Moisture 5";
    public static final String SI_SOIL_MOISTURE_6_DEVICE = "SI Soil Moisture 6";
    public static final String SI_SOIL_MOISTURE_7_DEVICE = "SI Soil Moisture 7";
    public static final String SI_SOIL_MOISTURE_8_DEVICE = "SI Soil Moisture 8";

    //Assets
    public static final String SI_FIELD_1_ASSET = "SI Field 1";
    public static final String SI_FIELD_2_ASSET = "SI Field 2";

    //Dashboards
    public static final String IRRIGATION_MANAGEMENT_DASHBOARD = "Irrigation Management";

    //Entity groups
    public static final String SMART_IRRIGATION_DEVICE_GROUP = "Smart Irrigation";
    public static final String SMART_IRRIGATION_ASSET_GROUP = "Smart Irrigation";
    public static final String SMART_IRRIGATION_DASHBOARD_GROUP = "Smart Irrigation";

    //Scheduler events
    public static final String EVENING_SCHEDULER_EVENT = "Evening";
    public static final String MORNING_SCHEDULER_EVENT = "Morning";

    //Instances
    public static final String REMOTE_FARM_EDGE = "Remote Farm R1";
}
