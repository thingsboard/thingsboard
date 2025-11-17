/**
 * Device Status Matrix Widget - Grid view of multiple device statuses
 * Shows real-time status of many devices in a compact matrix
 */
import { Box, Typography, Paper, Tooltip, Chip } from '@mui/material'
import {
  CheckCircle,
  Error,
  Warning,
  RemoveCircle,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface DeviceStatus {
  id: string
  name: string
  status: 'online' | 'warning' | 'error' | 'offline'
  uptime: number
  lastSeen: number
}

function DeviceStatusMatrix({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo device data (in production, from datasources)
  const generateDevices = (): DeviceStatus[] => {
    const statuses: Array<'online' | 'warning' | 'error' | 'offline'> = ['online', 'warning', 'error', 'offline']
    const devices: DeviceStatus[] = []

    for (let i = 1; i <= 40; i++) {
      const statusIndex = Math.floor(Math.random() * 10) // Weighted towards online
      const status = statusIndex < 7 ? 'online' : statusIndex < 8 ? 'warning' : statusIndex < 9 ? 'error' : 'offline'

      devices.push({
        id: `DEV${i.toString().padStart(3, '0')}`,
        name: `Device ${i}`,
        status,
        uptime: Math.random() * 100,
        lastSeen: Date.now() - Math.random() * 3600000,
      })
    }

    return devices
  }

  const devices = generateDevices()

  const statusConfig = {
    online: {
      icon: CheckCircle,
      color: '#2E7D6F',
      label: 'Online',
    },
    warning: {
      icon: Warning,
      color: '#FFB300',
      label: 'Warning',
    },
    error: {
      icon: Error,
      color: '#C62828',
      label: 'Error',
    },
    offline: {
      icon: RemoveCircle,
      color: '#757575',
      label: 'Offline',
    },
  }

  const statusCounts = {
    online: devices.filter((d) => d.status === 'online').length,
    warning: devices.filter((d) => d.status === 'warning').length,
    error: devices.filter((d) => d.status === 'error').length,
    offline: devices.filter((d) => d.status === 'offline').length,
  }

  const formatLastSeen = (timestamp: number): string => {
    const seconds = Math.floor((Date.now() - timestamp) / 1000)
    if (seconds < 60) return `${seconds}s ago`
    const minutes = Math.floor(seconds / 60)
    if (minutes < 60) return `${minutes}m ago`
    const hours = Math.floor(minutes / 60)
    return `${hours}h ago`
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Device Status Matrix'}
        </Typography>
      )}

      {/* Summary */}
      <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
        {Object.entries(statusCounts).map(([status, count]) => {
          const config = statusConfig[status as keyof typeof statusConfig]
          return (
            <Chip
              key={status}
              label={`${config.label}: ${count}`}
              size="small"
              sx={{
                bgcolor: config.color,
                color: 'white',
                fontWeight: 'bold',
                fontSize: '11px',
              }}
            />
          )
        })}
      </Box>

      {/* Device Matrix */}
      <Box sx={{ flex: 1, overflow: 'auto' }}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(80px, 1fr))',
            gap: 1,
          }}
        >
          {devices.map((device) => {
            const config = statusConfig[device.status]
            const StatusIcon = config.icon

            return (
              <Tooltip
                key={device.id}
                title={
                  <Box sx={{ p: 0.5 }}>
                    <Typography variant="caption" sx={{ fontWeight: 'bold', display: 'block' }}>
                      {device.name}
                    </Typography>
                    <Typography variant="caption" sx={{ fontSize: '10px', display: 'block' }}>
                      ID: {device.id}
                    </Typography>
                    <Typography variant="caption" sx={{ fontSize: '10px', display: 'block' }}>
                      Status: {config.label}
                    </Typography>
                    <Typography variant="caption" sx={{ fontSize: '10px', display: 'block' }}>
                      Uptime: {device.uptime.toFixed(1)}%
                    </Typography>
                    <Typography variant="caption" sx={{ fontSize: '10px', display: 'block' }}>
                      Last Seen: {formatLastSeen(device.lastSeen)}
                    </Typography>
                  </Box>
                }
                arrow
              >
                <Box
                  sx={{
                    p: 1,
                    borderRadius: 1,
                    bgcolor: '#F5F5F5',
                    border: `2px solid ${config.color}`,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: 0.5,
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    '&:hover': {
                      bgcolor: config.color,
                      color: 'white',
                      transform: 'scale(1.05)',
                      '& .MuiSvgIcon-root': {
                        color: 'white',
                      },
                      '& .MuiTypography-root': {
                        color: 'white',
                      },
                    },
                  }}
                >
                  <StatusIcon sx={{ fontSize: 24, color: config.color }} />
                  <Typography
                    variant="caption"
                    sx={{
                      fontSize: '9px',
                      fontWeight: 'bold',
                      textAlign: 'center',
                      color: '#0F3E5C',
                      lineHeight: 1,
                    }}
                  >
                    {device.id}
                  </Typography>
                </Box>
              </Tooltip>
            )
          })}
        </Box>
      </Box>

      {/* Footer Stats */}
      <Box sx={{ mt: 2, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
            Total Devices: {devices.length}
          </Typography>
          <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold', color: '#2E7D6F' }}>
            Health: {((statusCounts.online / devices.length) * 100).toFixed(0)}%
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'device_status_matrix',
  name: 'Device Status Matrix',
  description: 'Compact grid view showing status of multiple devices',
  type: 'latest',
  tags: ['devices', 'status', 'matrix', 'monitoring', 'grid'],
}

registerWidget(descriptor, DeviceStatusMatrix)
export default DeviceStatusMatrix
