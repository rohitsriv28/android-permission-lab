import ApiResponse from '../utils/apiResponse.js';
import logger from '../utils/logger.js';
import config from '../config/env.js';

export const errorHandler = (err, req, res, next) => {
  let statusCode = err.statusCode || 500;
  let message = err.message || 'Internal Server Error';
  let errors = null;

  // Mongoose Bad ObjectId (CastError)
  if (err.name === 'CastError') {
    statusCode = 400;
    message = `Resource not found with id of ${err.value}`;
  }

  // Mongoose Duplicate Key Error (Code 11000)
  if (err.code === 11000) {
    statusCode = 409;
    const field = Object.keys(err.keyValue || {})[0] || 'field';
    message = `Duplicate field value entered for ${field}`;
  }

  // Mongoose Validation Error
  if (err.name === 'ValidationError') {
    statusCode = 400;
    message = 'Validation Error';
    errors = Object.values(err.errors).map((val) => ({
      field: val.path,
      message: val.message,
    }));
  }

  // Log error stack in non-test environments
  if (statusCode >= 500) {
    logger.error(`[500 Server Error] ${req.method} ${req.url} - ${err.stack || err.message}`);
  } else {
    logger.warn(`[${statusCode} Client Error] ${req.method} ${req.url} - ${message}`);
  }

  // Return formatted JSON response
  return res.status(statusCode).json({
    success: false,
    error: message,
    ...(errors && { errors }),
    ...(config.nodeEnv === 'development' && { stack: err.stack }),
  });
};

export default errorHandler;
