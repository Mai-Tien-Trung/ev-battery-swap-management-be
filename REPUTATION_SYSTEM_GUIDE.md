# 📊 HỆ THỐNG UY TÍN RESERVATION

## 🎯 MỤC ĐÍCH

Ngăn chặn user lạm dụng hệ thống đặt lịch bằng cách:
- Hủy reservation liên tục
- Đặt lịch nhưng không đến swap (hết hạn)
- Làm lãng phí pin đã được lock cho reservation

## 📋 QUY TẮC UY TÍN

### Điểm Ban Đầu
- **Mỗi tháng**: User có **6 điểm uy tín**
- Điểm reset về 6 vào đầu tháng mới

### Cơ Chế Trừ Điểm

| Hành Vi | Trừ Điểm | Lý Do |
|---------|----------|-------|
| Hủy reservation (CANCELLED) | **-1 điểm** | User tự hủy đặt lịch |
| Hết hạn không swap (EXPIRED) | **-2 điểm** | Nghiêm trọng hơn: pin bị lock 1h nhưng không dùng |
| Swap thành công (USED) | **0 điểm** | Không thưởng/trừ (đây là hành vi bình thường) |

### Ngưỡng Chặn

```
Điểm uy tín > 0  ✅ Được phép đặt lịch
Điểm uy tín <= 0 ❌ KHÔNG được đặt lịch (chờ tháng sau)
```

## 🔄 FLOW HOẠT ĐỘNG

### 1. User Tạo Reservation

```
POST /api/user/reservations
{
  "vehicleId": 5,
  "stationId": 3,
  "quantity": 2
}

↓ ReservationController
↓ ReservationService.createReservation()
├─ Step 0: ✅ CHECK UY TÍN (MỚI)
│  └─ reputationService.validateReputationForReservation(userId)
│     ├─ Tính điểm = 6 - (cancelled × 1) - (expired × 2)
│     ├─ Nếu điểm <= 0 → THROW EXCEPTION
│     └─ Nếu điểm > 0 → PASS, tiếp tục tạo reservation
├─ Step 1: Validate user, vehicle, subscription
├─ Step 2-7: Tạo reservation như cũ
└─ Return response
```

### 2. User Get Uy Tín

```
GET /api/user/reputation

Response:
{
  "currentReputation": 4,       // Điểm hiện tại
  "maxReputation": 6,            // Điểm tối đa
  "cancelledCount": 1,           // Số lần hủy trong tháng
  "expiredCount": 0,             // Số lần hết hạn trong tháng
  "usedCount": 3,                // Số lần swap thành công
  "canReserve": true,            // Còn được đặt lịch không?
  "message": "Uy tín tốt: 4/6 điểm. Bạn có thể đặt lịch bình thường."
}
```

### 3. Ví Dụ Tính Điểm

**Scenario 1: User Bình Thường**
```
Tháng 11:
- Tạo 5 reservations
- Swap thành công: 4 lần (USED)
- Hủy: 1 lần (CANCELLED)

Điểm = 6 - (1 × 1) - (0 × 2) = 5 điểm ✅
→ Vẫn đặt lịch được bình thường
```

**Scenario 2: User Vi Phạm Nhẹ**
```
Tháng 11:
- Tạo 6 reservations
- Swap thành công: 2 lần (USED)
- Hủy: 3 lần (CANCELLED)
- Hết hạn: 1 lần (EXPIRED)

Điểm = 6 - (3 × 1) - (1 × 2) = 1 điểm ⚠️
→ Vẫn đặt được nhưng cần cẩn thận
```

**Scenario 3: User Vi Phạm Nghiêm Trọng**
```
Tháng 11:
- Tạo 8 reservations
- Swap thành công: 1 lần (USED)
- Hủy: 4 lần (CANCELLED)
- Hết hạn: 2 lần (EXPIRED)

Điểm = 6 - (4 × 1) - (2 × 2) = -2 → 0 điểm ❌
→ KHÔNG được đặt lịch nữa trong tháng này
→ Phải đợi đến tháng 12 (reset về 6 điểm)
```

## 📁 CẤU TRÚC CODE

### Entities
- **Reservation.java**: Entity gốc (không thay đổi)
  - Chỉ query data, không tạo bảng mới

### Repositories
- **ReservationRepository.java**:
  ```java
  // Query reservations trong khoảng thời gian (tháng)
  List<Reservation> findByUserIdAndReservedAtBetween(
      Long userId, 
      LocalDateTime startDate, 
      LocalDateTime endDate
  );
  ```

### Services
- **ReputationService.java**: Interface
  - `getUserReputation(userId)`: Lấy thông tin uy tín
  - `validateReputationForReservation(userId)`: Validate trước khi tạo reservation
  - `calculateReputation(userId)`: Tính điểm uy tín

- **ReputationServiceImpl.java**: Implementation
  ```java
  public int calculateReputation(Long userId) {
      // 1. Lấy tháng hiện tại
      YearMonth currentMonth = YearMonth.now();
      LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
      LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
      
      // 2. Query reservations trong tháng
      List<Reservation> reservations = reservationRepository
              .findByUserIdAndReservedAtBetween(userId, startOfMonth, endOfMonth);
      
      // 3. Đếm theo status
      long cancelledCount = reservations.stream()
              .filter(r -> r.getStatus() == ReservationStatus.CANCELLED)
              .count();
      
      long expiredCount = reservations.stream()
              .filter(r -> r.getStatus() == ReservationStatus.EXPIRED)
              .count();
      
      // 4. Tính điểm
      int reputation = 6 - (cancelledCount × 1) - (expiredCount × 2);
      
      return Math.max(0, reputation); // Không âm
  }
  ```

- **ReservationServiceImpl.java**: Tích hợp reputation check
  ```java
  @Override
  public ReservationResponse createReservation(Long userId, ReservationRequest request) {
      // ===== 0. CHECK UY TÍN - BƯỚC MỚI =====
      reputationService.validateReputationForReservation(userId);
      
      // ===== 1-8. Các bước khác như cũ =====
      ...
  }
  ```

### Controllers
- **ReputationController.java**:
  ```java
  @GetMapping
  public ResponseEntity<ReputationResponse> getMyReputation(@AuthenticationPrincipal CustomUserDetails userDetails) {
      Long userId = userDetails.getId();
      ReputationResponse reputation = reputationService.getUserReputation(userId);
      return ResponseEntity.ok(reputation);
  }
  ```

### DTOs
- **ReputationResponse.java**: Response chứa thông tin uy tín

## 🧪 TESTING

### Test Case 1: User Có Uy Tín Tốt
```bash
# 1. Get uy tín
GET /api/user/reputation
→ { "currentReputation": 6, "canReserve": true }

# 2. Tạo reservation
POST /api/user/reservations { "vehicleId": 5, "stationId": 3, "quantity": 1 }
→ 201 Created ✅
```

### Test Case 2: User Hết Uy Tín
```bash
# Setup: User đã có 4 cancelled + 2 expired trong tháng
# Điểm = 6 - (4×1) - (2×2) = -2 → 0

# 1. Get uy tín
GET /api/user/reputation
→ { "currentReputation": 0, "canReserve": false, "message": "Hết uy tín..." }

# 2. Thử tạo reservation
POST /api/user/reservations { "vehicleId": 5, "stationId": 3, "quantity": 1 }
→ 400 Bad Request ❌
{
  "error": "Bạn không thể đặt lịch do hết uy tín (0/6 điểm). 
           Trong tháng này: 4 lần hủy, 2 lần hết hạn. 
           Vui lòng đợi đến tháng sau để đặt lịch lại."
}
```

### Test Case 3: Reset Đầu Tháng Mới
```bash
# Ngày 30/11: User có 0 điểm (hết uy tín)
GET /api/user/reputation
→ { "currentReputation": 0 }

# Ngày 01/12: Tháng mới, query mới, reset về 6
GET /api/user/reputation
→ { "currentReputation": 6, "canReserve": true }
```

## 📊 DATABASE QUERIES

### Query Tính Uy Tín (SQL tương đương)
```sql
-- Lấy reservations trong tháng 11/2025
SELECT *
FROM reservations
WHERE user_id = ?
  AND reserved_at >= '2025-11-01 00:00:00'
  AND reserved_at <= '2025-11-30 23:59:59';

-- Đếm theo status
SELECT 
    COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_count,
    COUNT(CASE WHEN status = 'EXPIRED' THEN 1 END) as expired_count,
    COUNT(CASE WHEN status = 'USED' THEN 1 END) as used_count
FROM reservations
WHERE user_id = ?
  AND reserved_at >= '2025-11-01 00:00:00'
  AND reserved_at <= '2025-11-30 23:59:59';

-- Tính điểm
-- reputation = 6 - (cancelled_count × 1) - (expired_count × 2)
```

## 🔐 SECURITY

### Authorization
- ✅ Chỉ user đăng nhập mới xem được uy tín của chính họ
- ✅ Sử dụng `@AuthenticationPrincipal` để lấy userId từ JWT token
- ✅ Không thể xem uy tín của user khác

### Business Rules
- ✅ Không tạo bảng mới → Giảm complexity
- ✅ Query real-time từ `reservations` table
- ✅ Tự động reset mỗi tháng (query theo range date)

## 📈 MONITORING & LOGS

### Log Patterns
```
INFO  | REPUTATION CALCULATED | userId=5 | reputation=4/6 | cancelled=1 | expired=0 | used=3 | canReserve=true
WARN  | REPUTATION CHECK FAILED | userId=5 | reputation=0 | cancelled=4 | expired=2
INFO  | REPUTATION CHECK PASSED | userId=5 | reputation=5/6
```

### Metrics để Track
- Số user bị chặn đặt lịch mỗi tháng
- Tỷ lệ cancelled/expired trên tổng reservations
- Phân bố điểm uy tín (bao nhiêu user có 6, 5, 4... điểm)

## 🚀 DEPLOYMENT NOTES

### Không Cần Migration
- ✅ Không tạo bảng mới
- ✅ Chỉ thêm method vào existing repository
- ✅ Safe để deploy

### Rollback Plan
- Nếu có vấn đề: Comment dòng check uy tín trong `ReservationServiceImpl`:
  ```java
  // reputationService.validateReputationForReservation(userId);
  ```

## 💡 FUTURE ENHANCEMENTS

### Có Thể Mở Rộng:
1. **Bonus Points**: Thưởng +1 điểm nếu swap đúng giờ (trong 30 phút đầu)
2. **VIP Tier**: User trung thành có 8 điểm/tháng thay vì 6
3. **Grace Period**: Lần vi phạm đầu tiên chỉ cảnh báo, không trừ điểm
4. **Appeal System**: User có thể kháng cáo nếu có lý do chính đáng

## 📞 SUPPORT

Nếu user hỏi "Tại sao tôi không đặt lịch được?":
1. Kiểm tra uy tín: `GET /api/user/reputation`
2. Xem lịch sử: `GET /api/user/reservations`
3. Giải thích: Quá nhiều cancelled/expired trong tháng
4. Hướng dẫn: Đợi đến tháng sau hoặc liên hệ support
