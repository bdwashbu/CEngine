package scala.c.engine

import org.eclipse.cdt.core.dom.ast._
import java.util.Formatter
import java.util.Locale

import org.eclipse.cdt.internal.core.dom.parser.c.{CBasicType, _}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

// 'isNative' implies the function is in C, not Scala
abstract case class Function(name: String, isNative: Boolean) {
  var index = -1
  var node: IASTNode = null

  val staticVars: List[Variable] = List()

  def run(formattedOutputParams: Array[RValue], state: State): Option[AnyVal]
}

object Functions {
  
  var varArgStartingAddr = List[Int]()
  
  val scalaFunctions = new ListBuffer[Function]()
  
  scalaFunctions += new Function("rand", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          Some(Math.abs(scala.util.Random.nextInt))
        }
      }
  
  scalaFunctions += new Function("isalpha", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val theChar = formattedOutputParams.head.value.asInstanceOf[char].toChar
          Some(if (theChar.isLetter) 1 else 0)
        }
      }

  scalaFunctions += new Function("isdigit", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      val theChar = formattedOutputParams.head.value match {
        case c: char => c.toChar
        case int: Int => int.toChar
      }
      Some(if (theChar.isDigit) 1 else 0)
    }
  }

  scalaFunctions += new Function("isxdigit", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      val theChar = formattedOutputParams.head.value match {
        case c: char => c.toChar
        case int: Int => int.toChar
      }
      Some(if (theChar.toString.matches("^[0-9a-fA-F]+$")) 1 else 0)
    }
  }
  
  scalaFunctions += new Function("tolower", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val theChar = formattedOutputParams.head.value match {
            case c: char => c.toChar
            case int: Int => int.toChar
          }
          Some(theChar.toLower.toByte)
        }
      }
  
  scalaFunctions += new Function("toupper", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val theChar = formattedOutputParams.head.value.asInstanceOf[char].toChar
          Some(theChar.toUpper.toByte)
        }
      }
  
  scalaFunctions += new Function("isupper", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val theChar = formattedOutputParams.head.value match {
            case int: int => int.toChar
            case char: char => char.toChar
          }
          Some(if (theChar.isUpper) 1 else 0)
        }
      }

  scalaFunctions += new Function("isspace", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      val theChar = formattedOutputParams.head.value match {
        case c: char => c.toChar
        case int: Int => int.toChar
      }
      Some(if (theChar.isSpaceChar || theChar.toInt == 13 || theChar.toInt == 10) 1 else 0)
    }
  }
  
  scalaFunctions += new Function("calloc", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val numBlocks = formattedOutputParams(0).value.asInstanceOf[Int]
          val blockSize = formattedOutputParams(1).value.asInstanceOf[Int]

          val addr = state.allocateHeapSpace(numBlocks * blockSize)

          state.Stack.clearMemory(addr, numBlocks * blockSize)

          Some(addr)
        }
      }
  
  scalaFunctions += new Function("malloc", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val returnVal = formattedOutputParams.head.value match {
            case long: Long => state.allocateHeapSpace(long.toInt)
            case int: Int => state.allocateHeapSpace(int)
          }
          Some(returnVal)
        }
      }
  
 scalaFunctions += new Function("realloc", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          Some(state.allocateHeapSpace(formattedOutputParams.head.value.asInstanceOf[Long].toInt))
        }
      }
 
  scalaFunctions += new Function("memmove", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val dst = formattedOutputParams(0).value.asInstanceOf[Int]
          val src = formattedOutputParams(1).value.asInstanceOf[Int]
          val numBytes = formattedOutputParams(2).value.asInstanceOf[Int]
          
          state.copy(dst, src, numBytes)
          None
        }
      }
  
  scalaFunctions += new Function("memcpy", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val numBytes = formattedOutputParams(0).value match {
            case int: Int => int
            case long: Long => long.toInt
          }
          val src = formattedOutputParams(1).value.asInstanceOf[Int]
          val dst = formattedOutputParams(2).value.asInstanceOf[Int]

          state.copy(dst, src, numBytes)
          None
        }
      }

    scalaFunctions += new Function("memset", false) {
      def run(formattedOutputParams: Array[RValue], state: State) = {
        val numBytes = formattedOutputParams(0).value match {
          case int: Int => int
          case long: Long => long.toInt
        }
        val value = formattedOutputParams(1).value.asInstanceOf[Int].toByte
        println("VALUE: " + value)
        println("NUM BYTES: " + numBytes)
        val dst = formattedOutputParams(2).value.asInstanceOf[Int]

        state.set(dst, value, numBytes)
        None
      }
    }
  
   scalaFunctions += new Function("_assert", false) {
        def run(formattedOutputParams: Array[RValue], state: State): Option[AnyVal] = {
          val addy = formattedOutputParams(0).value.asInstanceOf[Int]
          println(Utils.readString(addy)(state) + " FAILED")
          None
        }
      }

   scalaFunctions += new Function("modf", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val fraction = formattedOutputParams(0).value.asInstanceOf[Double]
          val intPart = formattedOutputParams(1).value.asInstanceOf[Int]
          
          state.Stack.writeToMemory(fraction.toInt, intPart, TypeHelper.intType)
          
          Some(fraction % 1.0)
        }
      }

  scalaFunctions += new Function("sqrt", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      val num = formattedOutputParams(0).value.asInstanceOf[Double]
      Some(Math.sqrt(num))
    }
  }
   
   scalaFunctions += new Function("putchar", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val char = formattedOutputParams(0).value match {
            case int: Int => int.toChar
            case char: char => char.toChar
          }

          state.stdout += char

          None
        }
      }

   scalaFunctions += new Function("fopen", false) {
     def run(formattedOutputParams: Array[RValue], state: State) = {
       val fileName = Utils.readString(formattedOutputParams.last.value.asInstanceOf[Int])(state)

       import java.nio.file.{Files, Paths}

       val byteArray = Files.readAllBytes(Paths.get(fileName))

       val fileData = state.allocateSpace(byteArray.size + 4) // extra space for index

       state.Stack.tape.putInt(fileData, 0) // index space
       state.writeDataBlock(byteArray, fileData + 4)(state)

       val pointer = state.allocateSpace(4)
       state.Stack.tape.putInt(pointer, fileData)

       Some(pointer)
     }
   }

   scalaFunctions += new Function("fread", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      val resultBuffer = formattedOutputParams(3).value.asInstanceOf[Int]
      val size = formattedOutputParams(2).value.asInstanceOf[Int]
      val numMembers = formattedOutputParams(1).value.asInstanceOf[Int]
      val fp = formattedOutputParams(0).value.asInstanceOf[Int]

      val location = state.readPtrVal(fp)

      var index = state.Stack.readFromMemory(location, TypeHelper.intType).value.asInstanceOf[Int]

      val result = (0 until numMembers).map { offset =>
        val result = state.Stack.readFromMemory(location + index + 4, new CBasicType(IBasicType.Kind.eChar, 0))
        index += size
        result
      }.toArray

      state.Stack.tape.putInt(location, index) // index space

      state.writeDataBlock(result, resultBuffer)(state)

      None
    }
   }

  def printf(formattedOutputParams: Array[RValue], state: State): String = {
    val str = Utils.readString(formattedOutputParams.last.value.asInstanceOf[Int])(state)
    val formatString = str.replaceAll("^\"|\"$", "").replaceAll("%ld", "%d").replaceAll("%l", "%d")

    val buffer = new StringBuffer()
    val formatter = new Formatter(buffer, Locale.US)

    var percentFound = false
    var paramCount = 0

    val resolved = new ListBuffer[Object]()

    val varArgs = formattedOutputParams.reverse.tail.toList

    var resultingFormatString = ""
    var formatFound = ""

    str.toCharArray.foreach{ c =>
      if (!percentFound && c == '%') {
        resultingFormatString += c
        formatFound = ""
        percentFound = true
      } else if (percentFound && c == 's') {
        percentFound = false
        val theVal = varArgs(paramCount).value
        val stringAddr = theVal.asInstanceOf[Int]

        if (stringAddr != 0) {
          val str = Utils.readString(stringAddr)(state)
          resolved += str.split(System.lineSeparator()).mkString.asInstanceOf[Object]
        } else {
          resolved += "(null)".asInstanceOf[Object]
        }
        formatFound += c
        resultingFormatString += formatFound
        paramCount += 1
      } else if (percentFound && (c == 'u')) {
        formatFound += 'd'

        val x = TypeHelper.resolve(varArgs(paramCount))(state).value
        resolved += (if (x.isInstanceOf[Boolean]) {
          if (x.asInstanceOf[Boolean]) 1 else 0
        } else {
          x
        }).asInstanceOf[Object]

        resultingFormatString += 'd'

        percentFound = false
        paramCount += 1
      } else if (percentFound && (c == 'd')) {
        formatFound += c

        val x = TypeHelper.resolve(varArgs(paramCount))(state).value
        resolved += (if (x.isInstanceOf[Boolean]) {
          if (x.asInstanceOf[Boolean]) 1 else 0
        } else {
          x
        }).asInstanceOf[Object]

        if (formatFound != "ld") {
          resultingFormatString += formatFound
        } else {
          resultingFormatString += 'd'
        }

        percentFound = false
        paramCount += 1
      } else if (percentFound && c == 'c') {
        resolved += TypeHelper.resolve(varArgs(paramCount))(state).value.asInstanceOf[Object]
        percentFound = false
        formatFound += c
        resultingFormatString += formatFound
        paramCount += 1
      } else if (percentFound && c == 'f') {
        formatFound += c

        val buffer2 = new StringBuffer()
        val formatter2 = new Formatter(buffer2, Locale.US)

        val base = TypeHelper.resolve(varArgs(paramCount))(state).value.asInstanceOf[Object]

        formatter2.format("%f", List(base): _*)

        if (buffer2.toString.contains("Infinity") || buffer2.toString.contains("NaN")) {
          resultingFormatString += 's'
          resolved += buffer2.toString.replace("Infinity", "1.#INF00")
            .replace("NaN", "-1.#IND00")
        } else {
          resultingFormatString += formatFound
          resolved += base
        }

        percentFound = false
        paramCount += 1
      } else if (percentFound) {
        formatFound += c
      } else {
        resultingFormatString += c
      }
    }

    formatter.format(resultingFormatString, resolved: _*)

    buffer.toString
  }

  scalaFunctions += new Function("atoi", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      val str = Utils.readString(formattedOutputParams.last.value.asInstanceOf[Int])(state)
      println("STRING: " + str)
      Some(str.toInt)
    }
  }

  scalaFunctions += new Function("fabs", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.abs(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("sin", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.sin(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("cos", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.cos(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("tan", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.tan(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("acos", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.acos(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("cosh", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.cosh(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("asin", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.asin(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("sinh", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.sinh(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("atan", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.atan(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("tanh", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.tanh(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("atan2", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.atan2(formattedOutputParams.last.value.asInstanceOf[Double],
        formattedOutputParams.head.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("exp", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.exp(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("ceil", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.ceil(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("floor", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.floor(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("log", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.log(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("log10", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.log10(formattedOutputParams.last.value.asInstanceOf[Double]))
    }
  }

  scalaFunctions += new Function("pow", false) {
    def run(formattedOutputParams: Array[RValue], state: State) = {
      Some(Math.pow(formattedOutputParams.last.value.asInstanceOf[Double],
        formattedOutputParams.head.value.asInstanceOf[Double]))
    }
  }

   scalaFunctions += new Function("sscanf", false) {
     def run(formattedOutputParams: Array[RValue], state: State) = {
       val resultBuffer = formattedOutputParams.last.value.asInstanceOf[Int]

       val varArgs = formattedOutputParams.drop(2).toList

       val result = printf(formattedOutputParams, state)

       state.writeDataBlock(result.getBytes, resultBuffer)(state)

       Some(varArgs.size)
     }
   }

   scalaFunctions += new Function("printf", false) {
      def run(formattedOutputParams: Array[RValue], state: State) = {

        val result = printf(formattedOutputParams, state)

        result.getBytes.foreach{char =>
          state.callFunctionFromScala("putchar", Array(RValue(char.toInt, TypeHelper.intType)))
        }

        None
      }
    }

   scalaFunctions += new Function("strlen", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val straddy = formattedOutputParams.head.value match {
            //case AddressInfo(addr, _) => addr.value
            case int: Int => int
          }
          var current: char = 0
          var i = 0
          do {
            current = state.Stack.readFromMemory(straddy + i, new CBasicType(IBasicType.Kind.eChar, 0)).value.asInstanceOf[char]
            if (current != 0) {
              i += 1
            }
          } while (current != 0)
          Some(i)
        }
      }
   
  scalaFunctions += new Function("strchr", false) {
      def run(formattedOutputParams: Array[RValue], state: State) = {
        val char = formattedOutputParams(0).value.asInstanceOf[Int]
        val straddy = formattedOutputParams(1).value.asInstanceOf[Int]

        val str = Utils.readString(straddy)(state)

        val offset = str.indexOf(char.toChar)

        if (offset != -1) {
          Some(straddy + offset)
        } else {
          Some(0)
        }
      }
    }
  
  scalaFunctions += new Function("offsetof", false) {
      def run(formattedOutputParams: Array[RValue], state: State) = {
        val straddy = formattedOutputParams(0).value.asInstanceOf[Int]
        val straddy2 = formattedOutputParams(1).value.asInstanceOf[Int]

        val memberName = Utils.readString(straddy)(state)
        val stuctName = Utils.readString(straddy2)(state)

        val structs = state.declarations

        val struct = structs.find{x => ("struct " + x.getName) == stuctName}.get

        Some(TypeHelper.offsetof(struct, memberName, state))
      }
    }
  
 scalaFunctions += new Function("strcmp", false) {
      def run(formattedOutputParams: Array[RValue], state: State) = {
        val straddy = formattedOutputParams(0).value.asInstanceOf[Int]
        val straddy2 = formattedOutputParams(1).value.asInstanceOf[Int]

        val str1 = Utils.readString(straddy)(state)
        val str2 = Utils.readString(straddy2)(state)

        val same = str1 == str2
        Some((if (same) 0 else 1))
      }
    }
 
  scalaFunctions += new Function("memcmp", false) {
      def run(formattedOutputParams: Array[RValue], state: State) = {
        val numBytes = formattedOutputParams(0).value match {
          case long: Long => long.toInt
          case int: Int => int
        }
        val memaddy = formattedOutputParams(1).value.asInstanceOf[int]
        val memaddy2 = formattedOutputParams(2).value.asInstanceOf[int]

        var same = true

        for (i <- (0 until numBytes)) {
          same &= state.Stack.readFromMemory(memaddy + i, new CBasicType(IBasicType.Kind.eChar, 0)).value == state.Stack.readFromMemory(memaddy2 + i, new CBasicType(IBasicType.Kind.eChar, 0)).value
        }

        Some((if (same) 0 else 1))
      }
    }
  
  scalaFunctions += new Function("free", false) {
        def run(formattedOutputParams: Array[RValue], state: State): Option[AnyVal] = {
          None
        }
      }
  
  scalaFunctions += new Function("va_arg", false) {
      def run(formattedOutputParams: Array[RValue], state: State) = {
        val argTypeStr = formattedOutputParams(0).value.asInstanceOf[Int]

        val str = Utils.readString(argTypeStr)(state)

        val (offset, theType) = (str match {
          case "unsigned int" => (4, TypeHelper.unsignedIntType)
          case "int" => (4, TypeHelper.intType)
          case "double" => (8, TypeHelper.doubleType)
          case "char" => (1, new CBasicType(IBasicType.Kind.eChar, 0))
          case "char *" => (4, new CPointerType(new CBasicType(IBasicType.Kind.eChar, 0), 0))
          case "unsigned long" => (8, new CPointerType(new CBasicType(IBasicType.Kind.eInt, IBasicType.IS_LONG), 0))
        })

        val current = varArgStartingAddr.head
        varArgStartingAddr = varArgStartingAddr.tail
        val result = state.Stack.readFromMemory(current, theType).value
        varArgStartingAddr = (current + offset) +: varArgStartingAddr

        Some(result)
      }
    }
  
   scalaFunctions += new Function("va_start", false) {
      def run(formattedOutputParams: Array[RValue], state: State) = {
        val lastNamedArgAddr = formattedOutputParams(0).value.asInstanceOf[Int]
        varArgStartingAddr = (lastNamedArgAddr + 4) +: varArgStartingAddr
        None
      }
    }
   
   scalaFunctions += new Function("va_end", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
           varArgStartingAddr = varArgStartingAddr.tail
           None
        }
      }
      //fcvtbuf(double arg, int ndigits, int *decpt, int *sign, char *buf)
  scalaFunctions += new Function("fcvtbuf", false) {
        def run(formattedOutputParams: Array[RValue], state: State) = {
          val buf = formattedOutputParams(0).value.asInstanceOf[Int]
          val sign = formattedOutputParams(1).value.asInstanceOf[Int]
          val decpt = formattedOutputParams(2).value.asInstanceOf[Int]
          val ndigits = formattedOutputParams(3).value.asInstanceOf[Int]
          val arg = formattedOutputParams(4).value.asInstanceOf[Double]
          
          state.Stack.writeToMemory(1, decpt, TypeHelper.intType)
          
          val buffer = new StringBuffer();
          val formatter = new Formatter(buffer, Locale.US);
          
          val formatString = "%." + ndigits + "f"
          
          val args = Array[Object](arg.asInstanceOf[Object])

          formatter.format(formatString, args: _*)
          
          val result1 = buffer.toString
          val index = result1.indexOf('.')
          val resultString = result1.replace(".", "")

          val array = resultString.toCharArray.map{ char => RValue(char.toByte, new CBasicType(IBasicType.Kind.eChar, 0))}

          state.Stack.writeToMemory(index, decpt, TypeHelper.intType)

          // to-do: find a way to do this without allocating?
          val result = state.allocateHeapSpace(20)

          state.writeDataBlock(array, result)(state)
          Some(result)
        }
      }
}