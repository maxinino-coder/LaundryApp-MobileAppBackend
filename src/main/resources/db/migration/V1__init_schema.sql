-- ============================================================
--  Laundry App - Full Database Schema
--  Database: PostgreSQL 14+
--  Migration tool: Flyway (filename: V1__init_schema.sql)
-- ============================================================

-- -----------------------------------------------
--  EXTENSIONS
-- -----------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
-- PostGIS deliberately not enabled: no column in this schema uses a geometry or
-- geography type (rider/business coordinates are plain NUMERIC lat/lng), and on
-- managed Postgres such as Supabase the role running migrations often cannot
-- CREATE EXTENSION postgis into the public schema. Add it back in a later
-- migration if real geospatial queries are introduced.

-- -----------------------------------------------
--  ENUMS
-- -----------------------------------------------
CREATE TYPE account_role      AS ENUM ('USER', 'BUSINESS', 'RIDER');
CREATE TYPE order_status      AS ENUM (
    'PENDING', 'CONFIRMED', 'PICKED_UP',
    'IN_PROGRESS', 'READY', 'OUT_FOR_DELIVERY',
    'DELIVERED', 'CANCELLED', 'REFUNDED'
);
CREATE TYPE payment_status    AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED');
CREATE TYPE payment_method    AS ENUM ('CARD', 'MOBILE_MONEY', 'CASH', 'WALLET');
CREATE TYPE settlement_status AS ENUM ('PENDING', 'SETTLED', 'FAILED');
CREATE TYPE vehicle_type      AS ENUM ('BICYCLE', 'MOTORCYCLE', 'CAR', 'VAN');
CREATE TYPE service_category  AS ENUM (
    'WASH_AND_FOLD', 'DRY_CLEAN', 'IRON_ONLY',
    'WASH_AND_IRON', 'DUVET', 'SHOES', 'OTHER'
);
CREATE TYPE notification_type AS ENUM (
    'ORDER_UPDATE', 'PAYMENT', 'PROMOTION',
    'SYSTEM', 'REVIEW_REQUEST'
);
CREATE TYPE rider_type AS ENUM ('EMPLOYED', 'CONTRACT');

CREATE TYPE pricing_model AS ENUM ('PER_ITEM', 'PER_KG');

-- -----------------------------------------------
--  ACCOUNTS  (central auth table for all actors)
-- -----------------------------------------------
CREATE TABLE accounts (
                          id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                          email            VARCHAR(255) NOT NULL UNIQUE,
                          phone            VARCHAR(20)   UNIQUE,
                          google_id         VARCHAR(255) UNIQUE,
                          password_hash    TEXT         NOT NULL,
                          role             account_role NOT NULL,
                          is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
                          is_email_verified BOOLEAN     NOT NULL DEFAULT FALSE,
                          is_phone_verified BOOLEAN     NOT NULL DEFAULT FALSE,
                          last_login_at    TIMESTAMPTZ,
                          created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                          updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  REFRESH TOKENS  (JWT refresh token store)
-- -----------------------------------------------
CREATE TABLE refresh_tokens (
                                id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                account_id   UUID        NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
                                refresh_token_hash   TEXT        NOT NULL UNIQUE,
                                access_token_hash TEXT    NOT NULL UNIQUE ,
                                expires_at   TIMESTAMPTZ NOT NULL,
                                revoked      BOOLEAN     NOT NULL DEFAULT FALSE,
                                created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  USERS  (customer profile)
-- -----------------------------------------------
CREATE TABLE users (
                       id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       account_id   UUID         NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
                       first_name   VARCHAR(100) NOT NULL,
                       last_name    VARCHAR(100) NOT NULL,
                       avatar_url   TEXT,
    -- default delivery address
                       address      TEXT,
                       city         VARCHAR(100),
                       latitude     DOUBLE PRECISION,
                       longitude    DOUBLE PRECISION,
                       created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  BUSINESSES  (laundry business / admin profile)
-- -----------------------------------------------
CREATE TABLE businesses (
                            id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                            account_id      UUID         NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
                            business_name   VARCHAR(255) NOT NULL,
                            description     TEXT,
                            logo_url        TEXT,
                            banner_url      TEXT,
                            address         TEXT         NOT NULL,
                            city            VARCHAR(100),
                            latitude        DOUBLE PRECISION,
                            longitude       DOUBLE PRECISION,
                            is_approved     BOOLEAN      NOT NULL DEFAULT FALSE,
                            is_open         BOOLEAN      NOT NULL DEFAULT FALSE,
                            opening_time    TIME,
                            closing_time    TIME,
    -- bank / payout details
                            bank_name       VARCHAR(100),
                            bank_account_no VARCHAR(20),
                            bank_account_name VARCHAR(150),
                            momo_number     VARCHAR(20),
                            created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                            updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  RIDERS  (delivery rider profile)
-- -----------------------------------------------
CREATE TABLE riders (
                        id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                        account_id      UUID         NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
                        business_id     UUID         REFERENCES businesses(id) ON DELETE  SET NULL,
                        first_name      VARCHAR(100) NOT NULL,
                        last_name       VARCHAR(100) NOT NULL,
                        avatar_url      TEXT,
                        vehicle_type    vehicle_type NOT NULL,
                        k  rider_type NOT NULL DEFAULT 'EMPLOYED',
                        vehicle_plate   VARCHAR(20),
                        is_available    BOOLEAN      NOT NULL DEFAULT FALSE,
                        is_approved     BOOLEAN      NOT NULL DEFAULT FALSE,
                        current_lat     DOUBLE PRECISION,
                        current_lng     DOUBLE PRECISION,
                        last_location_at TIMESTAMPTZ,
    -- payout details
                        momo_number     VARCHAR(20),
                        bank_account_no VARCHAR(20),
                        created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                        updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  SERVICE ITEMS  (business catalogue)
-- -----------------------------------------------
CREATE TABLE service_items (
                               id              UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
                               business_id     UUID             NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
                               name            VARCHAR(255)     NOT NULL,
                               description     TEXT,
                               category        service_category NOT NULL,
                            pricing_model pricing_model NOT NULL DEFAULT 'PER_ITEM',
                               unit_price  NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
                               unit            VARCHAR(50)      NOT NULL DEFAULT 'piece',  -- e.g. piece, kg, pair
                               image_url       TEXT,
                               is_active       BOOLEAN          NOT NULL DEFAULT TRUE,
                               created_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
                               updated_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
                               UNIQUE (business_id, name)
);

-- -----------------------------------------------
--  ORDERS
-- -----------------------------------------------
CREATE TABLE orders (
                        id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                        order_number     VARCHAR(20)  NOT NULL UNIQUE,  -- human-readable e.g. ORD-20240601-0001
                        user_id          UUID         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                        business_id      UUID         NOT NULL REFERENCES businesses(id) ON DELETE RESTRICT,
                        rider_id         UUID         REFERENCES riders(id) ON DELETE SET NULL,
                        status           order_status NOT NULL DEFAULT 'PENDING',
    -- pickup
                        pickup_address   TEXT         NOT NULL,
                        pickup_lat       DOUBLE PRECISION,
                        pickup_lng       DOUBLE PRECISION,
                        pickup_time      TIMESTAMPTZ,
    -- delivery
                        delivery_address TEXT         NOT NULL,
                        delivery_lat     DOUBLE PRECISION,
                        delivery_lng     DOUBLE PRECISION,
                        delivery_time    TIMESTAMPTZ,
    -- financials
                        subtotal         NUMERIC(12,2) NOT NULL DEFAULT 0,
                        delivery_fee     NUMERIC(12,2) NOT NULL DEFAULT 0,
                        discount_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
                        total_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    -- meta
                        notes            TEXT,
                        cancelled_reason TEXT,
                        created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                        updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  ORDER ITEMS  (line items on an order)
-- -----------------------------------------------
CREATE TABLE order_items (
                             id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                             order_id        UUID          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             service_item_id UUID          NOT NULL REFERENCES service_items(id) ON DELETE RESTRICT,
                             quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
                             weight_kg NUMERIC(8, 3) DEFAULT NULL,
                             unit_price      NUMERIC(10,2) NOT NULL,  -- snapshot at time of order
                             line_total      NUMERIC(12,2) NOT NULL,  -- quantity * unit_price
                             notes           TEXT,
                             created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  PAYMENTS  (customer-facing transactions)
-- -----------------------------------------------
CREATE TABLE payments (
                          id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                          order_id            UUID           NOT NULL UNIQUE REFERENCES orders(id) ON DELETE RESTRICT,
                          payer_account_id    UUID           NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
                          amount              NUMERIC(12,2)  NOT NULL,
                          currency            VARCHAR(3)     NOT NULL DEFAULT 'GHS',
                          payment_method      payment_method NOT NULL,
                          status              payment_status NOT NULL DEFAULT 'PENDING',
                          provider            VARCHAR(50),   -- e.g. Paystack, Flutterwave, MTN MoMo
                          transaction_ref     VARCHAR(255)   UNIQUE,
                          provider_response   JSONB,         -- raw webhook payload
                          paid_at             TIMESTAMPTZ,
                          created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                          updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  RIDER EARNINGS  (per-delivery pay tracking)
-- -----------------------------------------------
CREATE TABLE rider_earnings (
                                id           UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
                                rider_id     UUID             NOT NULL REFERENCES riders(id) ON DELETE RESTRICT,
                                order_id     UUID             NOT NULL UNIQUE REFERENCES orders(id) ON DELETE RESTRICT,
                                amount       NUMERIC(10,2)    NOT NULL,
                                status       settlement_status NOT NULL DEFAULT 'PENDING',
                                settled_at   TIMESTAMPTZ,
                                created_at   TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);
-- -----------------------------------------------
--  RIDERS_CONTRACT_TABLE
-- -----------------------------------------------
CREATE TABLE rider_business_assignments (
                                            id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            rider_id     UUID NOT NULL REFERENCES riders(id) ON DELETE CASCADE,
                                            business_id  UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
                                            order_id     UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                                            assigned_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                            completed_at TIMESTAMPTZ,

                                            UNIQUE (rider_id, order_id),

                                            CHECK (
                                                completed_at IS NULL
                                                    OR completed_at >= assigned_at
                                                )
);


-- -----------------------------------------------
--  BUSINESS PAYOUTS  (per-order business revenue)
-- -----------------------------------------------
CREATE TABLE business_payouts (
                                  id                  UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
                                  business_id         UUID             NOT NULL REFERENCES businesses(id) ON DELETE RESTRICT,
                                  order_id            UUID             NOT NULL UNIQUE REFERENCES orders(id) ON DELETE RESTRICT,
                                  order_revenue       NUMERIC(12,2)    NOT NULL,
                                  platform_commission NUMERIC(12,2)    NOT NULL DEFAULT 0,
                                  rider_fee           NUMERIC(12,2)    NOT NULL DEFAULT 0,
                                  net_payout          NUMERIC(12,2)    NOT NULL,
                                  status              settlement_status NOT NULL DEFAULT 'PENDING',
                                  settled_at          TIMESTAMPTZ,
                                  created_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  REVIEWS  (customer reviews per order)
-- -----------------------------------------------
CREATE TABLE reviews (
                         id                   UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
                         order_id             UUID  NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
                         reviewer_account_id  UUID  NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
                         business_id          UUID  NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
                         rider_id             UUID  REFERENCES riders(id) ON DELETE SET NULL,
                         business_rating      SMALLINT NOT NULL CHECK (business_rating BETWEEN 1 AND 5),
                         rider_rating         SMALLINT CHECK (rider_rating BETWEEN 1 AND 5),
                         comment              TEXT,
                         is_visible           BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  NOTIFICATIONS
-- -----------------------------------------------
CREATE TABLE notifications (
                               id          UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
                               account_id  UUID              NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
                               type        notification_type NOT NULL,
                               title       VARCHAR(255)      NOT NULL,
                               body        TEXT              NOT NULL,
                               data        JSONB,            -- extra payload (e.g. order_id for deep linking)
                               is_read     BOOLEAN           NOT NULL DEFAULT FALSE,
                               created_at  TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  ADDRESSES  (saved user addresses)
-- -----------------------------------------------
CREATE TABLE user_addresses (
                                id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id     UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                label       VARCHAR(100) NOT NULL,   -- e.g. Home, Office
                                address     TEXT    NOT NULL,
                                city        VARCHAR(100),
                                latitude    DOUBLE PRECISION,
                                longitude   DOUBLE PRECISION,
                                is_default  BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  PROMOTIONS / COUPONS  (optional but useful)
-- -----------------------------------------------
CREATE TABLE promotions (
                            id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                            business_id     UUID          REFERENCES businesses(id) ON DELETE CASCADE,  -- NULL = platform-wide
                            code            VARCHAR(50)   NOT NULL UNIQUE,
                            description     TEXT,
                            discount_type   VARCHAR(20)   NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
                            discount_value  NUMERIC(10,2) NOT NULL,
                            min_order_value NUMERIC(10,2) NOT NULL DEFAULT 0,
                            max_uses        INT,
                            used_count      INT           NOT NULL DEFAULT 0,
                            valid_from      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                            valid_until     TIMESTAMPTZ,
                            is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
                            created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------
--  INDEXES
-- -----------------------------------------------
-- accounts
CREATE INDEX idx_accounts_email    ON accounts(email);
CREATE INDEX idx_accounts_phone    ON accounts(phone);
CREATE INDEX idx_accounts_role     ON accounts(role);

-- orders (most-queried table)
CREATE INDEX idx_orders_user_id       ON orders(user_id);
CREATE INDEX idx_orders_business_id   ON orders(business_id);
CREATE INDEX idx_orders_rider_id      ON orders(rider_id);
CREATE INDEX idx_orders_status        ON orders(status);
CREATE INDEX idx_orders_created_at    ON orders(created_at DESC);

-- payments
CREATE INDEX idx_payments_order_id        ON payments(order_id);
CREATE INDEX idx_payments_transaction_ref ON payments(transaction_ref);
CREATE INDEX idx_payments_status          ON payments(status);

-- notifications
CREATE INDEX idx_notifications_account_id ON notifications(account_id);
CREATE INDEX idx_notifications_is_read    ON notifications(account_id, is_read);

-- rider / business settlement
CREATE INDEX idx_rider_earnings_rider_id    ON rider_earnings(rider_id);
CREATE INDEX idx_business_payouts_business  ON business_payouts(business_id);

-- service items
CREATE INDEX idx_service_items_business_id ON service_items(business_id);

-- reviews
CREATE INDEX idx_reviews_business_id ON reviews(business_id);

-- refresh tokens
CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens(account_id);

--riders_contract
CREATE INDEX idx_rba_rider_id    ON rider_business_assignments(rider_id);
CREATE INDEX idx_rba_business_id ON rider_business_assignments(business_id);

CREATE INDEX idx_riders_rider_type  ON riders(rider_type);
CREATE INDEX idx_riders_is_available ON riders(is_available) WHERE is_available = TRUE;
CREATE INDEX idx_service_items_pricing_model ON service_items(pricing_model);
 --googleId index
CREATE INDEX idx_accounts_google_id ON accounts(google_id);

-- -----------------------------------------------
--  AUTO-UPDATE updated_at TRIGGER
-- -----------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$;

DO $$ DECLARE t TEXT;
BEGIN
FOR t IN SELECT unnest(ARRAY[
                           'accounts','users','businesses','riders',
                       'service_items','orders','payments'
                           ]) LOOP
             EXECUTE format(
      'CREATE TRIGGER trg_%I_updated_at
       BEFORE UPDATE ON %I
       FOR EACH ROW EXECUTE FUNCTION set_updated_at()', t, t);
END LOOP;
END $$;

-- -----------------------------------------------
--  ORDER NUMBER SEQUENCE + GENERATOR
-- -----------------------------------------------
CREATE SEQUENCE order_seq START 1;

CREATE OR REPLACE FUNCTION generate_order_number()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.order_number := 'ORD-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' ||
                        LPAD(nextval('order_seq')::TEXT, 4, '0');
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_orders_order_number
    BEFORE INSERT ON orders
    FOR EACH ROW EXECUTE FUNCTION generate_order_number();

-- -----------------------------------------------
--  CONSTRAINS
-- -----------------------------------------------
-- Add a CHECK: employed riders must have a business_id; contract riders must not
ALTER TABLE riders
    ADD CONSTRAINT chk_rider_type_business
        CHECK (
            (rider_type = 'EMPLOYED' AND business_id IS NOT NULL) OR
            (rider_type = 'CONTRACT' AND business_id IS NULL)
            );

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_item_pricing
        CHECK (
            weight_kg IS NULL OR (weight_kg > 0 AND quantity = 1)
            );