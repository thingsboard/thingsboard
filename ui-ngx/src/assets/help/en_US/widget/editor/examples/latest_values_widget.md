#### Sample Latest Values widget

<div class="divider"></div>
<br/>

In the **Widgets Bundle** view, click the big “+” button at the bottom-right part of the screen and then click the “Create new widget type” button.<br>
Click the **Latest Values** button on the **Select widget type** popup.<br>
The **Widget Editor** will open, pre-populated with the content of the default **Latest Values** template widget.

 - Clear content of the CSS tab of "Resources" section.
 - Put the following HTML code inside the HTML tab of "Resources" section:

```html
  <div fxFlex fxLayout="column" style="height: 100%;" fxLayoutAlign="center stretch">
    <div>My first latest values widget.</div>
    <div fxFlex fxLayout="row" *ngFor="let dataKeyData of data" fxLayoutAlign="space-around center">
        <div>{{dataKeyData.dataKey.label}}:</div>
        <div>{{(dataKeyData.data[0] && dataKeyData.data[0][0]) | date : 'yyyy-MM-dd HH:mm:ss' }}</div>
        <div>{{dataKeyData.data[0] && dataKeyData.data[0][1]}}</div>
    </div>
  </div>
{:copy-code}
```

 - Put the following JavaScript code inside the "JavaScript" section:

```javascript
    self.onInit = function() {
       self.ctx.$scope.data = self.ctx.defaultSubscription.data;
    }
        
    self.onDataUpdated = function() {
        self.ctx.detectChanges();
    }
{:copy-code}
```

 - Click the **Run** button on the **Widget Editor Toolbar** in order to see the result in **Widget preview** section.

![image](${helpBaseUrl}/help/images/widget/editor/examples/latest-values-widget-sample.png)

In this example, the **data** property of <span trigger-style="fontSize: 16px;" trigger-text="<b>subscription</b>" tb-help-popup="widget/editor/widget_js_subscription_object"></span> is assigned to the **$scope** and becomes accessible within the HTML template.

Inside the HTML, a special [***ngFor**{:target="_blank"}](https://angular.io/api/common/NgForOf) structural angular directive is used in order to iterate over available dataKeys & datapoints then render latest values with their corresponding timestamps. 

<br/>
<br/>
