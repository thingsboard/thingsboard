/**
 * Asset Profiles Page
 * Manage asset profiles - complete CRUD interface
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
import { assetProfileService } from '../services/assetProfileService'
import {
  AssetProfile,
  createDefaultAssetProfile,
} from '../types/assetprofile.types'

export default function AssetProfilesPage() {
  const [profiles, setProfiles] = useState<AssetProfile[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchText, setSearchText] = useState('')
  const [editDialog, setEditDialog] = useState(false)
  const [deleteDialog, setDeleteDialog] = useState(false)
  const [selectedProfile, setSelectedProfile] = useState<AssetProfile | null>(null)
  const [formData, setFormData] = useState<AssetProfile>(createDefaultAssetProfile())

  useEffect(() => {
    loadProfiles()
  }, [])

  const loadProfiles = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await assetProfileService.getAssetProfiles()
      setProfiles(data)
    } catch (err) {
      setError('Failed to load asset profiles')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = () => {
    setFormData(createDefaultAssetProfile())
    setSelectedProfile(null)
    setEditDialog(true)
  }

  const handleEdit = (profile: AssetProfile) => {
    setFormData({ ...profile })
    setSelectedProfile(profile)
    setEditDialog(true)
  }

  const handleSave = async () => {
    try {
      setError(null)
      if (selectedProfile) {
        await assetProfileService.updateAssetProfile(formData)
      } else {
        await assetProfileService.createAssetProfile(formData)
      }
      setEditDialog(false)
      loadProfiles()
    } catch (err) {
      setError('Failed to save asset profile')
      console.error(err)
    }
  }

  const handleDelete = async () => {
    if (!selectedProfile?.id) return

    try {
      setError(null)
      await assetProfileService.deleteAssetProfile(selectedProfile.id.id)
      setDeleteDialog(false)
      setSelectedProfile(null)
      loadProfiles()
    } catch (err) {
      setError('Failed to delete asset profile')
      console.error(err)
    }
  }

  const handleSetDefault = async (profile: AssetProfile) => {
    if (!profile.id) return

    try {
      setError(null)
      await assetProfileService.setDefaultAssetProfile(profile.id.id)
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
        <Typography variant="h4">Asset Profiles</Typography>
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
              <TableCell>Description</TableCell>
              <TableCell>Default</TableCell>
              <TableCell>Queue</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  Loading...
                </TableCell>
              </TableRow>
            ) : filteredProfiles.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  No asset profiles found
                </TableCell>
              </TableRow>
            ) : (
              filteredProfiles.map((profile) => (
                <TableRow key={profile.id?.id || profile.name} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={600}>
                      {profile.name}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="caption" color="text.secondary">
                      {profile.description || '-'}
                    </Typography>
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
              rows={3}
              fullWidth
            />
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
        <DialogTitle>Delete Asset Profile</DialogTitle>
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
