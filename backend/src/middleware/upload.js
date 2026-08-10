import multer from 'multer';
import path from 'path';
import config from '../config/env.js';
import ApiError from '../utils/apiError.js';

// Configure memory storage to obtain file buffer for Cloudinary stream upload
const storage = multer.memoryStorage();

// File filter function to validate MIME types and file extensions
const fileFilter = (req, file, cb) => {
  const allowedTypes = config.upload.allowedMimeTypes;
  const ext = path.extname(file.originalname || '').toLowerCase();
  const validExtensions = ['.jpg', '.jpeg', '.png', '.webp', '.gif'];

  const isMimeAllowed = allowedTypes.includes(file.mimetype) || file.mimetype.startsWith('image/');
  const isExtAllowed = validExtensions.includes(ext);

  if (isMimeAllowed || isExtAllowed || file.mimetype === 'application/octet-stream') {
    cb(null, true);
  } else {
    cb(
      new ApiError(
        400,
        `Invalid file type (${file.mimetype}). Allowed image types: JPEG, PNG, WEBP, GIF`
      ),
      false
    );
  }
};

const upload = multer({
  storage,
  limits: {
    fileSize: config.upload.maxFileSizeBytes,
  },
  fileFilter,
});

// Single image upload field handler
export const uploadSingle = (req, res, next) => {
  const uploadHandler = upload.fields([
    { name: 'photo', maxCount: 1 },
    { name: 'image', maxCount: 1 },
    { name: 'file', maxCount: 1 },
  ]);

  uploadHandler(req, res, (err) => {
    if (err instanceof multer.MulterError) {
      if (err.code === 'LIMIT_FILE_SIZE') {
        return next(new ApiError(400, `File size exceeds the limit of ${config.upload.maxFileSizeBytes / (1024 * 1024)}MB`));
      }
      return next(new ApiError(400, `Upload error: ${err.message}`));
    } else if (err) {
      return next(err);
    }

    // Standardize single file under req.file
    if (req.files) {
      req.file = req.files.photo?.[0] || req.files.image?.[0] || req.files.file?.[0] || null;
    }
    next();
  });
};

// Batch image upload handler
export const uploadBatch = (req, res, next) => {
  const uploadHandler = upload.fields([
    { name: 'photos', maxCount: config.upload.maxBatchSize },
    { name: 'images', maxCount: config.upload.maxBatchSize },
    { name: 'files', maxCount: config.upload.maxBatchSize },
  ]);

  uploadHandler(req, res, (err) => {
    if (err instanceof multer.MulterError) {
      if (err.code === 'LIMIT_FILE_SIZE') {
        return next(new ApiError(400, `File size exceeds limit of ${config.upload.maxFileSizeBytes / (1024 * 1024)}MB`));
      }
      if (err.code === 'LIMIT_UNEXPECTED_FILE') {
        return next(new ApiError(400, `Maximum batch upload limit is ${config.upload.maxBatchSize} images per request`));
      }
      return next(new ApiError(400, `Batch upload error: ${err.message}`));
    } else if (err) {
      return next(err);
    }

    // Standardize batch files under req.batchFiles array
    if (req.files) {
      req.batchFiles = [
        ...(req.files.photos || []),
        ...(req.files.images || []),
        ...(req.files.files || []),
      ];
    } else {
      req.batchFiles = [];
    }
    next();
  });
};

export default { uploadSingle, uploadBatch };
