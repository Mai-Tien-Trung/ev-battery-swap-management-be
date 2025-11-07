# Payment Integration Summary

## ✅ Hoàn Tất Tích Hợp Thanh Toán

Hệ thống đã tích hợp **3 loại thanh toán**:

### 1. ⚡ Swap Overage Payment
**Khi nào:** User đổi pin vượt base usage (mileage/energy)
**Flow:**
- Swap pin → Tính overage → Tạo invoice (SWAP_OVERAGE)
- User thanh toán qua VNPay
- Invoice → PAID

**Invoice Fields:**
- `invoiceType`: `"SWAP_OVERAGE"`
- `swapTransaction`: có
- `usageType`, `overage`, `rate`: có
- Description: `"Overage: 1.5 kWh × 13,826₫/kWh = 20,739₫"`

---

### 2. 🆕 Initial Subscription Payment (NEW)
**Khi nào:** User đăng ký gói lần đầu (linkVehicle)
**Flow:**
1. POST /api/user/vehicles/link
   - Tạo vehicle
   - Tạo subscription (status: **PENDING**)
   - Tạo batteries (status: **AVAILABLE**, vehicle: null)
   - Tạo invoice (SUBSCRIPTION_RENEWAL)
   - Return invoiceId

2. User thanh toán invoice qua VNPay

3. VNPay callback success:
   - Invoice → PAID
   - **Auto trigger `activateSubscription()`**
   - Subscription: PENDING → **ACTIVE**
   - Batteries: AVAILABLE → **IN_USE**, gán cho vehicle

**Invoice Fields:**
- `invoiceType`: `"SUBSCRIPTION_RENEWAL"` (dùng chung)
- `swapTransaction`: null
- Phân biệt: `subscription.status == PENDING`
- Description: `"Subscription Renewal: Premium Plan - 299000₫"`

**Code Changes:**
```java
// LinkVehicleServiceImpl.java
subscription.setStatus(SubscriptionStatus.PENDING);  // Chờ payment
battery.setStatus(BatteryStatus.AVAILABLE);
battery.setVehicle(null);  // Chưa gán

Invoice invoice = invoiceService.createSubscriptionRenewalInvoice(
    subscription, plan.getPrice(), plan.getName()
);

return LinkVehicleResponse.builder()
    .invoiceId(invoice.getId())
    .invoiceAmount(invoice.getAmount())
    .build();
```

```java
// SubscriptionServiceImpl.java - NEW METHOD
@Transactional
public Subscription activateSubscription(Long subscriptionId) {
    // 1. Check PENDING status
    // 2. Check no pending invoices
    // 3. Subscription → ACTIVE
    // 4. Assign batteries: AVAILABLE → IN_USE
}
```

```java
// VNPayServiceImpl.java
if (subscription.getStatus() == SubscriptionStatus.PENDING) {
    subscriptionService.activateSubscription(subscriptionId);
}
```

---

### 3. 🔄 Subscription Renewal Payment
**Khi nào:** Subscription hết hạn (auto-renew job)
**Flow:**
1. Auto-renew job chạy:
   - Tìm subscriptions hết hạn
   - Check pending invoices → BLOCK nếu có
   - Tạo renewal invoice
   - **KHÔNG renew ngay** (chờ payment)

2. User thanh toán invoice qua VNPay

3. VNPay callback success:
   - Invoice → PAID
   - **Auto trigger `completeRenewal()`**
   - Old subscription → COMPLETED
   - New subscription → ACTIVE (plan mới nếu có nextPlanId)

**Invoice Fields:**
- `invoiceType`: `"SUBSCRIPTION_RENEWAL"`
- `swapTransaction`: null
- Phân biệt: `subscription.status == ACTIVE/COMPLETED`
- Description: `"Subscription Renewal: Premium Plan - 299000₫"`

**Code Changes:**
```java
// SubscriptionServiceImpl.autoRenewSubscriptions()
// Tạo invoice, KHÔNG renew
Invoice invoice = invoiceService.createSubscriptionRenewalInvoice(
    subscription, newPlan.getPrice(), newPlan.getName()
);
// BLOCK - chờ payment
```

```java
// SubscriptionServiceImpl.completeRenewal() - NEW METHOD
@Transactional
public Subscription completeRenewal(Long subscriptionId) {
    // 1. Check no pending invoices
    // 2. Old sub → COMPLETED
    // 3. Create new sub → ACTIVE
    // 4. Reset usage counters
}
```

```java
// VNPayServiceImpl.java
if ("SUBSCRIPTION_RENEWAL".equals(invoice.getInvoiceType())) {
    subscriptionService.completeRenewal(subscriptionId);
}
```

---

## 🗂️ Database Changes

### Invoice Table
```sql
-- swap_transaction_id nullable
ALTER TABLE invoices 
    ALTER COLUMN swap_transaction_id DROP NOT NULL;

-- invoice_type column
ALTER TABLE invoices
    ADD COLUMN invoice_type VARCHAR(50);

-- usage_type, overage, rate nullable
ALTER TABLE invoices
    ALTER COLUMN usage_type DROP NOT NULL,
    ALTER COLUMN overage DROP NOT NULL,
    ALTER COLUMN rate DROP NOT NULL;
```

### SubscriptionStatus Enum
```java
public enum SubscriptionStatus {
    AVAILABLE,
    PENDING,    // ⚠️ NEW - Chờ thanh toán initial invoice
    ACTIVE,
    COMPLETED,
    CANCELLED
}
```

---

## 📝 API Changes

### LinkVehicleResponse
```java
public class LinkVehicleResponse {
    // ... existing fields
    private Long invoiceId;        // NEW
    private Double invoiceAmount;  // NEW
}
```

### SubscriptionService
```java
// NEW METHODS
Subscription activateSubscription(Long subscriptionId);
Subscription completeRenewal(Long subscriptionId);
```

### BatterySerialRepository
```java
// NEW QUERIES
List<BatterySerial> findByStatusAndVehicleIsNull(BatteryStatus status);
List<BatterySerial> findByVehicleId(Long vehicleId);
```

---

## 🔄 Complete Payment Flows

### Flow 1: Link Vehicle (Initial Subscription)
```
POST /api/user/vehicles/link
  ↓
Vehicle created
Subscription created (PENDING)
Batteries created (AVAILABLE, vehicle=null)
Invoice created (SUBSCRIPTION_RENEWAL)
  ↓
Return invoiceId to frontend
  ↓
User pays via VNPay
  ↓
VNPay callback (responseCode=00)
  ↓
Invoice → PAID
  ↓
activateSubscription() triggered
  ↓
Subscription: PENDING → ACTIVE
Batteries: AVAILABLE → IN_USE, assigned to vehicle
  ↓
✅ User có thể swap pin
```

### Flow 2: Auto Renewal
```
Auto-renew job runs (daily)
  ↓
Find expired subscriptions
Check pending invoices → BLOCK if any
  ↓
Create renewal invoice (SUBSCRIPTION_RENEWAL)
BLOCK renewal - wait for payment
  ↓
User pays via VNPay
  ↓
VNPay callback (responseCode=00)
  ↓
Invoice → PAID
  ↓
completeRenewal() triggered
  ↓
Old subscription → COMPLETED
New subscription → ACTIVE (with new plan if changed)
Usage counters reset to 0
  ↓
✅ Subscription renewed
```

### Flow 3: Swap Overage (Existing)
```
User swaps battery
  ↓
Calculate usage
If overage > 0:
  Create invoice (SWAP_OVERAGE)
  ↓
User pays via VNPay
  ↓
Invoice → PAID
  ↓
✅ Payment complete
```

---

## 🚨 Important Notes

### 1. Invoice Type Detection
```java
// VNPayServiceImpl.processVNPayCallback()

if ("SUBSCRIPTION_RENEWAL".equals(invoice.getInvoiceType())) {
    if (subscription.getStatus() == SubscriptionStatus.PENDING) {
        // Initial subscription - activate
        activateSubscription(subscriptionId);
    } else {
        // Renewal - create new subscription
        completeRenewal(subscriptionId);
    }
}
```

### 2. Battery Assignment Logic
- **Before payment:** Batteries created with `AVAILABLE` status, `vehicle = null`
- **After payment:** Find batteries by `findByStatusAndVehicleIsNull(AVAILABLE)`, assign to vehicle, set `IN_USE`

### 3. Error Handling
- VNPay callback success but activation/renewal failed → Manual intervention needed
- Check logs: `"Failed to activate subscription after payment"`
- Admin can manually call activation/renewal endpoints

---

## 📚 Files Modified

### Entities
- ✅ `Invoice.java` - Make fields nullable, add invoiceType

### Services
- ✅ `InvoiceService.java` - Add createSubscriptionRenewalInvoice()
- ✅ `InvoiceServiceImpl.java` - Implement renewal invoice creation
- ✅ `SubscriptionService.java` - Add activateSubscription(), completeRenewal()
- ✅ `SubscriptionServiceImpl.java` - Implement activation & renewal
- ✅ `LinkVehicleServiceImpl.java` - Create PENDING subscription & invoice
- ✅ `VNPayServiceImpl.java` - Trigger activation/renewal on payment

### Repositories
- ✅ `BatterySerialRepository.java` - Add findByStatusAndVehicleIsNull()

### DTOs
- ✅ `LinkVehicleResponse.java` - Add invoiceId, invoiceAmount

### Documentation
- ✅ `SUBSCRIPTION_RENEWAL_PAYMENT.md` - Complete guide
- ✅ `PAYMENT_INTEGRATION_SUMMARY.md` - This file
- ✅ `migration_update_invoices_for_renewal.sql` - DB migration

---

## ✅ Testing Checklist

### Initial Subscription
- [ ] Link vehicle creates PENDING subscription
- [ ] Invoice created with correct amount
- [ ] Batteries created as AVAILABLE
- [ ] Payment activates subscription
- [ ] Batteries assigned to vehicle (IN_USE)

### Renewal
- [ ] Auto-renew creates invoice
- [ ] Renewal blocked until payment
- [ ] Payment creates new subscription
- [ ] Old subscription marked COMPLETED
- [ ] nextPlanId respected

### Swap Overage (Regression)
- [ ] Overage invoice still created
- [ ] Payment works as before

---

## 🎯 Result

| Scenario | Before ❌ | After ✅ |
|----------|-----------|----------|
| Link Vehicle | Free subscription | Pay first, then activate |
| Auto Renewal | Free renewal | Pay to renew |
| Swap Overage | ✅ Already has payment | ✅ No change |

**100% payment coverage achieved! 🎉**
