/**
 * Main Dashboard Page
 * Implements Payvar Industrial IoT Platform design with full ThingsBoard features
 * Includes dashboard editor with drag-and-drop grid system
 */

import { useState, useCallback, useEffect } from 'react'
import { Box } from '@mui/material'
import MainLayout from '@/components/layout/MainLayout'
import DashboardEditor from '@/components/dashboard/DashboardEditor'
import DashboardToolbar from '@/components/dashboard/DashboardToolbar'
import WidgetLibrary from '@/components/dashboard/WidgetLibrary'
import WidgetConfigDialog from '@/components/dashboard/WidgetConfigDialog'
import TimewindowSelector from '@/components/dashboard/TimewindowSelector'
import DashboardImport from '@/components/dashboard/DashboardImport'
import { Widget, WidgetData, WidgetTypeId } from '@/types/dashboard'
import '@/widgets' // Import to register all widgets

export default function DashboardPage() {
  const [editMode, setEditMode] = useState(false)
  const [hasChanges, setHasChanges] = useState(false)
  const [showWidgetLibrary, setShowWidgetLibrary] = useState(false)
  const [showWidgetConfig, setShowWidgetConfig] = useState(false)
  const [showTimewindow, setShowTimewindow] = useState(false)
  const [showImport, setShowImport] = useState(false)
  const [selectedWidget, setSelectedWidget] = useState<Widget | null>(null)
  const [originalWidgets, setOriginalWidgets] = useState<Widget[]>([])
  const [currentWidgets, setCurrentWidgets] = useState<Widget[]>([])

  // Initialize widgets on mount
  useEffect(() => {
    const initialWidgets = getInitialWidgets()
    setOriginalWidgets(initialWidgets)
    setCurrentWidgets(initialWidgets)
  }, [])

  // Handle edit mode toggle
  const handleEditModeToggle = useCallback(() => {
    if (editMode) {
      // If exiting edit mode, prompt to save changes
      if (hasChanges) {
        const confirm = window.confirm('You have unsaved changes. Save before exiting edit mode?')
        if (confirm) {
          handleSave()
        } else {
          // Revert changes
          setCurrentWidgets(originalWidgets)
          setHasChanges(false)
        }
      }
      setEditMode(false)
    } else {
      setEditMode(true)
    }
  }, [editMode, hasChanges, originalWidgets])

  // Handle layout changes
  const handleLayoutChange = useCallback((updatedWidgets: Widget[]) => {
    setCurrentWidgets(updatedWidgets)
    setHasChanges(true)
  }, [])

  // Handle save
  const handleSave = useCallback(() => {
    console.log('Saving dashboard layout...', currentWidgets)
    // TODO: Integrate with backend API
    setOriginalWidgets(currentWidgets)
    setHasChanges(false)
    setEditMode(false)
    alert('Dashboard saved successfully!')
  }, [currentWidgets])

  // Handle cancel
  const handleCancel = useCallback(() => {
    const confirm = window.confirm('Discard all changes?')
    if (confirm) {
      setCurrentWidgets(originalWidgets)
      setHasChanges(false)
      setEditMode(false)
    }
  }, [originalWidgets])

  // Handle add widget
  const handleAddWidget = useCallback(() => {
    setShowWidgetLibrary(true)
  }, [])

  // Handle widget selection from library
  const handleSelectWidget = useCallback((typeId: WidgetTypeId) => {
    // Find next available position in grid
    const nextRow = currentWidgets.length > 0
      ? Math.max(...currentWidgets.map(w => w.row + w.sizeY)) + 1
      : 0

    const newWidget: Widget = {
      id: `widget-${Date.now()}`,
      typeId,
      type: typeId.includes('chart') || typeId.includes('timeseries') ? 'timeseries' :
            typeId.includes('card') || typeId.includes('value') ? 'latest' :
            typeId.includes('alarm') ? 'alarm' :
            typeId.includes('rpc') ? 'rpc' : 'static',
      row: nextRow,
      col: 0,
      sizeX: 6, // Default width
      sizeY: 4, // Default height
      config: {
        datasources: [],
        title: 'New Widget',
        settings: {},
      },
    }

    setCurrentWidgets([...currentWidgets, newWidget])
    setHasChanges(true)
  }, [currentWidgets])

  // Handle widget delete
  const handleWidgetDelete = useCallback((widgetId: string) => {
    const confirm = window.confirm('Delete this widget?')
    if (confirm) {
      setCurrentWidgets(currentWidgets.filter(w => w.id !== widgetId))
      setHasChanges(true)
    }
  }, [currentWidgets])

  // Handle widget edit
  const handleWidgetEdit = useCallback((widget: Widget) => {
    setSelectedWidget(widget)
    setShowWidgetConfig(true)
  }, [])

  // Handle widget config save
  const handleWidgetConfigSave = useCallback((updatedWidget: Widget) => {
    setCurrentWidgets((widgets) =>
      widgets.map((w) => (w.id === updatedWidget.id ? updatedWidget : w))
    )
    setHasChanges(true)
  }, [])

  // Handle fullscreen
  const handleFullscreen = useCallback(() => {
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      document.documentElement.requestFullscreen()
    }
  }, [])

  // Mock data for widgets (in production, this comes from WebSocket/API)
  const mockData: Record<string, WidgetData> = generateMockData(currentWidgets)

  return (
    <MainLayout>
      <Box sx={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 64px)' }}>
        {/* Dashboard Toolbar */}
        <DashboardToolbar
          editMode={editMode}
          onEditModeToggle={handleEditModeToggle}
          onSave={handleSave}
          onCancel={handleCancel}
          onAddWidget={handleAddWidget}
          onSettings={() => alert('Dashboard settings coming soon!')}
          onFullscreen={handleFullscreen}
          onExport={() => exportDashboard(currentWidgets)}
          onImport={() => setShowImport(true)}
          onTimewindow={() => setShowTimewindow(true)}
          onFilters={() => alert('Filters coming soon!')}
          hasChanges={hasChanges}
          dashboardTitle="Main Control Dashboard"
        />

        {/* Dashboard Editor */}
        <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
          <DashboardEditor
            widgets={currentWidgets}
            widgetData={mockData}
            editMode={editMode}
            onLayoutChange={handleLayoutChange}
            onWidgetEdit={handleWidgetEdit}
            onWidgetDelete={handleWidgetDelete}
            onWidgetFullscreen={(id) => console.log('Fullscreen widget:', id)}
          />
        </Box>

        {/* Widget Library Dialog */}
        <WidgetLibrary
          open={showWidgetLibrary}
          onClose={() => setShowWidgetLibrary(false)}
          onSelectWidget={handleSelectWidget}
        />

        {/* Widget Configuration Dialog */}
        <WidgetConfigDialog
          open={showWidgetConfig}
          onClose={() => setShowWidgetConfig(false)}
          widget={selectedWidget}
          onSave={handleWidgetConfigSave}
        />

        {/* Timewindow Selector */}
        <TimewindowSelector
          open={showTimewindow}
          onClose={() => setShowTimewindow(false)}
          onApply={(timewindow) => {
            console.log('Timewindow applied:', timewindow)
            // TODO: Apply timewindow to all widgets
          }}
        />

        {/* Dashboard Import */}
        <DashboardImport
          open={showImport}
          onClose={() => setShowImport(false)}
          onImport={(dashboard, mode) => {
            if (mode === 'replace') {
              setCurrentWidgets(dashboard.widgets || [])
            } else {
              setCurrentWidgets([...currentWidgets, ...(dashboard.widgets || [])])
            }
            setHasChanges(true)
          }}
        />
      </Box>
    </MainLayout>
  )
}

// Helper function to get initial widgets
function getInitialWidgets(): Widget[] {
  return [
    // Stats Cards Row
    {
      id: 'turbine-rpm',
      typeId: 'latest_value_card',
      type: 'latest',
      row: 0,
      col: 0,
      sizeX: 6,
      sizeY: 3,
      config: {
        datasources: [
          {
            type: 'entity',
            name: 'Turbine RPM',
            dataKeys: [{ name: 'rpm', type: 'timeseries', label: 'RPM' }],
          },
        ],
        title: 'Turbine RPM',
        settings: {
          showTrend: true,
          trendValue: 1.2,
          decimals: 0,
          units: '',
        },
      },
    },
    {
      id: 'coolant-pressure',
      typeId: 'latest_value_card',
      type: 'latest',
      row: 0,
      col: 6,
      sizeX: 6,
      sizeY: 3,
      config: {
        datasources: [
          {
            type: 'entity',
            name: 'Coolant Pressure',
            dataKeys: [{ name: 'pressure', type: 'timeseries', label: 'PSI' }],
          },
        ],
        title: 'Coolant Pressure',
        settings: {
          showTrend: true,
          trendValue: -0.5,
          decimals: 0,
          units: 'PSI',
        },
      },
    },
    {
      id: 'system-efficiency',
      typeId: 'latest_value_card',
      type: 'latest',
      row: 0,
      col: 12,
      sizeX: 6,
      sizeY: 3,
      config: {
        datasources: [
          {
            type: 'entity',
            name: 'System Efficiency',
            dataKeys: [{ name: 'efficiency', type: 'timeseries', label: 'Efficiency' }],
          },
        ],
        title: 'System Efficiency',
        settings: {
          showTrend: true,
          trendValue: 0.1,
          decimals: 1,
          units: '%',
        },
      },
    },
    {
      id: 'active-alarms',
      typeId: 'latest_value_card',
      type: 'latest',
      row: 0,
      col: 18,
      sizeX: 6,
      sizeY: 3,
      config: {
        datasources: [
          {
            type: 'entity',
            name: 'Active Alarms',
            dataKeys: [{ name: 'alarmCount', type: 'timeseries', label: 'Alarms' }],
          },
        ],
        title: 'Active Alarms',
        settings: {
          showTrend: false,
          decimals: 0,
          units: 'Critical',
          valueFont: {
            color: '#C62828',
          },
        },
      },
    },

    // Process Flow Chart
    {
      id: 'process-flow',
      typeId: 'timeseries_line_chart',
      type: 'timeseries',
      row: 3,
      col: 0,
      sizeX: 16,
      sizeY: 8,
      config: {
        datasources: [
          {
            type: 'entity',
            name: 'Cooling System',
            dataKeys: [
              {
                name: 'temperature',
                type: 'timeseries',
                label: 'Temperature',
                color: '#FFB300',
              },
              {
                name: 'pressure',
                type: 'timeseries',
                label: 'Pressure',
                color: '#2E7D6F',
              },
              {
                name: 'flowRate',
                type: 'timeseries',
                label: 'Flow Rate',
                color: '#0F3E5C',
              },
            ],
          },
        ],
        title: 'Cooling System Process Flow',
        showTitle: true,
        timewindow: {
          realtime: {
            timewindowMs: 300000, // 5 minutes
            interval: 1000,
          },
        },
        settings: {
          showLegend: true,
          showTooltip: true,
          smooth: true,
          animation: true,
          decimals: 2,
        },
      },
    },

    // Alarm Panel
    {
      id: 'alarm-panel',
      typeId: 'alarm_table',
      type: 'alarm',
      row: 3,
      col: 16,
      sizeX: 8,
      sizeY: 8,
      config: {
        datasources: [],
        title: 'Real-Time Alarms',
        settings: {
          showTitle: true,
          maxAlarms: 10,
          enableAcknowledge: true,
        },
      },
    },
  ]
}

// Helper function to generate mock data
function generateMockData(widgets: Widget[]): Record<string, WidgetData> {
  const mockData: Record<string, WidgetData> = {}

  widgets.forEach((widget) => {
    if (widget.typeId === 'latest_value_card') {
      const value = widget.id.includes('rpm') ? 4820 :
                    widget.id.includes('pressure') ? 250 :
                    widget.id.includes('efficiency') ? 98.7 :
                    widget.id.includes('alarm') ? 3 : 0

      mockData[widget.id] = {
        datasources: [
          {
            datasource: widget.config.datasources[0],
            data: [{ ts: Date.now(), value }],
          },
        ],
      }
    } else if (widget.typeId === 'timeseries_line_chart') {
      mockData[widget.id] = {
        datasources: [
          {
            datasource: widget.config.datasources[0],
            data: generateTimeseriesData(),
          },
        ],
      }
    }
  })

  return mockData
}

// Helper function to generate mock timeseries data
function generateTimeseriesData() {
  const now = Date.now()
  const data = []
  for (let i = 60; i >= 0; i--) {
    const ts = now - i * 5000 // 5 second intervals
    data.push({
      ts,
      value: {
        temperature: 45 + Math.random() * 10,
        pressure: 240 + Math.random() * 20,
        flowRate: 180 + Math.random() * 40,
      },
    })
  }

  // Transform to proper format
  const result: any[] = []
  data.forEach((point) => {
    result.push(
      { ts: point.ts, value: point.value.temperature },
      { ts: point.ts, value: point.value.pressure },
      { ts: point.ts, value: point.value.flowRate }
    )
  })

  return result
}

// Helper function to export dashboard
function exportDashboard(widgets: Widget[]) {
  const dashboardConfig = {
    title: 'Main Control Dashboard',
    version: '1.0.0',
    widgets,
    createdAt: new Date().toISOString(),
  }

  const dataStr = JSON.stringify(dashboardConfig, null, 2)
  const dataBlob = new Blob([dataStr], { type: 'application/json' })
  const url = URL.createObjectURL(dataBlob)
  const link = document.createElement('a')
  link.href = url
  link.download = `dashboard-${Date.now()}.json`
  link.click()
  URL.revokeObjectURL(url)
}
