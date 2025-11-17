/**
 * Liquid Level Widget - Visual liquid level indicator
 */
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function LiquidLevel({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const latestValue = data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 65
  const percentage = Math.max(0, Math.min(100, latestValue))

  const getColor = () => {
    if (percentage < 25) return '#C62828'
    if (percentage < 50) return '#FFB300'
    return '#0F3E5C'
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 3, justifyContent: 'center', alignItems: 'center' }}>
      {config.showTitle && (
        <Typography variant="subtitle1" sx={{ mb: 2, fontSize: '14px', fontWeight: 'bold', color: '#757575' }}>
          {config.title || 'Liquid Level'}
        </Typography>
      )}
      <Box sx={{ position: 'relative', width: 120, height: 200, border: '3px solid #0F3E5C', borderRadius: '10px', overflow: 'hidden', backgroundColor: '#F5F5F5' }}>
        <Box sx={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: `${percentage}%`, backgroundColor: getColor(), transition: 'height 0.5s ease', background: `linear-gradient(180deg, ${getColor()}CC 0%, ${getColor()} 100%)` }} />
        <Box sx={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: 2 }}>
          <Typography variant="h4" sx={{ fontWeight: 'bold', color: '#FFF', textShadow: '2px 2px 4px rgba(0,0,0,0.5)' }}>
            {percentage.toFixed(0)}%
          </Typography>
        </Box>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '120px', mt: 1 }}>
        <Typography variant="caption" color="text.secondary">0%</Typography>
        <Typography variant="caption" color="text.secondary">100%</Typography>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'liquid_level',
  name: 'Liquid Level',
  description: 'Visual liquid level indicator with tank',
  type: 'latest',
  tags: ['indicator', 'liquid', 'level', 'tank'],
}

registerWidget(descriptor, LiquidLevel)
export default LiquidLevel
