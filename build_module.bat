@echo off
echo Building module...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo Copying .omod to docker/modules...
if not exist "docker\modules" mkdir "docker\modules"
copy /Y omod\target\webservices.rest-*.omod docker\modules\

echo Done! Now restart Docker to load the module:
echo   docker compose down
echo   docker compose --env-file .env up -d
pause
