import { createTheme } from '@mui/material/styles'

// Theme matching Angular ThingsBoard UI
export const theme = createTheme({
  palette: {
    primary: {
      main: '#106cc8', // Exact Angular primary color
    },
    secondary: {
      main: '#527a9e',
    },
    background: {
      default: '#eee', // Angular body background
      paper: '#fff',
    },
  },
  typography: {
    fontFamily: 'Roboto, "Helvetica Neue", sans-serif', // Exact Angular font stack
    fontSize: 16, // Angular base font size
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
        },
      },
    },
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          backgroundColor: '#eee',
        },
      },
    },
  },
})
