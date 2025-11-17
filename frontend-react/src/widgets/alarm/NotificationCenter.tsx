/**
 * Notification Center Widget - Centralized notification management
 * Shows alerts, updates, and system notifications
 */
import { useState } from 'react'
import { Box, Typography, Paper, IconButton, Chip, Badge, Tabs, Tab } from '@mui/material'
import {
  Notifications,
  NotificationsActive,
  Error,
  Warning,
  Info,
  CheckCircle,
  Delete,
  DoneAll,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface Notification {
  id: string
  type: 'error' | 'warning' | 'info' | 'success'
  title: string
  message: string
  timestamp: number
  read: boolean
  category: 'alerts' | 'updates' | 'system'
}

function NotificationCenter({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const [activeTab, setActiveTab] = useState(0)

  // Demo notifications
  const [notifications, setNotifications] = useState<Notification[]>([
    {
      id: '1',
      type: 'error',
      title: 'Critical Alert',
      message: 'Temperature exceeded threshold on Device-042',
      timestamp: Date.now() - 300000,
      read: false,
      category: 'alerts',
    },
    {
      id: '2',
      type: 'warning',
      title: 'Battery Low',
      message: 'Gateway-007 battery level at 15%',
      timestamp: Date.now() - 900000,
      read: false,
      category: 'alerts',
    },
    {
      id: '3',
      type: 'info',
      title: 'Firmware Update Available',
      message: 'Version 2.1.1 is now available for download',
      timestamp: Date.now() - 1800000,
      read: true,
      category: 'updates',
    },
    {
      id: '4',
      type: 'success',
      title: 'Backup Completed',
      message: 'Database backup completed successfully',
      timestamp: Date.now() - 3600000,
      read: true,
      category: 'system',
    },
    {
      id: '5',
      type: 'warning',
      title: 'High CPU Usage',
      message: 'Server CPU usage at 85%',
      timestamp: Date.now() - 7200000,
      read: false,
      category: 'system',
    },
    {
      id: '6',
      type: 'info',
      title: 'New Device Added',
      message: 'Sensor-098 successfully provisioned',
      timestamp: Date.now() - 10800000,
      read: true,
      category: 'updates',
    },
  ])

  const typeConfig = {
    error: {
      icon: Error,
      color: '#C62828',
      bgcolor: 'rgba(198, 40, 40, 0.1)',
    },
    warning: {
      icon: Warning,
      color: '#FFB300',
      bgcolor: 'rgba(255, 179, 0, 0.1)',
    },
    info: {
      icon: Info,
      color: '#1E88E5',
      bgcolor: 'rgba(30, 136, 229, 0.1)',
    },
    success: {
      icon: CheckCircle,
      color: '#2E7D6F',
      bgcolor: 'rgba(46, 125, 111, 0.1)',
    },
  }

  const categories = [
    { id: 'all', label: 'All', filter: () => true },
    { id: 'alerts', label: 'Alerts', filter: (n: Notification) => n.category === 'alerts' },
    { id: 'updates', label: 'Updates', filter: (n: Notification) => n.category === 'updates' },
    { id: 'system', label: 'System', filter: (n: Notification) => n.category === 'system' },
  ]

  const filteredNotifications = notifications.filter(categories[activeTab].filter)
  const unreadCount = notifications.filter((n) => !n.read).length

  const formatTimestamp = (timestamp: number): string => {
    const diff = Date.now() - timestamp
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(minutes / 60)

    if (hours > 0) return `${hours}h ago`
    if (minutes > 0) return `${minutes}m ago`
    return 'Just now'
  }

  const handleMarkAsRead = (id: string) => {
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)))
  }

  const handleDelete = (id: string) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id))
  }

  const handleMarkAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box
        sx={{
          p: 2,
          pb: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Badge badgeContent={unreadCount} color="error">
            <Notifications sx={{ fontSize: 20, color: '#0F3E5C' }} />
          </Badge>
          <Typography variant="h6" sx={{ fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
            {config.title || 'Notifications'}
          </Typography>
        </Box>
        <IconButton size="small" onClick={handleMarkAllAsRead} title="Mark all as read">
          <DoneAll fontSize="small" />
        </IconButton>
      </Box>

      {/* Tabs */}
      <Tabs
        value={activeTab}
        onChange={(_, v) => setActiveTab(v)}
        sx={{
          px: 2,
          minHeight: 40,
          '& .MuiTab-root': {
            minHeight: 40,
            fontSize: '12px',
          },
        }}
      >
        {categories.map((cat) => (
          <Tab
            key={cat.id}
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                {cat.label}
                {cat.id === 'all' && unreadCount > 0 && (
                  <Chip label={unreadCount} size="small" sx={{ height: 16, fontSize: '9px', bgcolor: '#C62828', color: 'white' }} />
                )}
              </Box>
            }
          />
        ))}
      </Tabs>

      {/* Notifications list */}
      <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
        {filteredNotifications.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Notifications sx={{ fontSize: 48, color: '#E0E0E0', mb: 1 }} />
            <Typography variant="body2" color="text.secondary">
              No notifications
            </Typography>
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {filteredNotifications.map((notification) => {
              const typeConf = typeConfig[notification.type]
              const TypeIcon = typeConf.icon

              return (
                <Box
                  key={notification.id}
                  sx={{
                    p: 1.5,
                    borderRadius: 1,
                    bgcolor: notification.read ? '#F5F5F5' : typeConf.bgcolor,
                    border: `1px solid ${notification.read ? '#E0E0E0' : typeConf.color}40`,
                    opacity: notification.read ? 0.7 : 1,
                    transition: 'all 0.2s ease',
                    '&:hover': {
                      boxShadow: `0 2px 8px ${typeConf.color}40`,
                    },
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
                    {/* Icon */}
                    <Box
                      sx={{
                        width: 32,
                        height: 32,
                        borderRadius: '50%',
                        bgcolor: typeConf.color,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}
                    >
                      <TypeIcon sx={{ fontSize: 18, color: 'white' }} />
                    </Box>

                    {/* Content */}
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography variant="subtitle2" sx={{ fontWeight: 'bold', fontSize: '13px', color: '#0F3E5C' }}>
                        {notification.title}
                      </Typography>
                      <Typography variant="caption" sx={{ display: 'block', fontSize: '11px', color: '#757575', mb: 0.5 }}>
                        {notification.message}
                      </Typography>
                      <Typography variant="caption" sx={{ fontSize: '10px', color: '#999' }}>
                        {formatTimestamp(notification.timestamp)}
                      </Typography>
                    </Box>

                    {/* Actions */}
                    <Box sx={{ display: 'flex', gap: 0.5 }}>
                      {!notification.read && (
                        <IconButton size="small" onClick={() => handleMarkAsRead(notification.id)} title="Mark as read">
                          <NotificationsActive sx={{ fontSize: 16 }} />
                        </IconButton>
                      )}
                      <IconButton size="small" onClick={() => handleDelete(notification.id)} title="Delete">
                        <Delete sx={{ fontSize: 16 }} />
                      </IconButton>
                    </Box>
                  </Box>
                </Box>
              )
            })}
          </Box>
        )}
      </Box>

      {/* Footer */}
      <Box sx={{ p: 1.5, borderTop: '1px solid #E0E0E0', bgcolor: '#F5F5F5' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
            Total: {notifications.length} notifications
          </Typography>
          <Box sx={{ display: 'flex', gap: 0.5 }}>
            {Object.entries({
              error: notifications.filter((n) => n.type === 'error').length,
              warning: notifications.filter((n) => n.type === 'warning').length,
            }).map(
              ([type, count]) =>
                count > 0 && (
                  <Chip
                    key={type}
                    label={`${count}`}
                    size="small"
                    sx={{
                      fontSize: '9px',
                      height: 18,
                      bgcolor: typeConfig[type as keyof typeof typeConfig].color,
                      color: 'white',
                    }}
                  />
                )
            )}
          </Box>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'notification_center',
  name: 'Notification Center',
  description: 'Centralized notification and alert management',
  type: 'alarm',
  tags: ['notifications', 'alerts', 'center', 'updates', 'messages'],
}

registerWidget(descriptor, NotificationCenter)
export default NotificationCenter
