import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import routes from './routes/index.js';
import { errorHandler } from './middleware/errorHandler.js';
import { apiRateLimiter } from './middleware/rateLimiter.js';
import ApiResponse from './utils/apiResponse.js';
import logger from './utils/logger.js';

const app = express();

// Security HTTP headers
app.use(helmet());

// CORS configuration (allow requests from Android emulator / cross-origin web apps)
app.use(
  cors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With'],
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

// Apply rate limiting
app.use('/api', apiRateLimiter);

// API Documentation / Welcome Root Route
app.get('/', (req, res) => {
  return ApiResponse.success(res, 200, 'Welcome to Android Permission Lab REST API Backend', {
    application: 'Android Permission Lab REST API',
    version: '1.0.0',
    status: 'Running',
    endpoints: {
      health: 'GET /api/health',
      auth: {
        register: 'POST /api/auth/register',
        login: 'POST /api/auth/login',
        me: 'GET /api/auth/me',
      },
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
