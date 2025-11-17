/**
 * LED Indicator Widget - Simple on/off indicator
 */
import { Box, Typography, Paper } from '@mui/material'
import { Circle } from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function LEDIndicator({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const latestValue = data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 0
  const isOn = latestValue > 0

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 3, justifyContent: 'center', alignItems: 'center' }}>
      {config.showTitle && (
        <Typography variant="subtitle1" sx={{ mb: 2, fontSize: '14px', fontWeight: 'bold', color: '#757575' }}>
          {config.title || 'LED Indicator'}
        </Typography>
      )}
      <Box sx={{ position: 'relative', display: 'inline-block' }}>
        <Circle sx={{ fontSize: 100, color: isOn ? '#2E7D6F' : '#E0E0E0', filter: isOn ? 'drop-shadow(0 0 10px #2E7D6F)' : 'none', transition: 'all 0.3s ease' }} />
        {isOn && (
          <Box sx={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', width: 40, height: 40, borderRadius: '50%', backgroundColor: 'rgba(46, 125, 111, 0.3)', animation: 'pulse 2s infinite' }} />
        )}
      </Box>
      <Typography variant="h6" sx={{ mt: 2, fontWeight: 'bold', color: isOn ? '#2E7D6F' : '#757575' }}>
        {isOn ? 'ON' : 'OFF'}
      </Typography>
      <style>
        {`@keyframes pulse {
            0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
            50% { transform: translate(-50%, -50%) scale(1.5); opacity: 0; }
          }`}
      </style>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'led_indicator',
  name: 'LED Indicator',
  description: 'Simple on/off LED indicator',
  type: 'latest',
  tags: ['indicator', 'led', 'status', 'light'],
}

registerWidget(descriptor, LEDIndicator)
export default LEDIndicator
