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

echo Restarting OpenMRS to load the module...
docker compose restart openmrs

echo Done! Check http://localhost:8080/openmrs
pause
