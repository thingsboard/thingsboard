#### Short entity view object

<div class="divider"></div>
<br/>

Presenting basic entity properties <code>id</code>, <code>name</code> and provides access to entity fields/attributes/timeseries<br> configured in entity group <b>Columns</b> configuration.<br>
Short entity view object has the following structure:

```typescript
{
    id: EntityId,
    name: string,
    [key: string]: any;
}
```
Where:
<ul>
  <li><b>id:</b> <code><a href="https://github.com/thingsboard/thingsboard/blob/07dca7ef6391f35d6c17d4437750aca8c4ad82cc/ui-ngx/src/app/shared/models/id/entity-id.ts#L21" target="_blank">EntityId</a></code> - An 
            entity id object with the following structure: <code>{id: string, entityType: EntityType}</code>.
  </li>
  <li><b>name:</b> <code>string</code> - An entity name.
  </li>
  <li><b>[key: string]:</b> <code>any</code> - All other properties - entity fields/attributes/timeseries configured in entity group <b>Columns</b> configuration.<br>
    Fetched timeseries/attribute values are added into properties using attribute/latest timeseries keys with scope prefix:
   <ol type="1">
    <li>
      shared attribute -> <code>shared_</code>
    </li>
    <li>
      client attribute -> <code>client_</code>
    </li>
    <li>
      server attribute -> <code>server_</code>
    </li>
    <li>
      telemetry -> <code>timeseries_</code>
    </li>
   </ol>
  </li>
</ul>
