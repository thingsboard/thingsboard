#### Potential unexpected behavior with mixed processing strategies

When configuring the processing strategies, certain combinations can lead to unexpected behavior. Consider the following scenarios:

- **Skipping database storage**

  Choosing to disable attribute persistence introduces the risk of having only partial data available.
  For example, if a message is processed solely for real-time notifications via WebSockets and not stored in the database, then attribute queries might not reflect the data visible on the dashboard.

- **Disabling WebSocket (WS) updates**

  If WS updates are disabled, any changes to the attribute data won’t be pushed to dashboards (or other WS subscriptions).
  This means that even if a database is updated, dashboards may not display the updated data until browser page is reloaded.

- **Skipping calculated field recalculation**

  If attribute data is saved to the database while bypassing calculated field recalculation, the aggregated value may not update to reflect the saved data.
  Conversely, if the calculated field is recalculated with new data but the corresponding attribute value is not persisted in the database, the calculated field's value might include data that isn’t stored.

- **Different deduplication intervals across actions**

  When you configure different deduplication intervals for actions, the same incoming message might be processed differently for each action.
  For example, a message might be stored immediately in the Attributes table (if set to *On every message*) while not being present on a dashboard because its deduplication interval hasn’t elapsed.

- **Deduplication cache clearing**

  The deduplication mechanism uses a cache to track processed messages within each interval.
  For performance and system stability reasons, this cache is periodically cleared.
  As a result, if a cache entry is removed during the deduplication period, messages from the same originator may be processed more than once within that interval.
  This means deduplication should be used as a performance optimization rather than an absolute guarantee of single processing per interval.

We recommend using deduplication only when the occasional repeated processing is acceptable and won't cause system correctness issue or data inconsistencies.
