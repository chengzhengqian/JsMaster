package com.serendipity.chengzhengqian.jsmaster;

public class TestClass {
    Integer a=12;
    public static Integer b=123;
    public static TestClass create(Integer a){
        TestClass c=new TestClass(a);
        c.a=a;
        return c;
    }
    public TestClass(Integer a){
        this.a=a;
    }
    public static Integer get(TestClass c){
        return c.a;
    }
    public int a(){return a;};
    public int setInt(int b){a=b;return a;}
}
