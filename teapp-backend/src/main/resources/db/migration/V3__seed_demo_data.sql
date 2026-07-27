-- Datos de demostración para la evaluación del trabajo final.
-- Las credenciales de ambos usuarios son las publicadas en el documento de
-- Entrega Final: padre@teapp.com y terapeuta@teapp.com, contraseña Test1234!
-- El hash es BCrypt (strength 10), el mismo algoritmo que usa AuthService.

INSERT INTO users (id, email, password, full_name, role, invite_code, date_of_birth) VALUES
  ('11111111-1111-1111-1111-111111111111',
   'padre@teapp.com',
   '$2a$10$fCrnSD1Os0WAtXnXYYzRjuZXoj0evo/40WAdU/IGh4wVBvqRctmgu',
   'Juan Pérez', 'PARENT', NULL, DATE '1990-05-14'),
  ('22222222-2222-2222-2222-222222222222',
   'terapeuta@teapp.com',
   '$2a$10$fCrnSD1Os0WAtXnXYYzRjuZXoj0evo/40WAdU/IGh4wVBvqRctmgu',
   'Lucía Gómez', 'THERAPIST', 'TERAPEUT', DATE '1988-11-02')
ON CONFLICT (email) DO NOTHING;

-- Participante de ejemplo. La fecha se calcula para que siempre tenga 6 años.
INSERT INTO children (id, user_id, name, date_of_birth, avatar_color, notes) VALUES
  ('33333333-3333-3333-3333-333333333333',
   '11111111-1111-1111-1111-111111111111',
   'Sofía',
   (CURRENT_DATE - INTERVAL '6 years 4 months')::date,
   '#C9B8E8',
   'Perfil de ejemplo para la demostración del sistema.')
ON CONFLICT (id) DO NOTHING;

-- El terapeuta supervisa al padre (solo lectura sobre sus agendas).
INSERT INTO therapist_parent_links (therapist_id, parent_id) VALUES
  ('22222222-2222-2222-2222-222222222222',
   '11111111-1111-1111-1111-111111111111')
ON CONFLICT DO NOTHING;

-- Agenda semanal de Sofía, armada con actividades predefinidas del catálogo.
INSERT INTO schedule_entries (child_id, activity_id, day_of_week, time_slot, sort_order, duration_minutes)
SELECT '33333333-3333-3333-3333-333333333333'::uuid, a.id, d.dia, d.franja, d.orden, d.duracion
FROM (VALUES
    ('MONDAY',    'MORNING',   'Vestirse',             0, NULL::integer),
    ('MONDAY',    'MORNING',   'Desayuno',             1, NULL),
    ('MONDAY',    'MORNING',   'Lavarse los dientes',  2, 3),
    ('MONDAY',    'AFTERNOON', 'Almuerzo',             0, NULL),
    ('MONDAY',    'AFTERNOON', 'Tareas escolares',     1, 30),
    ('MONDAY',    'AFTERNOON', 'Merienda',             2, NULL),
    ('MONDAY',    'NIGHT',     'Cena',                 0, NULL),
    ('MONDAY',    'NIGHT',     'Ducharse / Bañarse',   1, 10),
    ('MONDAY',    'NIGHT',     'Hora de dormir',       2, NULL),

    ('TUESDAY',   'MORNING',   'Vestirse',             0, NULL),
    ('TUESDAY',   'MORNING',   'Desayuno',             1, NULL),
    ('TUESDAY',   'AFTERNOON', 'Almuerzo',             0, NULL),
    ('TUESDAY',   'AFTERNOON', 'Terapia del lenguaje', 1, 45),
    ('TUESDAY',   'NIGHT',     'Cena',                 0, NULL),
    ('TUESDAY',   'NIGHT',     'Hora de dormir',       1, NULL),

    ('WEDNESDAY', 'MORNING',   'Vestirse',             0, NULL),
    ('WEDNESDAY', 'MORNING',   'Desayuno',             1, NULL),
    ('WEDNESDAY', 'AFTERNOON', 'Almuerzo',             0, NULL),
    ('WEDNESDAY', 'AFTERNOON', 'Pintar / Colorear',    1, 20),
    ('WEDNESDAY', 'NIGHT',     'Cena',                 0, NULL),
    ('WEDNESDAY', 'NIGHT',     'Hora de dormir',       1, NULL),

    ('THURSDAY',  'MORNING',   'Vestirse',             0, NULL),
    ('THURSDAY',  'MORNING',   'Desayuno',             1, NULL),
    ('THURSDAY',  'AFTERNOON', 'Almuerzo',             0, NULL),
    ('THURSDAY',  'AFTERNOON', 'Natación',             1, 60),
    ('THURSDAY',  'NIGHT',     'Cena',                 0, NULL),
    ('THURSDAY',  'NIGHT',     'Hora de dormir',       1, NULL),

    ('FRIDAY',    'MORNING',   'Vestirse',             0, NULL),
    ('FRIDAY',    'MORNING',   'Desayuno',             1, NULL),
    ('FRIDAY',    'AFTERNOON', 'Almuerzo',             0, NULL),
    ('FRIDAY',    'AFTERNOON', 'Juego libre',          1, NULL),
    ('FRIDAY',    'NIGHT',     'Cena',                 0, NULL),
    ('FRIDAY',    'NIGHT',     'Ver televisión',       1, 30),
    ('FRIDAY',    'NIGHT',     'Hora de dormir',       2, NULL),

    ('SATURDAY',  'MORNING',   'Desayuno',             0, NULL),
    ('SATURDAY',  'MORNING',   'Paseo al parque',      1, NULL),
    ('SATURDAY',  'AFTERNOON', 'Almuerzo',             0, NULL),
    ('SATURDAY',  'AFTERNOON', 'Siesta',               1, NULL),
    ('SATURDAY',  'NIGHT',     'Cena',                 0, NULL),
    ('SATURDAY',  'NIGHT',     'Hora de dormir',       1, NULL),

    ('SUNDAY',    'MORNING',   'Desayuno',             0, NULL),
    ('SUNDAY',    'MORNING',   'Juego con bloques',    1, NULL),
    ('SUNDAY',    'AFTERNOON', 'Almuerzo',             0, NULL),
    ('SUNDAY',    'AFTERNOON', 'Lectura',              1, 15),
    ('SUNDAY',    'NIGHT',     'Cena',                 0, NULL),
    ('SUNDAY',    'NIGHT',     'Hora de dormir',       1, NULL)
  ) AS d(dia, franja, actividad, orden, duracion)
JOIN activities a ON a.name = d.actividad AND a.is_predefined = TRUE;
