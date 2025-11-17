/**
 * Widget Library Exports
 * Import all widgets to ensure they register themselves
 */

// Latest value widgets (10)
import './latest/ValueCard'
import './latest/ProgressBar'
import './latest/Gauge'
import './latest/LabelCard'
import './latest/BatteryLevel'
import './latest/SignalStrength'
import './latest/LEDIndicator'
import './latest/LiquidLevel'

// Timeseries widgets (8)
import './timeseries/LineChart'
import './timeseries/BarChart'
import './timeseries/PieChart'
import './timeseries/DoughnutChart'
import './timeseries/AreaChart'
import './timeseries/RadarChart'
import './timeseries/ScatterChart'

// Table widgets (3)
import './tables/EntitiesTable'
import './tables/TimeseriesTable'

// Alarm widgets (1)
import './alarm/AlarmList'

// Control widgets (3)
import './controls/SwitchControl'
import './controls/SliderControl'
import './controls/KnobControl'

// Static widgets (1)
import './static/HTMLCard'

// Export registry
export { widgetRegistry, registerWidget } from './widgetRegistry'
