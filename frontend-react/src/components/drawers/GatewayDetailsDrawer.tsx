/**
 * Gateway Details Drawer
 * Right-side slide-in drawer matching ThingsBoard's exact pattern
 * Tabs: Details, Attributes, Latest telemetry, Alarms, Relations, Audit logs
 */

import { useState, useEffect } from 'react'
import {
  Box,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Grid,
  Paper,
  Typography,
  Alert,
  Switch,
  FormControlLabel,
} from '@mui/material'
import {
  Router as GatewayIcon,
} from '@mui/icons-material'
import EntityDrawer, { SectionHeader, StatusBadge } from './EntityDrawer'
import { format } from 'date-fns'

interface Gateway {
  id: string
  name: string
  type: string
  label?: string
  active: boolean
  accessToken?: string
  customerId?: string
  createdTime: number
  additionalInfo?: any
}

interface GatewayDetailsDrawerProps {
  open: boolean
  onClose: () => void
  gateway: Gateway | null
  onSave?: (gateway: Gateway) => void
  onDelete?: (gatewayId: string) => void
  mode?: 'view' | 'edit' | 'create'
}

export default function GatewayDetailsDrawer({
  open,
  onClose,
  gateway: initialGateway,
  onSave,
  onDelete,
  mode: initialMode = 'view',
}: GatewayDetailsDrawerProps) {
  const [mode, setMode] = useState<'view' | 'edit' | 'create'>(initialMode)
  const [gateway, setGateway] = useState<Gateway | null>(initialGateway)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setGateway(initialGateway)
    setMode(initialMode)
  }, [initialGateway, initialMode])

  const handleSave = () => {
    if (gateway && onSave) {
      setLoading(true)
      onSave(gateway)
      setTimeout(() => {
        setLoading(false)
      }, 500)
    }
  }

  const handleDelete = () => {
    if (gateway && onDelete) {
      onDelete(gateway.id)
    }
  }

  const handleCopy = () => {
    alert('Copy gateway functionality')
  }

  if (!gateway) return null

  // Details Tab
  const detailsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <SectionHeader title="Gateway Information" />
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Name"
              required
              fullWidth
              value={gateway.name || ''}
              onChange={(e) => setGateway({ ...gateway, name: e.target.value })}
              disabled={mode === 'view'}
              helperText="Unique gateway name"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Label"
              fullWidth
              value={gateway.label || ''}
              onChange={(e) => setGateway({ ...gateway, label: e.target.value })}
              disabled={mode === 'view'}
              helperText="Display label for UI"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <FormControl fullWidth disabled={mode === 'view'}>
              <InputLabel>Gateway Type *</InputLabel>
              <Select
                value={gateway.type || 'default'}
                onChange={(e) => setGateway({ ...gateway, type: e.target.value })}
                label="Gateway Type *"
              >
                <MenuItem value="default">Default</MenuItem>
                <MenuItem value="mqtt">MQTT Gateway</MenuItem>
                <MenuItem value="modbus">Modbus Gateway</MenuItem>
                <MenuItem value="opcua">OPC-UA Gateway</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={6}>
            <FormControlLabel
              control={
                <Switch
                  checked={gateway.active}
                  onChange={(e) => setGateway({ ...gateway, active: e.target.checked })}
                  disabled={mode === 'view'}
                />
              }
              label="Active"
            />
          </Grid>
          {mode === 'view' && (
            <>
              <Grid item xs={12}>
                <SectionHeader title="System Information" />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Gateway ID
                </Typography>
                <Typography variant="body1">{gateway.id}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Status
                </Typography>
                <StatusBadge active={gateway.active} />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Created
                </Typography>
                <Typography variant="body1">
                  {format(new Date(gateway.createdTime), 'MMM dd, yyyy HH:mm')}
                </Typography>
              </Grid>
              {gateway.accessToken && (
                <Grid item xs={12}>
                  <SectionHeader title="Access Token" />
                  <TextField
                    fullWidth
                    value={gateway.accessToken}
                    disabled
                    size="small"
                    helperText="Use this token for gateway authentication"
                  />
                </Grid>
              )}
            </>
          )}
        </Grid>
      </Paper>
    </Box>
  )

  // Attributes Tab
  const attributesTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Attributes
        </Typography>
        <Alert severity="info">
          Gateway attributes management will be implemented
        </Alert>
      </Paper>
    </Box>
  )

  // Telemetry Tab
  const telemetryTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Latest Telemetry
        </Typography>
        <Alert severity="info">
          Real-time gateway telemetry (CPU, Memory, Connected Devices) via WebSocket will be implemented
        </Alert>
      </Paper>
    </Box>
  )

  // Alarms Tab
  const alarmsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Alarms
        </Typography>
        <Alert severity="info">
          Gateway alarms will be displayed here
        </Alert>
      </Paper>
    </Box>
  )

  // Relations Tab
  const relationsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Relations
        </Typography>
        <Alert severity="info">
          Gateway relations (connected devices) will be displayed here
        </Alert>
      </Paper>
    </Box>
  )

  // Audit Logs Tab
  const auditLogsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Audit Logs
        </Typography>
        <Alert severity="info">
          Audit logs will be implemented
        </Alert>
      </Paper>
    </Box>
  )

  const tabs = [
    { label: 'Details', content: detailsTab },
    { label: 'Attributes', content: attributesTab, disabled: mode === 'create' },
    { label: 'Latest telemetry', content: telemetryTab, disabled: mode === 'create' },
    { label: 'Alarms', content: alarmsTab, disabled: mode === 'create' },
    { label: 'Relations', content: relationsTab, disabled: mode === 'create' },
    { label: 'Audit logs', content: auditLogsTab, disabled: mode === 'create' },
  ]

  return (
    <EntityDrawer
      open={open}
      onClose={onClose}
      title={mode === 'create' ? 'Add Gateway' : gateway.name}
      subtitle={mode === 'view' ? `Gateway Type: ${gateway.type}` : undefined}
      icon={<GatewayIcon />}
      tabs={tabs}
      mode={mode}
      onModeChange={setMode}
      onSave={handleSave}
      onDelete={handleDelete}
      onCopy={handleCopy}
      loading={loading}
      width="70%"
    />
  )
}
