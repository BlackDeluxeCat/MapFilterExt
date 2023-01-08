package mfe.guides;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mfe.MapFilterExt.titleTogglet;
import static mfe.guides.GuideSeqImage.*;

public class BaseGuide{
    public String name;
    public Color color = new Color(Color.salmon).a(0.5f);
    public Vec2 off = new Vec2();
    public float rotDegree = 0f;
    public boolean enable = true, axis = false;
    public Table buttons = new Table();
    public static final Rect drawRect = new Rect(), tileRect = new Rect();

    public BaseGuide(){
        name = "BaseDebug";
        buttons.background(Styles.black3);
        buttons.defaults().size(24f).pad(2f);
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
        Lines.line(iposx(), iposy() + yt2i(off.y), iposx() + getW(), iposy() + yt2i(off.y));
        Lines.line(iposx() + yt2i(off.x), iposy(), iposx() + yt2i(off.x), iposy() + getH());
    }

    public void buildConfigure(Table table){
        table.clear();
        table.background(Styles.flatOver);
        table.image().fillX().color(Color.white).height(2f);
        table.row();
        table.defaults().pad(3f);

        table.table(title -> {
            title.background(Styles.black);
            title.button(name, titleTogglet, () -> enable = !enable).growX().pad(2f).with(b -> {
                b.update(() -> b.getLabel().setColor(color.r, color.g, color.b, color.a * (enable ? 1f : 0.5f)));
            }).checked(enable).width(176f);

            title.button("" + Iconc.pick, Styles.flatt, () -> Vars.ui.picker.show(color, true, c -> color.set(c)))
                    .update(b -> b.getLabel().setColor(color)).size(24f).pad(2f);

            title.button("" + Iconc.up, Styles.flatt, () -> {
                int self = guides.indexOf(this);
                int tgt = Mathf.clamp(self - 1, 0, guides.size - 1);
                guides.swap(self, tgt);
                rebuild();
            }).size(24f).pad(2f);

            title.button("" + Iconc.down, Styles.flatt, () -> {
                int self = guides.indexOf(this);
                int tgt = Mathf.clamp(self + 1, 0, guides.size - 1);
                guides.swap(self, tgt);
                rebuild();
            }).size(24f).pad(2f);

            title.button("" + Iconc.cancel, Styles.flatt, () -> Vars.ui.showConfirm("Delete " + name + " ?", () -> {
                guides.remove(this);
                rebuild();
            })).size(24f).pad(2f).get().getLabel().setColor(Color.scarlet);
        }).growX();

        table.row();

        table.add(buttons).left().fill();

        table.row();

        table.table(this::buildContent).get().background(Styles.black6);
    }

    /** Add offset fields */
    public void buildOffsetConfigure(Table table){
        table.background(Styles.black3);
        TextField.TextFieldFilter filter = (field, c) -> TextField.TextFieldFilter.floatsOnly.acceptChar(field, c) || ((c == '-' && field.getCursorPosition() == 0 && !field.getText().contains("-")));
        Cons3<Cons<Float>, Prov<Float>, String> builder = (setter, getter, name) -> {
            Label l = table.add(name).get();
            TextField f = table.field("0", filter, s -> setter.get(Strings.parseFloat(s))).size(120f, 18f).get();
            l.addListener(new ElementGestureListener(){
                @Override
                public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                    super.pan(event, x, y, deltaX, deltaY);
                    cancelScroll();
                    setter.get((float)Mathf.floor(getter.get() + deltaX / 2f));
                    f.setText(String.valueOf(getter.get()));
                }

                @Override
                public void tap(InputEvent event, float x, float y, int count, KeyCode button){
                    super.tap(event, x, y, count, button);
                    if(count >= 2){
                        setter.get(0f);
                        f.setText(String.valueOf(getter.get()));
                    }
                }
            });
        };
        builder.get(x -> off.x = x, () -> off.x, Iconc.move + "dx");
        table.row();
        builder.get(y -> off.y = y, () -> off.y, Iconc.move + "dy");
        table.row();
        builder.get(r -> rotDegree = r, () -> rotDegree, Iconc.rotate + "rot");
    }

    public void buildContent(Table table){}

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
