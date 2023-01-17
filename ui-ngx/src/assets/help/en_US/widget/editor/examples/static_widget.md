#### Sample Static widget

<div class="divider"></div>
<br/>

In the **Widgets Bundle** view, click the big “+” button at the bottom-right part of the screen and then click the “Create new widget type” button.<br>
Click the **Static Widget** button on the **Select widget type** popup.<br>
The **Widget Editor** will be opened pre-populated with the content of default **Static** template widget.

 - Put the following HTML code inside the HTML tab of "Resources" section:

```html
  <div fxFlex fxLayout="column" style="height: 100%;" fxLayoutAlign="space-around stretch">
    <h3 style="text-align: center;">My first static widget.</h3>
    <button mat-raised-button color="primary" (click)="showAlert()">Click me</button>
  </div>
{:copy-code}
```

 - Put the following JSON content inside the "Settings schema" tab of **Settings schema section**:

```json
{
    "schema": {
        "type": "object",
        "title": "Settings",
        "properties": {
            "alertContent": {
                "title": "Alert content",
                "type": "string",
                "default": "Content derived from alertContent property of widget settings."
            }
        }
    },
    "form": [
        "alertContent"
    ]
}
{:copy-code}
```

 - Put the following JavaScript code inside the "JavaScript" section:

```javascript
self.onInit = function() {

    self.ctx.$scope.showAlert = function() {
        var alertContent = self.ctx.settings.alertContent;
        if (!alertContent) {
            alertContent = "Content derived from alertContent property of widget settings.";
        }
        window.alert(alertContent);  
    };

}
{:copy-code}
```

 - Click the **Run** button on the **Widget Editor Toolbar** to see the resulting **Widget preview** section.

![image](${helpBaseUrl}/help/images/widget/editor/examples/static-widget-sample.png)

This is just a static HTML widget.  There is no subscription data and no special widget API was used.

Only custom **showAlert** function was implemented showing an alert with the content of **alertContent** property of widget settings.

You can switch to dashboard edit mode in **Widget preview** section and change value of **alertContent** by changing widget settings in the "Advanced" tab of widget details.

Then you can see that the new alert content is displayed. 

<br/>
<br/>
