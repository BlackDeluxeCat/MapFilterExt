package mfe.guides;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mfe.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.ui;

/**
 * 地图编辑器的辅助线
 */
public class GuideSeqImage extends GridImage{
    public static GuideSeqImage guidesImage = new GuideSeqImage(0, 0);
    public static Seq<BaseGuide> guides = new Seq<>(true);
    public static BaseDialog selectDialog = new BaseDialog("Select New Guide Type");
    protected static ScrollPane paneList;
    protected static Table main = new Table(), mainParent;
    protected static boolean minimize = false, pop = true;
    public int imageWidth, imageHeight;

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
        if(pop){
            var view = ui.editor.getView();
            view.parent.addChild(main);
            main.setFillParent(true);
            main.top().left();
        }else{
            main.setFillParent(false);
            Table toolt = (Table)((Table)ui.editor.getChildren().first()).getChildren().first();
            if(toolt.find(e -> e == mainParent) == null){
                toolt.row();
                toolt.table(tt -> mainParent = tt).growX();
            }
            mainParent.clear();
            mainParent.add(main).growX();
            main.update(null);
        }
    }

    public static void build(Table table){
        table.clear();
        table.table(tit -> {
            tit.button(pop ? "<" : ">", Styles.flatBordert, () -> {
                pop = !pop;
                rebuild();
            }).size(28f).get().getLabel().setColor(Color.gray);
            tit.button("MFE-Guides", MapFilterExt.titleTogglet, () -> {
                minimize = !minimize;
                rebuild();
            }).grow().checked(minimize);
            tit.button("+", Styles.flatBordert, GuideSeqImage::showSelect).size(28f);
        }).fillX().minWidth(178f);

        if(minimize) return;
        table.row();

        table.pane(p1 -> {
            for(var guide : guides){
                p1.table(guide::buildConfigure).padBottom(2f).growX();
                p1.row();
            }
        }).with(p -> {
            paneList = p;
            p.setFadeScrollBars(true);
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
        selectDialog.cont.defaults().size(150f, 64f);
        selectDialog.cont.button("@guide.base", Styles.flatBordert, () -> addNewGuide(BaseGuide::new));
        selectDialog.cont.button("@guide.vanilla", Styles.flatBordert, () -> addNewGuide(VanillaGrid::new));
        selectDialog.cont.button("@guide.expression", Styles.flatBordert, () -> addNewGuide(ExpressionGuide::new));
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
