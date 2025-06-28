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
        public BaseDialog selectDialog = new BaseDialog("@guide.adddialog");
        protected ScrollPane paneList;
        public boolean needsRebuild = true;
        public boolean minimize = false;
        public Table list = new Table();

        public GuideListPop(){
            buildSelect();
            build();
            rebuildList();
            setTransform(true);
        }

        public void inject(){
            var view = ui.editor.getView();
            RefUtils.setValue(view, "image", guidesImage);
            view.parent.addChild(this);
            setPositionInScreen(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f);
            shown = true;
        }

        public void build(){
            clear();
            table(tit -> {
                tit.setBackground(Styles.black3);
                tit.margin(2f);
                tit.add("" + Iconc.move).size(buttonSize).with(this::addDragPopupListener).labelAlign(Align.center);
                tit.button(b -> {
                    b.label(() -> (minimize ? "[gray]" : "[accent]") + "MFE Guides");
                }, Styles.nonet, () -> {
                    minimize = !minimize;
                }).grow().height(buttonSize);
                tit.label(() -> "Mem:" + (int)(Core.app.getJavaHeap() / 1024 / 1024)).color(Tmp.c1.set(Color.green).a(0.7f)).style(Styles.outlineLabel).width(0.5f).fontScale(0.7f).labelAlign(Align.bottomRight).fill();

                tit.button("" + Iconc.add, Styles.cleart, () -> selectDialog.show()).size(buttonSize);

                tit.button("" + Iconc.copy, Styles.cleart, () -> Core.app.setClipboardText(JsonIO.write(guides))).size(buttonSize).with(tb -> UIUtils.tooltip(tb, "导出到剪切板"));;

                tit.button("" + Iconc.paste, Styles.cleart, () -> {
                    try{
                        clearGuides();
                        guides.set(JsonIO.read(Seq.class, Core.app.getClipboardText()));
                        setNeedsRebuild();
                    }catch(Throwable e){
                        ui.showException(e);
                        Log.err(e);
                    }
                }).size(buttonSize).with(tb -> UIUtils.tooltip(tb, "清空并导入剪切板"));;

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
                }).with(tb -> UIUtils.tooltip(tb, "在末尾导入剪切板"));

                tit.button("" + Iconc.cancel, Styles.cleart, () -> ui.showConfirm("确定清除所有图像吗?", () -> {
                    clearGuides();
                    setNeedsRebuild();
                })).size(buttonSize).with(tb -> tb.getLabel().setColor(Color.scarlet));

                tit.button("" + Iconc.list, Styles.cleart, () -> GuideSchematics.schematicsDialog.show()).size(buttonSize).with(tb -> UIUtils.tooltip(tb, "打开图像蓝图库"));
            });

            row();

            add(new MCollapser(t -> {
                t.pane(list).with(p -> {
                    paneList = p;
                    p.setScrollingDisabled(false, false);
                    p.exited(() -> {
                        if(p.hasScroll()){
                            Core.scene.setScrollFocus(ui.editor.getView());
                        }
                    });
                });
            }, minimize).setCollapsed(true, () -> minimize)).maxSize(400f, 500f);
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
            for(var guide : guides){
                list.table(guide::buildConfigure).padBottom(2f).growX();
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
