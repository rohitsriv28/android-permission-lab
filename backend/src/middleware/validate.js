import { validationResult } from 'express-validator';
import ApiResponse from '../utils/apiResponse.js';

export const validate = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const formattedErrors = errors.array().map((err) => ({
      field: err.path || err.param,
      message: err.msg,
    }));
    return ApiResponse.error(res, 400, 'Validation Failed', formattedErrors);
  }
  next();
};

export default validate;
