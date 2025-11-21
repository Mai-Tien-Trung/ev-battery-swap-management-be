# 💳 HƯỚNG DẪN TÍCH HỢP VNPAY THANH TOÁN

## 📋 CHUẨN BỊ

### 1. Đăng ký VNPay Sandbox

1. Truy cập: https://sandbox.vnpayment.vn/devreg
2. Đăng ký tài khoản merchant
3. Sau khi đăng ký, lấy thông tin:
   - **TMN Code** (Mã website)
   - **Hash Secret** (Secret key để mã hóa)

### 2. Cấu hình trong `application.properties`

Cập nhật các giá trị sau:

```properties
# ⚠️ THAY BẰNG THÔNG TIN THẬT TỪ VNPAY
vnpay.tmn-code=YOUR_TMN_CODE_FROM_VNPAY
vnpay.hash-secret=YOUR_HASH_SECRET_FROM_VNPAY

# URL return sau khi thanh toán
# Option 1: Return về backend
vnpay.return-url=http://your-domain.com/api/payment/vnpay-return

# Option 2: Return về frontend (khuyến nghị cho UX tốt hơn)
vnpay.return-url=http://your-frontend.com/payment-result
```

### 3. Chạy Database Migrations

```bash
# Chạy migration tạo bảng invoices
psql -U postgres -d battery_swap -f migration_invoices.sql

# Chạy migration tạo bảng payment_transactions
psql -U postgres -d battery_swap -f migration_payment_transactions.sql
```

---

## 🔄 FLOW THANH TOÁN

### **Bước 1: User swap pin → Tạo Invoice**

Khi user swap pin và vượt base usage:

```
POST /api/user/swap
Body: {
  "vehicleId": 8,
  "batterySerialId": 27,
  "stationId": 2,
  "endPercent": 20
}

→ System tự động tạo Invoice (status=PENDING) nếu vượt base
```

### **Bước 2: User xem invoice**

```
GET /api/user/invoices
→ Trả về danh sách invoice (bao gồm PENDING)
```

Response:

```json
[
  {
    "id": 5,
    "amount": 20739.0,
    "status": "PENDING",
    "description": "Overage: 1.50 kWh × 13826₫/kWh = 20739₫",
    "createdAt": "2025-11-07T14:30:00"
  }
]
```

### **Bước 3: Tạo URL thanh toán VNPay**

```
POST /api/payment/create-vnpay-url
Body: {
  "invoiceId": 5
}
```

Response:

```json
{
  "success": true,
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=2073900&vnp_Command=pay&...",
  "message": "Redirect user to this URL to complete payment"
}
```

**Frontend**: Redirect user đến `paymentUrl`

### **Bước 4: User thanh toán trên VNPay**

1. User nhập thông tin thẻ/tài khoản
2. VNPay xử lý thanh toán
3. VNPay redirect về `vnpay.return-url`

### **Bước 5: Backend xử lý callback**

VNPay gọi:

```
GET /api/payment/vnpay-return?vnp_TxnRef=INV5_xxx&vnp_ResponseCode=00&vnp_SecureHash=xxx
```

Backend:

1. Verify `vnp_SecureHash`
2. Kiểm tra `vnp_ResponseCode`:
   - `00` = SUCCESS → Cập nhật Invoice.status = PAID
   - Khác `00` = FAILED
3. Lưu `PaymentTransaction` vào DB
4. Return response cho frontend

---

## 🧪 TEST VỚI VNPAY SANDBOX

### Thông tin test (VNPay cung cấp)

**Thẻ nội địa (ATM)**

- Số thẻ: `9704198526191432198`
- Tên chủ thẻ: `NGUYEN VAN A`
- Ngày phát hành: `07/15`
- Mật khẩu OTP: `123456`

**Thẻ quốc tế (Visa/Master)**

- Số thẻ: `4026503926193577`
- Tên chủ thẻ: `NGUYEN VAN A`
- Ngày hết hạn: `12/25`
- CVV: `123`

### Flow test đầy đủ

```bash
# 1. Tạo invoice (swap pin vượt base)
POST http://localhost:8080/api/user/swap
Authorization: Bearer <JWT_TOKEN>
Body: {
  "vehicleId": 8,
  "batterySerialId": 27,
  "stationId": 2,
  "endPercent": 20
}

# 2. Xem invoice vừa tạo
GET http://localhost:8080/api/user/invoices
→ Lấy invoiceId (vd: 5)

# 3. Tạo VNPay payment URL
POST http://localhost:8080/api/payment/create-vnpay-url
Body: {"invoiceId": 5}
→ Lấy paymentUrl

# 4. Mở paymentUrl trong browser
→ Nhập thông tin thẻ test
→ Thanh toán

# 5. VNPay redirect về callback
→ Backend tự động cập nhật Invoice → PAID

# 6. Kiểm tra invoice đã PAID
GET http://localhost:8080/api/user/invoices
→ status should be "PAID"
```

---

## 📊 DATABASE SCHEMA

### `invoices` table

```sql
id | subscription_id | swap_transaction_id | amount | status | description | created_at | paid_at
```

### `payment_transactions` table

```sql
id | invoice_id | transaction_ref | amount | status | vnp_transaction_no | response_code | created_at | paid_at
```

---

## ⚠️ LƯU Ý QUAN TRỌNG

### 1. Return URL Configuration

**Option A: Return về Backend (đơn giản hơn)**

```properties
vnpay.return-url=http://your-domain.com/api/payment/vnpay-return
```

Backend xử lý và return JSON response.

**Option B: Return về Frontend (UX tốt hơn - KHUYẾN NGHỊ)**

```properties
vnpay.return-url=http://your-frontend.com/payment-result
```

Frontend nhận params, gọi API backend để verify:

```javascript
// Frontend code
const params = new URLSearchParams(window.location.search);
const txnRef = params.get("vnp_TxnRef");
const responseCode = params.get("vnp_ResponseCode");

// Gọi backend verify
fetch("/api/payment/vnpay-return?" + params.toString())
  .then((res) => res.json())
  .then((data) => {
    if (data.success) {
      showSuccess("Thanh toán thành công!");
    } else {
      showError("Thanh toán thất bại!");
    }
  });
```

### 2. Security

- ✅ Hash Secret KHÔNG được commit vào Git
- ✅ Sử dụng environment variables trong production
- ✅ Luôn verify `vnp_SecureHash` từ VNPay

### 3. Auto-Renew Logic

Subscription chỉ được gia hạn nếu **KHÔNG CÓ** invoice PENDING:

```java
if (invoiceService.hasPendingInvoices(subscription.getId())) {
    // ❌ BLOCK auto-renew
    log.warn("RENEW BLOCKED | reason=PENDING_INVOICES");
    continue;
}
// ✅ Gia hạn bình thường
```

### 4. Production Deployment

Khi deploy production, cập nhật:

```properties
vnpay.tmn-code=<PRODUCTION_TMN_CODE>
vnpay.hash-secret=<PRODUCTION_HASH_SECRET>
vnpay.pay-url=https://www.vnpay.vn/paymentv2/vpcpay.html  # URL production
vnpay.return-url=https://your-domain.com/api/payment/vnpay-return
```

---

## 🐛 TROUBLESHOOTING

### Lỗi "Invalid signature"

- Kiểm tra `vnpay.hash-secret` có đúng không
- Đảm bảo params được sort đúng thứ tự alphabetically

### Lỗi "Invoice already paid"

- Invoice đã được thanh toán trước đó
- Kiểm tra DB: `SELECT * FROM invoices WHERE id = X`

### Callback không được gọi

- Kiểm tra `vnpay.return-url` có accessible không
- VNPay sandbox chỉ gọi được public URL hoặc ngrok tunnel

### Test với localhost

Sử dụng **ngrok** để expose localhost:

```bash
ngrok http 8080
→ Lấy URL: https://abc123.ngrok.io
→ Cập nhật: vnpay.return-url=https://abc123.ngrok.io/api/payment/vnpay-return
```

---

## 📚 TÀI LIỆU THAM KHẢO

- VNPay Sandbox: https://sandbox.vnpayment.vn
- API Documentation: https://sandbox.vnpayment.vn/apis/docs
- Support: vnpaysandbox@gmail.com
