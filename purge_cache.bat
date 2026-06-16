@echo off
echo ========================================
echo  MOODLE CACHE PURGE HELPER
echo ========================================
echo.
echo Opening Moodle cache purge page...
echo.
start http://localhost/moodle/admin/purgecaches.php
echo.
echo ========================================
echo Instructions:
echo 1. Login as admin if needed
echo 2. Click "Purge all caches" button
echo 3. Wait for confirmation
echo 4. Press Ctrl + Shift + R to hard refresh
echo ========================================
echo.
pause
