/**
 * Rule Chains Page
 * Rule chain management with import/export and root designation
 * Matches ThingsBoard ui-ngx rule chains functionality
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
  Typography,
  Menu,
  MenuItem,
} from '@mui/material'
import {
  Visibility,
  Edit,
  Star,
  StarBorder,
  FileDownload,
  FileUpload,
  MoreVert,
  ContentCopy,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'
import EntityTable, { EntityColumn } from '@/components/entity/EntityTable'

interface RuleChain {
  id: string
  name: string
  type: 'CORE' | 'EDGE'
  root: boolean
  debugMode: boolean
  configuration?: any
  firstRuleNodeId?: string
  createdTime: number
  additionalInfo?: any
}

export default function RuleChainsPage() {
  const navigate = useNavigate()
  const [ruleChains, setRuleChains] = useState<RuleChain[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')

  // Dialog states
  const [openDialog, setOpenDialog] = useState(false)
  const [editingRuleChain, setEditingRuleChain] = useState<RuleChain | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    debugMode: false,
  })

  // Menu state
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [menuRuleChain, setMenuRuleChain] = useState<RuleChain | null>(null)

  useEffect(() => {
    loadRuleChains()
  }, [page, pageSize, searchQuery])

  const loadRuleChains = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockRuleChains: RuleChain[] = [
      {
        id: '1',
        name: 'Root Rule Chain',
        type: 'CORE',
        root: true,
        debugMode: false,
        firstRuleNodeId: 'node-1',
        createdTime: Date.now() - 86400000 * 365,
        configuration: {
          nodes: [],
          connections: [],
        },
      },
      {
        id: '2',
        name: 'Thermostat Rule Chain',
        type: 'CORE',
        root: false,
        debugMode: true,
        firstRuleNodeId: 'node-2',
        createdTime: Date.now() - 86400000 * 180,
      },
      {
        id: '3',
        name: 'Alarm Handling',
        type: 'CORE',
        root: false,
        debugMode: false,
        firstRuleNodeId: 'node-3',
        createdTime: Date.now() - 86400000 * 120,
      },
      {
        id: '4',
        name: 'Data Processing',
        type: 'CORE',
        root: false,
        debugMode: false,
        firstRuleNodeId: 'node-4',
        createdTime: Date.now() - 86400000 * 90,
      },
      {
        id: '5',
        name: 'Edge Processing',
        type: 'EDGE',
        root: false,
        debugMode: false,
        firstRuleNodeId: 'node-5',
        createdTime: Date.now() - 86400000 * 60,
      },
    ]

    // Filter by search query
    const filtered = searchQuery
      ? mockRuleChains.filter((rc) =>
          rc.name.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : mockRuleChains

    setRuleChains(filtered.slice(page * pageSize, (page + 1) * pageSize))
    setTotalElements(filtered.length)
    setLoading(false)
  }

  const columns: EntityColumn[] = [
    {
      id: 'name',
      label: 'Name',
      minWidth: 250,
      format: (value, row: RuleChain) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {row.root ? (
            <Star sx={{ fontSize: '1.2rem', color: '#FFB300' }} />
          ) : (
            <StarBorder sx={{ fontSize: '1.2rem', color: '#8C959D' }} />
          )}
          <Box>
            <Box
              sx={{ fontWeight: 600, color: '#0F3E5C', cursor: 'pointer' }}
              onClick={() => navigate(`/rule-chains/${row.id}`)}
            >
              {value}
            </Box>
            {row.root && (
              <Box sx={{ fontSize: '0.75rem', color: '#8C959D', mt: 0.5 }}>
                Root Rule Chain
              </Box>
            )}
          </Box>
        </Box>
      ),
    },
    {
      id: 'type',
      label: 'Type',
      minWidth: 100,
      format: (value: RuleChain['type']) => (
        <Chip
          label={value}
          size="small"
          sx={{
            bgcolor: value === 'CORE' ? '#E3F2FD' : '#FFF3E0',
            color: value === 'CORE' ? '#0F3E5C' : '#E65100',
            fontWeight: 500,
          }}
        />
      ),
    },
    {
      id: 'debugMode',
      label: 'Debug Mode',
      minWidth: 120,
      format: (value: boolean) =>
        value ? (
          <Chip
            label="Enabled"
            size="small"
            sx={{ bgcolor: '#FFB300', color: 'white', fontWeight: 500 }}
          />
        ) : (
          <Chip
            label="Disabled"
            size="small"
            variant="outlined"
            sx={{ color: '#8C959D', borderColor: '#8C959D' }}
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
    setEditingRuleChain(null)
    setFormData({
      name: '',
      debugMode: false,
    })
    setOpenDialog(true)
  }

  const handleEdit = (ruleChain: RuleChain) => {
    setEditingRuleChain(ruleChain)
    setFormData({
      name: ruleChain.name,
      debugMode: ruleChain.debugMode,
    })
    setOpenDialog(true)
  }

  const handleDelete = async (ids: string[]) => {
    // Check if any selected rule chain is root
    const hasRoot = ruleChains.some((rc) => ids.includes(rc.id) && rc.root)
    if (hasRoot) {
      alert('Cannot delete root rule chain. Please set another rule chain as root first.')
      return
    }

    if (confirm(`Delete ${ids.length} rule chain(s)?`)) {
      // API call would go here
      console.log('Deleting rule chains:', ids)
      loadRuleChains()
      setSelectedIds([])
    }
  }

  const handleSave = async () => {
    // API call would go here
    console.log('Saving rule chain:', formData)
    setOpenDialog(false)
    loadRuleChains()
  }

  const handleSetAsRoot = async (ruleChain: RuleChain) => {
    if (
      confirm(
        `Set "${ruleChain.name}" as root rule chain? The current root rule chain will become a regular rule chain.`
      )
    ) {
      // API call would go here
      console.log('Setting as root:', ruleChain.id)
      loadRuleChains()
    }
    handleCloseMenu()
  }

  const handleExport = (ruleChain: RuleChain) => {
    // Create mock export data
    const exportData = {
      ruleChain: {
        id: ruleChain.id,
        name: ruleChain.name,
        type: ruleChain.type,
        firstRuleNodeId: ruleChain.firstRuleNodeId,
        debugMode: ruleChain.debugMode,
        configuration: ruleChain.configuration || { nodes: [], connections: [] },
      },
      metadata: {
        exportTime: new Date().toISOString(),
        version: '1.0',
      },
    }

    // Create and download file
    const blob = new Blob([JSON.stringify(exportData, null, 2)], {
      type: 'application/json',
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${ruleChain.name.replace(/\s+/g, '_').toLowerCase()}_rule_chain.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)

    handleCloseMenu()
  }

  const handleImport = () => {
    // Create file input
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.json'
    input.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0]
      if (file) {
        const reader = new FileReader()
        reader.onload = (event) => {
          try {
            const data = JSON.parse(event.target?.result as string)
            console.log('Importing rule chain:', data)
            // API call would go here
            loadRuleChains()
          } catch (error) {
            alert('Invalid rule chain file')
          }
        }
        reader.readAsText(file)
      }
    }
    input.click()
  }

  const handleMakeCopy = (ruleChain: RuleChain) => {
    // API call would go here
    console.log('Making copy of:', ruleChain.id)
    loadRuleChains()
    handleCloseMenu()
  }

  const handleOpenMenu = (event: React.MouseEvent<HTMLElement>, ruleChain: RuleChain) => {
    setAnchorEl(event.currentTarget)
    setMenuRuleChain(ruleChain)
  }

  const handleCloseMenu = () => {
    setAnchorEl(null)
    setMenuRuleChain(null)
  }

  const rowActions = (row: RuleChain) => (
    <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
      <Tooltip title="Open rule chain">
        <IconButton size="small" onClick={() => navigate(`/rule-chains/${row.id}`)}>
          <Edit fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="More actions">
        <IconButton size="small" onClick={(e) => handleOpenMenu(e, row)}>
          <MoreVert fontSize="small" />
        </IconButton>
      </Tooltip>
    </Box>
  )

  const customActions = (
    <>
      <Tooltip title="Import rule chain">
        <IconButton onClick={handleImport}>
          <FileUpload />
        </IconButton>
      </Tooltip>
    </>
  )

  return (
    <MainLayout>
      <Box>
        <EntityTable
          title="Rule Chains"
          columns={columns}
          rows={ruleChains}
          totalElements={totalElements}
          loading={loading}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onRefresh={loadRuleChains}
          onSearch={setSearchQuery}
          onPageChange={(newPage, newPageSize) => {
            setPage(newPage)
            setPageSize(newPageSize)
          }}
          page={page}
          pageSize={pageSize}
          rowActions={rowActions}
          customActions={customActions}
        />

        {/* Add/Edit Dialog */}
        <Dialog
          open={openDialog}
          onClose={() => setOpenDialog(false)}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>
            {editingRuleChain ? 'Edit Rule Chain' : 'Add Rule Chain'}
          </DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Name"
                required
                fullWidth
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                helperText="Rule chain name"
              />

              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.debugMode}
                    onChange={(e) =>
                      setFormData({ ...formData, debugMode: e.target.checked })
                    }
                  />
                }
                label="Enable debug mode"
              />

              {!editingRuleChain && (
                <Typography variant="body2" color="textSecondary">
                  After creating the rule chain, you can open it to add and configure nodes.
                </Typography>
              )}
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
              {editingRuleChain ? 'Save' : 'Add'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Context Menu */}
        <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleCloseMenu}>
          {menuRuleChain && !menuRuleChain.root && (
            <MenuItem onClick={() => handleSetAsRoot(menuRuleChain)}>
              <Star sx={{ mr: 1, fontSize: '1.2rem' }} />
              Set as Root
            </MenuItem>
          )}
          <MenuItem onClick={() => menuRuleChain && handleExport(menuRuleChain)}>
            <FileDownload sx={{ mr: 1, fontSize: '1.2rem' }} />
            Export
          </MenuItem>
          <MenuItem onClick={() => menuRuleChain && handleMakeCopy(menuRuleChain)}>
            <ContentCopy sx={{ mr: 1, fontSize: '1.2rem' }} />
            Make a Copy
          </MenuItem>
        </Menu>
      </Box>
    </MainLayout>
  )
}
