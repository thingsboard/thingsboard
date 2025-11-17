/**
 * Heatmap Chart Widget - 2D heatmap visualization
 * Shows data intensity across two dimensions
 */
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface HeatmapCell {
  x: string
  y: string
  value: number
}

function HeatmapChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo heatmap data (hourly temperature by day of week)
  const generateHeatmapData = (): HeatmapCell[] => {
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
    const hours = ['00', '04', '08', '12', '16', '20']
    const data: HeatmapCell[] = []

    days.forEach((day) => {
      hours.forEach((hour) => {
        data.push({
          x: hour,
          y: day,
          value: Math.random() * 100,
        })
      })
    })

    return data
  }

  const heatmapData = generateHeatmapData()
  const hours = Array.from(new Set(heatmapData.map((d) => d.x)))
  const days = Array.from(new Set(heatmapData.map((d) => d.y)))
  const maxValue = Math.max(...heatmapData.map((d) => d.value))

  const getColor = (value: number) => {
    const intensity = value / maxValue
    if (intensity < 0.2) return '#E3F2FD'
    if (intensity < 0.4) return '#90CAF9'
    if (intensity < 0.6) return '#42A5F5'
    if (intensity < 0.8) return '#1E88E5'
    return '#0D47A1'
  }

  const getCellValue = (hour: string, day: string): number => {
    const cell = heatmapData.find((d) => d.x === hour && d.y === day)
    return cell ? cell.value : 0
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Heatmap'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <Box sx={{ display: 'inline-block' }}>
          {/* Header row */}
          <Box sx={{ display: 'flex', mb: 0.5 }}>
            <Box sx={{ width: 50 }} /> {/* Empty corner */}
            {hours.map((hour) => (
              <Box
                key={hour}
                sx={{
                  width: 50,
                  textAlign: 'center',
                  fontSize: '11px',
                  fontWeight: 'bold',
                  color: '#757575',
                }}
              >
                {hour}
              </Box>
            ))}
          </Box>

          {/* Data rows */}
          {days.map((day) => (
            <Box key={day} sx={{ display: 'flex', mb: 0.5 }}>
              {/* Row label */}
              <Box
                sx={{
                  width: 50,
                  display: 'flex',
                  alignItems: 'center',
                  fontSize: '11px',
                  fontWeight: 'bold',
                  color: '#757575',
                  pr: 1,
                }}
              >
                {day}
              </Box>

              {/* Cells */}
              {hours.map((hour) => {
                const value = getCellValue(hour, day)
                const color = getColor(value)

                return (
                  <Box
                    key={`${hour}-${day}`}
                    sx={{
                      width: 50,
                      height: 35,
                      bgcolor: color,
                      border: '1px solid white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      transition: 'all 0.2s ease',
                      '&:hover': {
                        transform: 'scale(1.1)',
                        zIndex: 10,
                        boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
                      },
                    }}
                    title={`${day} ${hour}:00 - ${value.toFixed(1)}`}
                  >
                    <Typography
                      variant="caption"
                      sx={{
                        fontSize: '10px',
                        fontWeight: 'bold',
                        color: value / maxValue > 0.5 ? 'white' : '#0F3E5C',
                      }}
                    >
                      {value.toFixed(0)}
                    </Typography>
                  </Box>
                )
              })}
            </Box>
          ))}

          {/* Legend */}
          <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 1, justifyContent: 'center' }}>
            <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
              Low
            </Typography>
            {[0.1, 0.3, 0.5, 0.7, 0.9].map((intensity) => (
              <Box
                key={intensity}
                sx={{
                  width: 30,
                  height: 15,
                  bgcolor: getColor(intensity * maxValue),
                  border: '1px solid #E0E0E0',
                }}
              />
            ))}
            <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
              High
            </Typography>
          </Box>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'heatmap_chart',
  name: 'Heatmap Chart',
  description: '2D heatmap visualization for data intensity',
  type: 'timeseries',
  tags: ['chart', 'heatmap', 'matrix', 'intensity'],
}

registerWidget(descriptor, HeatmapChart)
export default HeatmapChart
