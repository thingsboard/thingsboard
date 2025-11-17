/**
 * Device Credentials Dialog
 * Manage device authentication credentials
 */

import React, { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  Alert,
  Divider,
  InputAdornment,
} from '@mui/material'
import {
  Close,
  ContentCopy,
  Visibility,
  VisibilityOff,
  Key,
  Certificate,
  Refresh,
  Check,
} from '@mui/icons-material'
import {
  Device,
  DeviceCredentials,
  DeviceCredentialsType,
  deviceCredentialsTypeNames,
} from '../../types/device.types'

interface DeviceCredentialsDialogProps {
  open: boolean
  device: Device
  credentials?: DeviceCredentials
  onClose: () => void
  onSave: (credentials: DeviceCredentials) => void
}

export default function DeviceCredentialsDialog({
  open,
  device,
  credentials,
  onClose,
  onSave,
}: DeviceCredentialsDialogProps) {
  const [credentialsType, setCredentialsType] = useState<DeviceCredentialsType>(
    credentials?.credentialsType || DeviceCredentialsType.ACCESS_TOKEN
  )
  const [accessToken, setAccessToken] = useState(credentials?.credentialsId || '')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [clientId, setClientId] = useState('')
  const [certificate, setCertificate] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [copied, setCopied] = useState(false)

  const generateToken = () => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    let token = ''
    for (let i = 0; i < 20; i++) {
      token += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    setAccessToken(token)
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleSave = () => {
    const newCredentials: DeviceCredentials = {
      id: credentials?.id || { id: '', entityType: 'DEVICE_CREDENTIALS' },
      createdTime: credentials?.createdTime || Date.now(),
      deviceId: device.id,
      credentialsType,
      credentialsId: accessToken,
      credentialsValue:
        credentialsType === DeviceCredentialsType.MQTT_BASIC
          ? JSON.stringify({ clientId, username, password })
          : credentialsType === DeviceCredentialsType.X509_CERTIFICATE
            ? certificate
            : undefined,
    }
    onSave(newCredentials)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Key color="primary" />
            <Box>
              <Typography variant="h6">Device Credentials</Typography>
              <Typography variant="caption" color="text.secondary">
                {device.name}
              </Typography>
            </Box>
          </Box>
          <IconButton onClick={onClose} size="small">
            <Close />
          </IconButton>
        </Box>
      </DialogTitle>

      <Divider />

      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, pt: 2 }}>
          <FormControl fullWidth>
            <InputLabel>Credentials Type</InputLabel>
            <Select
              value={credentialsType}
              onChange={(e) => setCredentialsType(e.target.value as DeviceCredentialsType)}
              label="Credentials Type"
            >
              {Object.values(DeviceCredentialsType).map((type) => (
                <MenuItem key={type} value={type}>
                  {deviceCredentialsTypeNames[type]}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {credentialsType === DeviceCredentialsType.ACCESS_TOKEN && (
            <>
              <Alert severity="info">
                Access tokens are used for HTTP, MQTT, and CoAP authentication. Keep your token secure!
              </Alert>
              <TextField
                label="Access Token"
                value={accessToken}
                onChange={(e) => setAccessToken(e.target.value)}
                fullWidth
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => copyToClipboard(accessToken)} size="small">
                        {copied ? <Check color="success" /> : <ContentCopy />}
                      </IconButton>
                      <IconButton onClick={generateToken} size="small">
                        <Refresh />
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
                helperText="Use this token to authenticate device requests"
              />
            </>
          )}

          {credentialsType === DeviceCredentialsType.MQTT_BASIC && (
            <>
              <Alert severity="info">
                MQTT Basic authentication uses Client ID, Username, and Password.
              </Alert>
              <TextField
                label="Client ID"
                value={clientId}
                onChange={(e) => setClientId(e.target.value)}
                fullWidth
                helperText="Unique identifier for MQTT client"
              />
              <TextField
                label="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                fullWidth
              />
              <TextField
                label="Password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                fullWidth
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => setShowPassword(!showPassword)} size="small">
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
            </>
          )}

          {credentialsType === DeviceCredentialsType.X509_CERTIFICATE && (
            <>
              <Alert severity="info">
                Upload X.509 certificate for secure device authentication.
              </Alert>
              <TextField
                label="Certificate (PEM format)"
                value={certificate}
                onChange={(e) => setCertificate(e.target.value)}
                multiline
                rows={8}
                fullWidth
                placeholder="-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIJAKL0UG...
-----END CERTIFICATE-----"
                helperText="Paste your X.509 certificate in PEM format"
              />
            </>
          )}

          {credentialsType === DeviceCredentialsType.LWM2M_CREDENTIALS && (
            <>
              <Alert severity="warning">
                LwM2M credentials configuration requires advanced setup. Contact your administrator.
              </Alert>
              <TextField
                label="Endpoint Client Name"
                value={accessToken}
                onChange={(e) => setAccessToken(e.target.value)}
                fullWidth
                helperText="LwM2M client endpoint identifier"
              />
            </>
          )}

          <Divider />

          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Security Tips
            </Typography>
            <Typography variant="body2" color="text.secondary">
              • Never share credentials publicly
              <br />
              • Rotate credentials regularly
              <br />
              • Use X.509 certificates for production deployments
              <br />• Monitor device activity for suspicious behavior
            </Typography>
          </Box>
        </Box>
      </DialogContent>

      <Divider />

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          onClick={handleSave}
          variant="contained"
          disabled={
            (credentialsType === DeviceCredentialsType.ACCESS_TOKEN && !accessToken) ||
            (credentialsType === DeviceCredentialsType.MQTT_BASIC && (!clientId || !username || !password)) ||
            (credentialsType === DeviceCredentialsType.X509_CERTIFICATE && !certificate)
          }
        >
          Save Credentials
        </Button>
      </DialogActions>
    </Dialog>
  )
}
