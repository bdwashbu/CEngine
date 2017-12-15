package scala.c.engine

import better.files.File

class ListTest extends StandardTest {
  "list test" should "print the correct results" in {

    val allCode = Seq(File("tests\\scala\\c\\engine\\libds-master\\list.c").contentAsString,
      File("tests\\scala\\c\\engine\\libds-master\\listtest.c").contentAsString)


    checkResults2(allCode)
  }
}

class VectorTest extends StandardTest {
  "vector test" should "print the correct results" in {

    val allCode =  Seq(File("tests\\scala\\c\\engine\\libds-master\\vector.c").contentAsString,
                       File("tests\\scala\\c\\engine\\libds-master\\vectest.c").contentAsString)


    checkResults2(allCode)
  }
}

class HeapTest extends StandardTest {
  "heap test" should "print the correct results" in {

    val allCode =  Seq(File("tests\\scala\\c\\engine\\libds-master\\vector.c").contentAsString,
      File("tests\\scala\\c\\engine\\libds-master\\heap.c").contentAsString,
      File("tests\\scala\\c\\engine\\libds-master\\heaptest.c").contentAsString)


    checkResults2(allCode)
  }
}

class HashMapTest extends StandardTest {
  "heap test" should "print the correct results" in {

    val allCode =  Seq(File("tests\\scala\\c\\engine\\libds-master\\vector.c").contentAsString,
      File("tests\\scala\\c\\engine\\libds-master\\hashmap.c").contentAsString,
      File("tests\\scala\\c\\engine\\libds-master\\maptest.c").contentAsString)


    checkResults2(allCode)
  }
}