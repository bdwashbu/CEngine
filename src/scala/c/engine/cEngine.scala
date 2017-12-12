package scala.c.engine

import org.eclipse.cdt.core.dom.ast.{IASTCaseStatement, IASTDeclarationStatement, IASTEqualsInitializer, _}

import scala.collection.mutable.Stack
import scala.collection.mutable.ListBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

import org.eclipse.cdt.internal.core.dom.parser.c._


object Interpreter {
  implicit class CounterSC(val sc: StringContext) extends AnyVal {

    // Define functions that we want to use with string interpolation syntax
    def c(args: Any*)(implicit state: State): Unit = {
      Gcc.runCode(sc.parts.iterator.next, state)
    }

    def func(args: Any*)(implicit state: State): Unit = {
      Gcc.runGlobalCode(sc.parts.iterator.next, state)
    }
  }
}

class Memory(size: Int) {

  import org.eclipse.cdt.core.dom.ast.IBasicType.Kind._

  var insertIndex = 0
  // turing tape
  val tape = ByteBuffer.allocate(size)
  tape.order(ByteOrder.LITTLE_ENDIAN)

  // use Address type to prevent messing up argument order
  def writeToMemory(newVal: AnyVal, address: Int, theType: IType): Unit = {

    TypeHelper.stripSyntheticTypeInfo(theType) match {
      case basic: IBasicType if basic.getKind == eInt && basic.isShort =>
          newVal match {
            case int: Int => tape.putShort(address, int.asInstanceOf[Short])
            case short: Short => tape.putShort(address, short)
          }
      case basic: IBasicType if basic.getKind == eInt && basic.isLongLong =>
        newVal match {
          case long: Long => tape.putLong(address, long)
        }
      case basic: IBasicType if basic.getKind == eInt && basic.isLong =>
        newVal match {
          case int: Int => tape.putInt(address, int)
          case long: Long => tape.putInt(address, long.toInt)
        }
      case basic: IBasicType if basic.getKind == eInt || basic.getKind == eVoid =>
        newVal match {
          case int: Int => tape.putInt(address, int)
          case long: Long => tape.putInt(address, long.toInt)
        }
      case basic: IBasicType if basic.getKind == eDouble =>
        newVal match {
          case int: Int => tape.putDouble(address, int.toDouble)
          case double: Double => tape.putDouble(address, double)
        }
      case basic: IBasicType if basic.getKind == eFloat =>
        newVal match {
          case float: Float => tape.putFloat(address, float)
        }
      case basic: IBasicType if basic.getKind == eChar =>
        newVal match {
          case char: char => tape.put(address, char)
          case int: Int => tape.put(address, int.toByte)
        }
      case basic: IBasicType if basic.getKind == eBoolean =>
        newVal match {
          case char: char => tape.put(address, char)
          case int: Int => tape.putInt(address, int)
          case bool: Boolean => tape.putInt(address, if (bool) 1 else 0)
        }
      case basic: IFunctionType =>
        newVal match {
          case int: Int => tape.putInt(address, int)
        }
      case basic: CStructure =>
        newVal match {
          case int: Int => tape.putInt(address, int)
        }
      case ptr: IPointerType =>
        newVal match {
          case int: Int => tape.putInt(address, int)
          case long: Long => tape.putInt(address, long.toInt)
        }
    }
  }

  def readFromMemory(address: Int, theType: IType): RValue = {
    val result: AnyVal = theType match {
      case basic: IBasicType =>
        if (basic.getKind == eInt && basic.isShort) {
          tape.getShort(address)
        } else if (basic.getKind == eInt && basic.isLongLong) {
          tape.getLong(address)
        } else if (basic.getKind == eInt && basic.isLong) {
          tape.getInt(address)
        } else if (basic.getKind == eInt) {
          tape.getInt(address)
        } else if (basic.getKind == eBoolean) {
          tape.getInt(address)
        } else if (basic.getKind == eDouble) {
          tape.getDouble(address)
        } else if (basic.getKind == eFloat) {
          tape.getFloat(address)
        } else if (basic.getKind == eChar) {
          tape.get(address) // a C 'char' is a Java 'byte'
        } else if (basic.getKind == eVoid) {
          tape.getInt(address)
        } else {
          throw new Exception("Bad read val: " + basic.getKind)
        }
      case ptr: IPointerType => tape.getInt(address)
      case fcn: IFunctionType => tape.getInt(address)
      case struct: CStructure => tape.getInt(address)
      case qual: IQualifierType => readFromMemory(address, qual.getType).value
      case typedef: CTypedef => readFromMemory(address, typedef.getType).value
    }

    TypeHelper.castSign(theType, result)
  }
}

case class ReturnFromFunction() extends Exception("returning")

case class JmpIfNotEqual(expr: IASTExpression, relativeJump: Int)
case class JmpToLabelIfNotZero(expr: IASTExpression, label: Label)
case class JmpToLabelIfZero(expr: IASTExpression, label: Label)
case class JmpToLabelIfNotEqual(expr1: IASTExpression, expr2: IASTExpression, label: Label)
case class JmpToLabelIfEqual(expr1: IASTExpression, expr2: IASTExpression, label: Label)
case class Jmp(relativeJump: Int)
case class JmpLabel(label: Label)
case class JmpName(label: String) {
  var destAddress = 0
}

abstract class Label {
  var address = 0
}

case class GotoLabel(name: String) extends Label
case class BreakLabel() extends Label
case class ContinueLabel() extends Label
case class GenericLabel() extends Label

case class CaseLabel(caseStatement: IASTCaseStatement) extends Label
case class DefaultLabel(default: IASTDefaultStatement) extends Label

object State {
  def flattenNode(tUnit: IASTNode)(implicit state: State): List[Any] = {

    def recurse(node: IASTNode): List[Any] = {

      node match {
        case null => List()

        case ifStatement: IASTIfStatement =>
          val contents = recurse(ifStatement.getThenClause)
          val elseContents = List(Option(ifStatement.getElseClause)).flatten.flatMap(recurse)

          val jmp = if (ifStatement.getElseClause != null) {
            List(Jmp(elseContents.size))
          } else {
            List()
          }

          // add +1 for the jmp statement
          JmpIfNotEqual(ifStatement.getConditionExpression, (contents ++ jmp).size) +: ((contents ++ jmp) ++ elseContents)
        case forStatement: IASTForStatement =>

          val breakLabel = BreakLabel()
          state.breakLabelStack.push(breakLabel)
          val continueLabel = ContinueLabel()
          state.continueLabelStack.push(continueLabel)

          val init = List(forStatement.getInitializerStatement)
          val contents = recurse(forStatement.getBody)
          val iter = forStatement.getIterationExpression
          val beginLabel = new GotoLabel("")

          state.breakLabelStack.pop
          state.continueLabelStack.pop

          val execution = contents ++ List(continueLabel, iter)

          if (forStatement.getConditionExpression != null) {
            init ++ (beginLabel +: JmpToLabelIfNotZero(forStatement.getConditionExpression, breakLabel) +: execution :+ JmpLabel(beginLabel)) :+ breakLabel
          } else {
            init ++ (beginLabel +: execution :+ JmpLabel(beginLabel)) :+ breakLabel
          }
        case whileStatement: IASTWhileStatement =>

          val breakLabel = BreakLabel()
          state.breakLabelStack.push(breakLabel)
          val continueLabel = ContinueLabel()
          state.continueLabelStack.push(continueLabel)

          val contents = recurse(whileStatement.getBody)
          val begin = new GotoLabel("")
          val end = new GotoLabel("")

          state.breakLabelStack.pop
          state.continueLabelStack.pop

          List(JmpLabel(end), begin) ++ contents ++ List(end, continueLabel, JmpToLabelIfZero(whileStatement.getCondition, begin)) :+ breakLabel
        case doWhileStatement: IASTDoStatement =>

          val breakLabel = BreakLabel()
          state.breakLabelStack.push(breakLabel)
          val continueLabel = ContinueLabel()
          state.continueLabelStack.push(continueLabel)

          val contents = recurse(doWhileStatement.getBody)
          val begin = new GenericLabel()

          state.breakLabelStack.pop
          state.continueLabelStack.pop

          List(begin) ++ contents ++ List(continueLabel, JmpToLabelIfZero(doWhileStatement.getCondition, begin)) :+ breakLabel
        case switch: IASTSwitchStatement =>

          val breakLabel = BreakLabel()
          state.breakLabelStack.push(breakLabel)

          val descendants = recurse(switch.getBody)

          val jumpTable = descendants.flatMap{
            case x @ CaseLabel(caseStatement) =>
              List(JmpToLabelIfEqual(caseStatement.getExpression, switch.getControllerExpression, x))
            case x @ DefaultLabel(_) =>
              List(JmpLabel(x))
            case _ =>
              List()
          } :+ JmpLabel(breakLabel)

          state.breakLabelStack.pop

          val complete = jumpTable ++ descendants :+ breakLabel

          complete :+ breakLabel
        case x: IASTCaseStatement =>
          List(CaseLabel(x))
        case x: IASTDefaultStatement =>
          List(DefaultLabel(x))
        case _: IASTContinueStatement =>
          List(JmpLabel(state.continueLabelStack.head))
        case _: IASTBreakStatement =>
          List(JmpLabel(state.breakLabelStack.head))
        case _: IASTCompositeTypeSpecifier =>
          List()
        case _: IASTElaboratedTypeSpecifier =>
          List()
        case goto: IASTGotoStatement =>
          List(JmpName(goto.getName.getRawSignature))
        case fcn: IASTFunctionDefinition =>
          List(fcn)
        case compound: IASTCompoundStatement =>
          compound.getStatements.flatMap(recurse).toList
        case decl: IASTDeclarationStatement =>
          decl.getChildren.toList.flatMap(recurse)
        case decl: CASTSimpleDeclaration =>
          List(decl)
        case _: IASTSimpleDeclSpecifier =>
          List()
        case _: CASTTypedefNameSpecifier =>
          List()
        case decl: IASTDeclarator =>
          List(decl)
        case eq: IASTEqualsInitializer =>
          recurse(eq.getInitializerClause)
        case label: IASTLabelStatement =>
          GotoLabel(label.getName.getRawSignature) +: recurse(label.getNestedStatement)
        case exprState: CASTExpressionStatement =>
          List(exprState.getExpression)
        case fcn: IASTFunctionCallExpression =>
          List(fcn)
        case enum: IASTEnumerationSpecifier =>
          List(enum)
        case nameSpec: CASTTypedefNameSpecifier =>
          List(nameSpec)
        case _ =>
          println("SPLITTING: " + node.getClass.getSimpleName + " : " + node.getRawSignature)
          node +: node.getChildren.toList
      }
    }

    tUnit.getChildren.flatMap{recurse}.toList
  }
}

class State {

  object Stack extends Memory(100000)

  var heapInsertIndex = 50000

  val functionPrototypes = scala.collection.mutable.HashSet[IASTFunctionDeclarator]()

  val functionContexts = new Stack[FunctionScope]()
  def context: FunctionScope = functionContexts.head
  val functionList = new ListBuffer[Function]()
  val functionPointers = scala.collection.mutable.Map[String, Variable]()
  val stdout = new ListBuffer[Char]()
  var functionCount = 0

  val breakLabelStack = new Stack[Label]()
  val continueLabelStack = new Stack[Label]()

  val declarations = new ListBuffer[CStructure]()

  private val scopeCache = new scala.collection.mutable.HashMap[IASTNode, Scope]()

  def numScopes = functionContexts.size

  def pushScope(scope: FunctionScope): Unit = {
    functionContexts.push(scope)
  }

  def getFunctionScope = {
    functionContexts.collect{case fcnScope: FunctionScope => fcnScope}.head
  }

  def popFunctionContext = {
    Stack.insertIndex = functionContexts.head.startingStackAddr
    functionContexts.pop
  }

  def hasFunction(name: String): Boolean = functionList.exists{fcn => fcn.name == name}
  def getFunction(name: String): Function = functionList.find{fcn => fcn.name == name}.get
  def getFunctionByIndex(index: Int): Function = functionList.find{fcn => fcn.index == index}.get

  Functions.scalaFunctions.foreach{fcn =>
    addScalaFunctionDef(fcn)
  }

  def init(codes: Seq[String]): IASTNode = {
    val tUnit = Utils.getTranslationUnit(codes)

    val fcns = tUnit.getChildren.collect{case x:IASTFunctionDefinition => x}.filter(_.getDeclSpecifier.getStorageClass != IASTDeclSpecifier.sc_extern)
    fcns.foreach{fcnDef => addFunctionDef(fcnDef)}

    declarations ++= tUnit.getDeclarations.collect{case simp: CASTSimpleDeclaration => simp.getDeclSpecifier}
      .collect{case comp: CASTCompositeTypeSpecifier => comp}
      .map{x => x.getName.resolveBinding().asInstanceOf[CStructure]}

    tUnit
  }

  def addScalaFunctionDef(fcn: Function) = {

    fcn.index = functionCount

    functionList += fcn

    val fcnType = new CFunctionType(new CBasicType(IBasicType.Kind.eVoid, 0), null)

    val newVar = new Variable(new CASTName(fcn.name.toCharArray), State.this, fcnType)
    Stack.writeToMemory(functionCount, newVar.address, fcnType)

    functionPointers += fcn.name -> newVar
    functionCount += 1
  }

  private def addStaticFunctionVars(node: IASTNode, state: State): List[Variable] = {
    node match {
      case decl: IASTDeclarator =>
        val nameBinding = decl.getName.resolveBinding()

        if (nameBinding.isInstanceOf[IVariable]) {
          val theType = TypeHelper.stripSyntheticTypeInfo(nameBinding.asInstanceOf[IVariable].getType)

          if (decl.getParent.isInstanceOf[IASTSimpleDeclaration] &&
            decl.getParent.asInstanceOf[IASTSimpleDeclaration].getDeclSpecifier.getStorageClass == IASTDeclSpecifier.sc_static) {
            List(new Variable(decl.getName, state, theType))
          } else {
            List()
          }
        } else {
          List()
        }
      case x => x.getChildren.toList.flatMap{x => addStaticFunctionVars(x, state)}
    }
  }

  def addFunctionDef(fcnDef: IASTFunctionDefinition) = {
    val name = fcnDef.getDeclarator.getName

    val fcnType = fcnDef.getDeclarator.getName.resolveBinding().asInstanceOf[IFunction].getType

    functionList += new Function(name.getRawSignature, true) {
      index = functionCount
      node = fcnDef
      override val staticVars = addStaticFunctionVars(fcnDef, State.this)
      def parameters = fcnType.getParameterTypes.toList
      def run(formattedOutputParams: Array[RValue], state: State): Option[AnyVal] = {None}
    }

    val newVar = new Variable(name, State.this, fcnType)
    Stack.writeToMemory(functionCount, newVar.address, fcnType)

    functionPointers += name.getRawSignature -> newVar
    functionCount += 1
  }

  def callFunctionFromScala(name: String, args: Array[RValue]): Seq[IASTNode] = {

    functionList.find(_.name == name).map { fcn =>
      // this is a function simulated in scala
      fcn.run(args.reverse, this).foreach { retVal => context.stack.push(RValue(retVal, null)) }
    }

    Seq()
  }

  def callTheFunction(name: String, call: IASTFunctionCallExpression, args: Array[ValueType], scope: Option[FunctionScope]): Option[ValueType] = {

    functionList.find(_.name == name).map{ function =>

      val resolvedArgs: Array[RValue] = args.map{x =>
        x match {
          case StringLiteral(str) =>
            createStringVariable(str, false)(this)
          case info @ LValue(_, _) => info.value
          case value @ RValue(_, _) => value
        }
      }

      val convertedArgs = functionPrototypes.find{_.getName.getRawSignature == name}.map{ proto =>
        val params = proto.getChildren.collect{case param: CASTParameterDeclaration => param}
        var i = -1
        params.map{ p =>
          i += 1
          if (p.getDeclSpecifier.isInstanceOf[CASTSimpleDeclSpecifier]) {
            val param = p.getDeclarator.getName.resolveBinding().asInstanceOf[IParameter]
            TypeHelper.cast(param.getType, resolvedArgs(i).value)
          } else {
            resolvedArgs(i)
          }
        } ++ resolvedArgs.drop(i + 1)
      }.getOrElse {
        resolvedArgs
      }

      // printf assumes all floating point numbers are doubles
      // and shorts are 4 bytes
      val promoted = convertedArgs.map{arg =>
        if (arg.theType.isInstanceOf[IBasicType] && arg.theType.asInstanceOf[IBasicType].getKind == IBasicType.Kind.eFloat) {
          TypeHelper.cast(new CBasicType(IBasicType.Kind.eDouble, 0), arg.value)
        } else if (arg.theType.isInstanceOf[IBasicType] && arg.theType.asInstanceOf[IBasicType].isShort) {
          TypeHelper.cast(new CBasicType(IBasicType.Kind.eInt, 0), arg.value)
        } else if (arg.theType.isInstanceOf[IBasicType] && arg.theType.asInstanceOf[IBasicType].getKind == IBasicType.Kind.eChar) {
          TypeHelper.cast(new CBasicType(IBasicType.Kind.eInt, 0), arg.value)
        } else {
          arg
        }
      }

      if (!function.isNative) {
        // this is a function simulated in scala
        val returnVal = function.run(resolvedArgs.reverse, this)
        //popFunctionContext
        returnVal.map{theVal => RValue(theVal, null)}
      } else {

        val newScope = scope.getOrElse {
          if (call != null) {
            new FunctionScope(function.staticVars, functionContexts.headOption.getOrElse(null), new CFunctionType(call.getExpressionType, null))
          } else {
            new FunctionScope(function.staticVars, functionContexts.headOption.getOrElse(null), null)
          }
        }

        newScope.init(function.node, this, !scope.isDefined)

        functionContexts.push(newScope)

        //context.pathStack.push(NodePath(function.node, Stage1))
        context.stack.pushAll(promoted :+ RValue(resolvedArgs.size, null))
        context.run(this)
        if (!context.stack.isEmpty) {
          val returnVal = context.stack.pop
          if (!scope.isDefined) {
            popFunctionContext
          }
          Some(returnVal)
        } else {
          if (!scope.isDefined) {
            popFunctionContext
          }
          None
        }
      }
    }.getOrElse{
      // function pointer case
      val fcnPointer = functionContexts.head.resolveId(new CASTName(name.toCharArray)).get
      val function = getFunctionByIndex(fcnPointer.value.asInstanceOf[Int])
      val scope = new FunctionScope(function.staticVars, functionContexts.head, new CFunctionType(call.getExpressionType, null))
      functionContexts.push(scope)
      scope.init(call, this, true)
      //context.pathStack.push(NodePath(function.node, Stage1))
      context.run(this)
      popFunctionContext
      None
    }
  }

  def allocateSpace(numBytes: Int): Int = {
    if (numBytes > 0) {
      val result = Stack.insertIndex
      Stack.insertIndex += numBytes
      result
    } else {
      0
    }
  }

  def allocateHeapSpace(numBytes: Int): Int = {
    if (numBytes > 0) {
      val result = heapInsertIndex
      heapInsertIndex += numBytes
      result
    } else {
      0
    }
  }

  def copy(dst: Int, src: Int, numBytes: Int) = {
    if (numBytes != 0) {
      for (i <- (0 until numBytes)) {
         Stack.tape.put(dst + i, Stack.tape.get(src + i))
      }
    }
  }

  def readPtrVal(address: Int): Int = {
    Stack.readFromMemory(address, TypeHelper.pointerType).value.asInstanceOf[Int]
  }

  def createStringVariable(str: String, isHeap: Boolean)(implicit state: State): RValue = {
    val theStr = Utils.stripQuotes(str)
    val translateLineFeed = theStr.replace("\\n", 10.asInstanceOf[Char].toString)
    val withNull = (translateLineFeed.toCharArray() :+ 0.toChar).map{char => RValue(char.toByte, new CBasicType(IBasicType.Kind.eChar, 0))} // terminating null char
    val strAddr = if (isHeap) allocateHeapSpace(withNull.size) else allocateSpace(withNull.size)

    setArray(withNull, strAddr, 1)
    RValue(strAddr, TypeHelper.pointerType)
  }

  def setArray(array: Array[RValue], address: Int, stride: Int): Unit = {
      var i = 0
      array.foreach { element =>
        element match {
          case RValue(newVal, theType) =>
            Stack.writeToMemory(newVal, address + i, theType)
        }

        i += stride
      }
  }
}