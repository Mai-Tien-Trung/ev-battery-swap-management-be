# VNPAY Integration Guide

## Tổng quan

Dự án đã được tích hợp VNPAY payment gateway để xử lý thanh toán cho dịch vụ sạc pin xe điện. Tích hợp này bao gồm:

- **VnpayConfig**: Class cấu hình và utility cho VNPAY
- **Payment Entity**: Entity lưu trữ thông tin giao dịch
- **PaymentController**: REST API endpoints cho thanh toán
- **BatterySwapService**: Service xử lý logic thanh toán
- **Test Cases**: Đầy đủ test cases cho tất cả components

## Cấu hình

### 1. Application Properties

Thêm các cấu hình sau vào `application.properties`:

```properties
# VNPAY Configuration
# TODO: Replace with your actual VNPAY provided values
vnpay.tmnCode=YOUR_TMN_CODE
vnpay.secretKey=YOUR_SECRET_KEY
vnpay.payUrl=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.returnUrl=http://localhost:8080/api/payment/vnpay-return
vnpay.apiUrl=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
```

### 2. Dependencies

Đã thêm Gson dependency vào `pom.xml`:

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

## API Endpoints

### 1. Khởi tạo thanh toán

**POST** `/api/payment/init`

**Parameters:**
- `amount` (required): Số tiền thanh toán (VND)
- `bankCode` (optional): Mã ngân hàng
- `language` (optional): Ngôn ngữ (mặc định: "vn")
- `orderInfo` (optional): Mô tả đơn hàng

**Response:**
```json
{
  "code": "00",
  "message": "success",
  "data": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?..."
}
```

### 2. Callback từ VNPAY

**GET** `/api/payment/vnpay-return`

**Parameters:** (tự động từ VNPAY)
- `vnp_TxnRef`: Mã giao dịch
- `vnp_ResponseCode`: Mã phản hồi
- `vnp_TransactionNo`: Mã giao dịch VNPAY
- `vnp_SecureHash`: Chữ ký xác thực

**Response:**
```json
{
  "code": "00",
  "message": "Payment successful",
  "txnRef": "12345678",
  "status": "SUCCESS"
}
```

### 3. Kiểm tra trạng thái thanh toán

**GET** `/api/payment/status/{txnRef}`

**Response:**
```json
{
  "code": "00",
  "message": "success",
  "txnRef": "12345678",
  "amount": 100000,
  "status": "SUCCESS",
  "createdAt": "2023-12-01T12:00:00"
}
```

## Cấu trúc Database

### Payment Entity

```java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "txn_ref", unique = true, nullable = false)
    private String txnRef;
    
    @Column(name = "amount", nullable = false)
    private Long amount;
    
    @Column(name = "order_info")
    private String orderInfo;
    
    @Column(name = "bank_code")
    private String bankCode;
    
    @Column(name = "language")
    private String language;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;
    
    // ... other fields
}
```

### Payment Status

```java
public enum PaymentStatus {
    PENDING,    // Đang chờ thanh toán
    SUCCESS,    // Thanh toán thành công
    FAILED,     // Thanh toán thất bại
    CANCELLED   // Thanh toán bị hủy
}
```

## Sử dụng

### 1. Khởi tạo thanh toán

```bash
curl -X POST "http://localhost:8080/api/payment/init" \
  -d "amount=100000" \
  -d "bankCode=NCB" \
  -d "language=vn" \
  -d "orderInfo=Thanh toan dich vu sac pin"
```

### 2. Kiểm tra trạng thái

```bash
curl "http://localhost:8080/api/payment/status/12345678"
```

## Test Cases

### Chạy tests

```bash
mvn test
```

### Test Coverage

- **PaymentControllerTest**: Test tất cả endpoints
- **VnpayConfigTest**: Test utility methods
- **BatterySwapServiceTest**: Test business logic

## Lưu ý quan trọng

### 1. Cấu hình VNPAY

- **tmnCode**: Lấy từ VNPAY portal
- **secretKey**: Lấy từ VNPAY portal (KHÔNG chia sẻ công khai)
- **returnUrl**: URL callback của ứng dụng

### 2. Xử lý lỗi

- **RspCode=97**: Lỗi chữ ký - kiểm tra secretKey
- **RspCode=99**: Lỗi khác - kiểm tra logs

### 3. Security

- Luôn verify signature từ VNPAY
- Không hardcode secretKey trong code
- Sử dụng HTTPS trong production

### 4. Testing

- Sử dụng sandbox environment
- Test với các amount khác nhau
- Verify signature trong test cases

## Mở rộng

### 1. Tính toán amount

Có thể mở rộng `BatterySwapService.calculatePaymentAmount()` để tính toán dựa trên:
- Thời gian sạc
- Loại dịch vụ
- Gói subscription
- Vị trí trạm sạc

### 2. Webhook

Có thể thêm webhook endpoint để nhận thông báo real-time từ VNPAY.

### 3. Refund

Có thể thêm chức năng hoàn tiền thông qua VNPAY API.

## Troubleshooting

### 1. Lỗi chữ ký

```
RspCode=97: Invalid signature
```

**Giải pháp:**
- Kiểm tra secretKey có đúng không
- Kiểm tra thứ tự parameters
- Kiểm tra encoding (US_ASCII)

### 2. Lỗi amount

```
RspCode=24: Invalid amount
```

**Giải pháp:**
- Amount phải nhân 100 (VNPAY dùng VND * 100)
- Amount phải > 0

### 3. Lỗi timeout

```
RspCode=51: Transaction timeout
```

**Giải pháp:**
- Kiểm tra expireDate (+15 phút)
- Kiểm tra timezone (Etc/GMT+7)

## Liên hệ

Nếu có vấn đề với tích hợp VNPAY, vui lòng liên hệ:
- VNPAY Support: support@vnpay.vn
- Documentation: https://sandbox.vnpayment.vn/apis/
