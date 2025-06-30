# Banula Navigation Service Frontend

This is the frontend application for the Banula Navigation Service, a React-based web application for managing smart location information in the OCPI ecosystem.

## Features

- View and filter charging point locations by country code and party ID
- Edit location details including smart meter information
- Toggle publication status of locations
- View and manage EVSE and connector information

## Tech Stack

- React 19
- TypeScript
- Vite as build tool
- TailwindCSS for styling
- Shadcn UI components
- React Router for navigation
- React Query for data fetching

## Getting Started

### Prerequisites

- Node.js 18+
- npm or yarn

### Installation

1. Clone the repository
2. Navigate to the frontend directory:
   ```bash
   cd banula-navigation-service/frontend
   ```
3. Install dependencies:
   ```bash
   npm install
   # or
   yarn
   ```

### Development

Start the development server:

```bash
npm run dev
# or
yarn dev
```

The application will be available at `http://localhost:5173/navigator/ui` by default.

### Building for Production

```bash
npm run build
# or
yarn build
```

The build artifacts will be stored in the `dist/` directory.

## Configuration

The application is configured through the `vite.config.ts` file. You can adjust the following environment variables to match your deployment:

```typescript
define: {
  "process.env": {
    // Base path for the React Router
    routerBasename: "/navigator/ui",

    // API path for non-OCPI endpoints
    nonOcpiPath: process.env.NSP_API_NON_OCPI_PREFIX || "/navigator/non-ocpi/nsp",

    // Base URL for API requests - change this to match your backend
    serviceUrl: process.env.BASE_URL
      || "http://localhost:8080",

    // Authentication token for API requests
    tokenB: process.env.NSP_TOKEN_B || "493a7355-28f8-4575-ad73-a5b934b5ef25",
  },
}
```

These configurations can be overridden by setting the corresponding environment variables before building:

```bash
export NSP_API_NON_OCPI_PREFIX=/your-custom-path
export ENVIRONMENT=production
export NSP_TOKEN_B=your-token-here
npm run build
```

## Project Structure

- `src/components/` - Reusable UI components
- `src/components/ui/` - Generic UI components from Shadcn
- `src/components/layouts/` - Layout components
- `src/components/locations/` - Location-specific components
- `src/pages/` - Page components
- `src/services/api/` - API interaction layer
- `src/types/` - TypeScript type definitions
- `src/lib/` - Utility functions
- `src/routes.tsx` - Application routes

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run lint` - Run linting checks
- `npm run preview` - Preview production build locally

## Notes

- The application is designed to be served from the `/navigator/ui` path
- It communicates with the backend API using the configured `serviceUrl`
- Authentication is handled via the `tokenB` token in request headers
