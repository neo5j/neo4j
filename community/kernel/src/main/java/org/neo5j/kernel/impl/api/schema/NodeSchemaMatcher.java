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
package org.neo5j.kernel.impl.api.schema;

import java.util.Iterator;

import org.neo5j.collection.primitive.Primitive;
import org.neo5j.collection.primitive.PrimitiveIntSet;
import org.neo5j.function.ThrowingConsumer;
import org.neo5j.kernel.api.schema_new.LabelSchemaDescriptor;
import org.neo5j.kernel.api.schema_new.LabelSchemaSupplier;
import org.neo5j.kernel.impl.api.KernelStatement;
import org.neo5j.kernel.impl.api.operations.EntityReadOperations;
import org.neo5j.storageengine.api.NodeItem;

/**
 * This class holds functionality to match LabelSchemaDescriptors to nodes
 */
public class NodeSchemaMatcher
{
    private final EntityReadOperations readOps;

    public NodeSchemaMatcher( EntityReadOperations readOps )
    {
        this.readOps = readOps;
    }

    /**
     * Iterate over some schema suppliers, and invoke a callback for every supplier that matches the node. To match the
     * node N the supplier must supply a LabelSchemaDescriptor D, such that N has the label of D, and values for all
     * the properties of D.
     *
     * To avoid unnecessary store lookups, this implementation only gets propertyKeyIds for the node if some
     * descriptor has a valid label.
     *
     * @param state The current statement
     * @param schemaSuppliers The suppliers to match
     * @param node The node
     * @param specialPropertyId This property id will always count as a match for the descriptor, regardless of
     *                          whether the node has this property or not
     * @param callback The action to take on match
     * @param <SUPPLIER> the type to match. Must implement LabelSchemaDescriptor.Supplier
     * @param <EXCEPTION> The type of exception that can be thrown when taking the action
     * @throws EXCEPTION This exception is propagated from the action
     */
    public <SUPPLIER extends LabelSchemaSupplier,EXCEPTION extends Exception> void onMatchingSchema(
            KernelStatement state,
            Iterator<SUPPLIER> schemaSuppliers,
            NodeItem node,
            int specialPropertyId,
            ThrowingConsumer<SUPPLIER, EXCEPTION> callback
    ) throws EXCEPTION
    {
        PrimitiveIntSet nodePropertyIds = null;
        while ( schemaSuppliers.hasNext() )
        {
            SUPPLIER schemaSupplier = schemaSuppliers.next();
            LabelSchemaDescriptor schema = schemaSupplier.schema();
            if ( node.labels().contains( schema.getLabelId() ) )
            {
                if ( nodePropertyIds == null )
                {
                    nodePropertyIds = Primitive.intSet();
                    nodePropertyIds.addAll( readOps.nodeGetPropertyKeys( state, node ).iterator() );
                }

                if ( nodeHasSchemaProperties( nodePropertyIds, schema.getPropertyIds(), specialPropertyId ) )
                {
                    callback.accept( schemaSupplier );
                }
            }
        }
    }

    public static boolean nodeHasSchemaProperties(
            PrimitiveIntSet nodeProperties, int[] indexPropertyIds, int changedPropertyId )
    {
        for ( int indexPropertyId : indexPropertyIds )
        {
            if ( indexPropertyId != changedPropertyId && !nodeProperties.contains( indexPropertyId ) )
            {
                return false;
            }
        }
        return true;
    }
}
