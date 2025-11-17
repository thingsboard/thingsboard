/**
 * Device Profiles Page
 * Manage device profiles - complete CRUD interface
 */

import { useState, useEffect } from 'react'
import {
  Box,
  Button,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
  Tooltip,
  Alert,
} from '@mui/material'
import {
  Add,
  Edit,
  Delete,
  Star,
  StarBorder,
  Search,
} from '@mui/icons-material'
import { deviceProfileService } from '../services/deviceProfileService'
import {
  DeviceProfile,
  createDefaultDeviceProfile,
  deviceTransportTypeNames,
  DeviceTransportType,
  DeviceProvisionType,
  deviceProvisionTypeNames,
} from '../types/device.types'

export default function DeviceProfilesPage() {
  const [profiles, setProfiles] = useState<DeviceProfile[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchText, setSearchText] = useState('')
  const [editDialog, setEditDialog] = useState(false)
  const [deleteDialog, setDeleteDialog] = useState(false)
  const [selectedProfile, setSelectedProfile] = useState<DeviceProfile | null>(null)
  const [formData, setFormData] = useState<DeviceProfile>(createDefaultDeviceProfile())

  useEffect(() => {
    loadProfiles()
  }, [])

  const loadProfiles = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await deviceProfileService.getDeviceProfiles()
      setProfiles(data)
    } catch (err) {
      setError('Failed to load device profiles')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = () => {
    setFormData(createDefaultDeviceProfile())
    setSelectedProfile(null)
    setEditDialog(true)
  }

  const handleEdit = (profile: DeviceProfile) => {
    setFormData({ ...profile })
    setSelectedProfile(profile)
    setEditDialog(true)
  }

  const handleSave = async () => {
    try {
      setError(null)
      if (selectedProfile) {
        await deviceProfileService.updateDeviceProfile(formData)
      } else {
        await deviceProfileService.createDeviceProfile(formData)
      }
      setEditDialog(false)
      loadProfiles()
    } catch (err) {
      setError('Failed to save device profile')
      console.error(err)
    }
  }

  const handleDelete = async () => {
    if (!selectedProfile?.id) return

    try {
      setError(null)
      await deviceProfileService.deleteDeviceProfile(selectedProfile.id.id)
      setDeleteDialog(false)
      setSelectedProfile(null)
      loadProfiles()
    } catch (err) {
      setError('Failed to delete device profile')
      console.error(err)
    }
  }

  const handleSetDefault = async (profile: DeviceProfile) => {
    if (!profile.id) return

    try {
      setError(null)
      await deviceProfileService.setDefaultDeviceProfile(profile.id.id)
      loadProfiles()
    } catch (err) {
      setError('Failed to set default profile')
      console.error(err)
    }
  }

  const filteredProfiles = profiles.filter((profile) =>
    profile.name.toLowerCase().includes(searchText.toLowerCase())
  )

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Device Profiles</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={handleCreate}>
          Create Profile
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Search */}
      <Box sx={{ mb: 3 }}>
        <TextField
          fullWidth
          placeholder="Search profiles..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          InputProps={{
            startAdornment: <Search sx={{ mr: 1, color: 'action.active' }} />,
          }}
        />
      </Box>

      {/* Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Transport</TableCell>
              <TableCell>Provisioning</TableCell>
              <TableCell>Default</TableCell>
              <TableCell>Queue</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  Loading...
                </TableCell>
              </TableRow>
            ) : filteredProfiles.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  No device profiles found
                </TableCell>
              </TableRow>
            ) : (
              filteredProfiles.map((profile) => (
                <TableRow key={profile.id?.id || profile.name} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={600}>
                      {profile.name}
                    </Typography>
                    {profile.description && (
                      <Typography variant="caption" color="text.secondary">
                        {profile.description}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip label={profile.type} size="small" />
                  </TableCell>
                  <TableCell>
                    {deviceTransportTypeNames[profile.transportType] || profile.transportType}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={deviceProvisionTypeNames[profile.provisionType] || profile.provisionType}
                      size="small"
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    <IconButton
                      size="small"
                      onClick={() => handleSetDefault(profile)}
                      disabled={profile.isDefault}
                    >
                      {profile.isDefault ? (
                        <Star sx={{ color: 'warning.main' }} />
                      ) : (
                        <StarBorder />
                      )}
                    </IconButton>
                  </TableCell>
                  <TableCell>
                    <Typography variant="caption">{profile.defaultQueueName || 'Main'}</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Edit">
                      <IconButton size="small" onClick={() => handleEdit(profile)}>
                        <Edit fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete">
                      <IconButton
                        size="small"
                        onClick={() => {
                          setSelectedProfile(profile)
                          setDeleteDialog(true)
                        }}
                        disabled={profile.isDefault}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Edit Dialog */}
      <Dialog open={editDialog} onClose={() => setEditDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedProfile ? 'Edit Profile' : 'Create Profile'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            <TextField
              label="Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Description"
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              multiline
              rows={2}
              fullWidth
            />
            <FormControl fullWidth>
              <InputLabel>Transport Type</InputLabel>
              <Select
                value={formData.transportType}
                label="Transport Type"
                onChange={(e) =>
                  setFormData({ ...formData, transportType: e.target.value as DeviceTransportType })
                }
              >
                {Object.entries(deviceTransportTypeNames).map(([value, label]) => (
                  <MenuItem key={value} value={value}>
                    {label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Provisioning Strategy</InputLabel>
              <Select
                value={formData.provisionType}
                label="Provisioning Strategy"
                onChange={(e) =>
                  setFormData({ ...formData, provisionType: e.target.value as DeviceProvisionType })
                }
              >
                {Object.entries(deviceProvisionTypeNames).map(([value, label]) => (
                  <MenuItem key={value} value={value}>
                    {label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Default Queue Name"
              value={formData.defaultQueueName || ''}
              onChange={(e) => setFormData({ ...formData, defaultQueueName: e.target.value })}
              fullWidth
              placeholder="Main"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialog(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" disabled={!formData.name}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Dialog */}
      <Dialog open={deleteDialog} onClose={() => setDeleteDialog(false)}>
        <DialogTitle>Delete Device Profile</DialogTitle>
        <DialogContent>
          Are you sure you want to delete "{selectedProfile?.name}"? This action cannot be undone.
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialog(false)}>Cancel</Button>
          <Button onClick={handleDelete} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
