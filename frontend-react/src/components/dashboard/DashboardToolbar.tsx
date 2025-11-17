/**
 * Dashboard Toolbar Component
 * Provides controls for dashboard editing, settings, and operations
 * Matches ThingsBoard's toolbar functionality
 */

import { Box, Button, IconButton, Tooltip, Divider, Chip } from '@mui/material'
import {
  Edit as EditIcon,
  Save as SaveIcon,
  Close as CancelIcon,
  Add as AddIcon,
  Settings as SettingsIcon,
  Fullscreen as FullscreenIcon,
  GetApp as ExportIcon,
  Publish as ImportIcon,
  AccessTime as TimeIcon,
  FilterList as FilterIcon,
} from '@mui/icons-material'

interface DashboardToolbarProps {
  editMode: boolean
  onEditModeToggle: () => void
  onSave?: () => void
  onCancel?: () => void
  onAddWidget?: () => void
  onSettings?: () => void
  onFullscreen?: () => void
  onExport?: () => void
  onImport?: () => void
  onTimewindow?: () => void
  onFilters?: () => void
  hasChanges?: boolean
  dashboardTitle?: string
}

export default function DashboardToolbar({
  editMode,
  onEditModeToggle,
  onSave,
  onCancel,
  onAddWidget,
  onSettings,
  onFullscreen,
  onExport,
  onImport,
  onTimewindow,
  onFilters,
  hasChanges = false,
  dashboardTitle = 'Dashboard',
}: DashboardToolbarProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: 2,
        backgroundColor: (theme) =>
          theme.palette.mode === 'dark' ? '#1E1E1E' : '#FFFFFF',
        borderBottom: '1px solid',
        borderColor: (theme) =>
          theme.palette.mode === 'dark' ? '#333' : '#E0E0E0',
        boxShadow: 1,
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}
    >
      {/* Left Section: Title and Mode Indicator */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box sx={{ fontWeight: 'bold', fontSize: '18px', color: '#0F3E5C' }}>
          {dashboardTitle}
        </Box>
        {editMode && (
          <Chip
            label="EDIT MODE"
            color="warning"
            size="small"
            sx={{
              fontWeight: 'bold',
              animation: hasChanges ? 'pulse 2s infinite' : 'none',
              '@keyframes pulse': {
                '0%, 100%': { opacity: 1 },
                '50%': { opacity: 0.7 },
              },
            }}
          />
        )}
        {hasChanges && !editMode && (
          <Chip
            label="UNSAVED CHANGES"
            color="error"
            size="small"
            sx={{ fontWeight: 'bold' }}
          />
        )}
      </Box>

      {/* Right Section: Action Buttons */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        {editMode ? (
          // Edit Mode Controls
          <>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={onAddWidget}
              sx={{
                bgcolor: '#0F3E5C',
                '&:hover': { bgcolor: '#2E7D6F' },
              }}
            >
              Add Widget
            </Button>
            <Divider orientation="vertical" flexItem sx={{ mx: 1 }} />
            <Button
              variant="contained"
              color="success"
              startIcon={<SaveIcon />}
              onClick={onSave}
              disabled={!hasChanges}
            >
              Save
            </Button>
            <Button
              variant="outlined"
              color="error"
              startIcon={<CancelIcon />}
              onClick={onCancel}
            >
              Cancel
            </Button>
          </>
        ) : (
          // View Mode Controls
          <>
            <Tooltip title="Timewindow">
              <IconButton onClick={onTimewindow} color="primary">
                <TimeIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Filters">
              <IconButton onClick={onFilters} color="primary">
                <FilterIcon />
              </IconButton>
            </Tooltip>
            <Divider orientation="vertical" flexItem sx={{ mx: 1 }} />
            <Tooltip title="Export Dashboard">
              <IconButton onClick={onExport} color="primary">
                <ExportIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Import Dashboard">
              <IconButton onClick={onImport} color="primary">
                <ImportIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Dashboard Settings">
              <IconButton onClick={onSettings} color="primary">
                <SettingsIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Fullscreen">
              <IconButton onClick={onFullscreen} color="primary">
                <FullscreenIcon />
              </IconButton>
            </Tooltip>
            <Divider orientation="vertical" flexItem sx={{ mx: 1 }} />
            <Button
              variant="contained"
              startIcon={<EditIcon />}
              onClick={onEditModeToggle}
              sx={{
                bgcolor: '#0F3E5C',
                '&:hover': { bgcolor: '#2E7D6F' },
              }}
            >
              Edit Dashboard
            </Button>
          </>
        )}
      </Box>
    </Box>
  )
}
