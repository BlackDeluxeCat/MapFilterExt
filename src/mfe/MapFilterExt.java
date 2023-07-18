package mfe;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.math.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.util.*;
import mfe.filters.*;
import mfe.guides.*;
import mfe.math.*;
import mi2.setting.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.mod.*;
import mindustry.ui.*;

import java.util.*;

import static mfe.guides.GuideSeqImage.*;
import static mindustry.Vars.editor;
import static mindustry.Vars.ui;

public class MapFilterExt extends Mod{
    public static float buttonSize = 32f;
    public static float step = 1f;
    public static ConfigHandler config;

    public MapFilterExt(){
        Events.on(ClientLoadEvent.class, e -> {
            initGuideClassJsonIO();
            config = ConfigHandler.request(this);
            GuideSchematics.load();
            initStyles();
            Time.run(30f, () -> {
                addFilters();
                ui.editor.shown(GuideSeqImage::rebuild);
                buildSelect();
                if(editor.getClass() != MapEditor.class) ui.showOkText("", Core.bundle.format("warning.novanillaeditor", editor.getClass().getName()), () -> {});
            });

        });
    }

    public static void addFilters(){
        addFilter(TranslateTransFilter::new);
        addFilter(ScalingTransFilter::new);
        addFilter(RotateTransFilter::new);
        addFilter(PolarTransFilter::new);
        addFilter(AppliedRegionFilter::new);
        addFilter(AdvancedNoiseFilter::new);
        addFilter(AdvancedOreFilter::new);
        addFilter(GridFilter::new);
        addFilter(CopyPasteFilter::new);
    }

    public static void addFilter(Prov<GenerateFilter> filter){
        var newArr = Arrays.copyOf(Maps.allFilterTypes, Maps.allFilterTypes.length + 1);
        newArr[Maps.allFilterTypes.length] = filter;
        Maps.allFilterTypes = newArr;
        Log.info("Adding New Filters... Filters Size: " + newArr.length);
        GenerateFilter ins = filter.get();
        JsonIO.json.addClassTag(Strings.camelize(ins.getClass().getSimpleName().replace("Filter", "")), ins.getClass());
    }

    public static void initGuideClassJsonIO(){
        JsonIO.json.addClassTag(BaseGuide.class.getSimpleName(), BaseGuide.class);
        JsonIO.json.addClassTag(ExpressionGuide.class.getSimpleName(), ExpressionGuide.class);
        JsonIO.json.addClassTag(VanillaGridGuide.class.getSimpleName(), VanillaGridGuide.class);
        JsonIO.json.addClassTag(CurveGuide.class.getSimpleName(), CurveGuide.class);
    }

    public static TextButton.TextButtonStyle titleTogglet;
    public static TextureRegionDrawable uiStep025, uiStep05, uiStrokeCenter, uiStrokeAdd, uiCoordsysRect, uiCoordsysPolar;
    public static void initStyles(){
        titleTogglet = new TextButton.TextButtonStyle(Styles.squareTogglet);
        titleTogglet.up = Styles.black;
        uiStep025 = new TextureRegionDrawable(Core.atlas.find("mapfilterext-ui-step-025"));
        uiStep05 = new TextureRegionDrawable(Core.atlas.find("mapfilterext-ui-step-05"));
        uiStrokeCenter = new TextureRegionDrawable(Core.atlas.find("mapfilterext-ui-stroke-center"));
        uiStrokeAdd = new TextureRegionDrawable(Core.atlas.find("mapfilterext-ui-stroke-add"));
        uiCoordsysRect = new TextureRegionDrawable(Core.atlas.find("mapfilterext-ui-coordsys-rect"));
        uiCoordsysPolar = new TextureRegionDrawable(Core.atlas.find("mapfilterext-ui-coordsys-polar"));
    }

    public static TextField.TextFieldFilter filter = (field, c) -> TextField.TextFieldFilter.floatsOnly.acceptChar(field, c) || ((c == '-' && field.getCursorPosition() == 0 && !field.getText().contains("-")));

    public static Cons4<Floatc, Floatp, Label, TextField> addDragableFloatInput = (setter, getter, l, f) -> {
        f.setText(String.valueOf(getter.get()));
        f.setFilter(filter);
        f.changed(() -> {
            if(f.isValid()){
                setter.get(Strings.parseFloat(f.getText()));
            }
        });
        l.addListener(new ElementGestureListener(){
            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                super.pan(event, x, y, deltaX, deltaY);
                cancelScroll();
                setter.get(Mathf.floor(getter.get() + deltaX / 2f * step));
                f.setText(Strings.fixed(getter.get(), 0));
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
}
