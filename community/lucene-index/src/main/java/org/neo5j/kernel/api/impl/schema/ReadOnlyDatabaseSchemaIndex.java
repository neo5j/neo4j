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
package org.neo5j.kernel.api.impl.schema;

import java.io.IOException;
import java.util.List;

import org.neo5j.kernel.api.exceptions.index.IndexEntryConflictException;
import org.neo5j.kernel.api.impl.index.ReadOnlyAbstractDatabaseIndex;
import org.neo5j.kernel.api.impl.index.partition.ReadOnlyIndexPartitionFactory;
import org.neo5j.kernel.api.impl.index.storage.PartitionedIndexStorage;
import org.neo5j.kernel.api.impl.schema.writer.LuceneIndexWriter;
import org.neo5j.kernel.api.index.PropertyAccessor;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptor;
import org.neo5j.kernel.impl.api.index.sampling.IndexSamplingConfig;
import org.neo5j.storageengine.api.schema.IndexReader;

/**
 * Read only schema index
 */
public class ReadOnlyDatabaseSchemaIndex extends ReadOnlyAbstractDatabaseIndex<LuceneSchemaIndex> implements SchemaIndex
{
    public ReadOnlyDatabaseSchemaIndex( PartitionedIndexStorage indexStorage, NewIndexDescriptor descriptor,
            IndexSamplingConfig samplingConfig, ReadOnlyIndexPartitionFactory readOnlyIndexPartitionFactory )
    {
        super( new LuceneSchemaIndex( indexStorage, descriptor, samplingConfig, readOnlyIndexPartitionFactory ) );
    }

    @Override
    public LuceneIndexWriter getIndexWriter() throws IOException
    {
        throw new UnsupportedOperationException( "Can't get index writer for read only lucene index." );
    }

    @Override
    public IndexReader getIndexReader() throws IOException
    {
        return luceneIndex.getIndexReader();
    }

    @Override
    public NewIndexDescriptor getDescriptor()
    {
        return luceneIndex.getDescriptor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verifyUniqueness( PropertyAccessor accessor, int[] propertyKeyIds )
            throws IOException, IndexEntryConflictException
    {
        luceneIndex.verifyUniqueness( accessor, propertyKeyIds );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verifyUniqueness( PropertyAccessor accessor, int[] propertyKeyIds, List<Object> updatedPropertyValues )
            throws IOException, IndexEntryConflictException
    {
        luceneIndex.verifyUniqueness( accessor, propertyKeyIds, updatedPropertyValues );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnline() throws IOException
    {
        return luceneIndex.isOnline();
    }

    /**
     * Unsupported operation in read only index.
     */
    @Override
    public void markAsOnline() throws IOException
    {
        throw new UnsupportedOperationException( "Can't mark read only index." );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markAsFailed( String failure ) throws IOException
    {
        luceneIndex.markAsFailed( failure );
    }
}
