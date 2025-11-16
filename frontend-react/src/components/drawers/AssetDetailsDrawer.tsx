/**
 * Asset Details Drawer
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
  Button,
  Paper,
  Typography,
  Alert,
} from '@mui/material'
import {
  Inventory2 as AssetIcon,
} from '@mui/icons-material'
import EntityDrawer, { SectionHeader } from './EntityDrawer'
import AttributesTab from '@/components/entity/AttributesTab'
import EventsTab from '@/components/entity/EventsTab'
import RelationsTab from '@/components/entity/RelationsTab'
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

interface AssetDetailsDrawerProps {
  open: boolean
  onClose: () => void
  asset: Asset | null
  onSave?: (asset: Asset) => void
  onDelete?: (assetId: string) => void
  mode?: 'view' | 'edit' | 'create'
}

export default function AssetDetailsDrawer({
  open,
  onClose,
  asset: initialAsset,
  onSave,
  onDelete,
  mode: initialMode = 'view',
}: AssetDetailsDrawerProps) {
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
      }, 500)
    }
  }

  const handleDelete = () => {
    if (asset && onDelete) {
      onDelete(asset.id)
    }
  }

  const handleCopy = () => {
    alert('Copy asset functionality')
  }

  if (!asset) return null

  // Details Tab
  const detailsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
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
                <MenuItem value="vehicle">Vehicle</MenuItem>
                <MenuItem value="equipment">Equipment</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          {mode === 'view' && (
            <Grid item xs={12}>
              <Typography variant="caption" color="text.secondary">
                Created: {format(new Date(asset.createdTime), 'MMM dd, yyyy HH:mm')}
              </Typography>
            </Grid>
          )}
        </Grid>
      </Paper>
    </Box>
  )

  // Attributes Tab
  const attributesTab = (
    <AttributesTab
      entityId={asset.id}
      entityType="ASSET"
      attributes={[]}
      onRefresh={() => console.log('Refresh attributes')}
      onSave={(scope, key, value) => console.log('Save attribute:', scope, key, value)}
      onDelete={(scope, key) => console.log('Delete attribute:', scope, key)}
      readOnly={mode === 'view'}
    />
  )

  // Other tabs
  const telemetryTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Latest Telemetry
        </Typography>
        <Alert severity="info">
          Real-time telemetry via WebSocket will be implemented
        </Alert>
      </Paper>
    </Box>
  )

  const alarmsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Alarms
        </Typography>
        <Alert severity="info">
          Alarms management will be implemented
        </Alert>
      </Paper>
    </Box>
  )

  const eventsTab = (
    <EventsTab
      entityId={asset.id}
      entityType="ASSET"
      events={[
        {
          id: '1',
          type: 'LIFECYCLE',
          severity: 'INFO',
          message: 'Asset created',
          timestamp: asset.createdTime,
        },
      ]}
      onRefresh={() => console.log('Refresh events')}
      readOnly={true}
    />
  )

  const relationsTab = (
    <RelationsTab
      entityId={asset.id}
      entityType="ASSET"
      relations={[]}
      onRefresh={() => console.log('Refresh relations')}
      onSave={(relation) => console.log('Save relation:', relation)}
      onDelete={(relation) => console.log('Delete relation:', relation)}
      readOnly={mode === 'view'}
    />
  )

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
    { label: 'Events', content: eventsTab, disabled: mode === 'create' },
    { label: 'Relations', content: relationsTab, disabled: mode === 'create' },
    { label: 'Audit logs', content: auditLogsTab, disabled: mode === 'create' },
  ]

  return (
    <EntityDrawer
      open={open}
      onClose={onClose}
      title={mode === 'create' ? 'Add Asset' : asset.name}
      subtitle={mode === 'view' ? `Asset Type: ${asset.type}` : undefined}
      icon={<AssetIcon />}
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
