#### Latest Values widget Example with gauge.js library

<div class="divider"></div>
<br/>

In this example, **Latest Values** gauge widget will be created using external [gauge.js{:target="_blank"}](http://bernii.github.io/gauge.js/) library.

In the **Widgets Bundle** view, click the big “+” button at the bottom-right part of the screen, then click the “Create new widget type” button.<br>
Click the **Latest Values** button on the **Select widget type** popup.<br>
The **Widget Editor** will be opened, pre-populated with the content of default **Latest Values** template widget.

 - Open **Resources** tab and click "Add" then insert the following link:

```  
https://bernii.github.io/gauge.js/dist/gauge.min.js
{:copy-code}
```

 - Clear content of the CSS tab of "Resources" section.
 - Put the following HTML code inside the HTML tab of "Resources" section:

```html
  <canvas id="my-gauge"></canvas>
{:copy-code}
```

 - Put the following JavaScript code inside the "JavaScript" section:

```javascript
var canvasElement;
var gauge;

self.onInit = function() {
    canvasElement = $('#my-gauge', self.ctx.$container)[0];
    gauge = new Gauge(canvasElement);
    gauge.minValue = -1000; 
    gauge.maxValue = 1000; 
    gauge.animationSpeed = 16; 
    self.onResize();
}

self.onResize = function() {
    canvasElement.width = self.ctx.width;
    canvasElement.height = self.ctx.height;
    gauge.update(true);
    gauge.render();
}

self.onDataUpdated = function() {
    if (self.ctx.defaultSubscription.data[0].data.length) {
        var value = self.ctx.defaultSubscription.data[0].data[0][1];
        gauge.set(value);
    }
}
{:copy-code}
```

 - Click the **Run** button on the **Widget Editor Toolbar** in order to see the result in **Widget preview** section.

![image](${helpBaseUrl}/help/images/widget/editor/examples/external-js-widget-sample.png)

In this example, the external JS library API was used that becomes available after injecting the corresponding URL in **Resources** section.

The value displayed was obtained from <span trigger-style="fontSize: 16px;" trigger-text="<b>subscription</b>" tb-help-popup="widget/editor/widget_js_subscription_object"></span> **data** property for the first dataKey. 

<br/>
<br/>
