/**
 * Main Dashboard Page
 * Implements Payvar Industrial IoT Platform design with full ThingsBoard features
 */

import { Box, Typography, Grid } from '@mui/material'
import MainLayout from '@/components/layout/MainLayout'
import WidgetContainer from '@/components/dashboard/WidgetContainer'
import { Widget, WidgetData } from '@/types/dashboard'
import '@/widgets' // Import to register all widgets

export default function DashboardPage() {
  // Mock widgets configuration matching the Payvar design
  const widgets: Widget[] = [
    // Stats Cards Row
    {
      id: 'turbine-rpm',
      typeId: 'latest_value_card',
      type: 'latest',
      row: 0,
      col: 0,
      sizeX: 3,
      sizeY: 2,
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
      col: 3,
      sizeX: 3,
      sizeY: 2,
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
      col: 6,
      sizeX: 3,
      sizeY: 2,
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
      col: 9,
      sizeX: 3,
      sizeY: 2,
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

    // Process Flow and Alarm Panel Row
    {
      id: 'process-flow',
      typeId: 'timeseries_line_chart',
      type: 'timeseries',
      row: 2,
      col: 0,
      sizeX: 8,
      sizeY: 6,
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
    {
      id: 'alarm-panel',
      typeId: 'alarm_table',
      type: 'alarm',
      row: 2,
      col: 8,
      sizeX: 4,
      sizeY: 6,
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

  // Mock data for widgets (in production, this comes from WebSocket/API)
  const mockData: Record<string, WidgetData> = {
    'turbine-rpm': {
      datasources: [
        {
          datasource: widgets[0].config.datasources[0],
          data: [{ ts: Date.now(), value: 4820 }],
        },
      ],
    },
    'coolant-pressure': {
      datasources: [
        {
          datasource: widgets[1].config.datasources[0],
          data: [{ ts: Date.now(), value: 250 }],
        },
      ],
    },
    'system-efficiency': {
      datasources: [
        {
          datasource: widgets[2].config.datasources[0],
          data: [{ ts: Date.now(), value: 98.7 }],
        },
      ],
    },
    'active-alarms': {
      datasources: [
        {
          datasource: widgets[3].config.datasources[0],
          data: [{ ts: Date.now(), value: 3 }],
        },
      ],
    },
    'process-flow': {
      datasources: [
        {
          datasource: widgets[4].config.datasources[0],
          data: generateTimeseriesData(),
        },
      ],
    },
  }

  return (
    <MainLayout>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        {/* Page Header */}
        <Typography
          variant="h4"
          sx={{
            fontWeight: 'bold',
            color: (theme) =>
              theme.palette.mode === 'dark' ? '#E1E3E5' : '#121517',
          }}
        >
          Main Control Dashboard
        </Typography>

        {/* Stats Cards Row */}
        <Grid container spacing={3}>
          {widgets.slice(0, 4).map((widget) => (
            <Grid item xs={12} sm={6} md={3} key={widget.id}>
              <Box sx={{ height: 150 }}>
                <WidgetContainer widget={widget} data={mockData[widget.id]} />
              </Box>
            </Grid>
          ))}
        </Grid>

        {/* Process Flow and Alarms Row */}
        <Grid container spacing={3}>
          {/* Process Flow Chart */}
          <Grid item xs={12} lg={8}>
            <Box sx={{ height: 450 }}>
              <WidgetContainer
                widget={widgets[4]}
                data={mockData['process-flow']}
              />
            </Box>
          </Grid>

          {/* Alarm Panel */}
          <Grid item xs={12} lg={4}>
            <Box sx={{ height: 450 }}>
              <WidgetContainer widget={widgets[5]} />
            </Box>
          </Grid>
        </Grid>
      </Box>
    </MainLayout>
  )
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
