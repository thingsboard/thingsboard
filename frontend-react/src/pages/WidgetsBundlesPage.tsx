/**
 * Widgets Bundles Page
 * Widget bundle management for dashboard widgets
 * Matches ThingsBoard ui-ngx widgets bundles functionality
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
  Typography,
  Card,
  CardContent,
  CardActions,
  Grid,
  Avatar,
} from '@mui/material'
import {
  Widgets,
  Edit,
  Delete,
  Add,
  Visibility,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'

interface WidgetBundle {
  id: string
  title: string
  alias: string
  description?: string
  image?: string
  widgetCount: number
  createdTime: number
  system?: boolean
}

export default function WidgetsBundlesPage() {
  const navigate = useNavigate()
  const [bundles, setBundles] = useState<WidgetBundle[]>([])
  const [loading, setLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [openDialog, setOpenDialog] = useState(false)
  const [editingBundle, setEditingBundle] = useState<WidgetBundle | null>(null)
  const [formData, setFormData] = useState({
    title: '',
    alias: '',
    description: '',
  })

  useEffect(() => {
    loadBundles()
  }, [searchQuery])

  const loadBundles = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockBundles: WidgetBundle[] = [
      {
        id: '1',
        title: 'Cards',
        alias: 'cards',
        description: 'Card widgets for displaying simple data',
        widgetCount: 12,
        createdTime: Date.now() - 86400000 * 365,
        system: true,
      },
      {
        id: '2',
        title: 'Charts',
        alias: 'charts',
        description: 'Chart widgets for data visualization',
        widgetCount: 24,
        createdTime: Date.now() - 86400000 * 365,
        system: true,
      },
      {
        id: '3',
        title: 'Gauges',
        alias: 'gauges',
        description: 'Gauge widgets for displaying metrics',
        widgetCount: 8,
        createdTime: Date.now() - 86400000 * 365,
        system: true,
      },
      {
        id: '4',
        title: 'Maps',
        alias: 'maps',
        description: 'Map widgets for geolocation data',
        widgetCount: 6,
        createdTime: Date.now() - 86400000 * 365,
        system: true,
      },
      {
        id: '5',
        title: 'Control widgets',
        alias: 'control_widgets',
        description: 'Interactive control widgets',
        widgetCount: 15,
        createdTime: Date.now() - 86400000 * 365,
        system: true,
      },
      {
        id: '6',
        title: 'Alarm widgets',
        alias: 'alarm_widgets',
        description: 'Widgets for alarm management',
        widgetCount: 5,
        createdTime: Date.now() - 86400000 * 365,
        system: true,
      },
      {
        id: '7',
        title: 'Custom Bundle',
        alias: 'custom_bundle',
        description: 'User-created custom widgets',
        widgetCount: 3,
        createdTime: Date.now() - 86400000 * 30,
        system: false,
      },
    ]

    const filtered = searchQuery
      ? mockBundles.filter(
          (b) =>
            b.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
            b.description?.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : mockBundles

    setBundles(filtered)
    setLoading(false)
  }

  const handleAdd = () => {
    setEditingBundle(null)
    setFormData({
      title: '',
      alias: '',
      description: '',
    })
    setOpenDialog(true)
  }

  const handleEdit = (bundle: WidgetBundle) => {
    if (bundle.system) {
      alert('System bundles cannot be edited')
      return
    }
    setEditingBundle(bundle)
    setFormData({
      title: bundle.title,
      alias: bundle.alias,
      description: bundle.description || '',
    })
    setOpenDialog(true)
  }

  const handleDelete = (bundle: WidgetBundle) => {
    if (bundle.system) {
      alert('System bundles cannot be deleted')
      return
    }
    if (confirm(`Delete widget bundle "${bundle.title}"?`)) {
      console.log('Deleting bundle:', bundle.id)
      loadBundles()
    }
  }

  const handleSave = () => {
    console.log('Saving bundle:', formData)
    setOpenDialog(false)
    loadBundles()
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h5" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
            Widget Bundles
          </Typography>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField
              size="small"
              placeholder="Search bundles..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              sx={{ width: 300 }}
            />
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={handleAdd}
              sx={{ bgcolor: '#0F3E5C' }}
            >
              Add Bundle
            </Button>
          </Box>
        </Box>

        {/* Bundle Cards Grid */}
        <Grid container spacing={3}>
          {bundles.map((bundle) => (
            <Grid item xs={12} sm={6} md={4} key={bundle.id}>
              <Card
                sx={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  '&:hover': {
                    boxShadow: 3,
                  },
                }}
              >
                <CardContent sx={{ flex: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Avatar
                      sx={{
                        bgcolor: '#E3F2FD',
                        color: '#0F3E5C',
                        mr: 2,
                      }}
                    >
                      <Widgets />
                    </Avatar>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="h6" sx={{ fontWeight: 600 }}>
                        {bundle.title}
                      </Typography>
                      {bundle.system && (
                        <Chip
                          label="System"
                          size="small"
                          sx={{
                            bgcolor: '#E3F2FD',
                            color: '#0F3E5C',
                            fontWeight: 500,
                            height: 20,
                            mt: 0.5,
                          }}
                        />
                      )}
                    </Box>
                  </Box>

                  <Typography
                    variant="body2"
                    color="textSecondary"
                    sx={{ mb: 2, minHeight: 40 }}
                  >
                    {bundle.description || 'No description'}
                  </Typography>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="textSecondary">
                      Widgets: <strong>{bundle.widgetCount}</strong>
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      {format(new Date(bundle.createdTime), 'MMM dd, yyyy')}
                    </Typography>
                  </Box>
                </CardContent>

                <CardActions sx={{ p: 2, pt: 0 }}>
                  <Button
                    size="small"
                    startIcon={<Visibility />}
                    onClick={() => navigate(`/widgets-bundles/${bundle.id}`)}
                  >
                    View Widgets
                  </Button>
                  {!bundle.system && (
                    <>
                      <IconButton
                        size="small"
                        onClick={() => handleEdit(bundle)}
                      >
                        <Edit fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDelete(bundle)}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </>
                  )}
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>

        {bundles.length === 0 && !loading && (
          <Box
            sx={{
              textAlign: 'center',
              py: 8,
              color: 'textSecondary',
            }}
          >
            <Typography variant="h6">No widget bundles found</Typography>
          </Box>
        )}

        {/* Add/Edit Dialog */}
        <Dialog
          open={openDialog}
          onClose={() => setOpenDialog(false)}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>
            {editingBundle ? 'Edit Widget Bundle' : 'Add Widget Bundle'}
          </DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Title"
                required
                fullWidth
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              />
              <TextField
                label="Alias"
                required
                fullWidth
                value={formData.alias}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    alias: e.target.value.toLowerCase().replace(/\s+/g, '_'),
                  })
                }
                helperText="Lowercase, underscores only"
              />
              <TextField
                label="Description"
                fullWidth
                multiline
                rows={3}
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={!formData.title || !formData.alias}
              sx={{ bgcolor: '#0F3E5C' }}
            >
              {editingBundle ? 'Save' : 'Add'}
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </MainLayout>
  )
}
