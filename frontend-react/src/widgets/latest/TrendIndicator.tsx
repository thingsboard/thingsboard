/**
 * Trend Indicator Widget - Visual trend analysis
 * Shows trend direction with sparkline and statistics
 */
import { Box, Typography, Paper } from '@mui/material'
import {
  TrendingUp,
  TrendingDown,
  Remove as TrendingFlat,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function TrendIndicator({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const latestValue = data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 75.5

  // Generate sparkline data (in production, use actual historical data)
  const sparklineData = Array.from({ length: 20 }, (_, i) => {
    return latestValue + Math.sin(i / 3) * 10 + (Math.random() - 0.5) * 5
  })

  // Calculate trend
  const firstValue = sparklineData[0]
  const lastValue = sparklineData[sparklineData.length - 1]
  const change = lastValue - firstValue
  const changePercent = (change / firstValue) * 100

  const getTrendDirection = () => {
    if (changePercent > 1) return 'up'
    if (changePercent < -1) return 'down'
    return 'flat'
  }

  const trendDirection = getTrendDirection()

  const trendConfig = {
    up: {
      icon: TrendingUp,
      color: '#2E7D6F',
      bgcolor: 'rgba(46, 125, 111, 0.1)',
      label: 'Increasing',
    },
    down: {
      icon: TrendingDown,
      color: '#C62828',
      bgcolor: 'rgba(198, 40, 40, 0.1)',
      label: 'Decreasing',
    },
    flat: {
      icon: TrendingFlat,
      color: '#757575',
      bgcolor: 'rgba(117, 117, 117, 0.1)',
      label: 'Stable',
    },
  }

  const trend = trendConfig[trendDirection]
  const TrendIcon = trend.icon

  // Sparkline SVG
  const sparklineWidth = 200
  const sparklineHeight = 60
  const max = Math.max(...sparklineData)
  const min = Math.min(...sparklineData)
  const range = max - min || 1

  const points = sparklineData
    .map((value, index) => {
      const x = (index / (sparklineData.length - 1)) * sparklineWidth
      const y = sparklineHeight - ((value - min) / range) * sparklineHeight
      return `${x},${y}`
    })
    .join(' ')

  return (
    <Paper
      elevation={1}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        p: 3,
        background: `linear-gradient(135deg, ${trend.bgcolor} 0%, white 100%)`,
      }}
    >
      {config.showTitle && (
        <Typography variant="subtitle1" sx={{ mb: 2, fontSize: '14px', fontWeight: 'bold', color: '#757575' }}>
          {config.title || 'Trend Analysis'}
        </Typography>
      )}

      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        {/* Current Value */}
        <Box>
          <Typography variant="h3" sx={{ fontWeight: 'bold', color: '#0F3E5C', fontSize: '36px' }}>
            {latestValue.toFixed(1)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {config.settings?.units || 'units'}
          </Typography>
        </Box>

        {/* Trend Icon */}
        <Box
          sx={{
            width: 60,
            height: 60,
            borderRadius: '50%',
            bgcolor: trend.color,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: `0 4px 12px ${trend.color}40`,
          }}
        >
          <TrendIcon sx={{ fontSize: 36, color: 'white' }} />
        </Box>
      </Box>

      {/* Change Stats */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
            Change
          </Typography>
          <Typography
            variant="body2"
            sx={{
              fontWeight: 'bold',
              color: trend.color,
              fontSize: '14px',
            }}
          >
            {change > 0 ? '+' : ''}{change.toFixed(2)}
          </Typography>
        </Box>

        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
            Percent
          </Typography>
          <Typography
            variant="body2"
            sx={{
              fontWeight: 'bold',
              color: trend.color,
              fontSize: '14px',
            }}
          >
            {changePercent > 0 ? '+' : ''}{changePercent.toFixed(1)}%
          </Typography>
        </Box>

        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
            Status
          </Typography>
          <Typography
            variant="body2"
            sx={{
              fontWeight: 'bold',
              color: trend.color,
              fontSize: '14px',
            }}
          >
            {trend.label}
          </Typography>
        </Box>
      </Box>

      {/* Sparkline */}
      <Box sx={{ flex: 1, display: 'flex', alignItems: 'flex-end' }}>
        <svg
          width="100%"
          height={sparklineHeight}
          viewBox={`0 0 ${sparklineWidth} ${sparklineHeight}`}
          preserveAspectRatio="none"
        >
          {/* Area fill */}
          <polygon
            points={`0,${sparklineHeight} ${points} ${sparklineWidth},${sparklineHeight}`}
            fill={trend.color}
            fillOpacity="0.2"
          />

          {/* Line */}
          <polyline
            points={points}
            fill="none"
            stroke={trend.color}
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* Dots at endpoints */}
          <circle
            cx={0}
            cy={sparklineHeight - ((sparklineData[0] - min) / range) * sparklineHeight}
            r="3"
            fill={trend.color}
          />
          <circle
            cx={sparklineWidth}
            cy={sparklineHeight - ((sparklineData[sparklineData.length - 1] - min) / range) * sparklineHeight}
            r="3"
            fill={trend.color}
          />
        </svg>
      </Box>

      {/* Range info */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
        <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
          Min: {min.toFixed(1)}
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
          Max: {max.toFixed(1)}
        </Typography>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'trend_indicator',
  name: 'Trend Indicator',
  description: 'Visual trend analysis with sparkline and statistics',
  type: 'latest',
  tags: ['trend', 'indicator', 'sparkline', 'analysis'],
}

registerWidget(descriptor, TrendIndicator)
export default TrendIndicator
