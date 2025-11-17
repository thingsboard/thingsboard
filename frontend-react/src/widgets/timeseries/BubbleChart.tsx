/**
 * Bubble Chart Widget - 3-dimensional scatter plot
 * Shows X, Y position with bubble size as third dimension
 */
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface BubbleData {
  name: string
  x: number
  y: number
  size: number
  color: string
  category: string
}

function BubbleChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo bubble data (device metrics)
  const bubbles: BubbleData[] = config.settings?.bubbles || [
    { name: 'Device A', x: 45, y: 85, size: 120, color: '#0F3E5C', category: 'Sensors' },
    { name: 'Device B', x: 75, y: 60, size: 80, color: '#2E7D6F', category: 'Sensors' },
    { name: 'Device C', x: 30, y: 40, size: 95, color: '#FFB300', category: 'Actuators' },
    { name: 'Device D', x: 85, y: 75, size: 150, color: '#C62828', category: 'Gateways' },
    { name: 'Device E', x: 55, y: 50, size: 65, color: '#1E88E5', category: 'Sensors' },
    { name: 'Device F', x: 65, y: 90, size: 110, color: '#7B1FA2', category: 'Actuators' },
    { name: 'Device G', x: 40, y: 70, size: 85, color: '#00ACC1', category: 'Gateways' },
    { name: 'Device H', x: 90, y: 45, size: 100, color: '#FB8C00', category: 'Sensors' },
  ]

  const chartWidth = 600
  const chartHeight = 400
  const padding = 60

  const maxX = 100
  const maxY = 100
  const maxSize = Math.max(...bubbles.map((b) => b.size))

  const getX = (value: number) => padding + (value / maxX) * (chartWidth - 2 * padding)
  const getY = (value: number) => chartHeight - padding - (value / maxY) * (chartHeight - 2 * padding)
  const getRadius = (size: number) => (size / maxSize) * 40 + 10

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Bubble Chart'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <svg width={chartWidth} height={chartHeight} viewBox={`0 0 ${chartWidth} ${chartHeight}`}>
          {/* Grid */}
          {[0, 25, 50, 75, 100].map((value, i) => {
            const x = getX(value)
            const y = getY(value)
            return (
              <g key={i}>
                {/* Vertical grid line */}
                <line
                  x1={x}
                  y1={padding}
                  x2={x}
                  y2={chartHeight - padding}
                  stroke="#E0E0E0"
                  strokeWidth={1}
                  strokeDasharray="2 2"
                />
                {/* Horizontal grid line */}
                <line
                  x1={padding}
                  y1={y}
                  x2={chartWidth - padding}
                  y2={y}
                  stroke="#E0E0E0"
                  strokeWidth={1}
                  strokeDasharray="2 2"
                />
                {/* X axis label */}
                <text x={x} y={chartHeight - padding + 20} fontSize="10" fill="#757575" textAnchor="middle">
                  {value}
                </text>
                {/* Y axis label */}
                <text x={padding - 20} y={y + 4} fontSize="10" fill="#757575" textAnchor="middle">
                  {value}
                </text>
              </g>
            )
          })}

          {/* Axes */}
          <line
            x1={padding}
            y1={chartHeight - padding}
            x2={chartWidth - padding}
            y2={chartHeight - padding}
            stroke="#0F3E5C"
            strokeWidth={2}
          />
          <line
            x1={padding}
            y1={padding}
            x2={padding}
            y2={chartHeight - padding}
            stroke="#0F3E5C"
            strokeWidth={2}
          />

          {/* Axis labels */}
          <text
            x={chartWidth / 2}
            y={chartHeight - 10}
            fontSize="12"
            fontWeight="bold"
            fill="#0F3E5C"
            textAnchor="middle"
          >
            {config.settings?.xLabel || 'Performance (%)'}
          </text>
          <text
            x={20}
            y={chartHeight / 2}
            fontSize="12"
            fontWeight="bold"
            fill="#0F3E5C"
            textAnchor="middle"
            transform={`rotate(-90, 20, ${chartHeight / 2})`}
          >
            {config.settings?.yLabel || 'Reliability (%)'}
          </text>

          {/* Bubbles */}
          {bubbles.map((bubble, index) => {
            const cx = getX(bubble.x)
            const cy = getY(bubble.y)
            const r = getRadius(bubble.size)

            return (
              <g key={index}>
                {/* Bubble */}
                <circle
                  cx={cx}
                  cy={cy}
                  r={r}
                  fill={bubble.color}
                  fillOpacity={0.6}
                  stroke={bubble.color}
                  strokeWidth={2}
                  style={{
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.setAttribute('fill-opacity', '0.9')
                    e.currentTarget.setAttribute('r', (r * 1.1).toString())
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.setAttribute('fill-opacity', '0.6')
                    e.currentTarget.setAttribute('r', r.toString())
                  }}
                >
                  <title>
                    {bubble.name}
                    {'\n'}X: {bubble.x}
                    {'\n'}Y: {bubble.y}
                    {'\n'}Size: {bubble.size}
                    {'\n'}Category: {bubble.category}
                  </title>
                </circle>

                {/* Label */}
                <text
                  x={cx}
                  y={cy + 4}
                  fontSize="10"
                  fontWeight="bold"
                  fill="white"
                  textAnchor="middle"
                  pointerEvents="none"
                >
                  {bubble.name}
                </text>
              </g>
            )
          })}
        </svg>
      </Box>

      {/* Legend */}
      <Box sx={{ mt: 2, display: 'flex', flexWrap: 'wrap', gap: 2, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        {Array.from(new Set(bubbles.map((b) => b.category))).map((category) => {
          const bubble = bubbles.find((b) => b.category === category)!
          return (
            <Box key={category} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <Box
                sx={{
                  width: 16,
                  height: 16,
                  borderRadius: '50%',
                  bgcolor: bubble.color,
                  opacity: 0.8,
                }}
              />
              <Typography variant="caption" sx={{ fontSize: '11px', color: '#757575' }}>
                {category}
              </Typography>
            </Box>
          )
        })}
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'bubble_chart',
  name: 'Bubble Chart',
  description: '3-dimensional scatter plot with sized bubbles',
  type: 'timeseries',
  tags: ['chart', 'bubble', 'scatter', '3d', 'correlation'],
}

registerWidget(descriptor, BubbleChart)
export default BubbleChart
