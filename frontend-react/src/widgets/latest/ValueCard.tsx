/**
 * Value Card Widget
 * Displays latest telemetry value with optional trend indicator
 * Equivalent to ThingsBoard's Digital Gauge and Cards widgets
 */

import { Card, CardContent, Typography, Box } from '@mui/material'
import { ArrowUpward, ArrowDownward } from '@mui/icons-material'
import { WidgetComponentProps } from '@/types/dashboard'
import { registerWidget } from '../widgetRegistry'

function ValueCard({ widget, data }: WidgetComponentProps) {
  const { config } = widget
  const { settings } = config

  // Get the first datasource's latest value
  const datasourceData = data.datasources[0]
  const latestValue = datasourceData?.data?.[0]?.value ?? '--'

  // Get trend data if configured
  const showTrend = settings.showTrend ?? false
  const trendValue = settings.trendValue ?? 0
  const trendPositive = trendValue >= 0

  // Format value
  const formattedValue =
    typeof latestValue === 'number'
      ? latestValue.toFixed(settings.decimals ?? 0)
      : latestValue

  const units = settings.units ?? ''

  return (
    <Card
      sx={{
        height: '100%',
        bgcolor: (theme) =>
          settings.backgroundColor ??
          (theme.palette.mode === 'dark' ? '#1F2428' : '#FFFFFF'),
        borderColor: (theme) =>
          theme.palette.mode === 'dark' ? '#363C42' : '#E0E0E0',
        borderWidth: 1,
        borderStyle: 'solid',
        borderRadius: settings.borderRadius ?? 2,
      }}
    >
      <CardContent>
        {/* Label */}
        <Typography
          variant="body2"
          sx={{
            color: '#8C959D',
            fontWeight: 500,
            mb: 1,
          }}
        >
          {config.title || datasourceData?.datasource.name || 'Value'}
        </Typography>

        {/* Value */}
        <Typography
          variant="h3"
          sx={{
            fontWeight: 'bold',
            color: (theme) =>
              settings.valueFont?.color ??
              (theme.palette.mode === 'dark' ? '#E1E3E5' : '#121517'),
            fontSize: settings.valueFont?.size ?? '2rem',
            mb: showTrend ? 1 : 0,
          }}
        >
          {formattedValue}
          {units && (
            <Typography
              component="span"
              variant="h5"
              sx={{ ml: 1, fontWeight: 'normal' }}
            >
              {units}
            </Typography>
          )}
        </Typography>

        {/* Trend Indicator */}
        {showTrend && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            {trendPositive ? (
              <ArrowUpward sx={{ fontSize: '1rem', color: '#2E7D6F' }} />
            ) : (
              <ArrowDownward sx={{ fontSize: '1rem', color: '#C62828' }} />
            )}
            <Typography
              variant="body2"
              sx={{
                fontWeight: 500,
                color: trendPositive ? '#2E7D6F' : '#C62828',
              }}
            >
              {trendPositive ? '+' : ''}
              {trendValue}%
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  )
}

// Register widget
registerWidget(
  {
    id: 'latest_value_card',
    name: 'Value Card',
    type: 'latest',
    description: 'Display latest value with optional trend indicator',
    icon: 'crop_square',
    defaultConfig: {
      datasources: [],
      settings: {
        showTitle: true,
        showValue: true,
        showTrend: false,
        decimals: 0,
        units: '',
        backgroundColor: undefined,
        borderRadius: 2,
      },
    },
    defaultSizeX: 3,
    defaultSizeY: 2,
  },
  ValueCard
)

export default ValueCard
