package mfe.guides;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mfe.MapFilterExt.*;
import static mfe.guides.GuideSeqImage.*;

public class BaseGuide{
    public transient String name;
    public Color color = new Color(Color.salmon).a(0.5f);
    public Vec2 off = new Vec2();
    public float rotDegree = 0f;
    public boolean enable = true, axis = false;
    public transient Table buttons = new Table();
    public static final Rect drawRect = new Rect(), tileRect = new Rect();

    public BaseGuide(){
        name = "BaseDebug";
        buttons.background(Styles.black3);
        buttons.defaults().size(buttonSize).pad(2f);
    }

    public void draw(){
        /*
        if(axis) drawAxis();
        Draw.color(Color.white, 0.5f);
        Fill.rect(BaseGuide.drawRect);
         */
    }

    public void drawAxis(){
        Draw.color(Color.black, 0.8f);
        Lines.stroke(8f);
        Vec2 xpos = Tmp.v1.set(1.5f * getW(), 0f).rotate(rotDegree);//xpos
        Vec2 xneg = Tmp.v2.set(Tmp.v1).rotate90(0).rotate90(0);//xneg
        Vec2 o = Tmp.v3.set(iposx() + xt2i(off.x + 0.5f), iposy() + yt2i(off.y + 0.5f));//origin
        Lines.line(o.x + xneg.x, o.y + xneg.y, o.x + xpos.x, o.y + xpos.y);//xaxis
        xpos.rotate90(0);
        xneg.rotate90(0);
        Lines.line(o.x + xneg.x, o.y + xneg.y, o.x + xpos.x, o.y + xpos.y);//yaxis
    }

    public void buildConfigure(Table table){
        table.clear();
        table.background(Styles.flatOver);
        table.image().fillX().color(Color.white).height(2f);
        table.row();
        table.defaults().padLeft(4f).padRight(4f);

        table.table(title -> {
            title.background(Styles.black);
            title.button(name, titleTogglet, () -> enable = !enable).grow().pad(2f).with(b -> {
                b.update(() -> b.getLabel().setColor(color.r, color.g, color.b, color.a * (enable ? 1f : 0.5f)));
            }).checked(enable).minWidth(200f);

            title.button("" + Iconc.pick, Styles.flatt, () -> Vars.ui.picker.show(color, true, c -> color.set(c)))
                    .update(b -> b.getLabel().setColor(color)).size(buttonSize).pad(2f);

            title.button("" + Iconc.up, Styles.flatt, () -> {
                int self = guides.indexOf(this);
                int tgt = Mathf.clamp(self - 1, 0, guides.size - 1);
                guides.swap(self, tgt);
                rebuild();
            }).size(buttonSize).pad(2f);

            title.button("" + Iconc.down, Styles.flatt, () -> {
                int self = guides.indexOf(this);
                int tgt = Mathf.clamp(self + 1, 0, guides.size - 1);
                guides.swap(self, tgt);
                rebuild();
            }).size(buttonSize).pad(2f);

            title.button("" + Iconc.cancel, Styles.flatt, () -> Vars.ui.showConfirm("Delete " + Core.bundle.get(name) + " ?", () -> {
                onRemove();
                guides.remove(this);
                rebuild();
            })).size(buttonSize).pad(2f).get().getLabel().setColor(Color.scarlet);
        }).growX();

        table.row();

        table.add(buttons).left().fill();

        table.row();

        table.table(this::buildContent).fill().get().background(Styles.black6);
    }

    /** Add offset fields */
    public void buildOffsetConfigure(Table table, Runnable changed){
        table.background(Styles.black3);
        addDragableFloatInput.get(x -> {off.x = x; if(changed != null) changed.run();}, () -> off.x, table.add(Iconc.move + "dx").get(), table.add(new TextField()).size(110f, 18f).get());
        addDragableFloatInput.get(r -> {rotDegree = r; if(changed != null) changed.run();}, () -> rotDegree, table.add(Iconc.rotate + "rot").get(), table.add(new TextField()).size(110f, 18f).get());
        table.row();
        addDragableFloatInput.get(y -> {off.y = y; if(changed != null) changed.run();}, () -> off.y, table.add(Iconc.move + "dy").get(), table.add(new TextField()).size(110f, 18f).get());
    }

    public void buildContent(Table table){}

    public void onRemove(){}

    /**project coordinates from imageTiles to graph*/
    public Vec2 projt2g(Vec2 p){
        return p.sub(off).rotate(-rotDegree);
    }

    /**project coordinates from graph to imageTiles*/
    public Vec2 projg2t(Vec2 p){
        return p.rotate(rotDegree).add(off);
    }

    /** @return x scaled from ui to tile*/
    public static float xi2t(float x){
        return x * getIW() / getW();
    }

    /** @return y scaled from ui to tile*/
    public static float yi2t(float y){
        return y * getIH() / getH();
    }

    /** @return x scaled from tile to ui*/
    public static float xt2i(float x){
        return x / getIW() * getW();
    }

    /** @return y scaled from tile to ui*/
    public static float yt2i(float y){
        return y / getIH() * getH();
    }

    public static float getW(){
        return guidesImage.getWidth();
    }

    public static float getH(){
        return guidesImage.getHeight();
    }

    /** map width */
    public static float getIW(){
        return guidesImage.imageWidth;
    }

    /** map height */
    public static float getIH(){
        return guidesImage.imageHeight;
    }
    public static void updViewRect(){
        var view = Vars.ui.editor.getView();
        //camera view
        drawRect.set(Math.max(iposx(), view.x), Math.max(iposy(), view.y),
                Math.min(getW(), Math.min(view.getWidth() - iposx() + view.x, getW() + iposx() - view.x)),
                Math.min(getH(), Math.min(view.getHeight() - iposy() + view.y, getH() + iposy() - view.y)));
        tileRect.set(Math.max(xi2t(-iposx() + view.x), 0), Math.max(yi2t(-iposy() + view.y), 0),
                xi2t(drawRect.width), yi2t(drawRect.height));
    }

    public static float iposx(){
        return guidesImage.x;
    }

    public static float iposy(){
        return guidesImage.y;
    }
}
