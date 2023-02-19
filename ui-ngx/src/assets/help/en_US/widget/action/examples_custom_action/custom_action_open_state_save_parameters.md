#### Function open state conditionally with saving particular state parameters

```javascript
{:code-style="max-height: 400px;"}
var entitySubType;
var $injector = widgetContext.$scope.$injector;
$injector.get(widgetContext.servicesMap.get('entityService')).getEntity(entityId.entityType, entityId.id)
  .subscribe(function(data) {
    entitySubType = data.type;
    if (entitySubType == 'energy meter') {
      openDashboardStates('energy_meter_details_view');
    } else if (entitySubType == 'thermometer') {
      openDashboardStates('thermometer_details_view');
    }
  });

function openDashboardStates(statedId) {
  var stateParams = widgetContext.stateController.getStateParams();
  var params = {
    entityId: entityId,
    entityName: entityName
  };

  if (stateParams.city) {
    params.city = stateParams.city;
  }

  widgetContext.stateController.openState(statedId, params, false);
}
{:copy-code}
```

<br>
<br>
