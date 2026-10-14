@echo off
REM ============================================================
REM  Gera a aplicacao executavel para Windows (com o Java incluido).
REM  Requisitos nesta maquina: JDK 17 ou superior e Maven no PATH.
REM  Resultado: dist\Orcamentos\Orcamentos.exe  (pasta pronta a entregar)
REM ============================================================
setlocal
cd /d "%~dp0"

echo [1/3] A compilar e a juntar as bibliotecas...
call mvn -q clean package -DskipTests
if errorlevel 1 goto erro

echo [2/3] A criar a aplicacao com o Java incluido...
if exist dist rmdir /s /q dist
jpackage --type app-image ^
  --name Orcamentos ^
  --app-version 1.0 ^
  --vendor "Numeros Proficuos" ^
  --input target\jpackage ^
  --main-jar orcamentos.jar ^
  --main-class Main ^
  --icon packaging\icone.ico ^
  --add-modules java.base,java.desktop,java.logging,java.xml,java.prefs,jdk.unsupported ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --dest dist
if errorlevel 1 goto erro

echo [3/3] Concluido.
echo.
echo A pasta dist\Orcamentos esta pronta. Para entregar, comprima-a em ZIP.
echo O cliente so tem de abrir Orcamentos.exe (nao precisa de instalar o Java).
pause
exit /b 0

:erro
echo.
echo Ocorreu um erro. Confirme que o JDK 17+ e o Maven estao instalados e no PATH
echo (comandos: java -version, mvn -version, jpackage --version).
pause
exit /b 1
