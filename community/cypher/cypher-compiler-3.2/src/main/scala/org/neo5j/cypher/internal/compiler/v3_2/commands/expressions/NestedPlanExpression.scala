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
package org.neo5j.cypher.internal.compiler.v3_2.commands.expressions
import org.neo5j.cypher.internal.compiler.v3_2.ExecutionContext
import org.neo5j.cypher.internal.compiler.v3_2.pipes.QueryState
import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.plans.LogicalPlan

case class NestedPlanExpression(logicalPlan: LogicalPlan) extends Expression {
  override def rewrite(f: (Expression) => Expression): Expression = ???

  override def arguments: Seq[Expression] = Seq.empty

  override def apply(ctx: ExecutionContext)(implicit state: QueryState): Any = ???

  override def symbolTableDependencies: Set[String] = ???

  override def toString: String = {
    val planDescription = logicalPlan.flatten.map(_.getClass.getSimpleName).mkString("-")
    s"NestedPlanExpression($planDescription)"
  }
}
