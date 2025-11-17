/**
 * Widget Library Exports
 * Import all widgets to ensure they register themselves
 */

// Latest value widgets
import './latest/ValueCard'
import './latest/ProgressBar'
import './latest/Gauge'
import './latest/LabelCard'

// Timeseries widgets
import './timeseries/LineChart'
import './timeseries/BarChart'
import './timeseries/PieChart'
import './timeseries/DoughnutChart'

// Table widgets
import './tables/EntitiesTable'

// Alarm widgets
import './alarm/AlarmList'

// Export registry
export { widgetRegistry, registerWidget } from './widgetRegistry'
