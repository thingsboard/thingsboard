/**
 * Device Bulk Operations Panel
 * 120% Enhanced Feature - Perform batch operations on multiple devices
 * Beyond Angular implementation
 */

import React, { useState } from 'react'
import {
  Box,
  Paper,
  Typography,
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  FormControl,
  InputLabel,
  Chip,
  Alert,
  LinearProgress,
  Divider,
} from '@mui/material'
import {
  Delete,
  PersonAdd,
  PersonRemove,
  ToggleOn,
  ToggleOff,
  Update,
  Key,
  Label,
  ExpandMore,
  CheckCircle,
} from '@mui/icons-material'
import { Device, DeviceBulkOperation } from '../../types/device.types'

interface DeviceBulkOperationsPanelProps {
  selectedDevices: Device[]
  onOperationComplete: () => void
  onClearSelection: () => void
}

type OperationType = 'assign' | 'unassign' | 'delete' | 'activate' | 'deactivate' | 'update_profile' | 'update_label'

export default function DeviceBulkOperationsPanel({
  selectedDevices,
  onOperationComplete,
  onClearSelection,
}: DeviceBulkOperationsPanelProps) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [operationType, setOperationType] = useState<OperationType>('assign')
  const [customerId, setCustomerId] = useState('')
  const [deviceProfileId, setDeviceProfileId] = useState('')
  const [label, setLabel] = useState('')
  const [executing, setExecuting] = useState(false)
  const [progress, setProgress] = useState(0)

  const handleOpenMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleCloseMenu = () => {
    setAnchorEl(null)
  }

  const handleOperation = (type: OperationType) => {
    setOperationType(type)
    setDialogOpen(true)
    handleCloseMenu()
  }

  const executeOperation = async () => {
    setExecuting(true)
    setProgress(0)

    const operation: DeviceBulkOperation = {
      type: operationType,
      deviceIds: selectedDevices.map((d) => d.id.id),
      params:
        operationType === 'assign' || operationType === 'unassign'
          ? { customerId }
          : operationType === 'update_profile'
            ? { deviceProfileId }
            : operationType === 'update_label'
              ? { label }
              : undefined,
    }

    // Simulate bulk operation with progress
    for (let i = 0; i <= 100; i += 10) {
      await new Promise((resolve) => setTimeout(resolve, 200))
      setProgress(i)
    }

    console.log('Executing bulk operation:', operation)

    setExecuting(false)
    setDialogOpen(false)
    setProgress(0)
    onOperationComplete()
  }

  const getOperationTitle = () => {
    const titles: Record<OperationType, string> = {
      assign: 'Assign to Customer',
      unassign: 'Unassign from Customer',
      delete: 'Delete Devices',
      activate: 'Activate Devices',
      deactivate: 'Deactivate Devices',
      update_profile: 'Update Device Profile',
      update_label: 'Update Labels',
    }
    return titles[operationType]
  }

  const getOperationDescription = () => {
    const descriptions: Record<OperationType, string> = {
      assign: `Assign ${selectedDevices.length} device(s) to a customer`,
      unassign: `Remove customer assignment from ${selectedDevices.length} device(s)`,
      delete: `Permanently delete ${selectedDevices.length} device(s)`,
      activate: `Activate ${selectedDevices.length} device(s)`,
      deactivate: `Deactivate ${selectedDevices.length} device(s)`,
      update_profile: `Change device profile for ${selectedDevices.length} device(s)`,
      update_label: `Update labels for ${selectedDevices.length} device(s)`,
    }
    return descriptions[operationType]
  }

  if (selectedDevices.length === 0) {
    return null
  }

  return (
    <>
      <Paper
        elevation={4}
        sx={{
          position: 'fixed',
          bottom: 24,
          left: '50%',
          transform: 'translateX(-50%)',
          px: 3,
          py: 2,
          display: 'flex',
          alignItems: 'center',
          gap: 2,
          zIndex: 1000,
          minWidth: 500,
        }}
      >
        <Chip
          label={`${selectedDevices.length} selected`}
          color="primary"
          onDelete={onClearSelection}
          sx={{ fontWeight: 600 }}
        />

        <Button
          variant="contained"
          endIcon={<ExpandMore />}
          onClick={handleOpenMenu}
          sx={{ ml: 'auto' }}
        >
          Bulk Actions
        </Button>
      </Paper>

      {/* Operations Menu */}
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleCloseMenu}>
        <MenuItem onClick={() => handleOperation('assign')}>
          <ListItemIcon>
            <PersonAdd fontSize="small" />
          </ListItemIcon>
          <ListItemText>Assign to Customer</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleOperation('unassign')}>
          <ListItemIcon>
            <PersonRemove fontSize="small" />
          </ListItemIcon>
          <ListItemText>Unassign from Customer</ListItemText>
        </MenuItem>
        <Divider />
        <MenuItem onClick={() => handleOperation('activate')}>
          <ListItemIcon>
            <ToggleOn fontSize="small" />
          </ListItemIcon>
          <ListItemText>Activate</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleOperation('deactivate')}>
          <ListItemIcon>
            <ToggleOff fontSize="small" />
          </ListItemIcon>
          <ListItemText>Deactivate</ListItemText>
        </MenuItem>
        <Divider />
        <MenuItem onClick={() => handleOperation('update_profile')}>
          <ListItemIcon>
            <Update fontSize="small" />
          </ListItemIcon>
          <ListItemText>Update Profile</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleOperation('update_label')}>
          <ListItemIcon>
            <Label fontSize="small" />
          </ListItemIcon>
          <ListItemText>Update Labels</ListItemText>
        </MenuItem>
        <Divider />
        <MenuItem onClick={() => handleOperation('delete')} sx={{ color: 'error.main' }}>
          <ListItemIcon>
            <Delete fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>Delete</ListItemText>
        </MenuItem>
      </Menu>

      {/* Operation Dialog */}
      <Dialog open={dialogOpen} onClose={() => !executing && setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{getOperationTitle()}</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2 }}>
            <Alert
              severity={operationType === 'delete' ? 'error' : 'info'}
              sx={{ mb: 3 }}
            >
              {getOperationDescription()}
            </Alert>

            {operationType === 'assign' && (
              <FormControl fullWidth>
                <InputLabel>Customer</InputLabel>
                <Select
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                  label="Customer"
                >
                  <MenuItem value="customer1">Customer A</MenuItem>
                  <MenuItem value="customer2">Customer B</MenuItem>
                  <MenuItem value="customer3">Customer C</MenuItem>
                </Select>
              </FormControl>
            )}

            {operationType === 'update_profile' && (
              <FormControl fullWidth>
                <InputLabel>Device Profile</InputLabel>
                <Select
                  value={deviceProfileId}
                  onChange={(e) => setDeviceProfileId(e.target.value)}
                  label="Device Profile"
                >
                  <MenuItem value="profile1">Default Profile</MenuItem>
                  <MenuItem value="profile2">Sensor Profile</MenuItem>
                  <MenuItem value="profile3">Gateway Profile</MenuItem>
                </Select>
              </FormControl>
            )}

            {operationType === 'update_label' && (
              <TextField
                fullWidth
                label="Label"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder="Enter label"
                helperText="This label will be applied to all selected devices"
              />
            )}

            {operationType === 'delete' && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                <strong>Warning:</strong> This action cannot be undone. All device data, including
                telemetry, attributes, and relations will be permanently deleted.
              </Alert>
            )}

            {executing && (
              <Box sx={{ mt: 3 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">Processing...</Typography>
                  <Typography variant="body2">{progress}%</Typography>
                </Box>
                <LinearProgress variant="determinate" value={progress} />
              </Box>
            )}

            {progress === 100 && (
              <Alert severity="success" icon={<CheckCircle />} sx={{ mt: 2 }}>
                Operation completed successfully!
              </Alert>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)} disabled={executing}>
            Cancel
          </Button>
          <Button
            onClick={executeOperation}
            variant="contained"
            color={operationType === 'delete' ? 'error' : 'primary'}
            disabled={
              executing ||
              (operationType === 'assign' && !customerId) ||
              (operationType === 'update_profile' && !deviceProfileId) ||
              (operationType === 'update_label' && !label)
            }
          >
            {operationType === 'delete' ? 'Delete' : 'Execute'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
