package mfe.guides;

import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;

public class CurveGuide extends ExpressionGuide{
    public CurveGuide(){
        super();
        name = "@guide.curve";
    }

    @Override
    public void buildContent(Table table){
        table.table(t -> buildOffsetConfigure(t, () -> graphChanged = true));

        table.row();

        table.table(tline -> {
            tline.add("C(x,y): ");
            if(exp.expression.length() == 0) exp.parse("x - y", this);
            tline.field(exp.expression, s -> {
                if(exp.parse(s, this)) onExpressionUpdate(varsTable);
            }).update(f -> {
                f.color.set(exp.vaild ? Color.white : Color.scarlet);
            }).growX();
            tline.add("=0");
            tline.row();
            tline.add();
            tline.label(() -> exp.expression).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();

        table.row();

        table.table(tfill -> {
            tfill.add("d(x,y)=");
            if(strokeexp.expression.length() == 0) strokeexp.parse("1", this);
            tfill.field(strokeexp.expression, s -> {
                if(strokeexp.parse(s, this)) onExpressionUpdate(varsTable);
            }).update(f -> {
                f.color.set(strokeexp.vaild ? Color.white : Color.scarlet);
            }).growX();
            tfill.row();
            tfill.add();
            tfill.label(() -> strokeexp.expression).height(0.1f).color(Color.clear).padLeft(50f);
        }).left();

        table.row();

        onExpressionUpdate(varsTable);
        table.add(varsTable).fill();
    }

    @Override
    public void consTiles(Cons<Vec2> cons, float step){
        super.consTiles(cons, step);
        //re-parse for imported data init.
        if(!exp.vaild) exp.parse(exp.expression, this);
        if(!strokeexp.vaild) strokeexp.parse(strokeexp.expression, this);
        if(!exp.vaild || !strokeexp.vaild) return;

        for(float x = 0; x < getIW(); x += step){
            for(float y = 0; y < getIH(); y += step){
                projt2g(ptile.set(x, y));
                if(polar){
                    varx.v = Mathf.mod(ptile.angleRad() + 2*Mathf.pi, 2*Mathf.pi);//theta
                    vary.v = ptile.len();//rho
                }else{
                    varx.v = ptile.x;
                    vary.v = ptile.y;
                }
                //高差法: 求函数值z=C(x,y)是否落在指定0的邻域内. 零点法效果不好.
                float dz = strokeexp.get();
                if(Mathf.zero(dz)) continue;//zero stroke should be skipped
                float cz = exp.get();
                if(centerStroke && cz <= 0.5f * dz && cz >= -0.5f * dz){
                    cons.get(pcur.set(x, y));
                }else if(cz >= 0f && cz <= dz){
                    cons.get(pcur.set(x, y));
                }
            }
        }
    }
}
