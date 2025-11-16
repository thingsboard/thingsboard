/**
 * Relations Tab Component
 * Displays and manages entity relations (entity graph)
 * Matches ThingsBoard's exact relation management functionality
 *
 * Relation Types:
 * - Contains: Parent-child relationship
 * - Manages: Management relationship
 * - Uses: Usage relationship
 * - Custom: User-defined relationships
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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Alert,
  Autocomplete,
} from '@mui/material'
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
  AccountTree as RelationIcon,
  ArrowForward as ArrowIcon,
} from '@mui/icons-material'

export type RelationDirection = 'FROM' | 'TO'

export interface EntityRelation {
  from: {
    id: string
    entityType: string
  }
  to: {
    id: string
    entityType: string
  }
  type: string
  typeGroup?: string
}

interface RelationsTabProps {
  entityId: string
  entityType: 'DEVICE' | 'ASSET' | 'CUSTOMER' | 'TENANT' | 'USER' | 'DASHBOARD'
  relations?: EntityRelation[]
  onRefresh?: () => void
  onSave?: (relation: EntityRelation) => void
  onDelete?: (relation: EntityRelation) => void
  readOnly?: boolean
}

const COMMON_RELATION_TYPES = [
  'Contains',
  'Manages',
  'Uses',
  'Installed',
  'Produces',
  'Monitors',
  'Controls',
  'Located In',
  'Part Of',
  'Connected To',
]

const ENTITY_TYPES = [
  'DEVICE',
  'ASSET',
  'CUSTOMER',
  'TENANT',
  'USER',
  'DASHBOARD',
  'RULE_CHAIN',
  'ENTITY_VIEW',
]

export default function RelationsTab({
  entityId,
  entityType,
  relations = [],
  onRefresh,
  onSave,
  onDelete,
  readOnly = false,
}: RelationsTabProps) {
  const [openDialog, setOpenDialog] = useState(false)
  const [direction, setDirection] = useState<RelationDirection>('FROM')
  const [formData, setFormData] = useState({
    relationType: 'Contains',
    targetEntityType: 'DEVICE' as string,
    targetEntityId: '',
    targetEntityName: '',
  })

  // Filter relations by direction
  const outboundRelations = relations.filter(
    (rel) => rel.from.id === entityId && rel.from.entityType === entityType
  )
  const inboundRelations = relations.filter(
    (rel) => rel.to.id === entityId && rel.to.entityType === entityType
  )

  const displayRelations = direction === 'FROM' ? outboundRelations : inboundRelations

  const handleAdd = () => {
    setFormData({
      relationType: 'Contains',
      targetEntityType: 'DEVICE',
      targetEntityId: '',
      targetEntityName: '',
    })
    setOpenDialog(true)
  }

  const handleDelete = (relation: EntityRelation) => {
    if (confirm(`Delete relation "${relation.type}"?`)) {
      onDelete?.(relation)
    }
  }

  const handleSave = () => {
    if (!formData.targetEntityId || !formData.relationType) {
      alert('Please fill in all required fields')
      return
    }

    const relation: EntityRelation = direction === 'FROM'
      ? {
          from: { id: entityId, entityType },
          to: { id: formData.targetEntityId, entityType: formData.targetEntityType },
          type: formData.relationType,
        }
      : {
          from: { id: formData.targetEntityId, entityType: formData.targetEntityType },
          to: { id: entityId, entityType },
          type: formData.relationType,
        }

    onSave?.(relation)
    setOpenDialog(false)
  }

  const getEntityLabel = (entityId: string, entityType: string) => {
    // In real implementation, this would fetch the entity name from API
    return `${entityType} (${entityId.substring(0, 8)}...)`
  }

  return (
    <Box>
      <Paper sx={{ p: 3 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6" sx={{ color: '#0F3E5C' }}>
            Relations
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
                Add Relation
              </Button>
            )}
          </Box>
        </Box>

        {/* Direction Toggle */}
        <Box sx={{ mb: 2, display: 'flex', gap: 1 }}>
          <Button
            variant={direction === 'FROM' ? 'contained' : 'outlined'}
            onClick={() => setDirection('FROM')}
            size="small"
            sx={direction === 'FROM' ? { bgcolor: '#0F3E5C' } : {}}
          >
            Outbound ({outboundRelations.length})
          </Button>
          <Button
            variant={direction === 'TO' ? 'contained' : 'outlined'}
            onClick={() => setDirection('TO')}
            size="small"
            sx={direction === 'TO' ? { bgcolor: '#0F3E5C' } : {}}
          >
            Inbound ({inboundRelations.length})
          </Button>
        </Box>

        {/* Info Alert */}
        <Alert severity="info" sx={{ mb: 2 }}>
          {direction === 'FROM'
            ? `Showing entities that this ${entityType.toLowerCase()} is related TO`
            : `Showing entities that are related FROM this ${entityType.toLowerCase()}`}
        </Alert>

        {/* Relations Table */}
        {displayRelations.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <RelationIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
            <Typography color="text.secondary">
              No {direction === 'FROM' ? 'outbound' : 'inbound'} relations found
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
              Relations define connections between entities in the system
            </Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Relation Type</TableCell>
                  <TableCell>{direction === 'FROM' ? 'To Entity' : 'From Entity'}</TableCell>
                  <TableCell>Entity Type</TableCell>
                  {!readOnly && <TableCell align="right">Actions</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {displayRelations.map((relation, index) => {
                  const targetEntity = direction === 'FROM' ? relation.to : relation.from
                  return (
                    <TableRow key={index} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <ArrowIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
                          <strong>{relation.type}</strong>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {getEntityLabel(targetEntity.id, targetEntity.entityType)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={targetEntity.entityType}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                      </TableCell>
                      {!readOnly && (
                        <TableCell align="right">
                          <Tooltip title="Delete">
                            <IconButton size="small" onClick={() => handleDelete(relation)}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      )}
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}

        {/* Stats */}
        <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
          <Chip
            label={`${relations.length} total relations`}
            size="small"
            color="primary"
          />
          <Chip
            label={`${outboundRelations.length} outbound`}
            size="small"
            color="info"
            variant="outlined"
          />
          <Chip
            label={`${inboundRelations.length} inbound`}
            size="small"
            color="success"
            variant="outlined"
          />
        </Box>
      </Paper>

      {/* Add Relation Dialog */}
      <Dialog
        open={openDialog}
        onClose={() => setOpenDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Add Relation</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            <Alert severity="info">
              Creating {direction === 'FROM' ? 'outbound' : 'inbound'} relation
            </Alert>

            <Autocomplete
              freeSolo
              options={COMMON_RELATION_TYPES}
              value={formData.relationType}
              onChange={(_event, newValue) => {
                setFormData({ ...formData, relationType: newValue || '' })
              }}
              onInputChange={(_event, newValue) => {
                setFormData({ ...formData, relationType: newValue })
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Relation Type"
                  required
                  helperText="Select common type or enter custom"
                />
              )}
            />

            <FormControl fullWidth>
              <InputLabel>Target Entity Type</InputLabel>
              <Select
                value={formData.targetEntityType}
                label="Target Entity Type"
                onChange={(e) => setFormData({ ...formData, targetEntityType: e.target.value })}
              >
                {ENTITY_TYPES.map((type) => (
                  <MenuItem key={type} value={type}>
                    {type}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Target Entity ID"
              required
              fullWidth
              value={formData.targetEntityId}
              onChange={(e) => setFormData({ ...formData, targetEntityId: e.target.value })}
              helperText="Enter the ID of the entity to relate to"
            />

            <TextField
              label="Target Entity Name (Optional)"
              fullWidth
              value={formData.targetEntityName}
              onChange={(e) => setFormData({ ...formData, targetEntityName: e.target.value })}
              helperText="For display purposes only"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={!formData.targetEntityId || !formData.relationType}
            sx={{ bgcolor: '#0F3E5C' }}
          >
            Add Relation
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
