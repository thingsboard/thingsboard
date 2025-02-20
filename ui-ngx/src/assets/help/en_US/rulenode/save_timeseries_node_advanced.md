#### Potential unexpected behavior with mixed processing strategies

When configuring the processing strategies, certain combinations can lead to unexpected behavior. Consider the following scenarios:

- **Disabling WebSocket (WS) updates**

  If WS updates are disabled, any changes to the time series data won’t be pushed to dashboards (or other WS subscriptions).
  This means that even if a database is updated, dashboards may not display the updated data until browser page is reloaded.

- **Different deduplication intervals across actions**

  When you configure different deduplication intervals for actions, the same incoming message might be processed differently for each action.
  For example, a message might be stored immediately in the Time series table (if set to *On every message*) while not being stored in the Latest values table because its deduplication interval hasn’t elapsed.
  Also, if the WebSocket updates are configured with a different interval, dashboards might show updates that do not match what is stored in the database.

- **Skipping database storage**

  Choosing to disable one or more persistence actions (for instance, skipping database storage for Time series or Latest values while keeping WS updates enabled) introduces the risk of having only partial data available:
  - If a message is processed only for real-time notifications (WebSockets) and not stored in the database, historical queries may not match data on the dashboard.
  - When processing strategies for Time series and Latest values are out-of-sync, telemetry data may be stored in one table (e.g., Time series) while the same data is absent in the other (e.g., Latest values).

- **Deduplication cache clearing**

  The deduplication mechanism uses a cache to track processed messages within each interval.
  For performance and system stability reasons, this cache is periodically cleared.
  As a result, if a cache entry is removed during the deduplication period, messages from the same originator may be processed more than once within that interval.
  This means deduplication should be used as a performance optimization rather than an absolute guarantee of single processing per interval.

We recommend using deduplication only when the occasional repeated processing is acceptable and won't cause system correctness issue or data inconsistencies.
