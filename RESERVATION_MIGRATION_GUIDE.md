# Database Migration Guide - Reservation Feature

## Prerequisites
- PostgreSQL client (psql) installed
- Database connection details:
  - Host: `ep-spring-mode-a1wrdey0-pooler.ap-southeast-1.aws.neon.tech`
  - Database: Your database name
  - User: Your database user
  - Port: 5432

## Migration Steps

### Step 1: Connect to Database
```bash
psql -h ep-spring-mode-a1wrdey0-pooler.ap-southeast-1.aws.neon.tech \
     -U your_username \
     -d your_database \
     -p 5432
```

Or using connection string:
```bash
psql "postgresql://username:password@ep-spring-mode-a1wrdey0-pooler.ap-southeast-1.aws.neon.tech:5432/database_name?sslmode=require"
```

### Step 2: Run Migrations in Order

#### 1️⃣ Add RESERVED status to battery_serials
```bash
\i migration_add_reserved_status.sql
```

**Expected output:**
```
ALTER TABLE
ALTER TABLE
 constraint_name               | constraint_definition
-------------------------------+---------------------------------------
 battery_serials_status_check | CHECK (status IN ('AVAILABLE', ...))
```

#### 2️⃣ Create reservation tables
```bash
\i migration_create_reservations.sql
```

**Expected output:**
```
CREATE TABLE
CREATE TABLE
CREATE INDEX
CREATE INDEX
...
 table_name        | column_count
-------------------+-------------
 reservations      | 13
 reservation_items | 3
```

### Step 3: Verify Migration

#### Check battery_serials constraint
```sql
SELECT 
    con.conname AS constraint_name,
    pg_get_constraintdef(con.oid) AS constraint_definition
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
WHERE rel.relname = 'battery_serials' 
  AND con.conname = 'battery_serials_status_check';
```

#### Check reservations table
```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'reservations'
ORDER BY ordinal_position;
```

#### Check reservation_items table
```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'reservation_items'
ORDER BY ordinal_position;
```

#### Check indexes
```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('reservations', 'reservation_items');
```

## Rollback (If Needed)

### Drop reservation tables
```sql
DROP TABLE IF EXISTS reservation_items CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
```

### Revert battery_serials constraint
```sql
ALTER TABLE battery_serials 
DROP CONSTRAINT IF EXISTS battery_serials_status_check;

ALTER TABLE battery_serials 
ADD CONSTRAINT battery_serials_status_check 
CHECK (status IN (
    'AVAILABLE',
    'IN_USE',
    'MAINTENANCE',
    'RETIRED',
    'PENDING_IN',
    'PENDING_OUT'
));
```

## Alternative: Run via PowerShell

```powershell
# Set connection string
$env:PGPASSWORD = "your_password"

# Run migrations
psql -h ep-spring-mode-a1wrdey0-pooler.ap-southeast-1.aws.neon.tech `
     -U your_username `
     -d your_database `
     -f migration_add_reserved_status.sql

psql -h ep-spring-mode-a1wrdey0-pooler.ap-southeast-1.aws.neon.tech `
     -U your_username `
     -d your_database `
     -f migration_create_reservations.sql
```

## Troubleshooting

### Error: "relation already exists"
Tables already created. Safe to ignore or drop and recreate.

### Error: "constraint already exists"
Constraint already exists. Use `DROP CONSTRAINT IF EXISTS` first.

### Error: "permission denied"
Make sure your database user has CREATE/ALTER privileges.

## Post-Migration

After successful migration:
1. ✅ Restart Spring Boot application
2. ✅ Test reservation API endpoints
3. ✅ Verify scheduler is running (check logs every 1 minute)

## Notes

- All migrations use `IF NOT EXISTS` / `IF EXISTS` for idempotency
- Foreign keys have appropriate CASCADE/SET NULL rules
- Indexes created for optimal query performance
- Check constraints ensure data integrity
