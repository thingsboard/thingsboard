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
import { selectIsAuthenticated } from '@/store/auth/authSlice'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route
        path="/dashboard"
        element={
          <PrivateRoute>
            <DashboardPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/devices"
        element={
          <PrivateRoute>
            <DevicesPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/devices/:deviceId"
        element={
          <PrivateRoute>
            <DeviceDetailsPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/assets"
        element={
          <PrivateRoute>
            <AssetsPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/assets/:assetId"
        element={
          <PrivateRoute>
            <AssetDetailsPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/customers"
        element={
          <PrivateRoute>
            <CustomersPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/users"
        element={
          <PrivateRoute>
            <UsersPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/tenants"
        element={
          <PrivateRoute>
            <TenantsPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/alarms"
        element={
          <PrivateRoute>
            <AlarmsPage />
          </PrivateRoute>
        }
      />
      {/* More routes will be added here */}
    </Routes>
  )
}

export default App
