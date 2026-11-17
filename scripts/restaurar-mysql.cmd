@echo off
setlocal EnableExtensions DisableDelayedExpansion
REM RESTAURACAO DESTRUTIVA: confirme apenas para banco de TESTES separado.
REM Por seguranca, esta rotina nao cria banco nem faz DROP automatico.
if "%~1"=="" (
    echo Uso: restaurar-mysql.cmd "C:\caminho\backup.sql" --confirmar
    exit /b 2
)
if /I not "%~2"=="--confirmar" (
    echo Restauracao nao iniciada: falta --confirmar.
    exit /b 2
)
if not exist "%~1" (
    echo Arquivo nao encontrado.
    exit /b 2
)

echo ATENCAO: os comandos SQL podem modificar ou substituir dados.
echo Use somente um banco isolado para testar a recuperacao.
set /p "APROVACAO=Digite RESTAURAR para confirmar a operacao: "
if not "%APROVACAO%"=="RESTAURAR" (
    echo Cancelado.
    exit /b 2
)

REM O caminho de configuracao fica no login-path local do MySQL.
REM Para restaurar em banco de testes use login-path=soda_backup_teste.
mysql --login-path=soda_backup_teste soda_caustica_flanges_teste < "%~1"
if errorlevel 1 (
    echo Falha na restauracao. Verifique o banco de testes e o log.
    exit /b 1
)
echo Restauracao concluida no banco de testes. Compare contagens e integridade.
exit /b 0
