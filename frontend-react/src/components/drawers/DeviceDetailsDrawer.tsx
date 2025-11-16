/**
 * Device Details Drawer
 * Right-side slide-in drawer matching ThingsBoard's exact pattern
 * Tabs: Details, Attributes, Latest telemetry, Alarms, Events, Relations, Audit logs
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
  Button,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  Switch,
  FormControlLabel,
  Alert,
  Typography,
} from '@mui/material'
import {
  Memory as DeviceIcon,
  ContentCopy as CopyIcon,
  Refresh as RefreshIcon,
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  CheckCircle as CheckIcon,
} from '@mui/icons-material'
import EntityDrawer, { StatusBadge, InfoRow, SectionHeader } from './EntityDrawer'
import AttributesTab from '@/components/entity/AttributesTab'
import EventsTab from '@/components/entity/EventsTab'
import RelationsTab from '@/components/entity/RelationsTab'
import { format } from 'date-fns'

interface Device {
  id: string
  name: string
  type: string
  label?: string
  active: boolean
  customerId?: string
  deviceProfileId?: string
  credentials?: {
    credentialsType: string
    credentialsId: string
  }
  createdTime: number
  additionalInfo?: any
}

interface DeviceDetailsDrawerProps {
  open: boolean
  onClose: () => void
  device: Device | null
  onSave?: (device: Device) => void
  onDelete?: (deviceId: string) => void
  mode?: 'view' | 'edit' | 'create'
}

export default function DeviceDetailsDrawer({
  open,
  onClose,
  device: initialDevice,
  onSave,
  onDelete,
  mode: initialMode = 'view',
}: DeviceDetailsDrawerProps) {
  const [mode, setMode] = useState<'view' | 'edit' | 'create'>(initialMode)
  const [device, setDevice] = useState<Device | null>(initialDevice)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setDevice(initialDevice)
    setMode(initialMode)
  }, [initialDevice, initialMode])

  const handleSave = () => {
    if (device && onSave) {
      setLoading(true)
      onSave(device)
      setTimeout(() => {
        setLoading(false)
      }, 500)
    }
  }

  const handleDelete = () => {
    if (device && onDelete) {
      onDelete(device.id)
    }
  }

  const handleCopyAccessToken = () => {
    if (device?.credentials?.credentialsId) {
      navigator.clipboard.writeText(device.credentials.credentialsId)
      alert('Access token copied to clipboard')
    }
  }

  const handleCopy = () => {
    alert('Copy device functionality')
  }

  if (!device) return null

  // Details Tab
  const detailsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <SectionHeader title="Device Information" />
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Name"
              required
              fullWidth
              value={device.name || ''}
              onChange={(e) => setDevice({ ...device, name: e.target.value })}
              disabled={mode === 'view'}
              helperText="Unique device name"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Label"
              fullWidth
              value={device.label || ''}
              onChange={(e) => setDevice({ ...device, label: e.target.value })}
              disabled={mode === 'view'}
              helperText="Display label for UI"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <FormControl fullWidth disabled={mode === 'view'}>
              <InputLabel>Device Type *</InputLabel>
              <Select
                value={device.type || 'default'}
                onChange={(e) => setDevice({ ...device, type: e.target.value })}
                label="Device Type *"
              >
                <MenuItem value="default">Default</MenuItem>
                <MenuItem value="sensor">Sensor</MenuItem>
                <MenuItem value="gateway">Gateway</MenuItem>
                <MenuItem value="controller">Controller</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={6}>
            <FormControlLabel
              control={
                <Switch
                  checked={device.active}
                  onChange={(e) => setDevice({ ...device, active: e.target.checked })}
                  disabled={mode === 'view'}
                />
              }
              label="Active"
            />
          </Grid>
        </Grid>

        {mode === 'view' && (
          <>
            <SectionHeader title="System Information" />
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <InfoRow label="Device ID" value={device.id} />
              </Grid>
              <Grid item xs={12} md={6}>
                <InfoRow label="Created Time" value={format(new Date(device.createdTime), 'MMM dd, yyyy HH:mm')} />
              </Grid>
              <Grid item xs={12} md={6}>
                <InfoRow label="Status" value={<StatusBadge active={device.active} />} />
              </Grid>
            </Grid>

            <SectionHeader title="Credentials" />
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <TextField
                label="Access Token"
                fullWidth
                value={device.credentials?.credentialsId || 'N/A'}
                disabled
                size="small"
              />
              <Tooltip title="Copy Access Token">
                <IconButton onClick={handleCopyAccessToken} color="primary">
                  <CopyIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </>
        )}
      </Paper>
    </Box>
  )

  // Attributes Tab
  const attributesTab = (
    <AttributesTab
      entityId={device.id}
      entityType="DEVICE"
      attributes={[
        { key: 'model', value: 'v1.0', lastUpdateTs: Date.now(), scope: 'SERVER_SCOPE' },
      ]}
      onRefresh={() => console.log('Refresh attributes')}
      onSave={(scope, key, value) => console.log('Save attribute:', scope, key, value)}
      onDelete={(scope, key) => console.log('Delete attribute:', scope, key)}
      readOnly={mode === 'view'}
    />
  )

  // Latest Telemetry Tab
  const telemetryTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6" sx={{ color: '#0F3E5C' }}>
            Latest Telemetry
          </Typography>
          <IconButton color="primary">
            <RefreshIcon />
          </IconButton>
        </Box>
        <Alert severity="info" sx={{ mb: 2 }}>
          Real-time telemetry updates via WebSocket will be implemented
        </Alert>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Key</TableCell>
                <TableCell>Value</TableCell>
                <TableCell>Timestamp</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell>temperature</TableCell>
                <TableCell>23.5°C</TableCell>
                <TableCell>{format(new Date(), 'MMM dd, yyyy HH:mm:ss')}</TableCell>
              </TableRow>
              <TableRow>
                <TableCell>humidity</TableCell>
                <TableCell>65%</TableCell>
                <TableCell>{format(new Date(), 'MMM dd, yyyy HH:mm:ss')}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
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
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Severity</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Start Time</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell>
                  <Chip label="Critical" color="error" size="small" />
                </TableCell>
                <TableCell>High Temperature</TableCell>
                <TableCell>
                  <Chip label="Active" color="warning" size="small" />
                </TableCell>
                <TableCell>{format(new Date(), 'MMM dd, yyyy HH:mm')}</TableCell>
                <TableCell align="right">
                  <Button size="small" startIcon={<CheckIcon />}>
                    Acknowledge
                  </Button>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  )

  // Events Tab
  const eventsTab = (
    <EventsTab
      entityId={device.id}
      entityType="DEVICE"
      events={[
        {
          id: '1',
          type: 'LIFECYCLE',
          severity: 'INFO',
          message: 'Device created',
          timestamp: device.createdTime,
        },
      ]}
      onRefresh={() => console.log('Refresh events')}
      readOnly={true}
    />
  )

  // Relations Tab
  const relationsTab = (
    <RelationsTab
      entityId={device.id}
      entityType="DEVICE"
      relations={[]}
      onRefresh={() => console.log('Refresh relations')}
      onSave={(relation) => console.log('Save relation:', relation)}
      onDelete={(relation) => console.log('Delete relation:', relation)}
      readOnly={mode === 'view'}
    />
  )

  // Audit Logs Tab
  const auditLogsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Audit Logs
        </Typography>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Action</TableCell>
                <TableCell>User</TableCell>
                <TableCell>Timestamp</TableCell>
                <TableCell>Details</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell>Created</TableCell>
                <TableCell>admin@payvar.com</TableCell>
                <TableCell>{format(new Date(device.createdTime), 'MMM dd, yyyy HH:mm')}</TableCell>
                <TableCell>Device created</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  )

  const tabs = [
    { label: 'Details', content: detailsTab },
    { label: 'Attributes', content: attributesTab, disabled: mode === 'create' },
    { label: 'Latest telemetry', content: telemetryTab, disabled: mode === 'create' },
    { label: 'Alarms', content: alarmsTab, disabled: mode === 'create' },
    { label: 'Events', content: eventsTab, disabled: mode === 'create' },
    { label: 'Relations', content: relationsTab, disabled: mode === 'create' },
    { label: 'Audit logs', content: auditLogsTab, disabled: mode === 'create' },
  ]

  return (
    <EntityDrawer
      open={open}
      onClose={onClose}
      title={mode === 'create' ? 'Add Device' : device.name}
      subtitle={mode === 'view' ? `Device Type: ${device.type}` : undefined}
      icon={<DeviceIcon />}
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
