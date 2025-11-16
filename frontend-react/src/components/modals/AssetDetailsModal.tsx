/**
 * Asset Details Modal
 * Exactly matches ThingsBoard's asset details modal
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
} from '@mui/material'
import {
  Inventory2 as AssetIcon,
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import EntityDetailsModal, { StatusBadge, InfoRow, SectionHeader } from './EntityDetailsModal'
import { format } from 'date-fns'

interface Asset {
  id: string
  name: string
  type: string
  label?: string
  customerId?: string
  assetProfileId?: string
  createdTime: number
  additionalInfo?: any
}

interface AssetDetailsModalProps {
  open: boolean
  onClose: () => void
  asset: Asset | null
  onSave?: (asset: Asset) => void
  onDelete?: (assetId: string) => void
  mode?: 'view' | 'edit' | 'create'
}

export default function AssetDetailsModal({
  open,
  onClose,
  asset: initialAsset,
  onSave,
  onDelete,
  mode: initialMode = 'view',
}: AssetDetailsModalProps) {
  const [mode, setMode] = useState<'view' | 'edit' | 'create'>(initialMode)
  const [asset, setAsset] = useState<Asset | null>(initialAsset)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setAsset(initialAsset)
    setMode(initialMode)
  }, [initialAsset, initialMode])

  const handleSave = () => {
    if (asset && onSave) {
      setLoading(true)
      onSave(asset)
      setTimeout(() => {
        setLoading(false)
        setMode('view')
      }, 500)
    }
  }

  const handleDelete = () => {
    if (asset && onDelete) {
      onDelete(asset.id)
      onClose()
    }
  }

  if (!asset) return null

  // Details Tab
  const detailsTab = (
    <Box>
      <SectionHeader title="Asset Information" />
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <TextField
            label="Name"
            required
            fullWidth
            value={asset.name || ''}
            onChange={(e) => setAsset({ ...asset, name: e.target.value })}
            disabled={mode === 'view'}
            helperText="Unique asset name"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            label="Label"
            fullWidth
            value={asset.label || ''}
            onChange={(e) => setAsset({ ...asset, label: e.target.value })}
            disabled={mode === 'view'}
            helperText="Display label for UI"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <FormControl fullWidth disabled={mode === 'view'}>
            <InputLabel>Asset Type *</InputLabel>
            <Select
              value={asset.type || 'default'}
              onChange={(e) => setAsset({ ...asset, type: e.target.value })}
              label="Asset Type *"
            >
              <MenuItem value="default">Default</MenuItem>
              <MenuItem value="building">Building</MenuItem>
              <MenuItem value="floor">Floor</MenuItem>
              <MenuItem value="room">Room</MenuItem>
              <MenuItem value="vehicle">Vehicle</MenuItem>
              <MenuItem value="equipment">Equipment</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} md={6}>
          <FormControl fullWidth disabled={mode === 'view'}>
            <InputLabel>Asset Profile</InputLabel>
            <Select
              value={asset.assetProfileId || ''}
              onChange={(e) => setAsset({ ...asset, assetProfileId: e.target.value })}
              label="Asset Profile"
            >
              <MenuItem value="">None</MenuItem>
              <MenuItem value="default">Default Asset Profile</MenuItem>
              <MenuItem value="building-profile">Building Profile</MenuItem>
              <MenuItem value="equipment-profile">Equipment Profile</MenuItem>
            </Select>
          </FormControl>
        </Grid>
      </Grid>

      {mode === 'view' && (
        <Box sx={{ mt: 4 }}>
          <SectionHeader title="System Information" />
          <Paper variant="outlined" sx={{ p: 2 }}>
            <InfoRow label="Asset ID" value={asset.id} copyable />
            <InfoRow
              label="Created"
              value={format(new Date(asset.createdTime), 'MMM dd, yyyy HH:mm:ss')}
            />
            <InfoRow
              label="Customer"
              value={asset.customerId ? 'Assigned to customer' : 'Not assigned'}
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
        Attributes are used to store static or semi-static data about the asset (e.g., address,
        capacity, dimensions).
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
              <TableCell>address</TableCell>
              <TableCell>123 Main Street, New York, NY</TableCell>
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
            <TableRow>
              <TableCell>capacity</TableCell>
              <TableCell>500 people</TableCell>
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
        Telemetry data represents time-series data from the asset's devices.
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
              <TableCell>occupancy</TableCell>
              <TableCell>
                <Chip label="342 people" size="small" sx={{ bgcolor: '#E3F2FD' }} />
              </TableCell>
              <TableCell>{format(new Date(), 'MMM dd, yyyy HH:mm:ss')}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell>energyConsumption</TableCell>
              <TableCell>
                <Chip label="87.5 kWh" size="small" sx={{ bgcolor: '#E3F2FD' }} />
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
      <SectionHeader title="Asset Alarms" />
      <Alert severity="info" sx={{ mb: 2 }}>
        Alarms are generated by rule chains based on asset telemetry and attributes.
      </Alert>
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
      <Alert severity="info" sx={{ mb: 2 }}>
        Relations define connections between this asset and other entities (Devices, Assets,
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
              <TableCell>
                <Chip label="FROM" size="small" color="primary" />
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
              <TableCell>{format(new Date(asset.createdTime), 'MMM dd, yyyy HH:mm')}</TableCell>
              <TableCell>tenant@payvar.io</TableCell>
              <TableCell>
                <Chip label="CREATED" size="small" sx={{ bgcolor: '#E8F5E9' }} />
              </TableCell>
              <TableCell>Asset created</TableCell>
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
        ]
      : []

  return (
    <EntityDetailsModal
      open={open}
      onClose={onClose}
      title={mode === 'create' ? 'Add Asset' : asset.name}
      subtitle={mode !== 'create' ? `Type: ${asset.type}` : undefined}
      icon={<AssetIcon />}
      tabs={tabs}
      onSave={mode !== 'view' ? handleSave : undefined}
      onDelete={mode === 'view' && onDelete ? handleDelete : undefined}
      additionalActions={additionalActions}
      loading={loading}
      maxWidth="lg"
    />
  )
}
