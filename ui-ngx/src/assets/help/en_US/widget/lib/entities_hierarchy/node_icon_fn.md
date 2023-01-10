#### Node icon function

<div class="divider"></div>
<br/>

*function (nodeCtx): {iconUrl?: string, materialIcon?: string} | 'default'*

A JavaScript function used to compute node icon info.

**Parameters:**

<ul>
  <li><b>nodeCtx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a> object
            containing <code>entity</code> field holding basic entity properties <br> (ex. <code>id</code>, <code>name</code>, <code>label</code>) and <code>data</code> field holding other entity attributes/timeseries declared in widget datasource configuration.
   </li>
</ul>

**Returns:**

Should return node icon info object with the following structure:

```typescript
{
    iconUrl?: string,
    materialIcon?: string
}
```
Resulting object should contain either `materialIcon` or `iconUrl` property.<br>
Where:
 - `materialIcon` - name of the material icon to be used from the [Material Icons Library{:target="_blank"}](https://material.io/tools/icons);
 - `iconUrl` - url of the external image to be used as node icon.

Function can return `default` string value. In this case default icons according to entity type will be used.

<div class="divider"></div>

##### Examples

* Use external image for devices which name starts with `Test` and use default icons for the rest of entities:

```javascript
var entity = nodeCtx.entity;
if (entity.id.entityType === 'DEVICE' && entity.name.startsWith('Test')) {
  return {iconUrl: 'https://avatars1.githubusercontent.com/u/14793288?v=4&s=117'};
} else {
  return 'default';
}
{:copy-code}
```

<br>
<br>
