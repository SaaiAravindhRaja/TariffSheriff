# TariffSheriff Frontend

A modern, responsive React application for calculating and analyzing import tariffs and fees across countries, with a focus on the Electric Vehicle (EV) industry.

## 🚀 Features

- **Modern UI/UX**: Clean, responsive design with dark/light mode support
- **Real-time Calculations**: Instant tariff calculations with detailed breakdowns
- **Interactive Visualizations**: Charts and maps for trade route analysis
- **Comprehensive Dashboard**: Overview of trade operations and analytics
- **Mobile-First Design**: Optimized for all device sizes
- **Accessibility**: WCAG compliant with keyboard navigation support

## 🌐 Live Demo

[https://tariffsheriff-frontend.vercel.app/](https://tariffsheriff-frontend.vercel.app/)

## 🛠 Tech Stack

- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS with custom design system
- **UI Components**: Radix UI primitives with shadcn/ui
- **Animations**: Framer Motion
- **Charts**: Recharts
- **State Management**: TanStack Query (React Query)
- **Routing**: React Router v6
- **HTTP Client**: Axios
- **Form Handling**: React Hook Form with Zod validation

## 📁 Project Structure

```
src/
├── components/          # Reusable UI components
│   ├── ui/             # Base UI components (Button, Card, etc.)
│   ├── layout/         # Layout components (Header, Sidebar)
│   ├── charts/         # Chart components
│   └── dashboard/      # Dashboard-specific components
├── pages/              # Page components
├── hooks/              # Custom React hooks
├── lib/                # Utility functions
├── services/           # API services
├── styles/             # Global styles and CSS
└── types/              # TypeScript type definitions
```

## 🎨 Design System

### Colors
- **Brand**: Blue gradient (#0ea5e9 to #0369a1)
- **Success**: Green (#22c55e)
- **Warning**: Amber (#f59e0b)
- **Danger**: Red (#ef4444)

### Typography
- **Primary**: Inter (system font)
- **Monospace**: JetBrains Mono

### Components
All components follow the shadcn/ui design system with custom TariffSheriff branding.

## 🚦 Getting Started

### Prerequisites
- Node.js 18+ 
- npm or yarn

### Installation

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Set up environment variables**:
   ```bash
   cp .env.example .env
   ```
   
   Update the `.env` file with your configuration:
   ```env
   VITE_API_BASE_URL=http://localhost:8080/api
   VITE_APP_NAME=TariffSheriff
   ```

3. **Start development server**:
   ```bash
   npm run dev
   ```

4. **Open your browser**:
   Navigate to `http://localhost:3000`

### Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint
- `npm test` - Run tests

## 📱 Pages & Features

### Dashboard
- Overview of trade operations
- Key metrics and statistics
- Recent calculations
- Quick calculator widget
- Interactive trade route map

### Tariff Calculator
- Product information input
- HS code lookup
- Real-time tariff calculation
- Detailed cost breakdown
- Export results

### Database
- Browse tariff rules
- Search by country/product
- Filter and sort options
- Detailed rule information

### Analytics
- Trade volume trends
- Tariff rate analysis
- Country comparisons
- Market insights

### Simulator
- Policy scenario modeling
- Impact analysis
- Side-by-side comparisons
- What-if scenarios

### Trade Routes
- Route optimization
- Cost comparisons
- Interactive map
- Route recommendations

## 🎯 Key Components

### Layout Components
- **Header**: Navigation, search, user menu, theme toggle
- **Sidebar**: Collapsible navigation with route indicators

### Dashboard Components
- **QuickCalculator**: Instant tariff calculations
- **RecentCalculations**: History of calculations
- **TariffChart**: Interactive trend visualization
- **TradeRouteMap**: Global trade route visualization

### UI Components
- **Button**: Multiple variants with animations
- **Card**: Flexible container with hover effects
- **Input**: Form inputs with focus states
- **Badge**: Status indicators and labels

## 🔧 Customization

### Theme Customization
Update `tailwind.config.js` to modify:
- Color palette
- Typography scale
- Spacing system
- Border radius
- Animations

### Component Styling
Components use CSS variables for theming:
```css
:root {
  --primary: 200 98% 39%;
  --secondary: 210 40% 96%;
  /* ... */
}
```

### Adding New Pages
1. Create page component in `src/pages/`
2. Add route to `src/App.tsx`
3. Update navigation in `src/components/layout/Sidebar.tsx`

## 📊 Performance

- **Bundle Size**: Optimized with tree shaking
- **Code Splitting**: Route-based lazy loading
- **Caching**: TanStack Query for API caching
- **Images**: Optimized loading and formats

## 🧪 Testing

```bash
# Run unit tests
npm test

# Run tests in watch mode
npm run test:watch


## 👤 Profile (local)

This project includes a local, client-side Profile feature for quick demos:

- Profile data (name, role, email, location, avatar) is stored in `localStorage` under the key `app_profile`.
- To edit the profile, click the user name/avatar in the header and choose "Edit Profile" on the Profile page.
- To reset the profile data, open the browser devtools and run:

```js
localStorage.removeItem('app_profile')
window.location.reload()
```

Note: This is a local demo implementation. For production, integrate with your auth/user API and replace localStorage persistence.
# Generate coverage report
npm run test:coverage
```

## 🚀 Deployment

### Build for Production
```bash
npm run build
```

### Deploy to Vercel
```bash
npm install -g vercel
vercel --prod
```

### Deploy to Netlify
```bash
npm run build
# Upload dist/ folder to Netlify
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## Setup

1. Install dependencies: `npm install`
2. Run: `npm start`