# Battery Reservation Feature

## 📋 Overview

Chức năng đặt trước pin cho phép user đặt trước pin tại trạm cụ thể. Hệ thống sẽ lock pin trong **1 giờ** để đảm bảo có pin sẵn sàng khi user đến swap.

---

## 🎯 Business Rules

### Validation

- ✅ User phải có subscription ACTIVE cho vehicle đó
- ✅ Mỗi vehicle chỉ được có **1 reservation ACTIVE** tại 1 thời điểm
- ✅ Quantity phải `<= maxBatteries` của subscription plan
- ✅ Pin được chọn phải có `chargePercent >= 95%` (tự động hoặc thủ công)
- ✅ Pin phải ở status `AVAILABLE` tại station được chọn

### Expiration

- ⏱️ Reservation expire sau **1 giờ** từ khi tạo
- 🤖 Cron job chạy **mỗi 1 phút** để auto-expire
- 🔓 Khi expire: Batteries được release (`RESERVED` → `AVAILABLE`)

### Status Flow

```
ACTIVE ──────┬──→ USED (khi swap thành công)
             ├──→ EXPIRED (hết 1 giờ, auto-expire)
             └──→ CANCELLED (user/admin hủy)
```

---

## 🏗️ Architecture

### Database Schema

#### `reservations` table

```sql
id                  BIGSERIAL PRIMARY KEY
user_id             BIGINT NOT NULL
vehicle_id          BIGINT NOT NULL
station_id          BIGINT NOT NULL
subscription_id     BIGINT NOT NULL
status              VARCHAR(20) NOT NULL  -- ACTIVE, USED, EXPIRED, CANCELLED
quantity            INT NOT NULL
reserved_at         TIMESTAMP NOT NULL
expire_at           TIMESTAMP NOT NULL    -- reserved_at + 1 hour
used_at             TIMESTAMP
swap_transaction_id BIGINT
cancelled_at        TIMESTAMP
cancel_reason       VARCHAR(255)
```

#### `reservation_items` table (junction table)

```sql
id              BIGSERIAL PRIMARY KEY
reservation_id  BIGINT NOT NULL
battery_id      BIGINT NOT NULL
```

### Entity Relationships

```
Reservation ────┬─── User (ManyToOne)
                ├─── Vehicle (ManyToOne)
                ├─── Station (ManyToOne)
                ├─── Subscription (ManyToOne)
                └─── ReservationItems (OneToMany)
                         └─── BatterySerial (ManyToOne)
```

---

## 📡 API Endpoints

### 1. Create Reservation

**POST** `/api/user/reservations`

**Request:**

```json
{
  "vehicleId": 1,
  "stationId": 5,
  "quantity": 2,
  "batteryIds": [101, 102] // Optional - auto-select nếu null
}
```

**Response (201 Created):**

```json
{
  "reservationId": 123,
  "status": "ACTIVE",
  "vehicle": {
    "id": 1,
    "vin": "VIN123",
    "modelName": "VinFast Klara"
  },
  "station": {
    "id": 5,
    "name": "Station A",
    "address": "123 Main St"
  },
  "quantity": 2,
  "batteries": [
    {
      "id": 101,
      "serialNumber": "BAT-001",
      "chargePercent": 98.5,
      "stateOfHealth": 95.0
    },
    {
      "id": 102,
      "serialNumber": "BAT-002",
      "chargePercent": 97.0,
      "stateOfHealth": 96.0
    }
  ],
  "reservedAt": "2024-01-15T10:00:00",
  "expireAt": "2024-01-15T11:00:00",
  "remainingMinutes": 60,
  "message": "Reservation active. Batteries are held for you until 2024-01-15T11:00:00. Please come to swap within 60 minutes.",
  "swapTransactionId": null,
  "usedAt": null,
  "cancelReason": null
}
```

**Error Cases:**

- `400 Bad Request`: "This vehicle already has an ACTIVE reservation"
- `400 Bad Request`: "Reservation quantity (3) exceeds plan limit (2 batteries)"
- `400 Bad Request`: "Not enough AVAILABLE batteries at station"
- `404 Not Found`: "No active subscription for this vehicle"

---

### 2. Get Active Reservation

**GET** `/api/user/reservations/active?vehicleId=1`

**Response (200 OK):**

```json
{
  "reservationId": 123,
  "status": "ACTIVE",
  ...
}
```

**Response (204 No Content):**
Nếu vehicle không có reservation ACTIVE.

---

### 3. Get Reservation History

**GET** `/api/user/reservations`

**Response (200 OK):**

```json
[
  {
    "reservationId": 125,
    "status": "USED",
    "vehicle": { ... },
    "station": { ... },
    "usedAt": "2024-01-15T10:30:00",
    "swapTransactionId": 456,
    "message": "Reservation has been used for battery swap."
  },
  {
    "reservationId": 124,
    "status": "EXPIRED",
    "vehicle": { ... },
    "station": { ... },
    "cancelReason": "Auto-expired after 1 hour",
    "message": "Reservation has expired. Batteries have been released."
  }
]
```

---

### 4. Get Reservation Detail

**GET** `/api/user/reservations/{id}`

**Response (200 OK):**

```json
{
  "reservationId": 123,
  "status": "ACTIVE",
  ...
}
```

**Error Cases:**

- `404 Not Found`: "Reservation not found"
- `403 Forbidden`: "Reservation does not belong to this user"

---

### 5. Cancel Reservation

**DELETE** `/api/user/reservations/{id}`

**Request Body (optional):**

```json
{
  "reason": "Không đến được trạm"
}
```

**Response (200 OK):**

```json
{
  "reservationId": 123,
  "status": "CANCELLED",
  "cancelReason": "Không đến được trạm",
  "message": "Reservation has been cancelled."
}
```

**Error Cases:**

- `400 Bad Request`: "Cannot cancel reservation with status USED"
- `404 Not Found`: "Reservation not found"

---

## ⚙️ Implementation Details

### Battery Selection Logic

#### Auto-Select (batteryIds = null)

```java
// Query batteries:
// 1. station_id = ?
// 2. status = AVAILABLE
// 3. charge_percent >= 95%
// 4. ORDER BY charge_percent DESC, state_of_health DESC
// 5. LIMIT quantity

List<BatterySerial> batteries = batterySerialRepository
    .findByStation(station).stream()
    .filter(b -> b.getStatus() == BatteryStatus.AVAILABLE)
    .filter(b -> b.getChargePercent() >= 95.0)
    .sorted((b1, b2) -> {
        // Ưu tiên: chargePercent DESC, sau đó SoH DESC
        int chargeCompare = Double.compare(b2.getChargePercent(), b1.getChargePercent());
        return chargeCompare != 0 ? chargeCompare :
               Double.compare(b2.getStateOfHealth(), b1.getStateOfHealth());
    })
    .limit(request.getQuantity())
    .collect(Collectors.toList());
```

#### Manual Selection (batteryIds = [101, 102])

```java
// Validate:
// 1. batteryIds.size() == quantity
// 2. Tất cả batteries tồn tại
// 3. Thuộc về station được chọn
// 4. Status = AVAILABLE

if (battery.getStation().getId() != stationId) {
    throw new RuntimeException("Battery does not belong to station");
}
if (battery.getStatus() != BatteryStatus.AVAILABLE) {
    throw new RuntimeException("Battery is not AVAILABLE");
}
```

---

### Reservation-Swap Integration

Khi staff confirm swap (`SwapConfirmServiceImpl.confirmSwap()`):

```java
// Sau khi swap COMPLETED, check reservation
reservationRepository
    .findByUserIdAndVehicleIdAndStationIdAndStatus(
        userId, vehicleId, stationId, ReservationStatus.ACTIVE
    )
    .ifPresent(reservation -> {
        reservation.setStatus(ReservationStatus.USED);
        reservation.setUsedAt(LocalDateTime.now());
        reservation.setSwapTransactionId(tx.getId());
        reservationRepository.save(reservation);

        log.info("RESERVATION USED | reservationId={} | swapTxId={}",
                 reservation.getId(), tx.getId());
    });
```

**Flow:**

1. User tạo reservation → Batteries `RESERVED`
2. User đến trạm swap
3. Staff confirm → Swap `COMPLETED`
4. Hệ thống tự động mark reservation → `USED`
5. Link `swapTransactionId` để tracking

---

### Auto-Expire Scheduler

**ReservationScheduler.java**

```java
@Scheduled(cron = "0 */1 * * * ?")  // Mỗi 1 phút
public void autoExpireReservations() {
    // 1. Find: status = ACTIVE AND expireAt < now()
    // 2. Release batteries: RESERVED → AVAILABLE
    // 3. Update reservation: ACTIVE → EXPIRED
}
```

**Cron Expression: `0 */1 * * * ?`**

- `0` = Giây thứ 0
- `*/1` = Mỗi 1 phút
- `*` = Mọi giờ
- `*` = Mọi ngày
- `*` = Mọi tháng
- `?` = Không quan tâm thứ

**Timeline Example:**

```
10:00:00 → User tạo reservation (expireAt = 11:00:00)
10:01:00 → Scheduler chạy (chưa expire)
10:02:00 → Scheduler chạy (chưa expire)
...
11:00:00 → Scheduler chạy (chưa expire vì đúng expireAt)
11:01:00 → Scheduler chạy → EXPIRE! (now > expireAt)
```

---

## 📊 Logging Strategy

### Create Reservation

```log
INFO  CREATE RESERVATION | userId=1 | vehicleId=5 | stationId=3 | quantity=2
INFO  AUTO-SELECTED BATTERIES | stationId=3 | required=2 | found=2 |
      batteries=[BAT-001(98%/95%SoH), BAT-002(97%/96%SoH)]
INFO  BATTERIES LOCKED | stationId=3 | count=2 | batteries=[BAT-001, BAT-002]
INFO  RESERVATION CREATED | reservationId=123 | userId=1 | vehicleId=5 |
      stationId=3 | quantity=2 | expireAt=2024-01-15T11:00:00
```

### Cancel Reservation

```log
INFO  CANCEL RESERVATION | userId=1 | reservationId=123 | reason=Không đến được
INFO  BATTERIES RELEASED | reservationId=123 | count=2 | batteries=[BAT-001, BAT-002]
INFO  RESERVATION CANCELLED | reservationId=123 | userId=1 | reason=Không đến được
```

### Auto-Expire

```log
INFO  AUTO-EXPIRE: Found 3 expired reservations
INFO  RESERVATION EXPIRED | reservationId=120 | userId=1 | vehicleId=5 |
      batteries=[BAT-001, BAT-002]
INFO  RESERVATION EXPIRED | reservationId=121 | userId=2 | vehicleId=8 |
      batteries=[BAT-010]
```

### Reservation Used (Swap Integration)

```log
INFO  CONFIRM_SWAP | staff=staff1 | txId=456 | oldBattery=BAT-003 -> station=3 |
      newBattery=BAT-001 -> vehicle=5
INFO  RESERVATION USED | reservationId=123 | swapTxId=456 | userId=1 |
      vehicleId=5 | stationId=3
```

---

## 🧪 Testing Scenarios

### Scenario 1: Normal Reservation Flow

1. ✅ User có subscription ACTIVE
2. ✅ User tạo reservation với quantity=2
3. ✅ Hệ thống auto-select 2 pin tốt nhất (charge >= 95%)
4. ✅ Batteries lock → `RESERVED`
5. ✅ User đến swap trong 1 giờ
6. ✅ Staff confirm → Reservation `USED`

### Scenario 2: Reservation Expired

1. ✅ User tạo reservation
2. ❌ User không đến trong 1 giờ
3. ⏱️ Scheduler auto-expire sau 1 giờ
4. ✅ Batteries released → `AVAILABLE`
5. ✅ Reservation status → `EXPIRED`

### Scenario 3: User Cancel

1. ✅ User tạo reservation
2. ✅ User hủy với reason "Không đến được"
3. ✅ Batteries released → `AVAILABLE`
4. ✅ Reservation status → `CANCELLED`

### Scenario 4: Duplicate Reservation (Error)

1. ✅ User tạo reservation cho vehicle A
2. ❌ User tạo reservation thứ 2 cho vehicle A
3. 🚫 Error: "This vehicle already has an ACTIVE reservation"

### Scenario 5: Quantity Exceeds Plan (Error)

1. ✅ User có plan cho phép maxBatteries=2
2. ❌ User tạo reservation với quantity=3
3. 🚫 Error: "Reservation quantity (3) exceeds plan limit (2 batteries)"

### Scenario 6: Manual Battery Selection

1. ✅ User chọn batteries [101, 102]
2. ✅ Hệ thống validate: thuộc station, AVAILABLE
3. ✅ Lock batteries đã chọn
4. ✅ Reservation created

### Scenario 7: Not Enough Batteries (Error)

1. ✅ User yêu cầu quantity=5
2. ❌ Station chỉ có 3 batteries AVAILABLE (charge >= 95%)
3. 🚫 Error: "Not enough AVAILABLE batteries at station"

---

## 🔧 Configuration

### Application Properties

```properties
# Enable scheduling
spring.task.scheduling.enabled=true

# Database auto-create tables
spring.jpa.hibernate.ddl-auto=update
```

### Enable Scheduling

```java
@EnableScheduling
@SpringBootApplication
public class BatterySwapStationManagementSystemApplication {
    // ...
}
```

---

## 📦 Files Created

### Entities

- `Reservation.java` (~170 lines)
- `ReservationItem.java` (~50 lines)
- `ReservationStatus.java` (enum)
- `BatteryStatus.java` (updated - added RESERVED)

### Repositories

- `ReservationRepository.java` (~130 lines)
- `ReservationItemRepository.java`

### DTOs

- `ReservationRequest.java`
- `ReservationResponse.java` (~100 lines)
- `CancelReservationRequest.java`

### Service Layer

- `ReservationService.java` (interface)
- `ReservationServiceImpl.java` (~450 lines)

### Controller

- `ReservationController.java` (~150 lines)

### Scheduler

- `ReservationScheduler.java`

### Integration

- `SwapConfirmServiceImpl.java` (updated - added reservation check)

---

## 🎓 Key Concepts

### Why Vehicle-Based Instead of User-Based?

```
❌ User-based: "User chỉ có 1 reservation ACTIVE"
   → Problem: User có 2 xe, chỉ đặt được cho 1 xe

✅ Vehicle-based: "Mỗi vehicle chỉ có 1 reservation ACTIVE"
   → Solution: User có 2 xe, đặt được cho cả 2 xe (mỗi xe 1 reservation)
```

### Why 1 Hour Expiration?

- ⏱️ Đủ thời gian để user di chuyển đến trạm
- 🔒 Không lock pin quá lâu ảnh hưởng user khác
- 💡 Balance giữa UX và resource utilization

### Why Charge >= 95%?

- 🔋 Đảm bảo pin "gần như đầy" cho user
- 📊 Tránh chọn pin đang sạc dở dang
- ⚡ Tối ưu trải nghiệm user sau swap

---

## 🚀 Next Steps (Optional Enhancements)

1. **Admin Dashboard**

   - View all active reservations
   - Manually cancel reservations
   - Statistics: reservation usage rate

2. **Notifications**

   - Push notification 10 phút trước expire
   - Email confirmation sau khi tạo reservation
   - SMS reminder

3. **Dynamic Expiration**

   - VIP users: 2 giờ
   - Normal users: 1 giờ
   - Based on distance to station

4. **Reservation Priority**

   - VIP queue khi pin ít
   - First-come-first-served logic

5. **Analytics**
   - Reservation success rate (USED / total)
   - Average time from reserve to swap
   - Most popular reservation time slots

---

## 📝 Notes

- 🔐 Tất cả endpoints yêu cầu JWT authentication
- 🎯 Chỉ USER role mới có quyền tạo/hủy reservation
- 📊 Reservation không tính phí (free feature)
- 🔗 Tự động link với swap transaction khi swap
- ⚠️ Không thể cancel reservation đã USED/EXPIRED
- 🤖 Scheduler chạy mỗi 1 phút để tối ưu performance

---

**Created by:** GitHub Copilot  
**Date:** 2024-01-15  
**Version:** 1.0
