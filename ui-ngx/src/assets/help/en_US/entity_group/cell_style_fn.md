#### Cell style function

<div class="divider"></div>
<br/>

*function (value, entity): {[key: string]: string}*

A JavaScript function used to compute entity cell style depending on entity field value.

**Parameters:**

<ul>
  <li><b>value:</b> <code>any</code> - An entity field value displayed in the cell.
  </li>
  <li><b>entity:</b> <code>object</code> - An 
            <span trigger-style="fontSize: 16px;" trigger-text="<b>ShortEntityView</b>" tb-help-popup="entity_group/short_entity_view_obj"></span> object
            presenting basic entity properties (ex. <code>id</code>, <code>name</code>) and provides access to entity<br>
            fields/attributes/timeseries configured in entity group <b>Columns</b> configuration.
  </li>
</ul>

**Returns:**

Should return key/value object presenting style attributes.

<div class="divider"></div>

##### Examples

* Set color depending on device temperature value:

```javascript
var temperature = value;
var color = 'black';
if (temperature) {
    if (temperature > 25) {
      color = 'red';
    } else {
      color = 'green';
    }
}
return {
  fontWeight: 'bold',
  color: color
};
{:copy-code}
```

<br>
<br>
