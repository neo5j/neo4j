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
package org.neo5j.cypher.internal.compiler.v3_2.planner.logical

import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.plans.LogicalPlan
import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.steps._
import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.steps.solveOptionalMatches.OptionalSolver
import org.neo5j.cypher.internal.compiler.v3_2.{UpdateStrategy, defaultUpdateStrategy}
import org.neo5j.cypher.internal.ir.v3_2.QueryGraph

object QueryPlannerConfiguration {

  private val leafPlanFromExpressions: IndexedSeq[LeafPlanner with LeafPlanFromExpressions] = IndexedSeq(
    // MATCH (n) WHERE id(n) IN ... RETURN n
    idSeekLeafPlanner,

    // MATCH (n) WHERE n.prop IN ... RETURN n
    uniqueIndexSeekLeafPlanner,

    // MATCH (n) WHERE n.prop IN ... RETURN n
    indexSeekLeafPlanner,

    // MATCH (n) WHERE has(n.prop) RETURN n
    // MATCH (n:Person) WHERE n.prop CONTAINS ...
    indexScanLeafPlanner,

    // MATCH (n:Person) RETURN n
    labelScanLeafPlanner
  )

  val allLeafPlanners = leafPlanFromExpressions ++ IndexedSeq(
    argumentLeafPlanner,

    // MATCH (n) RETURN n
    allNodesLeafPlanner,

    // Legacy indices
    legacyHintLeafPlanner,

    // Handles OR between other leaf planners
    OrLeafPlanner(leafPlanFromExpressions))


  val default: QueryPlannerConfiguration = QueryPlannerConfiguration(
    pickBestCandidate = pickBestPlanUsingHintsAndCost,
    applySelections = Selector(pickBestPlanUsingHintsAndCost,
      selectPatternPredicates,
      triadicSelectionFinder,
      selectCovered,
      selectHasLabelWithJoin
    ),
    optionalSolvers = Seq(
      applyOptional,
      outerHashJoin
    ),
    leafPlanners = LeafPlannerList(allLeafPlanners),
  updateStrategy = defaultUpdateStrategy
  )
}

case class QueryPlannerConfiguration(leafPlanners: LeafPlannerIterable,
                                     applySelections: PlanTransformer[QueryGraph],
                                     optionalSolvers: Seq[OptionalSolver],
                                     pickBestCandidate: LogicalPlanningFunction0[CandidateSelector],
                                     updateStrategy: UpdateStrategy) {

  def toKit()(implicit context: LogicalPlanningContext): QueryPlannerKit =
    QueryPlannerKit(
      select = (plan: LogicalPlan, qg: QueryGraph) => applySelections(plan, qg),
      pickBest = pickBestCandidate(context)
    )

  def withLeafPlanners(leafPlanners: LeafPlannerIterable) = copy(leafPlanners = leafPlanners)

  def withUpdateStrategy(updateStrategy: UpdateStrategy) = copy(updateStrategy = updateStrategy)
}

case class QueryPlannerKit(select: (LogicalPlan, QueryGraph) => LogicalPlan,

                           pickBest: CandidateSelector) {
  def select(plans: Iterable[Seq[LogicalPlan]], qg: QueryGraph): Iterable[Seq[LogicalPlan]] =
    plans.map(_.map(plan => select(plan, qg)))
}
