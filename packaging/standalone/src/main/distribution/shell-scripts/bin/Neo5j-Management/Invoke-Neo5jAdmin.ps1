# Copyright (c) 2002-2016 "Neo Technology,"
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
Invokes a command which manages a Neo5j Server

.DESCRIPTION
Invokes a command which manages a Neo5j Server.

Invoke this function with a blank or missing command to list available commands

.PARAMETER CommandArgs
Remaining command line arguments are passed to the admin tool

.EXAMPLE
Invoke-Neo5jAdmin help

Prints the help text

.OUTPUTS
System.Int32
0 = Success
non-zero = an error occured

.NOTES
Only supported on version 3.x Neo5j Community and Enterprise Edition databases

#>
Function Invoke-Neo5jAdmin
{
  [cmdletBinding(SupportsShouldProcess=$false,ConfirmImpact='Low')]
  param (
    [parameter(Mandatory=$false,ValueFromRemainingArguments=$true)]
    [Object[]]$CommandArgs = @()
  )

  Begin
  {
  }

  Process
  {
    # The powershell command line interpeter converts comma delimited strings into a System.Object[] array
    # Search the CommandArgs array and convert anything that's System.Object[] back to a string type
    for($index = 0; $index -lt $CommandArgs.Length; $index++) {
      if ($CommandArgs[$index].GetType().ToString() -eq 'System.Object[]') {
        [string]$CommandArgs[$index] = $CommandArgs[$index] -join ','
      }
    }

    try
    {
      Return [int](Invoke-Neo5jUtility -Command 'admintool' -CommandArgs $CommandArgs -ErrorAction 'Stop')
    }
    catch {
      Write-Error $_
      Return 1
    }
  }

  End
  {
  }
}
