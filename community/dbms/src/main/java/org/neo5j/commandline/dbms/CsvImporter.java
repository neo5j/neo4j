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
package org.neo5j.commandline.dbms;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;

import org.neo5j.commandline.admin.IncorrectUsage;
import org.neo5j.commandline.admin.OutsideWorld;
import org.neo5j.dbms.DatabaseManagementSystemSettings;
import org.neo5j.graphdb.factory.GraphDatabaseSettings;
import org.neo5j.helpers.Args;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.tooling.ImportTool;
import org.neo5j.unsafe.impl.batchimport.Configuration;
import org.neo5j.unsafe.impl.batchimport.input.BadCollector;
import org.neo5j.unsafe.impl.batchimport.input.Collector;
import org.neo5j.unsafe.impl.batchimport.input.csv.CsvInput;
import org.neo5j.unsafe.impl.batchimport.input.csv.IdType;

import static java.nio.charset.Charset.defaultCharset;
import static org.neo5j.kernel.impl.util.Converters.withDefault;
import static org.neo5j.tooling.ImportTool.csvConfiguration;
import static org.neo5j.tooling.ImportTool.extractInputFiles;
import static org.neo5j.tooling.ImportTool.importConfiguration;
import static org.neo5j.tooling.ImportTool.nodeData;
import static org.neo5j.tooling.ImportTool.relationshipData;
import static org.neo5j.tooling.ImportTool.validateInputFiles;
import static org.neo5j.unsafe.impl.batchimport.input.Collectors.badCollector;
import static org.neo5j.unsafe.impl.batchimport.input.Collectors.collect;
import static org.neo5j.unsafe.impl.batchimport.input.csv.DataFactories.defaultFormatNodeFileHeader;
import static org.neo5j.unsafe.impl.batchimport.input.csv.DataFactories.defaultFormatRelationshipFileHeader;

class CsvImporter implements Importer
{
    private final Collection<Args.Option<File[]>> nodesFiles, relationshipsFiles;
    private final IdType idType;
    private final Charset inputEncoding;
    private final Config config;
    private final Args args;
    private final OutsideWorld outsideWorld;
    private final String reportFileName;
    private final boolean ignoreBadRelationships;
    private final boolean ignoreDuplicateNodes;
    private final boolean ignoreExtraColumns;

    CsvImporter( Args args, Config config, OutsideWorld outsideWorld ) throws IncorrectUsage
    {
        this.args = args;
        this.outsideWorld = outsideWorld;
        nodesFiles = extractInputFiles( args, "nodes", outsideWorld.errorStream() );
        relationshipsFiles = extractInputFiles( args, "relationships", outsideWorld.errorStream() );
        reportFileName =
                args.interpretOption( "report-file", withDefault( ImportCommand.DEFAULT_REPORT_FILE_NAME ), s -> s );
        ignoreExtraColumns = args.getBoolean( "ignore-extra-columns", false );
        ignoreDuplicateNodes = args.getBoolean( "ignore-duplicate-nodes", false );
        ignoreBadRelationships = args.getBoolean( "ignore-missing-nodes", false );
        try
        {
            validateInputFiles( nodesFiles, relationshipsFiles );
        }
        catch ( IllegalArgumentException e )
        {
            throw new IncorrectUsage( e.getMessage() );
        }

        idType = args.interpretOption( "id-type", withDefault( IdType.STRING ),
                from -> IdType.valueOf( from.toUpperCase() ) );
        inputEncoding = Charset.forName( args.get( "input-encoding", defaultCharset().name() ) );
        this.config = config;
    }

    @Override
    public void doImport() throws IOException
    {
        FileSystemAbstraction fs = outsideWorld.fileSystem();
        File storeDir = config.get( DatabaseManagementSystemSettings.database_path );
        File logsDir = config.get( GraphDatabaseSettings.logs_directory );
        File reportFile = new File( reportFileName );

        OutputStream badOutput = new BufferedOutputStream( fs.openAsOutputStream( reportFile, false ) );
        Collector badCollector = badCollector( badOutput, isIgnoringSomething() ? BadCollector.UNLIMITED_TOLERANCE : 0,
                collect( ignoreBadRelationships, ignoreDuplicateNodes, ignoreExtraColumns ) );

        Configuration configuration = importConfiguration( null, false, config );
        CsvInput input = new CsvInput( nodeData( inputEncoding, nodesFiles ), defaultFormatNodeFileHeader(),
                relationshipData( inputEncoding, relationshipsFiles ), defaultFormatRelationshipFileHeader(), idType,
                csvConfiguration( args, false ), badCollector, configuration.maxNumberOfProcessors() );

        ImportTool.doImport( outsideWorld.errorStream(), outsideWorld.errorStream(), storeDir, logsDir, reportFile, fs,
                nodesFiles, relationshipsFiles, false, input, config, badOutput, configuration );
    }

    private boolean isIgnoringSomething()
    {
        return ignoreBadRelationships || ignoreDuplicateNodes || ignoreExtraColumns;
    }
}
