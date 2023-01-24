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

    protected transient boolean graphChanged = true;
    protected transient float drawStep = 0.5f;
    protected transient Interval timer = new Interval();
    protected transient Pixmap image;
    protected transient Texture texture;
    protected transient TextureRegion region;

    public boolean detailGraph = true, centerStroke = true, polar = false;
    protected static final IntSeq tilesSort = new IntSeq();
    /**Handle vars.*/
    protected Seq<Variable> vars = new Seq<>();
    protected transient Variable varx, vary;
    protected transient Table varsTable;

    public ExpressionGuide(){
        varx = new Variable("x");
        vary = new Variable("y");
        name = "@guide.expression";

        varsTable = new Table();

        buttons.button("" + Iconc.move, titleTogglet, () -> axis = !axis).checked(axis).name("axis");
        buttons.button(polar ? uiCoordsysPolar : uiCoordsysRect, Styles.flati, buttonSize, () -> {}).with(b -> {
            b.clicked(() -> {
                polar = !polar;
                b.getStyle().imageUp = (polar ? uiCoordsysPolar : uiCoordsysRect);
                graphChanged = true;
            });
        }).name("polar");
        buttons.button(detailGraph ? uiStep025 : uiStep05, Styles.flati, buttonSize, () -> {}).with(b -> {
            b.clicked(() -> {
                detailGraph = !detailGraph;
                drawStep = detailGraph ? 0.25f : 0.5f;
                b.getStyle().imageUp = (detailGraph ? uiStep025 : uiStep05);
                graphChanged = true;
            });
        }).name("detailStep");
        buttons.button(centerStroke ? uiStrokeCenter : uiStrokeAdd, Styles.flati, buttonSize, () -> {}).with(b -> {
            b.clicked(() -> {
                centerStroke = !centerStroke;
                b.getStyle().imageUp = (centerStroke ? uiStrokeCenter : uiStrokeAdd);
                graphChanged = true;
            });
        }).name("centerStroke");
        buttons.button("" + Iconc.fill, Styles.flatt, () -> ui.showConfirm("Fill with " + editor.drawBlock.localizedName + " ?", this::fill)).name("fill");
    }

    @Override
    public void drawAxis(){
        if(polar){
            Draw.color(Color.black, 0.8f);
            Lines.stroke(8f);
            Vec2 xpos = Tmp.v1.set(1.5f * getW(), 0f).rotate(rotDegree);//xpos
            Vec2 xneg = Tmp.v2.set(Tmp.v1).rotate90(0).rotate90(0);//xneg
            Vec2 o = Tmp.v3.set(iposx() + xt2i(off.x + 0.5f), iposy() + yt2i(off.y + 0.5f));//origin
            Lines.line(o.x + xneg.x, o.y + xneg.y, o.x + xpos.x, o.y + xpos.y);//xaxis
            xpos.rotate90(0).limit(yt2i(10f));
            xneg.rotate90(0).limit(yt2i(10f));
            Lines.line(o.x + xneg.x, o.y + xneg.y, o.x + xpos.x, o.y + xpos.y);//yaxis
            for(int i = 1; i < getIH() / 50f; i++){
                Lines.circle(o.x, o.y, xt2i(i * 50f));
            }
        }else{
            super.drawAxis();
        }
    }

    @Override
    public void draw(){
        if(axis) drawAxis();

        updGraph();//if graph get modified, set graphChanged = true;

        Draw.color(color);
        Draw.rect(region, iposx() + getW() / 2f + xt2i(0.5f - drawStep/2f), iposy() + getH() / 2f + yt2i(0.5f - drawStep/2f), getW(), getH());
    }

    public void fill(){
        float brush = editor.brushSize;
        editor.brushSize = 1f;
        tilesSort.clear();

        consTiles(p -> tilesSort.add(Point2.pack((int)p.x, (int)p.y)), 1f);

        //rotated coordinates says it needs sorting!
        tilesSort.each(pos -> {
            editor.drawBlocks(Point2.x(pos), Point2.y(pos), false, editor.drawBlock.isOverlay(), t -> true);
        });

        editor.brushSize = brush;
        editor.flushOp();
    }

    protected transient Vec2 pcur = new Vec2(), pstk = new Vec2(), ptile = new Vec2(), pcps = new Vec2();
    protected transient Rect line = new Rect();
    /**Project tile view to graph and check whether each point is close to curve and should invoke the consumer or not.*/
    public void consTiles(Cons<Vec2> cons, float step){
        //re-parse for imported data init.
        if(!exp.vaild) exp.parse(exp.expression, this);
        if(!strokeexp.vaild) strokeexp.parse(strokeexp.expression, this);
        if(!exp.vaild || !strokeexp.vaild) return;

        for(float x = 0; x < getIW(); x += step){
            for(float y = 0; y < getIH(); y += step){
                projt2g(ptile.set(x, y));
                if(polar){
                    varx.v = Mathf.mod(ptile.angleRad() + 2*Mathf.pi, 2*Mathf.pi);
                    pcur.set(exp.get(), 0f).rotateRad(varx.v);
                    pstk.set(strokeexp.get(), 0f);
                    if(Mathf.zero(pstk.x)) continue;//zero stroke should be skipped
                    pstk.rotateRad(varx.v);

                    if(centerStroke) pcur.mulAdd(pstk, -1f / 2f);
                    pstk.add(pcur);
                }else{
                    varx.v = ptile.x;
                    pcur.set(ptile.x, exp.get());
                    pstk.set(ptile.x, strokeexp.get());
                    if(Mathf.zero(pstk.y)) continue;//zero stroke should be skipped

                    if(centerStroke) pcur.sub(0f, pstk.y / 2f);
                    pstk.y += pcur.y;
                }

                float agl = pcps.set(pstk).sub(pcur).angle();
                pcps.rotate(-(agl - 90));
                Tmp.v1.set(ptile).sub(pcur).rotate(-(agl - 90));
                if(line.set(-step/2f, -step/2f, step, pcps.len() + step).contains(Tmp.v1)){
                    cons.get(ptile.set(x, y));
                }
            }
        }
    }

    @Override
    public void buildContent(Table table){
        table.table(t -> buildOffsetConfigure(t, () -> graphChanged = true));

        table.row();

        table.table(tline -> {
            tline.add("f(x)=");
            if(exp.expression.length() == 0) exp.parse("x", this);
            tline.field(exp.expression, s -> {
                if(exp.parse(s, this)) onExpressionUpdate(varsTable);
            }).update(f -> {
                f.color.set(exp.vaild ? Color.white : Color.scarlet);
            }).growX();
            tline.row();
            tline.add();
            tline.label(() -> exp.expression).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();

        table.row();

        table.table(tfill -> {
            tfill.add("s(x)=");
            if(strokeexp.expression.length() == 0) strokeexp.parse("4", this);
            tfill.field(strokeexp.expression, s -> {
                if(strokeexp.parse(s, this)) onExpressionUpdate(varsTable);
            }).update(f -> {
                f.color.set(strokeexp.vaild ? Color.white : Color.scarlet);
            }).growX();
            tfill.row();
            tfill.add();
            tfill.label(() -> strokeexp.expression).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();

        table.row();

        onExpressionUpdate(varsTable);
        table.add(varsTable).fill();
    }

    public void onExpressionUpdate(Table t){
        graphChanged = true;

        t.clear();
        vars.removeAll(v -> exp.vaild && strokeexp.vaild && !(exp.vars.contains(v.n) || strokeexp.vars.contains(v.n)));
        final int[] c = {0};
        vars.each(var -> {
            addDragableFloatInput.get(v -> {
                var.v = v;
                graphChanged = true;
                }, () -> var.v, t.add(var.n + "=").right().get(), t.add(new TextField()).get());
            if(Mathf.mod(++c[0], 2) == 0) t.row();
        });
    }

    public void updGraph(){
        float step = drawStep;
        if(!graphChanged) return;
        if(!timer.get(10f)) return;
        graphChanged = false;

        if(image == null) image = new Pixmap((int)(getIW() / step), (int)(getIH() / step));
        if(image.width != (int)(getIW() / step) || image.height != (int)(getIH() / step)){
            image.dispose();
            image = new Pixmap((int)(getIW() / step), (int)(getIH() / step));
        }
        image.fill(Color.clear);
        consTiles(p -> image.set((int)(p.x / step), image.height - (int)(p.y / step) - 1, Color.whiteRgba), step);

        if(texture != null) texture.dispose();
        texture = new Texture(image);
        region = new TextureRegion(texture);
    }

    @Override
    public void onRemove(){
        if(image != null) image.dispose();
        if(texture != null) texture.dispose();
    }

    @Override
    public Variable getVar(String vname){
        if(vname.equals("x")) return varx;
        if(vname.equals("y")) return vary;

        var var = vars.find(v -> v.n.equals(vname));
        if(var == null){
            vars.add(var = new Variable(vname));
        }
        return var;
    }
}
