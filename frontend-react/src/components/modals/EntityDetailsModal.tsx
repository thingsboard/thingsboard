/**
 * Entity Details Modal - Base Component
 * Matches ThingsBoard's entity detail modal exactly
 * Reusable for Devices, Assets, Customers, Gateways, etc.
 */

import { useState, ReactNode } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  IconButton,
  Button,
  Tabs,
  Tab,
  Typography,
  Divider,
} from '@mui/material'
import {
  Close as CloseIcon,
  Delete as DeleteIcon,
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

interface EntityDetailsModalProps {
  open: boolean
  onClose: () => void
  title: string
  subtitle?: string
  icon?: ReactNode
  tabs: EntityTab[]
  onSave?: () => void
  onDelete?: () => void
  additionalActions?: ReactNode[]
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  fullWidth?: boolean
  loading?: boolean
}

export default function EntityDetailsModal({
  open,
  onClose,
  title,
  subtitle,
  icon,
  tabs,
  onSave,
  onDelete,
  additionalActions = [],
  maxWidth = 'lg',
  fullWidth = true,
  loading = false,
}: EntityDetailsModalProps) {
  const [activeTab, setActiveTab] = useState(0)

  const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue)
  }

  const handleSave = () => {
    if (onSave) {
      onSave()
    }
  }

  const handleDelete = () => {
    if (onDelete && confirm('Are you sure you want to delete this entity?')) {
      onDelete()
    }
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth={maxWidth}
      fullWidth={fullWidth}
      PaperProps={{
        sx: {
          minHeight: '80vh',
          maxHeight: '90vh',
        },
      }}
    >
      {/* Header */}
      <DialogTitle
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          bgcolor: '#F5F5F5',
          borderBottom: '1px solid #E0E0E0',
          py: 2,
          px: 3,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {icon && (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 48,
                height: 48,
                borderRadius: 1,
                bgcolor: '#0F3E5C',
                color: 'white',
              }}
            >
              {icon}
            </Box>
          )}
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
              {title}
            </Typography>
            {subtitle && (
              <Typography variant="body2" color="textSecondary">
                {subtitle}
              </Typography>
            )}
          </Box>
        </Box>
        <IconButton onClick={onClose} size="small">
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: '#FAFAFA' }}>
        <Tabs
          value={activeTab}
          onChange={handleTabChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{
            px: 2,
            '& .MuiTab-root': {
              textTransform: 'none',
              fontWeight: 500,
              fontSize: '0.875rem',
              minHeight: 48,
            },
            '& .Mui-selected': {
              color: '#0F3E5C',
            },
            '& .MuiTabs-indicator': {
              backgroundColor: '#0F3E5C',
            },
          }}
        >
          {tabs.map((tab, index) => (
            <Tab
              key={index}
              label={tab.label}
              disabled={tab.disabled}
              id={`entity-tab-${index}`}
              aria-controls={`entity-tabpanel-${index}`}
            />
          ))}
        </Tabs>
      </Box>

      {/* Tab Content */}
      <DialogContent sx={{ p: 0, overflow: 'auto' }}>
        {tabs.map((tab, index) => (
          <TabPanel key={index} value={activeTab} index={index}>
            {tab.content}
          </TabPanel>
        ))}
      </DialogContent>

      {/* Footer Actions */}
      <DialogActions
        sx={{
          borderTop: '1px solid #E0E0E0',
          bgcolor: '#FAFAFA',
          px: 3,
          py: 2,
          gap: 1,
        }}
      >
        {/* Left side actions */}
        <Box sx={{ flex: 1, display: 'flex', gap: 1 }}>
          {onDelete && (
            <Button
              variant="outlined"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={handleDelete}
              disabled={loading}
            >
              Delete
            </Button>
          )}
          {additionalActions.map((action, index) => (
            <Box key={index}>{action}</Box>
          ))}
        </Box>

        {/* Right side actions */}
        <Button onClick={onClose} disabled={loading}>
          Cancel
        </Button>
        {onSave && (
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={loading}
            sx={{
              bgcolor: '#0F3E5C',
              '&:hover': {
                bgcolor: '#0A2A3F',
              },
            }}
          >
            {loading ? 'Saving...' : 'Save'}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}

/**
 * Status Badge Component (used in entity modals)
 */
interface StatusBadgeProps {
  active: boolean
  label?: string
}

export function StatusBadge({ active, label }: StatusBadgeProps) {
  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.5,
        px: 1.5,
        py: 0.5,
        borderRadius: 1,
        bgcolor: active ? '#E8F5E9' : '#FFEBEE',
        color: active ? '#2E7D6F' : '#C62828',
        fontSize: '0.875rem',
        fontWeight: 500,
      }}
    >
      {active ? <ActiveIcon fontSize="small" /> : <InactiveIcon fontSize="small" />}
      {label || (active ? 'Active' : 'Inactive')}
    </Box>
  )
}

/**
 * Info Row Component (used in Details tab)
 */
interface InfoRowProps {
  label: string
  value: ReactNode
  copyable?: boolean
}

export function InfoRow({ label, value, copyable = false }: InfoRowProps) {
  const handleCopy = () => {
    if (typeof value === 'string') {
      navigator.clipboard.writeText(value)
    }
  }

  return (
    <Box
      sx={{
        display: 'flex',
        py: 1.5,
        borderBottom: '1px solid #F5F5F5',
        '&:last-child': {
          borderBottom: 'none',
        },
      }}
    >
      <Typography
        sx={{
          width: '30%',
          color: '#757575',
          fontSize: '0.875rem',
          fontWeight: 500,
        }}
      >
        {label}
      </Typography>
      <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
        <Typography sx={{ fontSize: '0.875rem', color: '#212121' }}>{value}</Typography>
        {copyable && (
          <IconButton size="small" onClick={handleCopy}>
            <CopyIcon fontSize="small" />
          </IconButton>
        )}
      </Box>
    </Box>
  )
}

/**
 * Section Header Component (used to divide tabs into sections)
 */
interface SectionHeaderProps {
  title: string
  action?: ReactNode
}

export function SectionHeader({ title, action }: SectionHeaderProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        mb: 2,
        pb: 1,
        borderBottom: '2px solid #0F3E5C',
      }}
    >
      <Typography variant="h6" sx={{ fontWeight: 600, color: '#0F3E5C', fontSize: '1rem' }}>
        {title}
      </Typography>
      {action}
    </Box>
  )
}
