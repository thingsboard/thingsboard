/**
 * Tenant Details Drawer
 * Right-side slide-in drawer matching ThingsBoard's exact pattern
 * Tabs: Details, Users, Assets, Devices, Dashboards, Tenant Profile, Audit logs
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
  FormControlLabel,
  Checkbox,
  Divider,
  Button,
} from '@mui/material'
import {
  Business as TenantIcon,
  Block as BlockIcon,
  CheckCircle as ActiveIcon,
} from '@mui/icons-material'
import EntityDrawer, { SectionHeader, StatusBadge } from './EntityDrawer'
import { format } from 'date-fns'

interface Tenant {
  id: string
  title: string
  name?: string
  email?: string
  phone?: string
  address?: string
  address2?: string
  city?: string
  state?: string
  zip?: string
  country?: string
  region?: string
  tenantProfileId?: string
  tenantProfileName?: string
  enabled?: boolean
  createdTime: number
  additionalInfo?: any
}

interface TenantDetailsDrawerProps {
  open: boolean
  onClose: () => void
  tenant: Tenant | null
  onSave?: (tenant: Tenant) => void
  onDelete?: (tenantId: string) => void
  onActivate?: (tenantId: string, enabled: boolean) => void
  mode?: 'view' | 'edit' | 'create'
}

const TENANT_PROFILES = [
  'Default',
  'Enterprise',
  'Starter',
  'Professional',
]

const COUNTRIES = [
  'United States',
  'Canada',
  'United Kingdom',
  'Germany',
  'France',
  'Japan',
  'Australia',
  'China',
  'India',
  'Brazil',
]

export default function TenantDetailsDrawer({
  open,
  onClose,
  tenant: initialTenant,
  onSave,
  onDelete,
  onActivate,
  mode: initialMode = 'view',
}: TenantDetailsDrawerProps) {
  const [mode, setMode] = useState<'view' | 'edit' | 'create'>(initialMode)
  const [tenant, setTenant] = useState<Tenant | null>(initialTenant)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setTenant(initialTenant)
    setMode(initialMode)
  }, [initialTenant, initialMode])

  const handleSave = () => {
    if (tenant && onSave) {
      setLoading(true)
      onSave(tenant)
      setTimeout(() => {
        setLoading(false)
      }, 500)
    }
  }

  const handleDelete = () => {
    if (tenant && onDelete) {
      onDelete(tenant.id)
    }
  }

  const handleActivate = () => {
    if (tenant && onActivate) {
      onActivate(tenant.id, !tenant.enabled)
      setTenant({ ...tenant, enabled: !tenant.enabled })
    }
  }

  const handleCopy = () => {
    alert('Copy tenant functionality')
  }

  if (!tenant) return null

  // Details Tab
  const detailsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <SectionHeader title="Tenant Information" />
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Title"
              required
              fullWidth
              value={tenant.title || ''}
              onChange={(e) => setTenant({ ...tenant, title: e.target.value })}
              disabled={mode === 'view'}
              helperText="Tenant organization name"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <FormControl fullWidth disabled={mode === 'view'}>
              <InputLabel>Tenant Profile</InputLabel>
              <Select
                value={tenant.tenantProfileName || 'Default'}
                onChange={(e) => setTenant({ ...tenant, tenantProfileName: e.target.value })}
                label="Tenant Profile"
              >
                {TENANT_PROFILES.map((profile) => (
                  <MenuItem key={profile} value={profile}>
                    {profile}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle1" sx={{ mb: 2, color: '#0F3E5C', fontWeight: 600 }}>
              Contact Information
            </Typography>
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Email"
              fullWidth
              type="email"
              value={tenant.email || ''}
              onChange={(e) => setTenant({ ...tenant, email: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Phone"
              fullWidth
              value={tenant.phone || ''}
              onChange={(e) => setTenant({ ...tenant, phone: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>

          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle1" sx={{ mb: 2, color: '#0F3E5C', fontWeight: 600 }}>
              Address
            </Typography>
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              label="Address"
              fullWidth
              value={tenant.address || ''}
              onChange={(e) => setTenant({ ...tenant, address: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Address 2"
              fullWidth
              value={tenant.address2 || ''}
              onChange={(e) => setTenant({ ...tenant, address2: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="City"
              fullWidth
              value={tenant.city || ''}
              onChange={(e) => setTenant({ ...tenant, city: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="State/Province"
              fullWidth
              value={tenant.state || ''}
              onChange={(e) => setTenant({ ...tenant, state: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Zip/Postal Code"
              fullWidth
              value={tenant.zip || ''}
              onChange={(e) => setTenant({ ...tenant, zip: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <FormControl fullWidth disabled={mode === 'view'}>
              <InputLabel>Country</InputLabel>
              <Select
                value={tenant.country || ''}
                onChange={(e) => setTenant({ ...tenant, country: e.target.value })}
                label="Country"
              >
                {COUNTRIES.map((country) => (
                  <MenuItem key={country} value={country}>
                    {country}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={tenant.enabled ?? true}
                  onChange={(e) => setTenant({ ...tenant, enabled: e.target.checked })}
                  disabled={mode === 'view'}
                />
              }
              label="Enabled (tenant can access system)"
            />
          </Grid>

          {mode === 'view' && (
            <>
              <Grid item xs={12}>
                <Divider sx={{ my: 2 }} />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Tenant ID
                </Typography>
                <Typography variant="body1">{tenant.id}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Status
                </Typography>
                <StatusBadge active={tenant.enabled ?? true} />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Created
                </Typography>
                <Typography variant="body1">
                  {format(new Date(tenant.createdTime), 'MMM dd, yyyy HH:mm')}
                </Typography>
              </Grid>
            </>
          )}
        </Grid>

        {/* Activation actions for view mode */}
        {mode === 'view' && (
          <Box sx={{ mt: 3 }}>
            <Button
              variant="outlined"
              color={tenant.enabled ? 'error' : 'success'}
              onClick={handleActivate}
              startIcon={tenant.enabled ? <BlockIcon /> : <ActiveIcon />}
            >
              {tenant.enabled ? 'Disable Tenant' : 'Enable Tenant'}
            </Button>
          </Box>
        )}
      </Paper>
    </Box>
  )

  // Users Tab
  const usersTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Tenant Users
        </Typography>
        <Alert severity="info">
          Tenant administrator and user management will be implemented
        </Alert>
      </Paper>
    </Box>
  )

  // Assets Tab
  const assetsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Tenant Assets
        </Typography>
        <Alert severity="info">
          Assets owned by this tenant will be displayed here
        </Alert>
      </Paper>
    </Box>
  )

  // Devices Tab
  const devicesTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Tenant Devices
        </Typography>
        <Alert severity="info">
          Devices owned by this tenant will be displayed here
        </Alert>
      </Paper>
    </Box>
  )

  // Dashboards Tab
  const dashboardsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Tenant Dashboards
        </Typography>
        <Alert severity="info">
          Dashboards created by this tenant will be displayed here
        </Alert>
      </Paper>
    </Box>
  )

  // Tenant Profile Tab
  const tenantProfileTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Tenant Profile Configuration
        </Typography>
        <Alert severity="info" sx={{ mb: 3 }}>
          Resource quotas and rate limits will be displayed here:
          <ul>
            <li>Maximum devices, assets, customers, users</li>
            <li>Maximum dashboards and rule chains</li>
            <li>API rate limits</li>
            <li>Data retention policies</li>
          </ul>
        </Alert>
        <Typography variant="subtitle1">
          Current Profile: <strong>{tenant.tenantProfileName || 'Default'}</strong>
        </Typography>
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
          Tenant activity audit logs will be implemented
        </Alert>
      </Paper>
    </Box>
  )

  const tabs = [
    { label: 'Details', content: detailsTab },
    { label: 'Users', content: usersTab, disabled: mode === 'create' },
    { label: 'Assets', content: assetsTab, disabled: mode === 'create' },
    { label: 'Devices', content: devicesTab, disabled: mode === 'create' },
    { label: 'Dashboards', content: dashboardsTab, disabled: mode === 'create' },
    { label: 'Tenant Profile', content: tenantProfileTab, disabled: mode === 'create' },
    { label: 'Audit logs', content: auditLogsTab, disabled: mode === 'create' },
  ]

  return (
    <EntityDrawer
      open={open}
      onClose={onClose}
      title={mode === 'create' ? 'Add Tenant' : tenant.title}
      subtitle={mode === 'view' ? tenant.tenantProfileName : undefined}
      icon={<TenantIcon />}
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
