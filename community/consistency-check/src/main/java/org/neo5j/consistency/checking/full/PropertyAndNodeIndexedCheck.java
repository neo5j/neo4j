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
package org.neo5j.consistency.checking.full;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.neo5j.collection.primitive.Primitive;
import org.neo5j.collection.primitive.PrimitiveIntObjectMap;
import org.neo5j.collection.primitive.PrimitiveIntSet;
import org.neo5j.collection.primitive.PrimitiveLongIterator;
import org.neo5j.consistency.checking.ChainCheck;
import org.neo5j.consistency.checking.CheckerEngine;
import org.neo5j.consistency.checking.RecordCheck;
import org.neo5j.consistency.checking.cache.CacheAccess;
import org.neo5j.consistency.checking.index.IndexAccessors;
import org.neo5j.consistency.report.ConsistencyReport;
import org.neo5j.consistency.store.RecordAccess;
import org.neo5j.kernel.api.exceptions.index.IndexNotApplicableKernelException;
import org.neo5j.kernel.api.schema_new.IndexQuery;
import org.neo5j.kernel.api.schema_new.LabelSchemaDescriptor;
import org.neo5j.kernel.impl.api.LookupFilter;
import org.neo5j.kernel.impl.store.record.IndexRule;
import org.neo5j.kernel.impl.store.record.NodeRecord;
import org.neo5j.kernel.impl.store.record.PropertyBlock;
import org.neo5j.kernel.impl.store.record.PropertyRecord;
import org.neo5j.kernel.impl.store.record.Record;
import org.neo5j.storageengine.api.schema.IndexReader;

import static java.lang.String.format;

/**
 * Checks nodes and how they're indexed in one go. Reports any found inconsistencies.
 */
public class PropertyAndNodeIndexedCheck implements RecordCheck<NodeRecord, ConsistencyReport.NodeConsistencyReport>
{
    private final IndexAccessors indexes;
    private final PropertyReader propertyReader;
    private final CacheAccess cacheAccess;

    public PropertyAndNodeIndexedCheck( IndexAccessors indexes, PropertyReader propertyReader, CacheAccess cacheAccess )
    {
        this.indexes = indexes;
        this.propertyReader = propertyReader;
        this.cacheAccess = cacheAccess;
    }

    @Override
    public void check( NodeRecord record,
                       CheckerEngine<NodeRecord, ConsistencyReport.NodeConsistencyReport> engine,
                       RecordAccess records )
    {
        Collection<PropertyRecord> properties = propertyReader.getPropertyRecordChain( record );
        cacheAccess.client().putPropertiesToCache(properties);
        if ( indexes != null )
        {
            matchIndexesToNode( record, engine, records, properties );
        }
        checkProperty( record, engine, properties );
    }

    /**
     * Matches indexes to a node. This implementation mirrors NodeSchemaMatcher.onMatchingSchema(...), but as all
     * accessor methods are different, a shared implementation was hard to achieve.
     */
    private void matchIndexesToNode(
            NodeRecord record,
            CheckerEngine<NodeRecord,
            ConsistencyReport.NodeConsistencyReport> engine,
            RecordAccess records,
            Collection<PropertyRecord> propertyRecs )
    {
        Set<Long> labels = NodeLabelReader.getListOfLabels( record, records, engine );
        PrimitiveIntObjectMap<PropertyBlock> nodePropertyMap = null;
        for ( IndexRule indexRule : indexes.rules() )
        {
            long labelId = indexRule.schema().getLabelId();
            if ( labels.contains( labelId ) )
            {
                if ( nodePropertyMap == null )
                {
                    nodePropertyMap = properties( propertyReader.propertyBlocks( propertyRecs ) );
                }

                int[] indexPropertyIds = indexRule.schema().getPropertyIds();
                if ( nodeHasSchemaProperties( nodePropertyMap, indexPropertyIds ) )
                {
                    Object[] values = getPropertyValues( nodePropertyMap, indexPropertyIds );
                    try ( IndexReader reader = indexes.accessorFor( indexRule ).newReader() )
                    {
                        long nodeId = record.getId();

                        if ( indexRule.canSupportUniqueConstraint() )
                        {
                            verifyNodeCorrectlyIndexedUniquely( nodeId, values, engine, indexRule, reader );
                        }
                        else
                        {
                            long count = reader.countIndexedNodes( nodeId, values );
                            reportIncorrectIndexCount( values, engine, indexRule, count );
                        }
                    }
                }
            }
        }
    }

    private void verifyNodeCorrectlyIndexedUniquely( long nodeId, Object[] propertyValues,
            CheckerEngine<NodeRecord,ConsistencyReport.NodeConsistencyReport> engine, IndexRule indexRule,
            IndexReader reader )
    {
        IndexQuery[] query = seek( indexRule.schema(), propertyValues );

        PrimitiveLongIterator indexedNodeIds = queryIndexOrEmpty( reader, query );

        long count = 0;
        while ( indexedNodeIds.hasNext() )
        {
            long indexedNodeId = indexedNodeIds.next();

            if ( nodeId == indexedNodeId )
            {
                count++;
            }
            else
            {
                engine.report().uniqueIndexNotUnique( indexRule, propertyValues, indexedNodeId );
            }
        }

        reportIncorrectIndexCount( propertyValues, engine, indexRule, count );
    }

    private void reportIncorrectIndexCount( Object[] propertyValues,
            CheckerEngine<NodeRecord,ConsistencyReport.NodeConsistencyReport> engine, IndexRule indexRule, long count )
    {
        if ( count == 0 )
        {
            engine.report().notIndexed( indexRule, propertyValues );
        }
        else if ( count != 1 )
        {
            engine.report().indexedMultipleTimes( indexRule, propertyValues, count );
        }
    }

    private void checkProperty( NodeRecord record,
            CheckerEngine<NodeRecord, ConsistencyReport.NodeConsistencyReport> engine,
            Collection<PropertyRecord> props )
    {
        if ( !Record.NO_NEXT_PROPERTY.is( record.getNextProp() ) )
        {
            PropertyRecord firstProp = props.iterator().next();
            if ( !Record.NO_PREVIOUS_PROPERTY.is( firstProp.getPrevProp() ) )
            {
                engine.report().propertyNotFirstInChain( firstProp );
            }

            PrimitiveIntSet keys = Primitive.intSet();
            for (PropertyRecord property : props)
            {
                if ( !property.inUse() )
                {
                    engine.report().propertyNotInUse( property );
                }
                else
                {
                    for ( int key : ChainCheck.keys( property ) )
                    {
                        if ( !keys.add( key ) )
                        {
                            engine.report().propertyKeyNotUniqueInChain();
                        }
                    }
                }
            }
        }
    }

    private Object[] getPropertyValues( PrimitiveIntObjectMap<PropertyBlock> propertyMap, int[] indexPropertyIds )
    {
        Object[] values = new Object[indexPropertyIds.length];
        for ( int i = 0; i < indexPropertyIds.length; i++ )
        {
            PropertyBlock propertyBlock = propertyMap.get( indexPropertyIds[i] );
            values[i] = propertyReader.propertyValue( propertyBlock ).value();
        }
        return values;
    }

    private PrimitiveIntObjectMap<PropertyBlock> properties( List<PropertyBlock> propertyBlocks )
    {
        PrimitiveIntObjectMap<PropertyBlock> propertyIds = Primitive.intObjectMap();
        for ( PropertyBlock propertyBlock : propertyBlocks )
        {
            propertyIds.put( propertyBlock.getKeyIndexId(), propertyBlock );
        }
        return propertyIds;
    }

    private IndexQuery[] seek( LabelSchemaDescriptor schema, Object[] propertyValues )
    {
        assert schema.getPropertyIds().length == propertyValues.length;
        IndexQuery[] query = new IndexQuery[propertyValues.length];
        for ( int i = 0; i < query.length; i++ )
        {
            query[i] = IndexQuery.exact( schema.getPropertyIds()[i], propertyValues[i] );
        }
        return query;
    }

    private PrimitiveLongIterator queryIndexOrEmpty( IndexReader reader, IndexQuery[] query )
    {
        PrimitiveLongIterator indexedNodeIds;
        try
        {
            indexedNodeIds = reader.query( query );
        }
        catch ( IndexNotApplicableKernelException e )
        {
            throw new RuntimeException( format(
                    "Consistency checking error: index provider does not support exact query %s",
                    Arrays.toString( query ) ), e );
        }

        indexedNodeIds = LookupFilter.exactIndexMatches( propertyReader, indexedNodeIds, query );
        return indexedNodeIds;
    }

    private static boolean nodeHasSchemaProperties(
            PrimitiveIntObjectMap<PropertyBlock> nodePropertyMap, int[] indexPropertyIds )
    {
        for ( int indexPropertyId : indexPropertyIds )
        {
            if ( !nodePropertyMap.containsKey( indexPropertyId ) )
            {
                return false;
            }
        }
        return true;
    }
}
