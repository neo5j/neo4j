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
package org.neo5j.metrics;

import com.codahale.metrics.MetricRegistry;

import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.spi.KernelContext;
import org.neo5j.kernel.lifecycle.LifeSupport;
import org.neo5j.kernel.lifecycle.Lifecycle;
import org.neo5j.logging.Log;
import org.neo5j.metrics.output.CompositeEventReporter;
import org.neo5j.metrics.output.EventReporterBuilder;
import org.neo5j.metrics.source.Neo5jMetricsBuilder;

public class MetricsExtension implements Lifecycle
{
    private final LifeSupport life = new LifeSupport();
    private Log logger;
    private CompositeEventReporter reporter;
    private boolean metricsBuilt;

    MetricsExtension( KernelContext kernelContext, MetricsKernelExtensionFactory.Dependencies dependencies )
    {
        LogService logService = dependencies.logService();
        Config configuration = dependencies.configuration();
        logger = logService.getUserLog( getClass() );

        MetricRegistry registry = new MetricRegistry();
        reporter = new EventReporterBuilder( configuration, registry, logger, kernelContext, life ).build();
        metricsBuilt = new Neo5jMetricsBuilder( registry, reporter, configuration, logService, kernelContext,
                                                dependencies, life ).build();
    }

    @Override
    public void init()
    {
        logger.info( "Initiating metrics..." );
        if ( metricsBuilt && reporter.isEmpty() )
        {
            logger.warn( "Several metrics were enabled but no exporting option was configured to report values to. " +
                         "Disabling kernel metrics extension." );
            life.clear();
        }

        if ( !reporter.isEmpty() && !metricsBuilt )
        {
            logger.warn( "Exporting tool have been configured to report values to but no metrics were enabled. " +
                         "Disabling kernel metrics extension." );
            life.clear();
        }

        life.init();
    }

    @Override
    public void start()
    {
        life.start();
    }

    @Override
    public void stop()
    {
        life.stop();
    }

    @Override
    public void shutdown()
    {
        life.shutdown();
    }
}
