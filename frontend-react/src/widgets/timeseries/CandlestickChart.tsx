/**
 * Candlestick Chart Widget - Financial-style time series
 * Shows OHLC (Open, High, Low, Close) data patterns
 */
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface CandleData {
  timestamp: number
  open: number
  high: number
  low: number
  close: number
}

function CandlestickChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Generate demo candlestick data
  const generateCandleData = (): CandleData[] => {
    const candles: CandleData[] = []
    let baseValue = 100
    const now = Date.now()

    for (let i = 0; i < 30; i++) {
      const change = (Math.random() - 0.5) * 10
      const open = baseValue
      const close = baseValue + change
      const high = Math.max(open, close) + Math.random() * 5
      const low = Math.min(open, close) - Math.random() * 5

      candles.push({
        timestamp: now - (29 - i) * 3600000, // Hourly data
        open,
        high,
        low,
        close,
      })

      baseValue = close
    }

    return candles
  }

  const candles = generateCandleData()
  const allValues = candles.flatMap((c) => [c.high, c.low])
  const maxValue = Math.max(...allValues)
  const minValue = Math.min(...allValues)
  const range = maxValue - minValue || 1

  const chartWidth = 800
  const chartHeight = 300
  const candleWidth = (chartWidth - 40) / candles.length
  const bodyWidth = candleWidth * 0.6

  const getY = (value: number) => {
    return chartHeight - 40 - ((value - minValue) / range) * (chartHeight - 60)
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Candlestick Chart'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        <svg width="100%" height={chartHeight} viewBox={`0 0 ${chartWidth} ${chartHeight}`}>
          {/* Grid lines */}
          {[0, 0.25, 0.5, 0.75, 1].map((percent, i) => {
            const y = 20 + percent * (chartHeight - 60)
            const value = maxValue - percent * range
            return (
              <g key={i}>
                <line x1={20} y1={y} x2={chartWidth - 20} y2={y} stroke="#E0E0E0" strokeWidth={1} />
                <text x={5} y={y + 4} fontSize="10" fill="#757575">
                  {value.toFixed(0)}
                </text>
              </g>
            )
          })}

          {/* Candles */}
          {candles.map((candle, i) => {
            const x = 20 + i * candleWidth + candleWidth / 2
            const isRising = candle.close >= candle.open
            const color = isRising ? '#2E7D6F' : '#C62828'

            const highY = getY(candle.high)
            const lowY = getY(candle.low)
            const openY = getY(candle.open)
            const closeY = getY(candle.close)

            const bodyTop = Math.min(openY, closeY)
            const bodyHeight = Math.abs(closeY - openY) || 1

            return (
              <g key={i}>
                {/* Wick (high-low line) */}
                <line x1={x} y1={highY} x2={x} y2={lowY} stroke={color} strokeWidth={1.5} />

                {/* Body (open-close rectangle) */}
                <rect
                  x={x - bodyWidth / 2}
                  y={bodyTop}
                  width={bodyWidth}
                  height={bodyHeight}
                  fill={isRising ? color : 'white'}
                  stroke={color}
                  strokeWidth={2}
                  style={{ cursor: 'pointer' }}
                >
                  <title>
                    {new Date(candle.timestamp).toLocaleString()}
                    {'\n'}O: {candle.open.toFixed(2)}
                    {'\n'}H: {candle.high.toFixed(2)}
                    {'\n'}L: {candle.low.toFixed(2)}
                    {'\n'}C: {candle.close.toFixed(2)}
                  </title>
                </rect>
              </g>
            )
          })}
        </svg>
      </Box>

      {/* Statistics */}
      <Box sx={{ mt: 2, display: 'flex', gap: 3, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
            High
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 'bold', color: '#2E7D6F' }}>
            {maxValue.toFixed(2)}
          </Typography>
        </Box>
        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
            Low
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 'bold', color: '#C62828' }}>
            {minValue.toFixed(2)}
          </Typography>
        </Box>
        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
            Last Close
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 'bold', color: '#0F3E5C' }}>
            {candles[candles.length - 1].close.toFixed(2)}
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'candlestick_chart',
  name: 'Candlestick Chart',
  description: 'OHLC financial-style chart for pattern analysis',
  type: 'timeseries',
  tags: ['chart', 'candlestick', 'ohlc', 'financial', 'patterns'],
}

registerWidget(descriptor, CandlestickChart)
export default CandlestickChart
