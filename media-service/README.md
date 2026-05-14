# ShopSphere - Media Service

The **Media Service** manages digital assets such as product images, user avatars, and promotional banners. It provides secure upload endpoints, performs processing (like resizing), and integrates with cloud storage providers.

## 🚀 Features
- **Image/Video Uploads:** Secure asset ingestion.
- **Asset Resizing & Optimization:** On-the-fly or background image processing.
- **Cloud Storage Integration:** Connects seamlessly with AWS S3 or MinIO.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER** (Read assets, Upload Avatars)
- **ADMIN** (Upload Product/Platform Assets)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, AWS S3 / MinIO, PostgreSQL (Metadata storage)

## 📂 API Endpoints
- `POST /api/v1/media/upload` (Requires CUSTOMER/ADMIN)
- `GET /api/v1/media/{id}` (Public/CUSTOMER)

## ⚙️ Configuration
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `STORAGE_BUCKET`