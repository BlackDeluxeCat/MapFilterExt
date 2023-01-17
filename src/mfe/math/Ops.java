package mfe.math;

import arc.math.*;

public enum Ops{
    e("e", () -> Mathf.E),
    pi("pi", () -> Mathf.pi),
    add("+", (a,b) -> a+b, 1),
    sub("-", (a,b) -> a-b, 1),
    mul("*", (a,b) -> a*b, 2),
    div("/", (a,b) -> a/b, 3),
    pow("^", Mathf::pow, 4),
    log("log", Mathf::log, 5),
    ln("ln", a -> Mathf.log(a, Mathf.E), 5),
    lg("lg", a -> Mathf.log(a, 10), 5),
    abs("abs", Math::abs, 5),
    sgn("sgn", a -> a == 0f ? 0f : a > 0f ? 1f : -1f, 5),
    sign("sign", Mathf::sign, 5),
    floor("floor", Mathf::floor, 5),
    ceil("ceil", Mathf::ceil, 5),
    round("round", a -> Mathf.round(a), 5),
    mod("mod", Mathf::mod, 5),
    max("max", Math::max, 5),
    min("min", Math::min, 5),
    len("len", Mathf::len, 5),
    sin("sin", a -> Mathf.sin(a), 5),
    cos("cos", Mathf::cos, 5),
    tan("tan", a -> Mathf.tan(a, 1f, 1f), 5),
    asin("arcsin", a -> (float)Math.asin(a), 5),
    acos("arccos", a -> (float)Math.acos(a), 5),
    atan("arctan", a -> (float)Math.atan(a), 5),
    ;
    public final String symbol;
    public final float l;
    public final Op2 op2;
    public final Op1 op1;
    public final Op0 op0;
    public final int ary;

    Ops(String symbol, Op0 op){
        this.symbol = symbol;
        l = Float.MAX_VALUE;
        op0 = op;
        op1 = null;
        op2 = null;
        ary = 0;
    }

    Ops(String symbol, Op1 op, float lvl){
        this.symbol = symbol;
        l = lvl;
        op0 = null;
        op1 = op;
        op2 = null;
        ary = 1;
    }

    Ops(String symbol, Op2 op, float lvl){
        this.symbol = symbol;
        l = lvl;
        op0 = null;
        op1 = null;
        op2 = op;
        ary = 2;
    }

    public boolean match(String str){
        return str.equals(this.symbol);
    }

    public interface Op2{
        float get(float a1, float a2);
    }

    public interface Op1{
        float get(float a);
    }

    public interface Op0{
        float get();
    }
}