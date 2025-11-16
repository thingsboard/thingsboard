/**
 * Entity Drawer - Base Component
 * Exactly matches ThingsBoard's right-side slide-in drawer pattern
 * Used for viewing/editing all entities (Devices, Assets, Customers, etc.)
 */

import { useState, ReactNode } from 'react'
import {
  Drawer,
  Box,
  IconButton,
  Button,
  Tabs,
  Tab,
  Typography,
  Divider,
  AppBar,
  Toolbar,
  Stack,
} from '@mui/material'
import {
  Close as CloseIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  ContentCopy as CopyIcon,
  CheckCircle as ActiveIcon,
  Cancel as InactiveIcon,
} from '@mui/icons-material'

interface TabPanelProps {
  children?: ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`entity-tabpanel-${index}`}
      aria-labelledby={`entity-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

interface EntityTab {
  label: string
  content: ReactNode
  disabled?: boolean
}

interface EntityDrawerProps {
  open: boolean
  onClose: () => void
  title: string
  subtitle?: string
  icon?: ReactNode
  tabs: EntityTab[]
  mode?: 'view' | 'edit' | 'create'
  onModeChange?: (mode: 'view' | 'edit' | 'create') => void
  onSave?: () => void
  onDelete?: () => void
  onCopy?: () => void
  additionalActions?: ReactNode[]
  loading?: boolean
  width?: string | number
}

export default function EntityDrawer({
  open,
  onClose,
  title,
  subtitle,
  icon,
  tabs,
  mode = 'view',
  onModeChange,
  onSave,
  onDelete,
  onCopy,
  additionalActions = [],
  loading = false,
  width = '70%',
}: EntityDrawerProps) {
  const [activeTab, setActiveTab] = useState(0)

  const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue)
  }

  const handleEdit = () => {
    if (onModeChange) {
      onModeChange('edit')
    }
  }

  const handleCancelEdit = () => {
    if (onModeChange) {
      onModeChange('view')
    }
  }

  const handleSave = () => {
    if (onSave) {
      onSave()
      if (onModeChange) {
        onModeChange('view')
      }
    }
  }

  const handleDelete = () => {
    if (onDelete && confirm('Are you sure you want to delete this entity?')) {
      onDelete()
      onClose()
    }
  }

  const handleCopy = () => {
    if (onCopy) {
      onCopy()
    }
  }

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: {
          width: width,
          maxWidth: '100%',
        },
      }}
    >
      {/* Header AppBar */}
      <AppBar
        position="sticky"
        elevation={0}
        sx={{
          bgcolor: '#0F3E5C',
          borderBottom: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Toolbar>
          {/* Icon and Title */}
          <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1, gap: 1.5 }}>
            {icon && <Box sx={{ display: 'flex', color: 'white' }}>{icon}</Box>}
            <Box>
              <Typography variant="h6" sx={{ color: 'white', fontWeight: 600 }}>
                {title}
              </Typography>
              {subtitle && (
                <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.7)' }}>
                  {subtitle}
                </Typography>
              )}
            </Box>
          </Box>

          {/* Action Buttons */}
          <Stack direction="row" spacing={1} sx={{ mr: 1 }}>
            {/* View Mode Actions */}
            {mode === 'view' && (
              <>
                {onCopy && (
                  <IconButton color="inherit" onClick={handleCopy} title="Copy">
                    <CopyIcon />
                  </IconButton>
                )}
                {onModeChange && (
                  <Button
                    variant="contained"
                    size="small"
                    startIcon={<EditIcon />}
                    onClick={handleEdit}
                    sx={{ bgcolor: 'rgba(255,255,255,0.2)', '&:hover': { bgcolor: 'rgba(255,255,255,0.3)' } }}
                  >
                    Edit
                  </Button>
                )}
                {onDelete && (
                  <IconButton color="inherit" onClick={handleDelete} title="Delete">
                    <DeleteIcon />
                  </IconButton>
                )}
              </>
            )}

            {/* Edit/Create Mode Actions */}
            {(mode === 'edit' || mode === 'create') && (
              <>
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<CancelIcon />}
                  onClick={mode === 'edit' ? handleCancelEdit : onClose}
                  sx={{ color: 'white', borderColor: 'rgba(255,255,255,0.5)' }}
                >
                  Cancel
                </Button>
                <Button
                  variant="contained"
                  size="small"
                  startIcon={<SaveIcon />}
                  onClick={handleSave}
                  disabled={loading}
                  sx={{ bgcolor: '#4CAF50', '&:hover': { bgcolor: '#45a049' } }}
                >
                  {mode === 'create' ? 'Create' : 'Save'}
                </Button>
              </>
            )}

            {/* Additional Actions */}
            {additionalActions.map((action, index) => (
              <Box key={index}>{action}</Box>
            ))}
          </Stack>

          {/* Close Button */}
          <IconButton edge="end" color="inherit" onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Toolbar>

        {/* Tabs */}
        {tabs.length > 1 && (
          <Tabs
            value={activeTab}
            onChange={handleTabChange}
            sx={{
              bgcolor: '#0F3E5C',
              borderTop: '1px solid rgba(255,255,255,0.1)',
              '& .MuiTab-root': {
                color: 'rgba(255,255,255,0.7)',
                '&.Mui-selected': {
                  color: 'white',
                },
              },
              '& .MuiTabs-indicator': {
                backgroundColor: 'white',
              },
            }}
          >
            {tabs.map((tab, index) => (
              <Tab
                key={index}
                label={tab.label}
                disabled={tab.disabled || false}
                id={`entity-tab-${index}`}
                aria-controls={`entity-tabpanel-${index}`}
              />
            ))}
          </Tabs>
        )}
      </AppBar>

      {/* Content */}
      <Box sx={{ flexGrow: 1, overflow: 'auto', bgcolor: '#f5f5f5' }}>
        {tabs.map((tab, index) => (
          <TabPanel key={index} value={activeTab} index={index}>
            {tab.content}
          </TabPanel>
        ))}
      </Box>
    </Drawer>
  )
}

// Reusable components for drawer content
export function StatusBadge({ active }: { active: boolean }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
      {active ? (
        <>
          <ActiveIcon sx={{ fontSize: 16, color: 'success.main' }} />
          <Typography variant="body2" color="success.main">
            Active
          </Typography>
        </>
      ) : (
        <>
          <InactiveIcon sx={{ fontSize: 16, color: 'error.main' }} />
          <Typography variant="body2" color="error.main">
            Inactive
          </Typography>
        </>
      )}
    </Box>
  )
}

export function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <Box sx={{ mb: 2 }}>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
        {label}
      </Typography>
      <Typography variant="body1">{value || '-'}</Typography>
    </Box>
  )
}

export function SectionHeader({ title }: { title: string }) {
  return (
    <>
      <Typography variant="h6" sx={{ mb: 2, mt: 2, color: '#0F3E5C', fontWeight: 600 }}>
        {title}
      </Typography>
      <Divider sx={{ mb: 2 }} />
    </>
  )
}
