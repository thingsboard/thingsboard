/**
 * Activity Feed Widget - Real-time activity and collaboration feed
 * Shows user actions, system events, and team activities
 */
import { Box, Typography, Paper, Avatar, Chip } from '@mui/material'
import {
  Person,
  Settings,
  DeviceHub,
  Dashboard,
  Security,
  Update,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface Activity {
  id: string
  timestamp: number
  user: string
  action: string
  target: string
  type: 'user' | 'device' | 'dashboard' | 'system' | 'security'
  avatar?: string
}

function ActivityFeed({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo activity data
  const generateActivities = (): Activity[] => {
    const now = Date.now()
    return [
      {
        id: '1',
        timestamp: now - 180000,
        user: 'Sarah Johnson',
        action: 'updated dashboard',
        target: 'Production Floor',
        type: 'dashboard',
      },
      {
        id: '2',
        timestamp: now - 360000,
        user: 'Mike Chen',
        action: 'added device',
        target: 'Temperature Sensor #42',
        type: 'device',
      },
      {
        id: '3',
        timestamp: now - 540000,
        user: 'System',
        action: 'firmware upgrade',
        target: '15 devices updated to v2.1.0',
        type: 'system',
      },
      {
        id: '4',
        timestamp: now - 900000,
        user: 'Emily Davis',
        action: 'modified user permissions',
        target: 'Operator Group',
        type: 'security',
      },
      {
        id: '5',
        timestamp: now - 1200000,
        user: 'Alex Kumar',
        action: 'configured device',
        target: 'Gateway-007',
        type: 'device',
      },
      {
        id: '6',
        timestamp: now - 1800000,
        user: 'Lisa Martinez',
        action: 'created new dashboard',
        target: 'Energy Monitoring',
        type: 'dashboard',
      },
      {
        id: '7',
        timestamp: now - 2700000,
        user: 'System',
        action: 'backup completed',
        target: 'Database backup successful',
        type: 'system',
      },
      {
        id: '8',
        timestamp: now - 3600000,
        user: 'John Smith',
        action: 'updated settings',
        target: 'MQTT Configuration',
        type: 'user',
      },
    ]
  }

  const activities = generateActivities()

  const typeConfig = {
    user: {
      icon: Person,
      color: '#1E88E5',
      bgcolor: 'rgba(30, 136, 229, 0.1)',
    },
    device: {
      icon: DeviceHub,
      color: '#2E7D6F',
      bgcolor: 'rgba(46, 125, 111, 0.1)',
    },
    dashboard: {
      icon: Dashboard,
      color: '#FFB300',
      bgcolor: 'rgba(255, 179, 0, 0.1)',
    },
    system: {
      icon: Settings,
      color: '#757575',
      bgcolor: 'rgba(117, 117, 117, 0.1)',
    },
    security: {
      icon: Security,
      color: '#C62828',
      bgcolor: 'rgba(198, 40, 40, 0.1)',
    },
  }

  const formatTimestamp = (timestamp: number): string => {
    const diff = Date.now() - timestamp
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(minutes / 60)

    if (hours > 0) return `${hours}h ago`
    if (minutes > 0) return `${minutes}m ago`
    return 'Just now'
  }

  const getInitials = (name: string): string => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Activity Feed'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {activities.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Update sx={{ fontSize: 48, color: '#E0E0E0', mb: 1 }} />
            <Typography variant="body2" color="text.secondary">
              No recent activity
            </Typography>
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            {activities.map((activity) => {
              const config = typeConfig[activity.type]
              const TypeIcon = config.icon
              const isSystemActivity = activity.user === 'System'

              return (
                <Box
                  key={activity.id}
                  sx={{
                    display: 'flex',
                    gap: 1.5,
                    p: 1.5,
                    borderRadius: 1,
                    transition: 'all 0.2s ease',
                    '&:hover': {
                      bgcolor: config.bgcolor,
                    },
                  }}
                >
                  {/* Avatar */}
                  <Avatar
                    sx={{
                      width: 36,
                      height: 36,
                      bgcolor: isSystemActivity ? config.color : '#0F3E5C',
                      fontSize: '12px',
                      fontWeight: 'bold',
                    }}
                  >
                    {isSystemActivity ? <TypeIcon sx={{ fontSize: 20 }} /> : getInitials(activity.user)}
                  </Avatar>

                  {/* Content */}
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 1 }}>
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography variant="body2" sx={{ fontSize: '13px', lineHeight: 1.4 }}>
                          <Typography
                            component="span"
                            sx={{ fontWeight: 'bold', color: '#0F3E5C' }}
                          >
                            {activity.user}
                          </Typography>
                          {' '}
                          <Typography component="span" color="text.secondary">
                            {activity.action}
                          </Typography>
                          {' '}
                          <Typography
                            component="span"
                            sx={{ fontWeight: 'bold', color: config.color }}
                          >
                            {activity.target}
                          </Typography>
                        </Typography>

                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
                          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
                            {formatTimestamp(activity.timestamp)}
                          </Typography>
                          <Chip
                            icon={<TypeIcon sx={{ fontSize: 12 }} />}
                            label={activity.type}
                            size="small"
                            sx={{
                              height: 16,
                              fontSize: '9px',
                              textTransform: 'capitalize',
                              '& .MuiChip-icon': { ml: 0.5 },
                            }}
                          />
                        </Box>
                      </Box>
                    </Box>
                  </Box>
                </Box>
              )
            })}
          </Box>
        )}
      </Box>

      {/* Footer Stats */}
      <Box sx={{ mt: 2, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
            Last 24 hours:
          </Typography>
          <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold', color: '#0F3E5C' }}>
            {activities.length} activities
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'activity_feed',
  name: 'Activity Feed',
  description: 'Real-time feed of user actions and system events',
  type: 'static',
  tags: ['activity', 'feed', 'collaboration', 'audit', 'timeline'],
}

registerWidget(descriptor, ActivityFeed)
export default ActivityFeed
