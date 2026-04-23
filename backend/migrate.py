"""Migration: add missing columns to addresses and orders tables."""
from app.database import SessionLocal
from sqlalchemy import text

db = SessionLocal()

alter_stmts = [
    # v1: address extra fields
    "ALTER TABLE addresses ADD COLUMN recipient_name VARCHAR(255)",
    "ALTER TABLE addresses ADD COLUMN recipient_phone VARCHAR(20)",
    "ALTER TABLE addresses ADD COLUMN house_number VARCHAR(100)",
    "ALTER TABLE addresses ADD COLUMN street VARCHAR(255)",
    "ALTER TABLE addresses ADD COLUMN landmark VARCHAR(255)",
    "ALTER TABLE addresses ADD COLUMN state VARCHAR(100)",
    # v1: order idempotency
    "ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(64)",
    # v2: order status timestamps (JSON dict: {status: iso_timestamp})
    "ALTER TABLE orders ADD COLUMN status_timestamps TEXT",
    # v3: unlimited stock for products
    "ALTER TABLE products ADD COLUMN unlimited_stock BOOLEAN DEFAULT 0",
]

for stmt in alter_stmts:
    try:
        db.execute(text(stmt))
        db.commit()
        col = stmt.split("ADD COLUMN ")[1].split(" ")[0]
        tbl = stmt.split("TABLE ")[1].split(" ")[0]
        print(f"  Added {tbl}.{col}")
    except Exception as e:
        db.rollback()
        if "duplicate column" in str(e).lower():
            col = stmt.split("ADD COLUMN ")[1].split(" ")[0]
            print(f"  (already exists) {col}")
        else:
            print(f"  ERROR: {e}")

# Verify
result = db.execute(text("PRAGMA table_info(addresses)"))
cols = [r[1] for r in result.fetchall()]
print(f"\nAddress columns: {cols}")

result2 = db.execute(text("PRAGMA table_info(orders)"))
cols2 = [r[1] for r in result2.fetchall()]
print(f"Order columns: {cols2}")

result3 = db.execute(text("PRAGMA table_info(products)"))
cols3 = [r[1] for r in result3.fetchall()]
print(f"Product columns: {cols3}")

db.close()
print("\nMigration complete!")
