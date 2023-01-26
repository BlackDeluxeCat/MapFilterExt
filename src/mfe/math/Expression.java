package mfe.math;

import arc.math.*;
import arc.struct.*;
import arc.util.*;

import java.util.regex.*;

public class Expression{
    /**signs of variables, used for checking.
     * variable instances get from {@link ExpressionHandler}*/
    public transient Seq<String> vars = new Seq<>();
    /**RPN in stack.*/
    protected transient Seq<Object> rpn = new Seq<>();
    //split before/behind any number, x, (, )
    protected static Pattern splitPattern = Pattern.compile(" |(?=[(),])|(?<=[(),])");  //just use space.
    protected static Matcher varMatcher = Pattern.compile("^[a-zA-Z_\\u4e00-\\u9fa5][0-9a-zA-Z_\\u4e00-\\u9fa5]*$").matcher("");
    /**Stack everything including op, constants, user variable instances.*/
    protected transient FloatSeq stk = new FloatSeq(), args = new FloatSeq();
    public transient boolean vaild = false;
    public String expression = "";

    public Expression(){
    }

    /**parse a string to RPN.*/
    public boolean parse(String str, ExpressionHandler handler){
        this.expression = str;
        rpn.clear();
        vars.clear();
        int arys = 0, consumes = 0, braceL = 0, braceR = 0;
        Seq<Object> seq = new Seq<>();
        try{
            String[] sps = splitPattern.split(str);

            for(String sp : sps){

                boolean matched = false;
                if(sp.equals("(")){
                    seq.add(sp);
                    braceL += 1;
                    continue;
                }
                
                if(sp.equals(",") || sp.equals(")")){
                    while(!seq.isEmpty()){
                        Object top = seq.pop();
                        if(top.equals("(") || top.equals(",")) break;
                        rpn.add(top);
                    }
                    if(sp.equals(",")){
                        seq.add(sp);
                    }else{
                        braceR += 1;
                    }
                    continue;
                }

                for(var op : Ops.values()){
                    if(op.match(sp)){
                        if(op.op0 != null){
                            rpn.add(op.op0.get());   //constant
                            arys += 1;
                        }else{
                            while(!seq.isEmpty() && seq.peek() instanceof Ops prev && prev.l > op.l){
                                rpn.add(seq.pop());
                            }
                            seq.add(op);
                        }
                        consumes += Mathf.maxZero(op.ary - 1);
                        matched = true;
                        break;
                    }
                }
                if(matched) continue;

                if(sp.isEmpty()) continue;

                if(Strings.canParseFloat(sp)){//constants
                    rpn.add(Float.parseFloat(sp));
                    arys += 1;
                }else if(varMatcher.reset(sp).matches()){//user vars
                    rpn.add(handler.getVar(sp));
                    vars.addUnique(sp);
                    arys += 1;
                }
            }

            while(!seq.isEmpty()){
                rpn.add(seq.pop());
            }
        }catch(Exception err){
            vaild = false;
            Log.err("Failure parsing expression: " + str, err);
            return false;
        }
        vaild = (arys - consumes == 1) && (braceL == braceR);
        return vaild;
    }

    /**get result with a vars map.*/
    public float get(){
        if(!vaild) return 0f;
        stk.clear();
        for(int i = 0; i < rpn.size; i++){
            Object obj = rpn.get(i);
            if(obj instanceof Variable svar) stk.add(svar.v);//user vars
            if(obj instanceof Float f) stk.add(f);//constants
            if(obj instanceof Ops ops){//operations
                args.clear();
                for(int ig = 0; ig < ops.ary; ig++){
                    args.add(stk.pop());
                }
                if(ops.op1 != null) stk.add(ops.op1.get(args.pop()));
                if(ops.op2 != null) stk.add(ops.op2.get(args.pop(), args.pop()));
                if(ops.op3 != null) stk.add(ops.op3.get(args.pop(), args.pop(), args.pop()));
            }
        }
        return stk.get(stk.size - 1);
    }
}