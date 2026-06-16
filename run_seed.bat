@echo off
echo Running Moodle Data Seeder...
echo.
cd c:\xampp\htdocs\moodle
c:\xampp\php\php.exe seed_data.php
echo.
echo Done!
pause
