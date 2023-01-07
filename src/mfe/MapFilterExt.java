package mfe;

import arc.*;
import arc.func.*;
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

    public static void initStyles(){
        titleTogglet = new TextButton.TextButtonStyle(Styles.squareTogglet);
        titleTogglet.up = Styles.black;
    }
}
