/**
 * Scatter Chart Widget - XY scatter plot
 */
import { Box, Typography, Paper } from '@mui/material'
import { ScatterChart as RechartsScatterChart, Scatter, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function ScatterChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const scatterData = [
    { x: 100, y: 200, z: 200 },
    { x: 120, y: 100, z: 260 },
    { x: 170, y: 300, z: 400 },
    { x: 140, y: 250, z: 280 },
    { x: 150, y: 400, z: 500 },
    { x: 110, y: 280, z: 200 },
  ]

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Scatter Chart'}
        </Typography>
      )}
      <Box sx={{ flex: 1, minHeight: 0 }}>
        <ResponsiveContainer width="100%" height="100%">
          <RechartsScatterChart>
            <CartesianGrid />
            <XAxis type="number" dataKey="x" name="X-Axis" />
            <YAxis type="number" dataKey="y" name="Y-Axis" />
            <Tooltip cursor={{ strokeDasharray: '3 3' }} />
            {config.settings?.showLegend && <Legend />}
            <Scatter name="Data Points" data={scatterData} fill="#0F3E5C" />
          </RechartsScatterChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'scatter_chart',
  name: 'Scatter Chart',
  description: 'XY scatter plot for correlation analysis',
  type: 'timeseries',
  tags: ['chart', 'scatter', 'xy', 'correlation'],
}

registerWidget(descriptor, ScatterChart)
export default ScatterChart
