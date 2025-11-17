/**
 * PID Controller Widget - Process control tuning interface
 * Allows configuration of Proportional, Integral, Derivative parameters
 */
import { useState } from 'react'
import { Box, Typography, Paper, Slider, TextField, Button, Chip } from '@mui/material'
import { TuneOutlined, PlayArrow, Stop } from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function PIDController({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  const [kp, setKp] = useState(config.settings?.kp ?? 1.0) // Proportional
  const [ki, setKi] = useState(config.settings?.ki ?? 0.1) // Integral
  const [kd, setKd] = useState(config.settings?.kd ?? 0.05) // Derivative
  const [setpoint, setSetpoint] = useState(config.settings?.setpoint ?? 50)
  const [isActive, setIsActive] = useState(false)

  const currentValue = data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 45
  const error = setpoint - currentValue

  const handleApply = () => {
    console.log('PID Parameters applied:', { kp, ki, kd, setpoint })
    setIsActive(true)
    // TODO: Send RPC command to device
  }

  const handleStop = () => {
    setIsActive(false)
    console.log('PID Controller stopped')
  }

  const handleAutoTune = () => {
    console.log('Auto-tuning PID parameters...')
    // TODO: Implement Ziegler-Nichols or similar auto-tuning
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6" sx={{ fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
            {config.title || 'PID Controller'}
          </Typography>
          <Chip
            label={isActive ? 'ACTIVE' : 'IDLE'}
            size="small"
            sx={{
              bgcolor: isActive ? '#2E7D6F' : '#757575',
              color: 'white',
              fontWeight: 'bold',
            }}
          />
        </Box>
      )}

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {/* Current Status */}
        <Box sx={{ mb: 3, p: 2, bgcolor: '#F5F5F5', borderRadius: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
                Setpoint
              </Typography>
              <Typography variant="h5" sx={{ fontWeight: 'bold', color: '#0F3E5C' }}>
                {setpoint.toFixed(1)}
              </Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
                Current Value
              </Typography>
              <Typography variant="h5" sx={{ fontWeight: 'bold', color: '#2E7D6F' }}>
                {currentValue.toFixed(1)}
              </Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
                Error
              </Typography>
              <Typography
                variant="h5"
                sx={{
                  fontWeight: 'bold',
                  color: Math.abs(error) < 2 ? '#2E7D6F' : '#C62828',
                }}
              >
                {error > 0 ? '+' : ''}
                {error.toFixed(1)}
              </Typography>
            </Box>
          </Box>

          {/* Visual error indicator */}
          <Box sx={{ mt: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="caption" sx={{ fontSize: '10px', minWidth: 40 }}>
                0
              </Typography>
              <Box sx={{ flex: 1, position: 'relative', height: 24, bgcolor: '#E0E0E0', borderRadius: 1 }}>
                {/* Setpoint marker */}
                <Box
                  sx={{
                    position: 'absolute',
                    left: `${(setpoint / 100) * 100}%`,
                    top: 0,
                    bottom: 0,
                    width: 3,
                    bgcolor: '#0F3E5C',
                  }}
                />
                {/* Current value marker */}
                <Box
                  sx={{
                    position: 'absolute',
                    left: `${(currentValue / 100) * 100}%`,
                    top: 0,
                    bottom: 0,
                    width: 3,
                    bgcolor: '#2E7D6F',
                  }}
                />
              </Box>
              <Typography variant="caption" sx={{ fontSize: '10px', minWidth: 40 }}>
                100
              </Typography>
            </Box>
          </Box>
        </Box>

        {/* Setpoint Control */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 'bold', color: '#0F3E5C' }}>
            Setpoint
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <Slider
              value={setpoint}
              onChange={(_, v) => setSetpoint(v as number)}
              min={0}
              max={100}
              step={0.1}
              valueLabelDisplay="auto"
              sx={{
                flex: 1,
                '& .MuiSlider-thumb': { width: 20, height: 20 },
                '& .MuiSlider-track': { bgcolor: '#0F3E5C' },
              }}
            />
            <TextField
              type="number"
              value={setpoint}
              onChange={(e) => setSetpoint(parseFloat(e.target.value) || 0)}
              size="small"
              sx={{ width: 80 }}
              inputProps={{ step: 0.1, min: 0, max: 100 }}
            />
          </Box>
        </Box>

        {/* PID Parameters */}
        <Box sx={{ mb: 2 }}>
          <Typography variant="subtitle2" sx={{ mb: 2, fontWeight: 'bold', color: '#0F3E5C' }}>
            PID Tuning Parameters
          </Typography>

          {/* Proportional */}
          <Box sx={{ mb: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption" sx={{ fontSize: '11px', color: '#757575' }}>
                Kp (Proportional)
              </Typography>
              <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold' }}>
                {kp.toFixed(2)}
              </Typography>
            </Box>
            <Slider
              value={kp}
              onChange={(_, v) => setKp(v as number)}
              min={0}
              max={10}
              step={0.01}
              size="small"
              sx={{ '& .MuiSlider-track': { bgcolor: '#C62828' } }}
            />
          </Box>

          {/* Integral */}
          <Box sx={{ mb: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption" sx={{ fontSize: '11px', color: '#757575' }}>
                Ki (Integral)
              </Typography>
              <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold' }}>
                {ki.toFixed(2)}
              </Typography>
            </Box>
            <Slider
              value={ki}
              onChange={(_, v) => setKi(v as number)}
              min={0}
              max={2}
              step={0.01}
              size="small"
              sx={{ '& .MuiSlider-track': { bgcolor: '#FFB300' } }}
            />
          </Box>

          {/* Derivative */}
          <Box sx={{ mb: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption" sx={{ fontSize: '11px', color: '#757575' }}>
                Kd (Derivative)
              </Typography>
              <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold' }}>
                {kd.toFixed(2)}
              </Typography>
            </Box>
            <Slider
              value={kd}
              onChange={(_, v) => setKd(v as number)}
              min={0}
              max={1}
              step={0.01}
              size="small"
              sx={{ '& .MuiSlider-track': { bgcolor: '#2E7D6F' } }}
            />
          </Box>
        </Box>
      </Box>

      {/* Control Buttons */}
      <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
        <Button
          variant="outlined"
          size="small"
          startIcon={<TuneOutlined />}
          onClick={handleAutoTune}
          sx={{ flex: 1 }}
        >
          Auto Tune
        </Button>
        {isActive ? (
          <Button
            variant="contained"
            size="small"
            startIcon={<Stop />}
            onClick={handleStop}
            sx={{ flex: 1, bgcolor: '#C62828', '&:hover': { bgcolor: '#8B1F1F' } }}
          >
            Stop
          </Button>
        ) : (
          <Button
            variant="contained"
            size="small"
            startIcon={<PlayArrow />}
            onClick={handleApply}
            sx={{ flex: 1, bgcolor: '#2E7D6F', '&:hover': { bgcolor: '#1a5345' } }}
          >
            Apply
          </Button>
        )}
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'pid_controller',
  name: 'PID Controller',
  description: 'Process control with PID parameter tuning',
  type: 'rpc',
  tags: ['control', 'pid', 'industrial', 'process', 'automation'],
}

registerWidget(descriptor, PIDController)
export default PIDController
