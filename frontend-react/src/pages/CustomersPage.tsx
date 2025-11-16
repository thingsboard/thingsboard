/**
 * Customers Page
 * Customer management with CRUD operations and hierarchy
 * Matches ThingsBoard ui-ngx customers functionality
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
  FormControlLabel,
  Checkbox,
} from '@mui/material'
import {
  Visibility,
  People,
  Link as LinkIcon,
  Public,
  Business,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'
import EntityTable, { EntityColumn } from '@/components/entity/EntityTable'
import CustomerDetailsDrawer from '@/components/drawers/CustomerDetailsDrawer'

interface Customer {
  id: string
  title: string
  email?: string
  phone?: string
  country?: string
  city?: string
  address?: string
  address2?: string
  zip?: string
  isPublic?: boolean
  tenantId?: string
  createdTime: number
  additionalInfo?: any
}

export default function CustomersPage() {
  const navigate = useNavigate()
  const [customers, setCustomers] = useState<Customer[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')

  // Dialog states
  const [openDialog, setOpenDialog] = useState(false)
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null)
  const [formData, setFormData] = useState({
    title: '',
    email: '',
    phone: '',
    country: '',
    city: '',
    address: '',
    address2: '',
    zip: '',
    isPublic: false,
  })

  // Drawer states
  const [openDrawer, setOpenDrawer] = useState(false)
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null)
  const [drawerMode, setDrawerMode] = useState<'view' | 'edit' | 'create'>('view')

  // Load customers (mock data for now)
  useEffect(() => {
    loadCustomers()
  }, [page, pageSize, searchQuery])

  const loadCustomers = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockCustomers: Customer[] = [
      {
        id: '1',
        title: 'ABC Manufacturing Inc',
        email: 'contact@abcmfg.com',
        phone: '+1-555-0100',
        country: 'USA',
        city: 'New York',
        address: '123 Industrial Blvd',
        isPublic: false,
        createdTime: Date.now() - 86400000 * 90,
      },
      {
        id: '2',
        title: 'XYZ Logistics',
        email: 'info@xyzlogistics.com',
        phone: '+1-555-0200',
        country: 'USA',
        city: 'Chicago',
        address: '456 Warehouse St',
        isPublic: true,
        createdTime: Date.now() - 86400000 * 75,
      },
      {
        id: '3',
        title: 'Global Energy Solutions',
        email: 'contact@globalenergy.com',
        phone: '+44-20-5550-300',
        country: 'UK',
        city: 'London',
        address: '789 Power Lane',
        isPublic: false,
        createdTime: Date.now() - 86400000 * 60,
      },
      {
        id: '4',
        title: 'Smart Building Systems',
        email: 'info@smartbuildings.com',
        phone: '+1-555-0400',
        country: 'USA',
        city: 'San Francisco',
        address: '321 Tech Drive',
        isPublic: false,
        createdTime: Date.now() - 86400000 * 45,
      },
      {
        id: '5',
        title: 'Industrial Automation Corp',
        email: 'sales@indauto.com',
        phone: '+49-30-5550-500',
        country: 'Germany',
        city: 'Berlin',
        address: '654 Automation Strasse',
        isPublic: true,
        createdTime: Date.now() - 86400000 * 30,
      },
    ]

    // Filter by search query
    const filtered = searchQuery
      ? mockCustomers.filter(
          (c) =>
            c.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
            c.email?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            c.city?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            c.country?.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : mockCustomers

    setCustomers(filtered.slice(page * pageSize, (page + 1) * pageSize))
    setTotalElements(filtered.length)
    setLoading(false)
  }

  const columns: EntityColumn[] = [
    {
      id: 'title',
      label: 'Title',
      minWidth: 200,
      format: (value, row: Customer) => (
        <Box>
          <Box
            sx={{
              fontWeight: 600,
              color: '#0F3E5C',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 1,
            }}
            onClick={() => navigate(`/customers/${row.id}`)}
          >
            {row.isPublic ? (
              <Public sx={{ fontSize: '1rem', color: '#FFB300' }} />
            ) : (
              <Business sx={{ fontSize: '1rem', color: '#8C959D' }} />
            )}
            {value}
          </Box>
        </Box>
      ),
    },
    {
      id: 'email',
      label: 'Email',
      minWidth: 180,
      format: (value) => value || '-',
    },
    {
      id: 'phone',
      label: 'Phone',
      minWidth: 130,
      format: (value) => value || '-',
    },
    {
      id: 'city',
      label: 'City',
      minWidth: 120,
      format: (value, row: Customer) => (
        <Box>
          {value}
          {row.country && (
            <Box sx={{ fontSize: '0.75rem', color: '#8C959D' }}>
              {row.country}
            </Box>
          )}
        </Box>
      ),
    },
    {
      id: 'isPublic',
      label: 'Public',
      minWidth: 100,
      align: 'center',
      format: (value: boolean) =>
        value ? (
          <Chip
            icon={<Public />}
            label="Public"
            size="small"
            sx={{
              bgcolor: '#FFF3E0',
              color: '#E65100',
              fontWeight: 500,
            }}
          />
        ) : (
          <Chip
            label="Private"
            size="small"
            variant="outlined"
            sx={{ fontWeight: 500 }}
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
    setSelectedCustomer({
      id: '',
      title: '',
      email: '',
      phone: '',
      country: '',
      city: '',
      address: '',
      address2: '',
      zip: '',
      isPublic: false,
      createdTime: Date.now(),
    })
    setDrawerMode('create')
    setOpenDrawer(true)
  }

  const handleEdit = (customer: Customer) => {
    setSelectedCustomer(customer)
    setDrawerMode('edit')
    setOpenDrawer(true)
  }

  const handleDelete = async (ids: string[]) => {
    if (confirm(`Delete ${ids.length} customer(s)?`)) {
      // API call would go here
      console.log('Deleting customers:', ids)
      loadCustomers()
      setSelectedIds([])
    }
  }

  const handleSave = async () => {
    // API call would go here
    console.log('Saving customer:', formData)
    setOpenDialog(false)
    loadCustomers()
  }

  const handleViewDetails = (customer: Customer) => {
    setSelectedCustomer(customer)
    setDrawerMode('view')
    setOpenDrawer(true)
  }

  const handleSaveCustomer = (customer: Customer) => {
    // API call would go here
    console.log('Saving customer:', customer)
    setOpenDrawer(false)
    loadCustomers()
  }

  const handleDeleteCustomer = (customerId: string) => {
    // API call would go here
    console.log('Deleting customer:', customerId)
    setOpenDrawer(false)
    loadCustomers()
  }

  const handleManageUsers = (customer: Customer) => {
    navigate(`/customers/${customer.id}/users`)
  }

  const rowActions = (row: Customer) => (
    <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
      <Tooltip title="View details">
        <IconButton size="small" onClick={() => handleViewDetails(row)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Manage users">
        <IconButton size="small" onClick={() => handleManageUsers(row)}>
          <People fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Manage relations">
        <IconButton size="small">
          <LinkIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    </Box>
  )

  return (
    <MainLayout>
      <Box>
        <EntityTable
          title="Customers"
          columns={columns}
          rows={customers}
          totalElements={totalElements}
          loading={loading}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onRefresh={loadCustomers}
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
          <DialogTitle>
            {editingCustomer ? 'Edit Customer' : 'Add Customer'}
          </DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Title"
                required
                fullWidth
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              />

              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                <TextField
                  label="Email"
                  type="email"
                  fullWidth
                  value={formData.email}
                  onChange={(e) =>
                    setFormData({ ...formData, email: e.target.value })
                  }
                />
                <TextField
                  label="Phone"
                  fullWidth
                  value={formData.phone}
                  onChange={(e) =>
                    setFormData({ ...formData, phone: e.target.value })
                  }
                />
              </Box>

              <TextField
                label="Address"
                fullWidth
                value={formData.address}
                onChange={(e) =>
                  setFormData({ ...formData, address: e.target.value })
                }
              />

              <TextField
                label="Address 2"
                fullWidth
                value={formData.address2}
                onChange={(e) =>
                  setFormData({ ...formData, address2: e.target.value })
                }
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
                  value={formData.zip}
                  onChange={(e) => setFormData({ ...formData, zip: e.target.value })}
                />
                <TextField
                  label="Country"
                  fullWidth
                  value={formData.country}
                  onChange={(e) =>
                    setFormData({ ...formData, country: e.target.value })
                  }
                />
              </Box>

              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.isPublic}
                    onChange={(e) =>
                      setFormData({ ...formData, isPublic: e.target.checked })
                    }
                  />
                }
                label="Public customer (visible to all tenant users)"
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={!formData.title}
              sx={{ bgcolor: '#0F3E5C' }}
            >
              {editingCustomer ? 'Save' : 'Add'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Customer Details Drawer - Right-side slide-in matching ThingsBoard */}
        <CustomerDetailsDrawer
          open={openDrawer}
          onClose={() => setOpenDrawer(false)}
          customer={selectedCustomer}
          onSave={handleSaveCustomer}
          onDelete={handleDeleteCustomer}
          mode={drawerMode}
        />
      </Box>
    </MainLayout>
  )
}
