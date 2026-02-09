-- =============================================================================
-- PostgreSQL (Render): permisos para que Hibernate pueda crear tablas
-- =============================================================================
-- Si el backend en Render falla con "la relación no existe" o "permission denied
-- for schema public", ejecuta este script UNA VEZ conectado a tu base en Render.
--
-- Cómo ejecutarlo en Render:
-- 1. Dashboard Render → tu base PostgreSQL → Connect → "PSQL Command" o "Shell"
-- 2. Conéctate con el usuario que usa la app (ej. comecyt_db_portal_user).
-- 3. Pega y ejecuta los comandos siguientes.
--
-- Si tu usuario es el dueño de la base, puede que no haga falta. Si no es owner,
-- un superuser (o el owner) debe ejecutar los GRANT sustituyendo YOUR_APP_USER
-- por el usuario de la aplicación (ej. comecyt_db_portal_user).
-- =============================================================================

-- Dar uso y creación en el schema public al usuario de la app
GRANT USAGE ON SCHEMA public TO comecyt_db_portal_user;
GRANT CREATE ON SCHEMA public TO comecyt_db_portal_user;

-- Permisos sobre tablas y secuencias (para cuando Hibernate las cree)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO comecyt_db_portal_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO comecyt_db_portal_user;

-- Si la base ya existía y el schema public tiene dueño distinto, el owner puede hacer:
-- GRANT ALL ON SCHEMA public TO comecyt_db_portal_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO comecyt_db_portal_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO comecyt_db_portal_user;
