#### Node has children function

<div class="divider"></div>
<br/>

*function (nodeCtx): boolean*

A JavaScript function evaluating whether current node has children (whether it can be expanded).

**Parameters:**

<ul>
  <li><b>nodeCtx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a> object
            containing <code>entity</code> field holding basic entity properties <br> (ex. <code>id</code>, <code>name</code>, <code>label</code>) and <code>data</code> field holding other entity attributes/timeseries declared in widget datasource configuration.
   </li>
</ul>

**Returns:**

`true` if node should have children, `false` otherwise.

<div class="divider"></div>

##### Examples

* Restrict entities hierarchy expansion up to third level:

```javascript
return nodeCtx.level <= 2;
{:copy-code}
```

* Restrict entities expansion according to the value of example `nodeHasChildren` attribute:

```javascript
var data = nodeCtx.data;
if (data.hasOwnProperty('nodeHasChildren') && data['nodeHasChildren'] !== null) {
  return data['nodeHasChildren'] === 'true';
} else {
  return true;
}
{:copy-code}
```

<br>
<br>
