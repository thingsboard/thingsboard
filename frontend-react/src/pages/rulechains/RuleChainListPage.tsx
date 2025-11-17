/**
 * Rule Chain List Page
 * Displays and manages all rule chains
 */

import React, { useState } from 'react'
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
  Chip,
  TextField,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControlLabel,
  Switch,
  Tooltip,
  Menu,
  MenuItem,
} from '@mui/material'
import {
  Add,
  Search,
  Edit,
  Delete,
  PlayArrow,
  ContentCopy,
  MoreVert,
  Star,
  StarBorder,
  BugReport,
  FileUpload,
  FileDownload,
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { RuleChain, RuleChainType } from '../../types/rulechain.types'
import { format } from 'date-fns'

export default function RuleChainListPage() {
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = useState('')
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [newRuleChainName, setNewRuleChainName] = useState('')
  const [newRuleChainDebugMode, setNewRuleChainDebugMode] = useState(false)
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [selectedRuleChain, setSelectedRuleChain] = useState<string | null>(null)

  // Mock data - will be replaced with API calls
  const [ruleChains, setRuleChains] = useState<RuleChain[]>([
    {
      id: { id: '1', entityType: 'RULE_CHAIN' },
      name: 'Root Rule Chain',
      root: true,
      debugMode: false,
      type: RuleChainType.CORE,
      createdTime: Date.now() - 86400000 * 7,
      additionalInfo: { description: 'Main processing rule chain' },
    },
    {
      id: { id: '2', entityType: 'RULE_CHAIN' },
      name: 'Temperature Alerts',
      root: false,
      debugMode: true,
      type: RuleChainType.CORE,
      createdTime: Date.now() - 86400000 * 3,
      additionalInfo: { description: 'Process temperature sensor data and create alerts' },
    },
    {
      id: { id: '3', entityType: 'RULE_CHAIN' },
      name: 'Device Telemetry Processing',
      root: false,
      debugMode: false,
      type: RuleChainType.CORE,
      createdTime: Date.now() - 86400000,
      additionalInfo: { description: 'Transform and save device telemetry' },
    },
  ])

  const filteredRuleChains = ruleChains.filter((rc) =>
    rc.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const handleCreateRuleChain = () => {
    const newRuleChain: RuleChain = {
      id: { id: String(Date.now()), entityType: 'RULE_CHAIN' },
      name: newRuleChainName,
      root: false,
      debugMode: newRuleChainDebugMode,
      type: RuleChainType.CORE,
      createdTime: Date.now(),
      additionalInfo: { description: '' },
    }

    setRuleChains([...ruleChains, newRuleChain])
    setCreateDialogOpen(false)
    setNewRuleChainName('')
    setNewRuleChainDebugMode(false)

    // Navigate to editor
    navigate(`/rulechains/${newRuleChain.id.id}`)
  }

  const handleEditRuleChain = (id: string) => {
    navigate(`/rulechains/${id}`)
  }

  const handleDeleteRuleChain = (id: string) => {
    setRuleChains(ruleChains.filter((rc) => rc.id.id !== id))
    handleCloseMenu()
  }

  const handleDuplicateRuleChain = (id: string) => {
    const original = ruleChains.find((rc) => rc.id.id === id)
    if (original) {
      const duplicate: RuleChain = {
        ...original,
        id: { id: String(Date.now()), entityType: 'RULE_CHAIN' },
        name: `${original.name} (Copy)`,
        root: false,
        createdTime: Date.now(),
      }
      setRuleChains([...ruleChains, duplicate])
    }
    handleCloseMenu()
  }

  const handleSetAsRoot = (id: string) => {
    setRuleChains(
      ruleChains.map((rc) => ({
        ...rc,
        root: rc.id.id === id,
      }))
    )
    handleCloseMenu()
  }

  const handleOpenMenu = (event: React.MouseEvent<HTMLElement>, ruleChainId: string) => {
    setAnchorEl(event.currentTarget)
    setSelectedRuleChain(ruleChainId)
  }

  const handleCloseMenu = () => {
    setAnchorEl(null)
    setSelectedRuleChain(null)
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box>
          <Typography variant="h4" gutterBottom>
            Rule Chains
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Build and manage automation workflows for your IoT devices
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button startIcon={<FileUpload />} variant="outlined">
            Import
          </Button>
          <Button startIcon={<Add />} variant="contained" onClick={() => setCreateDialogOpen(true)}>
            Create Rule Chain
          </Button>
        </Box>
      </Box>

      {/* Search and Filters */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <TextField
            placeholder="Search rule chains..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            size="small"
            sx={{ flex: 1 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search />
                </InputAdornment>
              ),
            }}
          />
          <Chip label={`${filteredRuleChains.length} rule chains`} />
        </Box>
      </Paper>

      {/* Rule Chains Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell width={40}></TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Created</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredRuleChains.map((ruleChain) => (
              <TableRow
                key={ruleChain.id.id}
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => handleEditRuleChain(ruleChain.id.id)}
              >
                <TableCell>
                  {ruleChain.root ? (
                    <Tooltip title="Root Rule Chain">
                      <Star sx={{ color: '#FFB300' }} />
                    </Tooltip>
                  ) : (
                    <StarBorder sx={{ color: 'text.disabled' }} />
                  )}
                </TableCell>
                <TableCell>
                  <Box>
                    <Typography variant="body1" fontWeight={500}>
                      {ruleChain.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      ID: {ruleChain.id.id.substring(0, 8)}...
                    </Typography>
                  </Box>
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="text.secondary">
                    {ruleChain.additionalInfo?.description || 'No description'}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Chip label={ruleChain.type} size="small" color="primary" variant="outlined" />
                </TableCell>
                <TableCell>
                  {ruleChain.debugMode ? (
                    <Chip
                      icon={<BugReport />}
                      label="Debug"
                      size="small"
                      color="warning"
                      sx={{ fontWeight: 600 }}
                    />
                  ) : (
                    <Chip label="Active" size="small" color="success" variant="outlined" />
                  )}
                </TableCell>
                <TableCell>
                  <Typography variant="body2">
                    {format(ruleChain.createdTime!, 'MMM d, yyyy')}
                  </Typography>
                </TableCell>
                <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                  <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
                    <Tooltip title="Edit">
                      <IconButton size="small" onClick={() => handleEditRuleChain(ruleChain.id.id)}>
                        <Edit fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Test">
                      <IconButton size="small" color="success">
                        <PlayArrow fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Export">
                      <IconButton size="small">
                        <FileDownload fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <IconButton
                      size="small"
                      onClick={(e) => handleOpenMenu(e, ruleChain.id.id)}
                    >
                      <MoreVert fontSize="small" />
                    </IconButton>
                  </Box>
                </TableCell>
              </TableRow>
            ))}
            {filteredRuleChains.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 8 }}>
                  <Typography variant="body1" color="text.secondary">
                    No rule chains found
                  </Typography>
                  <Button
                    startIcon={<Add />}
                    variant="outlined"
                    sx={{ mt: 2 }}
                    onClick={() => setCreateDialogOpen(true)}
                  >
                    Create Your First Rule Chain
                  </Button>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Create Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Rule Chain</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
            <TextField
              label="Rule Chain Name"
              value={newRuleChainName}
              onChange={(e) => setNewRuleChainName(e.target.value)}
              fullWidth
              autoFocus
              placeholder="e.g., Temperature Processing"
            />
            <FormControlLabel
              control={
                <Switch
                  checked={newRuleChainDebugMode}
                  onChange={(e) => setNewRuleChainDebugMode(e.target.checked)}
                />
              }
              label={
                <Box>
                  <Typography variant="body2">Enable Debug Mode</Typography>
                  <Typography variant="caption" color="text.secondary">
                    Log detailed processing information
                  </Typography>
                </Box>
              }
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleCreateRuleChain}
            variant="contained"
            disabled={!newRuleChainName.trim()}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* Context Menu */}
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleCloseMenu}>
        <MenuItem
          onClick={() => selectedRuleChain && handleSetAsRoot(selectedRuleChain)}
          disabled={
            ruleChains.find((rc) => rc.id.id === selectedRuleChain)?.root
          }
        >
          <Star sx={{ mr: 1 }} fontSize="small" />
          Set as Root
        </MenuItem>
        <MenuItem onClick={() => selectedRuleChain && handleDuplicateRuleChain(selectedRuleChain)}>
          <ContentCopy sx={{ mr: 1 }} fontSize="small" />
          Duplicate
        </MenuItem>
        <MenuItem
          onClick={() => selectedRuleChain && handleDeleteRuleChain(selectedRuleChain)}
          disabled={ruleChains.find((rc) => rc.id.id === selectedRuleChain)?.root}
          sx={{ color: 'error.main' }}
        >
          <Delete sx={{ mr: 1 }} fontSize="small" />
          Delete
        </MenuItem>
      </Menu>
    </Box>
  )
}
