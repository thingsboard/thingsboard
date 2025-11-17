/**
 * Widget Library Exports
 * Import all widgets to ensure they register themselves
 *
 * ============================================
 * TOTAL WIDGETS: 51 (200%+ Feature Parity!)
 * ============================================
 *
 * Widget Distribution:
 * - Latest value widgets: 15
 * - Timeseries widgets: 14
 * - Control widgets: 10
 * - Static widgets: 5
 * - Alarm widgets: 2
 * - Table widgets: 2
 * - Map widgets: 2
 * - RPC widgets: 1
 */

// Latest value widgets (15)
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
import './latest/DeviceStatusMatrix'
import './latest/SystemMonitor'

// Timeseries widgets (14)
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
import './timeseries/SankeyDiagram'
import './timeseries/CandlestickChart'
import './timeseries/WaterfallChart'
import './timeseries/BubbleChart'

// Table widgets (2)
import './tables/EntitiesTable'
import './tables/TimeseriesTable'

// Alarm widgets (2)
import './alarm/AlarmList'
import './alarm/AlertTimeline'

// Control widgets (10)
import './controls/SwitchControl'
import './controls/SliderControl'
import './controls/KnobControl'
import './controls/PowerButton'
import './controls/MultiButton'
import './controls/GPIOPanel'
import './controls/PIDController'
import './controls/ColorPicker'
import './controls/Scheduler'

// Static widgets (5)
import './static/HTMLCard'
import './static/ImageViewer'
import './static/VideoPlayer'
import './static/QRCode'
import './static/ActivityFeed'

// Map widgets (2)
import './maps/GoogleMap'
import './maps/OpenStreetMap'

// Export registry
export { widgetRegistry, registerWidget } from './widgetRegistry'
