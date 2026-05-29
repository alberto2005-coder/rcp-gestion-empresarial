@echo off
REM Script para empaquetar RCP Gestion como EXE en Windows
REM Requiere: JDK 21+ y WiX Toolset 3.11+

echo ========================================
echo RCP Gestion - Empaquetador a EXE
echo ========================================
echo.

REM Verificar si Maven está instalado
mvn -v >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven no está instalado o no está en PATH
    echo Descarga Maven desde: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Verificar si JDK está instalado
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java 21+ no está instalado o no está en PATH
    echo Descarga JDK desde: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo [1/4] Limpiando proyecto...
call mvn clean
if errorlevel 1 (
    echo ERROR durante limpieza
    pause
    exit /b 1
)

echo.
echo [2/4] Compilando y empaquetando...
call mvn package -DskipTests
if errorlevel 1 (
    echo ERROR durante empaquetado
    pause
    exit /b 1
)

echo.
echo [3/4] Moviendo JAR a directorio raíz...
if exist "target\rcp-empresarial.jar" (
    copy "target\rcp-empresarial.jar" "rcp-empresarial.jar"
    echo JAR movido correctamente
) else (
    echo ERROR: JAR no encontrado en target/
    pause
    exit /b 1
)

echo.
echo [4/4] Generando EXE con jpackage...
echo.

REM Verificar si jpackage está disponible
jpackage --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: jpackage no encontrado
    echo jpackage está incluido en JDK 16+
    echo Asegúrate de tener JDK 21+ instalado correctamente
    pause
    exit /b 1
)

REM Generar EXE
jpackage ^
    --input . ^
    --name "RCPGestion" ^
    --main-jar rcp-empresarial.jar ^
    --main-class com.empresa.rcp.App ^
    --type exe ^
    --win-console ^
    --win-menu ^
    --win-menu-group "Aplicaciones" ^
    --description "RCP Gestión Empresarial" ^
    --vendor "Gestión Sistemas"

if errorlevel 1 (
    echo ERROR durante generación del EXE
    echo Verifica que WiX Toolset 3.11+ esté instalado
    echo Descarga desde: https://wixtoolset.org/
    pause
    exit /b 1
)

echo.
echo ========================================
echo ^! ÉXITO - EXE Generado
echo ========================================
echo.
echo El archivo "RCPGestion-1.0.0.exe" se encuentra en el directorio actual
echo Puedes ejecutarlo directamente o compartirlo con otros usuarios
echo.
echo Para instalar en otra máquina:
echo 1. Copia RCPGestion-1.0.0.exe
echo 2. Ejecuta el instalador
echo 3. Java se incluye automáticamente
echo.
pause
