/**
 * Tenants Page
 * Tenant management (SYS_ADMIN only)
 * Matches ThingsBoard ui-ngx tenants functionality
 */

import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Chip,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  FormControlLabel,
  Checkbox,
  Typography,
  Alert,
} from '@mui/material'
import {
  Visibility,
  Block,
  CheckCircle,
  Business,
  People,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'
import EntityTable, { EntityColumn } from '@/components/entity/EntityTable'
import { useAppSelector } from '@/hooks/redux'
import { selectCurrentUser } from '@/store/auth/authSlice'
import TenantDetailsDrawer from '@/components/drawers/TenantDetailsDrawer'

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
]

export default function TenantsPage() {
  const navigate = useNavigate()
  const currentUser = useAppSelector(selectCurrentUser)
  const [tenants, setTenants] = useState<Tenant[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')

  // Dialog states
  const [openDialog, setOpenDialog] = useState(false)
  const [editingTenant, setEditingTenant] = useState<Tenant | null>(null)
  const [formData, setFormData] = useState({
    title: '',
    name: '',
    email: '',
    phone: '',
    address: '',
    address2: '',
    city: '',
    state: '',
    zip: '',
    country: 'United States',
    region: '',
    tenantProfileId: '',
    enabled: true,
  })

  // Drawer states
  const [openDrawer, setOpenDrawer] = useState(false)
  const [selectedTenant, setSelectedTenant] = useState<Tenant | null>(null)
  const [drawerMode, setDrawerMode] = useState<'view' | 'edit' | 'create'>('view')

  // Check if user is SYS_ADMIN
  const isSysAdmin = currentUser?.authority === 'SYS_ADMIN'

  useEffect(() => {
    if (isSysAdmin) {
      loadTenants()
    }
  }, [page, pageSize, searchQuery, isSysAdmin])

  const loadTenants = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockTenants: Tenant[] = [
      {
        id: '1',
        title: 'System Tenant',
        name: 'System Administrator',
        email: 'admin@payvar.io',
        phone: '+1-555-0100',
        tenantProfileName: 'Default',
        enabled: true,
        createdTime: Date.now() - 86400000 * 365,
        country: 'United States',
        city: 'San Francisco',
        state: 'CA',
      },
      {
        id: '2',
        title: 'ABC Manufacturing Inc',
        name: 'John Smith',
        email: 'admin@abcmfg.com',
        phone: '+1-555-0200',
        tenantProfileName: 'Enterprise',
        enabled: true,
        createdTime: Date.now() - 86400000 * 180,
        country: 'United States',
        city: 'Detroit',
        state: 'MI',
      },
      {
        id: '3',
        title: 'XYZ Logistics',
        name: 'Sarah Johnson',
        email: 'admin@xyzlogistics.com',
        phone: '+1-555-0300',
        tenantProfileName: 'Professional',
        enabled: true,
        createdTime: Date.now() - 86400000 * 120,
        country: 'United States',
        city: 'Chicago',
        state: 'IL',
      },
      {
        id: '4',
        title: 'Tech Innovations Ltd',
        name: 'Michael Brown',
        email: 'admin@techinnovations.com',
        phone: '+44-20-1234-5678',
        tenantProfileName: 'Enterprise',
        enabled: true,
        createdTime: Date.now() - 86400000 * 90,
        country: 'United Kingdom',
        city: 'London',
      },
      {
        id: '5',
        title: 'Disabled Tenant Co',
        name: 'David Wilson',
        email: 'admin@disabledtenant.com',
        phone: '+1-555-0500',
        tenantProfileName: 'Starter',
        enabled: false,
        createdTime: Date.now() - 86400000 * 60,
        country: 'United States',
        city: 'Boston',
        state: 'MA',
      },
    ]

    // Filter by search query
    const filtered = searchQuery
      ? mockTenants.filter(
          (t) =>
            t.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
            t.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            t.email?.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : mockTenants

    setTenants(filtered.slice(page * pageSize, (page + 1) * pageSize))
    setTotalElements(filtered.length)
    setLoading(false)
  }

  const columns: EntityColumn[] = [
    {
      id: 'title',
      label: 'Tenant',
      minWidth: 200,
      format: (value, row: Tenant) => (
        <Box>
          <Box
            sx={{ fontWeight: 600, color: '#0F3E5C', cursor: 'pointer' }}
            onClick={() => navigate(`/tenants/${row.id}`)}
          >
            <Business sx={{ fontSize: '1rem', mr: 0.5, verticalAlign: 'middle' }} />
            {value}
          </Box>
          {row.name && (
            <Box sx={{ fontSize: '0.75rem', color: '#8C959D', mt: 0.5 }}>
              Admin: {row.name}
            </Box>
          )}
        </Box>
      ),
    },
    {
      id: 'email',
      label: 'Contact',
      minWidth: 200,
      format: (value, row: Tenant) => (
        <Box>
          <Box sx={{ fontSize: '0.875rem' }}>{value}</Box>
          {row.phone && (
            <Box sx={{ fontSize: '0.75rem', color: '#8C959D', mt: 0.5 }}>
              {row.phone}
            </Box>
          )}
        </Box>
      ),
    },
    {
      id: 'city',
      label: 'Location',
      minWidth: 150,
      format: (value, row: Tenant) => {
        const location = [value, row.state, row.country].filter(Boolean).join(', ')
        return location || '-'
      },
    },
    {
      id: 'tenantProfileName',
      label: 'Tenant Profile',
      minWidth: 150,
      format: (value) => (
        <Chip
          label={value || 'Default'}
          size="small"
          sx={{
            bgcolor: '#E0F2F1',
            color: '#2E7D6F',
            fontWeight: 500,
          }}
        />
      ),
    },
    {
      id: 'enabled',
      label: 'Status',
      minWidth: 100,
      format: (value: boolean) =>
        value ? (
          <Chip
            icon={<CheckCircle />}
            label="Active"
            size="small"
            color="success"
            sx={{ fontWeight: 500 }}
          />
        ) : (
          <Chip
            icon={<Block />}
            label="Disabled"
            size="small"
            sx={{ bgcolor: '#8C959D', color: 'white', fontWeight: 500 }}
          />
        ),
    },
    {
      id: 'createdTime',
      label: 'Created',
      minWidth: 150,
      format: (value: number) => format(new Date(value), 'MMM dd, yyyy HH:mm'),
    },
  ]

  const handleAdd = () => {
    setSelectedTenant({
      id: '',
      title: '',
      email: '',
      phone: '',
      address: '',
      city: '',
      state: '',
      zip: '',
      country: 'United States',
      tenantProfileName: 'Default',
      enabled: true,
      createdTime: Date.now(),
    })
    setDrawerMode('create')
    setOpenDrawer(true)
  }

  const handleEdit = (tenant: Tenant) => {
    setSelectedTenant(tenant)
    setDrawerMode('edit')
    setOpenDrawer(true)
  }

  const handleViewDetails = (tenant: Tenant) => {
    setSelectedTenant(tenant)
    setDrawerMode('view')
    setOpenDrawer(true)
  }

  const handleSaveTenant = (tenant: Tenant) => {
    // API call would go here
    console.log('Saving tenant:', tenant)
    setOpenDrawer(false)
    loadTenants()
  }

  const handleDeleteTenant = (tenantId: string) => {
    // API call would go here
    console.log('Deleting tenant:', tenantId)
    setOpenDrawer(false)
    loadTenants()
  }

  const handleActivateTenant = (tenantId: string, enabled: boolean) => {
    // API call would go here
    console.log('Activating/Deactivating tenant:', tenantId, enabled)
    loadTenants()
  }

  const handleDelete = async (ids: string[]) => {
    if (confirm(`Delete ${ids.length} tenant(s)? This will permanently delete all associated data.`)) {
      // API call would go here
      console.log('Deleting tenants:', ids)
      loadTenants()
      setSelectedIds([])
    }
  }

  const handleSave = async () => {
    // API call would go here
    console.log('Saving tenant:', formData)
    setOpenDialog(false)
    loadTenants()
  }

  const handleToggleStatus = async (tenant: Tenant) => {
    // API call would go here
    console.log('Toggling tenant status:', tenant.id)
    loadTenants()
  }

  const handleViewUsers = (tenant: Tenant) => {
    navigate(`/tenants/${tenant.id}/users`)
  }

  const rowActions = (row: Tenant) => (
    <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
      <Tooltip title="View details">
        <IconButton size="small" onClick={() => handleViewDetails(row)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="View users">
        <IconButton size="small" onClick={() => handleViewUsers(row)}>
          <People fontSize="small" />
        </IconButton>
      </Tooltip>
      {!row.enabled ? (
        <Tooltip title="Enable tenant">
          <IconButton size="small" onClick={() => handleActivateTenant(row.id, true)}>
            <CheckCircle fontSize="small" />
          </IconButton>
        </Tooltip>
      ) : (
        <Tooltip title="Disable tenant">
          <IconButton size="small" onClick={() => handleActivateTenant(row.id, false)}>
            <Block fontSize="small" />
          </IconButton>
        </Tooltip>
      )}
    </Box>
  )

  if (!isSysAdmin) {
    return (
      <MainLayout>
        <Box sx={{ p: 3 }}>
          <Alert severity="error">
            Access Denied. Only System Administrators can manage tenants.
          </Alert>
        </Box>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <Box>
        <EntityTable
          title="Tenants"
          columns={columns}
          rows={tenants}
          totalElements={totalElements}
          loading={loading}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onRefresh={loadTenants}
          onSearch={setSearchQuery}
          onPageChange={(newPage, newPageSize) => {
            setPage(newPage)
            setPageSize(newPageSize)
          }}
          page={page}
          pageSize={pageSize}
          rowActions={rowActions}
        />

        {/* Add/Edit Dialog */}
        <Dialog
          open={openDialog}
          onClose={() => setOpenDialog(false)}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>{editingTenant ? 'Edit Tenant' : 'Add Tenant'}</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
                Tenant Information
              </Typography>
              <TextField
                label="Tenant Title"
                required
                fullWidth
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                helperText="Display name for the tenant"
              />

              <FormControl fullWidth>
                <InputLabel>Tenant Profile</InputLabel>
                <Select
                  value={formData.tenantProfileId}
                  label="Tenant Profile"
                  onChange={(e) =>
                    setFormData({ ...formData, tenantProfileId: e.target.value })
                  }
                >
                  {TENANT_PROFILES.map((profile) => (
                    <MenuItem key={profile} value={profile}>
                      {profile}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#0F3E5C', mt: 1 }}>
                Admin Contact
              </Typography>
              <TextField
                label="Admin Name"
                fullWidth
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />

              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                <TextField
                  label="Email"
                  type="email"
                  required
                  fullWidth
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
                <TextField
                  label="Phone"
                  fullWidth
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                />
              </Box>

              <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#0F3E5C', mt: 1 }}>
                Address
              </Typography>
              <TextField
                label="Address Line 1"
                fullWidth
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              />
              <TextField
                label="Address Line 2"
                fullWidth
                value={formData.address2}
                onChange={(e) => setFormData({ ...formData, address2: e.target.value })}
              />

              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 2 }}>
                <TextField
                  label="City"
                  fullWidth
                  value={formData.city}
                  onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                />
                <TextField
                  label="State/Province"
                  fullWidth
                  value={formData.state}
                  onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                />
                <TextField
                  label="ZIP/Postal Code"
                  fullWidth
                  value={formData.zip}
                  onChange={(e) => setFormData({ ...formData, zip: e.target.value })}
                />
              </Box>

              <FormControl fullWidth>
                <InputLabel>Country</InputLabel>
                <Select
                  value={formData.country}
                  label="Country"
                  onChange={(e) => setFormData({ ...formData, country: e.target.value })}
                >
                  {COUNTRIES.map((country) => (
                    <MenuItem key={country} value={country}>
                      {country}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.enabled}
                    onChange={(e) =>
                      setFormData({ ...formData, enabled: e.target.checked })
                    }
                  />
                }
                label="Tenant is active"
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={!formData.title || !formData.email}
              sx={{ bgcolor: '#0F3E5C' }}
            >
              {editingTenant ? 'Save' : 'Add'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Tenant Details Drawer - Right-side slide-in matching ThingsBoard */}
        <TenantDetailsDrawer
          open={openDrawer}
          onClose={() => setOpenDrawer(false)}
          tenant={selectedTenant}
          onSave={handleSaveTenant}
          onDelete={handleDeleteTenant}
          onActivate={handleActivateTenant}
          mode={drawerMode}
        />
      </Box>
    </MainLayout>
  )
}
