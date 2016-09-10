package app.astViewer

import org.eclipse.cdt.core.dom.ast._

import scala.collection.mutable.{ListBuffer, Stack}
import scala.util.control.Exception.allCatch
import java.util.Formatter;
import java.util.Locale;

object BinaryExpr {
  
  def parseAssign(op1: Any, op2: Any, context: State, stack: State#VarStack): Any = {
    val destinationAddress: Address = op1 match {
      case VarRef(name) =>
        context.vars.resolveId(name).address
      case addy @ Address(_) => addy
    }
    
    val resolvedop2 = op2 match {
      case VarRef(name) => 
        context.vars.resolveId(name).value
      case Address(address) => 
        op1 match {
          case Address(_) => stack.readVal(address)
          case VarRef(name) => 
            if (!context.vars.resolveId(name).isPointer) {
              // only if op1 is NOT a pointer, resolve op2
              stack.readVal(address)
            } else {
              address
            }
        }  
      case int: Int => int
      case doub: Double => doub
    }
    
    stack.setValue(resolvedop2, destinationAddress)

    resolvedop2
  }
  
  def parse(binaryExpr: IASTBinaryExpression, context: State, stack: State#VarStack): Any = {
    import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression._

    // read the two operands from right to left
    var op2: Any = context.stack.pop
    var op1: Any = context.stack.pop
    
    var isOp1Pointer = false
    
    def resolve(op: Any) = op match {
      case VarRef(name) => 
        val theVar = context.vars.resolveId(name)
        if (theVar.isPointer) {
          isOp1Pointer = true
          Address(theVar.value.asInstanceOf[Int])
        } else {
          theVar.value  
        }
      case Address(addy) => stack.readVal(addy)
      case int: Int => int
      case bool: Boolean => bool
      case double: Double => double
    }

    val op = binaryExpr.getOperator
    
    // resolve Op1 only if not assignment
    
    if (op != op_plusAssign &&
        op != op_minusAssign) {
      op1 = resolve(op1)
      op2 = resolve(op2)
    }
    
    val result: Any = binaryExpr.getOperator match {
      case `op_multiply` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x * y
          case (x: Double, y: Int) => x * y
          case (x: Int, y: Double) => x * y
          case (x: Double, y: Double) => x * y
        }
      case `op_plus` =>
        (op1, op2) match {
          case (addy @ Address(address), y: Int) => address + y * TypeHelper.sizeof(stack.getType(addy))
          case (x: Int, y: Int) => x + y
          case (x: Double, y: Int) => x + y
          case (x: Int, y: Double) => x + y
          case (x: Double, y: Double) => x + y
        }
      case `op_minus` =>
        (op1, op2) match {
          case (addy @ Address(address), y: Int) => address - y * TypeHelper.sizeof(stack.getType(addy))
          case (x: Int, y: Int) => x - y
          case (x: Double, y: Int) => x - y
          case (x: Int, y: Double) => x - y
          case (x: Double, y: Double) => x - y
        }
      case `op_divide` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x / y
          case (x: Double, y: Int) => x / y
          case (x: Int, y: Double) => x / y
          case (x: Double, y: Double) => x / y
        }
      //case `op_assign` =>
      //  parseAssign(op1, op2, context)
      case `op_equals` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x == y
          case (x: Double, y: Int) => x == y
          case (x: Int, y: Double) => x == y
          case (x: Double, y: Double) => x == y
        }
      case `op_notequals` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x != y
          case (x: Double, y: Int) => x != y
          case (x: Int, y: Double) => x != y
          case (x: Double, y: Double) => x != y
        }
      case `op_greaterThan` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x > y
          case (x: Double, y: Int) => x > y
          case (x: Int, y: Double) => x > y
          case (x: Double, y: Double) => x > y
        }
      case `op_lessThan` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x < y
          case (x: Double, y: Int) => x < y
          case (x: Int, y: Double) => x < y
          case (x: Double, y: Double) => x < y
        }
      case `op_modulo` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x % y
          case (x: Double, y: Int) => x % y
          case (x: Int, y: Double) => x % y
          case (x: Double, y: Double) => x % y
        } 
      case `op_binaryOr` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x | y
        }  
      case `op_plusAssign` =>
        op1 match {
          case VarRef(name) => 
            val vari = context.vars.resolveId(name)
            vari.setValue((vari.value, op2) match {
              case (x: Int, y: Int) => x + y
              case (x: Double, y: Int) => x + y
              case (x: Int, y: Double) => x + y
              case (x: Double, y: Double) => x + y
            })
            
        }
      case `op_minusAssign` =>
        op1 match {
          case VarRef(name) => 
            val vari = context.vars.resolveId(name)
            vari.setValue((vari.value, op2) match {
              case (x: Int, y: Int) => x - y
              case (x: Double, y: Int) => x - y
              case (x: Int, y: Double) => x - y
              case (x: Double, y: Double) => x - y
            })
        }
      case `op_logicalAnd` =>
        (op1, op2) match {
          case (x: Boolean, y: Boolean) => x && y
        }
      case `op_logicalOr` =>
        (op1, op2) match {
          case (x: Boolean, y: Boolean) => x || y
        }
      case _ => throw new Exception("unhandled binary operator: " + binaryExpr.getOperator); null
    }
    
    if (isOp1Pointer) {
      Address(result.asInstanceOf[Int])
    } else {
      result
    }
    
  }
}