@echo off
echo =========================================
echo    Compiling Spendly Java Application   
echo =========================================

if not exist "WebContent\WEB-INF\classes" mkdir "WebContent\WEB-INF\classes"
if not exist "dist" mkdir "dist"

copy /Y "src\main\resources\*" "WebContent\WEB-INF\classes\" >nul 2>&1

echo Compiling Java source files...
javac -cp "WebContent\WEB-INF\lib\*" -d "WebContent\WEB-INF\classes" -encoding UTF-8 src\main\java\com\spendly\config\*.java src\main\java\com\spendly\dao\*.java src\main\java\com\spendly\filter\*.java src\main\java\com\spendly\model\*.java src\main\java\com\spendly\service\*.java src\main\java\com\spendly\service\payment\*.java src\main\java\com\spendly\servlet\*.java src\main\java\com\spendly\util\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Compilation successful!
echo Packaging Spendly.war...
cd WebContent
jar -cvf ..\dist\Spendly.war *
cd ..

echo =========================================
echo  Build Complete: dist\Spendly.war ready!
echo =========================================
