-- Execute este script no MySQL Workbench usando uma conta com privilégios administrativos.
-- Substitua a senha placeholder pela mesma senha definida em .env (DB_PASSWORD).

CREATE DATABASE IF NOT EXISTS soda_caustica_flanges
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'soda_app'@'localhost'
    IDENTIFIED BY 'TROQUE_POR_UMA_SENHA_FORTE_2026';

ALTER USER 'soda_app'@'localhost'
    IDENTIFIED BY 'TROQUE_POR_UMA_SENHA_FORTE_2026';

GRANT ALL PRIVILEGES ON soda_caustica_flanges.*
    TO 'soda_app'@'localhost';

FLUSH PRIVILEGES;
