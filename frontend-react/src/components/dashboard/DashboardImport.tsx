/**
 * Dashboard Import Dialog
 * Import dashboard configuration from JSON file
 */

import { useState, useRef } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Alert,
  IconButton,
  TextField,
  FormControl,
  FormControlLabel,
  Radio,
  RadioGroup,
} from '@mui/material'
import {
  Publish as ImportIcon,
  Close as CloseIcon,
  CloudUpload as UploadIcon,
} from '@mui/icons-material'

interface DashboardImportProps {
  open: boolean
  onClose: () => void
  onImport: (dashboard: any, mode: 'merge' | 'replace') => void
}

export default function DashboardImport({
  open,
  onClose,
  onImport,
}: DashboardImportProps) {
  const [importMode, setImportMode] = useState<'merge' | 'replace'>('replace')
  const [jsonContent, setJsonContent] = useState('')
  const [error, setError] = useState('')
  const [fileName, setFileName] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    setFileName(file.name)
    const reader = new FileReader()

    reader.onload = (e) => {
      try {
        const content = e.target?.result as string
        const json = JSON.parse(content)

        // Validate dashboard structure
        if (!json.widgets || !Array.isArray(json.widgets)) {
          setError('Invalid dashboard format: missing widgets array')
          return
        }

        setJsonContent(content)
        setError('')
      } catch (err) {
        setError('Failed to parse JSON file. Please check the file format.')
        setJsonContent('')
      }
    }

    reader.onerror = () => {
      setError('Failed to read file')
    }

    reader.readAsText(file)
  }

  const handleImport = () => {
    try {
      const dashboard = JSON.parse(jsonContent)
      onImport(dashboard, importMode)
      handleClose()
    } catch (err) {
      setError('Failed to import dashboard. Please check the JSON format.')
    }
  }

  const handleClose = () => {
    setJsonContent('')
    setError('')
    setFileName('')
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
    onClose()
  }

  const isValid = jsonContent && !error

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ bgcolor: '#0F3E5C', color: 'white' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <ImportIcon />
            <Typography variant="h6">Import Dashboard</Typography>
          </Box>
          <IconButton onClick={handleClose} sx={{ color: 'white' }}>
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>

      <DialogContent sx={{ mt: 2 }}>
        <Box sx={{ mb: 3 }}>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json"
            onChange={handleFileSelect}
            style={{ display: 'none' }}
            id="dashboard-file-input"
          />
          <label htmlFor="dashboard-file-input">
            <Button
              variant="outlined"
              component="span"
              startIcon={<UploadIcon />}
              fullWidth
              sx={{ py: 2 }}
            >
              {fileName || 'Select JSON File'}
            </Button>
          </label>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {jsonContent && !error && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Dashboard configuration loaded successfully!
          </Alert>
        )}

        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" sx={{ mb: 1, fontWeight: 'bold' }}>
            Import Mode
          </Typography>
          <FormControl component="fieldset">
            <RadioGroup
              value={importMode}
              onChange={(e) => setImportMode(e.target.value as 'merge' | 'replace')}
            >
              <FormControlLabel
                value="replace"
                control={<Radio />}
                label="Replace - Remove all existing widgets and import new ones"
              />
              <FormControlLabel
                value="merge"
                control={<Radio />}
                label="Merge - Keep existing widgets and add imported ones"
              />
            </RadioGroup>
          </FormControl>
        </Box>

        {jsonContent && (
          <Box>
            <Typography variant="subtitle1" sx={{ mb: 1, fontWeight: 'bold' }}>
              Preview
            </Typography>
            <TextField
              fullWidth
              multiline
              rows={8}
              value={jsonContent}
              InputProps={{
                readOnly: true,
                sx: { fontFamily: 'monospace', fontSize: '12px' },
              }}
            />
          </Box>
        )}

        {!jsonContent && (
          <Alert severity="info">
            Select a JSON file exported from ThingsBoard dashboard to import it.
            The file should contain dashboard configuration including widgets and layouts.
          </Alert>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={handleClose} color="inherit">
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={handleImport}
          disabled={!isValid}
          sx={{ bgcolor: '#0F3E5C', '&:hover': { bgcolor: '#2E7D6F' } }}
        >
          Import
        </Button>
      </DialogActions>
    </Dialog>
  )
}
