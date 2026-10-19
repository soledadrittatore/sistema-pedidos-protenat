-- Usuario de base de datos exclusivo de la aplicación.
-- Solo tiene permisos de SELECT, INSERT, UPDATE y DELETE sobre bd_protenat.
-- Ejecutar una vez, como root, después de 00_script_completo.sql.
CREATE USER IF NOT EXISTS 'protenat_app'@'localhost' IDENTIFIED BY 'protenat123';
GRANT SELECT, INSERT, UPDATE, DELETE ON bd_protenat.* TO 'protenat_app'@'localhost';
FLUSH PRIVILEGES;
