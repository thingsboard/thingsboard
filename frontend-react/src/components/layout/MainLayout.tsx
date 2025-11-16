import { Box, Toolbar } from '@mui/material'
import Sidebar from './Sidebar'
import TopBar from './TopBar'

const DRAWER_WIDTH = 256

interface MainLayoutProps {
  children: React.ReactNode
}

export default function MainLayout({ children }: MainLayoutProps) {
  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      {/* Sidebar */}
      <Sidebar />

      {/* Main Content Area */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          display: 'flex',
          flexDirection: 'column',
          width: `calc(100% - ${DRAWER_WIDTH}px)`,
          height: '100vh',
          overflow: 'hidden',
        }}
      >
        {/* TopBar */}
        <TopBar />

        {/* Content */}
        <Box
          sx={{
            flexGrow: 1,
            overflow: 'auto',
            bgcolor: (theme) =>
              theme.palette.mode === 'dark' ? '#151a1d' : '#F3F4F6',
          }}
        >
          {/* Spacer for fixed AppBar */}
          <Toolbar />

          {/* Page Content */}
          <Box sx={{ p: 3 }}>{children}</Box>
        </Box>
      </Box>
    </Box>
  )
}
