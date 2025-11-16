# ThingsBoard IoT Platform - React Frontend

Modern React frontend for the ThingsBoard IoT platform.

## Tech Stack

- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **State Management**: Redux Toolkit
- **UI Library**: Material-UI (MUI)
- **Routing**: React Router v6
- **HTTP Client**: Axios
- **Forms**: React Hook Form + Zod
- **Charts**: Recharts
- **Maps**: Leaflet + React Leaflet

## Project Structure

```
frontend-react/
├── src/
│   ├── components/     # Reusable UI components
│   ├── pages/          # Page components
│   ├── services/       # API service layer
│   ├── store/          # Redux store and slices
│   ├── hooks/          # Custom React hooks
│   ├── types/          # TypeScript type definitions
│   ├── utils/          # Utility functions
│   ├── App.tsx         # Main app component
│   ├── main.tsx        # Entry point
│   └── theme.ts        # MUI theme configuration
├── public/             # Static assets
├── index.html          # HTML template
├── vite.config.ts      # Vite configuration
├── tsconfig.json       # TypeScript configuration
└── package.json        # Dependencies
```

## Getting Started

### Installation

```bash
npm install
```

### Development

```bash
npm run dev
```

The app will be available at http://localhost:3000

### Build

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## Features

### Implemented
- ✅ Authentication (Login/Logout)
- ✅ Protected routes
- ✅ JWT token management with auto-refresh
- ✅ Redux state management
- ✅ Material-UI components
- ✅ Responsive layout with sidebar
- ✅ Device listing page
- ✅ API service layer
- ✅ TypeScript types

### In Progress
- 🔄 Device management (Create/Edit/Delete)
- 🔄 Tenant management
- 🔄 Customer management
- 🔄 Telemetry visualization
- 🔄 Real-time WebSocket updates

### Planned
- ⏳ Dashboard widgets
- ⏳ Rule chains visualization
- ⏳ Alarm management
- ⏳ Asset management
- ⏳ User management
- ⏳ Entity relations graph
- ⏳ Advanced telemetry charts
- ⏳ Map widgets
- ⏳ Custom dashboards
- ⏳ Device profile management

## API Integration

The frontend connects to the Python FastAPI backend running on port 8080.

API proxy is configured in `vite.config.ts`:
```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
}
```

## State Management

Using Redux Toolkit with the following slices:
- **auth**: User authentication and session
- **devices**: Device management
- **tenants**: Tenant management
- **customers**: Customer management
- **telemetry**: Telemetry data and subscriptions

## Routing

Routes defined in `App.tsx`:
- `/login` - Login page
- `/dashboard` - Main dashboard
- `/devices` - Device management
- `/tenants` - Tenant management
- `/customers` - Customer management

All routes except `/login` require authentication.

## Development Tools

### Linting
```bash
npm run lint
```

### Formatting
```bash
npm run format
```

## Environment Variables

Create a `.env` file in the root:
```
VITE_API_URL=http://localhost:8080
```

## Material-UI Theme

Custom theme configured in `src/theme.ts` with ThingsBoard brand colors:
- Primary: `#305680`
- Secondary: `#527a9e`

## Contributing

When converting Angular components to React:
1. Create the component in `src/components/` or `src/pages/`
2. Add types in `src/types/`
3. Create API service in `src/services/`
4. Add Redux slice in `src/store/` if needed
5. Update routing in `App.tsx`

## Migration from Angular

### Component Conversion
- Angular Services → React Hooks + Redux Slices + API Services
- Angular Components → React Functional Components
- Angular Routing → React Router
- RxJS Observables → Redux Toolkit Async Thunks
- NgRx → Redux Toolkit
- Angular Material → Material-UI (MUI)

### Key Differences
- Class components → Functional components with hooks
- Two-way binding → Controlled components
- Dependency Injection → Props and Context
- Observables → Promises and async/await

## License

Apache License 2.0
