/**
 * Device Health Dashboard
 * 120% Enhanced Feature - Real-time device health monitoring
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
} from '@mui/material'
import {
  CheckCircle,
  Warning,
  Error,
  OfflineBolt,
  Refresh,
  TrendingUp,
  TrendingDown,
  Battery20,
  Battery50,
  Battery80,
  BatteryFull,
  SignalCellularAlt,
  Memory,
  Thermostat,
} from '@mui/icons-material'
import { Device, DeviceHealth, getDeviceHealthStatus } from '../../types/device.types'

interface DeviceHealthDashboardProps {
  devices: Device[]
  onRefresh?: () => void
}

export default function DeviceHealthDashboard({ devices, onRefresh }: DeviceHealthDashboardProps) {
  const [healthData, setHealthData] = useState<Map<string, DeviceHealth>>(new Map())
  const [loading, setLoading] = useState(false)

  // Mock health data generation (in production, fetch from backend)
  useEffect(() => {
    const mockHealthData = new Map<string, DeviceHealth>()

    devices.forEach((device) => {
      const isActive = device.active !== false
      mockHealthData.set(device.id.id, {
        deviceId: device.id.id,
        status: isActive
          ? Math.random() > 0.7
            ? 'healthy'
            : Math.random() > 0.5
              ? 'warning'
              : 'critical'
          : 'offline',
        uptime: Math.floor(Math.random() * 86400000 * 30), // Up to 30 days
        messageCount: Math.floor(Math.random() * 10000),
        errorCount: Math.floor(Math.random() * 100),
        lastTelemetryTime: Date.now() - Math.floor(Math.random() * 3600000), // Last hour
        batteryLevel: device.type.includes('sensor') ? Math.floor(Math.random() * 100) : undefined,
        signalStrength: Math.floor(Math.random() * 100),
        cpuUsage: Math.floor(Math.random() * 100),
        memoryUsage: Math.floor(Math.random() * 100),
        temperature: 20 + Math.floor(Math.random() * 40),
      })
    })

    setHealthData(mockHealthData)
  }, [devices])

  const healthyCount = Array.from(healthData.values()).filter((h) => h.status === 'healthy').length
  const warningCount = Array.from(healthData.values()).filter((h) => h.status === 'warning').length
  const criticalCount = Array.from(healthData.values()).filter((h) => h.status === 'critical').length
  const offlineCount = Array.from(healthData.values()).filter((h) => h.status === 'offline').length

  const totalMessages = Array.from(healthData.values()).reduce((sum, h) => sum + h.messageCount, 0)
  const totalErrors = Array.from(healthData.values()).reduce((sum, h) => sum + h.errorCount, 0)
  const errorRate = totalMessages > 0 ? ((totalErrors / totalMessages) * 100).toFixed(2) : '0'

  const getBatteryIcon = (level: number) => {
    if (level > 80) return <BatteryFull color="success" />
    if (level > 50) return <Battery80 color="success" />
    if (level > 20) return <Battery50 color="warning" />
    return <Battery20 color="error" />
  }

  const getStatusColor = (status: string) => {
    const colors = {
      healthy: '#4CAF50',
      warning: '#FF9800',
      critical: '#F44336',
      offline: '#9E9E9E',
    }
    return colors[status as keyof typeof colors] || colors.offline
  }

  const getStatusIcon = (status: string) => {
    const icons = {
      healthy: <CheckCircle sx={{ color: '#4CAF50' }} />,
      warning: <Warning sx={{ color: '#FF9800' }} />,
      critical: <Error sx={{ color: '#F44336' }} />,
      offline: <OfflineBolt sx={{ color: '#9E9E9E' }} />,
    }
    return icons[status as keyof typeof icons] || icons.offline
  }

  const formatUptime = (ms: number) => {
    const days = Math.floor(ms / 86400000)
    const hours = Math.floor((ms % 86400000) / 3600000)
    return `${days}d ${hours}h`
  }

  const handleRefresh = () => {
    setLoading(true)
    onRefresh?.()
    setTimeout(() => setLoading(false), 1000)
  }

  return (
    <Box>
      {/* Header with Refresh */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5" fontWeight={600}>
          Device Health Dashboard
        </Typography>
        <Tooltip title="Refresh">
          <IconButton onClick={handleRefresh} disabled={loading}>
            <Refresh className={loading ? 'spin' : ''} />
          </IconButton>
        </Tooltip>
      </Box>

      {/* Summary Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Avatar sx={{ bgcolor: '#4CAF50' }}>
                  <CheckCircle />
                </Avatar>
                <Box>
                  <Typography variant="h4" fontWeight={600}>
                    {healthyCount}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Healthy
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Avatar sx={{ bgcolor: '#FF9800' }}>
                  <Warning />
                </Avatar>
                <Box>
                  <Typography variant="h4" fontWeight={600}>
                    {warningCount}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Warning
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Avatar sx={{ bgcolor: '#F44336' }}>
                  <Error />
                </Avatar>
                <Box>
                  <Typography variant="h4" fontWeight={600}>
                    {criticalCount}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Critical
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Avatar sx={{ bgcolor: '#9E9E9E' }}>
                  <OfflineBolt />
                </Avatar>
                <Box>
                  <Typography variant="h4" fontWeight={600}>
                    {offlineCount}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Offline
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Overall Statistics */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Total Messages
              </Typography>
              <Typography variant="h5" fontWeight={600}>
                {totalMessages.toLocaleString()}
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={12} md={4}>
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Error Rate
              </Typography>
              <Typography variant="h5" fontWeight={600} color={parseFloat(errorRate) > 5 ? 'error' : 'success.main'}>
                {errorRate}%
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={12} md={4}>
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Average Uptime
              </Typography>
              <Typography variant="h5" fontWeight={600}>
                {formatUptime(
                  Array.from(healthData.values()).reduce((sum, h) => sum + h.uptime, 0) / healthData.size || 0
                )}
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </Paper>

      {/* Device List */}
      <Paper>
        <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="h6">Device Details</Typography>
        </Box>
        <Box sx={{ maxHeight: 400, overflow: 'auto' }}>
          {devices.map((device) => {
            const health = healthData.get(device.id.id)
            if (!health) return null

            return (
              <Box
                key={device.id.id}
                sx={{
                  p: 2,
                  borderBottom: '1px solid',
                  borderColor: 'divider',
                  '&:hover': { bgcolor: 'action.hover' },
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    {getStatusIcon(health.status)}
                    <Box>
                      <Typography variant="body1" fontWeight={600}>
                        {device.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {device.type} • Uptime: {formatUptime(health.uptime)}
                      </Typography>
                    </Box>
                  </Box>
                  <Chip
                    label={health.status.toUpperCase()}
                    size="small"
                    sx={{
                      bgcolor: getStatusColor(health.status),
                      color: 'white',
                      fontWeight: 600,
                    }}
                  />
                </Box>

                <Grid container spacing={2}>
                  {health.batteryLevel !== undefined && (
                    <Grid item xs={6} sm={3}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        {getBatteryIcon(health.batteryLevel)}
                        <Box>
                          <Typography variant="caption" color="text.secondary">
                            Battery
                          </Typography>
                          <Typography variant="body2" fontWeight={600}>
                            {health.batteryLevel}%
                          </Typography>
                        </Box>
                      </Box>
                    </Grid>
                  )}

                  <Grid item xs={6} sm={3}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <SignalCellularAlt color={health.signalStrength > 70 ? 'success' : 'warning'} />
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Signal
                        </Typography>
                        <Typography variant="body2" fontWeight={600}>
                          {health.signalStrength}%
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>

                  <Grid item xs={6} sm={3}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Memory color={health.cpuUsage > 80 ? 'error' : 'action'} />
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          CPU
                        </Typography>
                        <Typography variant="body2" fontWeight={600}>
                          {health.cpuUsage}%
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>

                  <Grid item xs={6} sm={3}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Thermostat color={health.temperature > 50 ? 'error' : 'action'} />
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Temp
                        </Typography>
                        <Typography variant="body2" fontWeight={600}>
                          {health.temperature}°C
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>
                </Grid>
              </Box>
            )
          })}
        </Box>
      </Paper>

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
