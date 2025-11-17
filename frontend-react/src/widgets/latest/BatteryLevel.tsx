/**
 * Battery Level Widget - Visual battery indicator
 */
import { Box, Typography, Paper } from '@mui/material'
import { Battery20, Battery50, Battery80, BatteryFull, BatteryAlert } from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function BatteryLevel({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const latestValue = data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 75

  const getBatteryIcon = () => {
    if (latestValue <= 0) return <BatteryAlert sx={{ fontSize: 80, color: '#C62828' }} />
    if (latestValue <= 20) return <Battery20 sx={{ fontSize: 80, color: '#C62828' }} />
    if (latestValue <= 50) return <Battery50 sx={{ fontSize: 80, color: '#FFB300' }} />
    if (latestValue <= 80) return <Battery80 sx={{ fontSize: 80, color: '#2E7D6F' }} />
    return <BatteryFull sx={{ fontSize: 80, color: '#2E7D6F' }} />
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 3, justifyContent: 'center', alignItems: 'center' }}>
      {config.showTitle && (
        <Typography variant="subtitle1" sx={{ mb: 2, fontSize: '14px', fontWeight: 'bold', color: '#757575' }}>
          {config.title || 'Battery Level'}
        </Typography>
      )}
      {getBatteryIcon()}
      <Typography variant="h4" sx={{ mt: 2, fontWeight: 'bold', color: '#0F3E5C' }}>
        {latestValue.toFixed(0)}%
      </Typography>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'battery_level',
  name: 'Battery Level',
  description: 'Visual battery charge indicator',
  type: 'latest',
  tags: ['indicator', 'battery', 'level', 'icon'],
}

registerWidget(descriptor, BatteryLevel)
export default BatteryLevel
