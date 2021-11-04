#### Sample Alarm widget

<div class="divider"></div>
<br/>

In the **Widgets Bundle** view, click the big “+” button at the bottom-right part of the screen and then click the “Create new widget type” button.<br>
Click the **Alarm Widget** button on the **Select widget type** popup.<br>
The **Widget Editor** will be opened, pre-populated with the content of the default **Alarm** template widget.

 - Replace content of the CSS tab in "Resources" section with the following one:

```css
.my-alarm-table th {
    text-align: left;
}
{:copy-code}
```

 - Put the following HTML code inside the HTML tab of "Resources" section:

```html
  <div fxFlex fxLayout="column" style="height: 100%;">
      <div>My first Alarm widget.</div>
      <table class="my-alarm-table" style="width: 100%;">
          <thead>
              <tr>
                  <th *ngFor="let dataKey of alarmSource?.dataKeys">{{dataKey.label}}</th> 
              <tr>          
          </thead>
          <tbody>
              <tr *ngFor="let alarm of alarms">
                  <td *ngFor="let dataKey of alarmSource?.dataKeys" 
                      [ngStyle]="getAlarmCellStyle(alarm, dataKey)">
                      {{getAlarmValue(alarm, dataKey)}}
                  </td>
              </tr>      
          </tbody>          
      </table>          
  </div>
{:copy-code}
```

 - Put the following JSON content inside the "Settings schema" tab of **Settings schema section**:

```json
{
    "schema": {
        "type": "object",
        "title": "AlarmTableSettings",
        "properties": {
            "alarmSeverityColorFunction": {
                "title": "Alarm severity color function: f(severity)",
                "type": "string",
                "default": "if(severity == 'CRITICAL') {return 'red';} else if (severity == 'MAJOR') {return 'orange';} else return 'green'; "
            }
        },
        "required": []
    },
    "form": [
        {
            "key": "alarmSeverityColorFunction",
            "type": "javascript"
        }
    ]
}
{:copy-code}
```

 - Put the following JavaScript code inside the "JavaScript" section:

```javascript
self.onInit = function() {
    var pageLink = self.ctx.pageLink();

    pageLink.typeList = self.ctx.widgetConfig.alarmTypeList;
    pageLink.statusList = self.ctx.widgetConfig.alarmStatusList;
    pageLink.severityList = self.ctx.widgetConfig.alarmSeverityList;
    pageLink.searchPropagatedAlarms = self.ctx.widgetConfig.searchPropagatedAlarms;

    self.ctx.defaultSubscription.subscribeForAlarms(pageLink, null);
    self.ctx.$scope.alarmSource = self.ctx.defaultSubscription.alarmSource;
    
    var alarmSeverityColorFunctionBody = self.ctx.settings.alarmSeverityColorFunction;
    if (typeof alarmSeverityColorFunctionBody === 'undefined' || !alarmSeverityColorFunctionBody.length) {
        alarmSeverityColorFunctionBody = "if(severity == 'CRITICAL') {return 'red';} else if (severity == 'MAJOR') {return 'orange';} else return 'green';";
    }
    
    var alarmSeverityColorFunction = null;
    try {
        alarmSeverityColorFunction = new Function('severity', alarmSeverityColorFunctionBody);
    } catch (e) {
        alarmSeverityColorFunction = null;
    }

    self.ctx.$scope.getAlarmValue = function(alarm, dataKey) {
        var alarmKey = dataKey.name;
        if (alarmKey === 'originator') {
            alarmKey = 'originatorName';
        }
        var value = alarm[alarmKey];
        if (alarmKey === 'createdTime') {
            return self.ctx.date.transform(value, 'yyyy-MM-dd HH:mm:ss');
        } else {
            return value;
        }
    }
    
    self.ctx.$scope.getAlarmCellStyle = function(alarm, dataKey) {
        var alarmKey = dataKey.name;
        if (alarmKey === 'severity' && alarmSeverityColorFunction) {
            var severity = alarm[alarmKey];
            var color = alarmSeverityColorFunction(severity);
            return {
                color: color  
            };
        } 
        return {};
    }
}

self.onDataUpdated = function() {
    self.ctx.$scope.alarms = self.ctx.defaultSubscription.alarms.data;
    self.ctx.detectChanges();
}
{:copy-code}
```

 - Click the **Run** button on the **Widget Editor Toolbar** in order to see the result in **Widget preview** section.

![image](${helpBaseUrl}/help/images/widget/editor/examples/alarm-widget-sample.png)

In this example, the **alarmSource** and **alarms** properties of <span trigger-style="fontSize: 16px;" trigger-text="<b>subscription</b>" tb-help-popup="widget/editor/widget_js_subscription_object"></span> are assigned to **$scope** and become accessible within HTML template.

Inside the HTML, a special [***ngFor**{:target="_blank"}](https://angular.io/api/common/NgForOf) structural angular directive is used in order to iterate over available alarm **dataKeys** of **alarmSource** and render corresponding columns.

The table rows are rendered by iterating over **alarms** array and corresponding cells rendered by iterating over **dataKeys**.

The function **getAlarmValue** is fetching alarm value and formatting **createdTime** alarm property using a [DatePipe{:target="_blank"}](https://angular.io/api/common/DatePipe) angular pipe accessible via **date** property of **ctx**.

The function **getAlarmCellStyle** is used to assign custom cell styles for each alarm cell.<br>In this example, we introduced new settings property called **alarmSeverityColorFunction** that contains function body returning color depending on alarm severity.

Inside the **getAlarmCellStyle** function there is corresponding invocation of **alarmSeverityColorFunction** with severity value in order to get color for alarm severity cell.

Note that in this code **onDataUpdated** function is implemented in order to update **alarms** property with latest alarms from subscription and invoke change detection using **detectChanges()** function.   

<br/>
<br/>
