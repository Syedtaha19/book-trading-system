@echo off
REM ================================================================================
REM Run GUI - Interactive book trading simulation with seller GUIs
REM ================================================================================

REM Check if Java is already in PATH, otherwise use local installation
where javac >nul 2>nul
if errorlevel 1 (
    echo Java not found in PATH, using local JDK installation...
    set "PATH=D:\Program Files\Java\jdk-17\bin;%PATH%"
)

echo ================================================================================
echo COMPILING JAVA FILES
echo ================================================================================
echo.

javac *.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo *** COMPILATION FAILED ***
    pause
    exit /b 1
)

echo.
echo *** Compilation successful ***
echo.
echo ================================================================================
echo RUNNING INTERACTIVE SIMULATION (WITH GUI)
echo ================================================================================
echo.
echo Instructions:
echo 1. Seller GUI windows will appear
echo 2. Add books to each seller's catalogue
echo - Use exact titles: e.g The-Kite-Runner, Life-of-Pi
echo 3. Press Enter in this console to start buyers
echo 4. Watch the table update as books are sold
echo.

java BookTradingSimulation

pause
