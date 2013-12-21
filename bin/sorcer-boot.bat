@echo off
pushd
set STARTER_MAIN_CLASS=sorcer.boot.ServiceStarter
set CONFIG=..\configs\SorcerBoot.groovy

SET CFG_EXT="%SORCER_EXT%\configs\SorcerExtBoot.groovy"
IF EXIST  (
set CONFIG=%CONFIG% %CFG_EXT%
)

SET mypath=%~dp0
SET SHOME_BIN=%mypath:~0,-1%
IF NOT DEFINED SORCER_HOME (
    IF EXIST %SHOME_BIN%\sorcer-boot.bat (
        SET SORCER_HOME=%SHOME_BIN%\..
    ) ELSE (
        ECHO Problem setting SORCER_HOME, please set this variable and point it to the main SORCER installation directory!
    )
)

IF defined SORCER_HOME ( 
  call %SORCER_HOME%\bin\common-run.bat
) ELSE (
  if exist %CD%\common-run.bat (
    call common-run.bat
  ) ELSE (
    call %CD%\bin\common-run.bat
  )
)

echo ##############################################################################
echo ##                       SORCER OS Booter                                
echo ##   SORCER_HOME: %SORCER_HOME%
echo ##   RIO_HOME   : %RIO_HOME%
echo ##   Webster URL: %WEBSTER_URL%
echo ##   
echo ##############################################################################
echo .
cd %SORCER_HOME%\bin
call %SOS_START_CMD%
popd
