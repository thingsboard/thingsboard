#### Action sources object

<div class="divider"></div>
<br/>

Map describing available widget action sources ([WidgetActionSource{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/2627fe51d491055d4140f16617ed543f7f5bd8f6/ui-ngx/src/app/shared/models/widget.models.ts#L121)) to which user actions can be assigned. It has the following structure:

```javascript
   return {
        'headerButton': { // Action source Id (unique action source identificator)
           name: 'widget-action.header-button', // Display name of action source, used in widget settings ('Actions' tab).
           value: 'headerButton', // Action source Id
           multiple: true // Boolean property indicating if this action source supports multiple action definitions
                          // (for ex. multiple buttons in one cell, or only one action can by assigned on table row click.)
        }
    };   
```
