/**
 * Doughnut Chart Widget
 * Displays data as a doughnut (hollow pie chart)
 */

import { Box, Typography, Paper } from '@mui/material'
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function DoughnutChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const showTitle = config.showTitle ?? true
  const showLegend = config.settings?.showLegend ?? true
  const showTooltip = config.settings?.showTooltip ?? true

  // Transform data for doughnut chart
  const chartData = transformData(data, widget)

  const COLORS = ['#0F3E5C', '#2E7D6F', '#FFB300', '#C62828', '#1976D2', '#388E3C']

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
          {config.title || 'Doughnut Chart'}
        </Typography>
      )}

      <Box sx={{ flex: 1, minHeight: 0 }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              dataKey="value"
              nameKey="name"
              cx="50%"
              cy="50%"
              innerRadius={50}
              outerRadius={80}
              label
            >
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            {showTooltip && <Tooltip />}
            {showLegend && <Legend />}
          </PieChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  )
}

function transformData(data: any, widget: any) {
  if (!data?.datasources?.[0]?.data) return []

  const dataKeys = widget.config.datasources?.[0]?.dataKeys || []
  const latestData = data.datasources[0].data.slice(-dataKeys.length)

  return dataKeys.map((key: any, index: number) => ({
    name: key.label || key.name,
    value: latestData[index]?.value || 0,
  }))
}

// Widget descriptor
const descriptor: WidgetTypeDescriptor = {
  id: 'doughnut_chart',
  name: 'Doughnut Chart',
  description: 'Display data distribution as a doughnut chart',
  type: 'timeseries',
  tags: ['chart', 'doughnut', 'pie', 'distribution'],
}

// Register widget
registerWidget(descriptor, DoughnutChart)

export default DoughnutChart
