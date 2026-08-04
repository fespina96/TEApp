-- Unicidad del email: insensible a mayúsculas y sin contar las cuentas dadas de baja.
--
-- La restricción original, uk_users_email UNIQUE (email), tenía dos problemas:
--
--   1. Distinguía mayúsculas, así que "Ana@x.com" y "ana@x.com" convivían como
--      dos cuentas distintas para la misma persona.
--   2. Alcanzaba también a las filas con deleted_at, que la aplicación ya no ve
--      por el @SQLRestriction de la entidad User. La validación previa al alta
--      daba vía libre y el INSERT terminaba violando la restricción: un error
--      500 en lugar del 400 que corresponde.

-- Los emails ya cargados pasan a minúsculas para que el índice nuevo no falle
-- y para que coincidan con lo que ahora guarda el registro.
UPDATE users SET email = LOWER(email) WHERE email <> LOWER(email);

-- Si el paso anterior dejó duplicados (por ejemplo "Ana@x.com" junto a
-- "ana@x.com"), se conserva la cuenta más antigua y las otras quedan de baja:
-- borrarlas arrastraría participantes, agendas y completitudes.
UPDATE users
   SET deleted_at = NOW()
 WHERE deleted_at IS NULL
   AND id NOT IN (
       SELECT DISTINCT ON (email) id
         FROM users
        WHERE deleted_at IS NULL
        ORDER BY email, created_at
   );

ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_email;

-- Índice parcial: sólo alcanza a las cuentas activas, así que dar de baja una
-- cuenta libera su email para volver a registrarlo.
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_activo
    ON users (email)
 WHERE deleted_at IS NULL;
