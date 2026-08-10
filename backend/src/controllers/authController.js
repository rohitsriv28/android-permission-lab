import AuthService from '../services/authService.js';
import ApiResponse from '../utils/apiResponse.js';

export class AuthController {
  /**
   * Register new user
   * POST /api/auth/register
   */
  static async register(req, res, next) {
    try {
      const { name, email, password } = req.body;
      const result = await AuthService.register({ name, email, password });
      return ApiResponse.success(res, 201, 'User registered successfully', result);
    } catch (error) {
      next(error);
    }
  }

  /**
   * Login user
   * POST /api/auth/login
   */
  static async login(req, res, next) {
    try {
      const { email, password } = req.body;
      const result = await AuthService.login({ email, password });
      return ApiResponse.success(res, 200, 'User logged in successfully', result);
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get authenticated user profile
   * GET /api/auth/me
   */
  static async getMe(req, res, next) {
    try {
      const user = await AuthService.getUserProfile(req.user._id);
      return ApiResponse.success(res, 200, 'User profile fetched successfully', {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        createdAt: user.createdAt,
      });
    } catch (error) {
      next(error);
    }
  }
}

export default AuthController;
