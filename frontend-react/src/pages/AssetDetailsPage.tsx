/**
 * Asset Details Page
 * Comprehensive asset view with tabs for attributes, telemetry, alarms, etc.
 * Matches ThingsBoard ui-ngx asset details functionality
 */

import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box,
  Paper,
  Tabs,
  Tab,
  Typography,
  IconButton,
  Breadcrumbs,
  Link,
  Chip,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Card,
  CardContent,
  Grid,
} from '@mui/material'
import {
  ArrowBack,
  Delete,
  Add,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`asset-tabpanel-${index}`}
      aria-labelledby={`asset-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

interface Asset {
  id: string
  name: string
  label?: string
  type: string
  assetProfileId?: string
  assetProfileName?: string
  customerId?: string
  customerTitle?: string
  createdTime: number
  additionalInfo?: any
}

interface Attribute {
  key: string
  value: any
  lastUpdateTs: number
  scope: 'SERVER_SCOPE' | 'SHARED_SCOPE' | 'CLIENT_SCOPE'
}

export default function AssetDetailsPage() {
  const { assetId } = useParams<{ assetId: string }>()
  const navigate = useNavigate()
  const [asset, setAsset] = useState<Asset | null>(null)
  const [activeTab, setActiveTab] = useState(0)
  const [loading, setLoading] = useState(false)

  // Attributes state
  const [attributes, setAttributes] = useState<Attribute[]>([])
  const [attributeScope, setAttributeScope] = useState<Attribute['scope']>('SERVER_SCOPE')
  const [openAttributeDialog, setOpenAttributeDialog] = useState(false)
  const [newAttribute, setNewAttribute] = useState({ key: '', value: '' })

  useEffect(() => {
    loadAsset()
    loadAttributes()
  }, [assetId])

  const loadAsset = async () => {
    setLoading(true)
    // Mock data - in production, this would be an API call
    const mockAsset: Asset = {
      id: assetId || '1',
      name: 'Building A',
      label: 'Main Manufacturing Facility',
      type: 'building',
      assetProfileName: 'Default',
      customerTitle: 'Customer A',
      createdTime: Date.now() - 86400000 * 60,
      additionalInfo: {
        description: 'Main manufacturing building',
        address: '123 Industrial Blvd',
      },
    }
    setAsset(mockAsset)
    setLoading(false)
  }

  const loadAttributes = async () => {
    // Mock attributes
    const mockAttributes: Attribute[] = [
      {
        key: 'address',
        value: '123 Industrial Blvd, City, State',
        lastUpdateTs: Date.now() - 86400000 * 30,
        scope: 'SERVER_SCOPE',
      },
      {
        key: 'buildingArea',
        value: 5000,
        lastUpdateTs: Date.now() - 86400000 * 30,
        scope: 'SERVER_SCOPE',
      },
      {
        key: 'maxOccupancy',
        value: 150,
        lastUpdateTs: Date.now() - 3600000,
        scope: 'SHARED_SCOPE',
      },
      {
        key: 'operatingHours',
        value: '8:00 AM - 6:00 PM',
        lastUpdateTs: Date.now() - 7200000,
        scope: 'SHARED_SCOPE',
      },
    ]
    setAttributes(mockAttributes)
  }

  const handleAddAttribute = () => {
    // API call would go here
    console.log('Adding attribute:', newAttribute, 'to scope:', attributeScope)
    setOpenAttributeDialog(false)
    setNewAttribute({ key: '', value: '' })
    loadAttributes()
  }

  const handleDeleteAttribute = (attribute: Attribute) => {
    if (confirm(`Delete attribute "${attribute.key}"?`)) {
      // API call would go here
      console.log('Deleting attribute:', attribute)
      loadAttributes()
    }
  }

  const filteredAttributes = attributes.filter((attr) => attr.scope === attributeScope)

  if (!asset) {
    return (
      <MainLayout>
        <Box sx={{ p: 3 }}>
          <Typography>Loading asset...</Typography>
        </Box>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        {/* Breadcrumbs */}
        <Breadcrumbs sx={{ mb: 2 }}>
          <Link
            color="inherit"
            sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
            onClick={() => navigate('/assets')}
          >
            Assets
          </Link>
          <Typography color="text.primary">{asset.name}</Typography>
        </Breadcrumbs>

        {/* Header */}
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
          <IconButton onClick={() => navigate('/assets')} sx={{ mr: 2 }}>
            <ArrowBack />
          </IconButton>
          <Box sx={{ flex: 1 }}>
            <Typography variant="h5" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
              {asset.name}
            </Typography>
            {asset.label && (
              <Typography variant="body2" color="textSecondary">
                {asset.label}
              </Typography>
            )}
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Chip
              label={asset.type.replace(/_/g, ' ').toUpperCase()}
              size="small"
              sx={{
                bgcolor: '#FFF3E0',
                color: '#E65100',
                fontWeight: 500,
              }}
            />
          </Box>
        </Box>

        {/* Tabs */}
        <Paper>
          <Tabs
            value={activeTab}
            onChange={(_, newValue) => setActiveTab(newValue)}
            sx={{
              borderBottom: 1,
              borderColor: 'divider',
              '& .MuiTab-root': {
                textTransform: 'none',
                fontWeight: 500,
              },
            }}
          >
            <Tab label="Details" />
            <Tab label="Attributes" />
            <Tab label="Latest Telemetry" />
            <Tab label="Alarms" />
            <Tab label="Relations" />
          </Tabs>

          {/* Details Tab */}
          <TabPanel value={activeTab} index={0}>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                      Basic Information
                    </Typography>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Asset Name
                        </Typography>
                        <Typography variant="body1">{asset.name}</Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Label
                        </Typography>
                        <Typography variant="body1">{asset.label || '-'}</Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Asset Type
                        </Typography>
                        <Typography variant="body1">
                          {asset.type.replace(/_/g, ' ').toUpperCase()}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Asset Profile
                        </Typography>
                        <Typography variant="body1">
                          {asset.assetProfileName || 'Default'}
                        </Typography>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                      System Information
                    </Typography>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Asset ID
                        </Typography>
                        <Typography
                          variant="body2"
                          sx={{ fontFamily: 'monospace', fontSize: '0.875rem' }}
                        >
                          {asset.id}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Customer
                        </Typography>
                        <Typography variant="body1">
                          {asset.customerTitle || 'Unassigned'}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Created Time
                        </Typography>
                        <Typography variant="body1">
                          {format(new Date(asset.createdTime), 'MMM dd, yyyy HH:mm:ss')}
                        </Typography>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </TabPanel>

          {/* Attributes Tab */}
          <TabPanel value={activeTab} index={1}>
            <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <FormControl size="small" sx={{ minWidth: 200 }}>
                <InputLabel>Attribute Scope</InputLabel>
                <Select
                  value={attributeScope}
                  label="Attribute Scope"
                  onChange={(e) => setAttributeScope(e.target.value as Attribute['scope'])}
                >
                  <MenuItem value="SERVER_SCOPE">Server Attributes</MenuItem>
                  <MenuItem value="SHARED_SCOPE">Shared Attributes</MenuItem>
                  <MenuItem value="CLIENT_SCOPE">Client Attributes</MenuItem>
                </Select>
              </FormControl>
              <Button
                variant="contained"
                startIcon={<Add />}
                onClick={() => setOpenAttributeDialog(true)}
                sx={{ bgcolor: '#0F3E5C' }}
              >
                Add Attribute
              </Button>
            </Box>

            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 600 }}>Key</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Value</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Last Update</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600 }}>
                      Actions
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredAttributes.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        <Typography color="textSecondary">No attributes found</Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredAttributes.map((attr) => (
                      <TableRow key={attr.key}>
                        <TableCell sx={{ fontFamily: 'monospace' }}>{attr.key}</TableCell>
                        <TableCell>
                          {typeof attr.value === 'object'
                            ? JSON.stringify(attr.value)
                            : String(attr.value)}
                        </TableCell>
                        <TableCell>
                          {format(new Date(attr.lastUpdateTs), 'MMM dd, yyyy HH:mm:ss')}
                        </TableCell>
                        <TableCell align="right">
                          <IconButton
                            size="small"
                            onClick={() => handleDeleteAttribute(attr)}
                          >
                            <Delete fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </TabPanel>

          {/* Latest Telemetry Tab */}
          <TabPanel value={activeTab} index={2}>
            <Typography color="textSecondary">
              Asset telemetry will be displayed here
            </Typography>
          </TabPanel>

          {/* Alarms Tab */}
          <TabPanel value={activeTab} index={3}>
            <Typography color="textSecondary">
              Asset-specific alarms will be displayed here
            </Typography>
          </TabPanel>

          {/* Relations Tab */}
          <TabPanel value={activeTab} index={4}>
            <Typography color="textSecondary">
              Asset relations will be displayed here
            </Typography>
          </TabPanel>
        </Paper>

        {/* Add Attribute Dialog */}
        <Dialog
          open={openAttributeDialog}
          onClose={() => setOpenAttributeDialog(false)}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>Add Attribute</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Key"
                required
                fullWidth
                value={newAttribute.key}
                onChange={(e) => setNewAttribute({ ...newAttribute, key: e.target.value })}
              />
              <TextField
                label="Value"
                required
                fullWidth
                value={newAttribute.value}
                onChange={(e) => setNewAttribute({ ...newAttribute, value: e.target.value })}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenAttributeDialog(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleAddAttribute}
              disabled={!newAttribute.key || !newAttribute.value}
              sx={{ bgcolor: '#0F3E5C' }}
            >
              Add
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </MainLayout>
  )
}
