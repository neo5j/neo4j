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
package org.neo5j.cypher.internal.compiled_runtime.v3_2.executionplan;

import java.util.Map;

import org.neo5j.cypher.internal.compiler.v3_2.ExecutionMode;
import org.neo5j.cypher.internal.compiler.v3_2.TaskCloser;
import org.neo5j.cypher.internal.compiled_runtime.v3_2.codegen.QueryExecutionTracer;
import org.neo5j.cypher.internal.compiler.v3_2.executionplan.Provider;
import org.neo5j.cypher.internal.compiler.v3_2.planDescription.InternalPlanDescription;
import org.neo5j.cypher.internal.compiler.v3_2.spi.QueryContext;

public interface GeneratedQuery
{
    GeneratedQueryExecution execute(
            TaskCloser closer,
            QueryContext queryContext,
            ExecutionMode executionMode,
            Provider<InternalPlanDescription> description,
            QueryExecutionTracer tracer,
            Map<String,Object> params );
}
