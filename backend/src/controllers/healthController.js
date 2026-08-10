import mongoose from 'mongoose';
import { checkCloudinaryStatus } from '../config/cloudinary.js';
import ApiResponse from '../utils/apiResponse.js';

export class HealthController {
  /**
   * Health and Status Check
   * GET /api/health
   */
  static getHealth(req, res) {
    const mongoStatusMap = {
      0: 'DISCONNECTED',
      1: 'CONNECTED',
      2: 'CONNECTING',
      3: 'DISCONNECTING',
    };

    const mongoState = mongoose.connection.readyState;
    const mongoStatus = mongoStatusMap[mongoState] || 'UNKNOWN';
    const cloudinaryConfigured = checkCloudinaryStatus();

    const healthData = {
      status: mongoState === 1 ? 'UP' : 'DEGRADED',
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
      services: {
        server: 'OPERATIONAL',
        database: {
          type: 'MongoDB',
          status: mongoStatus,
          connected: mongoState === 1,
        },
        cloudStorage: {
          provider: 'Cloudinary',
          configured: cloudinaryConfigured,
        },
      },
      environment: process.env.NODE_ENV || 'development',
    };

    const statusCode = mongoState === 1 ? 200 : 503;
    return ApiResponse.success(
      res,
      statusCode,
      `System status: ${healthData.status}`,
      healthData
    );
  }
}

export default HealthController;
