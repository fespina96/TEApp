-- ============================================================
-- TEApp — Migración inicial consolidada
-- Crea todas las tablas e inserta el catálogo de actividades
-- predefinidas con pictogramas ARASAAC y pasos detallados.
-- ============================================================


-- ─────────────────────────────────────────────────────────────
-- TABLAS
-- ─────────────────────────────────────────────────────────────

CREATE TABLE users (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL,
    password       VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'PARENT',
    invite_code    VARCHAR(10)  UNIQUE,
    date_of_birth  DATE,
    avatar_base64  TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users       PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE children (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    name          VARCHAR(100) NOT NULL,
    date_of_birth DATE         NOT NULL,
    avatar_color  VARCHAR(7)   NOT NULL DEFAULT '#A8D8EA',
    avatar_base64 TEXT,
    notes         TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_children      PRIMARY KEY (id),
    CONSTRAINT fk_children_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_children_user_id ON children(user_id);

CREATE TABLE activities (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    category        VARCHAR(20)  NOT NULL,
    icon_name       VARCHAR(50),
    color           VARCHAR(7)   NOT NULL DEFAULT '#A8D8EA',
    is_predefined   BOOLEAN      NOT NULL DEFAULT FALSE,
    pictogram_url   TEXT,
    image_base64    TEXT,
    duration_minutes INTEGER,
    pausable        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      UUID,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_activities      PRIMARY KEY (id),
    CONSTRAINT fk_activities_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_activities_predefined ON activities(is_predefined);
CREATE INDEX idx_activities_created_by ON activities(created_by);
CREATE INDEX idx_activities_category   ON activities(category);

CREATE TABLE schedule_entries (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    child_id            UUID        NOT NULL,
    activity_id         UUID        NOT NULL,
    day_of_week         VARCHAR(10) NOT NULL,
    time_slot           VARCHAR(10) NOT NULL,
    start_time          TIME,
    end_time            TIME,
    sort_order          INTEGER     NOT NULL DEFAULT 0,
    duration_minutes    INTEGER,
    pausable            BOOLEAN     NOT NULL DEFAULT TRUE,
    require_full_timer  BOOLEAN     NOT NULL DEFAULT FALSE,
    notes               VARCHAR(500),
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_schedule_entries    PRIMARY KEY (id),
    CONSTRAINT fk_schedule_child      FOREIGN KEY (child_id)    REFERENCES children(id)    ON DELETE CASCADE,
    CONSTRAINT fk_schedule_activity   FOREIGN KEY (activity_id) REFERENCES activities(id)
);

CREATE INDEX idx_schedule_child ON schedule_entries(child_id);
CREATE INDEX idx_schedule_day   ON schedule_entries(child_id, day_of_week);
CREATE INDEX idx_schedule_order ON schedule_entries(child_id, day_of_week, time_slot, sort_order);

CREATE TABLE activity_completions (
    id                UUID NOT NULL DEFAULT gen_random_uuid(),
    schedule_entry_id UUID NOT NULL,
    child_id          UUID NOT NULL,
    completed_date    DATE NOT NULL,

    CONSTRAINT pk_activity_completions    PRIMARY KEY (id),
    CONSTRAINT fk_completion_entry        FOREIGN KEY (schedule_entry_id) REFERENCES schedule_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_completion_child        FOREIGN KEY (child_id)          REFERENCES children(id)         ON DELETE CASCADE,
    CONSTRAINT uk_completion_entry_date   UNIQUE (schedule_entry_id, completed_date)
);

CREATE INDEX idx_completion_child_date ON activity_completions(child_id, completed_date);
CREATE INDEX idx_completion_entry      ON activity_completions(schedule_entry_id);

CREATE TABLE password_reset_tokens (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    token      VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_reset_token_user      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_reset_token           UNIQUE (token)
);

CREATE INDEX idx_reset_token ON password_reset_tokens(token);

CREATE TABLE therapist_parent_links (
    therapist_id UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    linked_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (therapist_id, parent_id)
);

CREATE TABLE activity_steps (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id   UUID         NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    step_order    INTEGER      NOT NULL DEFAULT 0,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    image_base64  TEXT,
    pictogram_url TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_activity_steps_activity ON activity_steps(activity_id, step_order);


-- ─────────────────────────────────────────────────────────────
-- CATÁLOGO DE ACTIVIDADES PREDEFINIDAS
-- Pictogramas ARASAAC: https://static.arasaac.org/pictograms/{id}/{id}_500.png
-- ─────────────────────────────────────────────────────────────

-- HYGIENE (#A8D8EA)
INSERT INTO activities (id, name, description, category, icon_name, color, is_predefined, pictogram_url) VALUES
  (gen_random_uuid(), 'Lavarse los dientes', 'Cepillado de dientes mañana y noche',  'HYGIENE', 'mood',        '#A8D8EA', TRUE, 'https://static.arasaac.org/pictograms/6971/6971_500.png'),
  (gen_random_uuid(), 'Ducharse / Bañarse',  'Higiene corporal diaria',               'HYGIENE', 'bathtub',     '#A8D8EA', TRUE, 'https://static.arasaac.org/pictograms/32426/32426_500.png'),
  (gen_random_uuid(), 'Lavarse las manos',   'Antes de comer y después del baño',    'HYGIENE', 'clean_hands', '#A8D8EA', TRUE, 'https://static.arasaac.org/pictograms/2443/2443_500.png'),
  (gen_random_uuid(), 'Peinarse',            'Cepillado y arreglo del cabello',       'HYGIENE', 'face',        '#A8D8EA', TRUE, 'https://static.arasaac.org/pictograms/26947/26947_500.png'),
  (gen_random_uuid(), 'Vestirse',            'Ponerse la ropa del día',               'HYGIENE', 'checkroom',   '#A8D8EA', TRUE, 'https://static.arasaac.org/pictograms/6627/6627_500.png');

-- MEAL (#FAF0BE)
INSERT INTO activities (id, name, description, category, icon_name, color, is_predefined, pictogram_url) VALUES
  (gen_random_uuid(), 'Desayuno',   'Comida de la mañana',          'MEAL', 'free_breakfast', '#FAF0BE', TRUE, 'https://static.arasaac.org/pictograms/4626/4626_500.png'),
  (gen_random_uuid(), 'Almuerzo',   'Comida del mediodía',          'MEAL', 'restaurant',     '#FAF0BE', TRUE, 'https://static.arasaac.org/pictograms/28207/28207_500.png'),
  (gen_random_uuid(), 'Merienda',   'Snack de la tarde',            'MEAL', 'coffee',         '#FAF0BE', TRUE, 'https://static.arasaac.org/pictograms/4695/4695_500.png'),
  (gen_random_uuid(), 'Cena',       'Comida de la noche',           'MEAL', 'dinner_dining',  '#FAF0BE', TRUE, 'https://static.arasaac.org/pictograms/4592/4592_500.png'),
  (gen_random_uuid(), 'Beber agua', 'Hidratación durante el día',   'MEAL', 'local_drink',    '#FAF0BE', TRUE, 'https://static.arasaac.org/pictograms/6061/6061_500.png');

-- EDUCATION (#B8E0C8)
INSERT INTO activities (id, name, description, category, icon_name, color, is_predefined, pictogram_url) VALUES
  (gen_random_uuid(), 'Lectura',           'Tiempo de leer cuentos o libros',     'EDUCATION', 'menu_book',  '#B8E0C8', TRUE, 'https://static.arasaac.org/pictograms/7141/7141_500.png'),
  (gen_random_uuid(), 'Tareas escolares',  'Hacer los deberes del colegio',       'EDUCATION', 'school',     '#B8E0C8', TRUE, 'https://static.arasaac.org/pictograms/11228/11228_500.png'),
  (gen_random_uuid(), 'Pintar / Colorear', 'Actividad de dibujo y color',         'EDUCATION', 'palette',    '#B8E0C8', TRUE, 'https://static.arasaac.org/pictograms/2348/2348_500.png'),
  (gen_random_uuid(), 'Puzzles',           'Armar rompecabezas y juegos lógicos', 'EDUCATION', 'extension',  '#B8E0C8', TRUE, 'https://static.arasaac.org/pictograms/2540/2540_500.png'),
  (gen_random_uuid(), 'Música',            'Escuchar o practicar música',         'EDUCATION', 'music_note', '#B8E0C8', TRUE, 'https://static.arasaac.org/pictograms/24791/24791_500.png');

-- PLAY (#C9B8E8)
INSERT INTO activities (id, name, description, category, icon_name, color, is_predefined, pictogram_url) VALUES
  (gen_random_uuid(), 'Juego libre',       'Tiempo de juego sin estructurar',      'PLAY', 'toys',       '#C9B8E8', TRUE, 'https://static.arasaac.org/pictograms/23392/23392_500.png'),
  (gen_random_uuid(), 'Juego con bloques', 'Construcción con piezas y bloques',    'PLAY', 'view_in_ar', '#C9B8E8', TRUE, 'https://static.arasaac.org/pictograms/4935/4935_500.png'),
  (gen_random_uuid(), 'Ver televisión',    'Tiempo de pantalla controlado',        'PLAY', 'tv',         '#C9B8E8', TRUE, 'https://static.arasaac.org/pictograms/25498/25498_500.png'),
  (gen_random_uuid(), 'Juego de mesa',     'Jugar juegos de mesa con la familia',  'PLAY', 'casino',     '#C9B8E8', TRUE, 'https://static.arasaac.org/pictograms/9810/9810_500.png');

-- THERAPY (#D4E8C8)
INSERT INTO activities (id, name, description, category, icon_name, color, is_predefined, pictogram_url) VALUES
  (gen_random_uuid(), 'Terapia ocupacional',    'Sesión de terapia ocupacional',       'THERAPY', 'accessibility_new',  '#D4E8C8', TRUE, 'https://static.arasaac.org/pictograms/24821/24821_500.png'),
  (gen_random_uuid(), 'Terapia del lenguaje',   'Sesión con logopeda/fonoaudiólogo',   'THERAPY', 'record_voice_over',  '#D4E8C8', TRUE, 'https://static.arasaac.org/pictograms/2454/2454_500.png'),
  (gen_random_uuid(), 'Terapia ABA',            'Sesión de análisis de conducta',      'THERAPY', 'psychology',         '#D4E8C8', TRUE, 'https://static.arasaac.org/pictograms/24679/24679_500.png'),
  (gen_random_uuid(), 'Ejercicios sensoriales', 'Actividades de integración sensorial','THERAPY', 'spa',                '#D4E8C8', TRUE, 'https://static.arasaac.org/pictograms/37134/37134_500.png');

-- REST (#E0D8F0)
INSERT INTO activities (id, name, description, category, icon_name, color, is_predefined, pictogram_url) VALUES
  (gen_random_uuid(), 'Siesta',          'Descanso del mediodía',              'REST', 'bedtime',          '#E0D8F0', TRUE, 'https://static.arasaac.org/pictograms/28425/28425_500.png'),
  (gen_random_uuid(), 'Hora de dormir',  'Preparación para la noche y sueño', 'REST', 'nights_stay',       '#E0D8F0', TRUE, 'https://static.arasaac.org/pictograms/6479/6479_500.png'),
  (gen_random_uuid(), 'Tiempo tranquilo','Momento de calma y relajación',     'REST', 'self_improvement',  '#E0D8F0', TRUE, 'https://static.arasaac.org/pictograms/38377/38377_500.png');

-- OUTDOOR (#C8E8D0)
INSERT INTO activities (id, name, description, category, icon_name, color, is_predefined, pictogram_url) VALUES
  (gen_random_uuid(), 'Paseo al parque',     'Salida al parque o zona verde',    'OUTDOOR', 'park',            '#C8E8D0', TRUE, 'https://static.arasaac.org/pictograms/2859/2859_500.png'),
  (gen_random_uuid(), 'Bicicleta',           'Paseo en bicicleta o triciclo',    'OUTDOOR', 'directions_bike', '#C8E8D0', TRUE, 'https://static.arasaac.org/pictograms/6935/6935_500.png'),
  (gen_random_uuid(), 'Juego en el jardín',  'Tiempo al aire libre en casa',     'OUTDOOR', 'yard',            '#C8E8D0', TRUE, 'https://static.arasaac.org/pictograms/2434/2434_500.png'),
  (gen_random_uuid(), 'Natación',            'Sesión de natación',               'OUTDOOR', 'pool',            '#C8E8D0', TRUE, 'https://static.arasaac.org/pictograms/25038/25038_500.png');


-- ─────────────────────────────────────────────────────────────
-- PASOS DE ACTIVIDADES PREDEFINIDAS
-- ─────────────────────────────────────────────────────────────

-- Lavarse los dientes
DO $$ DECLARE act_id UUID; BEGIN
  SELECT id INTO act_id FROM activities WHERE name = 'Lavarse los dientes' AND is_predefined = TRUE;
  INSERT INTO activity_steps (activity_id, step_order, title, pictogram_url) VALUES
    (act_id, 0, 'Agarrar el cepillo',     'https://static.arasaac.org/pictograms/38813/38813_500.png'),
    (act_id, 1, 'Poner pasta de dientes', 'https://static.arasaac.org/pictograms/2858/2858_500.png'),
    (act_id, 2, 'Cepillar los dientes',   'https://static.arasaac.org/pictograms/6971/6971_500.png'),
    (act_id, 3, 'Enjuagarse la boca',     'https://static.arasaac.org/pictograms/8560/8560_500.png'),
    (act_id, 4, 'Escupir el agua',        'https://static.arasaac.org/pictograms/7090/7090_500.png');
END $$;

-- Lavarse las manos
DO $$ DECLARE act_id UUID; BEGIN
  SELECT id INTO act_id FROM activities WHERE name = 'Lavarse las manos' AND is_predefined = TRUE;
  INSERT INTO activity_steps (activity_id, step_order, title, pictogram_url) VALUES
    (act_id, 0, 'Abrir la canilla',   'https://static.arasaac.org/pictograms/2414/2414_500.png'),
    (act_id, 1, 'Mojar las manos',    'https://static.arasaac.org/pictograms/8975/8975_500.png'),
    (act_id, 2, 'Poner jabón',        'https://static.arasaac.org/pictograms/8094/8094_500.png'),
    (act_id, 3, 'Frotar las manos',   'https://static.arasaac.org/pictograms/8251/8251_500.png'),
    (act_id, 4, 'Enjuagar las manos', 'https://static.arasaac.org/pictograms/2443/2443_500.png'),
    (act_id, 5, 'Secar las manos',    'https://static.arasaac.org/pictograms/2566/2566_500.png');
END $$;

-- Ducharse / Bañarse
DO $$ DECLARE act_id UUID; BEGIN
  SELECT id INTO act_id FROM activities WHERE name = 'Ducharse / Bañarse' AND is_predefined = TRUE;
  INSERT INTO activity_steps (activity_id, step_order, title, pictogram_url) VALUES
    (act_id, 0, 'Sacarse la ropa',      'https://static.arasaac.org/pictograms/11233/11233_500.png'),
    (act_id, 1, 'Entrar a la ducha',    'https://static.arasaac.org/pictograms/32426/32426_500.png'),
    (act_id, 2, 'Mojarse',              'https://static.arasaac.org/pictograms/6149/6149_500.png'),
    (act_id, 3, 'Jabonarse el cuerpo',  'https://static.arasaac.org/pictograms/35729/35729_500.png'),
    (act_id, 4, 'Enjuagarse',           'https://static.arasaac.org/pictograms/21345/21345_500.png'),
    (act_id, 5, 'Secarse con la toalla','https://static.arasaac.org/pictograms/2593/2593_500.png');
END $$;

-- Vestirse
DO $$ DECLARE act_id UUID; BEGIN
  SELECT id INTO act_id FROM activities WHERE name = 'Vestirse' AND is_predefined = TRUE;
  INSERT INTO activity_steps (activity_id, step_order, title, pictogram_url) VALUES
    (act_id, 0, 'Ponerse la ropa interior', 'https://static.arasaac.org/pictograms/25680/25680_500.png'),
    (act_id, 1, 'Ponerse los pantalones',   'https://static.arasaac.org/pictograms/2565/2565_500.png'),
    (act_id, 2, 'Ponerse la remera',        'https://static.arasaac.org/pictograms/2309/2309_500.png'),
    (act_id, 3, 'Ponerse las medias',       'https://static.arasaac.org/pictograms/2298/2298_500.png'),
    (act_id, 4, 'Ponerse las zapatillas',   'https://static.arasaac.org/pictograms/2621/2621_500.png');
END $$;
