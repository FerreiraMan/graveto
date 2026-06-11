DELETE FROM categories c WHERE c.name = 'GROCERIES' AND c.user_sid IS NULL;
DELETE FROM categories c where c.name = 'LIFE_INSURANCE' AND c.user_sid IS NULL;
DELETE FROM categories c where c.name = 'HOUSE_INSURANCE' AND c.user_sid IS NULL;
DELETE FROM categories c where c.name = 'INSURANCE' AND c.user_sid IS NULL;

UPDATE categories SET name = 'ENTERTAINMENT_&_LEISURE', display_name = 'Entertainment & Leisure', updated_at = now()
WHERE name = 'LEISURE' AND user_sid IS NULL;

-- LEVEL 0 — new top-level categories
INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'FOOD_&_DRINK',    'Food & Drink',        null, null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'HOUSEHOLD',         'Household',           null, null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'SHOPPING',          'Shopping',            null, null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'EDUCATION',         'Education',           null, null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'FINANCIAL',         'Financial',           null, null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'CHILDREN_&_FAMILY',   'Children & Family',   null, null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'OTHER',             'Other',               null, null, 'EXPENSE', now(), now());

-- INCOME — new subcategories
INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'FREELANCE_OR_SIDE_INCOME',     'Freelance or Side Income',  (SELECT id FROM categories WHERE name = 'INCOME'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'RENTAL_INCOME',        'Rental Income',            (SELECT id FROM categories WHERE name = 'INCOME'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'INVESTMENT_RETURNS',   'Investment Returns',       (SELECT id FROM categories WHERE name = 'INCOME'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'GOVERNMENT_BENEFITS',  'Government Benefits',      (SELECT id FROM categories WHERE name = 'INCOME'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'GOVERNMENT_RETURNS',  'Government Returns',      (SELECT id FROM categories WHERE name = 'INCOME'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'OTHER_INCOME',         'Other Income',             (SELECT id FROM categories WHERE name = 'INCOME'), null, 'INCOME', now(), now());


-- HOUSING — fix and expand
UPDATE categories SET name = 'CONDOMINIUM_FEES', display_name = 'Condominium Fees', updated_at = now()
WHERE name = 'CONDOMINIUM' AND user_sid IS NULL;

INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'RENT',               'Rent',               (SELECT id FROM categories WHERE name = 'HOUSING'),    null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'HOME_MAINTENANCE',   'Home Maintenance',   (SELECT id FROM categories WHERE name = 'HOUSING'),    null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'HOME_INSURANCE',     'Home Insurance',     (SELECT id FROM categories WHERE name = 'HOUSING'),    null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'LIFE_INSURANCE',     'Life Insurance',     (SELECT id FROM categories WHERE name = 'HOUSING'),    null, 'EXPENSE', now(), now());


-- TRANSPORTATION — fix and expand
INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'FUEL',                 'Fuel',               (SELECT id FROM categories WHERE name = 'TRANSPORTATION'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'PUBLIC_TRANSPORT',     'Public Transport',   (SELECT id FROM categories WHERE name = 'TRANSPORTATION'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'RIDE_SHARING',         'Ride Sharing',       (SELECT id FROM categories WHERE name = 'TRANSPORTATION'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'PARKING',              'Parking',            (SELECT id FROM categories WHERE name = 'TRANSPORTATION'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'VEHICLE_MAINTENANCE',  'Vehicle Maintenance',(SELECT id FROM categories WHERE name = 'TRANSPORTATION'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'VEHICLE_INSURANCE',    'Vehicle Insurance',  (SELECT id FROM categories WHERE name = 'TRANSPORTATION'), null, 'EXPENSE', now(), now());

UPDATE categories SET parent_id = (SELECT id FROM categories WHERE name = 'FUEL'), updated_at = now()
WHERE name IN ('DIESEL', 'GASOLINE') AND user_sid IS NULL;


-- FOOD & DRINK
DELETE FROM categories
WHERE name IN ('RESTAURANTS', 'BARS')
  AND user_sid IS NULL
  AND parent_id = (SELECT id FROM categories WHERE name = 'ENTERTAINMENT_&_LEISURE');

INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'RESTAURANTS',     'Restaurants',        (SELECT id FROM categories WHERE name = 'FOOD_&_DRINK'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'CAFES_&_COFFEE',    'Cafés & Coffee',     (SELECT id FROM categories WHERE name = 'FOOD_&_DRINK'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'BARS',            'Bars',               (SELECT id FROM categories WHERE name = 'FOOD_&_DRINK'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'TAKEAWAY_&_DELIVERY',        'Takeaway & Delivery',(SELECT id FROM categories WHERE name = 'FOOD_&_DRINK'), null, 'EXPENSE', now(), now());


-- HOUSEHOLD
INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'GROCERIES_&_SUPERMARKET',         'Groceries & Supermarket', (SELECT id FROM categories WHERE name = 'HOUSEHOLD'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'CLEANING_SUPPLIES', 'Cleaning Supplies',       (SELECT id FROM categories WHERE name = 'HOUSEHOLD'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'LAUNDRY',           'Laundry',                 (SELECT id FROM categories WHERE name = 'HOUSEHOLD'), null, 'EXPENSE', now(), now());


-- HEALTH — expand
UPDATE categories SET name = 'MEDICAL_APPOINTMENTS', display_name = 'Medical Appointments', updated_at = now()
WHERE name = 'APPOINTMENTS' AND user_sid IS NULL;

INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'DENTAL',          'Dental',        (SELECT id FROM categories WHERE name = 'HEALTH'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'VISION',          'Vision',        (SELECT id FROM categories WHERE name = 'HEALTH'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'MENTAL_HEALTH',   'Mental Health', (SELECT id FROM categories WHERE name = 'HEALTH'), null, 'EXPENSE', now(), now());

UPDATE categories SET name = 'GYM_&_SPORTS', display_name = 'Gym and Sports' ,parent_id = (SELECT id FROM categories WHERE name = 'HEALTH'), updated_at = now()
WHERE name = 'GYM' AND user_sid IS NULL;


-- PERSONAL CARE — fix and expand
INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'COSMETICS',   'Cosmetics & Toiletries', (SELECT id FROM categories WHERE name = 'PERSONAL_CARE'), null, 'EXPENSE', now(), now());

-- ENTERTAINMENT & LEISURE — expand
DELETE FROM categories WHERE name = 'EXPERIENCES' AND user_sid IS NULL;

INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'STREAMING',        'Streaming Services',   (SELECT id FROM categories WHERE name = 'ENTERTAINMENT_&_LEISURE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'GAMES_&_GAMING',     'Games & Gaming',       (SELECT id FROM categories WHERE name = 'ENTERTAINMENT_&_LEISURE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'BOOKS_&_MAGAZINES',  'Books & Magazines',    (SELECT id FROM categories WHERE name = 'ENTERTAINMENT_&_LEISURE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'CINEMA_&_EVENTS',    'Cinema & Events',      (SELECT id FROM categories WHERE name = 'ENTERTAINMENT_&_LEISURE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'SPORTS_&_OUTDOOR',   'Sports & Outdoor',     (SELECT id FROM categories WHERE name = 'ENTERTAINMENT_&_LEISURE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'HOBBIES',          'Hobbies',              (SELECT id FROM categories WHERE name = 'ENTERTAINMENT_&_LEISURE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'TRAVEL_&_HOLIDAYS',  'Travel & Holidays',    (SELECT id FROM categories WHERE name = 'ENTERTAINMENT_&_LEISURE'), null, 'EXPENSE', now(), now());


-- SHOPPING — subcategories
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE name = 'SHOPPING'), updated_at = now()
WHERE name IN ('CLOTHING', 'GADGETS', 'GIFTS') AND user_sid IS NULL;

UPDATE categories SET name = 'CLOTHING_&_ACCESSORIES', display_name = 'Clothing & Accessories', updated_at = now()
WHERE name = 'CLOTHING' AND user_sid IS NULL;

UPDATE categories SET name = 'ELECTRONICS_&_GADGETS', display_name = 'Electronics & Gadgets', updated_at = now()
WHERE name = 'GADGETS' AND user_sid IS NULL;

INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'HOME_&_FURNITURE', 'Home & Furniture', (SELECT id FROM categories WHERE name = 'SHOPPING'), null, 'EXPENSE', now(), now());


-- EDUCATION
INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'COURSES_SUBSCRIPTIONS', 'Courses & Subscriptions', (SELECT id FROM categories WHERE name = 'EDUCATION'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'BOOKS_SUPPLIES',        'Books & Supplies',        (SELECT id FROM categories WHERE name = 'EDUCATION'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'TUITION',               'Tuition',                 (SELECT id FROM categories WHERE name = 'EDUCATION'), null, 'EXPENSE', now(), now());


-- INVESTMENTS — subcategories
INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'DIVIDENDS_AND_INTEREST',      'Dividends and Interest',        (SELECT id FROM categories WHERE name = 'INVESTMENTS'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'PLATFORM_FEES',      'Platform Fees',        (SELECT id FROM categories WHERE name = 'INVESTMENTS'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'ASSET_PURCHASE',      'Asset Purchase',        (SELECT id FROM categories WHERE name = 'INVESTMENTS'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'ASSET_SALE',           'Asset Sale',               (SELECT id FROM categories WHERE name = 'INVESTMENTS'), null, 'INCOME', now(), now());

INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'PURCHASE_STOCKS',       'Stocks',            (SELECT id FROM categories WHERE name = 'ASSET_PURCHASE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'PURCHASE_ETFS',         'ETFs',              (SELECT id FROM categories WHERE name = 'ASSET_PURCHASE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'PURCHASE_CRYPTO',       'Crypto',            (SELECT id FROM categories WHERE name = 'ASSET_PURCHASE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'PURCHASE_BONDS',        'Bonds',             (SELECT id FROM categories WHERE name = 'ASSET_PURCHASE'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'PURCHASE_REAL_ESTATE',  'Real Estate Fund',  (SELECT id FROM categories WHERE name = 'ASSET_PURCHASE'), null, 'EXPENSE', now(), now());

INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'SALE_STOCKS',       'Stocks',            (SELECT id FROM categories WHERE name = 'ASSET_SALE'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'SALE_ETFS',         'ETFs',              (SELECT id FROM categories WHERE name = 'ASSET_SALE'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'SALE_CRYPTO',       'Crypto',            (SELECT id FROM categories WHERE name = 'ASSET_SALE'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'SALE_BONDS',        'Bonds',             (SELECT id FROM categories WHERE name = 'ASSET_SALE'), null, 'INCOME', now(), now()),
    (gen_random_uuid(), 'SALE_REAL_ESTATE',  'Real Estate Fund',  (SELECT id FROM categories WHERE name = 'ASSET_SALE'), null, 'INCOME', now(), now());


-- FINANCIAL
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE name = 'FINANCIAL'), updated_at = now()
WHERE name = 'TAXES' AND user_sid IS NULL;

INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'BANK_FEES',         'Bank Fees',          (SELECT id FROM categories WHERE name = 'FINANCIAL'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'LOAN_REPAYMENT',    'Loan Repayment',     (SELECT id FROM categories WHERE name = 'FINANCIAL'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'CREDIT_CARD',       'Credit Card',        (SELECT id FROM categories WHERE name = 'FINANCIAL'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'GENERAL_INSURANCE', 'Insurance',          (SELECT id FROM categories WHERE name = 'FINANCIAL'), null, 'EXPENSE', now(), now());


-- PETS — expand
INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'PET_CARE_&_HYGIENE',   'Pet Care & Hygiene',  (SELECT id FROM categories WHERE name = 'PETS'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'PET_INSURANCE',  'Pet Insurance', (SELECT id FROM categories WHERE name = 'PETS'), null, 'EXPENSE', now(), now());


-- CHILDREN & FAMILY
INSERT INTO categories (sid, name, display_name, parent_id, user_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'CHILDCARE_SCHOOL', 'Childcare & School',  (SELECT id FROM categories WHERE name = 'CHILDREN_&_FAMILY'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'TOYS_ACTIVITIES',  'Toys & Activities',   (SELECT id FROM categories WHERE name = 'CHILDREN_&_FAMILY'), null, 'EXPENSE', now(), now()),
    (gen_random_uuid(), 'BABY_SUPPLIES',    'Baby Supplies',       (SELECT id FROM categories WHERE name = 'CHILDREN_&_FAMILY'), null, 'EXPENSE', now(), now());
