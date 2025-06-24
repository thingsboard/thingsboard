#### Sample Static widget

<div class="divider"></div>
<br/>

In the **Widgets Bundle** view, click the big “+” button at the bottom-right part of the screen and then click the “Create new widget type” button.<br>
Click the **Static Widget** button on the **Select widget type** popup.<br>
The **Widget Editor** will be opened pre-populated with the content of default **Static** template widget.

 - Put the following HTML code inside the HTML tab of "Resources" section:

```html
  <div class="flex h-full flex-1 flex-col items-stretch justify-around">
    <h3 style="text-align: center;">My first static widget.</h3>
    <button mat-raised-button color="primary" (click)="showAlert()">Click me</button>
  </div>
{:copy-code}
```

 - Import the following JSON content inside the "Settings form" tab by clicking on 'Import form from JSON' button:

```json
[
  {
    "id": "alertContent",
    "name": "Alert content",
    "type": "text",
    "default": "Content derived from alertContent property of widget settings.",
    "fieldClass": "flex"
  }
]
{:copy-code}
```

 - Clear value of 'Settings form selector' in the "Widget settings" tab.

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
