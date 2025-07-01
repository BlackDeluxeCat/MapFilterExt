package mfe.guides;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mt.*;
import mt.ui.*;
import mt.utils.*;

import static mfe.MapFilterExt.*;
import static mindustry.Vars.*;

/**
 * 地图编辑器的辅助线
 */
public class GuideSeqImage extends GridImage{
    public static GuideSeqImage guidesImage = new GuideSeqImage(0, 0);
    public Seq<BaseGuide> guides = new Seq<>(true);
    public GuideListPop cfgPop = new GuideListPop();
    public int imageWidth, imageHeight;

    public GuideSeqImage(int w, int h){
        super(w, h);
        imageHeight = h;
        imageWidth = w;
    }

    @Override
    public void setImageSize(int w, int h){
        super.setImageSize(w, h);
        if(imageWidth == w && imageHeight == h) return;
        imageHeight = h;
        imageWidth = w;
        guides.each(g -> g.onResize());
    }

    @Override
    public void draw(){
        BaseGuide.updViewRect();
        guides.each(g -> {
            if(g.enable) g.draw();
        });
    }

    public void clearGuides(){
        guides.each(BaseGuide::onRemove);
        guides.clear();
    }

    public class GuideListPop extends MPopup{
        public BaseDialog selectDialog = new BaseDialog("@guide.addgraph");
        protected ScrollPane paneList;
        public boolean needsRebuild = true;
        public boolean minimize = false;
        public Table list = new Table();

        public GuideListPop(){
            buildSelect();
            build();
            rebuildList();
        }

        public void inject(){
            var view = ui.editor.getView();
            RefUtils.setValue(view, "image", guidesImage);
            view.parent.addChild(this);
            setPositionInScreen(0, 0);
            shown = true;
            rebuildList();
        }

        public void build(){
            clear();
            table(tit -> {
                tit.setBackground(Styles.black3);
                tit.margin(2f);
                tit.add(" " + Iconc.move + " MFE Graph").with(this::addDragPopupListener).height(buttonSize).growX();
                tit.label(() -> "Mem:" + (int)(Core.app.getJavaHeap() / 1024 / 1024)).color(Tmp.c1.set(Color.green).a(0.7f)).style(Styles.outlineLabel).width(0.5f).fontScale(0.7f).labelAlign(Align.bottomRight).fill();
                tit.button(b -> b.label(() -> (minimize ? Iconc.downOpen : Iconc.upOpen) + ""), Styles.cleart, () -> minimize = !minimize).size(buttonSize);
            }).growX();

            row();

            add(new MCollapser(t -> {
                t.setBackground(Styles.black3);
                t.table(tit -> {
                    tit.margin(2f);

                    tit.button("" + Iconc.add, Styles.cleart, () -> selectDialog.show()).size(buttonSize);

                    tit.button("" + Iconc.copy, Styles.cleart, () -> Core.app.setClipboardText(JsonIO.write(guides))).size(buttonSize).with(tb -> UIUtils.tooltip(tb, "@guide.exportclipboard"));;

                    tit.button("" + Iconc.paste, Styles.cleart, () -> {
                        try{
                            clearGuides();
                            guides.set(JsonIO.read(Seq.class, Core.app.getClipboardText()));
                            setNeedsRebuild();
                        }catch(Throwable e){
                            ui.showException(e);
                            Log.err(e);
                        }
                    }).size(buttonSize).with(tb -> UIUtils.tooltip(tb, "@guide.importclipboardclean"));;

                    tit.button("" + Iconc.paste, Styles.cleart, () -> {
                        try{
                            guides.add(JsonIO.read(Seq.class, Core.app.getClipboardText()));
                            setNeedsRebuild();
                        }catch(Throwable e){
                            ui.showException(e);
                            Log.err(e);
                        }
                    }).size(buttonSize).with(tb -> {
                        tb.add("+").color(Color.green).style(Styles.outlineLabel).width(0.1f).with(l -> {
                            l.setAlignment(Align.bottomRight);
                            l.setFontScale(0.75f);
                        }).fillY();
                    }).with(tb -> UIUtils.tooltip(tb, "@guide.importclipboardtail"));

                    tit.button("" + Iconc.trash, Styles.cleart, () -> ui.showConfirm("@guide.cleanconfirm", () -> {
                        clearGuides();
                        setNeedsRebuild();
                    })).size(buttonSize).with(tb -> tb.getLabel().setColor(Color.scarlet));

                    tit.image().color(Tmp.c1.set(Color.gray).a(0.5f)).growY().width(2f).pad(0, 8, 0, 8);

                    tit.button("" + Iconc.list, Styles.cleart, () -> GuideSchematics.schematicsDialog.show()).size(buttonSize).with(tb -> UIUtils.tooltip(tb, "@guide.schematics"));

                    tit.image().color(Tmp.c1.set(Color.gray).a(0.5f)).growY().width(2f).pad(0, 8, 0, 8);

                    tit.button("" + Iconc.fileText, Styles.cleart, () -> guidesImage.guides.set(JsonIO.read(Seq.class, editor.tags.get("MFE.Graphs")))).size(buttonSize).with(tb -> UIUtils.tooltip(tb, "@guide.maptag.read")).disabled(tb -> !editor.tags.containsKey("MFE.Graphs"));

                    tit.button("" + Iconc.save, Styles.cleart, () -> editor.tags.put("MFE.Graphs", JsonIO.write(guidesImage.guides))).size(buttonSize).with(tb -> UIUtils.tooltip(tb, "@guide.maptag.write"));

                    tit.button("" + Iconc.trash, Styles.cleart, () -> editor.tags.remove("MFE.Graphs")).size(buttonSize).with(tb -> UIUtils.tooltip(tb, "@guide.maptag.delete")).disabled(tb -> !editor.tags.containsKey("MFE.Graphs"));
                }).left();

                t.row();

                t.pane(list).with(p -> {
                    paneList = p;
                    p.setScrollingDisabled(false, false);
                    p.exited(() -> {
                        if(p.hasScroll()){
                            Core.scene.setScrollFocus(ui.editor.getView());
                        }
                    });
                }).growX().left();
            }, minimize).setCollapsed(true, () -> minimize).setDirection(true, true)).maxSize(400f, 500f).growX();
        }

        public void setNeedsRebuild(){
            needsRebuild = true;
        }

        public void cancelScroll(){
            if(paneList != null) paneList.cancel();
        }

        @Override
        public void act(float delta){
            if(needsRebuild) rebuildList();
            keepInScreen();
            super.act(delta);
        }

        public void rebuildList(){
            list.clear();
            list.defaults().pad(2f).padBottom(4f);
            for(var guide : guides){
                list.table(guide::buildConfigure).growX();
                list.row();
            }
            needsRebuild = false;
        }

        public void buildSelect(){
            selectDialog.addCloseButton();
            selectDialog.cont.pane(t -> {
                t.background(Styles.grayPanel);
                t.defaults().width(500f).minHeight(100f).top();
                t.button("@guide.vanilla", Styles.flatBordert, () -> {
                    guides.add(new VanillaGridGuide());
                    selectDialog.hide();
                    setNeedsRebuild();
                });
                t.button("@guide.expression", Styles.flatBordert, () -> {
                    guides.add(new ExpressionGuide());
                    selectDialog.hide();
                    setNeedsRebuild();
                });
                t.button("@guide.curve", Styles.flatBordert, () -> {
                    guides.add(new CurveGuide());
                    selectDialog.hide();
                    setNeedsRebuild();
                });

                t.row();

                t.labelWrap("@guide.vanilla.info").fill();
                t.labelWrap("@guide.expression.info").fill();
                t.labelWrap("@guide.curve.info").fill();
            }).growY();
        }
    }
}
