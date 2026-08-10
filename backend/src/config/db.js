import mongoose from 'mongoose';
import dns from 'dns';
import config from './env.js';
import logger from '../utils/logger.js';

// Configure fallback DNS servers for Windows Node.js SRV lookup issues
try {
  dns.setServers(['8.8.8.8', '1.1.1.1']);
} catch (dnsErr) {
  // Ignore DNS override errors if prohibited by system policy
}

export const connectDB = async () => {
  try {
    const conn = await mongoose.connect(config.mongoUri, {
      serverSelectionTimeoutMS: 5000,
    });

    logger.info(`MongoDB Connected: ${conn.connection.host}/${conn.connection.name}`);

    mongoose.connection.on('error', (err) => {
      logger.error(`MongoDB connection error: ${err.message}`);
    });

    mongoose.connection.on('disconnected', () => {
      logger.warn('MongoDB disconnected. Reconnecting...');
    });

    return conn;
  } catch (error) {
    logger.error(`Failed to connect to MongoDB: ${error.message}`);
    if (error.message.includes('Authentication failed') || error.message.includes('bad auth')) {
      logger.error('👉 MongoDB Atlas Authentication Error: Please check database username and password in your .env MONGODB_URI');
    }
    logger.warn('Database operations will fail until MongoDB connection is established.');
    return null;
  }
};

export default connectDB;
