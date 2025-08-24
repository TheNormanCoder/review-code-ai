# AI Code Review Dashboard

Modern React dashboard for monitoring and managing AI-powered code reviews.

## 🚀 Features

- **Pull Request Management**: View, filter, and trigger AI reviews for pull requests
- **AI Review Integration**: Real-time integration with AI review API endpoints  
- **Metrics Dashboard**: Comprehensive analytics with charts and trends
- **Responsive Design**: Works on desktop and mobile devices
- **GitHub Pages Ready**: Configured for easy deployment

## 🛠️ Tech Stack

- **React 18** with TypeScript
- **Vite** for fast development and building
- **Tailwind CSS** for styling
- **React Query** for API state management
- **React Router** for navigation
- **Recharts** for data visualization
- **Lucide React** for icons

## 🏃‍♂️ Quick Start

### Development

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Visit http://localhost:3000
```

### Environment Variables

Create a `.env` file:

```bash
VITE_API_URL=http://localhost:8080/api
```

### Build for Production

```bash
# Build the app
npm run build

# Preview the build
npm run preview
```

## 📱 Pages

### Dashboard (`/`)
- Overview metrics cards
- Recent pull requests
- Quality insights
- Team activity summary

### Pull Requests (`/pull-requests`)
- Filterable list of pull requests
- AI review triggers
- Status indicators
- Quick actions

### Pull Request Detail (`/pull-requests/:id`)
- Detailed PR information
- AI review results
- Review findings with severity levels
- Suggested improvements

### Metrics (`/metrics`)
- Comprehensive analytics dashboard
- Charts for trends and distributions
- Team performance metrics
- Quality insights over time

## 🔌 API Integration

The dashboard integrates with these API endpoints:

```
GET    /api/reviews/pull-requests          # List PRs
GET    /api/reviews/pull-requests/{id}     # PR details
POST   /api/reviews/pull-requests/{id}/ai-suggestions     # Trigger AI suggestions
POST   /api/reviews/pull-requests/{id}/ai-final-review    # Trigger final AI review
GET    /api/reviews/pull-requests/{id}/reviews            # Get reviews
GET    /api/reviews/metrics/dashboard      # Dashboard metrics
```

## 🚀 Deployment

### GitHub Pages

The app is configured for GitHub Pages deployment:

1. Push changes to the main branch
2. GitHub Actions will automatically build and deploy
3. Access at: `https://username.github.io/review-code-ai/`

### Manual Deployment

```bash
# Build for production
npm run build

# Deploy the dist/ folder to your hosting provider
```

## 🔧 Configuration

### API URL

Update the API URL in:
- `.env` for development
- GitHub Actions workflow for production
- `vite.config.ts` base path for GitHub Pages

### Styling

- Modify `tailwind.config.js` for theme customization
- Update `src/index.css` for global styles
- Components use Tailwind utility classes

## 📊 Data Flow

```
User Action → React Query → API Call → Backend → Database
     ↓              ↑
UI Update ← State Update ← Response ← Processing ← Data
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📝 License

This project is part of the AI Code Review System and follows the same license.