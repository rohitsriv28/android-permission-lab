import MediaItem from '../models/MediaItem.js';
import CloudinaryService from './cloudinaryService.js';
import ApiError from '../utils/apiError.js';
import logger from '../utils/logger.js';

export class MediaService {
  /**
   * Upload single photo with idempotency check and cleanup fallback
   */
  static async uploadSinglePhoto(file, metadata = {}, userId = null) {
    const clientMediaId = metadata.clientMediaId || metadata.id || `client_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const userQuery = userId || 'default_device_user';

    const fileName = metadata.fileName || (file && file.originalname) || `photo_${Date.now()}.jpg`;
    const mimeType = metadata.mimeType || (file && file.mimetype) || 'image/jpeg';
    const size = metadata.size ? Number(metadata.size) : (file ? file.size : 0);
    const width = metadata.width ? Number(metadata.width) : 0;
    const height = metadata.height ? Number(metadata.height) : 0;
    const dateAdded = metadata.dateAdded ? Number(metadata.dateAdded) : Date.now();
    const uri = metadata.uri || `content://media/external/images/media/${clientMediaId}`;

    // 1. Idempotency Check: search for existing record
    const existingDoc = await MediaItem.findOne({ clientMediaId, user: userQuery });
    if (existingDoc && existingDoc.uploadStatus === 'UPLOADED' && existingDoc.cloudinaryUrl) {
      // Confirm matching metadata or owner
      if (!userId || existingDoc.fileName === fileName || Math.abs(existingDoc.size - size) < 100) {
        logger.info(`Idempotency match: clientMediaId ${clientMediaId} already uploaded to Cloudinary. Returning existing record.`);
        return {
          mediaItem: existingDoc,
          isDuplicate: true,
        };
      }
    }

    if (!file || !file.buffer) {
      throw new ApiError(400, 'Image file buffer is required for upload');
    }

    let cloudinaryResult = null;

    try {
      // 2. Upload actual image binary stream to Cloudinary
      cloudinaryResult = await CloudinaryService.uploadStream(file.buffer, {
        folder: 'permission_lab_photos',
      });
    } catch (uploadError) {
      logger.error(`Cloudinary upload failed for ${fileName}: ${uploadError.message}`);
      throw new ApiError(500, `Image storage upload failed: ${uploadError.message}`);
    }

    // 3. Persist metadata in MongoDB with transactional fallback cleanup
    try {
      const mediaData = {
        clientMediaId,
        user: userQuery,
        fileName,
        uri,
        mimeType: mimeType === 'application/octet-stream' ? 'image/jpeg' : mimeType,
        size,
        width,
        height,
        dateAdded,
        uploadStatus: 'UPLOADED',
        cloudinaryPublicId: cloudinaryResult.public_id,
        cloudinaryUrl: cloudinaryResult.secure_url,
        cloudinaryResourceType: cloudinaryResult.resource_type || 'image',
        cloudinaryBytes: cloudinaryResult.bytes || size,
        cloudinaryFormat: cloudinaryResult.format || 'jpg',
      };

      let savedDoc;
      if (existingDoc) {
        // Update existing record
        Object.assign(existingDoc, mediaData);
        savedDoc = await existingDoc.save();
      } else {
        // Create new record
        savedDoc = await MediaItem.create(mediaData);
      }

      return {
        mediaItem: savedDoc,
        isDuplicate: false,
      };
    } catch (dbError) {
      logger.error(`Database save failed for ${fileName}: ${dbError.message}. Executing Cloudinary cleanup...`);
      // Cleanup Cloudinary asset to avoid orphan storage
      if (cloudinaryResult && cloudinaryResult.public_id) {
        await CloudinaryService.deleteResource(cloudinaryResult.public_id);
      }
      throw new ApiError(500, `Failed to persist media metadata: ${dbError.message}`);
    }
  }

  /**
   * Batch Upload handling with per-item status and controlled concurrency
   */
  static async uploadBatchPhotos(files, metadataList = [], userId = null) {
    if (!files || !Array.isArray(files) || files.length === 0) {
      throw new ApiError(400, 'At least one file is required for batch upload');
    }

    const results = [];

    // Process files with controlled execution loop
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      let meta = {};

      if (Array.isArray(metadataList) && metadataList[i]) {
        meta = typeof metadataList[i] === 'string' ? JSON.parse(metadataList[i]) : metadataList[i];
      } else if (typeof metadataList === 'string') {
        try {
          const parsed = JSON.parse(metadataList);
          if (Array.isArray(parsed) && parsed[i]) meta = parsed[i];
          else if (typeof parsed === 'object') meta = parsed;
        } catch (e) {
          meta = {};
        }
      }

      const clientMediaId = meta.clientMediaId || meta.id || file.originalname || `batch_${i}_${Date.now()}`;

      try {
        const uploadResult = await this.uploadSinglePhoto(file, { ...meta, clientMediaId }, userId);
        results.push({
          id: clientMediaId,
          clientMediaId,
          status: 'UPLOADED',
          isDuplicate: uploadResult.isDuplicate,
          mediaItem: uploadResult.mediaItem,
        });
      } catch (err) {
        logger.error(`Batch item ${i} (${clientMediaId}) failed: ${err.message}`);
        results.push({
          id: clientMediaId,
          clientMediaId,
          status: 'FAILED',
          error: err.message,
        });
      }
    }

    const successfulCount = results.filter((r) => r.status === 'UPLOADED').length;
    const failedCount = results.filter((r) => r.status === 'FAILED').length;

    return {
      total: files.length,
      successful: successfulCount,
      failed: failedCount,
      items: results,
    };
  }

  /**
   * Get Media items list
   */
  static async getMediaItems(userId = null, options = {}) {
    const query = userId ? { user: userId } : {};
    const page = parseInt(options.page || '1', 10);
    const limit = parseInt(options.limit || '50', 10);
    const skip = (page - 1) * limit;

    const items = await MediaItem.find(query)
      .sort({ dateAdded: -1, createdAt: -1 })
      .skip(skip)
      .limit(limit);

    const total = await MediaItem.countDocuments(query);

    return {
      items,
      pagination: {
        total,
        page,
        limit,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  /**
   * Get single Media item by ID or clientMediaId
   */
  static async getMediaById(id, userId = null) {
    const userQuery = userId ? { user: userId } : {};
    
    // Check MongoDB _id or clientMediaId or URI match
    let mediaItem = null;
    if (id.match(/^[0-9a-fA-F]{24}$/)) {
      mediaItem = await MediaItem.findOne({ _id: id, ...userQuery });
    }

    if (!mediaItem) {
      mediaItem = await MediaItem.findOne({ clientMediaId: id, ...userQuery });
    }

    if (!mediaItem) {
      mediaItem = await MediaItem.findOne({ uri: id, ...userQuery });
    }

    if (!mediaItem) {
      throw new ApiError(404, 'Media item not found');
    }

    return mediaItem;
  }

  /**
   * Delete media item by ID
   */
  static async deleteMedia(id, userId = null) {
    const mediaItem = await this.getMediaById(id, userId);
    
    // Delete Cloudinary asset
    if (mediaItem.cloudinaryPublicId) {
      await CloudinaryService.deleteResource(mediaItem.cloudinaryPublicId);
    }

    // Delete MongoDB document
    await MediaItem.deleteOne({ _id: mediaItem._id });

    return { id: mediaItem.clientMediaId, deleted: true };
  }

  /**
   * Aggregate analytics & user list for Privacy Dashboard
   */
  static async getUserSummary() {
    const summary = await MediaItem.aggregate([
      {
        $group: {
          _id: { $ifNull: ['$user', 'anonymous_device'] },
          count: { $sum: 1 },
          totalBytes: { $sum: { $ifNull: ['$cloudinaryBytes', '$size'] } },
          lastUpload: { $max: '$createdAt' }
        }
      },
      { $sort: { count: -1 } }
    ]);

    const totalPhotos = summary.reduce((acc, s) => acc + s.count, 0);
    const totalBytes = summary.reduce((acc, s) => acc + s.totalBytes, 0);

    return {
      totalPhotos,
      totalBytes,
      totalUsers: summary.length,
      users: summary.map(s => ({
        userId: s._id,
        count: s.count,
        totalBytes: s.totalBytes,
        lastUpload: s.lastUpload
      }))
    };
  }
}

export default MediaService;
