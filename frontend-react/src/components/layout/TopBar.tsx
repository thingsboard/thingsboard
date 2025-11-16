import { useState } from 'react'
import {
  AppBar,
  Toolbar,
  Box,
  TextField,
  IconButton,
  Badge,
  Avatar,
  Typography,
  Divider,
  InputAdornment,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
} from '@mui/material'
import {
  Search as SearchIcon,
  Notifications as NotificationsIcon,
  Settings as SettingsIcon,
  Person as PersonIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material'
import { useAppSelector, useAppDispatch } from '@/hooks/redux'
import { selectCurrentUser, logout } from '@/store/auth/authSlice'
import { useNavigate } from 'react-router-dom'

const DRAWER_WIDTH = 256

export default function TopBar() {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const currentUser = useAppSelector(selectCurrentUser)
  const [searchQuery, setSearchQuery] = useState('')
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)

  const handleUserMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleUserMenuClose = () => {
    setAnchorEl(null)
  }

  const handleLogout = async () => {
    handleUserMenuClose()
    await dispatch(logout())
    navigate('/login')
  }

  const handleSettings = () => {
    handleUserMenuClose()
    navigate('/settings')
  }

  const handleProfile = () => {
    handleUserMenuClose()
    navigate('/profile')
  }

  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        width: `calc(100% - ${DRAWER_WIDTH}px)`,
        ml: `${DRAWER_WIDTH}px`,
        bgcolor: (theme) =>
          theme.palette.mode === 'dark' ? '#1F2428' : '#FFFFFF',
        borderBottom: (theme) =>
          `1px solid ${theme.palette.mode === 'dark' ? '#363C42' : '#E0E0E0'}`,
      }}
    >
      <Toolbar sx={{ justifyContent: 'space-between' }}>
        {/* Search Bar */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          <TextField
            size="small"
            placeholder="Search for devices, assets, or alarms..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            sx={{
              width: 320,
              '& .MuiOutlinedInput-root': {
                bgcolor: (theme) =>
                  theme.palette.mode === 'dark' ? '#151a1d' : '#F3F4F6',
              },
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon sx={{ color: '#8C959D' }} />
                </InputAdornment>
              ),
            }}
          />
        </Box>

        {/* Right Side Actions */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {/* Notifications */}
          <IconButton
            sx={{
              color: '#8C959D',
              '&:hover': {
                bgcolor: (theme) =>
                  theme.palette.mode === 'dark' ? '#151a1d' : '#F3F4F6',
                color: (theme) =>
                  theme.palette.mode === 'dark' ? '#E1E3E5' : '#121517',
              },
            }}
          >
            <Badge
              badgeContent={3}
              color="error"
              variant="dot"
              sx={{
                '& .MuiBadge-dot': {
                  border: (theme) =>
                    `2px solid ${theme.palette.mode === 'dark' ? '#1F2428' : '#FFFFFF'}`,
                },
              }}
            >
              <NotificationsIcon />
            </Badge>
          </IconButton>

          {/* Settings */}
          <IconButton
            onClick={handleSettings}
            sx={{
              color: '#8C959D',
              '&:hover': {
                bgcolor: (theme) =>
                  theme.palette.mode === 'dark' ? '#151a1d' : '#F3F4F6',
                color: (theme) =>
                  theme.palette.mode === 'dark' ? '#E1E3E5' : '#121517',
              },
            }}
          >
            <SettingsIcon />
          </IconButton>

          {/* Divider */}
          <Divider
            orientation="vertical"
            flexItem
            sx={{
              bgcolor: (theme) =>
                theme.palette.mode === 'dark' ? '#363C42' : '#E0E0E0',
            }}
          />

          {/* User Profile */}
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1.5,
              cursor: 'pointer',
              '&:hover': {
                opacity: 0.8,
              },
            }}
            onClick={handleUserMenuOpen}
          >
            <Avatar
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuBUobXf8q3VCHcHcE8BUneOSzt0dfZtbvWltlo_yjHy9c1tEe5HD6K7emlXl2_A_UOqpLNtsK2PqayQqv_1XkaP0RcW99XIbNihFm26Wynxt7MWv57VsrX5OcsABfzk8mWXYt2_fm3bRXidtgImH2VM0ANUbphsGywEdPOMyNhhtJmvLH6fCPBwuJJUGrvLsoA5CUp98vvnACrhkKtsT6I4eMwD1JHLOy-HsKkbmQSsDqqCCKgJID7Ga9o2YU1NjBBLtPbANON-fTA"
              alt="User avatar"
              sx={{ width: 40, height: 40 }}
            />
            <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
              <Typography
                variant="body2"
                sx={{
                  fontWeight: 600,
                  color: (theme) =>
                    theme.palette.mode === 'dark' ? '#E1E3E5' : '#121517',
                }}
              >
                {currentUser?.first_name} {currentUser?.last_name}
              </Typography>
              <Typography variant="caption" sx={{ color: '#8C959D' }}>
                {currentUser?.authority === 'SYS_ADMIN'
                  ? 'System Administrator'
                  : currentUser?.authority === 'TENANT_ADMIN'
                  ? 'Tenant Administrator'
                  : 'Customer User'}
              </Typography>
            </Box>
          </Box>

          {/* User Menu */}
          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleUserMenuClose}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
          >
            <MenuItem onClick={handleProfile}>
              <ListItemIcon>
                <PersonIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText>Profile</ListItemText>
            </MenuItem>
            <MenuItem onClick={handleSettings}>
              <ListItemIcon>
                <SettingsIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText>Settings</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText>Logout</ListItemText>
            </MenuItem>
          </Menu>
        </Box>
      </Toolbar>
    </AppBar>
  )
}
