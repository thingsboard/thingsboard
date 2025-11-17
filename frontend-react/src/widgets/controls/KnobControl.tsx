/**
 * Knob Control Widget - Rotary knob for value control
 */
import { useState } from 'react'
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function KnobControl({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const min = config.settings?.min ?? 0
  const max = config.settings?.max ?? 100
  const [value, setValue] = useState(50)
  const [isDragging, setIsDragging] = useState(false)

  const handleMouseDown = () => setIsDragging(true)

  const handleMouseUp = () => setIsDragging(false)

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return
    const deltaY = e.movementY
    const newValue = Math.max(min, Math.min(max, value - deltaY))
    setValue(newValue)
  }

  const angle = ((value - min) / (max - min)) * 270 - 135

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 3, justifyContent: 'center', alignItems: 'center' }} onMouseMove={handleMouseMove} onMouseUp={handleMouseUp} onMouseLeave={handleMouseUp}>
      {config.showTitle && (
        <Typography variant="subtitle1" sx={{ mb: 2, fontSize: '14px', fontWeight: 'bold', color: '#757575' }}>
          {config.title || 'Knob Control'}
        </Typography>
      )}
      <Box sx={{ position: 'relative', width: 150, height: 150, cursor: isDragging ? 'grabbing' : 'grab' }} onMouseDown={handleMouseDown}>
        <svg width="150" height="150" viewBox="0 0 150 150">
          <circle cx="75" cy="75" r="60" fill="#E0E0E0" stroke="#0F3E5C" strokeWidth="3" />
          <circle cx="75" cy="75" r="50" fill="#F5F5F5" />
          <line x1="75" y1="75" x2="75" y2="25" stroke="#0F3E5C" strokeWidth="4" strokeLinecap="round" transform={`rotate(${angle} 75 75)`} style={{ transition: 'transform 0.1s ease' }} />
          <circle cx="75" cy="75" r="10" fill="#0F3E5C" />
        </svg>
      </Box>
      <Typography variant="h4" sx={{ mt: 2, fontWeight: 'bold', color: '#0F3E5C' }}>
        {value.toFixed(0)}
      </Typography>
      <Box sx={{ display: 'flex', gap: 4, mt: 1 }}>
        <Typography variant="caption" color="text.secondary">{min}</Typography>
        <Typography variant="caption" color="text.secondary">{max}</Typography>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'knob_control',
  name: 'Knob Control',
  description: 'Rotary knob for value control',
  type: 'rpc',
  tags: ['control', 'knob', 'rotary', 'rpc'],
}

registerWidget(descriptor, KnobControl)
export default KnobControl
