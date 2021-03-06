/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo5j.configuration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.neo5j.cluster.ClusterSettings;
import org.neo5j.cluster.InstanceId;
import org.neo5j.graphdb.config.InvalidSettingException;
import org.neo5j.helpers.HostnamePort;
import org.neo5j.kernel.configuration.Config;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo5j.helpers.collection.MapUtil.stringMap;

@RunWith( Parameterized.class )
public class HaConfigurationValidatorTest
{
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameterized.Parameter
    public ClusterSettings.Mode mode;

    @Parameterized.Parameters( name = "{0}" )
    public static List<ClusterSettings.Mode> recordFormats()
    {
        return Arrays.asList( ClusterSettings.Mode.HA, ClusterSettings.Mode.ARBITER );
    }

    @Test
    public void validateOnlyIfModeIsHA() throws Exception
    {
        // when
        Config config = Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), ClusterSettings.Mode.SINGLE.name(),
                        ClusterSettings.initial_hosts.name(), "" ),
                Collections.singleton( new HaConfigurationValidator() ) );

        // then
        assertEquals( "", config.getRaw( ClusterSettings.initial_hosts.name() ).get() );
    }

    @Test
    public void validateSuccess() throws Exception
    {
        // when
        Config config = Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), mode.name(),
                        ClusterSettings.server_id.name(), "1",
                        ClusterSettings.initial_hosts.name(), "localhost,remotehost" ),
                Collections.singleton( new HaConfigurationValidator() ) );

        // then
        assertEquals( asList( new HostnamePort( "localhost" ),
                new HostnamePort( "remotehost" ) ),
                config.get( ClusterSettings.initial_hosts ) );
        assertEquals( new InstanceId( 1 ), config.get( ClusterSettings.server_id ) );
    }

    @Test
    public void missingServerId() throws Exception
    {
        // then
        expected.expect( InvalidSettingException.class );
        expected.expectMessage( "Missing mandatory value for 'ha.server_id'" );

        // when
        Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), mode.name() ),
                Collections.singleton( new HaConfigurationValidator() ) );
    }

    @Test
    public void missingInitialHosts() throws Exception
    {
        // then
        expected.expect( InvalidSettingException.class );
        expected.expectMessage( "Missing mandatory non-empty value for 'ha.initial_hosts'" );

        // when
        Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), mode.name(),
                        ClusterSettings.server_id.name(), "1" ),
                Collections.singleton( new HaConfigurationValidator() ) );
    }

    @Test
    public void initialHostsEmpty() throws Exception
    {
        // then
        expected.expect( InvalidSettingException.class );
        expected.expectMessage( "Missing mandatory non-empty value for 'ha.initial_hosts'" );

        // when
        Config.embeddedDefaults(
                stringMap( ClusterSettings.mode.name(), mode.name(),
                        ClusterSettings.server_id.name(), "1",
                        ClusterSettings.initial_hosts.name(), "," ),
                Collections.singleton( new HaConfigurationValidator() ) );
    }
}
