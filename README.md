# Android Permission Lab - Monorepo

Welcome to the **Android Permission Lab** project. This repository is structured as a production monorepo containing both the server-side REST API backend and the Jetpack Compose Android application.

---

## 📁 Monorepo Structure

```
Permission-Lab/
├── backend/                   # Node.js + Express REST API Server
│   ├── src/
│   │   ├── config/            # DB & Cloudinary initializers
│   │   ├── controllers/       # HTTP route handlers
│   │   ├── middleware/        # Upload, Auth, Validation, Error handlers
│   │   ├── models/            # Mongoose schemas (MediaItem, User)
│   │   ├── routes/            # Express routers
│   │   ├── services/          # Media upload, Cloudinary stream, Auth services
│   │   └── utils/             # Winston logger, ApiError, ApiResponse
│   ├── server.js              # Express application bootstrap
│   ├── package.json           # Node.js dependencies & scripts
│   ├── .env.example           # Environment variables template (NO credentials)
│   └── .gitignore             # Excludes .env, node_modules/, logs
│
├── android/                   # Native Kotlin Android Application
│   ├── app/
│   │   ├── src/main/java/com/permissionlab/app/
│   │   │   ├── data/          # Repository & NetworkService
│   │   │   ├── model/         # MediaItem & Permission models
│   │   │   └── ui/            # Jetpack Compose UI Screens & ViewModels
│   │   ├── build.gradle.kts   # App Gradle configuration (BuildConfig.BASE_URL)
│   │   └── AndroidManifest.xml# Permissions & Network Security settings
│   ├── build.gradle.kts       # Root Gradle configuration
│   ├── gradlew.bat            # Windows Gradle wrapper
│   └── .gitignore             # Excludes build/, .gradle/, local.properties
│
├── .gitignore                 # Root Git exclusion rules
└── README.md                  # Monorepo documentation
```

---

## 🚀 Quick Start (Development Mode)

### 1. Backend Server Setup (`backend/`)
```bash
cd backend
npm install
cp .env.example .env
```
Fill in your credentials in `.env`:
```env
PORT=5000
MONGODB_URI=mongodb+srv://<user>:<password>@cluster.mongodb.net/permission_lab
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```
Run the development server:
```bash
npm run dev
```

### 2. Android Client Setup (`android/`)
1. Open the `android/` folder in Android Studio.
2. In `app/build.gradle.kts`, configure your development server address:
   ```kotlin
   debug {
       buildConfigField("String", "BASE_URL", "\"http://192.168.1.110:5000/api\"")
   }
   ```
3. Run the application on your physical device or Android Emulator.

---

## ☁️ Production Deployment Architecture

When deploying Permission Lab to production:

### 1. Backend Cloud Hosting
Deploy the Node.js application (`backend/`) to a production cloud host:
- **Render.com** *(Web Service)*
- **Railway.app**
- **AWS App Runner** / **ECS**
- **DigitalOcean App Platform**

Set environment variables (`MONGODB_URI`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `JWT_SECRET`) in your cloud provider's environment dashboard.

### 2. Database & Cloud Storage
- **Database**: MongoDB Atlas Cluster
- **Media Storage**: Cloudinary Cloud Account

### 3. Android Client Release Build
In `android/app/build.gradle.kts`, update the release build variant to point to your live HTTPS server URL:
```kotlin
release {
    buildConfigField("String", "BASE_URL", "\"https://your-permission-lab-api.onrender.com/api\"")
}
```
Build the signed APK / App Bundle:
```bash
cd android
./gradlew assembleRelease
```

---

## 🔒 Security & Privacy

- **Zero Secret Exposure**: MongoDB connection strings, Cloudinary credentials, and JWT secrets are strictly managed via environment variables (`.env`) and excluded from Git tracking.
- **User Privacy UX**: The Android user interface emphasizes local device photo security and privacy control, while background services perform REST API synchronization with Cloudinary and MongoDB.
