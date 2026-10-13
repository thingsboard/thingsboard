#### Custom action function

<div class="divider"></div>
<br/>

*function ($event, $injector, entityId, entityName, serviceMap, tableConfig): void*

A JavaScript function performing custom action.

**Parameters:**

<ul style="max-width: 800px">
  {% include entity_group/action/custom_action_args %}
</ul>

<div class="divider"></div>

##### Examples
<br>

<div style="padding-left: 32px;"
     tb-help-popup="entity_group/action/examples_custom_action/custom_action_copy_entity_id"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Copy Entity Id to buffer">
</div>

<br>

<div style="padding-left: 32px;"
     tb-help-popup="entity_group/action/examples_custom_action/custom_action_copy_access_token"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Copy device access token to buffer">
</div>

<br>

<div style="padding-left: 32px;"
     tb-help-popup="entity_group/action/examples_custom_action/custom_action_delete_entity_confirm"
     tb-help-popup-placement="top"
     [tb-help-popup-style]="{maxHeight: '50vh', maxWidth: '50vw'}"
     trigger-style="font-size: 16px;"
     trigger-text="Delete Entity after confirmation">
</div>

<br>
<br>
