/**
 * Slider Control Widget - Slider for numeric value control
 */
import { Box, Typography, Paper, Slider } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'
import { useState } from 'react'

function SliderControl({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const min = config.settings?.min ?? 0
  const max = config.settings?.max ?? 100
  const [value, setValue] = useState(50)

  const handleChange = (event: Event, newValue: number | number[]) => {
    setValue(newValue as number)
    console.log('Slider changed:', newValue)
    // TODO: Send RPC command to device
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 3, justifyContent: 'center' }}>
      {config.showTitle && (
        <Typography variant="subtitle1" sx={{ mb: 2, fontSize: '14px', fontWeight: 'bold', color: '#757575', textAlign: 'center' }}>
          {config.title || 'Slider Control'}
        </Typography>
      )}
      <Box sx={{ px: 2 }}>
        <Typography variant="h4" sx={{ textAlign: 'center', fontWeight: 'bold', color: '#0F3E5C', mb: 2 }}>
          {value}
        </Typography>
        <Slider value={value} onChange={handleChange} min={min} max={max} valueLabelDisplay="auto" sx={{ color: '#0F3E5C' }} />
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
          <Typography variant="caption" color="text.secondary">{min}</Typography>
          <Typography variant="caption" color="text.secondary">{max}</Typography>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'slider_control',
  name: 'Slider Control',
  description: 'Slider for numeric value control',
  type: 'rpc',
  tags: ['control', 'slider', 'numeric', 'rpc'],
}

registerWidget(descriptor, SliderControl)
export default SliderControl
