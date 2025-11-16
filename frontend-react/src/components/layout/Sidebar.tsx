import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Badge,
  Avatar,
  Divider,
} from '@mui/material'
import {
  Dashboard as DashboardIcon,
  Memory as DevicesIcon,
  Inventory2 as AssetsIcon,
  NotificationsActive as AlarmsIcon,
  Rule as RuleEngineIcon,
  Business as CustomersIcon,
  People as UsersIcon,
  Domain as TenantsIcon,
  Router as GatewaysIcon,
  Logout as LogoutIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { logout, selectCurrentUser } from '@/store/auth/authSlice'

const DRAWER_WIDTH = 256 // 64 * 4 = 256px (w-64 in Tailwind)

interface NavItem {
  text: string
  icon: React.ReactNode
  path: string
  badge?: number
}

const navItems: NavItem[] = [
  { text: 'Dashboards', icon: <DashboardIcon />, path: '/dashboard' },
  { text: 'Devices', icon: <DevicesIcon />, path: '/devices' },
  { text: 'Assets', icon: <AssetsIcon />, path: '/assets' },
  { text: 'Gateways', icon: <GatewaysIcon />, path: '/gateways' },
  { text: 'Customers', icon: <CustomersIcon />, path: '/customers' },
  { text: 'Users', icon: <UsersIcon />, path: '/users' },
  { text: 'Tenants', icon: <TenantsIcon />, path: '/tenants' },
  { text: 'Alarms', icon: <AlarmsIcon />, path: '/alarms', badge: 3 },
  { text: 'Rule Engine', icon: <RuleEngineIcon />, path: '/rule-chains' },
]

export default function Sidebar() {
  const navigate = useNavigate()
  const location = useLocation()
  const dispatch = useAppDispatch()
  const currentUser = useAppSelector(selectCurrentUser)

  const handleNavigation = (path: string) => {
    navigate(path)
  }

  const handleLogout = async () => {
    await dispatch(logout())
    navigate('/login')
  }

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: DRAWER_WIDTH,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: DRAWER_WIDTH,
          boxSizing: 'border-box',
          bgcolor: '#0F3E5C', // primary color from Payvar design
          color: 'white',
          borderRight: 'none',
        },
      }}
    >
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          height: '100%',
          p: 2,
        }}
      >
        {/* Logo Section */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, px: 1, mb: 3 }}>
          <Avatar
            src="https://lh3.googleusercontent.com/aida-public/AB6AXuAwfIeVXC0OZqjcC4koBMeTlahz-rpA61oOD6cfcllT_9S1WFRcwlb8dvggoUSZro_WfeeXf6K5Ksw4jhRmtkpmwA0LmX59MYcJx0PiR94iF_WwZWbXicRjmicHLinYPqtRXgDctRG1akibRnQLF5oBb653NM7anGpJhRglSIv62icuHvyxWGQDzL2c8Vljee_lVQ8vrgiVHpKmJx34UAH4gzTGFWfIW6MLQNtY_kJpRZpjbZdezSIVgcgOEBlY2RVqIKPpVhqgsD4"
            alt="Payvar logo"
            sx={{ width: 40, height: 40 }}
          />
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 'bold', color: 'white' }}>
              Payvar
            </Typography>
            <Typography variant="caption" sx={{ color: '#8C959D' }}>
              Industrial IoT Platform
            </Typography>
          </Box>
        </Box>

        {/* Navigation Items */}
        <List sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 1 }}>
          {navItems.map((item) => {
            const isActive = location.pathname === item.path
            return (
              <ListItem key={item.text} disablePadding>
                <ListItemButton
                  onClick={() => handleNavigation(item.path)}
                  sx={{
                    borderRadius: 1,
                    bgcolor: isActive ? 'rgba(255, 255, 255, 0.1)' : 'transparent',
                    color: isActive ? '#FFB300' : '#8C959D', // accent or secondary
                    '&:hover': {
                      bgcolor: 'rgba(255, 255, 255, 0.1)',
                      color: 'white',
                    },
                  }}
                >
                  <ListItemIcon
                    sx={{
                      color: 'inherit',
                      minWidth: 40,
                    }}
                  >
                    {item.badge ? (
                      <Badge badgeContent={item.badge} color="error">
                        {item.icon}
                      </Badge>
                    ) : (
                      item.icon
                    )}
                  </ListItemIcon>
                  <ListItemText
                    primary={item.text}
                    primaryTypographyProps={{
                      fontSize: '0.875rem',
                      fontWeight: 500,
                    }}
                  />
                  {item.badge && !isActive && (
                    <Box
                      sx={{
                        bgcolor: '#C62828', // danger color
                        color: 'white',
                        borderRadius: '9999px',
                        width: 20,
                        height: 20,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '0.75rem',
                        fontWeight: 'bold',
                      }}
                    >
                      {item.badge}
                    </Box>
                  )}
                </ListItemButton>
              </ListItem>
            )
          })}
        </List>

        {/* Logout Section */}
        <List sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          <ListItem disablePadding>
            <ListItemButton
              onClick={handleLogout}
              sx={{
                borderRadius: 1,
                color: '#8C959D',
                '&:hover': {
                  bgcolor: 'rgba(255, 255, 255, 0.1)',
                  color: 'white',
                },
              }}
            >
              <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}>
                <LogoutIcon />
              </ListItemIcon>
              <ListItemText
                primary="Log Out"
                primaryTypographyProps={{
                  fontSize: '0.875rem',
                  fontWeight: 500,
                }}
              />
            </ListItemButton>
          </ListItem>
        </List>
      </Box>
    </Drawer>
  )
}
