import app from './src/app.js';
import config from './src/config/env.js';
import { connectDB } from './src/config/db.js';
import { initCloudinary } from './src/config/cloudinary.js';
import logger from './src/utils/logger.js';

const startServer = async () => {
  // Initialize Cloudinary SDK
  initCloudinary();

  // Connect to MongoDB
  await connectDB();

  // Start Express server listening on designated host & port
  const server = app.listen(config.port, '0.0.0.0', () => {
    logger.info(`==================================================`);
    logger.info(`🚀 Android Permission Lab REST API Server Running `);
    logger.info(`🌐 Environment: ${config.nodeEnv}`);
    logger.info(`📍 Local URL:   http://localhost:${config.port}`);
    logger.info(`📱 Emulator:    http://10.0.2.2:${config.port}`);
    logger.info(`==================================================`);
  });

  // Handle unhandled process rejections
  process.on('unhandledRejection', (err) => {
    logger.error(`UNHANDLED REJECTION! Shutting down... ${err.name}: ${err.message}`);
    server.close(() => {
      process.exit(1);
    });
  });

  // Handle uncaught exceptions
  process.on('uncaughtException', (err) => {
    logger.error(`UNCAUGHT EXCEPTION! Shutting down... ${err.name}: ${err.message}`);
    process.exit(1);
  });
};

startServer();
