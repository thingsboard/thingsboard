/**
 * Line Chart Widget
 * Displays time series data as a line chart
 * Equivalent to ThingsBoard's Timeseries Line Chart widget
 */

import { Card, CardContent, Typography, Box } from '@mui/material'
import {
  LineChart as RechartsLineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import { WidgetComponentProps } from '@/types/dashboard'
import { registerWidget } from '../widgetRegistry'
import { format } from 'date-fns'

function LineChart({ widget, data }: WidgetComponentProps) {
  const { config } = widget
  const { settings } = config

  // Transform data for recharts
  const chartData = transformTimeseriesData(data)

  // Get all data keys
  const dataKeys = data.datasources.flatMap((ds) =>
    ds.datasource.dataKeys.map((key) => ({
      name: key.label || key.name,
      dataKey: key.name,
      color: key.color || generateColor(key.name),
    }))
  )

  return (
    <Card
      sx={{
        height: '100%',
        bgcolor: (theme) =>
          settings.backgroundColor ??
          (theme.palette.mode === 'dark' ? '#1F2428' : '#FFFFFF'),
        borderColor: (theme) =>
          theme.palette.mode === 'dark' ? '#363C42' : '#E0E0E0',
        borderWidth: 1,
        borderStyle: 'solid',
        borderRadius: settings.borderRadius ?? 2,
      }}
    >
      <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        {/* Title */}
        {config.showTitle !== false && (
          <Typography
            variant="h6"
            sx={{
              fontWeight: 600,
              mb: 2,
              color: (theme) =>
                theme.palette.mode === 'dark' ? '#E1E3E5' : '#121517',
            }}
          >
            {config.title || 'Time Series'}
          </Typography>
        )}

        {/* Chart */}
        <Box sx={{ flex: 1, minHeight: 0 }}>
          <ResponsiveContainer width="100%" height="100%">
            <RechartsLineChart
              data={chartData}
              margin={{ top: 5, right: 20, left: 10, bottom: 5 }}
            >
              <CartesianGrid
                strokeDasharray="3 3"
                stroke={settings.gridColor ?? '#E0E0E0'}
              />
              <XAxis
                dataKey="timestamp"
                tickFormatter={(value) => format(new Date(value), 'HH:mm:ss')}
                stroke="#8C959D"
                style={{ fontSize: '0.75rem' }}
              />
              <YAxis
                stroke="#8C959D"
                style={{ fontSize: '0.75rem' }}
                tickFormatter={(value) =>
                  typeof value === 'number'
                    ? value.toFixed(settings.decimals ?? 1)
                    : value
                }
              />
              {settings.showTooltip !== false && (
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#1F2428',
                    border: '1px solid #363C42',
                    borderRadius: 8,
                  }}
                  labelFormatter={(value) =>
                    format(new Date(value as number), 'MMM dd, HH:mm:ss')
                  }
                  formatter={(value: any) =>
                    typeof value === 'number'
                      ? value.toFixed(settings.decimals ?? 2)
                      : value
                  }
                />
              )}
              {settings.showLegend !== false && (
                <Legend
                  wrapperStyle={{ fontSize: '0.875rem' }}
                  iconType="line"
                />
              )}
              {dataKeys.map((key, index) => (
                <Line
                  key={key.dataKey}
                  type={settings.smooth ? 'monotone' : 'linear'}
                  dataKey={key.dataKey}
                  name={key.name}
                  stroke={key.color}
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 6 }}
                  animationDuration={settings.animation !== false ? 300 : 0}
                />
              ))}
            </RechartsLineChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  )
}

// Transform timeseries data to recharts format
function transformTimeseriesData(data: any): any[] {
  const pointsMap = new Map<number, Record<string, any>>()

  data.datasources.forEach((ds: any) => {
    ds.datasource.dataKeys.forEach((key: any) => {
      const keyData = ds.data || []
      keyData.forEach((point: any) => {
        const ts = point.ts
        if (!pointsMap.has(ts)) {
          pointsMap.set(ts, { timestamp: ts })
        }
        pointsMap.get(ts)![key.name] = point.value
      })
    })
  })

  return Array.from(pointsMap.values()).sort((a, b) => a.timestamp - b.timestamp)
}

// Generate color from string hash
function generateColor(str: string): string {
  const colors = [
    '#FFB300', // accent
    '#2E7D6F', // success
    '#0F3E5C', // primary
    '#C62828', // danger
    '#8C959D', // secondary
    '#7E57C2',
    '#26A69A',
    '#FF7043',
    '#42A5F5',
    '#AB47BC',
  ]

  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

// Register widget
registerWidget(
  {
    id: 'timeseries_line_chart',
    name: 'Line Chart',
    type: 'timeseries',
    description: 'Display time series data as a line chart',
    icon: 'show_chart',
    defaultConfig: {
      datasources: [],
      timewindow: {
        realtime: {
          timewindowMs: 60000, // 1 minute
          interval: 1000, // 1 second
        },
      },
      settings: {
        showTitle: true,
        showLegend: true,
        showTooltip: true,
        smooth: true,
        animation: true,
        decimals: 2,
        gridColor: '#E0E0E0',
      },
    },
    defaultSizeX: 6,
    defaultSizeY: 4,
  },
  LineChart
)

export default LineChart
