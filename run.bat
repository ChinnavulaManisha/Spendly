@echo off
echo ==================================================================
echo                  STARTING SPENDLY APPLICATION                     
echo ==================================================================

if not exist "WebContent\WEB-INF\classes\com\spendly\runner\SpendlyServer.class" (
    echo Compiling application first...
    call build.bat
)

echo Starting Spendly Server on http://localhost:8085/ ...
java -cp "WebContent\WEB-INF\classes;WebContent\WEB-INF\lib\*" com.spendly.runner.SpendlyServer 8085
pause
