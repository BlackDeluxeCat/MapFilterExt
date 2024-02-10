package mfe.editor;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.editor.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static mindustry.game.SpawnGroup.*;

public class MFEWaveInfoDialog extends BaseDialog{
    public static MFEWaveInfoDialog mfewave = new MFEWaveInfoDialog();
    Seq<SpawnGroup> groups = new Seq<>();
    Seq<SpawnGroup> selectedGroups = new Seq<>();
    WaveGraph graph = new WaveGraph();
    WaveCanvas canvas = new WaveCanvas();
    Table config;
    int search = -1;
    float updateTimer, updatePeriod = 1f;
    boolean batchEditing;
    boolean checkedSpawns;


    boolean showViewSettings;
    @Nullable
    UnitType filterType;
    boolean filterPayloads, filterItems, filterEffects;
    Sort sort = Sort.begin;
    boolean reverseSort = false;

    public MFEWaveInfoDialog(){
        super("@waves.title");

        shown(this::setup);
        hidden(() -> {
            if(state.isEditor() || state.isMenu()){
                //clean up invalid configs
                groups.each(g -> {
                    if(!(g.type instanceof Payloadc)) g.payloads = null;
                    if(g.type.itemCapacity == 0) g.items = null;
                });
                state.rules.spawns = groups;
            }
        });

        addCloseButton();

        buttons.button("@waves.edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@waves.edit");
            dialog.addCloseButton();
            dialog.setFillParent(false);
            dialog.cont.table(Tex.button, t -> {
                var style = Styles.cleart;
                t.defaults().size(280f, 64f).pad(2f);

                t.button("@waves.copy", Icon.copy, style, () -> {
                    ui.showInfoFade("@waves.copied");
                    Core.app.setClipboardText(maps.writeWaves(groups));
                    dialog.hide();
                }).disabled(b -> groups == null || groups.isEmpty()).marginLeft(12f).row();

                t.button("@waves.load", Icon.download, style, () -> {
                    try{
                        groups = maps.readWaves(Core.app.getClipboardText());
                        buildGroups();
                    }catch(Exception e){
                        e.printStackTrace();
                        ui.showErrorMessage("@waves.invalid");
                    }
                    dialog.hide();
                }).disabled(Core.app.getClipboardText() == null || !Core.app.getClipboardText().startsWith("[")).marginLeft(12f).row();

                t.button("@clear", Icon.none, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups.clear();
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f).row();

                t.button("@settings.reset", Icon.refresh, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups = JsonIO.copy(waves.get());
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f);
            });

            dialog.show();
        }).size(250f, 64f);

        if(experimental){
            buttons.button(Core.bundle.get("waves.random"), Icon.refresh, () -> {
                groups.clear();
                groups = Waves.generate(1f / 10f);
                buildGroups();
            }).width(200f);
        }
    }

    void setup(){
        groups = JsonIO.copy(state.rules.spawns.isEmpty() ? waves.get() : state.rules.spawns);
        if(groups == null) groups = new Seq<>();
        checkedSpawns = false;
        batchEditing = false;
        selectedGroups.clear();

        cont.clear();

        //main chart
        cont.table(main -> {
            main.stack(canvas,
                new Label("@waves.none"){{
                    visible(() -> groups.isEmpty());
                    this.touchable = Touchable.disabled;
                    setWrap(true);
                    setAlignment(Align.center, Align.center);
                }}, new Table(shell -> {
                    shell.visible(() -> !selectedGroups.isEmpty());
                    shell.pane(t -> {
                        config = t;
                        t.background(Tex.button);
                    });
                }).left().bottom(), new Table(shell -> {
                    shell.visible(() -> showViewSettings);
                    shell.pane(t -> {
                        t.background(Tex.button);
                        t.margin(16f);
                        t.add("@waveinfo.filters").height(40f).grow().row();
                        t.table(gf -> {
                            gf.defaults().size(40f).pad(4f);

                            gf.button(Icon.units, Styles.squarei, () -> showAnyContentsYouWant(groups.map(sg -> sg.type).asSet().toSeq().sort(type -> type.id), "", type -> {
                                filterType = type;
                                buildGroups();
                                canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                            })).update(b -> b.getStyle().imageUp = filterType != null ? new TextureRegionDrawable(filterType.uiIcon) : Icon.units);

                            gf.button(Icon.effect, Styles.squareTogglei, () -> {
                                filterEffects = !filterEffects;
                                buildGroups();
                                canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                            });

                            gf.button(Icon.box, Styles.squareTogglei, () -> {
                                filterItems = !filterItems;
                                buildGroups();
                                canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                            });

                            gf.button(Icon.itchioSmall, Styles.squareTogglei, () -> {
                                filterPayloads = !filterPayloads;
                                buildGroups();
                                canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                            });
                        }).row();

                        t.add("@waveinfo.sorters").height(40f).grow().row();

                        t.table(st -> {
                            st.defaults().pad(4f);
                            st.table(Tex.button, f -> {
                                for(Sort s : Sort.all){
                                    f.button("@waves.sort." + s, Styles.flatTogglet, () -> {
                                        sort = s;
                                        buildGroups();
                                        canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                                    }).size(80, 40f).checked(b -> s == sort);
                                }
                            }).row();
                            st.check("@waves.sort.reverse", b -> {
                                reverseSort = b;
                                buildGroups();
                                canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                            }).height(40f).checked(reverseSort);
                        }).row();
                    });
                }).right().bottom()
            ).grow().margin(16f);
        }).grow();

        cont.row();

        cont.table(tools -> {
            tools.defaults().minWidth(120f).height(60f).left();
            tools.button("@add", () -> {
                showAnyContentsYouWant(content.units().copy().removeAll(UnitType::isHidden), "title", type -> groups.add(new SpawnGroup(type)), null);
                buildGroups();
            });
            tools.check("@waveinfo.batchedit", b -> batchEditing = b);
            tools.table(s -> {
                s.image(Icon.zoom).padRight(8);
                s.field("", TextField.TextFieldFilter.digitsOnly, text -> {
                    search = Math.max(Strings.parseInt(text, 1) - 1, 0);
                    if(groups.any()) canvas.locateWave(search);
                }).growX().maxTextLength(8).get().setMessageText("@waves.search");
            });
            tools.add().growX().minWidth(0f);

            tools.button("-", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    scaleX(-1);
                }
            }).width(60f);
            tools.button("+", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    scaleX(1);
                }
            }).width(60f);
            tools.button(Icon.filter, () -> showViewSettings = !showViewSettings).checked(showViewSettings).width(60f);
        }).growX();

        //cont.add(graph = new WaveGraph()).grow();

        buildGroups();
    }

    boolean checkFilters(SpawnGroup group){
        if(filterType != null && group.type != filterType) return false;
        if(filterEffects && group.effect == null) return false;
        if(filterItems && group.items == null) return false;
        if(filterPayloads && group.payloads == null) return false;
        return true;
    }

    /**
     * Rebuild groups chart. Also rebuild config table.
     */
    void buildGroups(){
        canvas.clearChildren();
        float marginTop = 4f;
        int index = 0;

        if(groups != null){
            groups.sort(Structs.comps(Structs.comparingFloat(sort.sort), Structs.comparingFloat(sort.secondary)));
            if(reverseSort) groups.reverse();

            for(SpawnGroup group : groups){
                if(group.effect == StatusEffects.none) group.effect = null;

                if(!checkFilters(group)) continue;

                int index2 = index;
                canvas.addChild(new Table(){
                    boolean dragging;

                    {
                        margin(2f);
                        background(Tex.whiteui);
                        touchable = Touchable.enabled;
                        clicked(() -> {
                            if(!batchEditing){
                                if(!selectedGroups.remove(group)) selectedGroups.clear().add(group);
                            }else{
                                if(!selectedGroups.remove(group)) selectedGroups.add(group);
                            }
                            buildConfig();
                        });
                        setColor(color(group.type));

                        this.addListener(new InputListener(){
                            long time;
                            float downx;

                            @Override
                            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                                time = Time.millis();
                                downx = x;
                                return true;
                            }

                            @Override
                            public void touchDragged(InputEvent event, float x, float y, int pointer){
                                if(Time.millis() > time + 500){
                                    dragging = true;
                                    Core.scene.cancelTouchFocus(canvas);
                                    selectedGroups.remove(group, true);
                                    buildConfig();
                                    setTranslation(x - downx + translation.x, translation.y);
                                }
                                super.touchDragged(event, x, y, pointer);
                            }

                            @Override
                            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                                super.touchUp(event, x, y, pointer, button);
                                if(Time.millis() > time + 500){
                                    int gap = group.end - group.begin;
                                    float dx = x - downx + translation.x;
                                    if(Math.abs(dx) > canvas.tileW){
                                        group.begin += Mathf.round(dx / canvas.tileW);
                                        if(group.end != never) group.end += Mathf.round(dx / canvas.tileW);
                                    }

                                    group.begin = Math.max(group.begin, 0);
                                    group.end = Math.max(group.end, gap);
                                    setTranslation(0f, 0f);
                                    dragging = false;
                                }
                            }
                        });

                        label(() -> String.valueOf(group.begin + 1));
                        add(new Table(t -> {
                            image(group.type.uiIcon).size(32f).scaling(Scaling.fit);
                            if(group.effect != null) image(group.effect.uiIcon).size(32f).scaling(Scaling.fit);
                            if(group.items != null){
                                var label = new Label(String.valueOf(group.items.amount));
                                label.setAlignment(Align.bottomRight);
                                label.setFillParent(true);
                                label.setFontScale(0.7f);
                                stack(new Image(group.items.item.uiIcon).setScaling(Scaling.fit), label).size(32f);
                            }
                            label(() -> (group.payloads == null || group.payloads.isEmpty() ? "" : Iconc.itchio + "") + (group.spawn == -1 ? "" : Iconc.blockSpawn + ""));
                        }){
                            @Override
                            public float getPrefWidth(){
                                return 1f;
                            }
                        }).grow();
                        label(() -> group.end == SpawnGroup.never ? "∞" : String.valueOf(group.end + 1));
                    }

                    @Override
                    public void act(float delta){
                        super.act(delta);
                        //int index = groups.indexOf(group, true);
                        setPosition(canvas.tileX(group.begin), -canvas.camera.y + canvas.tileH * index2 + 16f);
                        setSize(Mathf.clamp(canvas.tileX(group.end + (group.end == never ? 0 : 1)) - canvas.tileX(group.begin), getPrefWidth(), canvas.getWidth()), canvas.tileH - marginTop);
                    }

                    @Override
                    protected void drawBackground(float x, float y){
                        boolean selected = selectedGroups.contains(group, true);
                        Tmp.c2.set(color);
                        color.set(Color.white).a(selected ? 1f : 0.3f);
                        super.drawBackground(x, y);
                        color.set(Tmp.c2);

                        //spacing block
                        Draw.color(selected ? Pal.accent : dragging ? Color.royal : color);
                        Draw.alpha(0.6f * parentAlpha);
                        Fill.rect(Tmp.r1.set(canvas.tileX(group.begin), y, canvas.tileX(group.end) + canvas.tileW - canvas.tileX(group.begin), canvas.tileH - marginTop));

                        //spawning block
                        int start = canvas.getStartWave();
                        int end = canvas.getEndWave();
                        Draw.alpha(this.parentAlpha);
                        for(int i = group.begin; i <= group.end && i <= end; i += group.spacing){
                            if(i < start) continue;
                            Fill.rect(Tmp.r1.set(canvas.tileX(i), y, canvas.tileX(i + 1) - canvas.tileX(i), canvas.tileH - marginTop));
                        }
                        Draw.reset();
                        color.a(1f);
                    }
                });

                index++;
            }
        }


        buildConfig();
    }

    void buildConfig(){
        if(config == null || selectedGroups.isEmpty()) return;
        var group = selectedGroups.get(selectedGroups.size - 1);
        config.clear();
        config.button("@waveinfo.deselect", () -> selectedGroups.clear()).width(300f).row();
        config.table(b -> {
            b.left();
            b.image(group.type.uiIcon).size(32f).padRight(3).scaling(Scaling.fit);
            b.add(group.type.localizedName).color(Pal.accent);

            b.add().growX();

            b.button(Icon.copySmall, Styles.emptyi, () -> {
                groups.insert(groups.indexOf(group) + 1, group.copy());
                buildGroups();
            }).pad(-6).size(46f).tooltip("@editor.copy").disabled(bb -> !batchEditing);

            b.button(Icon.unitsSmall, Styles.emptyi, () -> showAnyContentsYouWant(content.units().copy().removeAll(UnitType::isHidden), "", type -> {
                batchEditing(g -> g.type = type);
                buildConfig();
            }, null)).pad(-6).size(46f).tooltip("@stat.unittype");

            b.button(Icon.cancel, Styles.emptyi, () -> {
                batchEditing(g -> groups.remove(g));
                selectedGroups.clear();
                buildGroups();
            }).pad(-6).size(46f).padRight(-12f).tooltip("@waves.remove");
            b.clicked(KeyCode.mouseMiddle, () -> {
                groups.insert(groups.indexOf(group) + 1, group.copy());
                buildGroups();
            });
        }).row();

        config.table(t -> {
            t.table(spawns -> {
                spawns.field("" + (group.begin + 1), TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.begin = Strings.parseInt(text) - 1);
                    }
                }).width(100f);
                spawns.add("@waves.to").padLeft(4).padRight(4);
                spawns.field(group.end == never ? "" : (group.end + 1) + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.end = Strings.parseInt(text) - 1);
                    }else if(text.isEmpty()){
                        batchEditing(g -> g.end = never);
                    }
                }).width(100f).get().setMessageText("∞");
            }).row();

            t.table(p -> {
                p.add("@waves.every").padRight(4);
                p.field(group.spacing + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text) && Strings.parseInt(text) > 0){
                        batchEditing(g -> g.spacing = Strings.parseInt(text));
                    }
                }).width(100f);
                p.add("@waves.waves").padLeft(4);
            }).row();

            t.table(a -> {
                a.field(group.unitAmount + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.unitAmount = Strings.parseInt(text));
                    }
                }).width(80f);

                a.add(" + ");
                a.field(Strings.fixed(Math.max((Mathf.zero(group.unitScaling) ? 0 : 1f / group.unitScaling), 0), 2), TextField.TextFieldFilter.floatsOnly, text -> {
                    if(Strings.canParsePositiveFloat(text)){
                        batchEditing(g -> g.unitScaling = 1f / Strings.parseFloat(text));
                    }
                }).width(80f);
                a.add("@waves.perspawn").padLeft(4);
            }).row();

            t.table(a -> {
                a.field(group.max + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.max = Strings.parseInt(text));
                    }
                }).width(80f);

                a.add("@waves.max").padLeft(5);
            }).row();

            t.table(a -> {
                a.field((int)group.shields + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.shields = Strings.parseInt(text));
                    }
                }).width(80f);

                a.add(" + ");
                a.field((int)group.shieldScaling + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.shieldScaling = Strings.parseInt(text));
                    }
                }).width(80f);
                a.add("@waves.shields").padLeft(4);
            }).row();

            t.table(a -> {
                a.button(b -> {
                    b.image(group.effect != null ? group.effect.uiIcon : Icon.none.getRegion()).scaling(Scaling.fit).size(32f);
                }, () -> {
                    showEffects();
                    buildGroups();
                });

                a.check("@waves.guardian", b -> {
                    batchEditing(g -> g.effect = b ? StatusEffects.boss : null);
                    buildGroups();
                }).padTop(4).update(b -> b.setChecked(group.effect == StatusEffects.boss)).padBottom(8f);
            }).width(300f).row();

            t.table(a -> {
                a.button(b -> b.image(group.items == null ? Icon.none.getRegion() : group.items.item.uiIcon).scaling(Scaling.fit).size(32f), () -> showAnyContentsYouWant(content.items().removeAll(Item::isHidden), "", item -> {
                    batchEditing(g -> {
                        if(g.items == null) g.items = new ItemStack().set(Items.copper, g.type.itemCapacity);
                        g.items.item = item;
                    });
                    buildGroups();
                }, () -> group.items = null));

                a.field(group.items == null ? "" : String.valueOf(group.items.amount), TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> {
                            if(g.items != null) g.items.amount = Strings.parseInt(text);
                        });
                        buildGroups();
                    }
                }).width(80f).disabled(tf -> group.items == null);
            }).width(300f).row();

            t.table(a -> {
                a.button(b -> b.image(Icon.itchioSmall).scaling(Scaling.fit).size(32f), () -> showPayloads(group)).disabled(b -> batchEditing);
                if(group.payloads == null || group.payloads.isEmpty()) return;
                a.table(ps -> {
                    for(int i = 1; i <= 8 && i <= group.payloads.size; i++){
                        ps.image(group.payloads.get(group.payloads.size - i).uiIcon).size(16f).scaling(Scaling.fit);
                    }
                });
            }).width(300f).row();

            t.table(a -> {
                a.add("@waves.spawn").padRight(8);

                a.button("", () -> {
                    if(!checkedSpawns){
                        //recalculate waves when changed
                        Vars.spawner.reset();
                        checkedSpawns = true;
                    }

                    BaseDialog dialog = new BaseDialog("@waves.spawn.select");
                    dialog.cont.pane(p -> {
                        p.background(Tex.button).margin(10f);
                        int i = 0;
                        int cols = 4;
                        int max = 20;

                        if(spawner.getSpawns().size >= max){
                            p.add("[lightgray](first " + max + ")").colspan(cols).padBottom(4).row();
                        }

                        for(var spawn : spawner.getSpawns()){
                            p.button(spawn.x + ", " + spawn.y, Styles.flatTogglet, () -> {
                                batchEditing(g -> g.spawn = spawn.pos());
                                dialog.hide();
                            }).size(110f, 45f).checked(spawn.pos() == group.spawn);
                            if(++i % cols == 0){
                                p.row();
                            }

                            //only display first 20 spawns, you don't need to see more.
                            if(i >= 20){
                                break;
                            }
                        }

                        if(spawner.getSpawns().isEmpty()){
                            p.add("@waves.spawn.none");
                        }else{
                            p.button("@waves.spawn.all", Styles.flatTogglet, () -> {
                                batchEditing(g -> g.spawn = -1);
                                dialog.hide();
                            }).size(110f, 45f).checked(-1 == group.spawn);
                        }
                    }).grow();
                    dialog.setFillParent(false);
                    dialog.addCloseButton();
                    dialog.show();
                }).width(160f).height(36f).get().getLabel().setText(() -> group.spawn == -1 ? "@waves.spawn.all" : Point2.x(group.spawn) + ", " + Point2.y(group.spawn));
            }).padBottom(8f).row();
        }).width(300f).row();
    }

    void batchEditing(Cons<SpawnGroup> run){
        selectedGroups.each(run);
    }

    <T extends UnlockableContent> void showAnyContentsYouWant(Seq<T> seq, String title, Cons<T> setter){
        showAnyContentsYouWant(seq, title, setter, () -> setter.get(null));
    }

    <T extends UnlockableContent> void showAnyContentsYouWant(Seq<T> seq, String title, Cons<T> setter, @Nullable Runnable noneSetter){
        boolean reset = noneSetter != null;
        BaseDialog dialog = new BaseDialog(title);
        dialog.cont.pane(p -> {
            p.defaults().pad(2).fillX();
            if(reset){
                p.button(t -> {
                    t.left();
                    t.image(Icon.none).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add("@settings.resetKey");
                }, () -> {
                    noneSetter.run();
                    dialog.hide();
                    buildGroups();
                }).margin(12f);
            }
            int i = reset ? 1 : 0;
            for(T type : seq){
                p.button(t -> {
                    t.left();
                    t.image(type.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add(type.localizedName);
                }, () -> {
                    setter.get(type);
                    dialog.hide();
                    buildGroups();
                }).margin(12f);
                if(++i % 3 == 0) p.row();
            }
        }).growX().scrollX(false);
        dialog.addCloseButton();
        dialog.show();
    }

    void showEffects(){
        showAnyContentsYouWant(content.statusEffects().copy().removeAll(effect -> effect.reactive), "", effect -> batchEditing(g -> g.effect = effect));
    }

    void showPayloads(SpawnGroup group){
        BaseDialog dialog = new BaseDialog(""){
            Table pane;

            {
                addCloseButton();
                hidden(() -> {
                    buildGroups();
                });
                cont.pane(p -> {
                    pane = p;
                    build();
                }).scrollX(true).height(300f).row();

                cont.pane(p -> {
                    p.defaults().pad(2).fillX();
                    int i = 0;
                    for(var type : content.units()){
                        p.button(t -> {
                            t.center();
                            t.image(type.uiIcon).size(8 * 8).scaling(Scaling.fit).padRight(2f);
                            t.row();
                            t.add(type.localizedName);
                        }, () -> {
                            if(group.payloads == null) group.payloads = new Seq<>();
                            group.payloads.add(type);
                            build();
                        }).margin(12f).size(144);
                        if(++i % 8 == 0) p.row();
                    }
                }).grow().scrollX(false);
            }

            void build(){
                pane.clear();
                pane.defaults().pad(2).fillX();

                if(group.payloads == null || group.payloads.isEmpty()){
                    pane.add("@waves.none");
                    return;
                }

                for(int i = 0; i < group.payloads.size; i++){
                    if(i % 8 == 0) pane.row();
                    int ii = i;
                    pane.button(t -> {
                        t.center();
                        t.image(group.payloads.get(ii).uiIcon).size(8 * 8).scaling(Scaling.fit);
                        t.row();
                        t.add(group.payloads.get(ii).localizedName);
                    }, () -> {
                        group.payloads.ordered = true;
                        group.payloads.remove(ii);
                        build();
                    }).margin(12f).size(144f);
                }
            }
        };
        dialog.show();
    }

    void scaleX(int amount){
        updateTimer += Time.delta;
        if(updateTimer >= updatePeriod){
            canvas.tileW += amount;
            updateTimer = 0f;
        }
    }

    Color color(UnitType type){
        return Tmp.c1.fromHsv(type.id / (float)Vars.content.units().size * 360f, 0.7f, 0.8f).a(1f);
    }

    public class WaveCanvas extends WidgetGroup{
        float tileH, tileW;
        Vec2 vec = new Vec2(), sv = new Vec2();
        /**
         * camera position left bottom.
         */
        public Vec2 camera = new Vec2();

        public WaveCanvas(){
            this.setTransform(true);
            this.setCullingArea(new Rect());
            tileH = 50f;
            tileW = 50f;

            hovered(() -> Core.scene.setScrollFocus(this));

            addListener(new InputListener(){
                float lx, ly;

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    sv.setZero();
                    vec.setZero();
                    lx = x;
                    ly = y;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    sv.sub(x - lx, y - ly);
                    camera.sub(x - lx, y - ly);
                    lx = x;
                    ly = y;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                    vec.set(sv);
                }

                @Override
                public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                    scroll(amountY * 100f * (1f + Math.abs(x / getWidth() - 0.5f) * 8f));
                    return super.scrolled(event, x, y, amountX, amountY);
                }
            });
        }

        @Override
        public void act(float delta){
            super.act(delta);
            this.getCullingArea().set(0, 0, getWidth(), getHeight());
            camera.add(vec.x, vec.y);
            sv.scl(0.5f);
            vec.scl(0.9f);
            camera.lerpDelta(Math.max(camera.x, -20f), Mathf.clamp(camera.y, -20f, tileH * (groups.size + 1) - getHeight() + 20f), 0.1f);
        }

        public int getStartWave(){
            return Math.max(Mathf.floor(camera.x / tileW), 0);
        }

        public int getEndWave(){
            return Math.max(Mathf.ceil((camera.x + getWidth()) / tileW), 0);
        }

        public float tileX(int wave){
            return Mathf.clamp(-camera.x + wave * tileW, 0, getWidth());
        }

        @Override
        public void draw(){
            int start = getStartWave();
            int end = getEndWave();
            int gap = Mathf.ceil(100f / tileW);
            Lines.stroke(4f);
            Draw.color(Color.gray, 0.3f);
            Font font = Fonts.outline;
            font.setColor(Color.gray);
            for(int i = start / gap * gap; i < end; i += 1){
                float cx = -camera.x + i * tileW;
                if(cx < 0) continue;
                float dx = cx + this.x;
                float dy = this.y;
                Lines.dashLine(dx, dy + 16, dx, dy + getHeight(), 60);
                if(Mathf.mod(i, gap) == 0) font.draw(String.valueOf(i + 1), dx + tileW / 2f, dy + 12, Align.center);
                if(search == i){
                    Fill.rect(Tmp.r1.set(dx, dy, tileW, getHeight()));
                }
            }
            font.setColor(Color.white);
            Draw.reset();

            applyTransform(computeTransform());
            Draw.flush();
            if(clipBegin(0, 16, getWidth(), getHeight())){
                drawChildren();
                Draw.flush();
                clipEnd();
            }
            resetTransform();
        }

        public void locateWave(int wave){
            camera.x = Mathf.maxZero(canvas.tileW * wave - canvas.getWidth() / 2f);
        }

        public void scroll(float amount){
            camera.x += amount;
        }
    }

    enum Sort{
        begin(g -> g.begin, g -> g.type.id),
        end(g -> g.end, g -> g.type.id),
        type(g -> g.type.id);

        static final MFEWaveInfoDialog.Sort[] all = values();

        final Floatf<SpawnGroup> sort, secondary;

        Sort(Floatf<SpawnGroup> sort){
            this(sort, g -> g.begin);
        }

        Sort(Floatf<SpawnGroup> sort, Floatf<SpawnGroup> secondary){
            this.sort = sort;
            this.secondary = secondary;
        }
    }
}
