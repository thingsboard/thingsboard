# ThingsBoard IoT Platform - Business Requirements for UI/UX Design

## Executive Summary

ThingsBoard is an enterprise-grade IoT platform that enables users to collect, process, visualize, and manage data from connected devices. The platform serves multiple user personas with different levels of access and functionality, providing comprehensive device management, real-time telemetry visualization, rule-based automation, and customizable dashboards.

## Platform Overview

### What is ThingsBoard?

ThingsBoard is an open-source IoT platform that provides:
- **Device Management**: Register, configure, and manage IoT devices at scale
- **Data Collection**: Collect telemetry data from devices via multiple protocols (MQTT, HTTP, CoAP)
- **Data Processing**: Process and transform data using a visual rule engine
- **Visualization**: Create custom dashboards with widgets for data visualization
- **Alerts & Notifications**: Configure alarms and notifications based on device data
- **Multi-tenancy**: Support for multiple isolated tenants and customers

### Target Users

1. **System Administrators** - Manage the entire platform, create tenants
2. **Tenant Administrators** - Manage devices, users, and customers within their tenant
3. **Customer Users** - View and manage devices assigned to their customer account
4. **End Users/Viewers** - View dashboards and device data (read-only)

---

## User Personas

### Persona 1: System Administrator (Sarah)

**Role**: Platform Owner/Operator
**Technical Level**: High
**Goals**:
- Manage multiple tenant organizations
- Monitor platform health and performance
- Configure system-wide settings
- Handle billing and licensing

**Frustrations**:
- Complex multi-tenant configurations
- Need clear overview of all tenants
- Difficult to troubleshoot tenant issues

**Needs**:
- Clear tenant management interface
- System-wide monitoring dashboard
- Audit logs for all actions
- Quick access to tenant details

---

### Persona 2: Tenant Administrator (Tom)

**Role**: IoT Solution Manager at a company
**Technical Level**: Medium to High
**Goals**:
- Deploy and manage IoT devices for their organization
- Create custom dashboards for different teams
- Set up automation rules for device data
- Manage team members and permissions

**Frustrations**:
- Time-consuming device provisioning
- Complex rule engine setup
- Difficult to visualize data across many devices
- Managing customer access rights

**Needs**:
- Bulk device import/provisioning
- Visual rule engine editor
- Dashboard templates
- User-friendly permission management
- Device grouping and hierarchy

---

### Persona 3: Customer User (Carol)

**Role**: End customer viewing their devices
**Technical Level**: Low to Medium
**Goals**:
- Monitor their assigned devices
- View real-time and historical data
- Receive notifications for device issues
- Access from mobile and desktop

**Frustrations**:
- Too much technical information
- Complicated navigation
- Difficult to find specific devices
- Unclear alarm meanings

**Needs**:
- Simple, clean interface
- Easy device search and filtering
- Clear alarm notifications
- Mobile-responsive design
- Intuitive data visualization

---

## Core Features & User Flows

### 1. Authentication & User Management

#### Login Flow
1. User navigates to platform URL
2. Presented with login screen (email + password)
3. Option for "Remember Me" and "Forgot Password"
4. Upon successful login, redirect to Dashboard
5. Failed login shows clear error message

**UI Requirements**:
- Clean, professional login page with platform branding
- Form validation with inline error messages
- Loading indicator during authentication
- "Show/Hide Password" toggle
- Link to password reset flow

#### User Management
**For Tenant/System Admins**:
- List all users with filters (role, status, customer)
- Add new user form with fields:
  - Email (required, unique)
  - First Name, Last Name
  - Role (dropdown: SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER)
  - Customer assignment (if CUSTOMER_USER)
  - Activation method (email link or set password)
- Edit user details
- Activate/Deactivate users
- Delete users (with confirmation)
- Send activation/password reset emails

**UI Requirements**:
- Data table with sorting, filtering, pagination
- Quick actions menu (edit, deactivate, delete)
- Modal dialogs for add/edit forms
- Confirmation dialogs for destructive actions
- Search bar for finding users quickly

---

### 2. Tenant Management (System Admin Only)

#### Tenant List View
- Table showing all tenants with columns:
  - Tenant Name
  - Region
  - Email
  - Created Date
  - Number of Devices
  - Status (Active/Inactive)
  - Actions

#### Tenant Details/Edit
- Form fields:
  - Tenant Name (required)
  - Title
  - Region (dropdown or text)
  - Contact Information (email, phone)
  - Address (street, city, state, zip, country)
  - Additional Info (JSON editor or key-value pairs)
  - Tenant Profile (for resource limits)

**UI Requirements**:
- Clean form layout with logical grouping
- Address fields organized in a grid
- Expandable sections for optional details
- Save/Cancel buttons
- Validation messages

---

### 3. Customer Management (Tenant Admin)

#### Customer List View
- Table showing customers with:
  - Customer Title
  - Email
  - Phone
  - Number of Devices
  - Number of Users
  - Created Date
  - Actions

#### Customer Details/Edit
- Similar to tenant form but tenant-scoped
- Ability to assign devices to customer
- Assign users to customer
- View customer's dashboards

**UI Requirements**:
- Clean data table with search/filter
- Quick assign devices/users interface
- Modal for bulk assignments
- Customer hierarchy visualization (if parent-child relationships)

---

### 4. Device Management

#### Device List View (PRIMARY SCREEN)
- **Layout**: Data table with columns:
  - Checkbox (for bulk actions)
  - Device Name (clickable to details)
  - Device Type (with icon)
  - Label
  - Customer (if assigned)
  - Status Indicator (online/offline/inactive)
  - Latest Activity timestamp
  - Actions (view, edit, delete)

- **Top Actions Bar**:
  - "+ Add Device" button (primary CTA)
  - "Import Devices" button
  - Bulk actions dropdown (assign to customer, delete, etc.)
  - Export to CSV

- **Filters Sidebar/Panel**:
  - Device Type (multi-select dropdown)
  - Customer (dropdown)
  - Status (Active, Inactive, Online, Offline)
  - Date range (created/last activity)
  - Search by name/label

- **Key Metrics Cards** (above table):
  - Total Devices
  - Online Devices
  - Devices with Active Alarms
  - New Devices (last 7 days)

**UI Requirements**:
- Responsive table that works on tablets
- Real-time status updates (online/offline)
- Visual indicators for alarms
- Quick preview on hover showing key telemetry
- Empty state with helpful onboarding

#### Add/Edit Device Form
- **Basic Information**:
  - Device Name (required)
  - Device Type (dropdown or searchable select)
  - Label (optional)
  - Device Profile (dropdown)
  - Description

- **Assignment**:
  - Assign to Customer (dropdown, optional)

- **Advanced** (collapsible):
  - Additional Info (JSON editor)
  - Gateway assignment
  - Firmware version
  - Software version

**UI Requirements**:
- Wizard-style form for new devices OR
- Single-page form with sections
- Real-time validation
- Auto-save drafts
- "Save and Add Another" option

#### Device Details Page

**Layout**:
```
┌─────────────────────────────────────────┐
│ Device: Temperature Sensor #1            │
│ [Edit] [Delete] [Assign to Customer]    │
├─────────────────────────────────────────┤
│ Tabs: [Overview] [Telemetry] [Attributes]│
│       [Alarms] [Events] [Relations]      │
├─────────────────────────────────────────┤
│ Tab Content Area                         │
│                                          │
└─────────────────────────────────────────┘
```

**Overview Tab**:
- Device information card (name, type, label, customer)
- Latest telemetry values (key-value pairs with timestamps)
- Status indicator with uptime
- Device credentials section (for admins)
- Quick actions (send RPC command, copy device ID)

**Telemetry Tab**:
- **Latest Values** section:
  - Cards showing current value for each telemetry key
  - Timestamp of last update
  - Trend indicator (up/down/stable)

- **Historical Data**:
  - Time range selector (last hour, 24h, 7 days, custom)
  - Line charts for each telemetry key
  - Ability to overlay multiple keys
  - Zoom and pan controls
  - Export to CSV

**Attributes Tab**:
- Three sections: Client Attributes, Server Attributes, Shared Attributes
- Table for each with: Key, Value, Last Update Time
- Add/Edit/Delete attribute controls
- JSON view toggle

**Alarms Tab**:
- List of alarms for this device
- Filter by severity (Critical, Major, Minor, Warning)
- Filter by status (Active, Acknowledged, Cleared)
- Quick acknowledge/clear actions

**Events Tab**:
- Audit log of device events
- Lifecycle events (created, updated, deleted)
- Error events
- Stats events
- Filter by event type and date range

**Relations Tab**:
- Visual graph of related entities (assets, devices, dashboards)
- Add/remove relations
- Relation type (contains, manages, etc.)

**UI Requirements**:
- Smooth tab transitions
- Real-time data updates without page refresh
- Responsive charts that work on mobile
- Export functionality for all data
- Loading states for each section
- Error states with retry options

#### Device Credentials
**For Tenant Admins**:
- View device access token or credentials
- Regenerate credentials
- Copy to clipboard
- Different credential types:
  - Access Token (default)
  - X.509 Certificate
  - MQTT Basic
  - LWM2M Credentials

**UI Requirements**:
- Secure display (show/hide sensitive data)
- Copy buttons for credentials
- Warning before regenerating (will break existing connections)
- Instructions for using credentials

---

### 5. Asset Management

Assets are logical groupings/hierarchies of devices (e.g., Building > Floor > Room > Devices)

#### Asset List View
- Similar to device list but with:
  - Asset Name
  - Asset Type (Building, Floor, Room, Vehicle, etc.)
  - Parent Asset (if in hierarchy)
  - Number of Related Devices
  - Customer
  - Actions

#### Asset Hierarchy View
- Tree visualization showing asset relationships
- Expandable/collapsible nodes
- Drag-and-drop to reorganize (admin only)
- Click on asset to view details

**UI Requirements**:
- Visual tree with icons for asset types
- Breadcrumb navigation for current location
- Quick filters to find assets
- Context menu for quick actions

---

### 6. Dashboards

Dashboards are customizable views for visualizing device data.

#### Dashboard List View
- Grid or list of dashboards showing:
  - Dashboard thumbnail/preview
  - Dashboard Title
  - Description
  - Created By
  - Last Modified
  - Assigned Customers
  - Actions (view, edit, share, delete)

- **Filters**:
  - My Dashboards
  - Shared with Me
  - Public Dashboards
  - By Customer

**UI Requirements**:
- Grid view with large previews
- List view for compact display
- Toggle between views
- Star/favorite dashboards
- Quick open (single click to view)

#### Dashboard View/Edit Mode

**View Mode** (for viewing dashboards):
```
┌─────────────────────────────────────────┐
│ Dashboard: Factory Overview              │
│ [Share] [Export] [Enter Edit Mode]      │
├─────────────────────────────────────────┤
│ Time Range: [Last 24 Hours ▼]  [Refresh]│
├─────────────────────────────────────────┤
│ ┌─────────┐ ┌─────────┐ ┌─────────┐    │
│ │ Widget  │ │ Widget  │ │ Widget  │    │
│ │    1    │ │    2    │ │    3    │    │
│ └─────────┘ └─────────┘ └─────────┘    │
│ ┌───────────────┐ ┌───────────────┐    │
│ │   Widget 4    │ │   Widget 5    │    │
│ └───────────────┘ └───────────────┘    │
└─────────────────────────────────────────┘
```

**Edit Mode** (for building dashboards):
- Drag-and-drop widget placement
- Resize widgets by dragging corners
- Grid snapping for alignment
- Widget library sidebar
- Layout controls (add row, add column)
- Undo/Redo
- Save/Cancel

**Widget Types to Support**:

1. **Timeseries Chart Widgets**:
   - Line chart
   - Bar chart
   - Area chart
   - Scatter plot
   - Configuration: Select entities, telemetry keys, time range, aggregation

2. **Latest Values Widgets**:
   - Cards showing current value
   - Gauges (circular/linear)
   - Digital displays
   - Status indicators

3. **Alarm Widgets**:
   - Alarm list
   - Alarm count by severity
   - Alarm timeline

4. **Map Widgets**:
   - Show device locations on map
   - Clustered markers
   - Route visualization
   - Geofencing zones

5. **Control Widgets**:
   - Buttons to send RPC commands
   - Sliders for setting values
   - Toggle switches
   - Input forms

6. **Static Widgets**:
   - HTML/Markdown content
   - Images
   - Labels and titles

**Widget Configuration Panel**:
- Widget title
- Data source (select entities)
- Telemetry/attribute keys to display
- Appearance settings (colors, thresholds, icons)
- Advanced settings (decimals, units, aggregation)
- Card settings (background, borders, shadows)

**UI Requirements**:
- Smooth drag-and-drop
- Real-time preview while configuring
- Widget templates/presets
- Responsive grid layout
- Full-screen mode for single widgets
- Export dashboard as JSON
- Import dashboard from JSON

---

### 7. Telemetry & Data Visualization

#### Telemetry Requirements

**Real-time Updates**:
- WebSocket connection for live data
- Visual indicator when data is updating
- Configurable update interval

**Historical Data**:
- Time range selector with presets:
  - Last 5 minutes
  - Last hour
  - Last 24 hours
  - Last 7 days
  - Last 30 days
  - Custom range (date picker)

**Chart Features**:
- Zoom in/out
- Pan (drag to move time window)
- Crosshair with value tooltip
- Legend (show/hide series)
- Multiple Y-axes for different units
- Export chart as image (PNG/SVG)
- Export data as CSV

**Aggregation Options**:
- None (raw data)
- Average
- Min/Max
- Sum
- Count
- First/Last

**UI Requirements**:
- High-performance charting library (Recharts)
- Smooth animations
- Loading skeleton while fetching data
- Error states with retry
- No data state with helpful message
- Responsive charts that adapt to container size

---

### 8. Alarms & Notifications

#### Alarm List View
- Table with columns:
  - Severity (icon + color: Critical=Red, Major=Orange, Minor=Yellow, Warning=Blue)
  - Type/Name
  - Originator (device/asset)
  - Status (Active-Unack, Active-Ack, Cleared-Unack, Cleared-Ack)
  - Start Time
  - End Time (if cleared)
  - Actions (Acknowledge, Clear, View Details)

- **Filters**:
  - Severity
  - Status
  - Originator Type
  - Date Range
  - Alarm Type

- **Bulk Actions**:
  - Acknowledge selected
  - Clear selected

**UI Requirements**:
- Color-coded severity
- Sound notification for new alarms (optional)
- Browser notifications (optional)
- Real-time updates
- Alarm count badge in navigation

#### Alarm Details
- Full alarm information
- Timeline of status changes
- Related telemetry at time of alarm
- Acknowledge/Clear with comment field
- Propagation information (if alarm propagates to parent entities)

**UI Requirements**:
- Modal or side panel for details
- Visual timeline
- Comment history
- Quick actions at top

---

### 9. Rule Chains (Automation Engine)

Rule chains are visual workflows that process incoming device data.

#### Rule Chain List View
- Table with:
  - Rule Chain Name
  - Root Flag (is this the default chain?)
  - Debug Mode (on/off toggle)
  - Created Date
  - Actions (Edit, Export, Delete)

#### Rule Chain Editor (COMPLEX VISUAL INTERFACE)

**Layout**:
```
┌─────────────────────────────────────────┐
│ Rule Chain: Process Telemetry           │
│ [Save] [Debug On/Off] [Export]          │
├──────────┬──────────────────────────────┤
│  Node    │  Canvas                      │
│ Library  │                              │
│          │  ┌──────┐                    │
│ Input    │  │Input │                    │
│ Filter   │  └──┬───┘                    │
│ Enrich   │     │                        │
│ Transform│  ┌──▼───┐   ┌─────┐         │
│ Action   │  │Filter│───│Save │         │
│ External │  └──┬───┘   └─────┘         │
│          │     │                        │
│          │  ┌──▼──────┐                │
│          │  │Send Alarm│                │
│          │  └─────────┘                │
└──────────┴──────────────────────────────┘
```

**Node Types**:
1. **Input Nodes**: Entry points for messages
2. **Filter Nodes**: Filter messages by conditions
3. **Enrichment Nodes**: Add data to messages
4. **Transformation Nodes**: Transform message format/content
5. **Action Nodes**: Save data, create alarms, send emails
6. **External Nodes**: Call external APIs, integrate systems

**Canvas Features**:
- Drag nodes from library onto canvas
- Connect nodes by dragging from output to input
- Multiple connection types (Success, Failure, Other)
- Color-coded connections
- Delete connections (right-click or backspace)
- Zoom in/out
- Pan canvas
- Auto-layout option
- Minimap for large chains

**Node Configuration**:
- Click node to open config panel
- Each node type has specific configuration
- Script editor for custom logic (JavaScript)
- Test button to test node with sample data
- Validation of configuration

**Debug Mode**:
- When enabled, shows message flow in real-time
- Highlight nodes as messages pass through
- Display message payload at each node
- Error messages if node fails

**UI Requirements**:
- Smooth drag-and-drop
- Undo/Redo
- Copy/Paste nodes
- Node search in library
- Connection validation (prevent invalid connections)
- Performance with large chains (100+ nodes)
- Export/Import as JSON

---

### 10. Profile & Settings

#### User Profile
- View/Edit user information:
  - First Name, Last Name
  - Email (display only, cannot change)
  - Phone (optional)
  - Language preference
  - Timezone
  - Profile picture

- Change Password section:
  - Current password
  - New password
  - Confirm new password
  - Password strength indicator

**UI Requirements**:
- Clean form layout
- Avatar upload with crop
- Inline validation
- Success/error messages

#### Tenant Settings (Tenant Admin)
- Tenant profile information
- Default dashboard
- Default rule chain
- Email server configuration (SMTP)
- SMS provider configuration
- Branding (logo, colors) - optional feature

**UI Requirements**:
- Tabbed interface for different setting categories
- Save button per section or global save
- Preview for branding changes

---

### 11. Audit Logs

For System/Tenant Admins to track all actions.

#### Audit Log View
- Table with:
  - Timestamp
  - User (who performed action)
  - Action Type (Login, Create, Update, Delete, etc.)
  - Entity Type (Device, Customer, User, etc.)
  - Entity Name/ID
  - Status (Success, Failure)
  - Details (expandable)

- **Filters**:
  - Date range
  - User
  - Action type
  - Entity type
  - Status

**UI Requirements**:
- Expandable rows for details
- Export to CSV
- Real-time updates
- Search functionality

---

## Design System Requirements

### Color Palette

**Primary Colors**:
- Primary Blue: `#305680` - Main brand color for buttons, links, active states
- Secondary Blue: `#527a9e` - Secondary actions, hover states

**Semantic Colors**:
- Success: `#4caf50` (Green) - Successful operations, online status
- Warning: `#ff9800` (Orange) - Warnings, minor/major alarms
- Error: `#f44336` (Red) - Errors, critical alarms
- Info: `#2196f3` (Light Blue) - Informational messages

**Severity Colors** (for alarms):
- Critical: `#d32f2f` (Dark Red)
- Major: `#ff6f00` (Dark Orange)
- Minor: `#fbc02d` (Yellow)
- Warning: `#1976d2` (Blue)
- Indeterminate: `#757575` (Gray)

**Neutral Colors**:
- Background: `#f5f5f5` (Light Gray)
- Surface: `#ffffff` (White)
- Border: `#e0e0e0` (Light Gray)
- Text Primary: `#212121` (Almost Black)
- Text Secondary: `#757575` (Gray)
- Disabled: `#bdbdbd` (Light Gray)

### Typography

**Font Family**: Roboto (fallback: Arial, sans-serif)

**Font Sizes**:
- H1: 32px (Page titles)
- H2: 24px (Section headers)
- H3: 20px (Card titles)
- H4: 18px (Subsections)
- Body: 14px (Regular text)
- Small: 12px (Helper text, captions)

**Font Weights**:
- Regular: 400
- Medium: 500
- Bold: 700

### Spacing

Use 8px grid system:
- XS: 4px
- SM: 8px
- MD: 16px
- LG: 24px
- XL: 32px
- XXL: 48px

### Components

**Buttons**:
- Primary: Filled with primary color
- Secondary: Outlined with primary color
- Text: No background, just text
- Icon: Icon only, no text
- States: Default, Hover, Active, Disabled, Loading

**Forms**:
- Text inputs with labels above
- Required field indicator (*)
- Error messages below field in red
- Helper text in gray below field
- Validation on blur and submit

**Tables**:
- Alternating row colors for readability
- Hover state on rows
- Sortable columns (with sort icon)
- Fixed header on scroll
- Pagination at bottom (10, 25, 50, 100 per page)
- Row selection with checkboxes

**Cards**:
- White background
- Light shadow
- 8px border radius
- 16px padding
- Optional header with actions

**Modals/Dialogs**:
- Overlay with semi-transparent background
- Centered modal
- Close button (X) in top-right
- Actions (Cancel, Confirm) at bottom right
- Max width: 600px for forms
- Can be full-screen for complex interfaces

**Navigation**:
- Top app bar with logo, page title, user menu
- Left sidebar with main navigation items
- Active item highlighted
- Icons for each menu item
- Collapsible sidebar for more space

**Loading States**:
- Circular spinner for page loads
- Linear progress bar for long operations
- Skeleton screens for content loading
- Shimmer effect for placeholders

**Empty States**:
- Illustration or icon
- Helpful message
- Call-to-action button (e.g., "Add Your First Device")

**Error States**:
- Error icon
- Clear error message
- Suggested action or retry button

### Responsive Design

**Breakpoints**:
- Mobile: < 600px
- Tablet: 600px - 960px
- Desktop: > 960px

**Mobile Considerations**:
- Hamburger menu for navigation
- Bottom navigation for quick actions
- Simplified tables (card view)
- Touch-friendly button sizes (min 44x44px)
- Swipe gestures for actions

---

## User Flows & Wireframes Needed

### Critical User Flows

1. **Device Onboarding Flow**:
   - Add device → Configure settings → Generate credentials → Connect device → Verify telemetry

2. **Dashboard Creation Flow**:
   - Create dashboard → Add widgets → Configure data sources → Set time range → Save and share

3. **Alarm Response Flow**:
   - Receive alarm → View details → Acknowledge → Investigate telemetry → Clear alarm

4. **Rule Chain Setup Flow**:
   - Create rule chain → Add filter node → Add action node → Connect nodes → Test → Activate

### Wireframe Requirements

Please create wireframes/mockups for:

1. **Login Page**
2. **Dashboard (Home) Page** with sample widgets
3. **Device List Page**
4. **Device Details Page** (all tabs)
5. **Add/Edit Device Form**
6. **Dashboard View Mode**
7. **Dashboard Edit Mode** with widget configuration
8. **Rule Chain Editor**
9. **Alarm List Page**
10. **User Management Page**
11. **Mobile Views** for key pages

---

## Data Visualization Requirements

### Chart Types Needed

1. **Line Charts**:
   - Time-series telemetry
   - Multiple series overlay
   - Smooth/stepped lines
   - Area fill option

2. **Bar Charts**:
   - Comparing values
   - Horizontal/vertical
   - Stacked/grouped

3. **Gauges**:
   - Circular gauge (speedometer style)
   - Linear gauge (thermometer style)
   - Configurable min/max/thresholds
   - Color zones (green, yellow, red)

4. **Cards/Numeric Displays**:
   - Large number with label
   - Trend indicator (up/down arrow)
   - Sparkline option
   - Comparison to previous period

5. **Maps**:
   - Device location markers
   - Marker clustering
   - Custom icons based on device type/status
   - Info popup on marker click
   - Drawing tools for geofences

6. **Tables/Grids**:
   - Sortable columns
   - Filterable
   - Selectable rows
   - Cell formatting based on value
   - Pagination

### Thresholds & Conditional Formatting

Widgets should support:
- Color thresholds (e.g., temp > 30°C = red)
- Icon changes based on value
- Show/hide based on conditions
- Alerts/highlighting for out-of-range values

---

## Performance Requirements

- **Page Load Time**: < 2 seconds for initial load
- **Time to Interactive**: < 3 seconds
- **Real-time Updates**: < 1 second latency from device to UI
- **Chart Rendering**: Handle 1000+ data points smoothly
- **Table Performance**: Handle 10,000+ rows with virtual scrolling
- **Dashboard Load**: Load dashboard with 20+ widgets in < 3 seconds

---

## Accessibility Requirements

- **WCAG 2.1 Level AA Compliance**:
  - Color contrast ratios (4.5:1 for text)
  - Keyboard navigation support
  - Screen reader support (ARIA labels)
  - Focus indicators
  - Skip links for navigation
  - Alt text for images/icons

- **Keyboard Shortcuts**:
  - Global search: Ctrl/Cmd + K
  - Navigate between sections: Tab
  - Close modals: Esc
  - Save forms: Ctrl/Cmd + S

---

## Internationalization (i18n)

- Support for multiple languages (initially English)
- RTL (Right-to-Left) support for Arabic, Hebrew
- Date/time formatting based on locale
- Number formatting (decimal separator, thousands separator)
- Currency symbols

---

## Branding & Customization

Platform should support white-labeling:
- Custom logo upload
- Primary/secondary color customization
- Custom domain
- Favicon
- Email templates with branding

---

## Mobile App Requirements (Future)

While the primary focus is web, the design should consider:
- Progressive Web App (PWA) capabilities
- Mobile-responsive design
- Touch-optimized interactions
- Offline capability (view cached data)
- Push notifications for alarms

---

## Success Metrics & KPIs

**User Experience Metrics**:
- Time to complete device onboarding: < 2 minutes
- Dashboard creation time: < 5 minutes
- User task success rate: > 90%
- User satisfaction score (SUS): > 70

**Performance Metrics**:
- Page load time: < 2s
- Time to interactive: < 3s
- 99.9% uptime

**Engagement Metrics**:
- Daily active users
- Average session duration
- Feature adoption rates
- Dashboard views per user

---

## User Feedback & Iteration

- In-app feedback widget
- User testing sessions planned quarterly
- A/B testing for major UI changes
- Analytics tracking (respect privacy)

---

## Technical Constraints

- **Browser Support**:
  - Chrome (latest 2 versions)
  - Firefox (latest 2 versions)
  - Safari (latest 2 versions)
  - Edge (latest 2 versions)

- **Screen Resolutions**:
  - Minimum: 1280x720
  - Optimized for: 1920x1080
  - 4K support

- **Framework**: React 18 with Material-UI
- **State Management**: Redux Toolkit
- **Charts**: Recharts library
- **Maps**: Leaflet

---

## Out of Scope (Future Enhancements)

- Video streaming from devices
- Voice control integration
- AI/ML analytics dashboards
- Advanced data export (PDF reports)
- Mobile native apps (iOS/Android)
- Collaboration features (comments, annotations)

---

## Questions for UI/UX Team

1. Should we use a light theme, dark theme, or both?
2. How should we handle very large lists (10,000+ devices)?
3. What's the preferred method for bulk device import UI?
4. Should we support drag-and-drop file uploads for assets (images, configs)?
5. How should we visualize complex device hierarchies?
6. What onboarding flow should we provide for new users?
7. Should we have contextual help/tooltips throughout?
8. How should we handle real-time notifications (toast, banner, modal)?

---

## Deliverables Expected from UI/UX

1. **Design System**:
   - Color palette, typography, spacing
   - Component library (buttons, forms, tables, etc.)
   - Figma/Sketch file

2. **Wireframes**:
   - Low-fidelity wireframes for all major screens
   - User flow diagrams

3. **High-Fidelity Mockups**:
   - Pixel-perfect designs for all screens
   - Desktop, tablet, mobile views
   - Light/dark theme (if applicable)

4. **Interactive Prototype**:
   - Clickable prototype for user testing
   - Key user flows demonstrated

5. **UI Specifications**:
   - Spacing, sizing, colors documented
   - Component states (hover, active, disabled)
   - Animation/transition specifications

6. **Assets**:
   - Icons (SVG)
   - Illustrations
   - Logo variations
   - Export for development

7. **Documentation**:
   - Design guidelines
   - Component usage instructions
   - Accessibility notes

---

## Timeline

- **Discovery & Research**: 1-2 weeks
- **Wireframing**: 2-3 weeks
- **High-Fidelity Design**: 4-6 weeks
- **Prototype & Testing**: 2-3 weeks
- **Handoff & Support**: Ongoing

---

## Contact & Collaboration

- **Product Owner**: [Your Name]
- **Development Team**: React/Python team
- **Stakeholders**: IoT solution architects, customer support

**Collaboration Tools**:
- Figma for design
- Jira for task tracking
- Slack for communication
- Weekly design reviews

---

## Appendix: Example Use Cases

### Use Case 1: Smart Building Management
**Scenario**: Facility manager monitors HVAC systems across multiple buildings

**User Journey**:
1. Login to platform
2. Navigate to "Buildings" asset view
3. Expand building hierarchy to see floors and rooms
4. Click on a room to see assigned HVAC devices
5. View temperature/humidity telemetry in real-time
6. Receive alarm when temperature exceeds threshold
7. Acknowledge alarm and adjust HVAC settings via control widget

### Use Case 2: Fleet Tracking
**Scenario**: Logistics company tracks delivery vehicles

**User Journey**:
1. Login to platform
2. View "Fleet Dashboard" with map widget
3. See all vehicle locations on map in real-time
4. Click on vehicle marker to see details (speed, fuel, status)
5. Filter vehicles by status (in transit, idle, maintenance needed)
6. Create geofence around delivery zone
7. Receive alarm when vehicle leaves geofence
8. Generate report of vehicle activity for the day

### Use Case 3: Agricultural IoT
**Scenario**: Farmer monitors soil moisture and irrigation

**User Journey**:
1. Login to platform
2. View "Farm Overview" dashboard
3. See soil moisture sensors displayed on field map
4. View moisture levels as gauge widgets
5. Receive alarm when moisture drops below threshold
6. Manually trigger irrigation via control widget
7. Set up automation rule: "If moisture < 30%, turn on irrigation"
8. View historical moisture data as chart to optimize watering schedule

---

## Conclusion

This document provides comprehensive business requirements for designing the ThingsBoard IoT platform UI/UX. The focus is on creating an intuitive, powerful, and scalable interface that serves multiple user personas while handling complex IoT data workflows.

**Key Priorities**:
1. **Simplicity for non-technical users** (Carol the Customer User)
2. **Power and flexibility for admins** (Tom the Tenant Admin)
3. **Real-time data visualization** with high performance
4. **Mobile responsiveness** for on-the-go access
5. **Accessibility** for all users

Please use this document as a foundation for creating wireframes, mockups, and interactive prototypes. We're excited to collaborate and bring this IoT platform to life!
