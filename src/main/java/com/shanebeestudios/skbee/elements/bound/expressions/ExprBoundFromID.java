package com.shanebeestudios.skbee.elements.bound.expressions;

import java.util.ArrayList;
import java.util.List;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.shanebeestudios.skbee.SkBee;
import com.shanebeestudios.skbee.api.bound.Bound;
import com.shanebeestudios.skbee.config.BoundConfig;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.world.WorldEvent;
import org.jetbrains.annotations.NotNull;

@Name("Bound - From ID")
@Description("Get a bound object from a bound ID")
@Examples("set {_b} to bound from id \"%player%.home\"")
@Since("1.0.0")
public class ExprBoundFromID extends SimpleExpression<Bound> {

    static {
        Skript.registerExpression(ExprBoundFromID.class, Bound.class, ExpressionType.COMBINED,
                "bound[s] (of|from|with) id[s] %strings% [in %-world%]");
    }

    private Expression<String> ids;
    private Expression<World> world;
    private static final BoundConfig boundConfig = SkBee.getPlugin().getBoundConfig();

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
        this.ids = (Expression<String>) exprs[0];
        this.world = (Expression<World>) exprs[1];
        return true;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected Bound[] get(Event event) {
        World world = this.world.getOptionalSingle(event).orElseGet(() -> {
                    if (event instanceof WorldEvent worldEvent)
                        return worldEvent.getWorld();
                    return null;
                });
        List<Bound> bounds = new ArrayList<>();
        for (String id : this.ids.getArray(event)) {
            if (world != null) {
                Bound bound = boundConfig.getBoundFromID(id, world);
                if (bound == null || bounds.contains(bound)) continue;
                bounds.add(bound);
            } else {
                bounds.addAll(boundConfig.getBoundsFromID(id).stream().toList());
            }
        }
        return bounds.toArray(new Bound[0]);
    }

    @Override
    public boolean isSingle() {
        return ids.isSingle() || world != null;
    }

    @Override
    public @NotNull Class<? extends Bound> getReturnType() {
        return Bound.class;
    }

    @Override
    public @NotNull String toString(Event event, boolean debug) {
        if (world != null)
            return "bound from id " + this.ids.toString(event, debug) + " in " + world.toString(event, debug);
        return "bound from id " + this.ids.toString(event, debug);
    }

}
