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
package org.neo5j.cypher.internal.compiled_runtime.v3_2.codegen.ir.expressions

import org.neo5j.cypher.internal.compiled_runtime.v3_2.codegen.spi.MethodStructure
import org.neo5j.cypher.internal.compiled_runtime.v3_2.codegen.{CodeGenContext, Variable}
import org.neo5j.cypher.internal.frontend.v3_2.symbols
import org.neo5j.cypher.internal.frontend.v3_2.symbols._

case class RelationshipProjection(relId: Variable) extends CodeGenExpression {
  assert(relId.codeGenType.asInstanceOf[CypherCodeGenType].ct == symbols.CTRelationship)

  override def init[E](generator: MethodStructure[E])(implicit context: CodeGenContext) = {}

  override def generateExpression[E](structure: MethodStructure[E])(implicit context: CodeGenContext) ={
    if (relId.nullable)
      structure.nullableReference(relId.name, CodeGenType.primitiveRel,
        structure.materializeRelationship(relId.name, relId.codeGenType))
    else
      structure.materializeRelationship(relId.name, relId.codeGenType)
  }

  override def nullable(implicit context: CodeGenContext) = relId.nullable

  override def codeGenType(implicit context: CodeGenContext) = CypherCodeGenType(CTRelationship, ReferenceType)
}
