/**
 * Waterfall Chart Widget - Cumulative change visualization
 * Shows how values accumulate through sequential changes
 */
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface WaterfallItem {
  label: string
  value: number
  isTotal?: boolean
}

function WaterfallChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo waterfall data (budget breakdown)
  const items: WaterfallItem[] = config.settings?.items || [
    { label: 'Revenue', value: 100000, isTotal: true },
    { label: 'Materials', value: -25000 },
    { label: 'Labor', value: -18000 },
    { label: 'Equipment', value: -12000 },
    { label: 'Energy', value: -8000 },
    { label: 'Overhead', value: -7000 },
    { label: 'Net Profit', value: 0, isTotal: true },
  ]

  // Calculate cumulative values
  let cumulative = 0
  const bars = items.map((item, index) => {
    if (item.isTotal && index > 0) {
      // Total bar shows cumulative value
      return {
        ...item,
        start: 0,
        end: cumulative,
        value: cumulative,
      }
    } else if (item.isTotal) {
      // First total (starting value)
      cumulative = item.value
      return {
        ...item,
        start: 0,
        end: item.value,
      }
    } else {
      // Regular bar
      const start = cumulative
      cumulative += item.value
      return {
        ...item,
        start: item.value >= 0 ? start : cumulative,
        end: item.value >= 0 ? cumulative : start,
      }
    }
  })

  const maxValue = Math.max(...bars.map((b) => Math.max(b.start, b.end)))
  const minValue = Math.min(...bars.map((b) => Math.min(b.start, b.end)))
  const range = maxValue - minValue || 1

  const chartWidth = 700
  const chartHeight = 350
  const barWidth = (chartWidth - 100) / bars.length
  const chartAreaHeight = chartHeight - 80

  const getY = (value: number) => {
    return 40 + chartAreaHeight - ((value - minValue) / range) * chartAreaHeight
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Waterfall Chart'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto', display: 'flex', justifyContent: 'center' }}>
        <svg width={chartWidth} height={chartHeight} viewBox={`0 0 ${chartWidth} ${chartHeight}`}>
          {/* Grid lines */}
          {[0, 0.25, 0.5, 0.75, 1].map((percent, i) => {
            const value = minValue + percent * range
            const y = getY(value)
            return (
              <g key={i}>
                <line x1={50} y1={y} x2={chartWidth - 20} y2={y} stroke="#E0E0E0" strokeWidth={1} strokeDasharray="4 2" />
                <text x={10} y={y + 4} fontSize="10" fill="#757575">
                  {(value / 1000).toFixed(0)}K
                </text>
              </g>
            )
          })}

          {/* Zero line */}
          {minValue < 0 && (
            <line x1={50} y1={getY(0)} x2={chartWidth - 20} y2={getY(0)} stroke="#0F3E5C" strokeWidth={2} />
          )}

          {/* Bars and connectors */}
          {bars.map((bar, i) => {
            const x = 50 + i * barWidth
            const barActualWidth = barWidth * 0.7
            const startY = getY(bar.start)
            const endY = getY(bar.end)
            const barHeight = Math.abs(endY - startY)
            const barTop = Math.min(startY, endY)

            const isPositive = bar.value >= 0
            const isTotal = bar.isTotal
            const color = isTotal ? '#0F3E5C' : isPositive ? '#2E7D6F' : '#C62828'

            return (
              <g key={i}>
                {/* Connector to next bar */}
                {i < bars.length - 1 && !bars[i + 1].isTotal && (
                  <line
                    x1={x + barActualWidth}
                    y1={endY}
                    x2={x + barWidth}
                    y2={endY}
                    stroke="#999"
                    strokeWidth={1}
                    strokeDasharray="3 2"
                  />
                )}

                {/* Bar */}
                <rect
                  x={x}
                  y={barTop}
                  width={barActualWidth}
                  height={Math.max(barHeight, 2)}
                  fill={color}
                  stroke={color}
                  strokeWidth={2}
                  opacity={isTotal ? 0.9 : 0.8}
                  style={{ cursor: 'pointer' }}
                >
                  <title>
                    {bar.label}: {bar.value >= 0 ? '+' : ''}
                    {(bar.value / 1000).toFixed(1)}K
                  </title>
                </rect>

                {/* Value label */}
                <text
                  x={x + barActualWidth / 2}
                  y={barTop - 5}
                  fontSize="11"
                  fontWeight="bold"
                  fill={color}
                  textAnchor="middle"
                >
                  {bar.value >= 0 ? '+' : ''}
                  {(bar.value / 1000).toFixed(0)}K
                </text>

                {/* Category label */}
                <text
                  x={x + barActualWidth / 2}
                  y={chartHeight - 15}
                  fontSize="10"
                  fill="#757575"
                  textAnchor="middle"
                  transform={`rotate(-45, ${x + barActualWidth / 2}, ${chartHeight - 15})`}
                >
                  {bar.label}
                </text>
              </g>
            )
          })}
        </svg>
      </Box>

      {/* Summary */}
      <Box sx={{ mt: 2, display: 'flex', justifyContent: 'space-between', p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
            Starting Value
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 'bold', color: '#0F3E5C' }}>
            ${(items[0].value / 1000).toFixed(0)}K
          </Typography>
        </Box>
        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
            Total Changes
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 'bold', color: '#C62828' }}>
            ${(cumulative - items[0].value) / 1000 >= 0 ? '+' : ''}
            {((cumulative - items[0].value) / 1000).toFixed(0)}K
          </Typography>
        </Box>
        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
            Final Value
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 'bold', color: '#2E7D6F' }}>
            ${(cumulative / 1000).toFixed(0)}K
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'waterfall_chart',
  name: 'Waterfall Chart',
  description: 'Cumulative change visualization for sequential values',
  type: 'timeseries',
  tags: ['chart', 'waterfall', 'cumulative', 'financial', 'bridge'],
}

registerWidget(descriptor, WaterfallChart)
export default WaterfallChart
