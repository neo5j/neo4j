/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo5j.kernel.lifecycle;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.neo5j.kernel.lifecycle.LifecycleStatus.NONE;
import static org.neo5j.kernel.lifecycle.LifecycleStatus.SHUTDOWN;
import static org.neo5j.kernel.lifecycle.LifecycleStatus.STARTED;
import static org.neo5j.kernel.lifecycle.LifecycleStatus.STOPPED;

public class TestLifecycleException
{

    @Test
    public void shouldMakeNoneToStoppedIntoHumanReadableInitMessage() throws Exception
    {
        assertThat(
                exceptionFor( NONE, STOPPED ).getMessage(),
                is("Component 'SomeComponent' failed to initialize. Please see attached cause exception."));
    }

    @Test
    public void shouldMakeStoppedToStartedIntoHumanReadableStartingMessage() throws Exception
    {
        assertThat(
                exceptionFor( STOPPED, STARTED ).getMessage(),
                is("Component 'SomeComponent' was successfully initialized, but failed to start. Please see attached cause exception."));
    }

    @Test
    public void shouldMakeStartedToStoppedIntoHumanReadableStoppingMessage() throws Exception
    {
        assertThat(
                exceptionFor( STARTED, STOPPED ).getMessage(),
                is("Component 'SomeComponent' failed to stop. Please see attached cause exception."));
    }

    @Test
    public void shouldMakeShutdownIntoHumanReadableShutdownMessage() throws Exception
    {
        assertThat(
                exceptionFor( STOPPED, SHUTDOWN ).getMessage(),
                is("Component 'SomeComponent' failed to shut down. Please see attached cause exception."));
    }

    private LifecycleException exceptionFor( LifecycleStatus from, LifecycleStatus to )
    {
        return new LifecycleException( new Object()
        {
            @Override
            public String toString()
            {
                return "SomeComponent";
            }
        }, from, to, null );
    }

}
