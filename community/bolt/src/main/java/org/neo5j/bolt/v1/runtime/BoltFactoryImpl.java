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

import java.time.Clock;
import java.time.Duration;

import org.neo5j.bolt.security.auth.Authentication;
import org.neo5j.graphdb.DependencyResolver;
import org.neo5j.graphdb.factory.GraphDatabaseSettings;
import org.neo5j.kernel.AvailabilityGuard;
import org.neo5j.kernel.GraphDatabaseQueryService;
import org.neo5j.kernel.api.bolt.BoltConnectionTracker;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.query.QueryExecutionEngine;
import org.neo5j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo5j.kernel.internal.GraphDatabaseAPI;
import org.neo5j.kernel.lifecycle.LifecycleAdapter;
import org.neo5j.udc.UsageData;

public class BoltFactoryImpl extends LifecycleAdapter implements BoltFactory
{
    private final GraphDatabaseAPI gds;
    private final UsageData usageData;
    private final LogService logging;
    private final Authentication authentication;
    private final BoltConnectionTracker connectionTracker;
    private final ThreadToStatementContextBridge txBridge;
    private final Config config;

    private QueryExecutionEngine queryExecutionEngine;
    private GraphDatabaseQueryService queryService;
    private TransactionIdStore transactionIdStore;
    private AvailabilityGuard availabilityGuard;

    public BoltFactoryImpl( GraphDatabaseAPI gds, UsageData usageData, LogService logging,
            ThreadToStatementContextBridge txBridge, Authentication authentication,
            BoltConnectionTracker connectionTracker, Config config )
    {
        this.gds = gds;
        this.usageData = usageData;
        this.logging = logging;
        this.txBridge = txBridge;
        this.authentication = authentication;
        this.connectionTracker = connectionTracker;
        this.config = config;
    }

    @Override
    public void start() throws Throwable
    {
        DependencyResolver dependencyResolver = gds.getDependencyResolver();
        queryExecutionEngine = dependencyResolver.resolveDependency( QueryExecutionEngine.class );
        queryService = dependencyResolver.resolveDependency( GraphDatabaseQueryService.class );
        transactionIdStore = dependencyResolver.resolveDependency( TransactionIdStore.class );
        availabilityGuard = dependencyResolver.resolveDependency( AvailabilityGuard.class );
    }

    @Override
    public void stop() throws Throwable
    {
        queryExecutionEngine = null;
        queryService = null;
        transactionIdStore = null;
        availabilityGuard = null;
    }

    @Override
    public BoltStateMachine newMachine( BoltConnectionDescriptor connectionDescriptor, Runnable onClose, Clock clock )
    {
        TransactionStateMachine.SPI transactionSPI = createTxSpi( clock );
        BoltStateMachine.SPI boltSPI = new BoltStateMachineSPI( connectionDescriptor, usageData,
                logging, authentication, connectionTracker, transactionSPI );
        return new BoltStateMachine( boltSPI, onClose, clock );
    }

    private TransactionStateMachine.SPI createTxSpi( Clock clock )
    {
        long bookmarkReadyTimeout = config.get( GraphDatabaseSettings.bookmark_ready_timeout );
        Duration txAwaitDuration = Duration.ofMillis( bookmarkReadyTimeout );

        return new TransactionStateMachineSPI( gds, txBridge, queryExecutionEngine, transactionIdStore,
                availabilityGuard, queryService, txAwaitDuration, clock );
    }
}
