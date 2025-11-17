/**
 * Gateway Logs Viewer
 * 120% Enhanced Feature - Real-time log streaming with filtering
 * Beyond Angular implementation
 */

import React, { useState, useEffect, useRef } from 'react'
import {
  Box,
  Paper,
  Typography,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  Chip,
  Tooltip,
  ToggleButtonGroup,
  ToggleButton,
  InputAdornment,
  Divider,
} from '@mui/material'
import {
  Search,
  FilterList,
  Refresh,
  Pause,
  PlayArrow,
  Download,
  Delete,
  BugReport,
  Info,
  Warning,
  Error as ErrorIcon,
} from '@mui/icons-material'
import {
  GatewayLog,
  LogLevel,
  GatewayLogsFilter,
  logLevelColors,
} from '../../types/gateway.types'
import { format } from 'date-fns'

interface GatewayLogsViewerProps {
  gatewayId: string
  connectors?: string[]
}

export default function GatewayLogsViewer({ gatewayId, connectors = [] }: GatewayLogsViewerProps) {
  const [logs, setLogs] = useState<GatewayLog[]>([])
  const [filteredLogs, setFilteredLogs] = useState<GatewayLog[]>([])
  const [filter, setFilter] = useState<GatewayLogsFilter>({})
  const [searchQuery, setSearchQuery] = useState('')
  const [isPaused, setIsPaused] = useState(false)
  const [autoScroll, setAutoScroll] = useState(true)
  const logsEndRef = useRef<HTMLDivElement>(null)

  // Mock log generation (in production, use WebSocket)
  useEffect(() => {
    if (isPaused) return

    const logMessages = [
      'Connected to MQTT broker at mqtt://localhost:1883',
      'Device "Sensor-001" published telemetry data',
      'Modbus slave response timeout on address 10',
      'OPC-UA subscription created for node "Temperature"',
      'BLE device "Beacon-A1" discovered',
      'REST API request processed successfully',
      'Configuration updated from platform',
      'Network connection interrupted, retrying...',
      'Device authentication successful',
      'Data transformation applied to incoming message',
    ]

    const levels = [LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR]
    const connectorNames = connectors.length > 0 ? connectors : ['MQTT', 'Modbus', 'OPC-UA', 'BLE', 'REST']

    const interval = setInterval(() => {
      const newLog: GatewayLog = {
        timestamp: Date.now(),
        level: levels[Math.floor(Math.random() * levels.length)],
        connector: Math.random() > 0.3 ? connectorNames[Math.floor(Math.random() * connectorNames.length)] : undefined,
        message: logMessages[Math.floor(Math.random() * logMessages.length)],
        details: Math.random() > 0.7 ? 'Additional diagnostic information available' : undefined,
      }

      setLogs((prev) => [...prev.slice(-199), newLog]) // Keep last 200 logs
    }, 2000) // New log every 2 seconds

    return () => clearInterval(interval)
  }, [isPaused, connectors])

  // Apply filters
  useEffect(() => {
    let filtered = [...logs]

    // Filter by level
    if (filter.level && filter.level.length > 0) {
      filtered = filtered.filter((log) => filter.level!.includes(log.level))
    }

    // Filter by connector
    if (filter.connector && filter.connector.length > 0) {
      filtered = filtered.filter((log) => log.connector && filter.connector!.includes(log.connector))
    }

    // Filter by search query
    if (searchQuery) {
      const query = searchQuery.toLowerCase()
      filtered = filtered.filter(
        (log) =>
          log.message.toLowerCase().includes(query) ||
          (log.connector && log.connector.toLowerCase().includes(query)) ||
          (log.details && log.details.toLowerCase().includes(query))
      )
    }

    setFilteredLogs(filtered)
  }, [logs, filter, searchQuery])

  // Auto-scroll to bottom
  useEffect(() => {
    if (autoScroll && !isPaused) {
      logsEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [filteredLogs, autoScroll, isPaused])

  const handleLevelFilterChange = (event: React.ChangeEvent<HTMLInputElement>, levels: LogLevel[]) => {
    setFilter((prev) => ({ ...prev, level: levels.length > 0 ? levels : undefined }))
  }

  const handleClearLogs = () => {
    setLogs([])
    setFilteredLogs([])
  }

  const handleDownloadLogs = () => {
    const logsText = filteredLogs
      .map(
        (log) =>
          `[${format(log.timestamp, 'yyyy-MM-dd HH:mm:ss')}] [${log.level}] ${log.connector ? `[${log.connector}] ` : ''}${log.message}`
      )
      .join('\n')

    const blob = new Blob([logsText], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `gateway-${gatewayId}-logs-${Date.now()}.txt`
    link.click()
    URL.revokeObjectURL(url)
  }

  const getLevelIcon = (level: LogLevel) => {
    const icons = {
      [LogLevel.DEBUG]: <BugReport fontSize="small" />,
      [LogLevel.INFO]: <Info fontSize="small" />,
      [LogLevel.WARNING]: <Warning fontSize="small" />,
      [LogLevel.ERROR]: <ErrorIcon fontSize="small" />,
      [LogLevel.CRITICAL]: <ErrorIcon fontSize="small" />,
    }
    return icons[level]
  }

  return (
    <Box>
      {/* Toolbar */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <TextField
            size="small"
            placeholder="Search logs..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            sx={{ minWidth: 250 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search fontSize="small" />
                </InputAdornment>
              ),
            }}
          />

          <ToggleButtonGroup
            size="small"
            value={filter.level || []}
            onChange={handleLevelFilterChange as any}
            aria-label="log level filter"
          >
            <ToggleButton value={LogLevel.DEBUG} aria-label="debug">
              <Chip
                label="DEBUG"
                size="small"
                sx={{
                  bgcolor: filter.level?.includes(LogLevel.DEBUG) ? logLevelColors[LogLevel.DEBUG] : 'transparent',
                  color: filter.level?.includes(LogLevel.DEBUG) ? 'white' : 'inherit',
                  fontSize: '0.7rem',
                }}
              />
            </ToggleButton>
            <ToggleButton value={LogLevel.INFO} aria-label="info">
              <Chip
                label="INFO"
                size="small"
                sx={{
                  bgcolor: filter.level?.includes(LogLevel.INFO) ? logLevelColors[LogLevel.INFO] : 'transparent',
                  color: filter.level?.includes(LogLevel.INFO) ? 'white' : 'inherit',
                  fontSize: '0.7rem',
                }}
              />
            </ToggleButton>
            <ToggleButton value={LogLevel.WARNING} aria-label="warning">
              <Chip
                label="WARN"
                size="small"
                sx={{
                  bgcolor: filter.level?.includes(LogLevel.WARNING) ? logLevelColors[LogLevel.WARNING] : 'transparent',
                  color: filter.level?.includes(LogLevel.WARNING) ? 'white' : 'inherit',
                  fontSize: '0.7rem',
                }}
              />
            </ToggleButton>
            <ToggleButton value={LogLevel.ERROR} aria-label="error">
              <Chip
                label="ERROR"
                size="small"
                sx={{
                  bgcolor: filter.level?.includes(LogLevel.ERROR) ? logLevelColors[LogLevel.ERROR] : 'transparent',
                  color: filter.level?.includes(LogLevel.ERROR) ? 'white' : 'inherit',
                  fontSize: '0.7rem',
                }}
              />
            </ToggleButton>
          </ToggleButtonGroup>

          <Box sx={{ ml: 'auto', display: 'flex', gap: 1 }}>
            <Tooltip title={isPaused ? 'Resume' : 'Pause'}>
              <IconButton onClick={() => setIsPaused(!isPaused)} size="small">
                {isPaused ? <PlayArrow /> : <Pause />}
              </IconButton>
            </Tooltip>
            <Tooltip title="Download Logs">
              <IconButton onClick={handleDownloadLogs} size="small">
                <Download />
              </IconButton>
            </Tooltip>
            <Tooltip title="Clear Logs">
              <IconButton onClick={handleClearLogs} size="small" color="error">
                <Delete />
              </IconButton>
            </Tooltip>
          </Box>
        </Box>
      </Paper>

      {/* Logs Display */}
      <Paper
        sx={{
          height: 600,
          overflow: 'auto',
          bgcolor: '#1E1E1E',
          p: 2,
          fontFamily: 'monospace',
          fontSize: '0.875rem',
        }}
      >
        {filteredLogs.length === 0 ? (
          <Box
            sx={{
              height: '100%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#9E9E9E',
            }}
          >
            <Typography>
              {logs.length === 0 ? 'Waiting for logs...' : 'No logs match the current filters'}
            </Typography>
          </Box>
        ) : (
          filteredLogs.map((log, index) => (
            <Box
              key={index}
              sx={{
                mb: 1,
                pb: 1,
                borderBottom: index < filteredLogs.length - 1 ? '1px solid #333' : 'none',
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                <Typography
                  variant="caption"
                  sx={{
                    color: '#9E9E9E',
                    minWidth: 160,
                  }}
                >
                  {format(log.timestamp, 'yyyy-MM-dd HH:mm:ss')}
                </Typography>
                <Chip
                  icon={getLevelIcon(log.level)}
                  label={log.level}
                  size="small"
                  sx={{
                    bgcolor: logLevelColors[log.level],
                    color: 'white',
                    fontWeight: 600,
                    fontSize: '0.65rem',
                    height: 20,
                  }}
                />
                {log.connector && (
                  <Chip
                    label={log.connector}
                    size="small"
                    variant="outlined"
                    sx={{
                      color: '#64B5F6',
                      borderColor: '#64B5F6',
                      fontSize: '0.65rem',
                      height: 20,
                    }}
                  />
                )}
              </Box>
              <Typography
                sx={{
                  color: '#D4D4D4',
                  pl: 2,
                }}
              >
                {log.message}
              </Typography>
              {log.details && (
                <Typography
                  variant="caption"
                  sx={{
                    color: '#9E9E9E',
                    pl: 2,
                    display: 'block',
                    mt: 0.5,
                  }}
                >
                  ↳ {log.details}
                </Typography>
              )}
            </Box>
          ))
        )}
        <div ref={logsEndRef} />
      </Paper>

      {/* Status Bar */}
      <Box
        sx={{
          mt: 1,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <Typography variant="caption" color="text.secondary">
          Showing {filteredLogs.length} of {logs.length} logs
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          {isPaused && (
            <Chip label="PAUSED" size="small" color="warning" sx={{ fontSize: '0.7rem' }} />
          )}
          <Typography variant="caption" color="text.secondary">
            Auto-scroll: {autoScroll ? 'ON' : 'OFF'}
          </Typography>
        </Box>
      </Box>
    </Box>
  )
}
