-- Demo data for local development only. This migration lives in a profile-scoped folder
-- (classpath:db/migration/dev, only added to spring.flyway.locations under the "dev"
-- profile) so it can never reach a production database by accident.
--
-- Every account below uses the password "Password123" — hashed with BCrypt strength 12,
-- the same setting the application uses at runtime.

INSERT INTO users (id, name, email, password, role, phone, created_at, updated_at) VALUES
    (1, 'Pak Slamet',  'owner1@mamikos.test',    '$2a$12$0QyxbXV1AeTfMGhNkP76SO0Ff4407.8YZGb4Ij.AVGlL/xy8TRQTa', 'OWNER',   '081200000001', now(), now()),
    (2, 'Bu Sri',       'owner2@mamikos.test',    '$2a$12$0QyxbXV1AeTfMGhNkP76SO0Ff4407.8YZGb4Ij.AVGlL/xy8TRQTa', 'OWNER',   '081200000002', now(), now()),
    (3, 'Rina Regular', 'regular@mamikos.test',   '$2a$12$0QyxbXV1AeTfMGhNkP76SO0Ff4407.8YZGb4Ij.AVGlL/xy8TRQTa', 'REGULAR', '081200000003', now(), now()),
    (4, 'Dimas Premium','premium@mamikos.test',   '$2a$12$0QyxbXV1AeTfMGhNkP76SO0Ff4407.8YZGb4Ij.AVGlL/xy8TRQTa', 'PREMIUM', '081200000004', now(), now()),
    (5, 'Lowcredit Leo', 'lowcredit@mamikos.test','$2a$12$0QyxbXV1AeTfMGhNkP76SO0Ff4407.8YZGb4Ij.AVGlL/xy8TRQTa', 'REGULAR', '081200000005', now(), now());
SELECT setval('users_id_seq', (SELECT max(id) FROM users));

INSERT INTO credit_balances (user_id, balance, last_recharged_at, created_at, updated_at) VALUES
    (3, 20, NULL, now(), now()),
    (4, 40, NULL, now(), now()),
    (5, 3,  NULL, now(), now());
SELECT setval('credit_balances_id_seq', (SELECT max(id) FROM credit_balances));

INSERT INTO credit_transactions (user_id, type, amount, balance_before, balance_after, description, created_at) VALUES
    (3, 'INITIAL_GRANT', 20, 0, 20, 'Initial credit grant for regular account', now()),
    (4, 'INITIAL_GRANT', 40, 0, 40, 'Initial credit grant for premium account', now()),
    (5, 'INITIAL_GRANT', 20, 0, 20, 'Initial credit grant for regular account', now()),
    (5, 'INQUIRY_DEDUCTION', -17, 20, 3, 'Room availability inquiry (seed data)', now());
SELECT setval('credit_transactions_id_seq', (SELECT max(id) FROM credit_transactions));

-- Thirty kosts spread across five cities with a wide price range, so the search filters
-- (name, location, price) and the price sort all have something real to demonstrate against.
INSERT INTO kosts (
    id, owner_id, name, description, address_street, address_district, address_city, address_province,
    price_per_month, room_type, total_rooms, available_rooms, is_active, created_at, updated_at
) VALUES
    (1,  1, 'Kost Melati Residence',  'Kost putri eksklusif dekat kampus UGM.',        'Jl. Kaliurang KM 5',      'Depok',       'Sleman',    'DI Yogyakarta', 950000,  'PUTRI',  12, 4, true, now(), now()),
    (2,  1, 'Kost Anggrek Putra',     'Kost putra dekat stasiun.',                     'Jl. Malioboro No. 10',    'Gedong Tengen','Yogyakarta','DI Yogyakarta', 750000,  'PUTRA',  10, 6, true, now(), now()),
    (3,  1, 'Kost Mawar Campur',      'Kost campur dekat kampus UNY.',                 'Jl. Colombo No. 3',       'Depok',       'Sleman',    'DI Yogyakarta', 1100000, 'CAMPUR', 8,  2, true, now(), now()),
    (4,  2, 'Kost Dahlia Residence',  'Kost putri fasilitas lengkap.',                 'Jl. Sudirman No. 45',     'Menteng',     'Jakarta Pusat','DKI Jakarta',3200000, 'PUTRI',  15, 5, true, now(), now()),
    (5,  2, 'Kost Kenanga Putra',     'Kost putra murah dekat kampus.',                'Jl. Margonda Raya No. 8', 'Beji',        'Depok',     'Jawa Barat',    900000,  'PUTRA',  20, 10,true, now(), now()),
    (6,  2, 'Kost Teratai Campur',    'Kost campur nyaman dan aman.',                  'Jl. Dago No. 22',         'Coblong',     'Bandung',   'Jawa Barat',    1500000, 'CAMPUR', 12, 3, true, now(), now()),
    (7,  1, 'Kost Flamboyan Putri',   'Kost putri dekat ITB.',                         'Jl. Ganesha No. 5',       'Coblong',     'Bandung',   'Jawa Barat',    1750000, 'PUTRI',  9,  1, true, now(), now()),
    (8,  2, 'Kost Cempaka Putra',     'Kost putra dekat Unair.',                       'Jl. Airlangga No. 7',     'Gubeng',      'Surabaya',  'Jawa Timur',    850000,  'PUTRA',  14, 8, true, now(), now()),
    (9,  1, 'Kost Bougenville Campur','Kost campur dengan dapur bersama.',             'Jl. Diponegoro No. 12',   'Gubeng',      'Surabaya',  'Jawa Timur',    980000,  'CAMPUR', 10, 4, true, now(), now()),
    (10, 2, 'Kost Seruni Putri',      'Kost putri harga terjangkau.',                  'Jl. Kaliurang KM 8',      'Ngaglik',     'Sleman',    'DI Yogyakarta', 650000,  'PUTRI',  16, 12,true, now(), now()),
    (11, 1, 'Kost Anyelir Putra',     'Kost putra dekat kampus UI.',                    'Jl. Margonda Raya No. 20','Beji',        'Depok',     'Jawa Barat',    1050000, 'PUTRA',  11, 3, true, now(), now()),
    (12, 2, 'Kost Wijaya Kusuma',     'Kost campur eksklusif.',                        'Jl. Sudirman No. 100',    'Setiabudi',   'Jakarta Selatan','DKI Jakarta',4500000,'CAMPUR', 6,  1, true, now(), now()),
    (13, 1, 'Kost Sakura Putri',      'Kost putri modern.',                            'Jl. Malioboro No. 55',    'Gedong Tengen','Yogyakarta','DI Yogyakarta', 1300000, 'PUTRI',  8,  2, true, now(), now()),
    (14, 2, 'Kost Melati Putra',      'Kost putra fasilitas AC.',                      'Jl. Dago No. 88',         'Coblong',     'Bandung',   'Jawa Barat',    1650000, 'PUTRA',  10, 5, true, now(), now()),
    (15, 1, 'Kost Nusa Indah',        'Kost campur dekat pusat kota.',                 'Jl. Diponegoro No. 33',   'Gubeng',      'Surabaya',  'Jawa Timur',    1200000, 'CAMPUR', 12, 6, true, now(), now()),
    (16, 2, 'Kost Cendana Putri',     'Kost putri nyaman dan tenang.',                 'Jl. Kaliurang KM 12',     'Ngaglik',     'Sleman',    'DI Yogyakarta', 800000,  'PUTRI',  9,  4, true, now(), now()),
    (17, 1, 'Kost Akasia Putra',      'Kost putra ekonomis.',                          'Jl. Margonda Raya No. 55','Beji',        'Depok',     'Jawa Barat',    700000,  'PUTRA',  18, 9, true, now(), now()),
    (18, 2, 'Kost Palem Campur',      'Kost campur dekat mall.',                       'Jl. Sudirman No. 200',    'Menteng',     'Jakarta Pusat','DKI Jakarta',2800000, 'CAMPUR', 10, 3, true, now(), now()),
    (19, 1, 'Kost Bambu Putri',       'Kost putri dekat kampus Unpad.',                'Jl. Dago No. 150',        'Coblong',     'Bandung',   'Jawa Barat',    1400000, 'PUTRI',  11, 5, true, now(), now()),
    (20, 2, 'Kost Jati Putra',        'Kost putra bangunan baru.',                     'Jl. Airlangga No. 30',    'Gubeng',      'Surabaya',  'Jawa Timur',    1050000, 'PUTRA',  13, 7, true, now(), now()),
    (21, 1, 'Kost Kamboja Campur',    'Kost campur harga bersahabat.',                 'Jl. Kaliurang KM 3',      'Depok',       'Sleman',    'DI Yogyakarta', 600000,  'CAMPUR', 20, 15,true, now(), now()),
    (22, 2, 'Kost Lily Putri',        'Kost putri suasana asri.',                      'Jl. Margonda Raya No. 90','Beji',        'Depok',     'Jawa Barat',    950000,  'PUTRI',  10, 4, true, now(), now()),
    (23, 1, 'Kost Tulip Putra',       'Kost putra lokasi strategis.',                  'Jl. Dago No. 200',        'Coblong',     'Bandung',   'Jawa Barat',    1550000, 'PUTRA',  9,  2, true, now(), now()),
    (24, 2, 'Kost Violet Campur',     'Kost campur dekat stasiun.',                    'Jl. Diponegoro No. 60',   'Gubeng',      'Surabaya',  'Jawa Timur',    1100000, 'CAMPUR', 12, 6, true, now(), now()),
    (25, 1, 'Kost Aster Putri',       'Kost putri dengan taman.',                      'Jl. Sudirman No. 300',    'Menteng',     'Jakarta Pusat','DKI Jakarta',3500000, 'PUTRI',  8,  3, true, now(), now()),
    (26, 2, 'Kost Krisan Putra',      'Kost putra dekat kampus ITS.',                  'Jl. Airlangga No. 45',    'Gubeng',      'Surabaya',  'Jawa Timur',    900000,  'PUTRA',  15, 9, true, now(), now()),
    (27, 1, 'Kost Geranium Campur',   'Kost campur baru direnovasi.',                  'Jl. Kaliurang KM 7',      'Ngaglik',     'Sleman',    'DI Yogyakarta', 850000,  'CAMPUR', 14, 8, true, now(), now()),
    (28, 2, 'Kost Lavender Putri',    'Kost putri dekat perkantoran.',                 'Jl. Sudirman No. 400',    'Setiabudi',   'Jakarta Selatan','DKI Jakarta',5000000,'PUTRI',  6,  1, true, now(), now()),
    (29, 1, 'Kost Iris Putra',        'Kost putra lengkap dengan gym.',                'Jl. Dago No. 250',        'Coblong',     'Bandung',   'Jawa Barat',    2000000, 'PUTRA',  10, 4, true, now(), now()),
    (30, 2, 'Kost Camelia Campur',    'Kost campur dekat pasar tradisional.',          'Jl. Margonda Raya No. 120','Beji',       'Depok',     'Jawa Barat',    800000,  'CAMPUR', 16, 10,true, now(), now());
SELECT setval('kosts_id_seq', (SELECT max(id) FROM kosts));

INSERT INTO kost_facilities (kost_id, name)
SELECT id, unnest(ARRAY['wifi', 'kamar mandi dalam', 'parkir motor']) FROM kosts;
