# Review Service - Quick Start Guide

## Start the Service

```bash
# Start all required services
docker-compose up -d review-db kafka discovery-service review-service

# Or start individually
docker-compose up -d review-db
docker-compose up -d kafka
docker-compose up -d discovery-service
docker-compose up -d review-service

# Wait for service to be healthy (30-60 seconds)
docker ps | grep review-service  # Should show "healthy"
```

## Test the Service

```bash
# Run comprehensive test suite (26 tests)
./test-review-service.sh

# Quick health check
curl http://localhost:8089/api/v1/welcome
curl http://localhost:8089/actuator/health

# View test results
cat review-service-test-results.txt
```

## API Examples

### Create a Review
```bash
curl -X POST http://localhost:8089/api/v1/reviews \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-001",
    "userId": "user-001",
    "rating": 5,
    "title": "Excellent product!",
    "comment": "Highly recommended"
  }'
```

### Get Review by ID
```bash
curl http://localhost:8089/api/v1/reviews/{review_id}
```

### Get All Reviews (Paginated)
```bash
curl "http://localhost:8089/api/v1/reviews?page=0&size=10"
```

### Get Reviews by Product
```bash
curl "http://localhost:8089/api/v1/reviews/product/prod-001?page=0&size=10"
```

### Get Approved Reviews Only
```bash
curl "http://localhost:8089/api/v1/reviews/product/prod-001/approved?page=0&size=10"
```

### Get Reviews by User
```bash
curl "http://localhost:8089/api/v1/reviews/user/user-001?page=0&size=10"
```

### Get Reviews by Status
```bash
curl "http://localhost:8089/api/v1/reviews/status/PENDING?page=0&size=10"
```

### Update a Review
```bash
curl -X PUT http://localhost:8089/api/v1/reviews/{review_id} \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 4,
    "title": "Updated title",
    "comment": "Updated comment"
  }'
```

### Approve a Review
```bash
curl -X POST http://localhost:8089/api/v1/reviews/{review_id}/approve
```

### Reject a Review
```bash
curl -X POST http://localhost:8089/api/v1/reviews/{review_id}/reject
```

### Delete a Review
```bash
curl -X DELETE http://localhost:8089/api/v1/reviews/{review_id}
```

## Useful Commands

### View Service Logs
```bash
docker logs review-service
docker logs review-service --follow  # Follow logs in real-time
```

### Check Service Health
```bash
curl http://localhost:8089/actuator/health | jq
```

### Check Eureka Registration
```bash
curl http://localhost:8761/eureka/apps | grep -A 10 "REVIEW-SERVICE"
```

### Stop the Service
```bash
docker-compose down review-service
# or
docker stop review-service
```

### Rebuild and Restart
```bash
docker-compose down review-service
docker-compose build review-service
docker-compose up -d review-service
```

## Key Information

| Item | Value |
|------|-------|
| Service Name | Review Service |
| Port | 8089 |
| Database | MongoDB (review-db) |
| Database Name | shopsphere_review |
| Eureka Service ID | REVIEW-SERVICE |
| Health Endpoint | /actuator/health |
| API Base Path | /api/v1 |

## Response Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 204 | Deleted (No Content) |
| 400 | Bad Request (Validation Error) |
| 404 | Not Found |
| 500 | Server Error |

## Review States

- **PENDING** - Awaiting approval/rejection
- **APPROVED** - Review has been approved
- **REJECTED** - Review has been rejected

## Validation Rules

- **Product ID** - Required, non-blank
- **User ID** - Required, non-blank
- **Rating** - Required, must be between 1-5
- **Title** - Required, 3-100 characters
- **Comment** - Optional, max 500 characters

## File Locations

- **Service Code:** `/home/ubuntu/shop-sphere/review-service/`
- **Test Script:** `/home/ubuntu/shop-sphere/test-review-service.sh`
- **Test Results:** `/home/ubuntu/shop-sphere/review-service-test-results.txt`
- **Summary:** `/home/ubuntu/shop-sphere/REVIEW_SERVICE_SUMMARY.md`
- **Test Report:** `/home/ubuntu/shop-sphere/REVIEW_SERVICE_TEST_REPORT.md`

## Database Connection

```
MongoDB URI: mongodb://review-db:27017/shopsphere_review
Collections:
  - reviews (Review documents)
```

## Kafka Topics

```
Topics Published:
  - review-created
  - review-updated
  - review-deleted
  - review-approved
```

## Troubleshooting

### Service won't start
1. Check if port 8089 is already in use: `lsof -i :8089`
2. Check logs: `docker logs review-service`
3. Verify MongoDB is running: `docker ps | grep review-db`

### Cannot connect to database
1. Verify review-db container is running
2. Check network connectivity: `docker network inspect shop-sphere_shopsphere-network`
3. Test MongoDB connection: `docker exec review-db mongosh --eval "db.adminCommand('ping')"`

### Tests are failing
1. Ensure all dependencies are running
2. Rebuild the service: `docker-compose build --no-cache review-service`
3. Check recent logs: `docker logs review-service | tail -50`

## Performance Tips

- Use pagination for large datasets (page=0&size=20)
- Filter by specific attributes for better performance
- Indexes are automatically created on productId, userId, rating, status

---

**Last Updated:** 2025-12-13  
**Service Version:** 1.0.0
