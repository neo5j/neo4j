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
package org.neo5j.cypher.internal.compiler.v3_2.helpers

import org.neo5j.cypher.internal.frontend.v3_2.ast.Statement
import org.neo5j.cypher.internal.frontend.v3_2.{Scope, SemanticCheckResult, SemanticState}
import org.scalatest.Assertions

object StatementHelper extends Assertions {

  implicit class RichStatement(ast: Statement) {
    def semanticState: SemanticState = ast.semanticCheck(SemanticState.clean) match {
      case SemanticCheckResult(state, errors) =>
        if (errors.isEmpty) {
          state
        } else
          fail(s"Failure during semantic checking of $ast with errors $errors")
    }

    def scope: Scope = semanticState.scopeTree
  }
}
