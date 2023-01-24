package mfe.guides;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mfe.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mfe.MapFilterExt.*;
import static mindustry.Vars.ui;

/**
 * 地图编辑器的辅助线
 */
public class GuideSeqImage extends GridImage{
    public static GuideSeqImage guidesImage = new GuideSeqImage(0, 0);
    public static Seq<BaseGuide> guides = new Seq<>(true);
    public static BaseDialog selectDialog = new BaseDialog("Select New Guide Type");
    protected static ScrollPane paneList;
    protected static Table main = new Table();
    protected static boolean minimize = false;
    public transient int imageWidth, imageHeight;

    public GuideSeqImage(int w, int h){
        super(w, h);
        imageHeight = h;
        imageWidth = w;
    }

    @Override
    public void setImageSize(int w, int h){
        super.setImageSize(w, h);
        imageHeight = h;
        imageWidth = w;
    }

    @Override
    public void draw(){
        BaseGuide.updViewRect();
        guides.each(g -> {
            if(g.enable) g.draw();
        });
    }

    public static void rebuild(){
        build(main);
        MI2Utils.setValue(ui.editor.getView(), "image", guidesImage);
        var view = ui.editor.getView();
        view.parent.addChild(main);
        main.setFillParent(true);
        main.top().left();
    }

    public static void build(Table table){
        table.clear();
        table.table(tit -> {
            tit.button(String.valueOf(step), Styles.flatt, () -> {}).with(tb -> {
                tb.getLabel().setWrap(false);
                tb.clicked(() -> {
                    if(step == 1f) step = 0.1f;
                    else if(step == 0.1f) step = 0.01f;
                    else if(step == 0.01f) step = 10f;
                    else step = 1f;
                });
            }).update(tb -> tb.setText(String.valueOf(step)));
            tit.button("MFE-Guides", titleTogglet, () -> {
                minimize = !minimize;
                rebuild();
            }).with(b -> b.getLabel().setWrap(false)).checked(minimize).growX();
            tit.label(() -> "Mem:" + (int)(Core.app.getJavaHeap() / 1024 / 1024)).color(Tmp.c1.set(Color.green).a(0.7f)).style(Styles.outlineLabel).name("memory").width(0.5f).with(l -> {
                l.setAlignment(Align.bottomRight);
                l.setFontScale(0.7f);
            }).fillY();

            tit.button("" + Iconc.add, Styles.flatBordert, GuideSeqImage::showSelect).size(buttonSize);

            tit.button("" + Iconc.copy, Styles.flatBordert, () -> Core.app.setClipboardText(JsonIO.write(guides))).size(buttonSize);

            tit.button("" + Iconc.paste, Styles.flatBordert, () -> {
                try{
                    clearGuides();
                    guides.set(JsonIO.read(Seq.class, Core.app.getClipboardText()));
                    rebuild();
                }catch(Throwable e){
                    ui.showException(e);
                    Log.err(e);
                }
            }).size(buttonSize);

            tit.button("" + Iconc.paste, Styles.flatBordert, () -> {
                try{
                    guides.add(JsonIO.read(Seq.class, Core.app.getClipboardText()));
                    rebuild();
                }catch(Throwable e){
                    ui.showException(e);
                    Log.err(e);
                }
            }).size(buttonSize).with(tb -> {
                tb.add("+").color(Color.green).style(Styles.outlineLabel).name("memory").width(0.1f).with(l -> {
                    l.setAlignment(Align.bottomRight);
                    l.setFontScale(0.75f);
                }).fillY();
            });

            tit.button("" + Iconc.cancel, Styles.flatBordert, () -> ui.showConfirm("Confirm Delete All Guides?", () -> {
                clearGuides();
                rebuild();
            })).size(buttonSize).with(tb -> tb.getLabel().setColor(Color.scarlet));
        }).fillX().minWidth(200f);

        if(minimize) return;

        table.row();

        table.pane(p1 -> {
            for(var guide : guides){
                p1.table(guide::buildConfigure).padBottom(2f).growX();
                p1.row();
            }
        }).with(p -> {
            paneList = p;
            p.setFlickScroll(false);
            p.setScrollBarPositions(true, false);
            p.setScrollingDisabledX(true);
            p.exited(() -> {
                if(p.hasScroll()){
                    Core.scene.setScrollFocus(ui.editor.getView());
                }
            });
        }).fillX().maxHeight(500f);
    }

    public static void cancelScroll(){
        if(paneList != null) paneList.cancel();
    }

    public static void buildSelect(){
        selectDialog.addCloseButton();
        selectDialog.cont.pane(t -> {
            t.background(Styles.grayPanel);
            t.defaults().width(500f).minHeight(100f).top();
            //selectDialog.cont.button("@guide.base", Styles.flatBordert, () -> addNewGuide(BaseGuide::new));
            t.button("@guide.vanilla", Styles.flatBordert, () -> addNewGuide(VanillaGridGuide::new));
            t.button("@guide.expression", Styles.flatBordert, () -> addNewGuide(ExpressionGuide::new));
            t.button("@guide.curve", Styles.flatBordert, () -> addNewGuide(CurveGuide::new));

            t.row();

            t.labelWrap("@guide.vanilla.info").fill();
            t.labelWrap("@guide.expression.info").fill();
            t.labelWrap("@guide.curve.info").fill();
        }).growY();
    }

    private static void clearGuides(){
        guides.each(BaseGuide::onRemove);
        guides.clear();
    }

    private static void addNewGuide(Prov<BaseGuide> getter){
        guides.add(getter.get());
        rebuild();
        selectDialog.hide();
    }

    public static void showSelect(){
        selectDialog.show();
    }
}
