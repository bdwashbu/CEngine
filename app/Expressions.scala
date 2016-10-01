package app.astViewer

import org.eclipse.cdt.core.dom.ast._

import scala.collection.mutable.{ ListBuffer, Stack }
import scala.util.control.Exception.allCatch
import java.util.Formatter;
import java.util.Locale;
import java.math.BigInteger
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind._

object Expressions {

  def parse(expr: IASTExpression, direction: Direction, context: State, stack: State): Seq[IASTNode] = expr match {
    case cast: IASTCastExpression => // TODO
      if (direction == Entering) {
        println(cast.getRawSignature)
        Seq(cast.getOperand)
      } else {
        println(cast.getTypeId.getClass.getSimpleName)
        Seq()
      }
    case fieldRef: IASTFieldReference =>
      if (direction == Entering) {
        Seq(fieldRef.getFieldOwner)
      } else {
        val owner = context.stack.pop.asInstanceOf[VarRef]
        
        // add mangled name back onto the stack
        context.stack.push(VarRef(owner.name + "_" + fieldRef.getFieldName.getRawSignature))        
        Seq()
      }
    case subscript: IASTArraySubscriptExpression =>
      if (direction == Entering) {
        Seq(subscript.getArrayExpression, subscript.getArgument)
      } else {

        // index is first on the stack
        val index = context.stack.pop match {
          case VarRef(indexVarName) =>
            context.vars.resolveId(indexVarName).value.asInstanceOf[Int]
          case lit @ Literal(_) => lit.cast.asInstanceOf[Int]
          case x: Int =>
            x
        }

        val name = context.stack.pop
        val arrayVarPtr = context.vars.resolveId(name.asInstanceOf[VarRef].name)
        val arrayAddress = arrayVarPtr.value.asInstanceOf[Int]

        val ancestors = Utils.getAncestors(subscript)
        
        // We have to treat the destination op differently in an assignment
        val isParsingAssignmentDest = ancestors.find{ _.isInstanceOf[IASTBinaryExpression]}.map { binary =>
          val bin = binary.asInstanceOf[IASTBinaryExpression]
          bin.getOperator == IASTBinaryExpression.op_assign
        }.getOrElse(false)
        
        val elementAddress = Address(arrayAddress) + index * TypeHelper.sizeof(arrayVarPtr.theType)

        if (isParsingAssignmentDest) {
          context.stack.push(AddressInfo(elementAddress, TypeHelper.resolve(arrayVarPtr.theType)))
        } else {
          context.stack.push(stack.readVal(elementAddress.value, TypeHelper.resolve(arrayVarPtr.theType)))
        }

        Seq()
      }
    case unary: IASTUnaryExpression =>
      import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression._

      def resolveVar(variable: Any, func: (Int, IBasicType) => Unit) = {
        variable match {
          case VarRef(name) =>
            val variable = context.vars.resolveId(name)

            if (variable.isPointer) {
              func(variable.value.asInstanceOf[Int], TypeHelper.resolve(variable.theType))
            } else {
              func(variable.address.value, TypeHelper.resolve(variable.theType))
            }
          case AddressInfo(addy, theType) => func(addy.value, TypeHelper.resolve(theType))
          case int: Int          => int
        }
      }

      if (direction == Entering) {
        Seq(unary.getOperand)
      } else {
        unary.getOperator match {
          case `op_minus` =>

            
            def negativeResolver(variable: Variable) = {
              
              val info = AddressInfo(variable.address, variable.theType)
              
              resolveVar(info, (address, theType) => {
                val basicType = theType.asInstanceOf[IBasicType]
                context.stack.push(basicType.getKind match {
                  case `eInt`    => -stack.readVal(address).asInstanceOf[Int]
                  case `eDouble` => -stack.readVal(address).asInstanceOf[Double]
                })
            })}

            val cast = context.stack.pop match {
              case lit @ Literal(_) => lit.cast
              case x            => x
            }

            cast match {
              case int: Int     => context.stack.push(-int)
              case doub: Double => context.stack.push(-doub)
              //case variable @ Variable(_) => negativeResolver(variable)
              case VarRef(name) =>
                val variable = context.vars.resolveId(name)
                negativeResolver(variable)
            }
          case `op_postFixIncr` =>
            resolveVar(context.stack.pop, (address, theType) => {
              val currentVal = stack.readVal(address)
              context.stack.push(currentVal)
              stack.setValue(currentVal.asInstanceOf[Int] + 1, Address(address))
            })
          case `op_postFixDecr` =>
            resolveVar(context.stack.pop, (address, theType) => {
              val currentVal = stack.readVal(address)
              context.stack.push(currentVal)
              stack.setValue(currentVal.asInstanceOf[Int] - 1, Address(address))
            })
          case `op_prefixIncr` =>
            resolveVar(context.stack.pop, (address, theType) => {
              val currentVal = stack.readVal(address)
              stack.setValue(currentVal.asInstanceOf[Int] + 1, Address(address))
              context.stack.push(stack.readVal(address))
            })
          case `op_prefixDecr` =>
            resolveVar(context.stack.pop, (address, theType) => {
              val currentVal = stack.readVal(address)
              stack.setValue(currentVal.asInstanceOf[Int] - 1, Address(address))
              context.stack.push(stack.readVal(address))
            })
          case `op_sizeof` =>
            context.stack.pop match {
              case VarRef(name) =>
                context.stack.push(context.vars.resolveId(name).sizeof)
            }
          case `op_amper` =>
            context.stack.pop match {
              case VarRef(name) =>
                val variable = context.vars.resolveId(name)
                val info = AddressInfo(variable.address, variable.theType)
                context.stack.push(info)
            }
          case `op_star` =>
            context.stack.pop match {
              case VarRef(varName) =>
                val ptr = context.vars.resolveId(varName)
                val refAddressInfo = AddressInfo(Address(ptr.value.asInstanceOf[Int]), TypeHelper.resolve(ptr.theType))
                context.stack.push(refAddressInfo)
            }
          case `op_bracketedPrimary` => // not sure what this is for
        }
        Seq()
      }
    case lit: IASTLiteralExpression =>
      if (direction == Exiting) {
        //println("PUSHING LIT: " + castLiteral(lit))

        context.stack.push(Literal(lit.getRawSignature))

      }
      Seq()
    case id: IASTIdExpression =>
      if (direction == Exiting) {
        //println("PUSHING ID: " + id.getName.getRawSignature)
        context.stack.push(VarRef(id.getName.getRawSignature))
      }
      Seq()
    case typeExpr: IASTTypeIdExpression =>
      // used for sizeof calls on a type
      if (direction == Entering) {
        Seq(typeExpr.getTypeId)
      } else {
        Seq()
      }
    case call: IASTFunctionCallExpression =>
      FunctionCallExpr.parse(call, direction, context, stack)
    case bin: IASTBinaryExpression =>
      if (direction == Exiting) {

        if (context.vars.visited.contains(bin.getOperand2)) {
          val result = bin.getOperator match {
            case IASTBinaryExpression.op_assign =>
              var op2: Any = context.stack.pop
              var op1: Any = context.stack.pop
              BinaryExpr.parseAssign(op1, op2, context, stack)
            case _ => BinaryExpr.parse(bin, context, stack)
          }

          if (result != null) {
            context.stack.push(result)
          }
          Seq()
        } else {
          Seq(bin.getOperand2, bin)
        }
      } else {
        Seq(bin.getOperand1)
      }
  }
}