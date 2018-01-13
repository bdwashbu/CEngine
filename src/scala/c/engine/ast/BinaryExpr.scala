package scala.c.engine
package ast

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression._
import org.eclipse.cdt.core.dom.ast._
import org.eclipse.cdt.internal.core.dom.parser.c.CBasicType
import IBasicType.Kind._

object BinaryExpr {
  
  def parseAssign(node: IASTNode, op: Int, dst: LValue, op2: ValueType)(implicit state: State): LValue = {

    val result = evaluate(node, dst, op2, op)

    val casted = TypeHelper.cast(dst.theType, result.value).value
    dst.setValue(casted)

    dst
  }

  def evaluate(node: IASTNode, x: ValueType, y: ValueType, operator: Int)(implicit state: State): RValue = {

    val right = y match {
      case info @ LValue(_, ptr: IArrayType) => RValue(info.address, TypeHelper.pointerType)
      case info @ LValue(_, _) => info.value
      case value @ RValue(_, _) => value
      case StringLiteral(str) =>
        state.createStringVariable(str, true)
    }

    val left = x match {
      case info @ LValue(_, ptr: IArrayType) => RValue(info.address, TypeHelper.pointerType)
      case info @ LValue(_, _) => info.value
      case value @ RValue(_, _) => value
      case StringLiteral(str) =>
        state.createStringVariable(str, false)
    }

    val isLeftPointer = x.theType.isInstanceOf[IPointerType] || x.theType.isInstanceOf[IArrayType]
    val isRightPointer = y.theType.isInstanceOf[IPointerType] || y.theType.isInstanceOf[IArrayType]

    val initialRight = if (operator == `op_minus` || operator == `op_plus`) {
      if (isLeftPointer && !isRightPointer) {
        // increment by the size of the left arg
        right.value.asInstanceOf[Int] * TypeHelper.sizeof(TypeHelper.resolve(x.theType))
      } else if (isLeftPointer && isRightPointer) {
        // increment by the size of the left arg
        right.value.asInstanceOf[Int] / TypeHelper.sizeof(TypeHelper.resolve(x.theType))
      } else {
        right.value
      }
    } else {
      right.value
    }

    val initialLeft = if (operator == `op_minus` || operator == `op_plus`) {
      if (!isLeftPointer && isRightPointer) {
        // increment by the size of the left arg
        left.value.asInstanceOf[Int] * TypeHelper.sizeof(TypeHelper.resolve(y.theType))
      } else if (isLeftPointer && isRightPointer) {
        // increment by the size of the left arg
        left.value.asInstanceOf[Int] / TypeHelper.sizeof(TypeHelper.resolve(y.theType))
      } else {
        left.value
      }
    } else {
      left.value
    }

    // Because of integer promotion, C never does math on anything less than int's

    val op1 = initialLeft match {
      case theChar: char => theChar.toInt
      case theShort: short => theShort.toInt
      case x => x
    }

    val op2 = initialRight match {
      case theChar: char => theChar.toInt
      case theShort: short => theShort.toInt
      case x => x
    }

    val result: AnyVal = operator match {
      case `op_multiply` | `op_multiplyAssign` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x * y
          case (x: Int, y: Float) => x * y
          case (x: Int, y: Double) => x * y
          case (x: Int, y: Long) => x * y

          case (x: Float, y: Int) => x * y
          case (x: Float, y: Double) => x * y
          case (x: Float, y: Float) => x * y

          case (x: Double, y: Int) => x * y
          case (x: Double, y: Double) => x * y
          case (x: Double, y: Float) => x * y

          case (x: Long, y: Int) => x * y
          case (x: Long, y: Float) => x * y
          case (x: Long, y: Double) => x * y
          case (x: Long, y: Long) => x * y
        }
      case `op_plus` | `op_plusAssign` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x + y
          case (x: Int, y: Float) => x + y
          case (x: Int, y: Double) => x + y
          case (x: Int, y: Long) => x + y

          case (x: Float, y: Int) => x + y
          case (x: Float, y: Float) => x + y
          case (x: Float, y: Double) => x + y

          case (x: Double, y: Int) => x + y
          case (x: Double, y: Double) => x + y
          case (x: Double, y: Float) => x + y

          case (x: Long, y: Int) => x + y
          case (x: Long, y: Float) => x + y
          case (x: Long, y: Double) => x + y
          case (x: Long, y: Long) => x + y
        }
      case `op_minus` | `op_minusAssign` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x - y
          case (x: Int, y: Float) => x - y
          case (x: Int, y: Double) => x - y
          case (x: Int, y: Long) => x - y

          case (x: Float, y: Int) => x - y
          case (x: Float, y: Double) => x - y
          case (x: Float, y: Float) => x - y

          case (x: Double, y: Int) => x - y
          case (x: Double, y: Double) => x - y
          case (x: Double, y: Float) => x - y

          case (x: Long, y: Int) => x - y
          case (x: Long, y: Float) => x - y
          case (x: Long, y: Double) => x - y
          case (x: Long, y: Long) => x - y
        }
      case `op_divide` | `op_divideAssign` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x / y
          case (x: Int, y: Float) => x / y
          case (x: Int, y: Double) => x / y
          case (x: Int, y: Long) => x / y

          case (x: Float, y: Int) => x / y
          case (x: Float, y: Double) => x / y
          case (x: Float, y: Float) => x / y
          case (x: Float, y: Long) => x / y

          case (x: Double, y: Int) => x / y
          case (x: Double, y: Double) => x / y
          case (x: Double, y: Float) => x / y
          case (x: Double, y: Long) => x / y

          case (x: Long, y: Int) => x / y
          case (x: Long, y: Float) => x / y
          case (x: Long, y: Double) => x / y
          case (x: Long, y: Long) => x / y
        }
      case `op_shiftRight` | `op_shiftRightAssign` =>
        (op1, op2) match {
          case (x: Long, y: Int) => x >> y
          case (x: Int, y: Int) => x >> y
        }
      case `op_shiftLeft` | `op_shiftLeftAssign` =>
        (op1, op2) match {
          case (x: Long, y: Int) => x << y
          case (x: Int, y: Int) => x << y
        }
      case `op_equals` =>
        op1 == op2
      case `op_notequals` =>
        !evaluate(node, left, right, op_equals).value.asInstanceOf[Boolean]
      case `op_greaterThan` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x > y
          case (x: Int, y: Float) => x > y
          case (x: Int, y: Double) => x > y
          case (x: Int, y: Long) => x > y

          case (x: Float, y: Int) => x > y
          case (x: Float, y: Double) => x > y
          case (x: Float, y: Float) => x > y
          case (x: Float, y: Long) => x > y

          case (x: Double, y: Int) => x > y
          case (x: Double, y: Double) => x > y
          case (x: Double, y: Float) => x > y
          case (x: Double, y: Long) => x > y

          case (x: Long, y: Int) => x > y
          case (x: Long, y: Float) => x > y
          case (x: Long, y: Double) => x > y
          case (x: Long, y: Long) => x > y
        }
      case `op_greaterEqual` =>
        evaluate(node, left, right, op_greaterThan).value.asInstanceOf[Boolean] || evaluate(node, left, right, op_equals).value.asInstanceOf[Boolean]
      case `op_lessThan` =>
        !evaluate(node, left, right, op_greaterEqual).value.asInstanceOf[Boolean]
      case `op_lessEqual` =>
        !evaluate(node, left, right, op_greaterThan).value.asInstanceOf[Boolean]
      case `op_modulo` =>
        (op1, op2) match {
          case (x: Long, y: Long) => if (x % y >= 0) x % y else (x % y) + y
          case (x: Long, y: Int) => if (x % y >= 0) x % y else (x % y) + y
          case (x: Int, y: Int) => if (x % y >= 0) x % y else (x % y) + y
          case (x: Double, y: Int) => if (x % y >= 0) x % y else (x % y) + y
          case (x: Int, y: Double) => if (x % y >= 0) x % y else (x % y) + y
          case (x: Double, y: Double) => if (x % y >= 0) x % y else (x % y) + y
        } 
      case `op_binaryOr`  | `op_binaryOrAssign`=>
        (op1, op2) match {
          case (x: Int, y: Int) => x | y
          case (x: Int, y: Long) => x | y
          case (x: Long, y: Int) => x | y
          case (x: Long, y: Long) => x | y
        }  
      case `op_binaryXor` | `op_binaryXorAssign` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x ^ y
          case (x: Int, y: Long) => x ^ y
          case (x: Long, y: Int) => x ^ y
          case (x: Long, y: Long) => x ^ y
        }   
      case `op_binaryAnd` | `op_binaryAndAssign` =>
        (op1, op2) match {
          case (x: Int, y: Int) => x & y
          case (x: Int, y: Long) => x & y
          case (x: Long, y: Int) => x & y
          case (x: Long, y: Long) => x & y
        }
      case `op_logicalAnd` =>
        TypeHelper.resolveBoolean(op1) && TypeHelper.resolveBoolean(op2)
      case `op_logicalOr` =>
        TypeHelper.resolveBoolean(op1) || TypeHelper.resolveBoolean(op2)
      case `op_assign` =>
        op2
      case _ => throw new Exception("unhandled binary operator: " + operator); 0
    }

    if (Utils.isAssignment(operator)) {
      RValue(result, left.theType)
    } else {

      // offical conversion rules
      val resultType = (left.theType, right.theType) match {
        case (l: IBasicType, r: IBasicType) => (l.getKind, r.getKind) match {
          case (`eDouble`, _) if (l.isLong) => new CBasicType(eDouble, IBasicType.IS_LONG)
          case (_, `eDouble`) if (r.isLong) => new CBasicType(eDouble, IBasicType.IS_LONG)
          case (`eDouble`, _) => new CBasicType(eDouble, 0)
          case (_, `eDouble`) => new CBasicType(eDouble, 0)
          case (`eFloat`, _) => new CBasicType(eFloat, 0)
          case (_, `eFloat`) => new CBasicType(eFloat, 0)
          case (_, _) if (l.isLong && l.isUnsigned) || (r.isLong && r.isUnsigned) => new CBasicType(eInt, IBasicType.IS_UNSIGNED | IBasicType.IS_LONG)
          case (_, _) if (l.isLong || r.isLong) => new CBasicType(eInt, IBasicType.IS_LONG)
          case (_, _) if (l.isUnsigned || r.isUnsigned) => new CBasicType(eInt, IBasicType.IS_UNSIGNED)
          case _ => new CBasicType(eInt, 0)
        }
        case _ =>
          //println("ERROR: (" + left.theType + ", " + right.theType + ") " + node.getParent.getRawSignature)
          null
      }

      if (resultType != null) {
        RValue(result, resultType)
      } else {
        RValue(result, left.theType)
      }
    }
  }
}