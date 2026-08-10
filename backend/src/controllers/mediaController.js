import MediaService from '../services/mediaService.js';
import ApiResponse from '../utils/apiResponse.js';
import ApiError from '../utils/apiError.js';

export class MediaController {
  /**
   * Single Photo Upload
   * POST /api/uploads/photo
   */
  static async uploadPhoto(req, res, next) {
    try {
      if (!req.file) {
        throw new ApiError(400, 'No image file uploaded in field "photo", "image", or "file"');
      }

      const userId = req.user
        ? req.user._id
        : (req.headers['x-user-id'] || req.query.userId || req.body.userId || null);

      const metadata = {
        clientMediaId: req.body.clientMediaId || req.body.id,
        fileName: req.body.fileName,
        uri: req.body.uri,
        mimeType: req.body.mimeType || req.file.mimetype,
        size: req.body.size || req.file.size,
        width: req.body.width,
        height: req.body.height,
        dateAdded: req.body.dateAdded,
      };

      const result = await MediaService.uploadSinglePhoto(req.file, metadata, userId);
      const statusCode = result.isDuplicate ? 200 : 201;
      const message = result.isDuplicate
        ? 'Photo already uploaded (idempotent result)'
        : 'Photo uploaded successfully to Cloudinary';

      return ApiResponse.success(res, statusCode, message, result.mediaItem);
    } catch (error) {
      next(error);
    }
  }

  /**
   * Batch Gallery Photos Upload
   * POST /api/uploads/batch
   */
  static async uploadBatch(req, res, next) {
    try {
      const files = req.batchFiles || [];
      if (files.length === 0) {
        throw new ApiError(400, 'No image files uploaded in "photos", "images", or "files" field');
      }

      const userId = req.user
        ? req.user._id
        : (req.headers['x-user-id'] || req.query.userId || req.body.userId || null);

      const metadataList = req.body.metadata || req.body.items || [];

      const batchResult = await MediaService.uploadBatchPhotos(files, metadataList, userId);
      
      const statusCode = batchResult.failed === 0 ? 201 : batchResult.successful > 0 ? 207 : 400;

      return ApiResponse.success(
        res,
        statusCode,
        `Batch processing complete. Successful: ${batchResult.successful}/${batchResult.total}`,
        batchResult
      );
    } catch (error) {
      next(error);
    }
  }

  /**
   * List Uploaded Media Items for User
   * GET /api/uploads
   */
  static async getMediaList(req, res, next) {
    try {
      const userId = req.user
        ? req.user._id
        : (req.headers['x-user-id'] || req.query.userId || null);

      const options = {
        page: req.query.page,
        limit: req.query.limit,
      };

      const result = await MediaService.getMediaItems(userId, options);
      return ApiResponse.success(res, 200, 'Media list retrieved successfully', result.items, result.pagination);
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get Specific Media Item Details
   * GET /api/uploads/:id
   */
  static async getMediaDetail(req, res, next) {
    try {
      const userId = req.user
        ? req.user._id
        : (req.headers['x-user-id'] || req.query.userId || null);
      const { id } = req.params;

      const mediaItem = await MediaService.getMediaById(id, userId);
      return ApiResponse.success(res, 200, 'Media details retrieved successfully', mediaItem);
    } catch (error) {
      next(error);
    }
  }

  /**
   * Delete Media Item
   * DELETE /api/uploads/:id
   */
  static async deleteMedia(req, res, next) {
    try {
      const userId = req.user
        ? req.user._id
        : (req.headers['x-user-id'] || req.query.userId || null);
      const { id } = req.params;

      const result = await MediaService.deleteMedia(id, userId);
      return ApiResponse.success(res, 200, 'Media item deleted successfully from server and Cloudinary', result);
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get Aggregate User & Storage Summary Analytics
   * GET /api/uploads/summary
   */
  static async getUserSummary(req, res, next) {
    try {
      const summary = await MediaService.getUserSummary();
      return ApiResponse.success(res, 200, 'User media summary retrieved successfully', summary);
    } catch (error) {
      next(error);
    }
  }
}

export default MediaController;
