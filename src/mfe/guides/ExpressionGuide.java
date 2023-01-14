package mfe.guides;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mfe.math.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mfe.MapFilterExt.*;
import static mindustry.Vars.*;

public class ExpressionGuide extends BaseGuide implements ExpressionHandler{
    public Expression exp = new Expression(), strokeexp = new Expression();
    public boolean staticStep = true, centerStroke = true, polar = false;
    protected static final IntSet tiles = new IntSet(512);
    protected static final IntSeq tilesSort = new IntSeq();
    /**Handle vars.*/
    protected final Seq<Variable> vars = new Seq<>();
    protected Variable varx, vary;
    protected Table varsTable;

    public ExpressionGuide(){
        vars.add(varx = new Variable("x"));
        vars.add(vary = new Variable("y"));
        name = "@guide.expression";
        exp.parse("x", this);
        strokeexp.parse("4", this);

        varsTable = new Table();
        buildVars(varsTable);

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
            Lines.line(iposx() + xt2i(off.x), iposy() + yt2i(off.y - 5f), iposx() + xt2i(off.x), iposy() + yt2i(off.y + 5f));
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
        varx.value = x;
        float fy = exp.get(), prey;
        while(x < getIW()){
            step = Mathf.clamp(step, 0.02f, 0.5f);

            //polar won't apply offset to x in function, and x is scaled to [0, 2pi]
            float inputx = polar ? (2 * Mathf.pi * x / getIW()):x-off.x;
            varx.value = inputx;
            float sy = (strokeexp.vaild ? strokeexp.get() : 1f);

            if(polar){
                point.set(1f, 0f).scl(fy - (centerStroke ? sy/2f : 0f)).rotateRad(inputx).rotate(rotDegree).add(off);
                point2.set(1f, 0f).scl(fy + (centerStroke ? sy/2f : sy)).rotateRad(inputx).rotate(rotDegree).add(off);
            }else{
                point.set(x, fy - (centerStroke ? sy/2f : 0f)).sub(off).rotate(rotDegree).add(off);
                point2.set(x, fy + (centerStroke ? sy/2f : sy)).sub(off).rotate(rotDegree).add(off);
            }
            c3.get(point, point2, step);

            x += step;
            prey = exp.get() + (polar?0f:off.y);
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
            tline.field(exp.text, s -> {
                if(exp.parse(s, this)) buildVars(varsTable);
            }).update(f -> {
                f.color.set(exp.vaild ? Color.white : Color.scarlet);
            }).growX();
            tline.row();
            tline.add();
            tline.label(() -> exp.text).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();

        table.row();

        table.table(tfill -> {
            tfill.add("s(x)=");
            tfill.field(strokeexp.text, s -> {
                if(strokeexp.parse(s, this)) buildVars(varsTable);
            }).update(f -> {
                f.color.set(strokeexp.vaild ? Color.white : Color.scarlet);
            }).growX();
            tfill.row();
            tfill.add();
            tfill.label(() -> strokeexp.text).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();

        table.row();

        table.add(varsTable).fill();
    }

    void buildVars(Table t){
        t.clear();
        vars.removeAll(v -> !(v.name.equals("x") || v.name.equals("y") || exp.vars.contains(v.name) || strokeexp.vars.contains(v.name)));
        final int[] c = {0};
        vars.each(var -> {
            if(var.name.equals("x") || var.name.equals("y")) return;
            addDragableFloatInput.get(v -> var.value = v, () -> var.value, t.add(var.name + "=").right().get(), t.add(new TextField()).get());
            if(Mathf.mod(++c[0], 2) == 0) t.row();
        });
    }

    @Override
    public Variable getVar(String vname){
        var var = vars.find(v -> v.name.equals(vname));
        if(var == null){
            vars.add(var = new Variable(vname));
        }
        return var;
    }
}
