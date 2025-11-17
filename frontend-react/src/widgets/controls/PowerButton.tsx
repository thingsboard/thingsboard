/**
 * Power Button Widget - Large power button with ON/OFF state
 * Professional IoT control with visual feedback
 */
import { useState } from 'react'
import { Box, Typography, Paper } from '@mui/material'
import { PowerSettingsNew as PowerIcon } from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function PowerButton({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const [isOn, setIsOn] = useState(false)
  const [isPressed, setIsPressed] = useState(false)

  const handleClick = () => {
    setIsOn(!isOn)
    console.log('Power button toggled:', !isOn)
    // TODO: Send RPC command to device
  }

  return (
    <Paper
      elevation={1}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        p: 3,
        justifyContent: 'center',
        alignItems: 'center',
        background: isOn ? 'linear-gradient(135deg, #2E7D6F 0%, #1a5345 100%)' : '#F5F5F5',
        transition: 'all 0.3s ease',
      }}
    >
      {config.showTitle && (
        <Typography
          variant="subtitle1"
          sx={{
            mb: 3,
            fontSize: '16px',
            fontWeight: 'bold',
            color: isOn ? 'white' : '#757575',
          }}
        >
          {config.title || 'Power Control'}
        </Typography>
      )}

      <Box
        sx={{
          position: 'relative',
          width: 150,
          height: 150,
          cursor: 'pointer',
          userSelect: 'none',
        }}
        onClick={handleClick}
        onMouseDown={() => setIsPressed(true)}
        onMouseUp={() => setIsPressed(false)}
        onMouseLeave={() => setIsPressed(false)}
      >
        {/* Outer glow ring */}
        <Box
          sx={{
            position: 'absolute',
            inset: 0,
            borderRadius: '50%',
            background: isOn
              ? 'radial-gradient(circle, rgba(46, 125, 111, 0.4) 0%, transparent 70%)'
              : 'transparent',
            animation: isOn ? 'pulse 2s infinite' : 'none',
            '@keyframes pulse': {
              '0%, 100%': { transform: 'scale(1)', opacity: 0.6 },
              '50%': { transform: 'scale(1.1)', opacity: 0.3 },
            },
          }}
        />

        {/* Button circle */}
        <Box
          sx={{
            position: 'absolute',
            inset: 10,
            borderRadius: '50%',
            background: isOn
              ? 'linear-gradient(135deg, #2E7D6F 0%, #4CAF50 100%)'
              : 'linear-gradient(135deg, #E0E0E0 0%, #BDBDBD 100%)',
            boxShadow: isPressed
              ? 'inset 0 4px 8px rgba(0,0,0,0.3)'
              : isOn
              ? '0 8px 16px rgba(46, 125, 111, 0.4), inset 0 -2px 4px rgba(0,0,0,0.2)'
              : '0 4px 8px rgba(0,0,0,0.2), inset 0 -2px 4px rgba(0,0,0,0.1)',
            transform: isPressed ? 'scale(0.95)' : 'scale(1)',
            transition: 'all 0.2s ease',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <PowerIcon
            sx={{
              fontSize: 70,
              color: isOn ? '#FFF' : '#757575',
              filter: isOn ? 'drop-shadow(0 0 10px rgba(255,255,255,0.5))' : 'none',
              transition: 'all 0.3s ease',
            }}
          />
        </Box>

        {/* Inner highlight */}
        <Box
          sx={{
            position: 'absolute',
            top: 15,
            left: 15,
            right: 15,
            height: 30,
            borderRadius: '50% 50% 0 0',
            background: isOn
              ? 'linear-gradient(180deg, rgba(255,255,255,0.3) 0%, transparent 100%)'
              : 'linear-gradient(180deg, rgba(255,255,255,0.4) 0%, transparent 100%)',
            pointerEvents: 'none',
          }}
        />
      </Box>

      <Typography
        variant="h5"
        sx={{
          mt: 3,
          fontWeight: 'bold',
          color: isOn ? '#FFF' : '#0F3E5C',
          textTransform: 'uppercase',
          letterSpacing: 2,
        }}
      >
        {isOn ? 'ON' : 'OFF'}
      </Typography>

      {isOn && (
        <Box
          sx={{
            mt: 1,
            px: 2,
            py: 0.5,
            borderRadius: 1,
            background: 'rgba(255,255,255,0.2)',
            backdropFilter: 'blur(10px)',
          }}
        >
          <Typography variant="caption" sx={{ color: 'white', fontSize: '12px' }}>
            ● Active
          </Typography>
        </Box>
      )}
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'power_button',
  name: 'Power Button',
  description: 'Large power button with ON/OFF state and visual feedback',
  type: 'rpc',
  tags: ['control', 'power', 'button', 'rpc', 'toggle'],
}

registerWidget(descriptor, PowerButton)
export default PowerButton
