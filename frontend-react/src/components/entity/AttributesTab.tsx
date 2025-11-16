/**
 * Attributes Tab Component
 * Reusable component for displaying and managing entity attributes
 * Matches ThingsBoard's exact attribute management functionality
 *
 * Attribute Types:
 * - Server: Set by server-side rules/plugins
 * - Shared: Bidirectional sync between server and device
 * - Client: Set by device/client
 */

import { useState } from 'react'
import {
  Box,
  Paper,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Tooltip,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  Chip,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import { format } from 'date-fns'

export type AttributeScope = 'SERVER_SCOPE' | 'SHARED_SCOPE' | 'CLIENT_SCOPE'

export interface Attribute {
  key: string
  value: any
  lastUpdateTs?: number
  scope?: AttributeScope
}

interface AttributesTabProps {
  entityId: string
  entityType: 'DEVICE' | 'ASSET' | 'CUSTOMER' | 'TENANT' | 'USER'
  attributes?: Attribute[]
  onRefresh?: () => void
  onSave?: (scope: AttributeScope, key: string, value: any) => void
  onDelete?: (scope: AttributeScope, key: string) => void
  readOnly?: boolean
}

export default function AttributesTab({
  entityId,
  entityType,
  attributes = [],
  onRefresh,
  onSave,
  onDelete,
  readOnly = false,
}: AttributesTabProps) {
  const [selectedScope, setSelectedScope] = useState<AttributeScope>('SERVER_SCOPE')
  const [openDialog, setOpenDialog] = useState(false)
  const [editingAttribute, setEditingAttribute] = useState<Attribute | null>(null)
  const [formData, setFormData] = useState({
    key: '',
    value: '',
  })

  // Filter attributes by scope
  const filteredAttributes = attributes.filter((attr) => attr.scope === selectedScope)

  const handleScopeChange = (_event: React.SyntheticEvent, newValue: AttributeScope) => {
    setSelectedScope(newValue)
  }

  const handleAdd = () => {
    setEditingAttribute(null)
    setFormData({ key: '', value: '' })
    setOpenDialog(true)
  }

  const handleEdit = (attribute: Attribute) => {
    setEditingAttribute(attribute)
    setFormData({
      key: attribute.key,
      value: typeof attribute.value === 'object' ? JSON.stringify(attribute.value, null, 2) : String(attribute.value),
    })
    setOpenDialog(true)
  }

  const handleDelete = (attribute: Attribute) => {
    if (confirm(`Delete attribute "${attribute.key}"?`)) {
      onDelete?.(selectedScope, attribute.key)
    }
  }

  const handleSave = () => {
    if (!formData.key || !formData.value) {
      alert('Please fill in both key and value')
      return
    }

    try {
      // Try to parse as JSON, otherwise use as string
      let parsedValue: any
      try {
        parsedValue = JSON.parse(formData.value)
      } catch {
        parsedValue = formData.value
      }

      onSave?.(selectedScope, formData.key, parsedValue)
      setOpenDialog(false)
    } catch (error) {
      alert('Error saving attribute: ' + error)
    }
  }

  const getScopeLabel = (scope: AttributeScope) => {
    switch (scope) {
      case 'SERVER_SCOPE':
        return 'Server attributes'
      case 'SHARED_SCOPE':
        return 'Shared attributes'
      case 'CLIENT_SCOPE':
        return 'Client attributes'
    }
  }

  const getScopeDescription = (scope: AttributeScope) => {
    switch (scope) {
      case 'SERVER_SCOPE':
        return 'Server-side attributes are set by server-side rules, plugins, or REST API'
      case 'SHARED_SCOPE':
        return 'Shared attributes are synchronized between the server and the device'
      case 'CLIENT_SCOPE':
        return 'Client-side attributes are reported by the device'
    }
  }

  const formatValue = (value: any): string => {
    if (value === null || value === undefined) return ''
    if (typeof value === 'object') return JSON.stringify(value)
    return String(value)
  }

  return (
    <Box>
      <Paper sx={{ p: 3 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6" sx={{ color: '#0F3E5C' }}>
            Attributes
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {onRefresh && (
              <IconButton color="primary" onClick={onRefresh} title="Refresh">
                <RefreshIcon />
              </IconButton>
            )}
            {!readOnly && (
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={handleAdd}
                size="small"
                sx={{ bgcolor: '#0F3E5C' }}
              >
                Add Attribute
              </Button>
            )}
          </Box>
        </Box>

        {/* Scope Tabs */}
        <Tabs
          value={selectedScope}
          onChange={handleScopeChange}
          sx={{
            mb: 2,
            borderBottom: 1,
            borderColor: 'divider',
          }}
        >
          <Tab label="Server" value="SERVER_SCOPE" />
          <Tab label="Shared" value="SHARED_SCOPE" />
          <Tab label="Client" value="CLIENT_SCOPE" />
        </Tabs>

        {/* Scope Description */}
        <Alert severity="info" sx={{ mb: 2 }}>
          {getScopeDescription(selectedScope)}
        </Alert>

        {/* Attributes Table */}
        {filteredAttributes.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography color="text.secondary">
              No {getScopeLabel(selectedScope).toLowerCase()} found
            </Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Key</TableCell>
                  <TableCell>Value</TableCell>
                  {selectedScope !== 'CLIENT_SCOPE' && <TableCell>Last Update</TableCell>}
                  {!readOnly && <TableCell align="right">Actions</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredAttributes.map((attr) => (
                  <TableRow key={attr.key}>
                    <TableCell>
                      <strong>{attr.key}</strong>
                    </TableCell>
                    <TableCell>
                      <Typography
                        variant="body2"
                        sx={{
                          maxWidth: 400,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {formatValue(attr.value)}
                      </Typography>
                    </TableCell>
                    {selectedScope !== 'CLIENT_SCOPE' && (
                      <TableCell>
                        {attr.lastUpdateTs
                          ? format(new Date(attr.lastUpdateTs), 'MMM dd, yyyy HH:mm:ss')
                          : '-'}
                      </TableCell>
                    )}
                    {!readOnly && (
                      <TableCell align="right">
                        <Tooltip title="Edit">
                          <IconButton size="small" onClick={() => handleEdit(attr)}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton size="small" onClick={() => handleDelete(attr)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}

        {/* Stats */}
        <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
          <Chip
            label={`${filteredAttributes.length} ${getScopeLabel(selectedScope).toLowerCase()}`}
            size="small"
            color="primary"
          />
        </Box>
      </Paper>

      {/* Add/Edit Dialog */}
      <Dialog
        open={openDialog}
        onClose={() => setOpenDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          {editingAttribute ? 'Edit Attribute' : 'Add Attribute'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            <Alert severity="info">
              Scope: <strong>{getScopeLabel(selectedScope)}</strong>
            </Alert>
            <TextField
              label="Key"
              required
              fullWidth
              value={formData.key}
              onChange={(e) => setFormData({ ...formData, key: e.target.value })}
              disabled={!!editingAttribute}
              helperText="Attribute key (cannot be changed after creation)"
            />
            <TextField
              label="Value"
              required
              fullWidth
              multiline
              rows={4}
              value={formData.value}
              onChange={(e) => setFormData({ ...formData, value: e.target.value })}
              helperText="Enter value as string, number, boolean, or JSON object"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={!formData.key || !formData.value}
            sx={{ bgcolor: '#0F3E5C' }}
          >
            {editingAttribute ? 'Save' : 'Add'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
