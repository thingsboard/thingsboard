/**
 * Alert Timeline Widget - Chronological alert/event history
 * Shows alerts and events in a timeline format
 */
import { Box, Typography, Paper, Chip } from '@mui/material'
import {
  Error,
  Warning,
  Info,
  CheckCircle,
  Timeline as TimelineIcon,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface Alert {
  id: string
  timestamp: number
  severity: 'critical' | 'warning' | 'info' | 'resolved'
  title: string
  message: string
  device: string
}

function AlertTimeline({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo alert data
  const generateAlerts = (): Alert[] => {
    const now = Date.now()
    return [
      {
        id: '1',
        timestamp: now - 300000,
        severity: 'critical',
        title: 'Temperature Exceeded',
        message: 'Device temperature reached 85°C',
        device: 'Sensor-001',
      },
      {
        id: '2',
        timestamp: now - 900000,
        severity: 'warning',
        title: 'Low Battery',
        message: 'Battery level dropped to 15%',
        device: 'Gateway-003',
      },
      {
        id: '3',
        timestamp: now - 1800000,
        severity: 'resolved',
        title: 'Connection Restored',
        message: 'Network connection re-established',
        device: 'Device-042',
      },
      {
        id: '4',
        timestamp: now - 3600000,
        severity: 'info',
        title: 'Firmware Update',
        message: 'Successfully updated to v2.1.0',
        device: 'Controller-007',
      },
      {
        id: '5',
        timestamp: now - 7200000,
        severity: 'warning',
        title: 'High Memory Usage',
        message: 'Memory usage at 82%',
        device: 'Gateway-001',
      },
      {
        id: '6',
        timestamp: now - 10800000,
        severity: 'critical',
        title: 'Communication Failure',
        message: 'Failed to communicate with server',
        device: 'Device-015',
      },
    ]
  }

  const alerts = generateAlerts()

  const severityConfig = {
    critical: {
      icon: Error,
      color: '#C62828',
      bgcolor: 'rgba(198, 40, 40, 0.1)',
      label: 'Critical',
    },
    warning: {
      icon: Warning,
      color: '#FFB300',
      bgcolor: 'rgba(255, 179, 0, 0.1)',
      label: 'Warning',
    },
    info: {
      icon: Info,
      color: '#1E88E5',
      bgcolor: 'rgba(30, 136, 229, 0.1)',
      label: 'Info',
    },
    resolved: {
      icon: CheckCircle,
      color: '#2E7D6F',
      bgcolor: 'rgba(46, 125, 111, 0.1)',
      label: 'Resolved',
    },
  }

  const formatTimestamp = (timestamp: number): string => {
    const diff = Date.now() - timestamp
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)

    if (days > 0) return `${days}d ago`
    if (hours > 0) return `${hours}h ago`
    if (minutes > 0) return `${minutes}m ago`
    return 'Just now'
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Alert Timeline'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {alerts.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <TimelineIcon sx={{ fontSize: 48, color: '#E0E0E0', mb: 1 }} />
            <Typography variant="body2" color="text.secondary">
              No alerts
            </Typography>
          </Box>
        ) : (
          <Box sx={{ position: 'relative' }}>
            {/* Timeline line */}
            <Box
              sx={{
                position: 'absolute',
                left: 19,
                top: 0,
                bottom: 0,
                width: 2,
                bgcolor: '#E0E0E0',
              }}
            />

            {/* Alert items */}
            {alerts.map((alert, index) => {
              const severityConf = severityConfig[alert.severity]
              const Icon = severityConf.icon

              return (
                <Box
                  key={alert.id}
                  sx={{
                    position: 'relative',
                    pl: 5,
                    pb: 3,
                    '&:last-child': { pb: 0 },
                  }}
                >
                  {/* Timeline dot */}
                  <Box
                    sx={{
                      position: 'absolute',
                      left: 11,
                      top: 4,
                      width: 18,
                      height: 18,
                      borderRadius: '50%',
                      bgcolor: severityConf.color,
                      border: '3px solid white',
                      boxShadow: `0 0 0 2px ${severityConf.color}`,
                      zIndex: 1,
                    }}
                  />

                  {/* Alert card */}
                  <Box
                    sx={{
                      p: 1.5,
                      borderRadius: 1,
                      bgcolor: severityConf.bgcolor,
                      border: `1px solid ${severityConf.color}40`,
                      transition: 'all 0.2s ease',
                      '&:hover': {
                        boxShadow: `0 2px 8px ${severityConf.color}40`,
                        transform: 'translateX(2px)',
                      },
                    }}
                  >
                    {/* Header */}
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 0.5 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        <Icon sx={{ fontSize: 16, color: severityConf.color }} />
                        <Typography variant="subtitle2" sx={{ fontWeight: 'bold', fontSize: '13px', color: '#0F3E5C' }}>
                          {alert.title}
                        </Typography>
                      </Box>
                      <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
                        {formatTimestamp(alert.timestamp)}
                      </Typography>
                    </Box>

                    {/* Message */}
                    <Typography variant="caption" sx={{ display: 'block', mb: 1, fontSize: '11px', color: '#757575' }}>
                      {alert.message}
                    </Typography>

                    {/* Footer */}
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <Chip
                        label={alert.device}
                        size="small"
                        sx={{
                          fontSize: '9px',
                          height: 18,
                          bgcolor: '#0F3E5C',
                          color: 'white',
                        }}
                      />
                      <Chip
                        label={severityConf.label}
                        size="small"
                        sx={{
                          fontSize: '9px',
                          height: 18,
                          bgcolor: severityConf.color,
                          color: 'white',
                        }}
                      />
                    </Box>
                  </Box>
                </Box>
              )
            })}
          </Box>
        )}
      </Box>

      {/* Footer Stats */}
      <Box sx={{ mt: 2, display: 'flex', gap: 1, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1, flexWrap: 'wrap' }}>
        {Object.entries({
          critical: alerts.filter((a) => a.severity === 'critical').length,
          warning: alerts.filter((a) => a.severity === 'warning').length,
          info: alerts.filter((a) => a.severity === 'info').length,
        }).map(([severity, count]) => {
          const conf = severityConfig[severity as keyof typeof severityConfig]
          return (
            <Chip
              key={severity}
              label={`${conf.label}: ${count}`}
              size="small"
              sx={{
                fontSize: '10px',
                height: 20,
                bgcolor: conf.color,
                color: 'white',
              }}
            />
          )
        })}
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'alert_timeline',
  name: 'Alert Timeline',
  description: 'Chronological timeline of alerts and events',
  type: 'alarm',
  tags: ['alerts', 'timeline', 'events', 'history', 'monitoring'],
}

registerWidget(descriptor, AlertTimeline)
export default AlertTimeline
