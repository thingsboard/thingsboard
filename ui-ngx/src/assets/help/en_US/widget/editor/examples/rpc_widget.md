#### Sample RPC (Control) widget

<div class="divider"></div>
<br/>

In the **Widgets Bundle** view, click the big “+” button at the bottom-right part of the screen and then click the “Create new widget type” button.<br>
Click the **Control Widget** button on the **Select widget type** popup.<br>
The **Widget Editor** will open, pre-populated with default **Control** template widget content.

 - Clear content of the CSS tab of "Resources" section.
 - Put the following HTML code inside the HTML tab of "Resources" section:

```html
    <form #rpcForm="ngForm" (submit)="sendCommand()">
      <div class="mat-content mat-padding" fxLayout="column">
        <mat-form-field class="mat-block">
          <mat-label>RPC method</mat-label>
          <input matInput required name="rpcMethod" #rpcMethodField="ngModel" [(ngModel)]="rpcMethod"/>
          <mat-error *ngIf="rpcMethodField.hasError('required')">
            RPC method name is required.
          </mat-error>
        </mat-form-field>
        <mat-form-field class="mat-block">
          <mat-label>RPC params</mat-label>
          <input matInput required name="rpcParams" #rpcParamsField="ngModel" [(ngModel)]="rpcParams"/>
          <mat-error *ngIf="rpcParamsField.hasError('required')">
            RPC params is required.
          </mat-error>
        </mat-form-field>
        <button [disabled]="rpcForm.invalid || !rpcForm.dirty" mat-raised-button color="primary" type="submit" >
          Send RPC command
        </button>
        <div>
          <label>RPC command response</label>
          <div style="width: 100%; height: 100px; border: solid 2px gray" [innerHTML]="rpcCommandResponse">
          </div>
        </div>
      </div>
    </form>
{:copy-code}
```

 - Put the following JSON content inside the "Settings schema" tab of **Settings schema section**:

```json
{
    "schema": {
        "type": "object",
        "title": "Settings",
        "properties": {
            "oneWayElseTwoWay": {
                "title": "Is One Way Command",
                "type": "boolean",
                "default": true
            },
            "requestTimeout": {
                "title": "RPC request timeout",
                "type": "number",
                "default": 500
            }
        },
        "required": []
    },
    "form": [
        "oneWayElseTwoWay",
        "requestTimeout"
    ]
}
{:copy-code}
```

 - Put the following JavaScript code inside the "JavaScript" section:

```javascript
self.onInit = function() {
    
    self.ctx.$scope.sendCommand = function() {
        var rpcMethod = self.ctx.$scope.rpcMethod;
        var rpcParams = self.ctx.$scope.rpcParams;
        var timeout = self.ctx.settings.requestTimeout;
        var oneWayElseTwoWay = self.ctx.settings.oneWayElseTwoWay ? true : false;

        var commandObservable;
        if (oneWayElseTwoWay) {
            commandObservable = self.ctx.controlApi.sendOneWayCommand(rpcMethod, rpcParams, timeout);
        } else {
            commandObservable = self.ctx.controlApi.sendTwoWayCommand(rpcMethod, rpcParams, timeout);
        }
        commandObservable.subscribe(
            function (response) {
                if (oneWayElseTwoWay) {
                    self.ctx.$scope.rpcCommandResponse = "Command was successfully received by device.<br> No response body because of one way command mode.";
                } else {
                    self.ctx.$scope.rpcCommandResponse = "Response from device:<br>";                    
                    self.ctx.$scope.rpcCommandResponse += JSON.stringify(response, undefined, 2);
                }
                self.ctx.detectChanges();
            },
            function (rejection) {
                self.ctx.$scope.rpcCommandResponse = "Failed to send command to the device:<br>"
                self.ctx.$scope.rpcCommandResponse += "Status: " + rejection.status + "<br>";
                self.ctx.$scope.rpcCommandResponse += "Status text: '" + rejection.statusText + "'";
                self.ctx.detectChanges();
            }
            
        );
    }
    
}
{:copy-code}
```

 - Fill **Widget title** field with widget type name, for ex. "My first control widget".
 - Click the **Run** button on the **Widget Editor Toolbar** in order to see the result in **Widget preview** section.
 - Click dashboard edit button on the preview section to change the size of the resulting widget. Then click dashboard apply button. The final widget should look like the image below.

![image](${helpBaseUrl}/help/images/widget/editor/examples/control-widget-sample.png)

- Click the **Save** button on the **Widget Editor Toolbar** to save widget type.

To test how this widget performs RPC commands, we will need to place it in a dashboard then bind it to a device working with RPC commands. To do this, perform the following steps:

- Login as Tenant administrator.
- Navigate to **Devices** and create new device with some name, for ex. "My RPC Device".
- Open device details and click "Copy Access Token" button to copy device access token to clipboard.
- Download [mqtt-js-rpc-from-server.sh{:target="_blank"}](${siteBaseUrl}/docs/reference/resources/mqtt-js-rpc-from-server.sh) and [mqtt-js-rpc-from-server.js{:target="_blank"}](${siteBaseUrl}/docs/reference/resources/mqtt-js-rpc-from-server.js). Place these files in a folder.
  Edit **mqtt-js-rpc-from-server.sh** - replace **$ACCESS_TOKEN** with your device access token from the clipboard. And install mqtt client library.
- Run **mqtt-js-rpc-from-server.sh** script. You should see a "connected" message in the console.
- Navigate to **Dashboards** and create a new dashboard with some name, for ex. "My first control dashboard". Open this dashboard.
- Click dashboard "edit" button. In the dashboard edit mode, click the "Entity aliases" button located on the dashboard toolbar.

![image](${helpBaseUrl}/help/images/widget/editor/examples/dashboard-toolbar-entity-aliases.png)

- Inside **Entity aliases** popup click "Add alias".
- Fill "Alias name" field, for ex. "My RPC Device Alias".
- Select "Entity list" in "Filter type" field.
- Choose "Device" in "Type" field.
- Select your device in "Entity list" field. In this example "My RPC Device".

![image](${helpBaseUrl}/help/images/widget/editor/examples/add-rpc-device-alias.png)

- Click "Add" and then "Save" in **Entity aliases**.
- Click dashboard "+" button then click "Create new widget" button.

![image](${helpBaseUrl}/help/images/widget/editor/examples/dashboard-create-new-widget-button.png)

- Then select **Widget Bundle** where your RPC widget was saved. Select "Control widget" tab.
- Click your widget. In this example, "My first control widget".
- From **Add Widget** popup, select your device alias in **Target device** section. In this example "My RPC Device Alias".
- Click **Add**. Your Control widget will appear in the dashboard. Click dashboard **Apply changes** button to save dashboard and leave editing mode.
- Fill **RPC method** field with RPC method name. For ex. "TestMethod".
- Fill **RPC params** field with RPC params. For ex. "{ param1: "value1" }".
- Click **Send RPC command** button. You should see the following response in the widget.

![image](${helpBaseUrl}/help/images/widget/editor/examples/control-widget-sample-response-one-way.png)

The following output should be printed in the device console:

```bash   
  request.topic: v1/devices/me/rpc/request/0
  request.body: {"method":"TestMethod","params":"{ param1: \"value1\" }"}
```

In order to test "Two way" RPC command mode, we need to change the corresponding widget settings property. To do this, perform the following steps:

- Click dashboard "edit" button. In dashboard edit mode, click **Edit widget** button located in the header of Control widget.
- In the widget details, view select "Advanced" tab and uncheck "Is One Way Command" checkbox.

![image](${helpBaseUrl}/help/images/widget/editor/examples/control-widget-sample-settings.png)

- Click **Apply changes** button on the widget details header. Close details and click dashboard **Apply changes** button.
- Fill widget fields with RPC method name and params like in previous steps.
  Click **Send RPC command** button. You should see the following response in the widget.

![image](${helpBaseUrl}/help/images/widget/editor/examples/control-widget-sample-response-two-way.png)

- stop **mqtt-js-rpc-from-server.sh** script.
  Click **Send RPC command** button. You should see the following response in the widget.

![image](${helpBaseUrl}/help/images/widget/editor/examples/control-widget-sample-response-timeout.png)

In this example, **controlApi** is used to send RPC commands. Additionally, custom widget settings were introduced in order to configure RPC command mode and RPC request timeout.

The response from the device is handled by **commandObservable**.  It has success and failed callbacks with corresponding response, or rejection objects containing information about request execution result.     

<br>
<br>
