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
package org.neo5j.cypher.internal.compiler.v3_2.planner.logical.steps

import org.neo5j.cypher.internal.compiler.v3_2.IndexDescriptor
import org.neo5j.cypher.internal.compiler.v3_2.commands.QueryExpression
import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.LogicalPlanningContext
import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.plans.LogicalPlan
import org.neo5j.cypher.internal.frontend.v3_2.ast.{Expression, LabelToken, PropertyKeyToken, UsingIndexHint}
import org.neo5j.cypher.internal.ir.v3_2.IdName

object uniqueIndexSeekLeafPlanner extends AbstractIndexSeekLeafPlanner {
  protected def constructPlan(idName: IdName,
                              label: LabelToken,
                              propertyKeys: Seq[PropertyKeyToken],
                              valueExpr: QueryExpression[Expression],
                              hint: Option[UsingIndexHint],
                              argumentIds: Set[IdName])
                             (implicit context: LogicalPlanningContext): (Seq[Expression]) => LogicalPlan =
    (predicates: Seq[Expression]) =>
      context.logicalPlanProducer.planNodeUniqueIndexSeek(idName, label, propertyKeys, valueExpr, predicates, hint, argumentIds)

  protected def findIndexesForLabel(labelId: Int)(implicit context: LogicalPlanningContext): Iterator[IndexDescriptor] =
    context.planContext.uniqueIndexesGetForLabel(labelId)

  protected def findIndexesFor(label: String, properties: Seq[String])(implicit context: LogicalPlanningContext): Option[IndexDescriptor] =
    context.planContext.uniqueIndexGet(label, properties)
}
