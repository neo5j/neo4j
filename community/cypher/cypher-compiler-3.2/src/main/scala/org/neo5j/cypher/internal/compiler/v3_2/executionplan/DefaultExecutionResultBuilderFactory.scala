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
package org.neo5j.cypher.internal.compiler.v3_2.executionplan

import org.neo5j.cypher.internal.compiler.v3_2.helpers.RuntimeTypeConverter
import org.neo5j.cypher.internal.compiler.v3_2.pipes._
import org.neo5j.cypher.internal.compiler.v3_2.planDescription.{Id, InternalPlanDescription}
import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.LogicalPlan2PlanDescription
import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.plans.LogicalPlan
import org.neo5j.cypher.internal.compiler.v3_2.spi.{CSVResources, QueryContext}
import org.neo5j.cypher.internal.compiler.v3_2.{ExecutionMode, ExplainMode, _}
import org.neo5j.cypher.internal.frontend.v3_2.CypherException
import org.neo5j.cypher.internal.frontend.v3_2.phases.InternalNotificationLogger

import scala.collection.mutable

case class DefaultExecutionResultBuilderFactory(pipeInfo: PipeInfo,
                                                columns: List[String],
                                                typeConverter: RuntimeTypeConverter,
                                                logicalPlan: LogicalPlan,
                                                idMap: Map[LogicalPlan, Id]) extends ExecutionResultBuilderFactory {
  def create(): ExecutionResultBuilder =
    ExecutionWorkflowBuilder()

  case class ExecutionWorkflowBuilder() extends ExecutionResultBuilder {
    private val taskCloser = new TaskCloser
    private var externalResource: ExternalCSVResource = new CSVResources(taskCloser)
    private var maybeQueryContext: Option[QueryContext] = None
    private var pipeDecorator: PipeDecorator = NullPipeDecorator
    private var exceptionDecorator: CypherException => CypherException = identity

    def setQueryContext(context: QueryContext) {
      maybeQueryContext = Some(context)
    }

    def setLoadCsvPeriodicCommitObserver(batchRowCount: Long) {
      val observer = new LoadCsvPeriodicCommitObserver(batchRowCount, externalResource, queryContext)
      externalResource = observer
      setExceptionDecorator(observer)
    }

    def setPipeDecorator(newDecorator: PipeDecorator) {
      pipeDecorator = newDecorator
    }

    def setExceptionDecorator(newDecorator: CypherException => CypherException) {
      exceptionDecorator = newDecorator
    }

    def build(queryId: AnyRef, planType: ExecutionMode, params: Map[String, Any], notificationLogger: InternalNotificationLogger): InternalExecutionResult = {
      taskCloser.addTask(queryContext.transactionalContext.close)
      val state = new QueryState(queryContext, externalResource, params, pipeDecorator, queryId = queryId,
                                 triadicState = mutable.Map.empty, repeatableReads = mutable.Map.empty,
                                 typeConverter = typeConverter)
      try {
        try {
          createResults(state, planType, notificationLogger)
        }
        catch {
          case e: CypherException =>
            throw exceptionDecorator(e)
        }
      }
      catch {
        case (t: Throwable) =>
          taskCloser.close(success = false)
          throw t
      }
    }

    private def createResults(state: QueryState, planType: ExecutionMode, notificationLogger: InternalNotificationLogger): InternalExecutionResult = {
      val queryType: InternalQueryType = getQueryType
      val planDescription: InternalPlanDescription = LogicalPlan2PlanDescription(logicalPlan, idMap)
      if (planType == ExplainMode) {
        //close all statements
        taskCloser.close(success = true)
        ExplainExecutionResult(columns, planDescription, queryType, notificationLogger.notifications)
      } else {
        val results = pipeInfo.pipe.createResults(state)
        val resultIterator = buildResultIterator(results, pipeInfo.updating)
        val descriptor = buildDescriptor(planDescription, resultIterator.wasMaterialized)
        new PipeExecutionResult(resultIterator, columns, state, descriptor, planType, queryType)
      }
    }

    private def queryContext = maybeQueryContext.get

    private def buildResultIterator(results: Iterator[ExecutionContext], isUpdating: Boolean): ResultIterator = {
      val closingIterator = new ClosingIterator(results, taskCloser, exceptionDecorator)
      val resultIterator = if (isUpdating) closingIterator.toEager else closingIterator
      resultIterator
    }

    private def buildDescriptor(planDescription: InternalPlanDescription, isProfileReady: => Boolean): () => InternalPlanDescription =
      () => pipeDecorator.decorate(planDescription, isProfileReady)
  }

  private def getQueryType = {
    val queryType =
      if (pipeInfo.pipe.isInstanceOf[IndexOperationPipe] || pipeInfo.pipe.isInstanceOf[ConstraintOperationPipe])
        SCHEMA_WRITE
      else if (pipeInfo.updating) {
        if (columns.isEmpty)
          WRITE
        else
          READ_WRITE
      } else
        READ_ONLY
    queryType
  }
}
