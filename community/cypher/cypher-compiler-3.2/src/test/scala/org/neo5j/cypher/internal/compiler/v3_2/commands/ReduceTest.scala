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
package org.neo5j.cypher.internal.compiler.v3_2.commands

import org.neo5j.cypher.internal.compiler.v3_2._
import org.neo5j.cypher.internal.compiler.v3_2.commands.expressions._
import org.neo5j.cypher.internal.compiler.v3_2.pipes.QueryStateHelper
import org.neo5j.cypher.internal.frontend.v3_2.test_helpers.CypherFunSuite

class ReduceTest extends CypherFunSuite {

  test("canReturnSomethingFromAnIterable") {
    val l = Seq("x", "xxx", "xx")
    val expression = Add(Variable("acc"), LengthFunction(Variable("n")))
    val collection = Variable("l")
    val m = ExecutionContext.from("l" -> l)
    val s = QueryStateHelper.empty

    val reduce = ReduceFunction(collection, "n", expression, "acc", Literal(0))

    reduce.apply(m)(s) should equal(6)
  }

  test("returns_null_from_null_collection") {
    val expression = Add(Variable("acc"), LengthFunction(Variable("n")))
    val collection = Literal(null)
    val m = ExecutionContext.empty
    val s = QueryStateHelper.empty

    val reduce = ReduceFunction(collection, "n", expression, "acc", Literal(0))

    reduce(m)(s) should equal(null.asInstanceOf[Any])
  }
}
