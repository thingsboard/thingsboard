#### Child entities filter function

<div class="divider"></div>
<br/>

*function Filter(attributes): boolean*

JavaScript function evaluating **true/false** condition on child entity attributes.

**Parameters:**

<ul>
  <li><b>attributes:</b> <code>{[key: string]: any}</code> - map containing fetched attributes/latest timeseries values as input argument.<br>
Fetched attribute values are added into attributes map using attribute/latest timeseries keys with scope prefix:
   <ol type="1">
    <li>
      shared attribute -> <code>shared_</code>
    </li>
    <li>
      client attribute -> <code>cs_</code>
    </li>
    <li>
      server attribute -> <code>ss_</code>
    </li>
    <li>
      telemetry -> no prefix used
    </li>
   </ol>
  </li>
</ul>


**Returns:**

Should return `boolean` value. If `true` - use child entity attributes for aggregation, otherwise ignore child entity attributes.

<div class="divider"></div>

##### Examples

* Include only child entities with client attribute `state` having value `true`:

```javascript
return attributes['cs_state'] === 'true';
{:copy-code}
```

<br>
<br>

