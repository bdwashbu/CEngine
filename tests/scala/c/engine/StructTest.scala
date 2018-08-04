package scala.c.engine

class StructTestStaging extends StandardTest {


  "pointer to struct with array member" should "print the correct results" in {
    val code = """

      struct Test {
        int y;
        int x;
        int length;
        int *z;
        struct Test *next;
      };

      struct Test *head = 0;

      void main() {
        int i = 0;
        struct Test x;
         x.y = 343;
         x.x = 543;
        struct Test *y = 0;
        int blah[] = {1,2,3,4,5,6,7,8,9,0};

        y = &x;
        y->z = malloc(40);
         memcpy(y->z, blah, 40);
         for (i = 0; i < 10; i++) {
           printf("%d\n", y->z[i]);
         }
        y->next = head;
        y->y = 465;
        printf("%d %d %d\n", x.y, y->y, y->next);
      }"""

    checkResults(code)
  }
}

class StructTest extends StandardTest {
  
  "initializer list populating a pointer" should "print the correct results" in {
    val code = """
      
      struct Test {
        int y;
        int x;
        struct Test *next;
      };

      void main() {
        struct Test x = {343, 543, 0};
        printf("%d %d %d\n", x.y, x.y, x.next);
      }"""

    checkResults(code)
  }

  "struct array init" should "print the correct results" in {
    val code = """

     typedef struct {
         int weight;
         int value;
         int count;
       } item_t;

       item_t items[] = {
         {9,   150,   1},
         {13,    35,   1}
       };

      void main() {
        printf("%d\n", items[0].weight);
        printf("%d\n", items[0].value);
        printf("%d\n", items[0].count);
      }"""

    checkResults(code)
  }

  "advanced struct init" should "print the correct results" in {
    val code = """

     typedef struct {
         char *name;
         int weight;
         int value;
         int count;
       } item_t;

       item_t items[] = {
         {"map",      9,   150,   1},
         {"compass",  13,    35,   1}
       };

      void main() {
        printf("%d\n", items[0].weight);
        printf("%s\n", items[0].name);
        printf("%d\n", items[0].value);
        printf("%d\n", items[0].count);
        printf("%d\n", items[1].weight);
        printf("%s\n", items[1].name);
        printf("%d\n", items[1].value);
        printf("%d\n", items[1].count);
      }"""

    checkResults(code)
  }

  "indexing with a variable" should "print the correct results" in {
    val code = """

      struct Test {
        int* data;
        int length;
      };

      void main() {
        struct Test a = {0};
        a.data = malloc(12);
        a.data[2] = 10;
        a.length = 2;
        printf("%d\n", a.data[a.length]);
      }"""

    checkResults(code)
  }
  
  "structure pointer typedef" should "print the correct results" in {
    val code = """
      
      typedef struct linked_node* lnode_p;
      
      struct list{
      	lnode_p first;
      };
      
      struct linked_node{
      	void* data;
      };
      
      typedef struct list * list_p;

      void main() {
        struct list x = {4};
        struct linked_node test = {14};
        x.first = &test;

        lnode_p y = x.first;
        void *before = y->data;
        x.first = 0;
        void *after = y->data;
        printf("%d\n", before != after);
        
      }"""

    checkResults(code)
  }
  
  "function which takes struct pointer" should "print the correct results" in {
    val code = """
      
      struct Test {
        int y;
        int x;
        struct Test *next;
      };
      
      typedef struct TypedefType {
        int y;
        int x;
        struct Test *next;
      } TypedefType;
      
      void modify(struct Test *test) {
         test->x = 15;
         test->y = test->x + 55;
      }
      
      void modify2(TypedefType *test) {
         test->next->x = 15;
         test->next->y = test->x + 55;
      }
      
      void main() {
        struct Test x = {343, 543, 0};
        TypedefType y = {1, 40, &x};
        printf("%d %d\n", x.y, x.y);
        printf("%d %d\n", y.y, y.y);
        modify(&x);
        modify2(&y);
        printf("%d %d\n", x.y, x.y);
        printf("%d %d\n", y.next->y, y.next->y);
      }"""

    checkResults(code)
  }
  
  "struct with function pointer field" should "print the correct results" in {
    val code = """
      #include <stdlib.h>

      struct Test {
        int (*testFcn)(int);
        void (*testFcn2)(void*);
      };
      
      int what(int x) {
         return x + 10;
      }
      
      void setFcn(struct Test *test) {
         test->testFcn = what;
         test->testFcn2 = free;
      }
      
      void passStruct(struct Test *test) {
         printf("%d\n", test->testFcn('d'));
         test->testFcn2(10);
      }
      
      void main() {
        struct Test a = {0};
        struct Test *b = &a;
        setFcn(b);
        printf("%d %d\n", a.testFcn(4), b->testFcn(57));
        passStruct(b);
      }"""

    checkResults(code)
  } 
  
  "struct ptr assignment" should "print the correct results" in {
    val code = """
      
      struct Test {
        int y;
        int z;
      };
      
      void main() {
        struct Test a = {1,2};
        struct Test *x;
        struct Test *y = &a;
        struct Test *z = y;
        x = y;
        printf("%d %d %d %d\n", x->y, x->z, y->z, z->z);
      }"""

    checkResults(code)
  } 
  
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
  
  "basic struct sizeof test" should "print the correct results" in {
    val code = """
      
      struct Test {
        int y;
        int z;
      };
      
      void main() {
        printf("%d\n", sizeof(struct Test));
      }"""

    checkResults(code)
  }

  "setting nested field test" should "print the correct results" in {
    val code = """

      struct Inner {
           int branches;
           int num_branches;
      };

      struct WithInner {
          struct Inner inner;
      };

      void main() {
        struct WithInner blah;
        struct WithInner blah2;

        blah2.inner.branches = 66;
        blah2.inner.num_branches = 10;

        //inner = blah2->inner;

        printf("%d\n", blah2.inner.branches);
        printf("%d\n", blah2.inner.num_branches);
      }"""

    checkResults(code)
  }

  "basic copying structure test" should "print the correct results" in {
    val code = """

      struct Test {
         const char *ptr;
         int len;
         int branches;
         int num_branches;
      };

      struct Inner {
           int branches;
           int num_branches;
      };

      struct WithInner {
          struct Inner inner;
      };

      void main() {
        struct Test x = {0, 10, 15, 20};
        struct Test y = x;
        struct Test z;

        struct Inner inner = {6546, 1232};

        z = x;

        struct WithInner blah;
        struct WithInner blah2;

        blah2.inner.branches = 66;
        blah2.inner.num_branches = 10;

        inner = blah2.inner;

        blah.inner = blah2.inner;

        printf("%d\n", inner.branches);
        printf("%d\n", inner.num_branches);

        printf("%d\n", blah.inner.branches);
        printf("%d\n", blah.inner.num_branches);

        printf("%d\n", y.ptr);
        printf("%d\n", y.len);
        printf("%d\n", y.branches);
        printf("%d\n", y.num_branches);

        printf("%d\n", z.ptr);
        printf("%d\n", z.len);
        printf("%d\n", z.branches);
        printf("%d\n", z.num_branches);


      }"""

    checkResults(code)
  }

  "advanced struct sizeof test" should "print the correct results" in {
    val code = """

        struct slre_cap {
           const char *ptr;
           int len;
         };

        struct bracket_pair {
         const char *ptr;
         int len;
         int branches;
         int num_branches;
       };

       struct branch {
         int bracket_index;
         const char *schlong;
       };

      struct Test {
         struct bracket_pair brackets[24];
         int num_brackets;
         struct branch branches[24];
         int num_branches;
         int num_caps;
         int flags;
       };

      void main() {

        struct Test test;

        printf("%d\n", sizeof(struct Test));
        printf("%d\n", sizeof(struct branch));
        printf("%d\n", sizeof(struct bracket_pair));
        printf("%d\n", sizeof(struct slre_cap));

        printf("%d\n", sizeof(test.brackets));
        printf("%d\n", sizeof(test.branches));
        printf("%d\n", sizeof(test.num_branches));
        printf("%d\n", sizeof(test.flags));
      }"""

    checkResults(code)
  }

  "setting a structure pointer equal to a pointer" should "print the correct results" in {
    val code = """
      
      struct Test {
        int y;
        int x;
        struct Test *next;
      };
      
      struct Test *head = 0;
      
      void main() {
        struct Test x = {0,0,0};
        head = x.next;
        printf("%d\n", head);
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

  "struct field offset test" should "not clobber" in {
    val code = """

     #include <stddef.h>
     #include <stdio.h>
     #include <stdlib.h>

     struct bracket_pair {
       const char *ptr;
       int len;
       int branches;
       int num_branches;
     };

     struct branch {
       int bracket_index;
       const char *schlong;
     };

      struct Test {
        struct bracket_pair brackets[30];
        int num_brackets;
        struct branch branches[30];
        int num_branches;
      };

      void main() {
        printf("%d %d %d %d\n", offsetof(struct Test, brackets),
                                offsetof(struct Test, num_brackets),
                                offsetof(struct Test, branches),
                                offsetof(struct Test, num_branches));
      }"""

    checkResults(code, true)
  }

  "struct field clobbering test" should "not clobber" in {
    val code = """
      struct bracket_pair {
        const char *ptr;
      };

      struct branch {
        int bracket_index;
      };

      struct Test {
        struct bracket_pair brackets[2];
        struct branch branches[2];
      };

      void main() {
        struct Test x;
        char string[] = "hello";
        x.brackets[0].ptr = string;
        x.branches[0].bracket_index = 50;

        printf("%d\n", x.brackets[0].ptr == string);
      }"""

    checkResults(code, true)
  }


  
  
}
