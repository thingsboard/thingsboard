#### Node relations query function

<div class="divider"></div>
<br/>

*function (nodeCtx): [EntityRelationsQuery{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/dda61383933cac9aa6821a77ff9b19291e69db9f/ui-ngx/src/app/shared/models/relation.models.ts#L69) | 'default'*

A JavaScript function used to compute child nodes relations query for current node.

**Parameters:**

<ul>
  <li><b>nodeCtx:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a></code> - An 
            <a href="https://github.com/thingsboard/thingsboard/blob/e264f7b8ddff05bda85c4833bf497f47f447496e/ui-ngx/src/app/modules/home/components/widget/lib/entities-hierarchy-widget.models.ts#L35" target="_blank">HierarchyNodeContext</a> object
            containing <code>entity</code> field holding basic entity properties <br> (ex. <code>id</code>, <code>name</code>, <code>label</code>) and <code>data</code> field holding other entity attributes/timeseries declared in widget datasource configuration.
   </li>
</ul>

**Returns:**

Should return [EntityRelationsQuery{:target="_blank"}](https://github.com/thingsboard/thingsboard/blob/dda61383933cac9aa6821a77ff9b19291e69db9f/ui-ngx/src/app/shared/models/relation.models.ts#L69) for current node used to fetch entity children.<br>
Function can return `default` string value. In this case default relations query will be used.

<div class="divider"></div>

##### Examples

* Fetch child entities having relations of type `Contains` from the current entity:

```javascript
var entity = nodeCtx.entity;
var query = {
  parameters: {
    rootId: entity.id.id,
    rootType: entity.id.entityType,
    direction: "FROM",
    maxLevel: 1
  },
  filters: [{
    relationType: "Contains",
    entityTypes: []
  }]
};
return query;
{:copy-code}
```

<br>
<br>
