/**
 * Customer Details Modal
 * Exactly matches ThingsBoard's customer details modal
 * Tabs: Details, Users, Assets, Devices, Dashboards, Audit logs
 */

import { useState, useEffect } from 'react'
import {
  Box,
  TextField,
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
  Typography,
  Alert,
} from '@mui/material'
import {
  Business as CustomerIcon,
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Email as EmailIcon,
  Phone as PhoneIcon,
} from '@mui/icons-material'
import EntityDetailsModal, { InfoRow, SectionHeader } from './EntityDetailsModal'
import { format } from 'date-fns'

interface Customer {
  id: string
  title: string
  email?: string
  phone?: string
  address?: string
  address2?: string
  city?: string
  state?: string
  zip?: string
  country?: string
  createdTime: number
  additionalInfo?: any
}

interface CustomerDetailsModalProps {
  open: boolean
  onClose: () => void
  customer: Customer | null
  onSave?: (customer: Customer) => void
  onDelete?: (customerId: string) => void
  mode?: 'view' | 'edit' | 'create'
}

export default function CustomerDetailsModal({
  open,
  onClose,
  customer: initialCustomer,
  onSave,
  onDelete,
  mode: initialMode = 'view',
}: CustomerDetailsModalProps) {
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
        setMode('view')
      }, 500)
    }
  }

  const handleDelete = () => {
    if (customer && onDelete) {
      onDelete(customer.id)
      onClose()
    }
  }

  if (!customer) return null

  // Details Tab
  const detailsTab = (
    <Box>
      <SectionHeader title="Customer Information" />
      <Grid container spacing={3}>
        <Grid item xs={12}>
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
            type="email"
            fullWidth
            value={customer.email || ''}
            onChange={(e) => setCustomer({ ...customer, email: e.target.value })}
            disabled={mode === 'view'}
            InputProps={{
              startAdornment: <EmailIcon sx={{ mr: 1, color: '#757575' }} />,
            }}
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            label="Phone"
            fullWidth
            value={customer.phone || ''}
            onChange={(e) => setCustomer({ ...customer, phone: e.target.value })}
            disabled={mode === 'view'}
            InputProps={{
              startAdornment: <PhoneIcon sx={{ mr: 1, color: '#757575' }} />,
            }}
          />
        </Grid>
      </Grid>

      <Box sx={{ mt: 4 }}>
        <SectionHeader title="Address" />
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <TextField
              label="Address"
              fullWidth
              value={customer.address || ''}
              onChange={(e) => setCustomer({ ...customer, address: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12}>
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
              label="City"
              fullWidth
              value={customer.city || ''}
              onChange={(e) => setCustomer({ ...customer, city: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="State / Province"
              fullWidth
              value={customer.state || ''}
              onChange={(e) => setCustomer({ ...customer, state: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="ZIP / Postal Code"
              fullWidth
              value={customer.zip || ''}
              onChange={(e) => setCustomer({ ...customer, zip: e.target.value })}
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
        </Grid>
      </Box>

      {mode === 'view' && (
        <Box sx={{ mt: 4 }}>
          <SectionHeader title="System Information" />
          <Paper variant="outlined" sx={{ p: 2 }}>
            <InfoRow label="Customer ID" value={customer.id} copyable />
            <InfoRow
              label="Created"
              value={format(new Date(customer.createdTime), 'MMM dd, yyyy HH:mm:ss')}
            />
          </Paper>
        </Box>
      )}
    </Box>
  )

  // Users Tab
  const usersTab = (
    <Box>
      <SectionHeader
        title="Customer Users"
        action={
          <Button variant="outlined" size="small" startIcon={<AddIcon />}>
            Add User
          </Button>
        }
      />
      <Alert severity="info" sx={{ mb: 2 }}>
        Manage users associated with this customer organization.
      </Alert>
      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Last Login</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell>John Doe</TableCell>
              <TableCell>john.doe@example.com</TableCell>
              <TableCell>
                <Chip label="Active" color="success" size="small" />
              </TableCell>
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

  // Assets Tab
  const assetsTab = (
    <Box>
      <SectionHeader
        title="Customer Assets"
        action={
          <Button variant="outlined" size="small" startIcon={<AddIcon />}>
            Assign Asset
          </Button>
        }
      />
      <Alert severity="info" sx={{ mb: 2 }}>
        Assets assigned to this customer.
      </Alert>
      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Label</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell colSpan={4} align="center" sx={{ py: 3, color: '#757575' }}>
                No assets assigned
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )

  // Devices Tab
  const devicesTab = (
    <Box>
      <SectionHeader
        title="Customer Devices"
        action={
          <Button variant="outlined" size="small" startIcon={<AddIcon />}>
            Assign Device
          </Button>
        }
      />
      <Alert severity="info" sx={{ mb: 2 }}>
        Devices assigned to this customer.
      </Alert>
      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell colSpan={4} align="center" sx={{ py: 3, color: '#757575' }}>
                No devices assigned
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )

  // Dashboards Tab
  const dashboardsTab = (
    <Box>
      <SectionHeader
        title="Customer Dashboards"
        action={
          <Button variant="outlined" size="small" startIcon={<AddIcon />}>
            Assign Dashboard
          </Button>
        }
      />
      <Alert severity="info" sx={{ mb: 2 }}>
        Dashboards shared with this customer.
      </Alert>
      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Title</TableCell>
              <TableCell>Description</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell colSpan={3} align="center" sx={{ py: 3, color: '#757575' }}>
                No dashboards assigned
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
              <TableCell>{format(new Date(customer.createdTime), 'MMM dd, yyyy HH:mm')}</TableCell>
              <TableCell>admin@payvar.io</TableCell>
              <TableCell>
                <Chip label="CREATED" size="small" sx={{ bgcolor: '#E8F5E9' }} />
              </TableCell>
              <TableCell>Customer created</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
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
      title={mode === 'create' ? 'Add Customer' : customer.title}
      subtitle={mode !== 'create' ? `Customer` : undefined}
      icon={<CustomerIcon />}
      tabs={tabs}
      onSave={mode !== 'view' ? handleSave : undefined}
      onDelete={mode === 'view' && onDelete ? handleDelete : undefined}
      additionalActions={additionalActions}
      loading={loading}
      maxWidth="lg"
    />
  )
}
