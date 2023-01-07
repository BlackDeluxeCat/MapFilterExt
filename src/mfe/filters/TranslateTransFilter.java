package mfe.filters;

import mindustry.gen.*;
import mindustry.maps.filters.*;

public class TranslateTransFilter extends MI2UGenerateFilter{
    public float offX = 0f, offY = 0f;
    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
                new FilterOptions.SliderFieldOption("offsetX", () -> offX, f -> offX = f, -100f, 100f, 1f),
                new FilterOptions.SliderFieldOption("offsetY", () -> offY, f -> offY = f, -100f, 100f, 1f)
        };
    }

    @Override
    public char icon(){
        return Iconc.add;
    }

    @Override
    public void apply(GenerateInput in){
        if(in.x == 0 && in.y == 0){
            if(transConsumer != null){
                transeq.clear();
                transConsumer = null;
            }
            transeq.addLast(vec2 -> vec2.add(offX, offY));
        }
    }
}
