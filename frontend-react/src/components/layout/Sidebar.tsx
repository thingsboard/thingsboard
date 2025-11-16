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
  Collapse,
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
  Widgets as WidgetsIcon,
  History as AuditIcon,
  Logout as LogoutIcon,
  Settings as SettingsIcon,
  ExpandLess,
  ExpandMore,
  BusinessCenter as TenantProfileIcon,
  Storage as QueueIcon,
  Public as GeneralIcon,
  Email as EmailIcon,
  Sms as SmsIcon,
  Security as SecurityIcon,
} from '@mui/icons-material'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { logout, selectCurrentUser } from '@/store/auth/authSlice'

const DRAWER_WIDTH = 256 // 64 * 4 = 256px (w-64 in Tailwind)

type UserRole = 'SYS_ADMIN' | 'TENANT_ADMIN' | 'CUSTOMER_USER'

interface NavItem {
  text: string
  icon: React.ReactNode
  path: string
  badge?: number
  roles: UserRole[] // Which roles can see this item
}

const navItems: NavItem[] = [
  // System Administrator only
  {
    text: 'Tenants',
    icon: <TenantsIcon />,
    path: '/tenants',
    roles: ['SYS_ADMIN']
  },
  {
    text: 'Tenant Profiles',
    icon: <TenantProfileIcon />,
    path: '/tenant-profiles',
    roles: ['SYS_ADMIN']
  },
  {
    text: 'Queues',
    icon: <QueueIcon />,
    path: '/queues',
    roles: ['SYS_ADMIN']
  },
  {
    text: 'Audit Logs',
    icon: <AuditIcon />,
    path: '/audit-logs',
    roles: ['SYS_ADMIN', 'TENANT_ADMIN']
  },

  // Tenant Administrator and Customer User
  {
    text: 'Dashboards',
    icon: <DashboardIcon />,
    path: '/dashboard',
    roles: ['TENANT_ADMIN', 'CUSTOMER_USER']
  },
  {
    text: 'Devices',
    icon: <DevicesIcon />,
    path: '/devices',
    roles: ['TENANT_ADMIN', 'CUSTOMER_USER']
  },
  {
    text: 'Assets',
    icon: <AssetsIcon />,
    path: '/assets',
    roles: ['TENANT_ADMIN', 'CUSTOMER_USER']
  },
  {
    text: 'Alarms',
    icon: <AlarmsIcon />,
    path: '/alarms',
    badge: 3,
    roles: ['TENANT_ADMIN', 'CUSTOMER_USER']
  },

  // Tenant Administrator only
  {
    text: 'Gateways',
    icon: <GatewaysIcon />,
    path: '/gateways',
    roles: ['TENANT_ADMIN']
  },
  {
    text: 'Customers',
    icon: <CustomersIcon />,
    path: '/customers',
    roles: ['TENANT_ADMIN']
  },
  {
    text: 'Users',
    icon: <UsersIcon />,
    path: '/users',
    roles: ['TENANT_ADMIN', 'CUSTOMER_USER']
  },
  {
    text: 'Rule Engine',
    icon: <RuleEngineIcon />,
    path: '/rule-chains',
    roles: ['TENANT_ADMIN']
  },
  {
    text: 'Widget Library',
    icon: <WidgetsIcon />,
    path: '/widgets-bundles',
    roles: ['TENANT_ADMIN']
  },
]

const settingsItems: NavItem[] = [
  {
    text: 'General',
    icon: <GeneralIcon />,
    path: '/settings/general',
    roles: ['SYS_ADMIN']
  },
  {
    text: 'Mail Server',
    icon: <EmailIcon />,
    path: '/settings/mail-server',
    roles: ['SYS_ADMIN']
  },
  {
    text: 'SMS Provider',
    icon: <SmsIcon />,
    path: '/settings/sms-provider',
    roles: ['SYS_ADMIN']
  },
  {
    text: 'Security',
    icon: <SecurityIcon />,
    path: '/settings/security',
    roles: ['SYS_ADMIN']
  },
]

export default function Sidebar() {
  const navigate = useNavigate()
  const location = useLocation()
  const dispatch = useAppDispatch()
  const currentUser = useAppSelector(selectCurrentUser)
  const [settingsOpen, setSettingsOpen] = useState(false)

  const handleNavigation = (path: string) => {
    navigate(path)
  }

  const handleLogout = async () => {
    await dispatch(logout())
    navigate('/login')
  }

  const handleSettingsToggle = () => {
    setSettingsOpen(!settingsOpen)
  }

  // Filter navigation items based on user role
  const filteredNavItems = navItems.filter((item) => {
    if (!currentUser?.authority) return false
    return item.roles.includes(currentUser.authority as UserRole)
  })

  // Filter settings items based on user role
  const filteredSettingsItems = settingsItems.filter((item) => {
    if (!currentUser?.authority) return false
    return item.roles.includes(currentUser.authority as UserRole)
  })

  // Check if current route is a settings page
  const isSettingsRoute = location.pathname.startsWith('/settings/')
  const hasSettingsAccess = filteredSettingsItems.length > 0

  // Get role display name
  const getRoleDisplayName = (authority?: string) => {
    switch (authority) {
      case 'SYS_ADMIN':
        return 'System Administrator'
      case 'TENANT_ADMIN':
        return 'Tenant Administrator'
      case 'CUSTOMER_USER':
        return 'Customer User'
      default:
        return 'User'
    }
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

        {/* User Profile Section */}
        {currentUser && (
          <Box
            sx={{
              bgcolor: 'rgba(255, 255, 255, 0.05)',
              borderRadius: 1,
              p: 1.5,
              mb: 2,
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <Avatar
                sx={{
                  width: 36,
                  height: 36,
                  bgcolor: '#FFB300',
                  color: '#0F3E5C',
                  fontSize: '0.875rem',
                  fontWeight: 'bold',
                }}
              >
                {currentUser.first_name?.[0]}
                {currentUser.last_name?.[0]}
              </Avatar>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography
                  variant="body2"
                  sx={{
                    color: 'white',
                    fontWeight: 500,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {currentUser.first_name} {currentUser.last_name}
                </Typography>
                <Typography
                  variant="caption"
                  sx={{
                    color: '#8C959D',
                    display: 'block',
                    fontSize: '0.7rem',
                  }}
                >
                  {getRoleDisplayName(currentUser.authority)}
                </Typography>
              </Box>
            </Box>
          </Box>
        )}

        <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.1)', mb: 2 }} />

        {/* Navigation Items */}
        <List sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 1 }}>
          {filteredNavItems.map((item) => {
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

          {/* Settings Menu (expandable) */}
          {hasSettingsAccess && (
            <>
              <ListItem disablePadding>
                <ListItemButton
                  onClick={handleSettingsToggle}
                  sx={{
                    borderRadius: 1,
                    bgcolor: isSettingsRoute ? 'rgba(255, 255, 255, 0.1)' : 'transparent',
                    color: isSettingsRoute ? '#FFB300' : '#8C959D',
                    '&:hover': {
                      bgcolor: 'rgba(255, 255, 255, 0.1)',
                      color: 'white',
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}>
                    <SettingsIcon />
                  </ListItemIcon>
                  <ListItemText
                    primary="Settings"
                    primaryTypographyProps={{
                      fontSize: '0.875rem',
                      fontWeight: 500,
                    }}
                  />
                  {settingsOpen ? <ExpandLess /> : <ExpandMore />}
                </ListItemButton>
              </ListItem>

              <Collapse in={settingsOpen} timeout="auto" unmountOnExit>
                <List component="div" disablePadding>
                  {filteredSettingsItems.map((item) => {
                    const isActive = location.pathname === item.path
                    return (
                      <ListItem key={item.text} disablePadding>
                        <ListItemButton
                          onClick={() => handleNavigation(item.path)}
                          sx={{
                            pl: 4,
                            borderRadius: 1,
                            bgcolor: isActive ? 'rgba(255, 255, 255, 0.1)' : 'transparent',
                            color: isActive ? '#FFB300' : '#8C959D',
                            '&:hover': {
                              bgcolor: 'rgba(255, 255, 255, 0.1)',
                              color: 'white',
                            },
                          }}
                        >
                          <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}>
                            {item.icon}
                          </ListItemIcon>
                          <ListItemText
                            primary={item.text}
                            primaryTypographyProps={{
                              fontSize: '0.8125rem',
                              fontWeight: 500,
                            }}
                          />
                        </ListItemButton>
                      </ListItem>
                    )
                  })}
                </List>
              </Collapse>
            </>
          )}
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
