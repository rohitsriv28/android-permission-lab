import { Router } from 'express';
import MediaController from '../controllers/mediaController.js';
import { uploadSingle, uploadBatch } from '../middleware/upload.js';
import { optionalAuth } from '../middleware/auth.js';

const router = Router();

// Single photo upload: POST /api/uploads/photo
router.post('/photo', optionalAuth, uploadSingle, MediaController.uploadPhoto);

// Batch photos upload: POST /api/uploads/batch
router.post('/batch', optionalAuth, uploadBatch, MediaController.uploadBatch);

// List uploaded media: GET /api/uploads
router.get('/', optionalAuth, MediaController.getMediaList);

// Get single media item details: GET /api/uploads/:id
router.get('/:id', optionalAuth, MediaController.getMediaDetail);

// Delete media item: DELETE /api/uploads/:id
router.delete('/:id', optionalAuth, MediaController.deleteMedia);

export default router;
