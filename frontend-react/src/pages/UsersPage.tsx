/**
 * Users Page
 * User management with roles and permissions
 * Matches ThingsBoard ui-ngx users functionality
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
  InputAdornment,
} from '@mui/material'
import {
  Visibility,
  VisibilityOff,
  Block,
  CheckCircle,
  Email,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'
import EntityTable, { EntityColumn } from '@/components/entity/EntityTable'
import UserDetailsDrawer from '@/components/drawers/UserDetailsDrawer'

interface User {
  id: string
  email: string
  firstName?: string
  lastName?: string
  authority: 'SYS_ADMIN' | 'TENANT_ADMIN' | 'CUSTOMER_USER'
  customerId?: string
  customerTitle?: string
  enabled?: boolean
  createdTime: number
  additionalInfo?: any
}

const USER_AUTHORITIES = [
  { value: 'TENANT_ADMIN', label: 'Tenant Administrator' },
  { value: 'CUSTOMER_USER', label: 'Customer User' },
]

const SYS_ADMIN_AUTHORITIES = [
  { value: 'SYS_ADMIN', label: 'System Administrator' },
  ...USER_AUTHORITIES,
]

export default function UsersPage() {
  const navigate = useNavigate()
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')

  // Dialog states
  const [openDialog, setOpenDialog] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const [formData, setFormData] = useState({
    email: '',
    firstName: '',
    lastName: '',
    authority: 'CUSTOMER_USER' as User['authority'],
    customerId: '',
    password: '',
    enabled: true,
  })

  // Assume current user is TENANT_ADMIN for demo
  const currentUserAuthority = 'TENANT_ADMIN'

  // Drawer states
  const [openDrawer, setOpenDrawer] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [drawerMode, setDrawerMode] = useState<'view' | 'edit' | 'create'>('view')

  // Load users (mock data for now)
  useEffect(() => {
    loadUsers()
  }, [page, pageSize, searchQuery])

  const loadUsers = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockUsers: User[] = [
      {
        id: '1',
        email: 'admin@payvar.io',
        firstName: 'System',
        lastName: 'Administrator',
        authority: 'SYS_ADMIN',
        enabled: true,
        createdTime: Date.now() - 86400000 * 365,
      },
      {
        id: '2',
        email: 'tenant@payvar.io',
        firstName: 'Tenant',
        lastName: 'Admin',
        authority: 'TENANT_ADMIN',
        enabled: true,
        createdTime: Date.now() - 86400000 * 180,
      },
      {
        id: '3',
        email: 'john.doe@abcmfg.com',
        firstName: 'John',
        lastName: 'Doe',
        authority: 'CUSTOMER_USER',
        customerTitle: 'ABC Manufacturing Inc',
        enabled: true,
        createdTime: Date.now() - 86400000 * 90,
      },
      {
        id: '4',
        email: 'jane.smith@xyzlogistics.com',
        firstName: 'Jane',
        lastName: 'Smith',
        authority: 'CUSTOMER_USER',
        customerTitle: 'XYZ Logistics',
        enabled: true,
        createdTime: Date.now() - 86400000 * 60,
      },
      {
        id: '5',
        email: 'disabled.user@example.com',
        firstName: 'Disabled',
        lastName: 'User',
        authority: 'CUSTOMER_USER',
        customerTitle: 'ABC Manufacturing Inc',
        enabled: false,
        createdTime: Date.now() - 86400000 * 120,
      },
    ]

    // Filter by search query
    const filtered = searchQuery
      ? mockUsers.filter(
          (u) =>
            u.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
            u.firstName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            u.lastName?.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : mockUsers

    setUsers(filtered.slice(page * pageSize, (page + 1) * pageSize))
    setTotalElements(filtered.length)
    setLoading(false)
  }

  const columns: EntityColumn[] = [
    {
      id: 'email',
      label: 'Email',
      minWidth: 200,
      format: (value, row: User) => (
        <Box>
          <Box
            sx={{ fontWeight: 600, color: '#0F3E5C', cursor: 'pointer' }}
            onClick={() => navigate(`/users/${row.id}`)}
          >
            {value}
          </Box>
          {row.firstName && row.lastName && (
            <Box sx={{ fontSize: '0.75rem', color: '#8C959D', mt: 0.5 }}>
              {row.firstName} {row.lastName}
            </Box>
          )}
        </Box>
      ),
    },
    {
      id: 'authority',
      label: 'Authority',
      minWidth: 180,
      format: (value: User['authority']) => {
        const authorityConfig = {
          SYS_ADMIN: { label: 'System Admin', color: '#C62828', bgcolor: '#FFEBEE' },
          TENANT_ADMIN: { label: 'Tenant Admin', color: '#0F3E5C', bgcolor: '#E3F2FD' },
          CUSTOMER_USER: { label: 'Customer User', color: '#2E7D6F', bgcolor: '#E0F2F1' },
        }
        const config = authorityConfig[value]
        return (
          <Chip
            label={config.label}
            size="small"
            sx={{
              bgcolor: config.bgcolor,
              color: config.color,
              fontWeight: 500,
            }}
          />
        )
      },
    },
    {
      id: 'customerTitle',
      label: 'Customer',
      minWidth: 150,
      format: (value, row: User) =>
        row.authority === 'CUSTOMER_USER' ? (
          value || <Chip label="Unassigned" size="small" variant="outlined" />
        ) : (
          '-'
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
    setSelectedUser({
      id: '',
      email: '',
      firstName: '',
      lastName: '',
      authority: 'CUSTOMER_USER',
      enabled: true,
      createdTime: Date.now(),
    })
    setDrawerMode('create')
    setOpenDrawer(true)
  }

  const handleEdit = (user: User) => {
    setSelectedUser(user)
    setDrawerMode('edit')
    setOpenDrawer(true)
  }

  const handleViewDetails = (user: User) => {
    setSelectedUser(user)
    setDrawerMode('view')
    setOpenDrawer(true)
  }

  const handleSaveUser = (user: User) => {
    // API call would go here
    console.log('Saving user:', user)
    setOpenDrawer(false)
    loadUsers()
  }

  const handleDeleteUser = (userId: string) => {
    // API call would go here
    console.log('Deleting user:', userId)
    setOpenDrawer(false)
    loadUsers()
  }

  const handleActivateUser = (userId: string, enabled: boolean) => {
    // API call would go here
    console.log('Activating/Deactivating user:', userId, enabled)
    loadUsers()
  }

  const handleResendActivation = (userId: string) => {
    // API call would go here
    console.log('Resending activation to user:', userId)
  }

  const handleDelete = async (ids: string[]) => {
    if (confirm(`Delete ${ids.length} user(s)?`)) {
      // API call would go here
      console.log('Deleting users:', ids)
      loadUsers()
      setSelectedIds([])
    }
  }

  const handleSave = async () => {
    // API call would go here
    console.log('Saving user:', formData)
    setOpenDialog(false)
    loadUsers()
  }

  const rowActions = (row: User) => (
    <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
      <Tooltip title="View details">
        <IconButton size="small" onClick={() => handleViewDetails(row)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>
      {!row.enabled && (
        <Tooltip title="Activate user">
          <IconButton size="small" onClick={() => handleActivateUser(row.id, true)}>
            <CheckCircle fontSize="small" />
          </IconButton>
        </Tooltip>
      )}
      {row.enabled && (
        <Tooltip title="Disable user">
          <IconButton size="small" onClick={() => handleActivateUser(row.id, false)}>
            <Block fontSize="small" />
          </IconButton>
        </Tooltip>
      )}
      <Tooltip title="Send activation email">
        <IconButton size="small" onClick={() => handleResendActivation(row.id)}>
          <Email fontSize="small" />
        </IconButton>
      </Tooltip>
    </Box>
  )

  const availableAuthorities =
    currentUserAuthority === 'SYS_ADMIN'
      ? SYS_ADMIN_AUTHORITIES
      : USER_AUTHORITIES

  return (
    <MainLayout>
      <Box>
        <EntityTable
          title="Users"
          columns={columns}
          rows={users}
          totalElements={totalElements}
          loading={loading}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onRefresh={loadUsers}
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
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>{editingUser ? 'Edit User' : 'Add User'}</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Email"
                type="email"
                required
                fullWidth
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                disabled={!!editingUser}
              />

              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                <TextField
                  label="First Name"
                  fullWidth
                  value={formData.firstName}
                  onChange={(e) =>
                    setFormData({ ...formData, firstName: e.target.value })
                  }
                />
                <TextField
                  label="Last Name"
                  fullWidth
                  value={formData.lastName}
                  onChange={(e) =>
                    setFormData({ ...formData, lastName: e.target.value })
                  }
                />
              </Box>

              <FormControl fullWidth>
                <InputLabel>Authority</InputLabel>
                <Select
                  value={formData.authority}
                  label="Authority"
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      authority: e.target.value as User['authority'],
                    })
                  }
                >
                  {availableAuthorities.map((auth) => (
                    <MenuItem key={auth.value} value={auth.value}>
                      {auth.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              {!editingUser && (
                <TextField
                  label="Password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  fullWidth
                  value={formData.password}
                  onChange={(e) =>
                    setFormData({ ...formData, password: e.target.value })
                  }
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => setShowPassword(!showPassword)}
                          edge="end"
                        >
                          {showPassword ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  }}
                />
              )}

              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.enabled}
                    onChange={(e) =>
                      setFormData({ ...formData, enabled: e.target.checked })
                    }
                  />
                }
                label="User is active"
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={!formData.email || (!editingUser && !formData.password)}
              sx={{ bgcolor: '#0F3E5C' }}
            >
              {editingUser ? 'Save' : 'Add'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* User Details Drawer - Right-side slide-in matching ThingsBoard */}
        <UserDetailsDrawer
          open={openDrawer}
          onClose={() => setOpenDrawer(false)}
          user={selectedUser}
          onSave={handleSaveUser}
          onDelete={handleDeleteUser}
          onActivate={handleActivateUser}
          onResendActivation={handleResendActivation}
          mode={drawerMode}
          currentUserAuthority={currentUserAuthority}
        />
      </Box>
    </MainLayout>
  )
}
