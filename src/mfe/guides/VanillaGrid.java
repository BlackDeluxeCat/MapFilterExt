package mfe.guides;

import arc.graphics.*;
import arc.graphics.g2d.*;

public class VanillaGrid extends BaseGuide{
    public VanillaGrid(){
        name = "Vanilla Grid";
        color.set(Color.gray);
    }

    @Override
    public void draw(){
        float iw = getIW(), ih = getIH();
        float xspace = (getW() / iw);
        float yspace = (getH() / ih);
        float s = 1f;

        int minspace = 10;

        int jumpx = (int)(Math.max(minspace, xspace) / xspace);
        int jumpy = (int)(Math.max(minspace, yspace) / yspace);

        Draw.color(color);
        for(int x = 0; x <= iw; x += jumpx){
            Fill.crect((int)(iposx() + xspace * x - s), iposy() - s, 2, getH() + (x == ih ? 1 : 0));
        }

        for(int y = 0; y <= ih; y += jumpy){
            Fill.crect(iposx() - s, (int)(iposy() + y * yspace - s), getW(), 2);
        }
    }
}
