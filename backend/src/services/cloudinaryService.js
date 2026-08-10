import { v2 as cloudinary } from 'cloudinary';
import { Readable } from 'stream';
import logger from '../utils/logger.js';
import ApiError from '../utils/apiError.js';

export class CloudinaryService {
  /**
   * Upload a memory buffer stream to Cloudinary
   * @param {Buffer} buffer - Image file buffer from Multer
   * @param {Object} options - Upload options (folder, filename, public_id)
   * @returns {Promise<Object>} Cloudinary upload result
   */
  static uploadStream(buffer, options = {}) {
    return new Promise((resolve, reject) => {
      const uploadFolder = options.folder || 'permission_lab_uploads';
      
      const uploadOptions = {
        folder: uploadFolder,
        resource_type: 'auto',
        overwrite: true,
        invalidate: true,
        ...(options.publicId && { public_id: options.publicId }),
      };

      const stream = cloudinary.uploader.upload_stream(
        uploadOptions,
        (error, result) => {
          if (error) {
            logger.error(`Cloudinary Upload Stream Error: ${error.message || JSON.stringify(error)}`);
            return reject(new ApiError(500, `Cloudinary upload failed: ${error.message}`));
          }
          logger.info(`Cloudinary Upload Success: public_id=${result.public_id}, url=${result.secure_url}`);
          resolve(result);
        }
      );

      const bufferStream = new Readable();
      bufferStream.push(buffer);
      bufferStream.push(null);
      bufferStream.pipe(stream);
    });
  }

  /**
   * Delete an asset from Cloudinary by public ID
   * @param {string} publicId 
   * @returns {Promise<Object>}
   */
  static async deleteResource(publicId) {
    try {
      if (!publicId) return null;
      const result = await cloudinary.uploader.destroy(publicId);
      logger.info(`Cloudinary Delete Resource: public_id=${publicId}, result=${result.result}`);
      return result;
    } catch (error) {
      logger.error(`Failed to delete Cloudinary resource ${publicId}: ${error.message}`);
      return null;
    }
  }
}

export default CloudinaryService;
