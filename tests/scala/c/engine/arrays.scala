package scala.c.engine

class OneDimAddressingCheck extends StandardTest2("An 1d array addressing check",
  """
      void main() {
        int x[2];
        printf("%d\n", &x[1] - x);
      }"""
)

class TwoDimAddressingCheck extends StandardTest2("2d array pointer arithmetic",
  """
      void main() {
        int x[2][2] = {1,2,3,4};
        int *ptr = x;
        printf("%d\n", *ptr);
        ptr++;
        printf("%d\n", *ptr);
        ptr++;
        printf("%d\n", *ptr);
        ptr++;
        printf("%d\n", *ptr);
      }"""
)

class MultiDimArrayClobberingTest extends StandardTest2("2d array with a variable as a dimension",
  """
      int blah() {
        return 7;
      }

      int main()
      {
        int lx = blah();
        int lnk[7][lx];
        int i = 0; // clobber test

        printf("%d\n", i);
        lnk[0][0] = 1;
        printf("%d\n", i);
      }
  """
)

class ArrayInitSanityTest extends StandardTest2("tests on the array initializer",
  """

      int main()
      {
        int x[5] = {1};
        float y[5] = {1.0};
        printf("%d %f\n", x[0], y[0]);
      }
  """
)

class ArrayMutableInitList extends StandardTest2("initializer lists with mutable operations within",
  """

      int main()
      {
        int i = 0;
        int x[5] = {i, ++i, ++i, ++i, ++i};
        for (i = 0; i < 5; i++) {
           printf("%d\n", x[i]);
        }

        i = 0;
        int y[3]={i,i+=29,i+=7};

        for (i = 0; i < 3; i++) {
           printf("%d\n", y[i]);
        }

      }
  """
)

class ArrayInitTest extends StandardTest {
  "Sized arrays initialized with initLists" should "print the correct results" in {
    val code = """
      void main() {
        int x[5] = {1, 2, 3, 4, 5};
        printf("%d %d %d %d %d\n", x[0], x[1], x[2], x[3], x[4]);
        
        char y[5] = {'a', 'b', 'c', 'd', 'e'};
        printf("%c %c %c %c %c\n", y[0], y[1], y[2], y[3], y[4]);
        
        double z[5] = {5.6, 38.5, 2.945, 347.2, 378.2};
        printf("%f %f %f %f %f\n", z[0], z[1], z[2], z[3], z[4]);

        float j[5] = {5.6, 38.5, 2.945, 347.2, 378.2};
        printf("%f %f %f %f %f\n", j[0], j[1], j[2], j[3], j[4]);
      }"""

    checkResults(code)
  }

  "Sized arrays initialized with initLists smaller than the true size" should "print the correct results" in {
    val code = """
      void main() {
        int x[5] = {1, 2, 3, 4};
        printf("%d %d %d %d\n", x[0], x[1], x[2], x[3]);
        printf("%d\n", sizeof(x));

        char y[5] = {'a', 'b', 'c', 'd'};
        printf("%c %c %c %c\n", y[0], y[1], y[2], y[3]);
        printf("%d\n", sizeof(y));

        double z[5] = {5.6, 38.5, 2.945, 347.2};
        printf("%f %f %f %f\n", z[0], z[1], z[2], z[3]);
        printf("%d\n", sizeof(z));
      }"""

    checkResults(code)
  }

  "array indexed with a division binary expression" should "print the correct results" in {
    val code = """
      void main() {
        int x[5] = {1, 2, 3, 4, 5};
        int y = 4;
        int z = 2;
        printf("%d\n", x[2]);
        printf("%d\n", x[4 / 2]);
        printf("%d\n", x[y / 2]);
        printf("%d\n", x[4 / z]);
      }"""

    checkResults(code)
  }
  
  "Unsized arrays initialized with initLists" should "print the correct results" in {
    val code = """
      void main() {
        int x[] = {1, 2, 3, 4, 5};
        printf("%d %d %d %d %d\n", x[0], x[1], x[2], x[3], x[4]);
        printf("%d\n", sizeof(x));
        
        char y[] = {'a', 'b', 'c', 'd', 'e'};
        printf("%c %c %c %c %c\n", y[0], y[1], y[2], y[3], y[4]);
        printf("%d\n", sizeof(y));
        
        double z[] = {5.6, 38.5, 2.945, 347.2, 378.2};
        printf("%f %f %f %f %f\n", z[0], z[1], z[2], z[3], z[4]);
        printf("%d\n", sizeof(z));
      }"""

    checkResults(code)
  }
}

class SimpleHigherDimArrays extends StandardTest {
  "check for array clobbering" should "print the correct results" in {
    val code = """
      void main() {
        int x[3][9];
        x[1][0] = 43424;
        x[1][1] = 43;
        x[1][2] = 64565;
        x[0][0] = 5645;
        x[0][1] = 878;
        x[0][2] = 98797;
        printf("%d %d %d\n", x[1][0], x[1][1], x[1][2]);
        printf("%d %d %d\n", x[0][0], x[0][1], x[0][2]);
      }"""

    checkResults(code)
  }
}

class Initializing2dArrayWith2dList extends StandardTest {
  "initializing 2d array with 2d list" should "print the correct results" in {
    val code = """
      void main() {
        double elems[4][4] = {
          { 2, -1,  5,  1},
          { 3,  2,  2, -6},
          { 1,  3,  3, -1},
          { 5, -2, -3,  3}
        };

        printf("%f\n", elems[1][2]);
      }"""

    checkResults(code)
  }
}

class HigherDimArrays extends StandardTest {

  "A 2d array" should "print the correct results" in {
    val code = """
      void main() {
        int x[2][2];
        int i, j = 0;
        int count = 0;

        for (i = 0; i < 2; i++) {
          for (j = 0; j < 2; j++) {
            x[i][j] = count;
            count += 1;
          }
        }

        for (i = 0; i < 2; i++) {
          for (j = 0; j < 2; j++) {
            printf("%d\n", x[i][j]);
          }
        }
      }"""

    checkResults(code)
  }

  "2d array addressing" should "print the correct results" in {
    val code = """
      void main() {
        int a[2][3] = {1, 2, 3, 4, 5, 6};
        printf("%d\n", &(a[0][0]) - &(a[0][0]));
        printf("%d\n", &(a[0][0]) - &(a[0][1]));
        printf("%d\n", &(a[0][0]) - &(a[0][2]));
        printf("%d\n", &(a[0][0]) - &(a[1][0]));
        printf("%d\n", &(a[0][0]) - &(a[1][1]));
      }"""

    checkResults(code, true)
  }

  "Initialize a 2d array" should "print the correct results" in {
    val code = """
      void main() {
        int a[2][3] = {1, 2, 3, 4, 5, 6};
        printf("%d\n", a[0][0]);
        printf("%d\n", a[0][1]);
        printf("%d\n", a[0][2]);
        printf("%d\n", a[1][0]);
        printf("%d\n", a[1][1]);
        printf("%d\n", a[1][2]);
        printf("%d\n", *a[0]);
        printf("%d\n", *a[1]);
      }"""

    checkResults(code, true)
  }

  "Initialize a boundless 2d array" should "print the correct results" in {
    val code = """
      void main() {
        int a[][3] = {1, 2, 3, 4, 5, 6};
        printf("%d\n", a[0][0]);
        printf("%d\n", a[0][1]);
        printf("%d\n", a[0][2]);
        printf("%d\n", a[1][0]);
        printf("%d\n", a[1][1]);
        printf("%d\n", a[1][2]);
        printf("%d\n", *a[0]);
        printf("%d\n", *a[1]);
      }"""

    checkResults(code, true)
  }

  "access a pointer to a array" should "print the correct results" in {
    val code = """
      void main() {
        int a[][3] = {1, 2, 3, 4, 5, 6};
        int (*ptr)[3] = a;
        printf("%d %d ", (*ptr)[1], (*ptr)[2]);
      }"""

    checkResults(code, true)
  }
  
  "A 3d array" should "print the correct results" in {
    val code = """
      void main() {
        int x[2][2][2];
        int i, j, k = 0;
        int count = 0;
        
        for (i = 0; i < 2; i++) {
          for (j = 0; j < 2; j++) {
            for (k = 0; k < 2; k++) {
              x[i][j][k] = count;
              count += 1;
            }
          }
        }
        
        for (i = 0; i < 2; i++) {
          for (j = 0; j < 2; j++) {
            for (k = 0; k < 2; k++) {
              printf("%d\n", x[i][j][k]);
            }
          }
        }  
      }"""

    checkResults(code, true)
  }

  "A 3d array with different sizes" should "print the correct results" in {
    val code = """
      void main() {
        int x[2][4][6];
        int i, j, k = 0;
        int count = 0;

        for (i = 0; i < 2; i++) {
          for (j = 0; j < 4; j++) {
            for (k = 0; k < 6; k++) {
              x[i][j][k] = count;
              count += 1;
            }
          }
        }

        for (i = 0; i < 2; i++) {
          for (j = 0; j < 4; j++) {
            for (k = 0; k < 6; k++) {
              printf("%d\n", x[i][j][k]);
            }
          }
        }
      }"""

    checkResults(code, true)
  }
}

class ArrayTest extends StandardTest {

  "Array sanity check" should "print the correct results" in {
    val code = """
      void main() {
        char s[100] = "hello";
        if(s == &s[0]) printf("true.\n");
        if(s == &s) printf("true.\n");
      }"""

    checkResults(code)
  }

  "A trivial array assignment" should "print the correct results" in {
    val code = """
      void main() {
        int x[5];
        x[2] = 5;
        printf("%d\n", x[2]);
      }"""

    checkResults(code)
  }

  "An array sized with a long long variable" should "print the correct results" in {
    val code = """
      void main() {
        long long size = 5L;
        int x[size];
        x[2] = 5;
        printf("%d\n", x[2]);
      }"""

    checkResults(code)
  }

  "An array sized with a short variable" should "print the correct results" in {
    val code = """
      void main() {
        short size = 5;
        int x[size];
        x[2] = 5;
        printf("%d\n", x[2]);
      }"""

    checkResults(code)
  }

  "An array indexed by different typed indexes" should "print the correct results" in {
    val code = """
      void main() {
        long long y = 2;
        short z = 3;
        int x[5];
        x[y] = 5;
        x[z] = 10;
        printf("%d %d\n", x[y], x[z]);
      }"""

    checkResults(code)
  }
  
  "An array with dimensions from a binary expr" should "print the correct results" in {
    val code = """
      void main() {
        int x[5*5];
        x[23] = 5;
        printf("%d\n", x[23]);
      }"""

    checkResults(code)
  }
  
  "assignment operators on an array element" should "print the correct results" in {
    val code = """
      void main() {
        int x[5] = {1, 2, 3, 4, 5};
        x[0] += 1;
        x[1] -= 1;
        x[2] *= 1;
        x[3] ^= 1;
        printf("%d %d %d %d\n", x[0], x[1], x[2], x[3]);
      }"""

    checkResults(code)
  }
  
  "A trivial array binary expression" should "print the correct results" in {
    val code = """
      void main() {
        int x[5];
        x[2] = 5;
        x[3] = 3;
        printf("%d\n", x[2] * x[3]);
      }"""

    checkResults(code)
  }
  
  "An array subscript with advanced binary expression" should "print the correct results" in {
    val code = """
      void main() {
        int x[5];
        int y = 2;
        x[1] = 3;
        x[3] = 12;
        printf("%d\n", x[y - 2 + x[1]]);
      }"""

    checkResults(code)
  }
  
  "An array prefixed subscript" should "print the correct results" in {
    val code = """
      void main() {
        int x[5] = {3, 68, 44, 29, 45};
        int y = 0;
        printf("%d %d\n", x[++y], y);
      }"""

    checkResults(code)
  }
  
  "An array postfixed subscript" should "print the correct results" in {
    val code = """
      void main() {
        int x[5] = {3, 68, 44, 29, 45};
        int y = 0;
        printf("%d %d\n", x[y++], y);
      }"""

    checkResults(code)
  }

  "An array being set to a single non-zero value" should "print the correct results" in {
    val code = """
      void main() {
        int x[5] = {3};
        printf("%d %d %d %d\n", x[0], x[1], x[2], x[3], x[4]);
      }"""

    checkResults(code)
  }

  "An array being set to a single non-zero value 2d" should "print the correct results" in {
    val code = """
      void main() {
        int x[2][3][4] = { { {1, 2, 3, 4}, {1, 2, 3, 4}, {1, 2, 3, 4} },
                           { {1, 2, 3, 4}, {1, 2, 3, 4}, {1, 2, 3, 4} } };

        printf("%d\n", x[0][0][0]);
        printf("%d\n", *(x[0][0]));
        printf("%d\n", **(x[0]));
        printf("%d\n", **(x[1]));
        printf("%d\n", *(x[0][0]));
        printf("%d\n", *(x[0][1]));
        printf("%d\n", *(x[0][2]));

        printf("%d\n", x[0][0][0]);
        printf("%d\n", x[0][0][1]);
        printf("%d\n", x[0][0][2]);
        printf("%d\n", x[0][0][3]);

        printf("%d\n", x[0][0][5]);
        printf("%d\n", x[0][0][6]);
        printf("%d\n", x[0][0][7]);
        printf("%d\n", x[0][0][8]);

        printf("%d\n", x[0][0][9]);
        printf("%d\n", x[0][0][10]);
        printf("%d\n", x[0][0][11]);
        printf("%d\n", x[0][0][12]);

        printf("%d\n", x[0][0][13]);
        printf("%d\n", x[0][0][14]);
        printf("%d\n", x[0][0][15]);
        printf("%d\n", x[0][0][16]);

        printf("%d\n", *x[0][0]);
        printf("%d\n", *x[0][1]);
        printf("%d\n", *x[0][2]);

        printf("%d\n", *x[1][0]);
        printf("%d\n", *x[1][1]);
        printf("%d\n", *x[1][2]);

        printf("%d\n", **x[0]);
        printf("%d\n", **x[1]);

      }"""

    checkResults(code)
  }

  "indexes are the plus operator" should "print the correct results" in {
    val code = """
      void main() {
        int z[5] = {1,2,3,4,5};
        int x = 2;

        printf("%d %d %d\n", z[x], x[z], 2[z]);
      }"""

    checkResults(code)
  }
}
