/**
 * System Monitor Widget - Real-time system metrics display
 * Shows CPU, Memory, Disk, Network usage
 */
import { Box, Typography, Paper, LinearProgress } from '@mui/material'
import {
  Memory as CPUIcon,
  Storage as MemoryIcon,
  SdStorage as DiskIcon,
  NetworkCheck as NetworkIcon,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface SystemMetric {
  name: string
  value: number
  max: number
  unit: string
  icon: any
  color: string
}

function SystemMonitor({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo system metrics (in production, from real system data)
  const metrics: SystemMetric[] = [
    {
      name: 'CPU Usage',
      value: 45.3,
      max: 100,
      unit: '%',
      icon: CPUIcon,
      color: '#C62828',
    },
    {
      name: 'Memory',
      value: 6.2,
      max: 16,
      unit: 'GB',
      icon: MemoryIcon,
      color: '#FFB300',
    },
    {
      name: 'Disk Usage',
      value: 256,
      max: 512,
      unit: 'GB',
      icon: DiskIcon,
      color: '#2E7D6F',
    },
    {
      name: 'Network',
      value: 125,
      max: 1000,
      unit: 'Mbps',
      icon: NetworkIcon,
      color: '#1E88E5',
    },
  ]

  const getPercentage = (value: number, max: number): number => {
    return (value / max) * 100
  }

  const getProgressColor = (percentage: number): string => {
    if (percentage >= 90) return '#C62828'
    if (percentage >= 75) return '#FFB300'
    return '#2E7D6F'
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'System Monitor'}
        </Typography>
      )}

      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 3 }}>
        {metrics.map((metric) => {
          const Icon = metric.icon
          const percentage = getPercentage(metric.value, metric.max)
          const progressColor = getProgressColor(percentage)

          return (
            <Box key={metric.name}>
              {/* Header */}
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Icon sx={{ fontSize: 20, color: metric.color }} />
                  <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: '#0F3E5C', fontSize: '13px' }}>
                    {metric.name}
                  </Typography>
                </Box>
                <Typography variant="body2" sx={{ fontWeight: 'bold', color: metric.color }}>
                  {metric.value.toFixed(1)} / {metric.max} {metric.unit}
                </Typography>
              </Box>

              {/* Progress Bar */}
              <Box sx={{ position: 'relative' }}>
                <LinearProgress
                  variant="determinate"
                  value={percentage}
                  sx={{
                    height: 12,
                    borderRadius: 6,
                    bgcolor: '#E0E0E0',
                    '& .MuiLinearProgress-bar': {
                      bgcolor: progressColor,
                      borderRadius: 6,
                      transition: 'all 0.5s ease',
                    },
                  }}
                />
                <Typography
                  variant="caption"
                  sx={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    fontSize: '9px',
                    fontWeight: 'bold',
                    color: percentage > 50 ? 'white' : '#0F3E5C',
                    textShadow: percentage > 50 ? '0 0 2px rgba(0,0,0,0.5)' : 'none',
                  }}
                >
                  {percentage.toFixed(1)}%
                </Typography>
              </Box>

              {/* Status indicator */}
              <Box sx={{ mt: 0.5, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
                  {percentage < 75 ? 'Normal' : percentage < 90 ? 'High' : 'Critical'}
                </Typography>
                <Box sx={{ display: 'flex', gap: 0.5 }}>
                  {[1, 2, 3, 4, 5].map((bar) => (
                    <Box
                      key={bar}
                      sx={{
                        width: 4,
                        height: 8 + bar * 2,
                        bgcolor: bar * 20 <= percentage ? progressColor : '#E0E0E0',
                        borderRadius: 0.5,
                        transition: 'all 0.3s ease',
                      }}
                    />
                  ))}
                </Box>
              </Box>
            </Box>
          )
        })}
      </Box>

      {/* Footer Summary */}
      <Box sx={{ mt: 2, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
            System Health:
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: '#2E7D6F' }} />
            <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold', color: '#2E7D6F' }}>
              Healthy
            </Typography>
          </Box>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'system_monitor',
  name: 'System Monitor',
  description: 'Real-time system metrics (CPU, Memory, Disk, Network)',
  type: 'latest',
  tags: ['system', 'monitor', 'performance', 'metrics', 'resources'],
}

registerWidget(descriptor, SystemMonitor)
export default SystemMonitor
