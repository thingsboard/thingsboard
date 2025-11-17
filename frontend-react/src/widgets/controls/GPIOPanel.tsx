/**
 * GPIO Panel Widget - GPIO pin control panel
 * Digital I/O control for embedded devices
 */
import { useState } from 'react'
import { Box, Typography, Paper, Switch, Chip, Grid } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface GPIOPin {
  pin: number
  label: string
  mode: 'input' | 'output'
  state: boolean
}

function GPIOPanel({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Default GPIO configuration (8 pins)
  const defaultPins: GPIOPin[] = [
    { pin: 2, label: 'LED 1', mode: 'output', state: false },
    { pin: 3, label: 'LED 2', mode: 'output', state: false },
    { pin: 4, label: 'Relay 1', mode: 'output', state: false },
    { pin: 5, label: 'Relay 2', mode: 'output', state: false },
    { pin: 6, label: 'Motor', mode: 'output', state: false },
    { pin: 7, label: 'Buzzer', mode: 'output', state: false },
    { pin: 8, label: 'Fan', mode: 'output', state: false },
    { pin: 9, label: 'Heater', mode: 'output', state: false },
  ]

  const [pins, setPins] = useState<GPIOPin[]>(config.settings?.pins || defaultPins)

  const handleToggle = (pinNumber: number) => {
    setPins((prevPins) =>
      prevPins.map((pin) =>
        pin.pin === pinNumber ? { ...pin, state: !pin.state } : pin
      )
    )
    console.log(`GPIO Pin ${pinNumber} toggled`)
    // TODO: Send RPC command to device
  }

  const getPinColor = (pin: GPIOPin) => {
    if (pin.mode === 'input') return '#757575'
    return pin.state ? '#2E7D6F' : '#E0E0E0'
  }

  const getPinIcon = (pin: GPIOPin) => {
    if (pin.mode === 'input') return '📥'
    return pin.state ? '🟢' : '⚫'
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'GPIO Control Panel'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        <Grid container spacing={2}>
          {pins.map((pin) => (
            <Grid item xs={12} sm={6} key={pin.pin}>
              <Box
                sx={{
                  p: 2,
                  border: '2px solid',
                  borderColor: getPinColor(pin),
                  borderRadius: 2,
                  bgcolor: pin.state ? 'rgba(46, 125, 111, 0.1)' : '#FAFAFA',
                  transition: 'all 0.3s ease',
                }}
              >
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography variant="body2" sx={{ fontSize: '20px' }}>
                      {getPinIcon(pin)}
                    </Typography>
                    <Box>
                      <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: '#0F3E5C' }}>
                        {pin.label}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        GPIO {pin.pin}
                      </Typography>
                    </Box>
                  </Box>

                  <Chip
                    label={pin.mode.toUpperCase()}
                    size="small"
                    sx={{
                      bgcolor: pin.mode === 'output' ? '#0F3E5C' : '#757575',
                      color: 'white',
                      fontWeight: 'bold',
                      fontSize: '10px',
                    }}
                  />
                </Box>

                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography
                    variant="caption"
                    sx={{
                      fontWeight: 'bold',
                      color: pin.state ? '#2E7D6F' : '#757575',
                      textTransform: 'uppercase',
                    }}
                  >
                    {pin.mode === 'input' ? (pin.state ? 'HIGH' : 'LOW') : pin.state ? 'ON' : 'OFF'}
                  </Typography>

                  {pin.mode === 'output' && (
                    <Switch
                      checked={pin.state}
                      onChange={() => handleToggle(pin.pin)}
                      sx={{
                        '& .MuiSwitch-switchBase.Mui-checked': {
                          color: '#2E7D6F',
                        },
                        '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                          backgroundColor: '#2E7D6F',
                        },
                      }}
                    />
                  )}

                  {pin.mode === 'input' && (
                    <Box
                      sx={{
                        width: 40,
                        height: 24,
                        borderRadius: 12,
                        bgcolor: pin.state ? '#2E7D6F' : '#E0E0E0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontSize: '10px',
                        fontWeight: 'bold',
                      }}
                    >
                      {pin.state ? '1' : '0'}
                    </Box>
                  )}
                </Box>
              </Box>
            </Grid>
          ))}
        </Grid>
      </Box>

      <Box sx={{ mt: 2, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
            Active Pins: {pins.filter((p) => p.state).length}/{pins.length}
          </Typography>
          <Chip
            label="Connected"
            size="small"
            sx={{
              bgcolor: '#2E7D6F',
              color: 'white',
              fontSize: '10px',
              height: 20,
            }}
          />
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'gpio_panel',
  name: 'GPIO Panel',
  description: 'Digital I/O control panel for GPIO pins',
  type: 'rpc',
  tags: ['control', 'gpio', 'pins', 'digital', 'rpc', 'embedded'],
}

registerWidget(descriptor, GPIOPanel)
export default GPIOPanel
