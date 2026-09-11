package studio.ykz.energyexchange;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ExchangeCommands {
    private ExchangeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(literal("energyexchange")
                .executes(c -> help(c.getSource()))
                .then(literal("help").executes(c -> help(c.getSource())))
                .then(literal("balance").executes(c -> run(c, p -> reply(c, "balance", ExchangeService.account(p).energy().toString()))))
                .then(literal("value").executes(c -> run(c, p -> reply(c, "value", p.getMainHandItem().getHoverName(), ExchangeService.heldRule(p).value().toString()))))
                .then(literal("learn").executes(c -> run(c, p -> {
                    var name = p.getMainHandItem().getHoverName();
                    ExchangeService.learn(p);
                    reply(c, "learned", name);
                })))
                .then(literal("burn").executes(c -> burn(c, 1))
                        .then(literal("all").executes(c -> burn(c, c.getSource().getPlayerOrException().getMainHandItem().getCount())))
                        .then(argument("count", integer(1, 2304)).executes(c -> burn(c, getInteger(c, "count")))))
                .then(literal("buy").then(argument("item", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .suggests((c, builder) -> {
                            var player = c.getSource().getPlayer();
                            if (player == null) return builder.buildFuture();
                            try { return SharedSuggestionProvider.suggest(ExchangeService.account(player).learned().stream().sorted(), builder); }
                            catch (IllegalArgumentException ignored) { return builder.buildFuture(); }
                        })
                        .executes(c -> buy(c, 1))
                        .then(argument("count", integer(1, 2304)).executes(c -> buy(c, getInteger(c, "count"))))))
                .then(literal("list").executes(c -> list(c, 1))
                        .then(argument("page", integer(1)).executes(c -> list(c, getInteger(c, "page"))))));
        dispatcher.register(literal("ee").executes(c -> help(c.getSource())).redirect(root));
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Messages.text("help"), false);
        return 1;
    }

    private static int burn(CommandContext<CommandSourceStack> c, int count) throws CommandSyntaxException {
        return run(c, p -> {
            var name = p.getMainHandItem().getHoverName();
            var account = ExchangeService.burn(p, count);
            reply(c, "burned", Integer.toString(count), name, account.energy().toString());
        });
    }

    private static int buy(CommandContext<CommandSourceStack> c, int count) throws CommandSyntaxException {
        return run(c, p -> {
            String[] parts = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "item").trim().split("\\s+");
            if (parts.length < 1 || parts.length > 2) throw new IllegalArgumentException("energyexchange.error.count");
            int amount = count;
            if (parts.length == 2) {
                try { amount = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { throw new IllegalArgumentException("energyexchange.error.count"); }
            }
            var account = ExchangeService.buy(p, parts[0], amount);
            reply(c, "bought", Integer.toString(amount), parts[0], account.energy().toString());
        });
    }

    private static int list(CommandContext<CommandSourceStack> c, int page) throws CommandSyntaxException {
        return run(c, p -> {
            List<String> learned = ExchangeService.account(p).learned().stream().sorted().toList();
            int pages = Math.max(1, (learned.size() + 11) / 12);
            if (page > pages) throw new IllegalArgumentException("energyexchange.error.page");
            reply(c, "list", Integer.toString(page), Integer.toString(pages), Integer.toString(learned.size()));
            for (String item : learned.subList((page - 1) * 12, Math.min(page * 12, learned.size()))) reply(c, "list_entry", item);
        });
    }

    private static int run(CommandContext<CommandSourceStack> c, PlayerAction action) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        try {
            action.run(player);
            return 1;
        } catch (IllegalArgumentException exception) {
            String key = exception.getMessage();
            if (key == null || !key.startsWith("energyexchange.error.")) {
                EnergyExchange.LOGGER.error("Exchange failed / 交换失败", exception);
                key = "energyexchange.error.internal";
            }
            c.getSource().sendFailure(Messages.text(key));
            return 0;
        }
    }

    private static void reply(CommandContext<CommandSourceStack> c, String key, Object... args) {
        c.getSource().sendSuccess(() -> Messages.text(key, args), false);
    }

    @FunctionalInterface
    private interface PlayerAction { void run(ServerPlayer player); }
}
