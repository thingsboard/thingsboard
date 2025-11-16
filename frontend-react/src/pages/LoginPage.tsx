import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Button,
  TextField,
  Typography,
  Alert,
  IconButton,
  InputAdornment,
  Link,
} from '@mui/material'
import { Visibility, VisibilityOff, Settings } from '@mui/icons-material'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { login, demoLogin, selectAuth } from '@/store/auth/authSlice'

export default function LoginPage() {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const { loading, error } = useAppSelector(selectAuth)

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [language, setLanguage] = useState<'EN' | 'FA'>('EN')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const result = await dispatch(login({ username, password }))
    if (login.fulfilled.match(result)) {
      navigate('/dashboard')
    }
  }

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword)
  }

  const toggleLanguage = () => {
    setLanguage(language === 'EN' ? 'FA' : 'EN')
  }

  const handleDemoLogin = (role: 'SYS_ADMIN' | 'TENANT_ADMIN' | 'CUSTOMER_USER') => {
    dispatch(demoLogin({ authority: role }))

    // Navigate to role-appropriate default page
    switch (role) {
      case 'SYS_ADMIN':
        navigate('/tenants')
        break
      case 'TENANT_ADMIN':
      case 'CUSTOMER_USER':
        navigate('/dashboard')
        break
    }
  }

  return (
    <Box
      sx={{
        position: 'relative',
        minHeight: '100vh',
        width: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        background: (theme) =>
          theme.palette.mode === 'dark'
            ? '#1A202C'
            : '#E9ECEF',
      }}
    >
      {/* Geometric Background Pattern */}
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          opacity: 0.3,
          backgroundImage: (theme) =>
            theme.palette.mode === 'dark'
              ? `url("data:image/svg+xml,%3Csvg width='100%' height='100%' xmlns='http://www.w3.org/2000/svg'%3E%3Cdefs%3E%3Cpattern id='p' width='100' height='100' patternUnits='userSpaceOnUse'%3E%3Cpath d='M50 0L100 25L100 75L50 100L0 75L0 25Z M50 50L0 25 M50 50L0 75 M50 50L100 25 M50 50L100 75 M50 50L50 100 M50 50L50 0' stroke='%234A5568' stroke-width='1' fill='none'/%3E%3Ccircle cx='50' cy='50' r='4' fill='%234A5568'/%3E%3Ccircle cx='0' cy='25' r='4' fill='%234A5568'/%3E%3Ccircle cx='0' cy='75' r='4' fill='%234A5568'/%3E%3Ccircle cx='100' cy='25' r='4' fill='%234A5568'/%3E%3Ccircle cx='100' cy='75' r='4' fill='%234A5568'/%3E%3Ccircle cx='50' cy='0' r='4' fill='%234A5568'/%3E%3Ccircle cx='50' cy='100' r='4' fill='%234A5568'/%3E%3C/pattern%3E%3C/defs%3E%3Crect width='100%' height='100%' fill='url(%23p)'/%3E%3C/svg%3E")`
              : `url("data:image/svg+xml,%3Csvg width='100%' height='100%' xmlns='http://www.w3.org/2000/svg'%3E%3Cdefs%3E%3Cpattern id='p' width='100' height='100' patternUnits='userSpaceOnUse'%3E%3Cpath d='M50 0L100 25L100 75L50 100L0 75L0 25Z M50 50L0 25 M50 50L0 75 M50 50L100 25 M50 50L100 75 M50 50L50 100 M50 50L50 0' stroke='%23CFD8DC' stroke-width='1' fill='none'/%3E%3Ccircle cx='50' cy='50' r='4' fill='%23CFD8DC'/%3E%3Ccircle cx='0' cy='25' r='4' fill='%23CFD8DC'/%3E%3Ccircle cx='0' cy='75' r='4' fill='%23CFD8DC'/%3E%3Ccircle cx='100' cy='25' r='4' fill='%23CFD8DC'/%3E%3Ccircle cx='100' cy='75' r='4' fill='%23CFD8DC'/%3E%3Ccircle cx='50' cy='0' r='4' fill='%23CFD8DC'/%3E%3Ccircle cx='50' cy='100' r='4' fill='%23CFD8DC'/%3E%3C/pattern%3E%3C/defs%3E%3Crect width='100%' height='100%' fill='url(%23p)'/%3E%3C/svg%3E")`,
          backgroundSize: '200px 200px',
        }}
      />

      {/* Main Content */}
      <Box
        sx={{
          position: 'relative',
          zIndex: 10,
          width: '100%',
          maxWidth: '1200px',
          mx: 'auto',
          px: { xs: 2, sm: 3, lg: 4 },
        }}
      >
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
            alignItems: 'center',
            gap: { xs: 6, lg: 12 },
          }}
        >
          {/* Left Column - Illustration (Hidden on mobile) */}
          <Box
            sx={{
              display: { xs: 'none', md: 'flex' },
              flexDirection: 'column',
              alignItems: 'center',
              textAlign: 'center',
            }}
          >
            <Box
              component="img"
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuB4Mf7gNP2b0vj0Wir-rDnDZGGFa1Ib71sZ4BgaSc19pyCjBeBYZ1Gbk-i5UlaGg1h8HYMWOVXfcodNI75MvKgk-4SWWdJnSpFLbWoFLWdu0nIVOIuWX1oV7BH8OKedmP2_U5isxpgc-aNr3uFTggz3LMuvS3feRJY1Nl4Yu0vyhgllcdnomHzFXGP6U6aQNNJguGokR_O65EgfntEPpKSjQTONra3biVE4Y3zxkm1jFRvbRd35XqW11IePmlid5SJtwO4eZMLX7Tw"
              alt="Illustration of an engineer monitoring IoT signals"
              sx={{
                width: '100%',
                maxWidth: '450px',
              }}
            />
            <Typography
              variant="h4"
              sx={{
                mt: 4,
                fontWeight: 'bold',
                color: (theme) =>
                  theme.palette.mode === 'dark'
                    ? 'rgba(255,255,255,0.87)'
                    : 'rgba(0,0,0,0.7)',
              }}
            >
              Intelligent Industrial Solutions
            </Typography>
            <Typography
              variant="body1"
              sx={{
                mt: 1,
                color: (theme) =>
                  theme.palette.mode === 'dark'
                    ? 'rgba(255,255,255,0.6)'
                    : 'rgba(0,0,0,0.6)',
              }}
            >
              Connecting your assets, empowering your decisions.
            </Typography>
          </Box>

          {/* Right Column - Login Form */}
          <Box
            sx={{
              width: '100%',
              maxWidth: '450px',
              mx: 'auto',
            }}
          >
            <Box
              sx={{
                bgcolor: (theme) =>
                  theme.palette.mode === 'dark'
                    ? 'rgba(30, 41, 59, 0.8)'
                    : 'rgba(255, 255, 255, 0.8)',
                backdropFilter: 'blur(10px)',
                boxShadow: 24,
                borderRadius: 2,
                p: { xs: 4, sm: 5 },
              }}
            >
              {/* Header */}
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  mb: 4,
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  {/* Icon/Logo */}
                  <Settings
                    sx={{
                      fontSize: 48,
                      color: '#2D6B9A',
                    }}
                  />
                  <Box>
                    <Typography
                      variant="h4"
                      sx={{
                        fontWeight: 'bold',
                        color: (theme) =>
                          theme.palette.mode === 'dark' ? 'white' : '#111827',
                      }}
                    >
                      Payvar
                    </Typography>
                    <Typography
                      variant="caption"
                      sx={{
                        color: (theme) =>
                          theme.palette.mode === 'dark'
                            ? 'rgba(255,255,255,0.5)'
                            : 'rgba(0,0,0,0.5)',
                      }}
                    >
                      Industrial IOT Platform
                    </Typography>
                  </Box>
                </Box>

                {/* Language Switcher */}
                <Box sx={{ display: 'flex', gap: 0.5, fontSize: '0.875rem' }}>
                  <Link
                    component="button"
                    onClick={toggleLanguage}
                    sx={{
                      color: language === 'FA' ? '#2D6B9A' : 'text.secondary',
                      fontWeight: language === 'FA' ? 'medium' : 'normal',
                      textDecoration: language === 'FA' ? 'underline' : 'none',
                      cursor: 'pointer',
                    }}
                  >
                    FA
                  </Link>
                  <Typography sx={{ color: 'text.secondary' }}>/</Typography>
                  <Link
                    component="button"
                    onClick={toggleLanguage}
                    sx={{
                      color: language === 'EN' ? '#2D6B9A' : 'text.secondary',
                      fontWeight: language === 'EN' ? 'medium' : 'normal',
                      textDecoration: language === 'EN' ? 'underline' : 'none',
                      cursor: 'pointer',
                    }}
                  >
                    EN
                  </Link>
                </Box>
              </Box>

              {/* Error Alert */}
              {error && (
                <Alert severity="error" sx={{ mb: 3 }}>
                  {error}
                </Alert>
              )}

              {/* Login Form */}
              <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <Box>
                  <Typography
                    variant="body2"
                    sx={{
                      fontWeight: 'medium',
                      mb: 0.5,
                      color: (theme) =>
                        theme.palette.mode === 'dark'
                          ? 'rgba(255,255,255,0.7)'
                          : 'rgba(0,0,0,0.7)',
                    }}
                  >
                    Email
                  </Typography>
                  <TextField
                    required
                    fullWidth
                    type="email"
                    autoComplete="email"
                    autoFocus
                    placeholder="user.name@company.com"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        bgcolor: (theme) =>
                          theme.palette.mode === 'dark'
                            ? 'rgba(51, 65, 85, 0.5)'
                            : 'rgba(249, 250, 251, 1)',
                      },
                    }}
                  />
                </Box>

                <Box>
                  <Typography
                    variant="body2"
                    sx={{
                      fontWeight: 'medium',
                      mb: 0.5,
                      color: (theme) =>
                        theme.palette.mode === 'dark'
                          ? 'rgba(255,255,255,0.7)'
                          : 'rgba(0,0,0,0.7)',
                    }}
                  >
                    Password
                  </Typography>
                  <TextField
                    required
                    fullWidth
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        bgcolor: (theme) =>
                          theme.palette.mode === 'dark'
                            ? 'rgba(51, 65, 85, 0.5)'
                            : 'rgba(249, 250, 251, 1)',
                      },
                    }}
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton
                            onClick={togglePasswordVisibility}
                            edge="end"
                            sx={{
                              color: (theme) =>
                                theme.palette.mode === 'dark'
                                  ? 'rgba(255,255,255,0.5)'
                                  : 'rgba(0,0,0,0.4)',
                            }}
                          >
                            {showPassword ? <VisibilityOff /> : <Visibility />}
                          </IconButton>
                        </InputAdornment>
                      ),
                    }}
                  />
                </Box>

                <Box
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                  }}
                >
                  <Link
                    href="#"
                    variant="body2"
                    sx={{
                      color: '#2D6B9A',
                      textDecoration: 'none',
                      '&:hover': {
                        textDecoration: 'underline',
                      },
                    }}
                  >
                    Forgot Password?
                  </Link>

                  <Button
                    type="submit"
                    variant="contained"
                    disabled={loading}
                    sx={{
                      bgcolor: '#2D6B9A',
                      px: 4,
                      py: 1.5,
                      borderRadius: 1.5,
                      textTransform: 'none',
                      fontSize: '1rem',
                      fontWeight: 'medium',
                      '&:hover': {
                        bgcolor: '#245580',
                      },
                      '&:disabled': {
                        bgcolor: '#2D6B9A',
                        opacity: 0.6,
                      },
                    }}
                  >
                    {loading ? 'Logging in...' : 'Login'}
                  </Button>
                </Box>

                {/* Demo Login Buttons */}
                <Box sx={{ mt: 2 }}>
                  <Typography
                    variant="body2"
                    sx={{
                      mb: 1.5,
                      textAlign: 'center',
                      color: (theme) =>
                        theme.palette.mode === 'dark'
                          ? 'rgba(255,255,255,0.5)'
                          : 'rgba(0,0,0,0.5)',
                    }}
                  >
                    Demo Login (No Credentials Required)
                  </Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    <Button
                      fullWidth
                      variant="outlined"
                      onClick={() => handleDemoLogin('SYS_ADMIN')}
                      sx={{
                        borderColor: '#C62828',
                        color: '#C62828',
                        py: 1,
                        borderRadius: 1.5,
                        textTransform: 'none',
                        fontSize: '0.875rem',
                        fontWeight: 'medium',
                        '&:hover': {
                          borderColor: '#B71C1C',
                          bgcolor: 'rgba(198, 40, 40, 0.04)',
                        },
                      }}
                    >
                      System Administrator
                    </Button>
                    <Button
                      fullWidth
                      variant="outlined"
                      onClick={() => handleDemoLogin('TENANT_ADMIN')}
                      sx={{
                        borderColor: '#2D6B9A',
                        color: '#2D6B9A',
                        py: 1,
                        borderRadius: 1.5,
                        textTransform: 'none',
                        fontSize: '0.875rem',
                        fontWeight: 'medium',
                        '&:hover': {
                          borderColor: '#245580',
                          bgcolor: 'rgba(45, 107, 154, 0.04)',
                        },
                      }}
                    >
                      Tenant Administrator
                    </Button>
                    <Button
                      fullWidth
                      variant="outlined"
                      onClick={() => handleDemoLogin('CUSTOMER_USER')}
                      sx={{
                        borderColor: '#2E7D6F',
                        color: '#2E7D6F',
                        py: 1,
                        borderRadius: 1.5,
                        textTransform: 'none',
                        fontSize: '0.875rem',
                        fontWeight: 'medium',
                        '&:hover': {
                          borderColor: '#26695C',
                          bgcolor: 'rgba(46, 125, 111, 0.04)',
                        },
                      }}
                    >
                      Customer User
                    </Button>
                  </Box>
                </Box>
              </Box>
            </Box>
          </Box>
        </Box>
      </Box>
    </Box>
  )
}
