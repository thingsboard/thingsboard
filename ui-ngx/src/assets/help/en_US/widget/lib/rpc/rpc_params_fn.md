#### RPC parameters function

<div class="divider"></div>
<br/>

*function (): any*

A JavaScript function returning the parameters (payload) of the RPC command used to get the value.

**Returns:**

Payload object or primitive to be used as the parameters of the RPC get value command.

<div class="divider"></div>

##### Examples

* Request the state of a specific pin:

```javascript
return { pin: 5 };
{:copy-code}
```

* Request several values with a single RPC call:

```javascript
return { keys: ['temperature', 'humidity'] };
{:copy-code}
```

<br>
<br>
