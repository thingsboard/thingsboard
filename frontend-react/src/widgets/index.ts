/**
 * Widget Library Exports
 * Import all widgets to ensure they register themselves
 */

// Latest value widgets
import './latest/ValueCard'

// Timeseries widgets
import './timeseries/LineChart'

// Alarm widgets
import './alarm/AlarmList'

// Export registry
export { widgetRegistry, registerWidget } from './widgetRegistry'
