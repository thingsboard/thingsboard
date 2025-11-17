/**
 * Multi-Button Panel Widget - Multiple button controls
 * Grid of buttons for various device commands
 */
import { useState } from 'react'
import { Box, Typography, Paper, Button, Grid } from '@mui/material'
import {
  PlayArrow,
  Stop,
  Pause,
  SkipNext,
  SkipPrevious,
  VolumeUp,
  VolumeDown,
  VolumeMute,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface ButtonConfig {
  id: string
  label: string
  icon: any
  color?: string
}

function MultiButton({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const [activeButton, setActiveButton] = useState<string | null>(null)

  // Default button configuration
  const defaultButtons: ButtonConfig[] = [
    { id: 'play', label: 'Play', icon: PlayArrow, color: '#2E7D6F' },
    { id: 'pause', label: 'Pause', icon: Pause, color: '#FFB300' },
    { id: 'stop', label: 'Stop', icon: Stop, color: '#C62828' },
    { id: 'prev', label: 'Previous', icon: SkipPrevious, color: '#0F3E5C' },
    { id: 'next', label: 'Next', icon: SkipNext, color: '#0F3E5C' },
    { id: 'vol-up', label: 'Vol +', icon: VolumeUp, color: '#757575' },
    { id: 'vol-down', label: 'Vol -', icon: VolumeDown, color: '#757575' },
    { id: 'mute', label: 'Mute', icon: VolumeMute, color: '#757575' },
  ]

  const buttons = config.settings?.buttons || defaultButtons

  const handleButtonClick = (buttonId: string) => {
    setActiveButton(buttonId)
    console.log('Button clicked:', buttonId)
    // TODO: Send RPC command to device

    // Reset active state after animation
    setTimeout(() => setActiveButton(null), 300)
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Multi-Button Panel'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        <Grid container spacing={2}>
          {buttons.map((btn: ButtonConfig) => {
            const Icon = btn.icon
            const isActive = activeButton === btn.id

            return (
              <Grid item xs={6} sm={4} md={3} key={btn.id}>
                <Button
                  variant="contained"
                  fullWidth
                  onClick={() => handleButtonClick(btn.id)}
                  sx={{
                    height: 80,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 1,
                    bgcolor: isActive ? btn.color || '#0F3E5C' : '#F5F5F5',
                    color: isActive ? 'white' : btn.color || '#0F3E5C',
                    border: `2px solid ${btn.color || '#0F3E5C'}`,
                    transform: isActive ? 'scale(0.95)' : 'scale(1)',
                    transition: 'all 0.2s ease',
                    '&:hover': {
                      bgcolor: btn.color || '#0F3E5C',
                      color: 'white',
                      transform: 'scale(1.05)',
                    },
                  }}
                >
                  <Icon sx={{ fontSize: 32 }} />
                  <Typography variant="caption" sx={{ fontWeight: 'bold', fontSize: '11px' }}>
                    {btn.label}
                  </Typography>
                </Button>
              </Grid>
            )
          })}
        </Grid>
      </Box>

      <Box sx={{ mt: 2, p: 1, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
          {activeButton ? `Sent: ${buttons.find((b: ButtonConfig) => b.id === activeButton)?.label}` : 'Ready'}
        </Typography>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'multi_button',
  name: 'Multi-Button Panel',
  description: 'Grid of configurable buttons for device commands',
  type: 'rpc',
  tags: ['control', 'buttons', 'panel', 'rpc', 'commands'],
}

registerWidget(descriptor, MultiButton)
export default MultiButton
