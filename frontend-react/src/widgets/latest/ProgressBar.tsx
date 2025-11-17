/**
 * Progress Bar Widget
 * Displays a value as a progress bar with percentage
 */

import { Box, Typography, Paper, LinearProgress } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function ProgressBar({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const showTitle = config.showTitle ?? true
  const decimals = config.settings?.decimals ?? 0
  const min = config.settings?.min ?? 0
  const max = config.settings?.max ?? 100
  const units = config.settings?.units || '%'

  // Get latest value
  const latestValue =
    data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 0

  // Calculate percentage
  const percentage = ((latestValue - min) / (max - min)) * 100
  const normalizedValue = Math.max(0, Math.min(100, percentage))

  // Color based on value
  const getColor = () => {
    if (normalizedValue < 33) return 'error'
    if (normalizedValue < 66) return 'warning'
    return 'success'
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
      }}
    >
      {showTitle && (
        <Typography
          variant="subtitle1"
          sx={{
            mb: 2,
            fontSize: '14px',
            fontWeight: 'bold',
            color: '#757575',
            textAlign: 'center',
          }}
        >
          {config.title || 'Progress'}
        </Typography>
      )}

      <Box sx={{ mb: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
          <Typography variant="body2" color="text.secondary">
            {min}{units}
          </Typography>
          <Typography variant="h5" sx={{ fontWeight: 'bold', color: '#0F3E5C' }}>
            {latestValue.toFixed(decimals)}{units}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {max}{units}
          </Typography>
        </Box>

        <LinearProgress
          variant="determinate"
          value={normalizedValue}
          color={getColor()}
          sx={{
            height: 12,
            borderRadius: 6,
            bgcolor: '#E0E0E0',
          }}
        />
      </Box>

      <Typography
        variant="h6"
        sx={{
          textAlign: 'center',
          color: '#757575',
        }}
      >
        {normalizedValue.toFixed(1)}% Complete
      </Typography>
    </Paper>
  )
}

// Widget descriptor
const descriptor: WidgetTypeDescriptor = {
  id: 'progress_bar',
  name: 'Progress Bar',
  description: 'Display value as a progress bar with percentage',
  type: 'latest',
  tags: ['card', 'progress', 'indicator'],
}

// Register widget
registerWidget(descriptor, ProgressBar)

export default ProgressBar
