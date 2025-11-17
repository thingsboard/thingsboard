/**
 * Widget Configuration Dialog
 * Comprehensive widget settings matching ThingsBoard's widget configuration
 */

import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Tabs,
  Tab,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Grid,
  IconButton,
  Typography,
  Divider,
  Chip,
  Paper,
} from '@mui/material'
import {
  Settings as SettingsIcon,
  Close as CloseIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  DataObject as DataIcon,
  AccessTime as TimeIcon,
  Palette as StyleIcon,
  TouchApp as ActionIcon,
} from '@mui/icons-material'
import { Widget, Datasource, DataKey, Timewindow } from '@/types/dashboard'

interface WidgetConfigDialogProps {
  open: boolean
  onClose: () => void
  widget: Widget | null
  onSave: (widget: Widget) => void
}

interface TabPanelProps {
  children?: React.ReactNode
  value: number
  index: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`widget-config-tabpanel-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

export default function WidgetConfigDialog({
  open,
  onClose,
  widget: initialWidget,
  onSave,
}: WidgetConfigDialogProps) {
  const [tabValue, setTabValue] = useState(0)
  const [widget, setWidget] = useState<Widget | null>(initialWidget)

  useEffect(() => {
    setWidget(initialWidget)
    setTabValue(0)
  }, [initialWidget])

  if (!widget) return null

  const handleSave = () => {
    if (widget) {
      onSave(widget)
      onClose()
    }
  }

  const updateConfig = (path: string, value: any) => {
    setWidget((prev) => {
      if (!prev) return prev
      const newWidget = { ...prev }
      const keys = path.split('.')
      let current: any = newWidget.config
      for (let i = 0; i < keys.length - 1; i++) {
        if (!current[keys[i]]) current[keys[i]] = {}
        current = current[keys[i]]
      }
      current[keys[keys.length - 1]] = value
      return newWidget
    })
  }

  const addDatasource = () => {
    const newDatasource: Datasource = {
      type: 'entity',
      name: 'New Datasource',
      dataKeys: [],
    }
    setWidget((prev) => {
      if (!prev) return prev
      return {
        ...prev,
        config: {
          ...prev.config,
          datasources: [...(prev.config.datasources || []), newDatasource],
        },
      }
    })
  }

  const removeDatasource = (index: number) => {
    setWidget((prev) => {
      if (!prev) return prev
      const datasources = [...(prev.config.datasources || [])]
      datasources.splice(index, 1)
      return {
        ...prev,
        config: {
          ...prev.config,
          datasources,
        },
      }
    })
  }

  const updateDatasource = (index: number, field: string, value: any) => {
    setWidget((prev) => {
      if (!prev) return prev
      const datasources = [...(prev.config.datasources || [])]
      datasources[index] = { ...datasources[index], [field]: value }
      return {
        ...prev,
        config: {
          ...prev.config,
          datasources,
        },
      }
    })
  }

  const addDataKey = (datasourceIndex: number) => {
    const newDataKey: DataKey = {
      name: '',
      type: 'timeseries',
      label: '',
      color: '#2196F3',
    }
    setWidget((prev) => {
      if (!prev) return prev
      const datasources = [...(prev.config.datasources || [])]
      datasources[datasourceIndex] = {
        ...datasources[datasourceIndex],
        dataKeys: [...(datasources[datasourceIndex].dataKeys || []), newDataKey],
      }
      return {
        ...prev,
        config: {
          ...prev.config,
          datasources,
        },
      }
    })
  }

  const removeDataKey = (datasourceIndex: number, keyIndex: number) => {
    setWidget((prev) => {
      if (!prev) return prev
      const datasources = [...(prev.config.datasources || [])]
      const dataKeys = [...(datasources[datasourceIndex].dataKeys || [])]
      dataKeys.splice(keyIndex, 1)
      datasources[datasourceIndex] = { ...datasources[datasourceIndex], dataKeys }
      return {
        ...prev,
        config: {
          ...prev.config,
          datasources,
        },
      }
    })
  }

  const updateDataKey = (
    datasourceIndex: number,
    keyIndex: number,
    field: string,
    value: any
  ) => {
    setWidget((prev) => {
      if (!prev) return prev
      const datasources = [...(prev.config.datasources || [])]
      const dataKeys = [...(datasources[datasourceIndex].dataKeys || [])]
      dataKeys[keyIndex] = { ...dataKeys[keyIndex], [field]: value }
      datasources[datasourceIndex] = { ...datasources[datasourceIndex], dataKeys }
      return {
        ...prev,
        config: {
          ...prev.config,
          datasources,
        },
      }
    })
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="lg"
      fullWidth
      PaperProps={{
        sx: { height: '90vh', maxHeight: '900px' },
      }}
    >
      <DialogTitle sx={{ bgcolor: '#0F3E5C', color: 'white' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <SettingsIcon />
            <Typography variant="h6">Widget Configuration</Typography>
          </Box>
          <IconButton onClick={onClose} sx={{ color: 'white' }}>
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}>
          <Tab icon={<DataIcon />} label="Data" iconPosition="start" />
          <Tab icon={<TimeIcon />} label="Timewindow" iconPosition="start" />
          <Tab icon={<StyleIcon />} label="Appearance" iconPosition="start" />
          <Tab icon={<ActionIcon />} label="Actions" iconPosition="start" />
          <Tab icon={<SettingsIcon />} label="Advanced" iconPosition="start" />
        </Tabs>
      </Box>

      <DialogContent sx={{ p: 0, overflow: 'auto' }}>
        {/* Data Tab */}
        <TabPanel value={tabValue} index={0}>
          <Box sx={{ mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">Datasources</Typography>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={addDatasource}
                sx={{ bgcolor: '#0F3E5C' }}
              >
                Add Datasource
              </Button>
            </Box>

            {widget.config.datasources?.map((datasource, dsIndex) => (
              <Paper key={dsIndex} sx={{ p: 2, mb: 2 }} elevation={2}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Chip label={`Datasource ${dsIndex + 1}`} color="primary" />
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => removeDatasource(dsIndex)}
                  >
                    <DeleteIcon />
                  </IconButton>
                </Box>

                <Grid container spacing={2}>
                  <Grid item xs={12} md={6}>
                    <FormControl fullWidth size="small">
                      <InputLabel>Type</InputLabel>
                      <Select
                        value={datasource.type}
                        label="Type"
                        onChange={(e) => updateDatasource(dsIndex, 'type', e.target.value)}
                      >
                        <MenuItem value="entity">Entity</MenuItem>
                        <MenuItem value="function">Function</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      size="small"
                      label="Name"
                      value={datasource.name}
                      onChange={(e) => updateDatasource(dsIndex, 'name', e.target.value)}
                    />
                  </Grid>

                  {/* Data Keys */}
                  <Grid item xs={12}>
                    <Divider sx={{ my: 1 }} />
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                      <Typography variant="subtitle2">Data Keys</Typography>
                      <Button
                        size="small"
                        startIcon={<AddIcon />}
                        onClick={() => addDataKey(dsIndex)}
                      >
                        Add Key
                      </Button>
                    </Box>

                    {datasource.dataKeys?.map((dataKey, keyIndex) => (
                      <Box
                        key={keyIndex}
                        sx={{
                          display: 'flex',
                          gap: 1,
                          mb: 1,
                          p: 1,
                          border: '1px solid #E0E0E0',
                          borderRadius: 1,
                        }}
                      >
                        <TextField
                          size="small"
                          label="Key"
                          value={dataKey.name}
                          onChange={(e) =>
                            updateDataKey(dsIndex, keyIndex, 'name', e.target.value)
                          }
                          sx={{ flex: 2 }}
                        />
                        <FormControl size="small" sx={{ flex: 1 }}>
                          <InputLabel>Type</InputLabel>
                          <Select
                            value={dataKey.type}
                            label="Type"
                            onChange={(e) =>
                              updateDataKey(dsIndex, keyIndex, 'type', e.target.value)
                            }
                          >
                            <MenuItem value="timeseries">Timeseries</MenuItem>
                            <MenuItem value="attribute">Attribute</MenuItem>
                          </Select>
                        </FormControl>
                        <TextField
                          size="small"
                          label="Label"
                          value={dataKey.label}
                          onChange={(e) =>
                            updateDataKey(dsIndex, keyIndex, 'label', e.target.value)
                          }
                          sx={{ flex: 2 }}
                        />
                        <TextField
                          size="small"
                          type="color"
                          label="Color"
                          value={dataKey.color || '#2196F3'}
                          onChange={(e) =>
                            updateDataKey(dsIndex, keyIndex, 'color', e.target.value)
                          }
                          sx={{ width: 80 }}
                        />
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => removeDataKey(dsIndex, keyIndex)}
                        >
                          <DeleteIcon />
                        </IconButton>
                      </Box>
                    ))}
                  </Grid>
                </Grid>
              </Paper>
            ))}

            {(!widget.config.datasources || widget.config.datasources.length === 0) && (
              <Paper sx={{ p: 3, textAlign: 'center', bgcolor: '#F5F5F5' }}>
                <Typography color="text.secondary">
                  No datasources configured. Click "Add Datasource" to get started.
                </Typography>
              </Paper>
            )}
          </Box>
        </TabPanel>

        {/* Timewindow Tab */}
        <TabPanel value={tabValue} index={1}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={widget.config.useDashboardTimewindow ?? true}
                    onChange={(e) =>
                      updateConfig('useDashboardTimewindow', e.target.checked)
                    }
                  />
                }
                label="Use dashboard timewindow"
              />
            </Grid>

            {!widget.config.useDashboardTimewindow && (
              <>
                <Grid item xs={12}>
                  <Typography variant="subtitle1" sx={{ mb: 2 }}>
                    Realtime Settings
                  </Typography>
                  <TextField
                    fullWidth
                    type="number"
                    label="Time window (ms)"
                    value={widget.config.timewindow?.realtime?.timewindowMs || 300000}
                    onChange={(e) =>
                      updateConfig('timewindow.realtime.timewindowMs', parseInt(e.target.value))
                    }
                    helperText="Time range for realtime data (default: 300000ms = 5 minutes)"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Update interval (ms)"
                    value={widget.config.timewindow?.realtime?.interval || 1000}
                    onChange={(e) =>
                      updateConfig('timewindow.realtime.interval', parseInt(e.target.value))
                    }
                    helperText="How often to update data (default: 1000ms = 1 second)"
                  />
                </Grid>
              </>
            )}
          </Grid>
        </TabPanel>

        {/* Appearance Tab */}
        <TabPanel value={tabValue} index={2}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Widget Title"
                value={widget.config.title || ''}
                onChange={(e) => updateConfig('title', e.target.value)}
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={widget.config.showTitle ?? true}
                    onChange={(e) => updateConfig('showTitle', e.target.checked)}
                  />
                }
                label="Show title"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={widget.config.settings?.showLegend ?? true}
                    onChange={(e) => updateConfig('settings.showLegend', e.target.checked)}
                  />
                }
                label="Show legend"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={widget.config.settings?.showTooltip ?? true}
                    onChange={(e) => updateConfig('settings.showTooltip', e.target.checked)}
                  />
                }
                label="Show tooltip"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                type="number"
                label="Decimal places"
                value={widget.config.settings?.decimals ?? 2}
                onChange={(e) =>
                  updateConfig('settings.decimals', parseInt(e.target.value))
                }
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Units"
                value={widget.config.settings?.units || ''}
                onChange={(e) => updateConfig('settings.units', e.target.value)}
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Actions Tab */}
        <TabPanel value={tabValue} index={3}>
          <Typography variant="body2" color="text.secondary">
            Widget actions configuration will be implemented here.
            This allows adding custom buttons and click handlers to widgets.
          </Typography>
        </TabPanel>

        {/* Advanced Tab */}
        <TabPanel value={tabValue} index={4}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Custom CSS"
                value={widget.config.widgetCss || ''}
                onChange={(e) => updateConfig('widgetCss', e.target.value)}
                helperText="Add custom CSS styles for this widget"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={widget.config.settings?.animation ?? true}
                    onChange={(e) => updateConfig('settings.animation', e.target.checked)}
                  />
                }
                label="Enable animations"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={widget.config.settings?.smooth ?? true}
                    onChange={(e) => updateConfig('settings.smooth', e.target.checked)}
                  />
                }
                label="Smooth curves (for line charts)"
              />
            </Grid>
          </Grid>
        </TabPanel>
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid #E0E0E0' }}>
        <Button onClick={onClose} color="inherit">
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={handleSave}
          sx={{ bgcolor: '#0F3E5C', '&:hover': { bgcolor: '#2E7D6F' } }}
        >
          Save Configuration
        </Button>
      </DialogActions>
    </Dialog>
  )
}
