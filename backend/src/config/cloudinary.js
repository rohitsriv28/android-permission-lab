import { v2 as cloudinary } from 'cloudinary';
import config from './env.js';
import logger from '../utils/logger.js';

let isCloudinaryConfigured = false;

export const initCloudinary = () => {
  const { cloudName, apiKey, apiSecret } = config.cloudinary;

  if (cloudName && apiKey && apiSecret && cloudName !== 'your_cloud_name') {
    cloudinary.config({
      cloud_name: cloudName,
      api_key: apiKey,
      api_secret: apiSecret,
      secure: true,
    });
    isCloudinaryConfigured = true;
    logger.info(`Cloudinary initialized with cloud_name: ${cloudName}`);
  } else {
    logger.warn('Cloudinary credentials missing or using placeholders. Cloud uploads will fail until valid credentials are provided in .env');
    isCloudinaryConfigured = false;
  }

  return cloudinary;
};

export const checkCloudinaryStatus = () => {
  return isCloudinaryConfigured;
};

export { cloudinary };
export default cloudinary;
