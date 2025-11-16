import { Routes, Route, Navigate } from 'react-router-dom'
import { useAppSelector } from '@/hooks/redux'
import LoginPage from '@/pages/LoginPage'
import DashboardPage from '@/pages/DashboardPage'
import DevicesPage from '@/pages/DevicesPage'
import DeviceDetailsPage from '@/pages/DeviceDetailsPage'
import AssetsPage from '@/pages/AssetsPage'
import AssetDetailsPage from '@/pages/AssetDetailsPage'
import CustomersPage from '@/pages/CustomersPage'
import UsersPage from '@/pages/UsersPage'
import TenantsPage from '@/pages/TenantsPage'
import AlarmsPage from '@/pages/AlarmsPage'
import RuleChainsPage from '@/pages/RuleChainsPage'
import RuleChainDesignerPage from '@/pages/RuleChainDesignerPage'
import GatewaysPage from '@/pages/GatewaysPage'
import GatewayDetailsPage from '@/pages/GatewayDetailsPage'
import WidgetsBundlesPage from '@/pages/WidgetsBundlesPage'
import AuditLogsPage from '@/pages/AuditLogsPage'
import TenantProfilesPage from '@/pages/TenantProfilesPage'
import QueueManagementPage from '@/pages/QueueManagementPage'
import GeneralSettingsPage from '@/pages/settings/GeneralSettingsPage'
import MailServerPage from '@/pages/settings/MailServerPage'
import SmsProviderPage from '@/pages/settings/SmsProviderPage'
import SecuritySettingsPage from '@/pages/settings/SecuritySettingsPage'
import { selectIsAuthenticated, selectCurrentUser } from '@/store/auth/authSlice'

type UserRole = 'SYS_ADMIN' | 'TENANT_ADMIN' | 'CUSTOMER_USER'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

function RoleBasedRoute({
  children,
  allowedRoles,
}: {
  children: React.ReactNode
  allowedRoles: UserRole[]
}) {
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  const currentUser = useAppSelector(selectCurrentUser)

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (!currentUser?.authority || !allowedRoles.includes(currentUser.authority as UserRole)) {
    // Redirect to appropriate default page based on user role
    switch (currentUser?.authority) {
      case 'SYS_ADMIN':
        return <Navigate to="/tenants" replace />
      case 'TENANT_ADMIN':
      case 'CUSTOMER_USER':
        return <Navigate to="/dashboard" replace />
      default:
        return <Navigate to="/login" replace />
    }
  }

  return <>{children}</>
}

function RoleBasedDefaultRedirect() {
  const currentUser = useAppSelector(selectCurrentUser)
  const isAuthenticated = useAppSelector(selectIsAuthenticated)

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  // Redirect based on user role
  switch (currentUser?.authority) {
    case 'SYS_ADMIN':
      return <Navigate to="/tenants" replace />
    case 'TENANT_ADMIN':
    case 'CUSTOMER_USER':
      return <Navigate to="/dashboard" replace />
    default:
      return <Navigate to="/login" replace />
  }
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RoleBasedDefaultRedirect />} />

      {/* Dashboard - TENANT_ADMIN and CUSTOMER_USER */}
      <Route
        path="/dashboard"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN', 'CUSTOMER_USER']}>
            <DashboardPage />
          </RoleBasedRoute>
        }
      />

      {/* Devices - TENANT_ADMIN and CUSTOMER_USER */}
      <Route
        path="/devices"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN', 'CUSTOMER_USER']}>
            <DevicesPage />
          </RoleBasedRoute>
        }
      />
      <Route
        path="/devices/:deviceId"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN', 'CUSTOMER_USER']}>
            <DeviceDetailsPage />
          </RoleBasedRoute>
        }
      />

      {/* Assets - TENANT_ADMIN and CUSTOMER_USER */}
      <Route
        path="/assets"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN', 'CUSTOMER_USER']}>
            <AssetsPage />
          </RoleBasedRoute>
        }
      />
      <Route
        path="/assets/:assetId"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN', 'CUSTOMER_USER']}>
            <AssetDetailsPage />
          </RoleBasedRoute>
        }
      />

      {/* Alarms - TENANT_ADMIN and CUSTOMER_USER */}
      <Route
        path="/alarms"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN', 'CUSTOMER_USER']}>
            <AlarmsPage />
          </RoleBasedRoute>
        }
      />

      {/* Customers - TENANT_ADMIN only */}
      <Route
        path="/customers"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN']}>
            <CustomersPage />
          </RoleBasedRoute>
        }
      />

      {/* Users - TENANT_ADMIN and CUSTOMER_USER */}
      <Route
        path="/users"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN', 'CUSTOMER_USER']}>
            <UsersPage />
          </RoleBasedRoute>
        }
      />

      {/* Gateways - TENANT_ADMIN only */}
      <Route
        path="/gateways"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN']}>
            <GatewaysPage />
          </RoleBasedRoute>
        }
      />
      <Route
        path="/gateways/:id"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN']}>
            <GatewayDetailsPage />
          </RoleBasedRoute>
        }
      />

      {/* Rule Chains - TENANT_ADMIN only */}
      <Route
        path="/rule-chains"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN']}>
            <RuleChainsPage />
          </RoleBasedRoute>
        }
      />
      <Route
        path="/rule-chains/:id"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN']}>
            <RuleChainDesignerPage />
          </RoleBasedRoute>
        }
      />

      {/* Widget Library - TENANT_ADMIN only */}
      <Route
        path="/widgets-bundles"
        element={
          <RoleBasedRoute allowedRoles={['TENANT_ADMIN']}>
            <WidgetsBundlesPage />
          </RoleBasedRoute>
        }
      />

      {/* Tenants - SYS_ADMIN only */}
      <Route
        path="/tenants"
        element={
          <RoleBasedRoute allowedRoles={['SYS_ADMIN']}>
            <TenantsPage />
          </RoleBasedRoute>
        }
      />

      {/* Audit Logs - SYS_ADMIN and TENANT_ADMIN */}
      <Route
        path="/audit-logs"
        element={
          <RoleBasedRoute allowedRoles={['SYS_ADMIN', 'TENANT_ADMIN']}>
            <AuditLogsPage />
          </RoleBasedRoute>
        }
      />

      {/* Tenant Profiles - SYS_ADMIN only */}
      <Route
        path="/tenant-profiles"
        element={
          <RoleBasedRoute allowedRoles={['SYS_ADMIN']}>
            <TenantProfilesPage />
          </RoleBasedRoute>
        }
      />

      {/* Queue Management - SYS_ADMIN only */}
      <Route
        path="/queues"
        element={
          <RoleBasedRoute allowedRoles={['SYS_ADMIN']}>
            <QueueManagementPage />
          </RoleBasedRoute>
        }
      />

      {/* System Settings Pages - SYS_ADMIN only */}
      <Route
        path="/settings/general"
        element={
          <RoleBasedRoute allowedRoles={['SYS_ADMIN']}>
            <GeneralSettingsPage />
          </RoleBasedRoute>
        }
      />
      <Route
        path="/settings/mail-server"
        element={
          <RoleBasedRoute allowedRoles={['SYS_ADMIN']}>
            <MailServerPage />
          </RoleBasedRoute>
        }
      />
      <Route
        path="/settings/sms-provider"
        element={
          <RoleBasedRoute allowedRoles={['SYS_ADMIN']}>
            <SmsProviderPage />
          </RoleBasedRoute>
        }
      />
      <Route
        path="/settings/security"
        element={
          <RoleBasedRoute allowedRoles={['SYS_ADMIN']}>
            <SecuritySettingsPage />
          </RoleBasedRoute>
        }
      />
    </Routes>
  )
}

export default App
