/**
 * Radar Chart Widget - Multi-dimensional data visualization
 */
import { Box, Typography, Paper } from '@mui/material'
import { RadarChart as RechartsRadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar, Legend, ResponsiveContainer } from 'recharts'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function RadarChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const chartData = [
    { subject: 'CPU', value: 85 },
    { subject: 'Memory', value: 72 },
    { subject: 'Disk', value: 60 },
    { subject: 'Network', value: 90 },
    { subject: 'Load', value: 65 },
  ]

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Radar Chart'}
        </Typography>
      )}
      <Box sx={{ flex: 1, minHeight: 0 }}>
        <ResponsiveContainer width="100%" height="100%">
          <RechartsRadarChart data={chartData}>
            <PolarGrid />
            <PolarAngleAxis dataKey="subject" />
            <PolarRadiusAxis angle={90} domain={[0, 100]} />
            <Radar name="Metrics" dataKey="value" stroke="#0F3E5C" fill="#0F3E5C" fillOpacity={0.6} />
            {config.settings?.showLegend && <Legend />}
          </RechartsRadarChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'radar_chart',
  name: 'Radar Chart',
  description: 'Multi-dimensional radar/spider chart',
  type: 'timeseries',
  tags: ['chart', 'radar', 'spider', 'multi-dimensional'],
}

registerWidget(descriptor, RadarChart)
export default RadarChart
