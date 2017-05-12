package scala.astViewer

import app.astViewer.Gcc
import app.astViewer.State
import app.astViewer.cEngine._
import better.files._

class ListTest extends StandardTest {
  "list test" should "print the correct results" in {

    val allCode = Seq(File("test\\scala\\libds-master\\list.c").contentAsString,
      File("test\\scala\\libds-master\\listtest.c").contentAsString)


    checkResults2(allCode)
  }
}

class VectorTest extends StandardTest {
  "vector test" should "print the correct results" in {

    val allCode =  Seq(File("test\\scala\\libds-master\\vector.c").contentAsString,
                       File("test\\scala\\libds-master\\vectest.c").contentAsString)


    checkResults2(allCode)
  }
}

class HeapTest extends StandardTest {
  "heap test" should "print the correct results" in {

    val allCode =  Seq(File("test\\scala\\libds-master\\vector.c").contentAsString,
      File("test\\scala\\libds-master\\heap.c").contentAsString,
      File("test\\scala\\libds-master\\heaptest.c").contentAsString)


    checkResults2(allCode)
  }
}

class HashMapTest extends StandardTest {
  "heap test" should "print the correct results" in {

    val allCode =  Seq(File("test\\scala\\libds-master\\vector.c").contentAsString,
      File("test\\scala\\libds-master\\hashmap.c").contentAsString,
      File("test\\scala\\libds-master\\maptest.c").contentAsString)


    checkResults2(allCode)
  }
}