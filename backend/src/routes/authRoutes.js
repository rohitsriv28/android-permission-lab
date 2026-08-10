import { Router } from 'express';
import { body } from 'express-validator';
import AuthController from '../controllers/authController.js';
import { protect } from '../middleware/auth.js';
import validate from '../middleware/validate.js';
import { authRateLimiter } from '../middleware/rateLimiter.js';

const router = Router();

// Register validation rules
const registerValidation = [
  body('name').trim().notEmpty().withMessage('Name is required'),
  body('email').isEmail().withMessage('Valid email address is required'),
  body('password')
    .isLength({ min: 6 })
    .withMessage('Password must be at least 6 characters long'),
  validate,
];

// Login validation rules
const loginValidation = [
  body('email').isEmail().withMessage('Valid email address is required'),
  body('password').notEmpty().withMessage('Password is required'),
  validate,
];

router.post('/register', authRateLimiter, registerValidation, AuthController.register);
router.post('/login', authRateLimiter, loginValidation, AuthController.login);
router.get('/me', protect, AuthController.getMe);

export default router;
