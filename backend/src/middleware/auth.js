import jwt from 'jsonwebtoken';
import User from '../models/User.js';
import config from '../config/env.js';
import ApiError from '../utils/apiError.js';

/**
 * Protect routes - Require valid JWT Bearer token
 */
export const protect = async (req, res, next) => {
  let token;

  if (
    req.headers.authorization &&
    req.headers.authorization.startsWith('Bearer')
  ) {
    token = req.headers.authorization.split(' ')[1];
  }

  if (!token) {
    return next(new ApiError(401, 'Not authorized, no token provided'));
  }

  try {
    const decoded = jwt.verify(token, config.jwtSecret);
    const user = await User.findById(decoded.id);

    if (!user) {
      return next(new ApiError(401, 'User belonging to this token no longer exists'));
    }

    req.user = user;
    next();
  } catch (error) {
    return next(new ApiError(401, 'Not authorized, token invalid or expired'));
  }
};

/**
 * Optional Authentication - Attach user if valid token present, otherwise proceed
 */
export const optionalAuth = async (req, res, next) => {
  let token;

  if (
    req.headers.authorization &&
    req.headers.authorization.startsWith('Bearer')
  ) {
    token = req.headers.authorization.split(' ')[1];
  }

  if (!token) {
    req.user = null;
    return next();
  }

  try {
    const decoded = jwt.verify(token, config.jwtSecret);
    const user = await User.findById(decoded.id);
    req.user = user || null;
  } catch (error) {
    req.user = null;
  }
  next();
};

export default { protect, optionalAuth };
