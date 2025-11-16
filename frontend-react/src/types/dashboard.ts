/**
 * Dashboard and Widget Type Definitions
 * Based on ThingsBoard dashboard architecture
 */

// ============================================================================
// Entity and Time Series Types
// ============================================================================

export interface EntityId {
  id: string
  entityType: 'DEVICE' | 'ASSET' | 'ENTITY_VIEW' | 'TENANT' | 'CUSTOMER' | 'DASHBOARD' | 'USER'
}

export interface TimeseriesData {
  ts: number
  value: any
}

export interface LatestTelemetry {
  [key: string]: TimeseriesData[]
}

export interface AttributeData {
  key: string
  value: any
  lastUpdateTs?: number
}

// ============================================================================
// Widget Data Sources and Aliases
// ============================================================================

export type AliasFilterType =
  | 'singleEntity'
  | 'entityList'
  | 'entityName'
  | 'entityType'
  | 'stateEntity'
  | 'assetType'
  | 'deviceType'
  | 'edgeType'
  | 'entityViewType'
  | 'apiUsageState'
  | 'relationsQuery'

export interface EntityAlias {
  id: string
  alias: string
  filter: EntityAliasFilter
}

export interface EntityAliasFilter {
  type: AliasFilterType
  entityType?: string
  entityList?: string[]
  entityNameFilter?: string
  stateEntityParamName?: string
  defaultStateEntity?: EntityId
  resolveMultiple?: boolean
  singleEntity?: EntityId
  // Relations query
  rootStateEntity?: boolean
  stateEntityParamName?: string
  direction?: 'FROM' | 'TO'
  maxLevel?: number
  relationType?: string
  entityTypes?: string[]
}

export interface DataKey {
  name: string
  type: 'timeseries' | 'attribute' | 'entityField' | 'alarm' | 'function'
  label?: string
  color?: string
  settings?: Record<string, any>
  aggregationType?: 'NONE' | 'MIN' | 'MAX' | 'AVG' | 'SUM' | 'COUNT'
  funcBody?: string // For function type
  postFuncBody?: string
}

export interface Datasource {
  type: 'entity' | 'function'
  name?: string
  aliasName?: string
  entityAliasId?: string
  dataKeys: DataKey[]
  entityType?: string
  entityId?: string
  // Function datasource
  dataKeyType?: 'function'
}

// ============================================================================
// Time Window
// ============================================================================

export type TimewindowType = 'REALTIME' | 'HISTORY'

export interface Timewindow {
  displayValue?: string
  hideInterval?: boolean
  hideAggregation?: boolean
  hideAggInterval?: boolean
  hideTimezone?: boolean
  selectedTab?: number
  realtime?: {
    realtimeType?: number
    timewindowMs?: number
    interval?: number
    quickInterval?: string
  }
  history?: {
    historyType?: number
    timewindowMs?: number
    interval?: number
    fixedTimewindow?: {
      startTimeMs: number
      endTimeMs: number
    }
    quickInterval?: string
  }
  aggregation?: {
    type?: 'NONE' | 'MIN' | 'MAX' | 'AVG' | 'SUM' | 'COUNT'
    limit?: number
  }
}

// ============================================================================
// Widget Settings and Configuration
// ============================================================================

export interface WidgetSettings {
  // Common settings
  backgroundColor?: string
  color?: string
  padding?: string
  margin?: string
  borderRadius?: string

  // Title settings
  showTitle?: boolean
  title?: string
  titleStyle?: React.CSSProperties
  titleIcon?: string

  // Value display
  showValue?: boolean
  valueFont?: {
    family?: string
    size?: number
    style?: 'normal' | 'italic' | 'oblique'
    weight?: string | number
    color?: string
  }
  decimals?: number
  units?: string

  // Chart specific
  showLegend?: boolean
  legendPosition?: 'top' | 'bottom' | 'left' | 'right'
  showTooltip?: boolean
  stack?: boolean
  smooth?: boolean
  fill?: boolean
  animation?: boolean

  // Gauge specific
  minValue?: number
  maxValue?: number
  gaugeType?: 'arc' | 'donut' | 'horizontalBar' | 'verticalBar'
  showMinMax?: boolean
  gaugeColor?: string
  sectors?: Array<{
    from: number
    to: number
    color: string
  }>

  // Table specific
  enableSearch?: boolean
  enableSorting?: boolean
  enableFilter?: boolean
  displayPagination?: boolean
  defaultPageSize?: number

  // Map specific
  defaultZoomLevel?: number
  defaultCenterPosition?: [number, number]
  mapProvider?: 'google-map' | 'openstreet-map' | 'here' | 'image-map'

  // Alarm specific
  alarmSeverityColors?: {
    CRITICAL?: string
    MAJOR?: string
    MINOR?: string
    WARNING?: string
    INDETERMINATE?: string
  }

  // Custom settings
  [key: string]: any
}

export interface WidgetAction {
  id: string
  name: string
  icon?: string
  type:
    | 'openDashboard'
    | 'openDashboardState'
    | 'updateDashboardState'
    | 'custom'
    | 'mobileAction'
  targetDashboardId?: string
  targetDashboardStateId?: string
  setEntityId?: boolean
  stateEntityParamName?: string
  customFunction?: string
  useCustomFunction?: boolean
}

export interface WidgetConfig {
  datasources: Datasource[]
  timewindow?: Timewindow
  showTitle?: boolean
  title?: string
  titleStyle?: React.CSSProperties
  titleIcon?: string
  showTitleIcon?: boolean
  iconColor?: string
  iconSize?: string
  titleTooltip?: string
  dropShadow?: boolean
  enableFullscreen?: boolean

  settings: WidgetSettings

  // Actions
  actions?: {
    [actionSourceId: string]: WidgetAction[]
  }

  // Mobile
  mobileHeight?: number
  mobileOrder?: number

  // Advanced
  decimals?: number
  units?: string
  useDashboardTimewindow?: boolean
  showLegend?: boolean

  // Custom
  [key: string]: any
}

// ============================================================================
// Widget Type Definitions
// ============================================================================

export type WidgetType =
  | 'timeseries' // Line charts, bar charts, etc.
  | 'latest' // Latest values, cards, gauges
  | 'rpc' // Control widgets (buttons, switches, sliders)
  | 'alarm' // Alarm widgets
  | 'static' // HTML, Markdown, Image

export type WidgetTypeId =
  // Timeseries charts
  | 'timeseries_line_chart'
  | 'timeseries_bar_chart'
  | 'timeseries_area_chart'
  | 'timeseries_pie_chart'
  | 'timeseries_doughnut_chart'
  | 'timeseries_radar_chart'
  | 'timeseries_scatter_chart'

  // Latest value
  | 'latest_value_card'
  | 'latest_gauge'
  | 'latest_digital_gauge'
  | 'latest_compass'
  | 'latest_label'
  | 'latest_entities_table'
  | 'latest_state'

  // Control
  | 'rpc_button'
  | 'rpc_switch'
  | 'rpc_slider'
  | 'rpc_knob'
  | 'rpc_round_switch'

  // Alarm
  | 'alarm_table'
  | 'alarm_count'

  // Static
  | 'static_html'
  | 'static_markdown'
  | 'static_image'

  // Maps
  | 'map_openstreetmap'
  | 'map_google'
  | 'map_route'

  // Custom
  | 'custom'

export interface WidgetTypeDescriptor {
  id: WidgetTypeId
  name: string
  type: WidgetType
  description?: string
  icon?: string
  image?: string
  defaultConfig: Partial<WidgetConfig>
  defaultSizeX?: number
  defaultSizeY?: number
}

// ============================================================================
// Widget Instance
// ============================================================================

export interface Widget {
  id: string
  typeId: WidgetTypeId
  type: WidgetType

  // Position and size (grid units)
  row: number
  col: number
  sizeX: number // width in grid units
  sizeY: number // height in grid units

  config: WidgetConfig

  // Mobile layout
  mobileHeight?: number
  mobileHide?: boolean
  mobileOrder?: number

  // Widget title
  title?: string
}

// ============================================================================
// Dashboard Layout and Configuration
// ============================================================================

export interface DashboardLayout {
  widgets: {
    [id: string]: Widget
  }
  gridSettings: {
    backgroundColor?: string
    columns?: number
    margins?: [number, number]
    outerMargin?: boolean
    backgroundSizeMode?: 'cover' | 'contain' | 'original' | '100%'
    backgroundImageUrl?: string
    autoFillHeight?: boolean
    mobileAutoFillHeight?: boolean
    mobileRowHeight?: number
  }
}

export interface DashboardState {
  id: string
  name: string
  root: boolean
  layouts: {
    main: DashboardLayout
    right?: DashboardLayout
  }
}

export interface DashboardSettings {
  stateControllerId?: string
  showTitle?: boolean
  showDashboardsSelect?: boolean
  showEntitiesSelect?: boolean
  showFilters?: boolean
  showDashboardTimewindow?: boolean
  showDashboardExport?: boolean
  toolbarAlwaysOpen?: boolean
  hideToolbar?: boolean
}

export interface Dashboard {
  id: string
  tenantId: string
  title: string
  image?: string
  mobileHide?: boolean
  mobileOrder?: number

  // Configuration
  configuration: {
    description?: string
    entityAliases?: {
      [id: string]: EntityAlias
    }
    filters?: any // Filters configuration
    timewindow?: Timewindow
    settings?: DashboardSettings
    states?: {
      [id: string]: DashboardState
    }
    widgets?: {
      [id: string]: Widget
    }
  }

  // Metadata
  assignedCustomers?: any[]
  createdTime?: number
  name?: string
}

// ============================================================================
// Widget Component Props
// ============================================================================

export interface WidgetComponentProps {
  widget: Widget
  data: WidgetData
  timewindow?: Timewindow
  onAction?: (action: WidgetAction, entity?: EntityId) => void
  editMode?: boolean
}

export interface WidgetData {
  datasources: DatasourceData[]
  latestValues?: Record<string, any>
  timeseriesData?: Record<string, TimeseriesData[]>
  alarms?: any[]
}

export interface DatasourceData {
  datasource: Datasource
  data: TimeseriesData[] | AttributeData[] | any[]
  entityId?: EntityId
  entityName?: string
  entityLabel?: string
}
