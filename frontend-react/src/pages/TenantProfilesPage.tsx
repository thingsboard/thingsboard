import { useState, useEffect } from 'react'
import {
  Box,
  Paper,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  TextField,
  InputAdornment,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  BusinessCenter as ProfileIcon,
} from '@mui/icons-material'
import MainLayout from '@/components/layout/MainLayout'

interface TenantProfile {
  id: string
  name: string
  description: string
  isolated: boolean
  maxDevices: number
  maxAssets: number
  maxCustomers: number
  maxUsers: number
  maxDashboards: number
  maxRuleChains: number
}

export default function TenantProfilesPage() {
  const [profiles, setProfiles] = useState<TenantProfile[]>([
    {
      id: '1',
      name: 'Default',
      description: 'Default tenant profile',
      isolated: false,
      maxDevices: 0,
      maxAssets: 0,
      maxCustomers: 0,
      maxUsers: 0,
      maxDashboards: 0,
      maxRuleChains: 0,
    },
    {
      id: '2',
      name: 'Enterprise',
      description: 'Enterprise tenant profile with high limits',
      isolated: true,
      maxDevices: 10000,
      maxAssets: 5000,
      maxCustomers: 1000,
      maxUsers: 500,
      maxDashboards: 200,
      maxRuleChains: 50,
    },
    {
      id: '3',
      name: 'Starter',
      description: 'Starter tenant profile with low limits',
      isolated: false,
      maxDevices: 100,
      maxAssets: 50,
      maxCustomers: 10,
      maxUsers: 10,
      maxDashboards: 20,
      maxRuleChains: 5,
    },
  ])

  const [searchTerm, setSearchTerm] = useState('')

  const filteredProfiles = profiles.filter(
    (profile) =>
      profile.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      profile.description.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const handleAdd = () => {
    console.log('Add new tenant profile')
  }

  const handleEdit = (profile: TenantProfile) => {
    console.log('Edit tenant profile:', profile)
  }

  const handleDelete = (id: string) => {
    console.log('Delete tenant profile:', id)
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h4" sx={{ color: '#0F3E5C', fontWeight: 600 }}>
            Tenant Profiles - Payvar
          </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleAdd} sx={{ bgcolor: '#0F3E5C' }}>
          Add Tenant Profile
        </Button>
      </Box>

      <Paper sx={{ p: 2, mb: 2 }}>
        <TextField
          fullWidth
          placeholder="Search tenant profiles..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon sx={{ color: '#8C959D' }} />
              </InputAdornment>
            ),
          }}
        />
      </Paper>

      <Paper sx={{ p: 0 }}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: '#F5F5F5' }}>
                <TableCell sx={{ fontWeight: 600 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <ProfileIcon fontSize="small" />
                    Name
                  </Box>
                </TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Description</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Isolation</TableCell>
                <TableCell align="center" sx={{ fontWeight: 600 }}>
                  Max Devices
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 600 }}>
                  Max Assets
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 600 }}>
                  Max Customers
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 600 }}>
                  Max Users
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: 600 }}>
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredProfiles.map((profile) => (
                <TableRow key={profile.id} hover>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      {profile.name}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary">
                      {profile.description}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={profile.isolated ? 'Isolated' : 'Shared'}
                      size="small"
                      color={profile.isolated ? 'primary' : 'default'}
                    />
                  </TableCell>
                  <TableCell align="center">
                    {profile.maxDevices === 0 ? (
                      <Chip label="Unlimited" size="small" color="success" />
                    ) : (
                      profile.maxDevices.toLocaleString()
                    )}
                  </TableCell>
                  <TableCell align="center">
                    {profile.maxAssets === 0 ? (
                      <Chip label="Unlimited" size="small" color="success" />
                    ) : (
                      profile.maxAssets.toLocaleString()
                    )}
                  </TableCell>
                  <TableCell align="center">
                    {profile.maxCustomers === 0 ? (
                      <Chip label="Unlimited" size="small" color="success" />
                    ) : (
                      profile.maxCustomers.toLocaleString()
                    )}
                  </TableCell>
                  <TableCell align="center">
                    {profile.maxUsers === 0 ? (
                      <Chip label="Unlimited" size="small" color="success" />
                    ) : (
                      profile.maxUsers.toLocaleString()
                    )}
                  </TableCell>
                  <TableCell align="right">
                    <IconButton
                      size="small"
                      onClick={() => handleEdit(profile)}
                      sx={{ color: '#0F3E5C', mr: 1 }}
                    >
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => handleDelete(profile.id)}
                      sx={{ color: '#C62828' }}
                      disabled={profile.name === 'Default'}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          About Tenant Profiles
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          Tenant profiles define resource quotas and isolation settings for tenants. Each tenant must be assigned to a
          tenant profile that determines:
        </Typography>
        <Typography variant="body2" color="text.secondary" component="div">
          <ul>
            <li>Maximum number of devices, assets, customers, and users</li>
            <li>Maximum number of dashboards and rule chains</li>
            <li>Rate limits for API calls and data ingestion</li>
            <li>Isolation mode (shared vs isolated resources)</li>
          </ul>
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
          A value of 0 means unlimited quota for that resource.
        </Typography>
      </Paper>
      </Box>
    </MainLayout>
  )
}
