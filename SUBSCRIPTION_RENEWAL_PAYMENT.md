# Subscription Payment Integration

## 📋 Tổng Quan

Hệ thống đã được tích hợp đầy đủ **thanh toán cho subscription**:
1. **Initial Subscription** - Thanh toán khi đăng ký gói lần đầu (linkVehicle)
2. **Subscription Renewal** - Thanh toán khi gia hạn gói hết hạn

User phải thanh toán invoice trước khi subscription được kích hoạt hoặc gia hạn.

## 🆕 Flow Đăng Ký Gói Lần Đầu (Link Vehicle)

### 1. **User Đăng Ký Gói**
```http
POST /api/user/vehicles/link
Authorization: Bearer {token}
Content-Type: application/json

{
  "vehicleModelId": 1,
  "subscriptionPlanId": 2
}
```

**Response:**
```json
{
  "message": "Vehicle created. Please pay invoice #15 (299000₫) to activate subscription and receive 3 batteries.",
  "vehicle": {
    "id": 10,
    "vin": "VN-VF8ECOPLUS-A1B2C"
  },
  "subscription": {
    "id": 20,
    "planName": "Premium Plan",
    "status": "PENDING",  // ⚠️ PENDING - chờ thanh toán
    "startDate": "2025-11-07",
    "endDate": "2025-12-07"
  },
  "batteries": [
    {
      "id": 101,
      "serialNumber": "BAT-ABC123",
      "status": "AVAILABLE"  // ⚠️ AVAILABLE - chưa gán cho xe
    }
  ],
  "invoiceId": 15,
  "invoiceAmount": 299000.0
}
```

**Điểm Khác Biệt:**
- Subscription → `PENDING` (chưa active)
- Batteries → `AVAILABLE` (chưa gán cho xe, vehicle = null)
- Không có swap transaction logs

### 2. **User Thanh Toán Invoice**
```http
POST /api/payment/create-vnpay-url
Content-Type: application/json

{
  "invoiceId": 15
}
```

### 3. **VNPay Callback - Auto Activate**
Khi payment thành công (responseCode=00):
1. ✅ Invoice → `PAID`
2. 🔄 **Tự động trigger `subscriptionService.activateSubscription()`**
3. ✅ Subscription: `PENDING` → `ACTIVE`
4. ✅ Batteries: `AVAILABLE` → `IN_USE` và gán cho vehicle

**Log:**
```
SUBSCRIPTION ACTIVATED | subscriptionId=20 | invoiceId=15 | amount=299000₫
```

## 🔄 Flow Gia Hạn Subscription

### 1. **Auto-Renew Job Chạy Hàng Ngày**

```java
POST /api/admin/subscriptions/auto-renew
```

**Logic:**

1. Tìm tất cả subscriptions hết hạn hôm nay (`endDate = today`)
2. Kiểm tra subscription có pending invoices không (swap overage hoặc renewal cũ)
3. Nếu có pending invoices → BLOCK renewal
4. Nếu không có pending invoices → **Tạo renewal invoice** với giá plan
5. Subscription **chưa được renew** cho đến khi user thanh toán

### 2. **Tạo Renewal Invoice**

```java
invoiceService.createSubscriptionRenewalInvoice(subscription, planPrice, planName)
```

**Invoice Fields:**

- `invoiceType`: `"SUBSCRIPTION_RENEWAL"`
- `amount`: Giá của plan (VD: 299,000₫)
- `description`: `"Subscription Renewal: Premium Plan - 299000₫"`
- `swapTransaction`: `null` (không liên quan đến swap)
- `usageType`, `overage`, `rate`: `null`
- `status`: `PENDING`

### 3. **User Thanh Toán Invoice**

#### a. Lấy danh sách pending invoices

```http
GET /api/user/invoices
Authorization: Bearer {token}
```

**Response:**

```json
[
  {
    "invoiceId": 10,
    "subscriptionId": 5,
    "vehicleVin": "VF8ECOPLUS001",
    "planName": "Premium Plan",
    "amount": 299000.0,
    "status": "PENDING",
    "description": "Subscription Renewal: Premium Plan - 299000₫",
    "createdAt": "2025-11-07T10:00:00"
  }
]
```

#### b. Tạo payment URL

```http
POST /api/payment/create-vnpay-url
Content-Type: application/json

{
  "invoiceId": 10
}
```

**Response:**

```json
{
  "success": true,
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "message": "Redirect user to this URL to complete payment"
}
```

#### c. User thanh toán trên VNPay

#### d. VNPay callback

```http
GET /api/payment/vnpay-return?vnp_ResponseCode=00&vnp_TransactionNo=...
```

**Khi responseCode=00 (success):**

1. ✅ Invoice → `PAID`
2. 🔄 **Tự động trigger `subscriptionService.completeRenewal()`**
3. ✅ Old subscription → `COMPLETED`
4. ✅ New subscription → `ACTIVE` với plan mới

**Response:**

```json
{
  "success": true,
  "message": "Payment successful",
  "invoiceId": 10,
  "amount": 299000.0,
  "invoiceStatus": "PAID",
  "description": "Subscription Renewal: Premium Plan - 299000₫",
  "paidAt": "2025-11-07T10:30:00"
}
```

### 4. **Subscription Được Renew Tự Động**

Sau khi payment callback thành công:

```java
subscriptionService.completeRenewal(subscriptionId)
```

**Logic:**

1. Kiểm tra không còn pending invoices
2. Đóng subscription cũ → `COMPLETED`
3. Tạo subscription mới:
   - `user`, `vehicle`: giữ nguyên
   - `plan`: lấy từ `nextPlanId` hoặc plan hiện tại
   - `status`: `ACTIVE`
   - `startDate`: `oldSub.endDate + 1`
   - `endDate`: `startDate + plan.durationDays`
   - `energyUsedThisMonth`: reset về 0
   - `distanceUsedThisMonth`: reset về 0

## 📊 Invoice Types

### SWAP_OVERAGE

- Tạo khi user vượt base_mileage/base_energy
- Có `swapTransaction`
- Có `usageType`, `overage`, `rate`
- Description: `"Overage: 1.5 kWh × 13,826₫/kWh = 20,739₫"`

### SUBSCRIPTION_RENEWAL

- Tạo khi subscription hết hạn
- Không có `swapTransaction`
- Không có `usageType`, `overage`, `rate`
- Description: `"Subscription Renewal: Premium Plan - 299000₫"`

## 🔒 Business Rules

### Auto-Renew Blocking Conditions

Subscription sẽ **KHÔNG được renew** nếu:

1. ❌ Có pending swap overage invoices
2. ❌ Có pending renewal invoices từ lần renew trước
3. ❌ User chưa thanh toán renewal invoice hiện tại

### Payment Flow

1. **Auto-renew job** tạo invoice → subscription ở trạng thái "pending renewal"
2. **User phải thanh toán** invoice để kích hoạt renewal
3. **VNPay callback** tự động complete renewal sau khi thanh toán thành công
4. **Subscription mới** được tạo và active ngay lập tức

## 🛠️ Database Changes

### Invoice Table Updates

```sql
-- swap_transaction_id now nullable
ALTER TABLE invoices
    ALTER COLUMN swap_transaction_id DROP NOT NULL;

-- New field: invoice_type
ALTER TABLE invoices
    ADD COLUMN invoice_type VARCHAR(50);

-- usage_type, overage, rate now nullable
ALTER TABLE invoices
    ALTER COLUMN usage_type DROP NOT NULL,
    ALTER COLUMN overage DROP NOT NULL,
    ALTER COLUMN rate DROP NOT NULL;
```

## 📝 Example Scenarios

### Scenario 1: Normal Renewal (Same Plan)

1. User có subscription Premium Plan hết hạn `2025-12-01`
2. Auto-renew job chạy `2025-12-01`:
   - Tạo invoice: 299,000₫
   - Status: PENDING
3. User thanh toán VNPay
4. Callback success → tự động renew
5. New subscription: `2025-12-02` đến `2026-01-01` (Premium Plan)

### Scenario 2: Change Plan Before Renewal

1. User đang dùng Basic Plan (199,000₫)
2. User đổi sang Premium Plan (299,000₫):
   ```http
   PUT /api/user/subscriptions/{vehicleId}/change-plan
   { "newPlanId": 2 }
   ```
   - Set `nextPlanId = 2`
3. Auto-renew job chạy khi hết hạn:
   - Tạo invoice: 299,000₫ (Premium Plan price)
4. User thanh toán
5. Renew thành Premium Plan

### Scenario 3: Multiple Pending Invoices

1. User có 2 pending invoices:
   - Invoice #1: Swap overage 50,000₫
   - Invoice #2: Renewal 299,000₫
2. Auto-renew job → **BLOCKED**
3. User phải thanh toán cả 2 invoices:
   - Thanh toán invoice #1 trước
   - Thanh toán invoice #2 → tự động renew

## ⚠️ Important Notes

### Manual Intervention Required

Nếu VNPay callback thành công nhưng `completeRenewal()` failed:

- Invoice = PAID ✅
- Subscription vẫn chưa renew ❌
- Cần admin manually call:
  ```http
  POST /api/admin/subscriptions/{subscriptionId}/complete-renewal
  ```

### Testing

**Test renewal flow:**

1. Tạo subscription với `endDate = today`
2. Call auto-renew job
3. Verify renewal invoice created
4. Simulate VNPay payment
5. Verify subscription renewed

**Check pending invoices:**

```http
GET /api/user/invoices/check-pending/{subscriptionId}
```

Response:

```json
{
  "subscriptionId": 5,
  "hasPendingInvoices": true,
  "pendingCount": 2,
  "totalPendingAmount": 349000.0
}
```

## 🚀 Next Steps

### Future Enhancements

1. **Email notifications** khi renewal invoice được tạo
2. **SMS reminder** trước khi subscription hết hạn
3. **Auto-suspend** subscription nếu không thanh toán trong X ngày
4. **Refund logic** nếu user cancel subscription giữa chừng
5. **Webhook** để frontend realtime update subscription status

### Admin Tools Needed

```http
# Manual renew completion
POST /api/admin/subscriptions/{subscriptionId}/complete-renewal

# Query stuck renewals
GET /api/admin/invoices/stuck-renewals

# Refund invoice
POST /api/admin/invoices/{invoiceId}/refund
```

## 📚 Related Documentation

- [VNPAY_INTEGRATION_GUIDE.md](./VNPAY_INTEGRATION_GUIDE.md) - VNPay payment setup
- [migration_update_invoices_for_renewal.sql](./migration_update_invoices_for_renewal.sql) - Database migration
