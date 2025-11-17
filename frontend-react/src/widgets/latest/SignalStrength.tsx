/**
 * Signal Strength Widget - Network signal indicator
 */
import { Box, Typography, Paper } from '@mui/material'
import { SignalCellular0Bar, SignalCellular1Bar, SignalCellular2Bar, SignalCellular3Bar, SignalCellular4Bar } from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function SignalStrength({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const latestValue = data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 80

  const getSignalIcon = () => {
    if (latestValue <= 0) return <SignalCellular0Bar sx={{ fontSize: 80, color: '#C62828' }} />
    if (latestValue <= 25) return <SignalCellular1Bar sx={{ fontSize: 80, color: '#C62828' }} />
    if (latestValue <= 50) return <SignalCellular2Bar sx={{ fontSize: 80, color: '#FFB300' }} />
    if (latestValue <= 75) return <SignalCellular3Bar sx={{ fontSize: 80, color: '#2E7D6F' }} />
    return <SignalCellular4Bar sx={{ fontSize: 80, color: '#2E7D6F' }} />
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 3, justifyContent: 'center', alignItems: 'center' }}>
      {config.showTitle && (
        <Typography variant="subtitle1" sx={{ mb: 2, fontSize: '14px', fontWeight: 'bold', color: '#757575' }}>
          {config.title || 'Signal Strength'}
        </Typography>
      )}
      {getSignalIcon()}
      <Typography variant="h4" sx={{ mt: 2, fontWeight: 'bold', color: '#0F3E5C' }}>
        {latestValue.toFixed(0)}%
      </Typography>
      <Typography variant="caption" color="text.secondary">
        {latestValue >= 75 ? 'Excellent' : latestValue >= 50 ? 'Good' : latestValue >= 25 ? 'Fair' : 'Poor'}
      </Typography>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'signal_strength',
  name: 'Signal Strength',
  description: 'Network signal strength indicator',
  type: 'latest',
  tags: ['indicator', 'signal', 'network', 'icon'],
}

registerWidget(descriptor, SignalStrength)
export default SignalStrength
