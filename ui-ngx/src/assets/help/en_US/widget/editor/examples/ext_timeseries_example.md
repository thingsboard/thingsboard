#### Time-Series widget Example with Chart.js library

<div class="divider"></div>
<br/>

In this example, **Time-Series** line chart widget will be created using external [Chart.js{:target="_blank"}](https://www.chartjs.org/) library.

In the **Widgets Bundle** view, click the big “+” button at the bottom-right part of the screen, then click the “Create new widget type” button.<br>
Click the **Time-Series** button on the **Select widget type** popup.<br>
The **Widget Editor** will be opened, pre-populated with the content of default **Time-Series** template widget.

 - Open **Resources** tab and click "Add" then insert the following link:

```  
https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.9.3/Chart.min.js
{:copy-code}
```

 - Clear content of the CSS tab of "Resources" section.
 - Put the following HTML code inside the HTML tab of "Resources" section:

```html
  <canvas id="myChart"></canvas>
{:copy-code}
```

 - Put the following JavaScript code inside the "JavaScript" section:

```javascript
var myChart;

self.onInit = function() {
    
    var chartData = {
        datasets: []
    };

    for (var i=0; i < self.ctx.data.length; i++) {
        var dataKey = self.ctx.data[i].dataKey;
        var dataset = {
            label: dataKey.label,
            data: [],
            borderColor: dataKey.color,
            fill: false
        };
        chartData.datasets.push(dataset);
    }
    
    var options = {
        maintainAspectRatio: false,
        legend: {
            display: false
        },
        scales: {
        xAxes: [{
            type: 'time',
            ticks: {
                maxRotation: 0,
                autoSkipPadding: 30
            }
        }]
    }
    };
    
    var canvasElement = $('#myChart', self.ctx.$container)[0];
    var canvasCtx = canvasElement.getContext('2d');
    myChart = new Chart(canvasCtx, {
        type: 'line',
        data: chartData,
        options: options
    });
    self.onResize();
}

self.onResize = function() {
    myChart.resize();
}

self.onDataUpdated = function() {
    for (var i = 0; i < self.ctx.data.length; i++) {
        var datasourceData = self.ctx.data[i];
        var dataSet = datasourceData.data;
        myChart.data.datasets[i].data.length = 0;
        var data = myChart.data.datasets[i].data;
        for (var d = 0; d < dataSet.length; d++) {
            var tsValuePair = dataSet[d];
            var ts = tsValuePair[0];
            var value = tsValuePair[1];
            data.push({t: ts, y: value});
        }
    }
    myChart.options.scales.xAxes[0].ticks.min = self.ctx.timeWindow.minTime;
    myChart.options.scales.xAxes[0].ticks.max = self.ctx.timeWindow.maxTime;
    myChart.update();
}
{:copy-code}
```

 - Click the **Run** button on the **Widget Editor Toolbar** in order to see the result in **Widget preview** section.

![image](${helpBaseUrl}/help/images/widget/editor/examples/external-js-timeseries-widget-sample.png)

In this example, the external JS library API was used that becomes available after injecting the corresponding URL in **Resources** section.

Initially chart datasets prepared using configured dataKeys from **data** property of **ctx**.

In the **onDataUpdated** function datasources data converted to Chart.js line chart format and pushed to chart datasets.

Please note that xAxis (time axis) is limited to current timewindow bounds obtained from **timeWindow** property of **ctx**.  

<br/>
<br/>
