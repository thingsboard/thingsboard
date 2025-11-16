/**
 * Devices Page
 * Device management with CRUD operations
 * Matches ThingsBoard ui-ngx devices functionality
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
} from '@mui/material'
import {
  Visibility,
  Key,
  Link as LinkIcon,
  CheckCircle,
  Cancel,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'
import EntityTable, { EntityColumn } from '@/components/entity/EntityTable'
import DeviceDetailsDrawer from '@/components/drawers/DeviceDetailsDrawer'

interface Device {
  id: string
  name: string
  label?: string
  type: string
  deviceProfileId?: string
  deviceProfileName?: string
  customerId?: string
  customerTitle?: string
  customerIsPublic?: boolean
  active?: boolean
  createdTime: number
  additionalInfo?: any
}

const DEVICE_TYPES = [
  'default',
  'temperature_sensor',
  'humidity_sensor',
  'gateway',
  'controller',
  'actuator',
  'meter',
  'camera',
]

export default function DevicesPage() {
  const navigate = useNavigate()
  const [devices, setDevices] = useState<Device[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')

  // Dialog states
  const [openDialog, setOpenDialog] = useState(false)
  const [editingDevice, setEditingDevice] = useState<Device | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    label: '',
    type: 'default',
    deviceProfileId: '',
  })

  // Drawer states
  const [openDrawer, setOpenDrawer] = useState(false)
  const [selectedDevice, setSelectedDevice] = useState<Device | null>(null)
  const [drawerMode, setDrawerMode] = useState<'view' | 'edit' | 'create'>('view')

  // Load devices (mock data for now)
  useEffect(() => {
    loadDevices()
  }, [page, pageSize, searchQuery])

  const loadDevices = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockDevices: Device[] = [
      {
        id: '1',
        name: 'Temperature Sensor 001',
        label: 'Building A - Room 101',
        type: 'temperature_sensor',
        deviceProfileName: 'Default',
        customerTitle: 'Customer A',
        active: true,
        createdTime: Date.now() - 86400000 * 30,
      },
      {
        id: '2',
        name: 'Humidity Sensor 001',
        label: 'Building A - Room 102',
        type: 'humidity_sensor',
        deviceProfileName: 'Default',
        customerTitle: 'Customer A',
        active: true,
        createdTime: Date.now() - 86400000 * 25,
      },
      {
        id: '3',
        name: 'Gateway 001',
        label: 'Main Gateway',
        type: 'gateway',
        deviceProfileName: 'Gateway Profile',
        customerTitle: 'Customer B',
        active: false,
        createdTime: Date.now() - 86400000 * 20,
      },
      {
        id: '4',
        name: 'Controller 001',
        label: 'HVAC Controller',
        type: 'controller',
        deviceProfileName: 'Controller Profile',
        active: true,
        createdTime: Date.now() - 86400000 * 15,
      },
      {
        id: '5',
        name: 'Flow Meter 001',
        label: 'Building B - Pipe 1',
        type: 'meter',
        deviceProfileName: 'Meter Profile',
        customerTitle: 'Customer C',
        active: true,
        createdTime: Date.now() - 86400000 * 10,
      },
    ]

    // Filter by search query
    const filtered = searchQuery
      ? mockDevices.filter((d) =>
          d.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          d.label?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          d.type.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : mockDevices

    setDevices(filtered.slice(page * pageSize, (page + 1) * pageSize))
    setTotalElements(filtered.length)
    setLoading(false)
  }

  const columns: EntityColumn[] = [
    {
      id: 'name',
      label: 'Name',
      minWidth: 200,
      format: (value, row: Device) => (
        <Box>
          <Box sx={{ fontWeight: 600, color: '#0F3E5C', cursor: 'pointer' }}
            onClick={() => navigate(`/devices/${row.id}`)}
          >
            {value}
          </Box>
          {row.label && (
            <Box sx={{ fontSize: '0.75rem', color: '#8C959D', mt: 0.5 }}>
              {row.label}
            </Box>
          )}
        </Box>
      ),
    },
    {
      id: 'type',
      label: 'Device Type',
      minWidth: 150,
      format: (value) => (
        <Chip
          label={value.replace(/_/g, ' ').toUpperCase()}
          size="small"
          sx={{
            bgcolor: '#E3F2FD',
            color: '#0F3E5C',
            fontWeight: 500,
          }}
        />
      ),
    },
    {
      id: 'deviceProfileName',
      label: 'Device Profile',
      minWidth: 150,
    },
    {
      id: 'customerTitle',
      label: 'Customer',
      minWidth: 150,
      format: (value) => value || <Chip label="Unassigned" size="small" variant="outlined" />,
    },
    {
      id: 'active',
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
            icon={<Cancel />}
            label="Inactive"
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
    setSelectedDevice({
      id: '',
      name: '',
      label: '',
      type: 'default',
      active: true,
      createdTime: Date.now(),
    })
    setDrawerMode('create')
    setOpenDrawer(true)
  }

  const handleEdit = (device: Device) => {
    setSelectedDevice(device)
    setDrawerMode('edit')
    setOpenDrawer(true)
  }

  const handleDelete = async (ids: string[]) => {
    if (confirm(`Delete ${ids.length} device(s)?`)) {
      // API call would go here
      console.log('Deleting devices:', ids)
      loadDevices()
      setSelectedIds([])
    }
  }

  const handleSave = async () => {
    // API call would go here
    console.log('Saving device:', formData)
    setOpenDialog(false)
    loadDevices()
  }

  const handleViewDetails = (device: Device) => {
    setSelectedDevice(device)
    setDrawerMode('view')
    setOpenDrawer(true)
  }

  const handleSaveDevice = (device: Device) => {
    // API call would go here
    console.log('Saving device:', device)
    setOpenDrawer(false)
    loadDevices()
  }

  const handleDeleteDevice = (deviceId: string) => {
    // API call would go here
    console.log('Deleting device:', deviceId)
    setOpenDrawer(false)
    loadDevices()
  }

  const handleManageCredentials = (device: Device) => {
    navigate(`/devices/${device.id}/credentials`)
  }

  const rowActions = (row: Device) => (
    <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
      <Tooltip title="View details">
        <IconButton size="small" onClick={() => handleViewDetails(row)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Manage credentials">
        <IconButton size="small" onClick={() => handleManageCredentials(row)}>
          <Key fontSize="small" />
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
          title="Devices"
          columns={columns}
          rows={devices}
          totalElements={totalElements}
          loading={loading}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onRefresh={loadDevices}
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
        <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
          <DialogTitle>
            {editingDevice ? 'Edit Device' : 'Add Device'}
          </DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Device Name"
                required
                fullWidth
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
              <TextField
                label="Label"
                fullWidth
                value={formData.label}
                onChange={(e) => setFormData({ ...formData, label: e.target.value })}
                helperText="Optional device label"
              />
              <FormControl fullWidth>
                <InputLabel>Device Type</InputLabel>
                <Select
                  value={formData.type}
                  label="Device Type"
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                >
                  {DEVICE_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type.replace(/_/g, ' ').toUpperCase()}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={!formData.name}
              sx={{ bgcolor: '#0F3E5C' }}
            >
              {editingDevice ? 'Save' : 'Add'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Device Details Drawer - Right-side slide-in matching ThingsBoard */}
        <DeviceDetailsDrawer
          open={openDrawer}
          onClose={() => setOpenDrawer(false)}
          device={selectedDevice}
          onSave={handleSaveDevice}
          onDelete={handleDeleteDevice}
          mode={drawerMode}
        />
      </Box>
    </MainLayout>
  )
}
