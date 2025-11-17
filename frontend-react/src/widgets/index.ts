/**
 * Widget Library Exports
 * Import all widgets to ensure they register themselves
 *
 * Total Widgets: 35
 * - Latest value widgets: 13
 * - Timeseries widgets: 10
 * - Table widgets: 2
 * - Alarm widgets: 1
 * - Control widgets: 6
 * - Static widgets: 4
 * - Map widgets: 2
 */

// Latest value widgets (13)
import './latest/ValueCard'
import './latest/ProgressBar'
import './latest/Gauge'
import './latest/LabelCard'
import './latest/BatteryLevel'
import './latest/SignalStrength'
import './latest/LEDIndicator'
import './latest/LiquidLevel'
import './latest/Speedometer'
import './latest/Compass'
import './latest/RadialGauge'
import './latest/StatisticsCard'
import './latest/TrendIndicator'

// Timeseries widgets (10)
import './timeseries/LineChart'
import './timeseries/BarChart'
import './timeseries/PieChart'
import './timeseries/DoughnutChart'
import './timeseries/AreaChart'
import './timeseries/RadarChart'
import './timeseries/ScatterChart'
import './timeseries/FunnelChart'
import './timeseries/HeatmapChart'
import './timeseries/TreemapChart'

// Table widgets (2)
import './tables/EntitiesTable'
import './tables/TimeseriesTable'

// Alarm widgets (1)
import './alarm/AlarmList'

// Control widgets (6)
import './controls/SwitchControl'
import './controls/SliderControl'
import './controls/KnobControl'
import './controls/PowerButton'
import './controls/MultiButton'
import './controls/GPIOPanel'

// Static widgets (4)
import './static/HTMLCard'
import './static/ImageViewer'
import './static/VideoPlayer'
import './static/QRCode'

// Map widgets (2)
import './maps/GoogleMap'
import './maps/OpenStreetMap'

// Export registry
export { widgetRegistry, registerWidget } from './widgetRegistry'
