#### Subscription object

<div class="divider"></div>
<br/>

The widget subscription object is instance of [IWidgetSubscription{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/core/api/widget-api.models.ts#L264") and contains all subscription information, including current data, according to the [widget type{:target="_blank"}](${siteBaseUrl}/docs/user-guide/ui/widget-library/#widget-types).

Depending on widget type, subscription object provides different data structures.
For [Latest values{:target="_blank"}](${siteBaseUrl}/docs/user-guide/ui/widget-library/#latest-values) and [Time-series{:target="_blank"}](${siteBaseUrl}/docs/user-guide/ui/widget-library/#time-series) widget types, it provides the following properties:

- **datasources** - array of datasources (Array<[Datasource{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/shared/models/widget.models.ts#L279)>) used by this subscription, using the following structure:

```javascript
    datasources = [
        {  // datasource
           type: 'entity',// type of the datasource. Can be "function" or "entity"
           name: 'name', // name of the datasource (in case of "entity" usually Entity name)
           aliasName: 'aliasName', // name of the alias used to resolve this particular datasource Entity
           entityName: 'entityName', // name of the Entity used as datasource
           entityType: 'DEVICE', // datasource Entity type (for ex. "DEVICE", "ASSET", "TENANT", etc.)
           entityId: '943b8cd0-576a-11e7-824c-0b1cb331ec92', // entity identificator presented as string uuid. 
           dataKeys: [ //  array of keys (Array<DataKey>) (attributes or timeseries) of the entity used to fetch data 
               { // dataKey
                    name: 'name', // the name of the particular entity attribute/timeseries 
                    type: 'timeseries', // type of the dataKey. Can be "timeseries", "attribute" or "function" 
                    label: 'Sin', // label of the dataKey. Used as display value (for ex. in the widget legend section) 
                    color: '#ffffff', // color of the key. Can be used by widget to set color of the key data (for ex. lines in line chart or segments in the pie chart).  
                    funcBody: "", // only applicable for datasource with type "function" and "function" key type. Defines body of the function to generate simulated data.
                    settings: {} // dataKey specific settings with structure according to the defined Data key settings json schema. See "Settings schema section".
               },
               //...
           ]
        },
        //...
    ]
```

- **data** - array of latest data (Array<[DatasourceData{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/shared/models/widget.models.ts#L310)>) received in scope of this subscription, using the following structure:

```javascript
    data = [
        {
            datasource: {}, // datasource object of this data. See datasource structure above.
            dataKey: {}, // dataKey for which the data is held. See dataKey structure above.
            data: [ // array of data points
                [   // data point
                    1498150092317, // unix timestamp of datapoint in milliseconds
                    1, // value, can be either string, numeric or boolean  
                ],
                //...
            ]  
        },
        //...
    ]     
```

For [Alarm widget{:target="_blank"}](${siteBaseUrl}/docs/user-guide/ui/widget-library/#alarm-widget) type it provides the following properties:

- **alarmSource** - ([Datasource{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/shared/models/widget.models.ts#L279)) information about entity for which alarms are fetched, using the following structure:

```javascript
    alarmSource = {
         type: 'entity',// type of the alarm source. Can be "function" or "entity"
         name: 'name', // name of the alarm source (in case of "entity" usually Entity name)
         aliasName: 'aliasName', // name of the alias used to resolve this particular alarm source Entity
         entityName: 'entityName', // name of the Entity used as alarm source
         entityType: 'DEVICE', // alarm source Entity type (for ex. "DEVICE", "ASSET", "TENANT", etc.)
         entityId: '943b8cd0-576a-11e7-824c-0b1cb331ec92', // entity identificator presented as string uuid. 
         dataKeys: [ // array of keys indicating alarm fields used to display alarms data 
            { // dataKey
                 name: 'name', // the name of the particular alarm field 
                 type: 'alarm', // type of the dataKey. Only "alarm" in this case. 
                 label: 'Severity', // label of the dataKey. Used as display value (for ex. as a column title in the Alarms table) 
                 color: '#ffffff', // color of the key. Can be used by widget to set color of the key data.  
                 settings: {} // dataKey specific settings with structure according to the defined Data key settings json schema. See "Settings schema section".
            },
            //...
          ] 
    }
```

- **alarms** - array of alarms (Array<[Alarm{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/shared/models/alarm.models.ts#L89)>) received in scope of this subscription, using the following structure:

```javascript
    alarms = [
        { // alarm
            id: { // alarm id 
                entityType: "ALARM", 
                id: "943b8cd0-576a-11e7-824c-0b1cb331ec92"
            },
            createdTime: 1498150092317, // Alarm created time (unix timestamp)
            startTs: 1498150092316, // Alarm started time (unix timestamp)
            endTs: 1498563899065, // Alarm end time (unix timestamp)
            ackTs: 0, // Time of alarm acknowledgment (unix timestamp)
            clearTs: 0, // Time of alarm clear (unix timestamp)
            originator: { // Originator - id of entity produced this alarm 
                entityType: "ASSET", 
                id: "ceb16a30-4142-11e7-8b30-d5d66714ea5a"
            },
            originatorName: "Originator Name", // Name of originator entity
            type: "Temperature", // Type of the alarm            
            severity: "CRITICAL", // Severity of the alarm ("CRITICAL", "MAJOR", "MINOR", "WARNING", "INDETERMINATE") 
            status: "ACTIVE_UNACK", // Status of the alarm 
                                    // ("ACTIVE_UNACK" - active unacknowledged, 
                                    // "ACTIVE_ACK" - active acknowledged, 
                                    // "CLEARED_UNACK" - cleared unacknowledged, 
                                    // "CLEARED_ACK" - cleared acknowledged)
            details: {} // Alarm details object derived from alarm details json.
        }
    ]               
```

For [RPC{:target="_blank"}](${siteBaseUrl}/docs/user-guide/ui/widget-library/#rpc-control-widget) or [Static{:target="_blank"}](${siteBaseUrl}/docs/user-guide/ui/widget-library/#static) widget types, subscription object is optional and does not contain necessary information.
