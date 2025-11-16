/**
 * Device Details Modal
 * Exactly matches ThingsBoard's device details modal
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
import EntityDetailsModal, { StatusBadge, InfoRow, SectionHeader } from './EntityDetailsModal'
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

interface DeviceDetailsModalProps {
  open: boolean
  onClose: () => void
  device: Device | null
  onSave?: (device: Device) => void
  onDelete?: (deviceId: string) => void
  mode?: 'view' | 'edit' | 'create'
}

export default function DeviceDetailsModal({
  open,
  onClose,
  device: initialDevice,
  onSave,
  onDelete,
  mode: initialMode = 'view',
}: DeviceDetailsModalProps) {
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
        setMode('view')
      }, 500)
    }
  }

  const handleDelete = () => {
    if (device && onDelete) {
      onDelete(device.id)
      onClose()
    }
  }

  const handleCopyAccessToken = () => {
    if (device?.credentials?.credentialsId) {
      navigator.clipboard.writeText(device.credentials.credentialsId)
      alert('Access token copied to clipboard')
    }
  }

  const handleCheckConnectivity = () => {
    alert('Checking device connectivity...')
  }

  if (!device) return null

  // Details Tab
  const detailsTab = (
    <Box>
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
              <MenuItem value="thermostat">Thermostat</MenuItem>
              <MenuItem value="sensor">Sensor</MenuItem>
              <MenuItem value="gateway">Gateway</MenuItem>
              <MenuItem value="meter">Meter</MenuItem>
              <MenuItem value="actuator">Actuator</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} md={6}>
          <FormControl fullWidth disabled={mode === 'view'}>
            <InputLabel>Device Profile</InputLabel>
            <Select
              value={device.deviceProfileId || ''}
              onChange={(e) => setDevice({ ...device, deviceProfileId: e.target.value })}
              label="Device Profile"
            >
              <MenuItem value="">None</MenuItem>
              <MenuItem value="default">Default Device Profile</MenuItem>
              <MenuItem value="sensor-profile">Sensor Profile</MenuItem>
              <MenuItem value="gateway-profile">Gateway Profile</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12}>
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

      <Box sx={{ mt: 4 }}>
        <SectionHeader
          title="Device Credentials"
          action={
            mode !== 'view' && (
              <Button
                size="small"
                variant="outlined"
                startIcon={<EditIcon />}
                onClick={() => alert('Edit credentials')}
              >
                Edit Credentials
              </Button>
            )
          }
        />
        <Paper variant="outlined" sx={{ p: 2 }}>
          <InfoRow
            label="Credentials Type"
            value={device.credentials?.credentialsType || 'ACCESS_TOKEN'}
          />
          <InfoRow
            label="Access Token"
            value={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <code
                  style={{
                    padding: '4px 8px',
                    backgroundColor: '#F5F5F5',
                    borderRadius: 4,
                    fontSize: '0.875rem',
                  }}
                >
                  {device.credentials?.credentialsId || 'Not set'}
                </code>
                {device.credentials?.credentialsId && (
                  <Tooltip title="Copy access token">
                    <IconButton size="small" onClick={handleCopyAccessToken}>
                      <CopyIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                )}
              </Box>
            }
          />
        </Paper>
      </Box>

      {mode === 'view' && (
        <Box sx={{ mt: 4 }}>
          <SectionHeader title="System Information" />
          <Paper variant="outlined" sx={{ p: 2 }}>
            <InfoRow label="Device ID" value={device.id} copyable />
            <InfoRow
              label="Created"
              value={format(new Date(device.createdTime), 'MMM dd, yyyy HH:mm:ss')}
            />
            <InfoRow
              label="Customer"
              value={device.customerId ? 'Assigned to customer' : 'Not assigned'}
            />
          </Paper>
        </Box>
      )}

      {mode !== 'view' && (
        <Box sx={{ mt: 3 }}>
          <Alert severity="info">
            After creating the device, you can configure attributes, telemetry, and alarms.
          </Alert>
        </Box>
      )}
    </Box>
  )

  // Attributes Tab
  const attributesTab = (
    <Box>
      <SectionHeader
        title="Attributes"
        action={
          <Button variant="outlined" size="small" startIcon={<AddIcon />}>
            Add Attribute
          </Button>
        }
      />
      <Alert severity="info" sx={{ mb: 2 }}>
        Attributes are used to store static or semi-static data about the device (e.g., model,
        serial number, firmware version).
      </Alert>

      {/* Client-side Attributes */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
          Client-side Attributes
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Key</TableCell>
                <TableCell>Value</TableCell>
                <TableCell>Last Update</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell colSpan={4} align="center" sx={{ py: 3, color: '#757575' }}>
                  No client-side attributes
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      </Box>

      {/* Server-side Attributes */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
          Server-side Attributes
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Key</TableCell>
                <TableCell>Value</TableCell>
                <TableCell>Last Update</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell>model</TableCell>
                <TableCell>DHT22</TableCell>
                <TableCell>{format(new Date(), 'MMM dd, yyyy HH:mm')}</TableCell>
                <TableCell align="right">
                  <IconButton size="small">
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small">
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      </Box>

      {/* Shared Attributes */}
      <Box>
        <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
          Shared Attributes
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Key</TableCell>
                <TableCell>Value</TableCell>
                <TableCell>Last Update</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell colSpan={4} align="center" sx={{ py: 3, color: '#757575' }}>
                  No shared attributes
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      </Box>
    </Box>
  )

  // Latest Telemetry Tab
  const telemetryTab = (
    <Box>
      <SectionHeader
        title="Latest Telemetry"
        action={
          <Button variant="outlined" size="small" startIcon={<RefreshIcon />}>
            Refresh
          </Button>
        }
      />
      <Alert severity="info" sx={{ mb: 2 }}>
        Telemetry data represents time-series data from the device (e.g., temperature, humidity,
        pressure).
      </Alert>
      <TableContainer component={Paper} variant="outlined">
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
              <TableCell>
                <Chip label="23.5 °C" size="small" sx={{ bgcolor: '#E3F2FD' }} />
              </TableCell>
              <TableCell>{format(new Date(), 'MMM dd, yyyy HH:mm:ss')}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell>humidity</TableCell>
              <TableCell>
                <Chip label="65.2 %" size="small" sx={{ bgcolor: '#E3F2FD' }} />
              </TableCell>
              <TableCell>{format(new Date(), 'MMM dd, yyyy HH:mm:ss')}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )

  // Alarms Tab
  const alarmsTab = (
    <Box>
      <SectionHeader title="Device Alarms" />
      <Alert severity="info" sx={{ mb: 2 }}>
        Alarms are generated by rule chains based on device telemetry and attributes.
      </Alert>
      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Severity</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Created</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell colSpan={5} align="center" sx={{ py: 3, color: '#757575' }}>
                No active alarms
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )

  // Relations Tab
  const relationsTab = (
    <Box>
      <SectionHeader
        title="Relations"
        action={
          <Button variant="outlined" size="small" startIcon={<AddIcon />}>
            Add Relation
          </Button>
        }
      />
      <Alert severity="info" sx={{ mb: 2 }}>
        Relations define connections between this device and other entities (Assets, Devices,
        Dashboards, etc.).
      </Alert>
      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Direction</TableCell>
              <TableCell>Entity Type</TableCell>
              <TableCell>Entity Name</TableCell>
              <TableCell>Relation Type</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell colSpan={5} align="center" sx={{ py: 3, color: '#757575' }}>
                No relations defined
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )

  // Audit Logs Tab
  const auditLogsTab = (
    <Box>
      <SectionHeader title="Audit Logs" />
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell>User</TableCell>
              <TableCell>Action</TableCell>
              <TableCell>Details</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell>{format(new Date(device.createdTime), 'MMM dd, yyyy HH:mm')}</TableCell>
              <TableCell>tenant@payvar.io</TableCell>
              <TableCell>
                <Chip label="CREATED" size="small" sx={{ bgcolor: '#E8F5E9' }} />
              </TableCell>
              <TableCell>Device created</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
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

  const additionalActions =
    mode === 'view'
      ? [
          <Button
            key="edit"
            variant="outlined"
            onClick={() => setMode('edit')}
            sx={{ borderColor: '#0F3E5C', color: '#0F3E5C' }}
          >
            Edit
          </Button>,
          <Button
            key="check"
            variant="outlined"
            startIcon={<CheckIcon />}
            onClick={handleCheckConnectivity}
          >
            Check Connectivity
          </Button>,
          <Button
            key="copy"
            variant="outlined"
            startIcon={<CopyIcon />}
            onClick={handleCopyAccessToken}
          >
            Copy Access Token
          </Button>,
        ]
      : []

  return (
    <EntityDetailsModal
      open={open}
      onClose={onClose}
      title={mode === 'create' ? 'Add Device' : device.name}
      subtitle={mode !== 'create' ? `Type: ${device.type}` : undefined}
      icon={<DeviceIcon />}
      tabs={tabs}
      onSave={mode !== 'view' ? handleSave : undefined}
      onDelete={mode === 'view' && onDelete ? handleDelete : undefined}
      additionalActions={additionalActions}
      loading={loading}
      maxWidth="lg"
    />
  )
}
