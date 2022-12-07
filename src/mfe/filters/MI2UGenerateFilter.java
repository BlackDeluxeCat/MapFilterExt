package mfe.filters;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;

public class MI2UGenerateFilter extends GenerateFilter{
    //transformation stack
    public static Queue<Cons<Vec2>> transeq = new Queue<>();
    //transformation consumer
    @Nullable public static MI2UGenerateFilter transConsumer = null;
    //region stack
    public static Seq<Rect> regionseq = new Seq<>();
    //region consumer
    @Nullable public static MI2UGenerateFilter regionConsumer = null;

    @Override
    public FilterOption[] options() {
        return new FilterOption[0];
    }

    @Override
    public char icon(){
        return Iconc.blockLogicDisplay;
    }

    public void preConsume(GenerateInput in){
        if(in.x == 0 && in.y == 0){
            if(transConsumer == null && !transeq.isEmpty()) {
                transConsumer = this;
            }
            if(regionConsumer == null && !regionseq.isEmpty()) {
                regionConsumer = this;
            }
        }
    }

    public float noise(GenerateInput in, float sclX, float sclY, float offX, float offY, float rotate, float mag, float octaves, float persistence){
        Vec2 vec = Tmp.v1;
        vec.set(in.x, in.y).rotate(rotate);
        return Simplex.noise2d(seed, octaves, persistence, 1f, (vec.x + offX) / sclX, (vec.y + offY) / sclY) * mag;
    }

    public float noise(float x, float y, float mag, float octaves, float persistence){
        return Simplex.noise2d(seed, octaves, persistence, 1f, x, y) * mag;
    }
}
