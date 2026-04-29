-- V2: Soft delete para usuarios + timestamps de auditoría en entidades principales

-- Usuarios: columna deleted_at para baja lógica
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Participantes: columna deleted_at y updated_at para auditoría completa
ALTER TABLE children ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE children ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Actividades: deleted_at para baja lógica de personalizadas
ALTER TABLE activities ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Índices para consultas de elementos activos
CREATE INDEX IF NOT EXISTS idx_users_deleted_at    ON users(deleted_at)    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_children_deleted_at ON children(deleted_at) WHERE deleted_at IS NULL;
