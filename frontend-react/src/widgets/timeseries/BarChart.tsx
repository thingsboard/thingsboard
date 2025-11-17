/**
 * Bar Chart Widget
 * Displays data as vertical or horizontal bars
 */

import { Box, Typography, Paper } from '@mui/material'
import {
  BarChart as RechartsBarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'
import { format } from 'date-fns'

function BarChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const showTitle = config.showTitle ?? true
  const showLegend = config.settings?.showLegend ?? true
  const showTooltip = config.settings?.showTooltip ?? true
  const layout = config.settings?.layout || 'vertical'

  // Transform data for recharts format
  const chartData = transformData(data, widget)

  return (
    <Paper
      elevation={1}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        p: 2,
      }}
    >
      {showTitle && (
        <Typography
          variant="h6"
          sx={{
            mb: 2,
            fontSize: '16px',
            fontWeight: 'bold',
            color: '#0F3E5C',
          }}
        >
          {config.title || 'Bar Chart'}
        </Typography>
      )}

      <Box sx={{ flex: 1, minHeight: 0 }}>
        <ResponsiveContainer width="100%" height="100%">
          <RechartsBarChart
            data={chartData}
            layout={layout}
            margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis
              dataKey={layout === 'vertical' ? 'ts' : 'value'}
              type={layout === 'vertical' ? 'category' : 'number'}
              tickFormatter={(value) =>
                layout === 'vertical'
                  ? format(new Date(value), 'HH:mm:ss')
                  : value.toString()
              }
            />
            <YAxis
              dataKey={layout === 'vertical' ? 'value' : 'ts'}
              type={layout === 'vertical' ? 'number' : 'category'}
              tickFormatter={(value) =>
                layout === 'horizontal'
                  ? format(new Date(value), 'HH:mm:ss')
                  : value.toString()
              }
            />
            {showTooltip && (
              <Tooltip
                labelFormatter={(value) => format(new Date(value), 'MMM dd, HH:mm:ss')}
              />
            )}
            {showLegend && <Legend />}
            {config.datasources?.[0]?.dataKeys?.map((key, index) => (
              <Bar
                key={index}
                dataKey={key.name}
                fill={key.color || `hsl(${index * 137.5}, 70%, 50%)`}
                name={key.label || key.name}
              />
            ))}
          </RechartsBarChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  )
}

function transformData(data: any, widget: any) {
  if (!data?.datasources?.[0]?.data) return []

  const rawData = data.datasources[0].data
  const dataKeys = widget.config.datasources?.[0]?.dataKeys || []

  // Group by timestamp
  const grouped: Record<number, any> = {}
  rawData.forEach((point: any) => {
    const ts = point.ts
    if (!grouped[ts]) {
      grouped[ts] = { ts }
    }

    // Find which key this belongs to
    dataKeys.forEach((key: any) => {
      if (!grouped[ts][key.name]) {
        grouped[ts][key.name] = point.value
      }
    })
  })

  return Object.values(grouped).slice(-20) // Last 20 points
}

// Widget descriptor
const descriptor: WidgetTypeDescriptor = {
  id: 'bar_chart',
  name: 'Bar Chart',
  description: 'Display data as vertical or horizontal bars',
  type: 'timeseries',
  tags: ['chart', 'timeseries', 'bar'],
}

// Register widget
registerWidget(descriptor, BarChart)

export default BarChart
