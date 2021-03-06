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
package org.neo5j.cypher.internal.compiler.v3_2.pipes

import java.util.UUID

import org.neo5j.collection.primitive.PrimitiveLongSet
import org.neo5j.cypher.internal.compiler.v3_2._
import org.neo5j.cypher.internal.compiler.v3_2.commands.expressions.PathValueBuilder
import org.neo5j.cypher.internal.compiler.v3_2.commands.predicates.{InCheckContainer, SingleThreadedLRUCache}
import org.neo5j.cypher.internal.compiler.v3_2.helpers.{IdentityTypeConverter, RuntimeTypeConverter}
import org.neo5j.cypher.internal.compiler.v3_2.spi.QueryContext
import org.neo5j.cypher.internal.frontend.v3_2.ParameterNotFoundException

import scala.collection.mutable

class QueryState(val query: QueryContext,
                 val resources: ExternalCSVResource,
                 val params: Map[String, Any],
                 val decorator: PipeDecorator = NullPipeDecorator,
                 val timeReader: TimeReader = new TimeReader,
                 var initialContext: Option[ExecutionContext] = None,
                 val queryId: AnyRef = UUID.randomUUID().toString,
                 val triadicState: mutable.Map[String, PrimitiveLongSet] = mutable.Map.empty,
                 val repeatableReads: mutable.Map[Pipe, Seq[ExecutionContext]] = mutable.Map.empty,
                 val typeConverter: RuntimeTypeConverter = IdentityTypeConverter,
                 val cachedIn: SingleThreadedLRUCache[Any, InCheckContainer] =
                   new SingleThreadedLRUCache(maxSize = 16)) {
  private var _pathValueBuilder: PathValueBuilder = _

  def createOrGetInitialContext(): ExecutionContext = initialContext.getOrElse(ExecutionContext.empty)

  def clearPathValueBuilder: PathValueBuilder = {
    if (_pathValueBuilder == null) {
      _pathValueBuilder = new PathValueBuilder()
    }
    _pathValueBuilder.clear()
  }

  def readTimeStamp(): Long = timeReader.getTime

  def getParam(key: String): Any =
    params.getOrElse(key, throw new ParameterNotFoundException("Expected a parameter named " + key))

  def getStatistics: InternalQueryStatistics = query.getOptStatistics.getOrElse(QueryState.defaultStatistics)

  def withDecorator(decorator: PipeDecorator) =
    new QueryState(query, resources, params, decorator, timeReader, initialContext, queryId, triadicState, repeatableReads, typeConverter, cachedIn)

  def withInitialContext(initialContext: ExecutionContext) =
    new QueryState(query, resources, params, decorator, timeReader, Some(initialContext), queryId, triadicState, repeatableReads, typeConverter, cachedIn)

  def withQueryContext(query: QueryContext) =
    new QueryState(query, resources, params, decorator, timeReader, initialContext, queryId, triadicState, repeatableReads, typeConverter, cachedIn)
}

object QueryState {
  val defaultStatistics = InternalQueryStatistics()
}

class TimeReader {
  lazy val getTime: Long = System.currentTimeMillis()
}
