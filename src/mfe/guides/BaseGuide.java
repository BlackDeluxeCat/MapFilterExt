package mfe.guides;

import arc.graphics.*;
import arc.graphics.g2d.*;
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
    public Vec2 off = new Vec2();
    public Color color = new Color(Color.salmon);
    public boolean enable = true, axis = false;
    public Table buttons = new Table();
    protected static final Rect drawRect = new Rect(), tileRect = new Rect();

    public BaseGuide(){
        name = "Base: sin";
        buttons.background(Styles.black3);
        buttons.defaults().size(24f).pad(2f);
        buttons.button("" + Iconc.cancel, Styles.flatt, () -> Vars.ui.showConfirm("Delete " + name + " ?", () -> {
            guides.remove(this);
            rebuild();
        })).get().getLabel().setColor(Color.scarlet);
        buttons.button("" + Iconc.move, titleTogglet, () -> axis = !axis).checked(axis);
        buttons.button("" + Iconc.pick, Styles.flatt, () -> Vars.ui.picker.show(color, true, c -> color.set(c))).update(b -> b.getLabel().setColor(color));
    }

    public void draw(){
        if(axis) drawAxis();
        Draw.color(Color.white, 0.5f);
        Fill.rect(BaseGuide.drawRect);
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
        table.defaults().pad(3f).left();

        table.table(title -> {
            title.button(name, titleTogglet, () -> enable = !enable).growX().pad(2f).with(b -> {
                b.update(() -> b.getLabel().setColor(color.r, color.g, color.b, color.a * (enable ? 1f : 0.4f)));
            }).checked(enable).fill();
            title.row();
            title.add(buttons).left().fill();
        }).growX();

        table.row();

        table.table(this::buildOffset);
        table.row();
        table.table(this::buildContent).get().background(Styles.black6);
    }

    /** Add offset fields */
    public void buildOffset(Table table){
        table.background(Styles.black3);
        Label lx, ly;
        TextField fx, fy;
        TextField.TextFieldFilter filter = (field, c) -> TextField.TextFieldFilter.floatsOnly.acceptChar(field, c) || ((c == '-' && field.getCursorPosition() == 0 && !field.getText().contains("-")));
        lx = table.add("Δx:").get();
        fx = table.field("0", filter, s -> off.x = Strings.parseFloat(s)).size(120f, 18f).get();
        table.row();
        ly = table.add("Δy:").get();
        fy = table.field("0", filter, s -> off.y = Strings.parseFloat(s)).size(120f, 18f).get();
        //gesture
        lx.addListener(new ElementGestureListener(){
            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                super.pan(event, x, y, deltaX, deltaY);
                cancelScroll();
                off.x = Mathf.floor(off.x + deltaX / 2f);
                fx.setText(String.valueOf(off.x));
            }
        });
        ly.addListener(new ElementGestureListener(){
            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                super.pan(event, x, y, deltaX, deltaY);
                cancelScroll();
                off.y = Mathf.floor(off.y + deltaX / 2f);
                fy.setText(String.valueOf(off.y));
            }
        });
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
