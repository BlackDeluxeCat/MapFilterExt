package mfe.guides;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;

import java.util.regex.*;

import static mfe.MapFilterExt.*;
import static mindustry.Vars.*;

public class ExpressionGuide extends BaseGuide{
    public Expression exp = new Expression(), strokeexp = new Expression();
    public boolean staticStep = true, centerStroke = true, polar = false;
    private static final IntSet tiles = new IntSet(512);
    private static final IntSeq tilesSort = new IntSeq();

    public ExpressionGuide(){
        name = "@guide.expression";
        exp.parse("x");
        strokeexp.parse("4");
        buttons.button("" + Iconc.move, titleTogglet, () -> axis = !axis).checked(axis);
        buttons.button(polar ? uiCoordsysPolar : uiCoordsysRect, Styles.flati, 24f, () -> {}).with(b -> {
            b.clicked(() -> {
                polar = !polar;
                b.getStyle().imageUp = (polar ? uiCoordsysPolar : uiCoordsysRect);
            });
        });
        buttons.button(staticStep ? uiStepStatic : uiStepDynamic, Styles.flati, 24f, () -> {}).with(b -> {
            b.clicked(() -> {
                staticStep = !staticStep;
                b.getStyle().imageUp = (staticStep ? uiStepStatic : uiStepDynamic);
            });
        });
        buttons.button(centerStroke ? uiStrokeCenter : uiStrokeAdd, Styles.flati, 24f, () -> {}).with(b -> {
            b.clicked(() -> {
                centerStroke = !centerStroke;
                b.getStyle().imageUp = (centerStroke ? uiStrokeCenter : uiStrokeAdd);
            });
        });
        buttons.button("" + Iconc.fill, Styles.flatt, () -> ui.showConfirm("Fill with " + editor.drawBlock.localizedName + " ?", this::fill));
    }

    @Override
    public void drawAxis(){
        if(polar){
            Draw.color(Color.black, 0.8f);
            Lines.stroke(8f);
            Lines.line(iposx(), iposy() + yt2i(off.y), iposx() + getW(), iposy() + yt2i(off.y));
            for(int i = 1; i < getIH() / 50f; i++){
                Lines.circle(iposx() + xt2i(off.x), iposy() + yt2i(off.y), xt2i(i * 50f));
            }
        }else{
            super.drawAxis();
        }
    }

    @Override
    public void draw(){
        if(axis) drawAxis();
        if(!exp.vaild) return;
        Draw.color(color);
        //Be careful! ix(), iy() is the offset of overlay element, add to final coords!
        cons((p1, p2, step) -> {
            if((tileRect.contains(p1.x, p1.y) || tileRect.contains(p2.x, p2.y))){
                Lines.stroke(xt2i(step));
                Lines.line(iposx() + xt2i(p1.x), iposy() + yt2i(p1.y), iposx() + xt2i(p2.x), iposy() + yt2i(p2.y));
            }
        });
    }

    public void fill(){
        float brush = editor.brushSize;
        editor.brushSize = 1f;
        tiles.clear();
        tilesSort.clear();

        cons((p1, p2, step) -> {
            Vec2 dv = Tmp.v3.set(p2).sub(p1).nor().scl(0.1f);
            if(dv.isZero(0.001f)) return;
            for(; Tmp.v4.set(p2).sub(p1).dot(dv) >= 0f; p1.add(dv)){
                //Log.info(p1.toString() + "|||" + p2.toString());
                if(p1.x <= getIW() + 1f && p1.x >= 0 && p1.y <= getIH() + 1f && p1.y >= 0){
                    if(Mathf.mod(p1.x, 1f) > 0.25f && Mathf.mod(p1.x, 1f) < 0.75f && Mathf.mod(p1.y, 1f) > 0.25f && Mathf.mod(p1.y, 1f) < 0.75f) tiles.add(Point2.pack(Mathf.floor(p1.x), Mathf.floor(p1.y)));
                }
            }
        });

        //rotated coordinates says it needs sorting!
        tiles.each(tilesSort::add);
        tilesSort.sort();
        tilesSort.each(pos -> {
            editor.drawBlocks(Point2.x(pos), Point2.y(pos), false, editor.drawBlock.isOverlay(), t -> true);
        });

        editor.brushSize = brush;
        editor.flushOp();
    }

    //for editor users, tile axis are more common.
    public void cons(Cons3<Vec2, Vec2, Float> c3){
        var point = Tmp.v1;
        var point2 = Tmp.v2;
        float x = 0f;
        float step = 0.1f;
        float fy = exp.get(x), prey;
        while(x < getIW()){
            step = Mathf.clamp(step, 0.02f, 0.5f);

            //polar won't apply offset to x in function, and x is scaled to [0, 2pi]
            float inputx = polar ? (2 * Mathf.pi * x / getIW()):x-off.x;
            float sy = (strokeexp.vaild ? strokeexp.get(inputx) : 1f);

            if(polar){
                point.set(1f, 0f).scl(fy - (centerStroke ? sy/2f : 0f)).rotateRad(inputx).rotate(rotDegree).add(off);
                point2.set(1f, 0f).scl(fy + (centerStroke ? sy/2f : sy)).rotateRad(inputx).rotate(rotDegree).add(off);
            }else{
                point.set(x, fy - (centerStroke ? sy/2f : 0f)).sub(off).rotate(rotDegree).add(off);
                point2.set(x, fy + (centerStroke ? sy/2f : sy)).sub(off).rotate(rotDegree).add(off);
            }
            c3.get(point, point2, step);

            x += step;
            prey = exp.get(inputx) + (polar?0f:off.y);
            if(staticStep){
                step = 0.1f;
            }else if(Math.abs(prey - fy) > 0.5f){
                step /= 2f;
            }else if(Math.abs(prey - fy) < 0.1f){
                step *= 2f;
            }
            fy = prey;
        }
    }

    @Override
    public void buildContent(Table table){
        table.table(this::buildOffsetConfigure);

        table.row();

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
