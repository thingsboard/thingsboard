/**
 * Assets Page
 * Asset management with CRUD operations
 * Matches ThingsBoard ui-ngx assets functionality
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
  Link as LinkIcon,
  Dashboard as DashboardIcon,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'
import EntityTable, { EntityColumn } from '@/components/entity/EntityTable'
import AssetDetailsDrawer from '@/components/drawers/AssetDetailsDrawer'

interface Asset {
  id: string
  name: string
  label?: string
  type: string
  assetProfileId?: string
  assetProfileName?: string
  customerId?: string
  customerTitle?: string
  customerIsPublic?: boolean
  createdTime: number
  additionalInfo?: any
}

const ASSET_TYPES = [
  'building',
  'floor',
  'room',
  'production_line',
  'vehicle',
  'equipment',
  'infrastructure',
  'zone',
]

export default function AssetsPage() {
  const navigate = useNavigate()
  const [assets, setAssets] = useState<Asset[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')

  // Dialog states
  const [openDialog, setOpenDialog] = useState(false)
  const [editingAsset, setEditingAsset] = useState<Asset | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    label: '',
    type: 'building',
    assetProfileId: '',
  })

  // Drawer states
  const [openDrawer, setOpenDrawer] = useState(false)
  const [selectedAsset, setSelectedAsset] = useState<Asset | null>(null)
  const [drawerMode, setDrawerMode] = useState<'view' | 'edit' | 'create'>('view')

  // Load assets (mock data for now)
  useEffect(() => {
    loadAssets()
  }, [page, pageSize, searchQuery])

  const loadAssets = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockAssets: Asset[] = [
      {
        id: '1',
        name: 'Building A',
        label: 'Main Manufacturing Facility',
        type: 'building',
        assetProfileName: 'Default',
        customerTitle: 'Customer A',
        createdTime: Date.now() - 86400000 * 60,
      },
      {
        id: '2',
        name: 'Floor 1',
        label: 'Ground Floor',
        type: 'floor',
        assetProfileName: 'Default',
        customerTitle: 'Customer A',
        createdTime: Date.now() - 86400000 * 55,
      },
      {
        id: '3',
        name: 'Production Line 1',
        label: 'Assembly Line',
        type: 'production_line',
        assetProfileName: 'Production Profile',
        customerTitle: 'Customer A',
        createdTime: Date.now() - 86400000 * 50,
      },
      {
        id: '4',
        name: 'Warehouse B',
        label: 'Storage Facility',
        type: 'building',
        assetProfileName: 'Warehouse Profile',
        customerTitle: 'Customer B',
        createdTime: Date.now() - 86400000 * 45,
      },
      {
        id: '5',
        name: 'Vehicle Fleet',
        label: 'Delivery Trucks',
        type: 'vehicle',
        assetProfileName: 'Vehicle Profile',
        customerTitle: 'Customer C',
        createdTime: Date.now() - 86400000 * 40,
      },
      {
        id: '6',
        name: 'HVAC System',
        label: 'Building A Climate Control',
        type: 'equipment',
        assetProfileName: 'Equipment Profile',
        createdTime: Date.now() - 86400000 * 35,
      },
      {
        id: '7',
        name: 'North Zone',
        label: 'Manufacturing Zone North',
        type: 'zone',
        assetProfileName: 'Zone Profile',
        customerTitle: 'Customer A',
        createdTime: Date.now() - 86400000 * 30,
      },
    ]

    // Filter by search query
    const filtered = searchQuery
      ? mockAssets.filter((a) =>
          a.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          a.label?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          a.type.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : mockAssets

    setAssets(filtered.slice(page * pageSize, (page + 1) * pageSize))
    setTotalElements(filtered.length)
    setLoading(false)
  }

  const columns: EntityColumn[] = [
    {
      id: 'name',
      label: 'Name',
      minWidth: 200,
      format: (value, row: Asset) => (
        <Box>
          <Box
            sx={{ fontWeight: 600, color: '#0F3E5C', cursor: 'pointer' }}
            onClick={() => navigate(`/assets/${row.id}`)}
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
      label: 'Asset Type',
      minWidth: 150,
      format: (value) => (
        <Chip
          label={value.replace(/_/g, ' ').toUpperCase()}
          size="small"
          sx={{
            bgcolor: '#FFF3E0',
            color: '#E65100',
            fontWeight: 500,
          }}
        />
      ),
    },
    {
      id: 'assetProfileName',
      label: 'Asset Profile',
      minWidth: 150,
    },
    {
      id: 'customerTitle',
      label: 'Customer',
      minWidth: 150,
      format: (value) =>
        value || <Chip label="Unassigned" size="small" variant="outlined" />,
    },
    {
      id: 'createdTime',
      label: 'Created',
      minWidth: 150,
      format: (value: number) => format(new Date(value), 'MMM dd, yyyy HH:mm'),
    },
  ]

  const handleAdd = () => {
    setSelectedAsset({
      id: '',
      name: '',
      label: '',
      type: 'building',
      createdTime: Date.now(),
    })
    setDrawerMode('create')
    setOpenDrawer(true)
  }

  const handleEdit = (asset: Asset) => {
    setSelectedAsset(asset)
    setDrawerMode('edit')
    setOpenDrawer(true)
  }

  const handleDelete = async (ids: string[]) => {
    if (confirm(`Delete ${ids.length} asset(s)?`)) {
      // API call would go here
      console.log('Deleting assets:', ids)
      loadAssets()
      setSelectedIds([])
    }
  }

  const handleSave = async () => {
    // API call would go here
    console.log('Saving asset:', formData)
    setOpenDialog(false)
    loadAssets()
  }

  const handleViewDetails = (asset: Asset) => {
    setSelectedAsset(asset)
    setDrawerMode('view')
    setOpenDrawer(true)
  }

  const handleSaveAsset = (asset: Asset) => {
    // API call would go here
    console.log('Saving asset:', asset)
    setOpenDrawer(false)
    loadAssets()
  }

  const handleDeleteAsset = (assetId: string) => {
    // API call would go here
    console.log('Deleting asset:', assetId)
    setOpenDrawer(false)
    loadAssets()
  }

  const handleViewDashboard = (asset: Asset) => {
    navigate(`/dashboards/asset/${asset.id}`)
  }

  const rowActions = (row: Asset) => (
    <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
      <Tooltip title="View details">
        <IconButton size="small" onClick={() => handleViewDetails(row)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="View dashboard">
        <IconButton size="small" onClick={() => handleViewDashboard(row)}>
          <DashboardIcon fontSize="small" />
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
          title="Assets"
          columns={columns}
          rows={assets}
          totalElements={totalElements}
          loading={loading}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onRefresh={loadAssets}
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
          <DialogTitle>{editingAsset ? 'Edit Asset' : 'Add Asset'}</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Asset Name"
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
                helperText="Optional asset label"
              />
              <FormControl fullWidth>
                <InputLabel>Asset Type</InputLabel>
                <Select
                  value={formData.type}
                  label="Asset Type"
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                >
                  {ASSET_TYPES.map((type) => (
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
              {editingAsset ? 'Save' : 'Add'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Asset Details Drawer - Right-side slide-in matching ThingsBoard */}
        <AssetDetailsDrawer
          open={openDrawer}
          onClose={() => setOpenDrawer(false)}
          asset={selectedAsset}
          onSave={handleSaveAsset}
          onDelete={handleDeleteAsset}
          mode={drawerMode}
        />
      </Box>
    </MainLayout>
  )
}
