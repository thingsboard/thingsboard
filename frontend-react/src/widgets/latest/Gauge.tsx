/**
 * Gauge Widget
 * Displays a value as a circular gauge/meter
 */

import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function Gauge({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const showTitle = config.showTitle ?? true
  const decimals = config.settings?.decimals ?? 0
  const min = config.settings?.min ?? 0
  const max = config.settings?.max ?? 100
  const units = config.settings?.units || ''

  // Get latest value
  const latestValue =
    data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 0

  // Calculate percentage and angle
  const percentage = ((latestValue - min) / (max - min)) * 100
  const normalizedValue = Math.max(0, Math.min(100, percentage))
  const angle = (normalizedValue / 100) * 270 - 135 // -135 to 135 degrees

  // Color zones
  const getColor = () => {
    if (normalizedValue < 33) return '#C62828'
    if (normalizedValue < 66) return '#FFB300'
    return '#2E7D6F'
  }

  return (
    <Paper
      elevation={1}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        p: 2,
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {showTitle && (
        <Typography
          variant="subtitle1"
          sx={{
            mb: 1,
            fontSize: '14px',
            fontWeight: 'bold',
            color: '#757575',
          }}
        >
          {config.title || 'Gauge'}
        </Typography>
      )}

      <Box
        sx={{
          position: 'relative',
          width: 200,
          height: 200,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        {/* Gauge background */}
        <svg width="200" height="200" viewBox="0 0 200 200">
          {/* Background arc */}
          <path
            d="M 30 170 A 80 80 0 1 1 170 170"
            fill="none"
            stroke="#E0E0E0"
            strokeWidth="20"
            strokeLinecap="round"
          />

          {/* Colored arc */}
          <path
            d="M 30 170 A 80 80 0 1 1 170 170"
            fill="none"
            stroke={getColor()}
            strokeWidth="20"
            strokeLinecap="round"
            strokeDasharray={`${(270 * normalizedValue) / 100} 270`}
            style={{ transition: 'stroke-dasharray 0.5s ease' }}
          />

          {/* Needle */}
          <line
            x1="100"
            y1="100"
            x2="100"
            y2="40"
            stroke="#0F3E5C"
            strokeWidth="3"
            strokeLinecap="round"
            transform={`rotate(${angle} 100 100)`}
            style={{ transition: 'transform 0.5s ease' }}
          />

          {/* Center circle */}
          <circle cx="100" cy="100" r="8" fill="#0F3E5C" />
        </svg>

        {/* Value display */}
        <Box
          sx={{
            position: 'absolute',
            bottom: 30,
            textAlign: 'center',
          }}
        >
          <Typography
            variant="h4"
            sx={{
              fontWeight: 'bold',
              color: '#0F3E5C',
            }}
          >
            {latestValue.toFixed(decimals)}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {units}
          </Typography>
        </Box>
      </Box>

      {/* Min/Max labels */}
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          width: '180px',
          mt: 1,
        }}
      >
        <Typography variant="caption" color="text.secondary">
          {min}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {max}
        </Typography>
      </Box>
    </Paper>
  )
}

// Widget descriptor
const descriptor: WidgetTypeDescriptor = {
  id: 'gauge',
  name: 'Gauge',
  description: 'Display value as a circular gauge meter',
  type: 'latest',
  tags: ['card', 'gauge', 'meter', 'indicator'],
}

// Register widget
registerWidget(descriptor, Gauge)

export default Gauge
