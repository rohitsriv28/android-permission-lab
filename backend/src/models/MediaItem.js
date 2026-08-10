import mongoose from 'mongoose';

const mediaItemSchema = new mongoose.Schema(
  {
    clientMediaId: {
      type: String,
      required: [true, 'clientMediaId is required'],
      index: true,
    },
    user: {
      type: mongoose.Schema.Types.Mixed,
      default: 'default_device_user',
      index: true,
    },
    fileName: {
      type: String,
      required: [true, 'fileName is required'],
      trim: true,
    },
    uri: {
      type: String,
      required: [true, 'uri is required'],
    },
    mimeType: {
      type: String,
      required: [true, 'mimeType is required'],
      default: 'image/jpeg',
    },
    size: {
      type: Number,
      default: 0,
    },
    width: {
      type: Number,
      default: 0,
    },
    height: {
      type: Number,
      default: 0,
    },
    dateAdded: {
      type: Number,
      default: () => Date.now(),
    },
    uploadStatus: {
      type: String,
      enum: ['READY', 'UPLOADING', 'UPLOADED', 'FAILED'],
      default: 'UPLOADED',
      index: true,
    },
    cloudinaryPublicId: {
      type: String,
      required: true,
    },
    cloudinaryUrl: {
      type: String,
      required: true,
    },
    cloudinaryResourceType: {
      type: String,
      default: 'image',
    },
    cloudinaryBytes: {
      type: Number,
      default: 0,
    },
    cloudinaryFormat: {
      type: String,
      default: '',
    },
  },
  {
    timestamps: true,
  }
);

// Compound index for idempotency check: clientMediaId + user
mediaItemSchema.index({ clientMediaId: 1, user: 1 }, { unique: true });

// Transform to JSON response object matching Android MediaItem structure
mediaItemSchema.set('toJSON', {
  virtuals: true,
  transform: (doc, ret) => {
    ret.id = ret.clientMediaId || ret._id.toString();
    ret.serverId = ret._id.toString();
    delete ret._id;
    delete ret.__v;
    return ret;
  },
});

export const MediaItem = mongoose.model('MediaItem', mediaItemSchema);
export default MediaItem;
