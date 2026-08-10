import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import path from 'path';
import { fileURLToPath } from 'url';
import routes from './routes/index.js';
import { errorHandler } from './middleware/errorHandler.js';
import { apiRateLimiter } from './middleware/rateLimiter.js';
import ApiResponse from './utils/apiResponse.js';
import logger from './utils/logger.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();

// Security HTTP headers (allow cross-origin images for Cloudinary web dashboard)
app.use(
  helmet({
    crossOriginResourcePolicy: false,
    contentSecurityPolicy: false,
  })
);

// CORS configuration
app.use(
  cors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With', 'X-User-Id'],
  })
);

// HTTP request logger
app.use(
  morgan('combined', {
    stream: {
      write: (message) => logger.http(message.trim()),
    },
  })
);

// Body parser middleware
app.use(express.json({ limit: '20mb' }));
app.use(express.urlencoded({ extended: true, limit: '20mb' }));

// Serve static HTML dashboard files from /public
app.use(express.static(path.join(__dirname, '../public')));

// Web Dashboard Route
app.get('/dashboard', (req, res) => {
  res.sendFile(path.join(__dirname, '../public/dashboard.html'));
});

// Apply rate limiting to API
app.use('/api', apiRateLimiter);

// API Documentation / Welcome Root Route
app.get('/api-info', (req, res) => {
  return ApiResponse.success(res, 200, 'Welcome to Android Permission Lab REST API Backend', {
    application: 'Android Permission Lab REST API',
    version: '1.0.0',
    status: 'Running',
    dashboard: 'GET /dashboard',
    endpoints: {
      health: 'GET /api/health',
      uploads: {
        singlePhoto: 'POST /api/uploads/photo (multipart field: "photo")',
        batchPhotos: 'POST /api/uploads/batch (multipart field: "photos")',
        listMedia: 'GET /api/uploads',
        getDetail: 'GET /api/uploads/:id',
        deleteMedia: 'DELETE /api/uploads/:id',
      },
    },
  });
});

// API Routes
app.use('/api', routes);

// 404 Handler for unknown routes
app.use((req, res, next) => {
  return ApiResponse.error(res, 404, `Route ${req.originalUrl} not found on server`);
});

// Centralized Error Handler
app.use(errorHandler);

export default app;
