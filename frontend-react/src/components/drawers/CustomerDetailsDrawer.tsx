/**
 * Customer Details Drawer
 * Right-side slide-in drawer matching ThingsBoard's exact pattern
 * Tabs: Details, Users, Assets, Devices, Dashboards, Audit logs
 */

import { useState, useEffect } from 'react'
import {
  Box,
  TextField,
  Grid,
  Paper,
  Typography,
  Alert,
} from '@mui/material'
import {
  Business as CustomerIcon,
} from '@mui/icons-material'
import EntityDrawer, { SectionHeader } from './EntityDrawer'
import { format } from 'date-fns'

interface Customer {
  id: string
  title: string
  email?: string
  phone?: string
  country?: string
  city?: string
  state?: string
  address?: string
  address2?: string
  zip?: string
  isPublic?: boolean
  createdTime: number
  additionalInfo?: any
}

interface CustomerDetailsDrawerProps {
  open: boolean
  onClose: () => void
  customer: Customer | null
  onSave?: (customer: Customer) => void
  onDelete?: (customerId: string) => void
  mode?: 'view' | 'edit' | 'create'
}

export default function CustomerDetailsDrawer({
  open,
  onClose,
  customer: initialCustomer,
  onSave,
  onDelete,
  mode: initialMode = 'view',
}: CustomerDetailsDrawerProps) {
  const [mode, setMode] = useState<'view' | 'edit' | 'create'>(initialMode)
  const [customer, setCustomer] = useState<Customer | null>(initialCustomer)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setCustomer(initialCustomer)
    setMode(initialMode)
  }, [initialCustomer, initialMode])

  const handleSave = () => {
    if (customer && onSave) {
      setLoading(true)
      onSave(customer)
      setTimeout(() => {
        setLoading(false)
      }, 500)
    }
  }

  const handleDelete = () => {
    if (customer && onDelete) {
      onDelete(customer.id)
    }
  }

  const handleCopy = () => {
    alert('Copy customer functionality')
  }

  if (!customer) return null

  // Details Tab
  const detailsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <SectionHeader title="Customer Information" />
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Title"
              required
              fullWidth
              value={customer.title || ''}
              onChange={(e) => setCustomer({ ...customer, title: e.target.value })}
              disabled={mode === 'view'}
              helperText="Customer organization name"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Email"
              fullWidth
              type="email"
              value={customer.email || ''}
              onChange={(e) => setCustomer({ ...customer, email: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Phone"
              fullWidth
              value={customer.phone || ''}
              onChange={(e) => setCustomer({ ...customer, phone: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Country"
              fullWidth
              value={customer.country || ''}
              onChange={(e) => setCustomer({ ...customer, country: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="City"
              fullWidth
              value={customer.city || ''}
              onChange={(e) => setCustomer({ ...customer, city: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="State/Province"
              fullWidth
              value={customer.state || ''}
              onChange={(e) => setCustomer({ ...customer, state: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Address"
              fullWidth
              value={customer.address || ''}
              onChange={(e) => setCustomer({ ...customer, address: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Address 2"
              fullWidth
              value={customer.address2 || ''}
              onChange={(e) => setCustomer({ ...customer, address2: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Zip/Postal Code"
              fullWidth
              value={customer.zip || ''}
              onChange={(e) => setCustomer({ ...customer, zip: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          {mode === 'view' && (
            <Grid item xs={12}>
              <Typography variant="caption" color="text.secondary">
                Created: {format(new Date(customer.createdTime), 'MMM dd, yyyy HH:mm')}
              </Typography>
            </Grid>
          )}
        </Grid>
      </Paper>
    </Box>
  )

  // Other tabs
  const usersTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Customer Users
        </Typography>
        <Alert severity="info">
          Customer users management will be implemented
        </Alert>
      </Paper>
    </Box>
  )

  const assetsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Customer Assets
        </Typography>
        <Alert severity="info">
          Assets assigned to this customer will appear here
        </Alert>
      </Paper>
    </Box>
  )

  const devicesTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Customer Devices
        </Typography>
        <Alert severity="info">
          Devices assigned to this customer will appear here
        </Alert>
      </Paper>
    </Box>
  )

  const dashboardsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Customer Dashboards
        </Typography>
        <Alert severity="info">
          Dashboards assigned to this customer will appear here
        </Alert>
      </Paper>
    </Box>
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
    { label: 'Users', content: usersTab, disabled: mode === 'create' },
    { label: 'Assets', content: assetsTab, disabled: mode === 'create' },
    { label: 'Devices', content: devicesTab, disabled: mode === 'create' },
    { label: 'Dashboards', content: dashboardsTab, disabled: mode === 'create' },
    { label: 'Audit logs', content: auditLogsTab, disabled: mode === 'create' },
  ]

  return (
    <EntityDrawer
      open={open}
      onClose={onClose}
      title={mode === 'create' ? 'Add Customer' : customer.title}
      subtitle={mode === 'view' ? customer.email : undefined}
      icon={<CustomerIcon />}
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
