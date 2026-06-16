@echo off
echo Verifying Moodle Data...
echo.
cd c:\xampp\htdocs\moodle
c:\xampp\php\php.exe verify_data.php
echo.
echo Done!
pause
