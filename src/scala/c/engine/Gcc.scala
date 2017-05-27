package c.engine

import scala.sys.process.ProcessIO
import scala.sys.process._
import better.files._
import scala.collection.mutable.ListBuffer
import java.io.OutputStream
import java.io.InputStream

object Gcc {

  def runCode(code: String, state: State) = {
      val exeCode = s"""
        void main() {
           $code
        }
      """

      Executor.preload(Seq(exeCode), state)
      val main = state.getFunction("main")
      state.functionList -= main
      state.isFirst = false
      state.stackInsertIndex -= 4
      state.current = main.node
      Executor.run(state)
  }

  def runGlobalCode(code: String, state: State) = {
    val exeCode = s"""
       $code
    """

    Executor.preload(Seq(exeCode), state)
  }
  
  
  var count = 0
  var count2 = 0
  
  def compileAndGetOutput(codes: Seq[String]): Seq[String] = {
    
    val logger = new SyntaxLogger
    val linkerLogger = new LinkerLogger
    val runLogger = new RunLogger
    
    var myCount = 0;
    
    val fileNames = codes.map{ code =>
    
      synchronized {
        myCount = count
        count += 1
      }
      
      ("temp" + myCount, code)
    }

    synchronized {
      myCount = count
      count += 1
    }
    
    val files = fileNames.map{ case (fileName, code) =>
      val file = File(fileName + ".c")
      file.overwrite(code)
      file
    }
    
    val objectFiles = fileNames.map{ case (fileName, code) =>
      val file = File(fileName + ".o")
      file
    } 
    
    val exeFile = File("temp" + myCount + ".exe")
    
    val sourceFileTokens = files.flatMap{file => Seq("-c", file.path.toString)}
    val includeTokens = Seq("-I", Utils.mainPath, 
                            "-I", Utils.mainAdditionalPath)

    val processTokens =
        Seq("gcc") ++ sourceFileTokens ++ includeTokens ++ Seq("-D", "ALLOC_TESTING")
  
    val builder = Process(processTokens, File("").toJava)
    val compile = builder.run(logger.process)
    compile.exitValue()
    
    val numErrors = logger.errors.length

    val result = if (numErrors == 0) {    
      val linkTokens = Seq("gcc") ++ Seq("-o", exeFile.path.toString) ++ objectFiles.map(_.path.toString)
      
      val linker = Process(linkTokens, File("").toJava)
      val link = linker.run(linkerLogger.process)
      link.exitValue()
      
      var i = 0
      while (!File(exeFile.path.toString).exists && i < 1000) {
          Thread.sleep(50)
          i += 50
      }
      Thread.sleep(50)

      val runner = Process(Seq(exeFile.path.toString), File("").toJava)
      val run = runner.run(runLogger.process)
      run.exitValue()

      runLogger.stdout
    } else {
      logger.errors
    }
    
    files.foreach{file => file.delete(true)}
    objectFiles.foreach{file => file.delete(true)}
    exeFile.delete(true)
    
    result
  }
}

case class ErrorLocation(file: File, line: Int, column: Int)

case class BuildError(problemPath: Seq[ErrorLocation], function: Option[String], errorType: String, error: String)

class SyntaxLogger extends Logger {
  
  val errors = new ListBuffer[String]()

  def addErrors(newErrors: Seq[String]) = {
    errors ++= newErrors
  }
  
  def in(stream: OutputStream) = {}
  def out(stream: InputStream) = {}
  def err(stream: InputStream) = {

    val lines = scala.io.Source.fromInputStream(stream).getLines.toSeq
    val errors = getErrors(lines)
  }
}

class RunLogger {
  
  def process = new ProcessIO(in, out, err)

  val stdout = new ListBuffer[String]()
  
  def recordStdOut(lines: Seq[String]) = {
    stdout ++= lines
  }
  
  def in(stream: OutputStream) = {}
  def out(stream: InputStream) = {
    recordStdOut(scala.io.Source.fromInputStream(stream).getLines.toSeq)
  }
  def err(stream: InputStream) = {
    val lines = scala.io.Source.fromInputStream(stream).getLines.toSeq
  }
}

class LinkerLogger extends Logger {
  
  val errors = new ListBuffer[String]()

  def addErrors(newErrors: Seq[String]) = {
    errors ++= newErrors
  }

  def in(stream: OutputStream) = {}
  def out(stream: InputStream) = { scala.io.Source.fromInputStream(stream).getLines }
  def err(stream: InputStream) = {

    val lines = scala.io.Source.fromInputStream(stream).getLines.toSeq

    if (!lines.isEmpty) {
      lines.foreach(x => println("LINKER ERROR: " + x))
    }
  }
}

abstract class Logger {
  def in(stream: OutputStream): Unit
  def out(stream: InputStream): Unit
  def err(stream: InputStream): Unit

  def process = new ProcessIO(in, out, err)

  val availableErrors = ListBuffer[String]()
  var errorIsInHeaderFile = false
  val errorSource = ListBuffer[String]()
  var isParsingSourcePath = false
  var currentFunction = ""

  def addErrors(errors: Seq[String])
  
  def getErrors(lines: Seq[String]): Seq[String] = {
    lines
  }
}