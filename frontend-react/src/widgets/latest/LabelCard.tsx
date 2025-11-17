/**
 * Label Card Widget
 * Simple card showing label and value
 */

import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function LabelCard({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const showTitle = config.showTitle ?? true
  const decimals = config.settings?.decimals ?? 2
  const units = config.settings?.units || ''
  const backgroundColor = config.settings?.backgroundColor || '#FFFFFF'
  const textColor = config.settings?.textColor || '#0F3E5C'

  // Get latest value
  const latestValue =
    data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 0

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
        backgroundColor,
      }}
    >
      {showTitle && (
        <Typography
          variant="subtitle2"
          sx={{
            mb: 1,
            fontSize: '12px',
            fontWeight: 'medium',
            color: '#757575',
            textTransform: 'uppercase',
            letterSpacing: 1,
          }}
        >
          {config.title || 'Label'}
        </Typography>
      )}

      <Box sx={{ textAlign: 'center' }}>
        <Typography
          variant="h3"
          sx={{
            fontWeight: 'bold',
            color: textColor,
            fontSize: '48px',
          }}
        >
          {latestValue.toFixed(decimals)}
        </Typography>
        {units && (
          <Typography
            variant="h6"
            sx={{
              color: '#757575',
              mt: 1,
            }}
          >
            {units}
          </Typography>
        )}
      </Box>
    </Paper>
  )
}

// Widget descriptor
const descriptor: WidgetTypeDescriptor = {
  id: 'label_card',
  name: 'Label Card',
  description: 'Simple card showing label and value',
  type: 'latest',
  tags: ['card', 'label', 'simple'],
}

// Register widget
registerWidget(descriptor, LabelCard)

export default LabelCard
