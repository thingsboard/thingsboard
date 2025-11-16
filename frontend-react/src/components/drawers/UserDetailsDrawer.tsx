/**
 * User Details Drawer
 * Right-side slide-in drawer matching ThingsBoard's exact pattern
 * Tabs: Details, Security Settings, Audit logs
 */

import { useState, useEffect } from 'react'
import {
  Box,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Grid,
  Paper,
  Typography,
  Alert,
  FormControlLabel,
  Checkbox,
  InputAdornment,
  IconButton,
  Button,
  Divider,
} from '@mui/material'
import {
  Person as UserIcon,
  Visibility,
  VisibilityOff,
  VpnKey as KeyIcon,
} from '@mui/icons-material'
import EntityDrawer, { SectionHeader, StatusBadge } from './EntityDrawer'
import { format } from 'date-fns'

interface User {
  id: string
  email: string
  firstName?: string
  lastName?: string
  authority: 'SYS_ADMIN' | 'TENANT_ADMIN' | 'CUSTOMER_USER'
  customerId?: string
  customerTitle?: string
  enabled?: boolean
  createdTime: number
  additionalInfo?: any
}

interface UserDetailsDrawerProps {
  open: boolean
  onClose: () => void
  user: User | null
  onSave?: (user: User) => void
  onDelete?: (userId: string) => void
  onActivate?: (userId: string, enabled: boolean) => void
  onResendActivation?: (userId: string) => void
  mode?: 'view' | 'edit' | 'create'
  currentUserAuthority?: 'SYS_ADMIN' | 'TENANT_ADMIN' | 'CUSTOMER_USER'
}

const USER_AUTHORITIES = [
  { value: 'TENANT_ADMIN', label: 'Tenant Administrator' },
  { value: 'CUSTOMER_USER', label: 'Customer User' },
]

const SYS_ADMIN_AUTHORITIES = [
  { value: 'SYS_ADMIN', label: 'System Administrator' },
  ...USER_AUTHORITIES,
]

export default function UserDetailsDrawer({
  open,
  onClose,
  user: initialUser,
  onSave,
  onDelete,
  onActivate,
  onResendActivation,
  mode: initialMode = 'view',
  currentUserAuthority = 'TENANT_ADMIN',
}: UserDetailsDrawerProps) {
  const [mode, setMode] = useState<'view' | 'edit' | 'create'>(initialMode)
  const [user, setUser] = useState<User | null>(initialUser)
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  useEffect(() => {
    setUser(initialUser)
    setMode(initialMode)
    setPassword('')
    setConfirmPassword('')
  }, [initialUser, initialMode])

  const handleSave = () => {
    if (user && onSave) {
      // Validate passwords match for create mode
      if (mode === 'create' && password !== confirmPassword) {
        alert('Passwords do not match')
        return
      }

      setLoading(true)
      onSave(user)
      setTimeout(() => {
        setLoading(false)
      }, 500)
    }
  }

  const handleDelete = () => {
    if (user && onDelete) {
      onDelete(user.id)
    }
  }

  const handleActivate = () => {
    if (user && onActivate) {
      onActivate(user.id, !user.enabled)
      setUser({ ...user, enabled: !user.enabled })
    }
  }

  const handleResendActivation = () => {
    if (user && onResendActivation) {
      onResendActivation(user.id)
    }
  }

  const handleCopy = () => {
    alert('Copy user functionality')
  }

  if (!user) return null

  const authorities = currentUserAuthority === 'SYS_ADMIN' ? SYS_ADMIN_AUTHORITIES : USER_AUTHORITIES

  // Details Tab
  const detailsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <SectionHeader title="User Information" />
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Email"
              required
              fullWidth
              type="email"
              value={user.email || ''}
              onChange={(e) => setUser({ ...user, email: e.target.value })}
              disabled={mode === 'view'}
              helperText="User login email address"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <FormControl fullWidth disabled={mode === 'view'}>
              <InputLabel>Authority *</InputLabel>
              <Select
                value={user.authority || 'CUSTOMER_USER'}
                onChange={(e) => setUser({ ...user, authority: e.target.value as User['authority'] })}
                label="Authority *"
              >
                {authorities.map((auth) => (
                  <MenuItem key={auth.value} value={auth.value}>
                    {auth.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="First Name"
              fullWidth
              value={user.firstName || ''}
              onChange={(e) => setUser({ ...user, firstName: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Last Name"
              fullWidth
              value={user.lastName || ''}
              onChange={(e) => setUser({ ...user, lastName: e.target.value })}
              disabled={mode === 'view'}
            />
          </Grid>

          {/* Password fields for create mode */}
          {mode === 'create' && (
            <>
              <Grid item xs={12}>
                <Divider sx={{ my: 2 }} />
                <Typography variant="subtitle1" sx={{ mb: 2, color: '#0F3E5C', fontWeight: 600 }}>
                  Password
                </Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField
                  label="Password"
                  required
                  fullWidth
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton onClick={() => setShowPassword(!showPassword)} edge="end">
                          {showPassword ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  }}
                  helperText="Minimum 6 characters"
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField
                  label="Confirm Password"
                  required
                  fullWidth
                  type={showPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  error={password !== confirmPassword && confirmPassword.length > 0}
                  helperText={
                    password !== confirmPassword && confirmPassword.length > 0
                      ? 'Passwords do not match'
                      : ''
                  }
                />
              </Grid>
            </>
          )}

          <Grid item xs={12}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={user.enabled ?? true}
                  onChange={(e) => setUser({ ...user, enabled: e.target.checked })}
                  disabled={mode === 'view'}
                />
              }
              label="Enabled (user can log in)"
            />
          </Grid>

          {mode === 'view' && (
            <>
              <Grid item xs={12}>
                <Divider sx={{ my: 2 }} />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  User ID
                </Typography>
                <Typography variant="body1">{user.id}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Status
                </Typography>
                <StatusBadge active={user.enabled ?? true} />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Created
                </Typography>
                <Typography variant="body1">
                  {format(new Date(user.createdTime), 'MMM dd, yyyy HH:mm')}
                </Typography>
              </Grid>
              {user.customerTitle && (
                <Grid item xs={12} md={6}>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Customer
                  </Typography>
                  <Typography variant="body1">{user.customerTitle}</Typography>
                </Grid>
              )}
            </>
          )}
        </Grid>

        {/* Activation actions for view mode */}
        {mode === 'view' && (
          <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
            <Button
              variant="outlined"
              color={user.enabled ? 'error' : 'success'}
              onClick={handleActivate}
              startIcon={user.enabled ? <VisibilityOff /> : <Visibility />}
            >
              {user.enabled ? 'Disable User' : 'Enable User'}
            </Button>
            {!user.enabled && onResendActivation && (
              <Button
                variant="outlined"
                onClick={handleResendActivation}
                startIcon={<KeyIcon />}
              >
                Resend Activation Email
              </Button>
            )}
          </Box>
        )}
      </Paper>
    </Box>
  )

  // Security Settings Tab
  const securityTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Security Settings
        </Typography>
        <Alert severity="info" sx={{ mb: 3 }}>
          Change password, two-factor authentication, and API keys will be implemented here
        </Alert>
        <Button variant="outlined" startIcon={<KeyIcon />} disabled>
          Change Password
        </Button>
      </Paper>
    </Box>
  )

  // Audit Logs Tab
  const auditLogsTab = (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 3, color: '#0F3E5C' }}>
          Audit Logs
        </Typography>
        <Alert severity="info">
          User activity audit logs will be implemented
        </Alert>
      </Paper>
    </Box>
  )

  const tabs = [
    { label: 'Details', content: detailsTab },
    { label: 'Security Settings', content: securityTab, disabled: mode === 'create' },
    { label: 'Audit logs', content: auditLogsTab, disabled: mode === 'create' },
  ]

  const getUserTitle = () => {
    if (mode === 'create') return 'Add User'
    return `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email
  }

  const getSubtitle = () => {
    if (mode === 'view') {
      const authLabel = authorities.find(a => a.value === user.authority)?.label || user.authority
      return authLabel
    }
    return undefined
  }

  return (
    <EntityDrawer
      open={open}
      onClose={onClose}
      title={getUserTitle()}
      subtitle={getSubtitle()}
      icon={<UserIcon />}
      tabs={tabs}
      mode={mode}
      onModeChange={setMode}
      onSave={handleSave}
      onDelete={handleDelete}
      onCopy={handleCopy}
      loading={loading}
      width="70%"
    />
  )
}
