package scala.astViewer

class StructTestStaging extends StandardTest {
   "struct initializer" should "print the correct results" in {
    val code = """
      
      struct Test {
        int one;
        double two;
        char three;
      };
      
      void main() {
        struct Test x = {1, 2.0, 'a'};
        printf("%d %f %c\n", x.one, x.two, x.three);
      }"""

    checkResults(code)
  }
}

class StructTest extends StandardTest {
  "basic struct test" should "print the correct results" in {
    val code = """
      
      struct Test {
        int y;
        int z;
      };
      
      void main() {
        struct Test x;
        int y = 36;
        x.y = 465;
        printf("%d %d\n", x.y, y);
      }"""

    checkResults(code)
  }
  
  "moderate struct test" should "print the correct results" in {
    val code = """
      
      struct Test {
        int y;
      };
      
      void main() {
        struct Test x;
        struct Test u;
        x.y = 465;
        u.y = 234;
        printf("%d %d\n", u.y, x.y);
      }"""

    checkResults(code)
  }
  
  "struct test multiple members" should "print the correct results" in {
    val code = """
      
      struct Test {
        int y;
        int z;
      };
      
      void main() {
        struct Test x;
        x.y = 465;
        x.z = 234;
        printf("%d %d\n", x.y, x.z);
      }"""

    checkResults(code)
  }
  
  "pointer to struct" should "print the correct results" in {
    val code = """
      
      struct Test {
        int y;
      };
      
      void main() {
        struct Test x;
        struct Test *y = &x;
        y->y = 465;
        printf("%d %d\n", x.y, y->y);
      }"""

    checkResults(code)
  }
  
 
  
}