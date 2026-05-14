# UI Context — ShopSphere Grocery

## Frontend Status: NOT IMPLEMENTED

ShopSphere is currently a **backend-only** project. There is no frontend application included in this repository. All API endpoints are designed to be consumed by:

1. **Mobile app** (future)
2. **Web SPA** (future)
3. **Admin dashboard** (admin-service provides backend endpoints only)
4. **Third-party integrations** (potential)

---

## API Contract

### Base URL
```
Development: http://localhost:8080
Production:  https://{domain}
```

### Authentication

All protected endpoints require:
```
Authorization: Bearer {jwt-token}
```

### Standard Response Format

**Success:**
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-05-03T12:00:00Z"
}
```

**Error:**
```json
{
  "status": 400,
  "error": "Validation Error",
  "message": "Invalid request",
  "path": "/api/v1/products",
  "timestamp": "2026-05-03T12:00:00Z",
  "validationErrors": {
    "name": "Name is required"
  }
}
```

### Pagination Format

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false
}
```

---

## API Endpoints Reference

### Authentication (auth-service)
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/login` | Public | Login with email/password |
| POST | `/auth/register` | Public | Register new account |
| POST | `/auth/refresh` | Public | Refresh access token |
| POST | `/auth/logout` | Authenticated | Revoke tokens |
| POST | `/auth/validate` | Public | Validate a token |
| POST | `/auth/verify-email` | Public | Verify email with token |
| POST | `/auth/forgot-password` | Public | Request password reset |
| POST | `/auth/reset-password` | Public | Reset password with token |

### Catalog (catalog-service)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/products` | Public | List products (paginated) |
| GET | `/api/v1/products/{id}` | Public | Get product by ID |
| GET | `/api/v1/products/sku/{sku}` | Public | Get product by SKU |
| GET | `/api/v1/products/search` | Public | Search products |
| GET | `/api/v1/products/category/{categoryId}` | Public | Products by category |
| POST | `/api/v1/categories` | Public | Create category |
| GET | `/api/v1/categories` | Public | List categories |

### Cart (cart-service)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/cart` | Header: X-User-Id | Get user cart |
| POST | `/api/v1/cart/items` | Header: X-User-Id | Add item to cart |
| PUT | `/api/v1/cart/items/{productId}` | Header: X-User-Id | Update item quantity |
| DELETE | `/api/v1/cart/items/{productId}` | Header: X-User-Id | Remove item |
| DELETE | `/api/v1/cart` | Header: X-User-Id | Clear cart |

### Orders (order-service)
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/order` | Public | Create order |
| GET | `/order/{id}` | Public | Get order |
| GET | `/order/number/{orderNumber}` | Public | Get by order number |
| GET | `/order/customer/{customerId}` | Public | List customer orders |
| PUT | `/order/{id}/status` | Public | Update order status |
| GET | `/order` | Public | List all orders (NO PAGINATION — will be fixed) |

### Payments (payment-service)
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/payment/process` | Public | Process payment |
| GET | `/payment/transaction/{txId}` | Public | Get by transaction |
| GET | `/payment/order/{orderId}` | Public | Get payments for order |
| POST | `/payment/refund` | Public | Refund payment |
| GET | `/payment` | Public | List all (NO PAGINATION — will be fixed) |

### Inventory (inventory-service)
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/inventory` | Public | Create inventory item |
| GET | `/api/inventory/sku/{sku}` | Public | Get by SKU |
| POST | `/api/inventory/reserve` | Public | Reserve inventory |
| GET | `/api/inventory/check-availability` | Public | Check stock |

---

## Proposed Frontend Architecture

When a frontend is built, it should follow this pattern:

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Web SPA      │     │  Mobile App   │     │  Admin        │
│  (React/Vue)  │     │  (Flutter)    │     │  Dashboard    │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       └────────────────────┼────────────────────┘
                            │
                    ┌───────▼────────┐
                    │   API Gateway   │
                    │   (port 8080)   │
                    └───────┬────────┘
                            │
                    ┌───────▼────────┐
                    │    Auth JWT    │
                    └────────────────┘
```

### Key UI Considerations

1. **WebSockets for real-time**: Order status updates, inventory changes, delivery tracking
2. **Cart merge on login**: Guest cart should merge with user cart after authentication
3. **Optimistic UI**: Cart updates should feel instant (Redis-backed)
4. **Pagination everywhere**: Infinite scroll for product listing, orders, payments
5. **Loading states**: Circuit breakers will return fallback data — UI must handle gracefully
6. **Error states**: Standardized error responses — display field-level validation errors

---

## Current UI Gaps

| Gap | Impact | Backend Issue |
|---|---|---|
| No CORS configuration | Browser frontends blocked | API Gateway missing CORS |
| No WebSocket support | No real-time notifications | WebSocket service not implemented |
| No API versioning header | Breaking changes break clients | Inconsistent URL versioning |
| No rate limit headers | Clients can't back off | No rate limiting implemented |
| Gateway returns empty 401 body | Can't distinguish error types | `AuthenticationFilter` writes no body |

---

## Design System (Not Started)

When building the frontend, the following should be defined:
- Color palette (brand colors)
- Typography scale
- Component library (Material UI / Shadcn / custom)
- Form validation patterns
- Error state patterns
- Loading skeleton patterns
- Empty state patterns
