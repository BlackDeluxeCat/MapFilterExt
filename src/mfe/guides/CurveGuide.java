package mfe.guides;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;

public class CurveGuide extends ExpressionGuide{
    public CurveGuide(){
        super();
        name = Core.bundle.get("guide.curve");
    }

    @Override
    public void buildContent(Table table){
        table.table(tline -> {
            tline.add("C(x,y): ");
            if(exp.expression.length() == 0) exp.parse("x - y", this);
            tline.field(exp.expression, s -> {
                exp.parse(s, this);
                onExpressionUpdate(expsTable);
            }).update(f -> {
                f.color.set(exp.valid ? Color.white : Color.scarlet);
            }).growX();
            tline.label(() -> centerStroke ? "@guide.curve.neighborhood.center" : "@guide.curve.neighborhood.add");
            tline.row();
            tline.add();
            tline.label(() -> exp.expression).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();

        table.row();

        table.table(tfill -> {
            tfill.add("d(x,y)=");
            if(strokeexp.expression.length() == 0) strokeexp.parse("1", this);
            tfill.field(strokeexp.expression, s -> {
                strokeexp.parse(s, this);
                onExpressionUpdate(expsTable);
            }).update(f -> {
                f.color.set(strokeexp.valid ? Color.white : Color.scarlet);
            }).growX();
            tfill.row();
            tfill.add();
            tfill.label(() -> strokeexp.expression).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();

        table.row();

        onExpressionUpdate(expsTable);
        table.add(expsTable).fill();
    }

    @Override
    public void consTiles(Cons<Vec2> cons, float step){
        //re-parse for imported data init.
        if(!exp.valid) exp.parse(exp.expression, this);
        if(!strokeexp.valid) strokeexp.parse(strokeexp.expression, this);
        if(!exp.valid || !strokeexp.valid) return;

        for(float x = 0; x < getIW(); x += step){
            for(float y = 0; y < getIH(); y += step){
                projt2g(ptile.set(x, y));
                if(polar){
                    varx.parse(Mathf.mod(ptile.angleRad() + 2*Mathf.pi, 2*Mathf.pi));//theta
                    vary.parse(ptile.len());//rho
                }else{
                    varx.parse(ptile.x);
                    vary.parse(ptile.y);
                }
                //高差法: 求函数值z=C(x,y)是否落在指定的区间内. 零点法效果不好.
                float dz = strokeexp.get();
                if(Mathf.zero(dz)) continue;//zero stroke should be skipped
                float cz = exp.get();
                if(centerStroke && cz <= dz && cz >= -dz){
                    cons.get(pcur.set(x, y));
                }else if(!centerStroke && cz >= 0f && cz <= dz){
                    cons.get(pcur.set(x, y));
                }
            }
        }
    }
}
