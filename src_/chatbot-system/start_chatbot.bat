@echo off
echo ====================================
echo Starting Chatbot Service...
echo ====================================
cd /d "%~dp0"
mvn spring-boot:run
pause
