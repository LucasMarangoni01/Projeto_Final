@echo off
setlocal EnableExtensions DisableDelayedExpansion

REM Backup manual em Windows. Credenciais nao sao armazenadas neste arquivo.
REM Configure previamente: mysql_config_editor set --login-path=soda_backup --host=localhost --user=soda_app --password
set "DB_NAME=soda_caustica_flanges"
set "LOGIN_PATH=soda_backup"
set "BACKUP_DIR=%~dp0..\backups"

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
if errorlevel 1 (
    echo Falha ao criar pasta de backups.
    exit /b 1
)

for /f %%t in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "CARIMBO=%%t"
if not defined CARIMBO (
    echo Nao foi possivel obter a data para o backup.
    exit /b 1
)
set "DESTINO=%BACKUP_DIR%\flangeguard_%CARIMBO%.sql"

REM O .part so vira .sql apos backup completo. Nao usar redirecionamento em PowerShell.
mysqldump --login-path=%LOGIN_PATH% --single-transaction --set-gtid-purged=OFF --routines --triggers --events --hex-blob "%DB_NAME%" > "%DESTINO%.part"
if errorlevel 1 (
    del /q "%DESTINO%.part" 2>nul
    echo Falha no mysqldump. Nenhum backup foi considerado concluido.
    exit /b 1
)
for %%F in ("%DESTINO%.part") do if %%~zF EQU 0 (
    del /q "%DESTINO%.part" 2>nul
    echo Arquivo vazio. Backup abortado.
    exit /b 1
)
move /Y "%DESTINO%.part" "%DESTINO%" >nul
if errorlevel 1 (
    echo Falha ao finalizar backup.
    exit /b 1
)
echo Backup concluido: "%DESTINO%"
certutil -hashfile "%DESTINO%" SHA256
exit /b 0
