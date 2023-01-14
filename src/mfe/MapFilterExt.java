package mfe;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.math.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mfe.filters.*;
import mfe.guides.*;
import mindustry.game.EventType.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.mod.*;
import mindustry.ui.*;

import java.util.*;

import static mfe.guides.GuideSeqImage.*;
import static mindustry.Vars.ui;

public class MapFilterExt extends Mod{

    public MapFilterExt(){
        Events.on(ClientLoadEvent.class, e -> {
            initStyles();
            addFilters();
            ui.editor.shown(GuideSeqImage::rebuild);
            buildSelect();
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

    public static TextButton.TextButtonStyle titleTogglet;
    public static TextureRegionDrawable uiStepStatic, uiStepDynamic, uiStrokeCenter, uiStrokeAdd, uiCoordsysRect, uiCoordsysPolar;
    public static void initStyles(){
        titleTogglet = new TextButton.TextButtonStyle(Styles.squareTogglet);
        titleTogglet.up = Styles.black;
        uiStepStatic = new TextureRegionDrawable(Core.atlas.find("mapfilterext-ui-step-static"));
        uiStepDynamic = new TextureRegionDrawable(Core.atlas.find("mapfilterext-ui-step-dynamic"));
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
}
