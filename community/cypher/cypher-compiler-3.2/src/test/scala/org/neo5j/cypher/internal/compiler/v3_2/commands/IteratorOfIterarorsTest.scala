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

import org.neo5j.cypher.internal.frontend.v3_2.test_helpers.CypherFunSuite

class IteratorOfIterarorsTest extends CypherFunSuite {
  test("single inner iterator") {
    val r = new IteratorOfIterarors[Int](Seq(Iterator(1, 2, 3)))
    r.toList should equal(List(1, 2, 3))
  }

  test("two inner non-empty iterators") {
    val r = new IteratorOfIterarors[Int](Seq(Iterator(1, 2, 3), Iterator(4, 5, 6)))
    r.toList should equal(List(1, 2, 3, 4, 5, 6))
  }

  test("two empty inner iterators") {
    val r = new IteratorOfIterarors[Int](Seq(Iterator.empty, Iterator.empty))
    r.toList should equal(List.empty)
  }

  test("non-empty iterators with an empty one") {
    val r = new IteratorOfIterarors[Int](Seq(Iterator(1, 2, 3), Iterator.empty, Iterator(4, 5, 6)))
    r.toList should equal(List(1, 2, 3, 4, 5, 6))
  }
}
