-- Seed: proyectos de ejemplo para desarrollo
INSERT INTO projects (name, description, owner_id, state, energy_type, province, country,
                      latitude, longitude, installed_capacity_mw, total_tokens, token_price,
                      minimum_investment, expected_annual_yield, start_date, end_date, active,
                      created_at, updated_at)
VALUES
    ('Campo Solar Mendoza I',
     'Parque solar fotovoltaico en el Valle de Uco con 5 MW de capacidad instalada.',
     1, 'OPEN', 'SOLAR', 'Mendoza', 'Argentina',
     -33.5986, -69.1566, 5.0, 50000, 10.00,
     100.00, 8.50, '2026-01-01', '2036-01-01', TRUE,
     NOW(), NOW()),

    ('Parque Eólico Patagonia Sur',
     'Aerogeneradores en la Patagonia con vientos constantes de más de 8 m/s promedio.',
     1, 'PRE_OPEN', 'WIND', 'Santa Cruz', 'Argentina',
     -51.6230, -69.2168, 12.0, 120000, 8.50,
     85.00, 10.20, '2026-06-01', '2041-06-01', TRUE,
     NOW(), NOW()),

    ('Mini Hidro Neuquén',
     'Central hidroeléctrica de paso sobre el río Limay.',
     2, 'DRAFT', 'HYDRO', 'Neuquén', 'Argentina',
     -38.9516, -68.0591, 2.5, 25000, 12.00,
     120.00, 7.80, '2027-01-01', '2047-01-01', TRUE,
     NOW(), NOW());

-- Métricas para el proyecto en OPEN
INSERT INTO project_metrics (project_id, period_start, period_end, energy_generated_kwh, revenue_generated, source, recorded_at)
VALUES
    (1, '2026-01-01', '2026-01-31', 185000.00, 74000.00, 'CAMMESA', NOW()),
    (1, '2026-02-01', '2026-02-28', 162000.00, 64800.00, 'CAMMESA', NOW());
