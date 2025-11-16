/**
 * Gateway Details Modal
 * Exactly matches ThingsBoard's gateway details modal
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
  Switch,
  FormControlLabel,
  Alert,
  Typography,
} from '@mui/material'
import {
  Router as GatewayIcon,
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
  ContentCopy as CopyIcon,
} from '@mui/icons-material'
import EntityDetailsModal, { StatusBadge, InfoRow, SectionHeader } from './EntityDetailsModal'
import { format } from 'date-fns'

interface Gateway {
  id: string
  name: string
  type: string
  label?: string
  active: boolean
  customerId?: string
  gatewayConfiguration?: any
  credentials?: {
    credentialsType: string
    credentialsId: string
  }
  createdTime: number
  additionalInfo?: any
}

interface GatewayDetailsModalProps {
  open: boolean
  onClose: () => void
  gateway: Gateway | null
  onSave?: (gateway: Gateway) => void
  onDelete?: (gatewayId: string) => void
  mode?: 'view' | 'edit' | 'create'
}

export default function GatewayDetailsModal({
  open,
  onClose,
  gateway: initialGateway,
  onSave,
  onDelete,
  mode: initialMode = 'view',
}: GatewayDetailsModalProps) {
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
        setMode('view')
      }, 500)
    }
  }

  const handleDelete = () => {
    if (gateway && onDelete) {
      onDelete(gateway.id)
      onClose()
    }
  }

  const handleCopyToken = () => {
    if (gateway?.credentials?.credentialsId) {
      navigator.clipboard.writeText(gateway.credentials.credentialsId)
      alert('Access token copied to clipboard')
    }
  }

  if (!gateway) return null

  // Details Tab
  const detailsTab = (
    <Box>
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
              <MenuItem value="bacnet">BACnet Gateway</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} md={6}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Typography>Status:</Typography>
            <StatusBadge active={gateway.active} />
          </Box>
        </Grid>
        <Grid item xs={12}>
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
      </Grid>

      <Box sx={{ mt: 4 }}>
        <SectionHeader
          title="Gateway Credentials"
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
            value={gateway.credentials?.credentialsType || 'ACCESS_TOKEN'}
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
                  {gateway.credentials?.credentialsId || 'Not set'}
                </code>
                {gateway.credentials?.credentialsId && (
                  <IconButton size="small" onClick={handleCopyToken}>
                    <CopyIcon fontSize="small" />
                  </IconButton>
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
            <InfoRow label="Gateway ID" value={gateway.id} copyable />
            <InfoRow
              label="Created"
              value={format(new Date(gateway.createdTime), 'MMM dd, yyyy HH:mm:ss')}
            />
            <InfoRow
              label="Customer"
              value={gateway.customerId ? 'Assigned to customer' : 'Not assigned'}
            />
          </Paper>
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
        Gateway attributes store configuration and meta-information.
      </Alert>
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
              <TableCell>connectedDevices</TableCell>
              <TableCell>15</TableCell>
              <TableCell>{format(new Date(), 'MMM dd, yyyy HH:mm')}</TableCell>
              <TableCell align="right">
                <IconButton size="small">
                  <EditIcon fontSize="small" />
                </IconButton>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )

  // Telemetry Tab
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
        Gateway telemetry includes status metrics and connected device information.
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
              <TableCell>cpuUsage</TableCell>
              <TableCell>
                <Chip label="42.5 %" size="small" sx={{ bgcolor: '#E3F2FD' }} />
              </TableCell>
              <TableCell>{format(new Date(), 'MMM dd, yyyy HH:mm:ss')}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell>memoryUsage</TableCell>
              <TableCell>
                <Chip label="68.2 %" size="small" sx={{ bgcolor: '#E3F2FD' }} />
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
      <SectionHeader title="Gateway Alarms" />
      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Severity</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Created</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell colSpan={4} align="center" sx={{ py: 3, color: '#757575' }}>
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
              <TableCell>
                <Chip label="TO" size="small" color="secondary" />
              </TableCell>
              <TableCell>Device</TableCell>
              <TableCell>Temp Sensor 01</TableCell>
              <TableCell>Contains</TableCell>
              <TableCell align="right">
                <IconButton size="small">
                  <DeleteIcon fontSize="small" />
                </IconButton>
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
              <TableCell>{format(new Date(gateway.createdTime), 'MMM dd, yyyy HH:mm')}</TableCell>
              <TableCell>tenant@payvar.io</TableCell>
              <TableCell>
                <Chip label="CREATED" size="small" sx={{ bgcolor: '#E8F5E9' }} />
              </TableCell>
              <TableCell>Gateway created</TableCell>
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
            key="copy"
            variant="outlined"
            startIcon={<CopyIcon />}
            onClick={handleCopyToken}
          >
            Copy Access Token
          </Button>,
        ]
      : []

  return (
    <EntityDetailsModal
      open={open}
      onClose={onClose}
      title={mode === 'create' ? 'Add Gateway' : gateway.name}
      subtitle={mode !== 'create' ? `Type: ${gateway.type}` : undefined}
      icon={<GatewayIcon />}
      tabs={tabs}
      onSave={mode !== 'view' ? handleSave : undefined}
      onDelete={mode === 'view' && onDelete ? handleDelete : undefined}
      additionalActions={additionalActions}
      loading={loading}
      maxWidth="lg"
    />
  )
}
