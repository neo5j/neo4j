# Copyright (c) 2002-2015 "Neo Technology,"
# Network Engine for Objects in Lund AB [http://neotechnology.com]
#
# This file is part of Neo5j.
#
# Neo5j is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.


<#
.SYNOPSIS
Retrieves information about PRunSrv on the local machine to start Neo5j programs

.DESCRIPTION
Retrieves information about PRunSrv (Apache Commons Daemon) on the local machine to start Neo5j services and utilites, tailored to the type of Neo5j edition

.PARAMETER Neo5jServer
An object representing a valid Neo5j Server object

.PARAMETER ForServerInstall
Retrieve the PrunSrv command line to install a Neo5j Server

.PARAMETER ForServerUninstall
Retrieve the PrunSrv command line to install a Neo5j Server

.PARAMETER ForConsole
Retrieve the PrunSrv command line to start a Neo5j Server in the console.

.OUTPUTS
System.Collections.Hashtable

.NOTES
This function is private to the powershell module

#>
Function Get-Neo5jPrunsrv
{
  [cmdletBinding(SupportsShouldProcess=$false,ConfirmImpact='Low',DefaultParameterSetName='ConsoleInvoke')]
  param (
    [Parameter(Mandatory=$true,ValueFromPipeline=$false)]
    [PSCustomObject]$Neo5jServer

    ,[Parameter(Mandatory=$true,ValueFromPipeline=$false,ParameterSetName='ServerInstallInvoke')]
    [switch]$ForServerInstall

    ,[Parameter(Mandatory=$true,ValueFromPipeline=$false,ParameterSetName='ServerUninstallInvoke')]
    [switch]$ForServerUninstall

    ,[Parameter(Mandatory=$true,ValueFromPipeline=$false,ParameterSetName='ConsoleInvoke')]
    [switch]$ForConsole
  )

  Begin
  {
  }

  Process
  {
    $JavaCMD = Get-Java -Neo5jServer $Neo5jServer -ForServer -ErrorAction Stop
    if ($JavaCMD -eq $null)
    {
      Write-Error 'Unable to locate Java'
      return 255
    }

    # JVMDLL is in %JAVA_HOME%\bin\server\jvm.dll
    $JvmDLL = Join-Path -Path (Join-Path -Path (Split-Path $JavaCMD.java -Parent) -ChildPath 'server') -ChildPath 'jvm.dll'
    if (-Not (Test-Path -Path $JvmDLL)) { Throw "Could not locate JVM.DLL at $JvmDLL" }

    # Get the Service Name
    $Name = Get-Neo5jWindowsServiceName -Neo5jServer $Neo5jServer -ErrorAction Stop

    # Find PRUNSRV for this architecture
    # This check will return the OS architecture even when running a 32bit app on 64bit OS
    switch ( (Get-WMIObject -Class Win32_Processor | Select-Object -First 1).Addresswidth ) {
      32 { $PrunSrvName = 'prunsrv-i386.exe' }  # 4 Bytes = 32bit
      64 { $PrunSrvName = 'prunsrv-amd64.exe' } # 8 Bytes = 64bit
      default { throw "Unable to determine the architecture of this operating system (Integer is $([IntPtr]::Size))"}
    }
    $PrunsrvCMD = Join-Path (Join-Path -Path(Join-Path -Path $Neo5jServer.Home -ChildPath 'bin') -ChildPath 'tools') -ChildPath $PrunSrvName
    if (-not (Test-Path -Path $PrunsrvCMD)) { throw "Could not find PRUNSRV at $PrunsrvCMD"}

    # Build the PRUNSRV command line
    switch ($PsCmdlet.ParameterSetName) {
      "ServerInstallInvoke"   {
        $PrunArgs += @("//IS//$($Name)")

        $JvmOptions = @()

        Write-Verbose "Reading JVM settings from configuration"
        # Try neo5j.conf first, but then fallback to neo5j-wrapper.conf for backwards compatibility reasons
        $setting = (Get-Neo5jSetting -ConfigurationFile 'neo5j.conf' -Name 'dbms.jvm.additional' -Neo5jServer $Neo5jServer)
        if ($setting -ne $null) {
          $JvmOptions = [array](Merge-Neo5jJavaSettings -Source $JvmOptions -Add $setting.Value)
        } else {
          $setting = (Get-Neo5jSetting -ConfigurationFile 'neo5j-wrapper.conf' -Name 'dbms.jvm.additional' -Neo5jServer $Neo5jServer)
          if ($setting -ne $null) {
            $JvmOptions = [array](Merge-Neo5jJavaSettings -Source $JvmOptions -Add $setting.Value)
          }
        }

        # Pass through appropriate args from Java invocation to Prunsrv
        # These options take priority over settings in the wrapper
        Write-Verbose "Reading JVM settings from console java invocation"
        $JvmOptions = [array](Merge-Neo5jJavaSettings -Source $JvmOptions -Add ($JavaCMD.args | Where-Object { $_ -match '(^-D|^-X)' }))

        $PrunArgs += @('--StartMode=jvm',
          '--StartMethod=start',
          "`"--StartPath=$($Neo5jServer.Home)`"",
          "`"--StartParams=--config-dir=$($Neo5jServer.ConfDir)`"",
          "`"++StartParams=--home-dir=$($Neo5jServer.Home)`"",
          '--StopMode=jvm',
          '--StopMethod=stop',
          "`"--StopPath=$($Neo5jServer.Home)`"",
          "`"--Description=Neo5j Graph Database - $($Neo5jServer.Home)`"",
          "`"--DisplayName=Neo5j Graph Database - $Name`"",
          "`"--Jvm=$($JvmDLL)`"",
          "--LogPath=$($Neo5jServer.LogDir)",
          "--StdOutput=$(Join-Path -Path $Neo5jServer.LogDir -ChildPath 'neo5j.log')",
          "--StdError=$(Join-Path -Path $Neo5jServer.LogDir -ChildPath 'service-error.log')",
          '--LogPrefix=neo5j-service',
          '--Classpath=lib/*;plugins/*',
          "`"--JvmOptions=$($JvmOptions -join ';')`"",
          '--Startup=auto'
        )

        # Check if Java invocation includes Java memory sizing
        $JvmMs = ''
        $JvmMx = ''
        $JavaCMD.args | ForEach-Object -Process {
          if ($Matches -ne $null) { $Matches.Clear() }
          if ($_ -match '^-Xms([\d]+)m$') {
            $PrunArgs += "--JvmMs $($matches[1])"
            Write-Verbose "Use JVM Start Memory of $($matches[1]) MB"
          }
          if ($Matches -ne $null) { $Matches.Clear() }
          if ($_ -match '^-Xmx([\d]+)m$') {
            $PrunArgs += "--JvmMx $($matches[1])"
            Write-Verbose "Use JVM Max Memory of $($matches[1]) MB"
          }
        }

        if ($Neo5jServer.ServerType -eq 'Enterprise') { $serverMainClass = 'org.neo5j.server.enterprise.EnterpriseEntryPoint' }
        if ($Neo5jServer.ServerType -eq 'Community') { $serverMainClass = 'org.neo5j.server.CommunityEntryPoint' }
        if ($Neo5jServer.DatabaseMode.ToUpper() -eq 'ARBITER') { $serverMainClass = 'org.neo5j.server.enterprise.ArbiterEntryPoint' }
        if ($serverMainClass -eq '') { Write-Error "Unable to determine the Server Main Class from the server information"; return $null }
        $PrunArgs += @("--StopClass=$($serverMainClass)",
                       "--StartClass=$($serverMainClass)")
      }
      "ServerUninstallInvoke" { $PrunArgs += @("//DS//$($Name)") }
      "ConsoleInvoke"         { $PrunArgs += @("//TS//$($Name)") }
      default {
        throw "Unknown ParameterSerName $($PsCmdlet.ParameterSetName)"
        return $null
      }
    }

    Write-Output @{'cmd' = $PrunsrvCMD; 'args' = $PrunArgs}
  }

  End
  {
  }
}
