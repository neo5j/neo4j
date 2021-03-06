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
package org.neo5j.bolt.v1.runtime;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.time.Clock;

import org.neo5j.bolt.security.auth.Authentication;
import org.neo5j.graphdb.DependencyResolver;
import org.neo5j.kernel.GraphDatabaseQueryService;
import org.neo5j.kernel.api.bolt.BoltConnectionTracker;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo5j.kernel.impl.logging.NullLogService;
import org.neo5j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo5j.kernel.internal.GraphDatabaseAPI;
import org.neo5j.test.OnDemandJobScheduler;
import org.neo5j.udc.UsageData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BoltFactoryImplTest
{
    private static final Clock CLOCK = Clock.systemUTC();
    private static final BoltConnectionDescriptor CONNECTION_DESCRIPTOR = new BoltConnectionDescriptor(
            new InetSocketAddress( "client", 7474 ),
            new InetSocketAddress( "server", 7475 ) );

    @Test
    public void newMachineThrowsWhenNotStarted()
    {
        BoltFactoryImpl boltFactory = newBoltFactory();

        try
        {
            boltFactory.newMachine( CONNECTION_DESCRIPTOR, mock( Runnable.class ), CLOCK );
            fail( "Exception expected" );
        }
        catch ( Exception e )
        {
            // expected
        }
    }

    @Test
    public void newMachineThrowsWhenStopped() throws Throwable
    {
        BoltFactoryImpl boltFactory = newBoltFactory();

        boltFactory.start();

        BoltStateMachine stateMachine = boltFactory.newMachine( CONNECTION_DESCRIPTOR, mock( Runnable.class ), CLOCK );

        assertNotNull( stateMachine );

        boltFactory.stop();

        try
        {
            boltFactory.newMachine( CONNECTION_DESCRIPTOR, mock( Runnable.class ), CLOCK );
            fail( "Exception expected" );
        }
        catch ( Exception e )
        {
            // expected
        }
    }

    @Test
    public void txIdStoreRefreshedAfterRestart() throws Throwable
    {
        GraphDatabaseAPI db = newDbMock();
        DependencyResolver dependencyResolver = db.getDependencyResolver();
        TransactionIdStore txIdStoreBeforeRestart = mock( TransactionIdStore.class );
        when( txIdStoreBeforeRestart.getLastClosedTransactionId() ).thenReturn( 42L );
        TransactionIdStore txIdStoreAfterRestart = mock( TransactionIdStore.class );
        when( txIdStoreAfterRestart.getLastClosedTransactionId() ).thenReturn( 4242L );
        when( dependencyResolver.resolveDependency( TransactionIdStore.class ) )
                .thenReturn( txIdStoreBeforeRestart ).thenReturn( txIdStoreAfterRestart );

        BoltFactoryImpl boltFactory = newBoltFactory( db );

        boltFactory.start();

        BoltStateMachine stateMachine1 = boltFactory.newMachine( CONNECTION_DESCRIPTOR, mock( Runnable.class ), CLOCK );
        assertEquals( 42, stateMachine1.spi.transactionSpi().newestEncounteredTxId() );

        boltFactory.stop();
        boltFactory.start();

        BoltStateMachine stateMachine2 = boltFactory.newMachine( CONNECTION_DESCRIPTOR, mock( Runnable.class ), CLOCK );
        assertEquals( 4242, stateMachine2.spi.transactionSpi().newestEncounteredTxId() );
    }

    private static BoltFactoryImpl newBoltFactory()
    {
        return newBoltFactory( newDbMock() );
    }

    private static BoltFactoryImpl newBoltFactory( GraphDatabaseAPI db )
    {
        return new BoltFactoryImpl( db, new UsageData( new OnDemandJobScheduler() ), NullLogService.getInstance(),
                new ThreadToStatementContextBridge(), mock( Authentication.class ), BoltConnectionTracker.NOOP,
                Config.empty() );
    }

    private static GraphDatabaseAPI newDbMock()
    {
        GraphDatabaseAPI db = mock( GraphDatabaseAPI.class );
        DependencyResolver dependencyResolver = mock( DependencyResolver.class );
        when( db.getDependencyResolver() ).thenReturn( dependencyResolver );
        GraphDatabaseQueryService queryService = mock( GraphDatabaseQueryService.class );
        when( queryService.getDependencyResolver() ).thenReturn( dependencyResolver );
        when( dependencyResolver.resolveDependency( GraphDatabaseQueryService.class ) ).thenReturn( queryService );
        return db;
    }
}
