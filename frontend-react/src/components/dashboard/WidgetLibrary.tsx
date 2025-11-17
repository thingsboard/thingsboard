/**
 * Widget Library Dialog
 * Displays available widgets grouped by category
 * Matches ThingsBoard's widget library interface
 */

import { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Grid,
  Card,
  CardContent,
  CardActionArea,
  Typography,
  Tabs,
  Tab,
  TextField,
  InputAdornment,
  Chip,
} from '@mui/material'
import {
  Search as SearchIcon,
  ShowChart as ChartIcon,
  Dashboard as CardIcon,
  TableChart as TableIcon,
  ToggleOn as ControlIcon,
  Map as MapIcon,
  Input as InputIcon,
  Code as StaticIcon,
} from '@mui/icons-material'
import { widgetRegistry } from '@/widgets/widgetRegistry'
import { WidgetTypeId } from '@/types/dashboard'

interface WidgetLibraryProps {
  open: boolean
  onClose: () => void
  onSelectWidget: (typeId: WidgetTypeId) => void
}

type WidgetCategory = 'all' | 'charts' | 'cards' | 'tables' | 'controls' | 'maps' | 'input' | 'static'

const categoryIcons: Record<WidgetCategory, React.ReactNode> = {
  all: <CardIcon />,
  charts: <ChartIcon />,
  cards: <CardIcon />,
  tables: <TableIcon />,
  controls: <ControlIcon />,
  maps: <MapIcon />,
  input: <InputIcon />,
  static: <StaticIcon />,
}

const categoryLabels: Record<WidgetCategory, string> = {
  all: 'All Widgets',
  charts: 'Charts',
  cards: 'Cards',
  tables: 'Tables',
  controls: 'Controls',
  maps: 'Maps',
  input: 'Input',
  static: 'Static',
}

export default function WidgetLibrary({
  open,
  onClose,
  onSelectWidget,
}: WidgetLibraryProps) {
  const [selectedCategory, setSelectedCategory] = useState<WidgetCategory>('all')
  const [searchQuery, setSearchQuery] = useState('')

  // Get all available widgets from registry
  const allWidgets = widgetRegistry.getAllDescriptors()

  // Filter widgets by category and search
  const filteredWidgets = allWidgets.filter((descriptor) => {
    // Category filter
    if (selectedCategory !== 'all') {
      const widgetCategory = descriptor.type === 'timeseries' ? 'charts' :
                             descriptor.type === 'latest' ? 'cards' :
                             descriptor.type === 'alarm' ? 'tables' :
                             descriptor.type === 'rpc' ? 'controls' :
                             'static'
      if (widgetCategory !== selectedCategory) return false
    }

    // Search filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase()
      return (
        descriptor.name.toLowerCase().includes(query) ||
        descriptor.description?.toLowerCase().includes(query) ||
        descriptor.tags?.some(tag => tag.toLowerCase().includes(query))
      )
    }

    return true
  })

  const handleWidgetSelect = (typeId: WidgetTypeId) => {
    onSelectWidget(typeId)
    onClose()
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="lg"
      fullWidth
      PaperProps={{
        sx: {
          height: '80vh',
          maxHeight: '800px',
        },
      }}
    >
      <DialogTitle sx={{ bgcolor: '#0F3E5C', color: 'white' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CardIcon />
          <Typography variant="h6">Widget Library</Typography>
        </Box>
      </DialogTitle>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs
          value={selectedCategory}
          onChange={(_, value) => setSelectedCategory(value)}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ px: 2 }}
        >
          {(Object.keys(categoryLabels) as WidgetCategory[]).map((category) => (
            <Tab
              key={category}
              value={category}
              label={categoryLabels[category]}
              icon={categoryIcons[category]}
              iconPosition="start"
            />
          ))}
        </Tabs>
      </Box>

      <DialogContent>
        {/* Search Bar */}
        <Box sx={{ mb: 3 }}>
          <TextField
            fullWidth
            placeholder="Search widgets..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
            size="small"
          />
        </Box>

        {/* Widget Grid */}
        <Grid container spacing={2}>
          {filteredWidgets.length === 0 ? (
            <Grid item xs={12}>
              <Box
                sx={{
                  textAlign: 'center',
                  py: 6,
                  color: 'text.secondary',
                }}
              >
                <Typography variant="h6">No widgets found</Typography>
                <Typography variant="body2">
                  Try adjusting your search or category filter
                </Typography>
              </Box>
            </Grid>
          ) : (
            filteredWidgets.map((descriptor) => (
              <Grid item xs={12} sm={6} md={4} key={descriptor.id}>
                <Card
                  elevation={2}
                  sx={{
                    height: '100%',
                    transition: 'all 0.2s',
                    '&:hover': {
                      boxShadow: 6,
                      transform: 'translateY(-4px)',
                    },
                  }}
                >
                  <CardActionArea
                    onClick={() => handleWidgetSelect(descriptor.id)}
                    sx={{ height: '100%' }}
                  >
                    <CardContent>
                      {/* Widget Icon/Preview */}
                      <Box
                        sx={{
                          height: 100,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          bgcolor: '#F5F5F5',
                          borderRadius: 1,
                          mb: 2,
                        }}
                      >
                        {descriptor.type === 'timeseries' && <ChartIcon sx={{ fontSize: 48, color: '#0F3E5C' }} />}
                        {descriptor.type === 'latest' && <CardIcon sx={{ fontSize: 48, color: '#2E7D6F' }} />}
                        {descriptor.type === 'alarm' && <TableIcon sx={{ fontSize: 48, color: '#C62828' }} />}
                        {descriptor.type === 'rpc' && <ControlIcon sx={{ fontSize: 48, color: '#1976D2' }} />}
                        {descriptor.type === 'static' && <StaticIcon sx={{ fontSize: 48, color: '#757575' }} />}
                      </Box>

                      {/* Widget Info */}
                      <Typography
                        variant="h6"
                        sx={{ fontSize: '14px', fontWeight: 'bold', mb: 1 }}
                      >
                        {descriptor.name}
                      </Typography>
                      <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{
                          fontSize: '12px',
                          height: '40px',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          display: '-webkit-box',
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: 'vertical',
                        }}
                      >
                        {descriptor.description || 'No description available'}
                      </Typography>

                      {/* Tags */}
                      {descriptor.tags && descriptor.tags.length > 0 && (
                        <Box sx={{ mt: 2, display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                          {descriptor.tags.slice(0, 3).map((tag) => (
                            <Chip
                              key={tag}
                              label={tag}
                              size="small"
                              sx={{ fontSize: '10px', height: '20px' }}
                            />
                          ))}
                        </Box>
                      )}
                    </CardContent>
                  </CardActionArea>
                </Card>
              </Grid>
            ))
          )}
        </Grid>
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} color="inherit">
          Cancel
        </Button>
      </DialogActions>
    </Dialog>
  )
}
