# Công Thức Tính Tiền Swap - Progressive Tier Pricing

## 📊 Tổng Quan

Hệ thống tính phí swap theo **bậc thang lũy tiến** (progressive tier pricing):

- Càng dùng nhiều, đơn giá càng **GIẢM** (khuyến khích dùng nhiều)
- Mỗi phần overage được tính theo đúng tier rate tương ứng
- Không tính phí lại toàn bộ usage đã dùng trước đó

---

## 🎯 Nguyên Tắc Cơ Bản

### 1. Subscription Tracking

Subscription lưu **tổng usage tích lũy** trong tháng:

- `distanceUsedThisMonth`: Tổng km đã đi (DISTANCE plan)
- `energyUsedThisMonth`: Tổng kWh đã dùng (ENERGY plan)

### 2. Billing Logic

Mỗi lần swap chỉ tính phí cho **phần mới sử dụng**, không tính lại phần đã tính trước đó:

```java
double usedBefore = subscription.getDistanceUsedThisMonth();  // 150km
double usageThisSwap = distanceTraveled;                      // 100km
double totalAfter = usedBefore + usageThisSwap;               // 250km
double base = subscription.getPlan().getBaseMileage();        // 100km

if (totalAfter > base) {
    double chargeableStart = Math.max(usedBefore, base);  // max(150, 100) = 150km
    double chargeableEnd = totalAfter;                     // 250km

    cost = calculateTieredCost(DISTANCE, 150km, 250km);
    // Chỉ tính phí cho 100km mới (từ 150→250km)
}
```

---

## 📐 Progressive Tier Pricing

### Tier Structure Example (DISTANCE)

| Tier   | Range (km) | Rate (₫/km) | Note                        |
| ------ | ---------- | ----------- | --------------------------- |
| Base   | 0 - 100    | 0₫          | Miễn phí (included in plan) |
| Tier 1 | 100 - 200  | 5,000₫      | Cao nhất                    |
| Tier 2 | 200 - 300  | 4,000₫      | Giảm 20%                    |
| Tier 3 | 300+       | 3,000₫      | Giảm 40%                    |

### Tier Structure Example (ENERGY)

| Tier   | Range (kWh) | Rate (₫/kWh) | Note     |
| ------ | ----------- | ------------ | -------- |
| Base   | 0 - 50      | 0₫           | Miễn phí |
| Tier 1 | 50 - 100    | 3,500₫       | Cao nhất |
| Tier 2 | 100 - 150   | 3,000₫       | Giảm 14% |
| Tier 3 | 150+        | 2,500₫       | Giảm 29% |

---

## 💰 Calculation Algorithm

### Method: `calculateTieredCost(planType, rangeStart, rangeEnd)`

**Input:**

- `planType`: DISTANCE hoặc ENERGY
- `rangeStart`: Vị trí bắt đầu tính phí (km hoặc kWh)
- `rangeEnd`: Vị trí kết thúc (km hoặc kWh)

**Output:**

- `totalCost`: Tổng chi phí theo bậc thang (₫)

**Algorithm:**

```java
1. Load all tiers for planType (sorted by minValue ASC)
2. For each tier:
   a. Skip if rangeStart >= tier.maxValue (đã qua tier này)
   b. Skip if rangeEnd <= tier.minValue (chưa đến tier này)
   c. Calculate overlap: [max(rangeStart, tier.min), min(rangeEnd, tier.max)]
   d. tierCost = overlap × tier.rate
   e. totalCost += tierCost
3. Return totalCost
```

---

## 📝 Detailed Examples

### Example 1: First Swap (Chưa Vượt Base)

**Setup:**

- Plan: DISTANCE, base = 100km
- usedBefore = 0km
- thisSwap = 50km
- totalAfter = 50km

**Calculation:**

```
chargeableStart = max(0, 100) = 100km
chargeableEnd = 50km

50km < 100km (base) → cost = 0₫
```

**Result:** ✅ Miễn phí (trong base)

---

### Example 2: Vượt Base Lần Đầu

**Setup:**

- Plan: DISTANCE, base = 100km
- usedBefore = 80km
- thisSwap = 40km
- totalAfter = 120km

**Calculation:**

```
chargeableStart = max(80, 100) = 100km
chargeableEnd = 120km

Tier 1 [100-200km, 5000₫/km]:
  overlap = [100, 120] = 20km
  cost = 20km × 5,000₫ = 100,000₫

Total: 100,000₫
```

**Result:** 100,000₫ cho 20km vượt

---

### Example 3: Cross Multiple Tiers

**Setup:**

- Plan: DISTANCE, base = 100km
- usedBefore = 150km (đã vượt base)
- thisSwap = 200km
- totalAfter = 350km

**Tiers:**

- Tier 1: 100-200km = 5,000₫/km
- Tier 2: 200-300km = 4,000₫/km
- Tier 3: 300+km = 3,000₫/km

**Calculation:**

```
chargeableStart = max(150, 100) = 150km
chargeableEnd = 350km

Tier 1 [100-200km]:
  overlap = [150, 200] = 50km
  cost = 50km × 5,000₫ = 250,000₫

Tier 2 [200-300km]:
  overlap = [200, 300] = 100km
  cost = 100km × 4,000₫ = 400,000₫

Tier 3 [300+km]:
  overlap = [300, 350] = 50km
  cost = 50km × 3,000₫ = 150,000₫

Total: 250,000₫ + 400,000₫ + 150,000₫ = 800,000₫
```

**Breakdown:**

- 50km @ 5,000₫/km = 250,000₫
- 100km @ 4,000₫/km = 400,000₫
- 50km @ 3,000₫/km = 150,000₫
- **Total: 800,000₫ cho 200km**

**Average Rate:** 800,000₫ ÷ 200km = **4,000₫/km** (rẻ hơn tier 1!)

---

### Example 4: Full Month Usage

**Plan: DISTANCE, base = 100km**

| Swap | usedBefore | thisSwap | totalAfter | Chargeable Range                     | Cost                | Cumulative Cost |
| ---- | ---------- | -------- | ---------- | ------------------------------------ | ------------------- | --------------- |
| 1    | 0km        | 60km     | 60km       | -                                    | 0₫                  | 0₫              |
| 2    | 60km       | 30km     | 90km       | -                                    | 0₫                  | 0₫              |
| 3    | 90km       | 50km     | 140km      | 100-140 (tier 1)                     | 200,000₫            | 200,000₫        |
| 4    | 140km      | 80km     | 220km      | 140-200 (tier 1)<br>200-220 (tier 2) | 300,000₫<br>80,000₫ | 580,000₫        |
| 5    | 220km      | 100km    | 320km      | 220-300 (tier 2)<br>300-320 (tier 3) | 320,000₫<br>60,000₫ | 960,000₫        |

**Monthly Total:**

- 320km driven
- 220km charged (320 - 100 base)
- Total cost: 960,000₫
- Average rate: 960,000 ÷ 220 = **4,364₫/km**

---

## 🔍 Invoice Description

Invoice description hiển thị breakdown chi tiết:

```
Tier breakdown [150.00 → 350.00]:
  Tier[100-200]: 50.00 × 5000₫ = 250000₫;
  Tier[200-300]: 100.00 × 4000₫ = 400000₫;
  Tier[300-Infinity]: 50.00 × 3000₫ = 150000₫;
Total: 800000₫
```

---

## 🚨 Edge Cases

### Case 1: No Tiers Defined

```java
if (tiers.isEmpty()) {
    log.warn("No tier rates found for planType={}", planType);
    return 0.0;  // Miễn phí nếu không có tier
}
```

### Case 2: Gap Between Tiers

Nếu có gap (VD: tier 1 = 100-200, tier 2 = 250-300):

- Range 200-250km không có tier → **không tính phí** (miễn phí!)
- Hoặc có thể throw error tùy business logic

### Case 3: Overlap Tiers

Nếu tiers overlap, chỉ tier đầu tiên match được apply:

```java
// Sorted by minValue ASC đảm bảo tier thấp hơn được ưu tiên
List<PlanTierRate> tiers = repository.findByPlanTypeOrderByMinValueAsc(planType);
```

---

## 📊 Database Schema

### PlanTierRate Table

```sql
CREATE TABLE plan_tier_rate (
    id BIGSERIAL PRIMARY KEY,
    plan_type VARCHAR(50) NOT NULL,  -- 'DISTANCE' or 'ENERGY'
    min_value DOUBLE PRECISION NOT NULL,
    max_value DOUBLE PRECISION,  -- NULL = infinity
    rate DOUBLE PRECISION NOT NULL,
    note VARCHAR(255)
);
```

**Sample Data (DISTANCE):**

```sql
INSERT INTO plan_tier_rate (plan_type, min_value, max_value, rate, note) VALUES
('DISTANCE', 100, 200, 5000, 'Tier 1: Standard rate'),
('DISTANCE', 200, 300, 4000, 'Tier 2: 20% discount'),
('DISTANCE', 300, NULL, 3000, 'Tier 3: 40% discount (unlimited)');
```

**Sample Data (ENERGY):**

```sql
INSERT INTO plan_tier_rate (plan_type, min_value, max_value, rate, note) VALUES
('ENERGY', 50, 100, 3500, 'Tier 1: Standard rate'),
('ENERGY', 100, 150, 3000, 'Tier 2: 14% discount'),
('ENERGY', 150, NULL, 2500, 'Tier 3: 29% discount (unlimited)');
```

---

## 🧪 Testing

### Test Case 1: Single Tier

```java
// Setup: Tier 1 [100-200km] = 5000₫/km
cost = calculateTieredCost(DISTANCE, 120, 150);
// Expected: 30km × 5000 = 150,000₫
```

### Test Case 2: Cross Two Tiers

```java
// Setup: Tier 1 [100-200] = 5000₫, Tier 2 [200-300] = 4000₫
cost = calculateTieredCost(DISTANCE, 180, 250);
// Expected: (20km × 5000) + (50km × 4000) = 100,000 + 200,000 = 300,000₫
```

### Test Case 3: Start After Base

```java
// usedBefore = 150km, thisSwap = 50km, totalAfter = 200km
chargeableStart = max(150, 100) = 150;
chargeableEnd = 200;
cost = calculateTieredCost(DISTANCE, 150, 200);
// Expected: 50km × 5000 = 250,000₫
```

---

## ✅ Benefits of Progressive Tier Pricing

1. **Fair Pricing:** User chỉ trả cho phần sử dụng thực tế
2. **Incentivize Usage:** Càng dùng nhiều, average rate càng thấp
3. **Transparent:** Log chi tiết breakdown từng tier
4. **Flexible:** Admin dễ dàng thêm/sửa tier rates
5. **No Double Charging:** Không tính lại usage đã tính trước đó

---

## 📝 Log Output Example

```
DISTANCE BILLING | usedBefore=150.0km | thisSwap=200.0km | total=350.0km |
  base=100.0km | overage=200.0km | cost=800000.0₫

Tier breakdown [150.00 → 350.00]:
  Tier[100-200]: 50.00 × 5000₫ = 250000₫;
  Tier[200-300]: 100.00 × 4000₫ = 400000₫;
  Tier[300-Infinity]: 50.00 × 3000₫ = 150000₫;
Total: 800000₫

INVOICE CREATED | id=15 | type=SWAP_OVERAGE | subscription=5 |
  amount=800000₫ | overage=200.0 km
```

---

## 🔄 Subscription Update

Sau mỗi swap:

```java
subscription.setDistanceUsedThisMonth(totalAfter);
subscriptionRepository.save(subscription);
```

**Lưu ý:**

- Subscription lưu **tổng usage tích lũy**
- Cost chỉ tính cho **phần mới** (chargeableStart → chargeableEnd)
- Invoice lưu `overage` (số lượng vượt) và `rate` (tier rate cuối cùng)

---

## 🎓 Summary

**Công thức tính tiền swap:**

1. **Xác định range tính phí:**

   ```
   chargeableStart = max(usedBefore, base)
   chargeableEnd = totalAfter
   ```

2. **Tính cost theo bậc thang:**

   ```
   For each tier trong range:
     tierCost = (overlap với range) × tier.rate
   totalCost = sum(tierCost)
   ```

3. **Cập nhật subscription:**

   ```
   subscription.distanceUsedThisMonth = totalAfter
   ```

4. **Tạo invoice:**
   ```
   invoice.amount = totalCost
   invoice.overage = chargeableEnd - chargeableStart
   invoice.rate = tier rate cuối cùng (for display)
   ```

**Result:** Fair, transparent, và khuyến khích user dùng nhiều! 🎉
