package mfe.guides;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;

import java.util.regex.*;

import static mfe.MapFilterExt.titleTogglet;

public class ExpressionGuide extends BaseGuide{
    public Expression exp = new Expression(), strokeexp = new Expression();
    public boolean strokeGraph = true;
    public ExpressionGuide(){
        name = "Expression";
        strokeexp.parse("4");
        buttons.button("" + Iconc.crafting, titleTogglet, () -> strokeGraph = !strokeGraph).checked(strokeGraph);
    }

    @Override
    public void draw(){
        if(axis) drawAxis();
        if(!exp.vaild) return;
        Draw.color(color);
        //Be careful! ix(), iy() is the offset of overlay element, add to final coords!
        cons((tx, ty, w, tmp) -> {
            float sy = (strokeGraph && strokeexp.vaild) ? yt2i(strokeexp.get(tx)) : 4f;
            if((tileRect.contains(tx, ty) || tileRect.contains(tx, sy)) && Math.abs(ty - sy) < getIH()){
                Fill.crect(iposx() + xt2i(tx + off.x), iposy() + yt2i(ty + off.y), w, sy);
            }
        });
    }

    public void fill(){
    }

    //for editor users, tile axis are more common.
    public void cons(Floatc4 c4){
        float x = 0f;
        float step = 0.2f;
        float lasty = exp.get(x), prey;
        while(x < getIW()){
            c4.get(x, lasty, step, 0f);
            x += Mathf.clamp(step, 0.01f, 0.5f);
            prey = exp.get(x);
            if(Math.abs(prey - lasty) > 0.5f){
                step /= 2f;
            }else if(Math.abs(prey - lasty) < 0.1f){
                step *= 2f;
            }
            lasty = prey;
        }
    }

    @Override
    public void buildContent(Table table){
        table.table(tline -> {
            tline.add("f(x)=");
            tline.field(exp.text, s -> exp.parse(s)).update(f -> {
                f.color.set(exp.vaild ? Color.white : Color.scarlet);
            }).growX();
            tline.row();
            tline.add();
            tline.label(() -> exp.text).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();

        table.row();

        table.table(tfill -> {
            tfill.add("s(x)=");
            tfill.field(strokeexp.text, s -> strokeexp.parse(s)).update(f -> {
                f.color.set(strokeexp.vaild ? Color.white : Color.scarlet);
            }).growX();
            tfill.row();
            tfill.add();
            tfill.label(() -> strokeexp.text).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();
    }

    public class Expression{
        /** just a sign of argument. */
        private static final FloatFloatf varx = f -> f;
        /** RPN in stack. */
        Seq<Object> rpn = new Seq<>();
        //static Pattern pattern = Pattern.compile("(?=(\\d+(\\.\\d*)?|x|\\(|\\)|[+\\-*\\\\^]|([A-WYZa-wyz]([A-Za-z0-9_]+)?)))");    //split before/behind any number, x, (, )
        static Pattern pattern = Pattern.compile(" |,|(?=x|\\(|\\))|(?<=x|\\(|\\))");  //just use space.
        FloatSeq stk = new FloatSeq();
        public boolean vaild = false;
        public String text;
        public Expression(){}

        /** parse a string to RPN. */
        public boolean parse(String str){
            this.text = str;
            rpn.clear();
            int arys = 0, consumes = 0, braceL = 0, braceR = 0;
            Seq<Object> seq = new Seq<>();
            try{
                String[] sps = pattern.split(str);

                for(String sp : sps){

                    boolean matched = false;
                    if(sp.equals("(")){
                        seq.add(sp);
                        braceL += 1;
                        continue;
                    }
                    if(sp.equals(")")){
                        while(!seq.isEmpty()){
                            Object top = seq.pop();
                            if(top.equals("(")) break;
                            rpn.add(top);
                        }
                        braceR += 1;
                        continue;
                    }
                    /*this sort brace params in a ordinal stack.
                    if(sp.equals(",")){
                        seq.add(rpn.pop());
                        continue;
                    }
                     */

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

                    if(sp.equals("x")){
                        rpn.add(varx);
                        arys += 1;
                        continue;
                    }
                    if(sp.isEmpty()) continue;
                    rpn.add(Float.valueOf(sp));
                    arys += 1;
                }

                while(!seq.isEmpty()){
                    rpn.add(seq.pop());
                }

                get(1f);
            }catch(Exception err){
                vaild = false;
                Log.err("Failure parsing expression: " + str, err);
                return false;
            }
            vaild = (arys - consumes == 1) && (braceL == braceR);
            return vaild;
        }

        /** get f(x) with float argument. */
        public float get(float x){
            if(!vaild) return 0f;
            stk.clear();
            for(int i = 0; i < rpn.size; i++){
                Object obj = rpn.get(i);
                if(obj == varx) stk.add(x);
                if(obj instanceof Float f) stk.add(f);
                if(obj instanceof Ops ops){
                    if(ops.op1 != null) stk.add(ops.op1.get(stk.pop()));
                    if(ops.op2 != null){
                        float b = stk.pop();
                        float a = stk.pop();
                        stk.add(ops.op2.get(a, b));
                    }
                }
            }
            return stk.get(stk.size - 1);
        }
    }

    public enum Ops{
        e("e", () -> Mathf.E),
        pi("pi", () -> Mathf.pi),
        add("+", (a,b) -> a+b, 1),
        sub("-", (a,b) -> a-b, 1),
        mul("*", (a,b) -> a*b, 2),
        div("/", (a,b) -> a/b, 2),
        pow("^", Mathf::pow, 3),
        log("log", Mathf::log,5),
        ln("ln", a -> Mathf.log(a, Mathf.E),5),
        abs("abs", Math::abs,5),
        floor("floor", Mathf::floor,5),
        ceil("ceil", Mathf::ceil,5),
        max("max", Math::max,5),
        min("min", Math::min,5),
        len("len", Mathf::len,5),
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
    }

    interface Op2{
        float get(float a1, float a2);
    }

    interface Op1{
        float get(float a);
    }

    interface Op0{
        float get();
    }
}
