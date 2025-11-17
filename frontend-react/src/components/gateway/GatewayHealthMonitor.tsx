/**
 * Gateway Health Monitor
 * 120% Enhanced Feature - Real-time gateway and connector monitoring
 * Beyond Angular implementation
 */

import React, { useState, useEffect } from 'react'
import {
  Box,
  Paper,
  Typography,
  Grid,
  LinearProgress,
  Chip,
  IconButton,
  Tooltip,
  Card,
  CardContent,
  Avatar,
  Divider,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
} from '@mui/material'
import {
  CheckCircle,
  Warning,
  Error,
  OfflineBolt,
  Refresh,
  Router,
  Memory,
  Speed,
  Storage,
  CloudQueue,
  Engineering,
  Bluetooth,
  Wifi,
} from '@mui/icons-material'
import {
  Gateway,
  GatewayHealth,
  Connector,
  ConnectorStatus,
  getGatewayHealthStatus,
  getConnectorStatusColor,
  formatUptime,
  formatBytes,
  connectorTypeIcons,
} from '../../types/gateway.types'

interface GatewayHealthMonitorProps {
  gateway: Gateway
  onRefresh?: () => void
}

const iconMap: Record<string, React.ElementType> = {
  CloudQueue,
  Memory,
  Engineering,
  Bluetooth,
  Wifi,
  Router,
  Speed,
  Storage,
}

export default function GatewayHealthMonitor({ gateway, onRefresh }: GatewayHealthMonitorProps) {
  const [health, setHealth] = useState<GatewayHealth | null>(null)
  const [connectors, setConnectors] = useState<Connector[]>([])
  const [loading, setLoading] = useState(false)

  // Mock health data (in production, fetch from backend)
  useEffect(() => {
    const mockHealth: GatewayHealth = {
      gatewayId: gateway.id.id,
      status: gateway.connected ? 'healthy' : 'offline',
      uptime: Math.floor(Math.random() * 86400000 * 7), // Up to 7 days
      lastHeartbeat: Date.now() - Math.floor(Math.random() * 30000), // Last 30s
      connectors: {
        total: 5,
        active: Math.floor(Math.random() * 5) + 1,
        failed: Math.floor(Math.random() * 2),
      },
      devices: {
        total: Math.floor(Math.random() * 50) + 10,
        connected: Math.floor(Math.random() * 40) + 5,
        lastHour: Math.floor(Math.random() * 10),
      },
      performance: {
        cpu: Math.floor(Math.random() * 100),
        memory: Math.floor(Math.random() * 100),
        disk: Math.floor(Math.random() * 100),
        network: {
          rx: Math.floor(Math.random() * 1000000),
          tx: Math.floor(Math.random() * 1000000),
        },
      },
      errors: {
        last24h: Math.floor(Math.random() * 50),
        lastHour: Math.floor(Math.random() * 5),
        critical: Math.floor(Math.random() * 2),
      },
    }

    setHealth(mockHealth)

    // Mock connector data
    const mockConnectors: Connector[] = [
      {
        name: 'MQTT Broker',
        type: 'mqtt' as any,
        enabled: true,
        status: ConnectorStatus.CONNECTED,
        configuration: {} as any,
        devicesCount: 15,
        messagesCount: 1234,
        errors: 2,
        lastActivity: Date.now() - 5000,
      },
      {
        name: 'Modbus Master',
        type: 'modbus' as any,
        enabled: true,
        status: ConnectorStatus.CONNECTED,
        configuration: {} as any,
        devicesCount: 8,
        messagesCount: 567,
        errors: 0,
        lastActivity: Date.now() - 10000,
      },
      {
        name: 'OPC-UA Server',
        type: 'opcua' as any,
        enabled: true,
        status: ConnectorStatus.DISCONNECTED,
        configuration: {} as any,
        devicesCount: 12,
        messagesCount: 0,
        errors: 5,
        lastActivity: Date.now() - 120000,
      },
    ]

    setConnectors(mockConnectors)
  }, [gateway])

  const handleRefresh = () => {
    setLoading(true)
    onRefresh?.()
    setTimeout(() => setLoading(false), 1000)
  }

  const status = getGatewayHealthStatus(gateway, health || undefined)
  const statusColors = {
    healthy: '#4CAF50',
    degraded: '#FF9800',
    critical: '#F44336',
    offline: '#9E9E9E',
  }

  const getStatusIcon = (status: string) => {
    const icons = {
      healthy: <CheckCircle sx={{ color: '#4CAF50' }} />,
      degraded: <Warning sx={{ color: '#FF9800' }} />,
      critical: <Error sx={{ color: '#F44336' }} />,
      offline: <OfflineBolt sx={{ color: '#9E9E9E' }} />,
    }
    return icons[status as keyof typeof icons]
  }

  const getPerformanceColor = (value: number) => {
    if (value > 90) return 'error'
    if (value > 70) return 'warning'
    return 'success'
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {getStatusIcon(status)}
          <Box>
            <Typography variant="h6" fontWeight={600}>
              {gateway.name}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Gateway Health Monitor
            </Typography>
          </Box>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Chip
            label={status.toUpperCase()}
            sx={{
              bgcolor: statusColors[status],
              color: 'white',
              fontWeight: 600,
            }}
          />
          <Tooltip title="Refresh">
            <IconButton onClick={handleRefresh} disabled={loading}>
              <Refresh className={loading ? 'spin' : ''} />
            </IconButton>
          </Tooltip>
        </Box>
      </Box>

      {health && (
        <>
          {/* Summary Cards */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Uptime
                  </Typography>
                  <Typography variant="h5" fontWeight={600}>
                    {formatUptime(health.uptime)}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Active Connectors
                  </Typography>
                  <Typography variant="h5" fontWeight={600}>
                    {health.connectors.active} / {health.connectors.total}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Connected Devices
                  </Typography>
                  <Typography variant="h5" fontWeight={600}>
                    {health.devices.connected} / {health.devices.total}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Errors (24h)
                  </Typography>
                  <Typography variant="h5" fontWeight={600} color={health.errors.last24h > 50 ? 'error' : 'inherit'}>
                    {health.errors.last24h}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Performance Metrics */}
          <Paper sx={{ p: 2, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Performance Metrics
            </Typography>
            <Grid container spacing={3}>
              <Grid item xs={12} md={4}>
                <Box sx={{ mb: 1, display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">CPU Usage</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {health.performance.cpu}%
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={health.performance.cpu}
                  color={getPerformanceColor(health.performance.cpu)}
                  sx={{ height: 8, borderRadius: 4 }}
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <Box sx={{ mb: 1, display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">Memory Usage</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {health.performance.memory}%
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={health.performance.memory}
                  color={getPerformanceColor(health.performance.memory)}
                  sx={{ height: 8, borderRadius: 4 }}
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <Box sx={{ mb: 1, display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">Disk Usage</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {health.performance.disk}%
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={health.performance.disk}
                  color={getPerformanceColor(health.performance.disk)}
                  sx={{ height: 8, borderRadius: 4 }}
                />
              </Grid>
            </Grid>

            <Divider sx={{ my: 2 }} />

            <Grid container spacing={2}>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Network RX
                </Typography>
                <Typography variant="h6">{formatBytes(health.performance.network.rx)}</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Network TX
                </Typography>
                <Typography variant="h6">{formatBytes(health.performance.network.tx)}</Typography>
              </Grid>
            </Grid>
          </Paper>

          {/* Connectors Status */}
          <Paper>
            <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
              <Typography variant="h6">Connectors Status</Typography>
            </Box>
            <List>
              {connectors.map((connector, index) => {
                const IconComponent = iconMap[connectorTypeIcons[connector.type]] || Router
                return (
                  <ListItem
                    key={index}
                    sx={{
                      borderBottom: index < connectors.length - 1 ? '1px solid' : 'none',
                      borderColor: 'divider',
                    }}
                  >
                    <ListItemIcon>
                      <Avatar sx={{ bgcolor: getConnectorStatusColor(connector.status) }}>
                        <IconComponent />
                      </Avatar>
                    </ListItemIcon>
                    <ListItemText
                      primary={connector.name}
                      secondary={
                        <Box sx={{ display: 'flex', gap: 2, mt: 0.5 }}>
                          <Typography variant="caption">
                            Devices: {connector.devicesCount}
                          </Typography>
                          <Typography variant="caption">
                            Messages: {connector.messagesCount?.toLocaleString()}
                          </Typography>
                          {connector.errors && connector.errors > 0 && (
                            <Typography variant="caption" color="error">
                              Errors: {connector.errors}
                            </Typography>
                          )}
                        </Box>
                      }
                    />
                    <Chip
                      label={connector.status}
                      size="small"
                      sx={{
                        bgcolor: getConnectorStatusColor(connector.status),
                        color: 'white',
                      }}
                    />
                  </ListItem>
                )
              })}
            </List>
          </Paper>
        </>
      )}

      <style>{`
        .spin {
          animation: spin 1s linear infinite;
        }
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </Box>
  )
}
