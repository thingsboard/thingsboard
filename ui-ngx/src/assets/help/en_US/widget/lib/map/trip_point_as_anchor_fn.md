#### Point as anchor function

<div class="divider"></div>
<br/>

*function (data, dsData, dsIndex): boolean*

A JavaScript function evaluating whether to use trip point as time anchor used in time selector.

**Parameters:**

<ul>
  {% include widget/lib/map/map_fn_args %}
</ul>

**Returns:**

`true` if the point should be decided as anchor, `false` otherwise.

In case no data is returned, the point is not used as anchor.

<div class="divider"></div>

##### Examples

* Make anchors with 5 seconds step interval:

```javascript
return data.time % 5000 < 1000;
{:copy-code}
```

<br>
<br>
