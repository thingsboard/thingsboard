#### Sample Time-Series widget

<div class="divider"></div>
<br/>

In the **Widgets Bundle** view, click the big “+” button at the bottom-right part of the screen, then click the “Create new widget type” button.<br>
Click the **Time-Series** button on the **Select widget type** popup.<br>
The **Widget Editor** will open, pre-populated with default **Time-Series** template widget content.

 - Replace content of the CSS tab in "Resources" section with the following one:

```css
.my-data-table th {
    text-align: left;
}
{:copy-code}
```

 - Put the following HTML code inside the HTML tab of "Resources" section:

```html
  <mat-tab-group style="height: 100%;">
      <mat-tab *ngFor="let datasource of datasources; let $dsIndex = index" label="{{datasource.name}}">
          <table class="my-data-table" style="width: 100%;">
              <thead>
                  <tr>
                      <th>Timestamp</th>
                      <th *ngFor="let dataKeyData of datasourceData[$dsIndex]">{{dataKeyData.dataKey.label}}</th>
                  <tr>          
              </thead>
              <tbody>
                  <tr *ngFor="let data of datasourceData[$dsIndex][0].data; let $dataIndex = index">
                      <td>{{data[0] | date : 'yyyy-MM-dd HH:mm:ss'}}</td>
                      <td *ngFor="let dataKeyData of datasourceData[$dsIndex]">{{dataKeyData.data[$dataIndex] && dataKeyData.data[$dataIndex][1]}}</td>
                  </tr>      
              </tbody>          
          </table>          
      </mat-tab>       
  </mat-tab-group>
{:copy-code}
```

 - Put the following JavaScript code inside the "JavaScript" section:

```javascript
self.onInit = function() {
    self.ctx.widgetTitle = 'My first Time-Series widget';
    self.ctx.$scope.datasources = self.ctx.defaultSubscription.datasources;
    self.ctx.$scope.data = self.ctx.defaultSubscription.data;
    
    self.ctx.$scope.datasourceData = [];
    
    var currentDatasource = null;
    var currentDatasourceIndex = -1;
    
    for (var i=0;i<self.ctx.$scope.data.length;i++) {
        var dataKeyData = self.ctx.$scope.data[i];
        if (dataKeyData.datasource != currentDatasource) {
            currentDatasource = dataKeyData.datasource
            currentDatasourceIndex++;
            self.ctx.$scope.datasourceData[currentDatasourceIndex] = [];
            
        } 
        self.ctx.$scope.datasourceData[currentDatasourceIndex].push(dataKeyData);
    }
    self.ctx.updateWidgetParams();

}

self.onDataUpdated = function() {
    self.ctx.detectChanges();
}
{:copy-code}
```

 - Click the **Run** button on the **Widget Editor Toolbar** in order to see the result in **Widget preview** section.

![image](${helpBaseUrl}/help/images/widget/editor/examples/timeseries-widget-sample.png)

In this example, the <span trigger-style="fontSize: 16px;" trigger-text="<b>subscription</b>" tb-help-popup="widget/editor/widget_js_subscription_object"></span> **datasources** and **data** properties are assigned to **$scope** and become accessible within the HTML template.

The **$scope.datasourceData** property is introduced to map datasource specific dataKeys data by datasource index for flexible access within the HTML template.

Inside the HTML, a special [***ngFor**{:target="_blank"}](https://angular.io/api/common/NgForOf) structural angular directive is used in order to iterate over available datasources and render corresponding tabs.

Inside each tab, the table is rendered using dataKeys obtained from **datasourceData** scope property accessed by datasource index.<br>

Each table renders columns by iterating over all **dataKeyData** objects and renders all available datapoints by iterating over **data** array of each **dataKeyData** to render timestamps and values.

Note that in this code, **onDataUpdated** function is implemented with a call to **detectChanges** function necessary to perform new change detection cycle when new data is received.

<br/>
<br/>
