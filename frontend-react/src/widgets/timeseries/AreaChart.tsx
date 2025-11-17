/**
 * Area Chart Widget - Filled line chart for trends
 */
import { Box, Typography, Paper } from '@mui/material'
import { AreaChart as RechartsAreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'
import { format } from 'date-fns'

function AreaChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const chartData = transformData(data, widget)

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Area Chart'}
        </Typography>
      )}
      <Box sx={{ flex: 1, minHeight: 0 }}>
        <ResponsiveContainer width="100%" height="100%">
          <RechartsAreaChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="ts" tickFormatter={(value) => format(new Date(value), 'HH:mm')} />
            <YAxis />
            {config.settings?.showTooltip && <Tooltip labelFormatter={(value) => format(new Date(value), 'MMM dd, HH:mm:ss')} />}
            {config.settings?.showLegend && <Legend />}
            {config.datasources?.[0]?.dataKeys?.map((key: any, index: number) => (
              <Area key={index} type="monotone" dataKey={key.name} stroke={key.color || '#0F3E5C'} fill={key.color || '#0F3E5C'} fillOpacity={0.3} name={key.label || key.name} />
            ))}
          </RechartsAreaChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  )
}

function transformData(data: any, widget: any) {
  if (!data?.datasources?.[0]?.data) return []
  const rawData = data.datasources[0].data
  const dataKeys = widget.config.datasources?.[0]?.dataKeys || []
  const grouped: Record<number, any> = {}
  rawData.forEach((point: any) => {
    const ts = point.ts
    if (!grouped[ts]) grouped[ts] = { ts }
    dataKeys.forEach((key: any) => {
      if (!grouped[ts][key.name]) grouped[ts][key.name] = point.value
    })
  })
  return Object.values(grouped).slice(-30)
}

const descriptor: WidgetTypeDescriptor = {
  id: 'area_chart',
  name: 'Area Chart',
  description: 'Display trends with filled area chart',
  type: 'timeseries',
  tags: ['chart', 'area', 'timeseries', 'trend'],
}

registerWidget(descriptor, AreaChart)
export default AreaChart
