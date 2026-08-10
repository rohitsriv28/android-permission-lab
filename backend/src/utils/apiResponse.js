export class ApiResponse {
  static success(res, statusCode = 200, message = 'Success', data = null, meta = null) {
    const responsePayload = {
      success: true,
      message,
      ...(data !== null && { data }),
      ...(meta !== null && { meta }),
    };
    return res.status(statusCode).json(responsePayload);
  }

  static error(res, statusCode = 500, message = 'Internal Server Error', errors = null) {
    const responsePayload = {
      success: false,
      error: message,
      ...(errors !== null && { errors }),
    };
    return res.status(statusCode).json(responsePayload);
  }
}

export default ApiResponse;
