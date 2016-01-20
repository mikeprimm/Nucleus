package uk.co.drnaylor.minecraft.quickstart.commands.warps;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import uk.co.drnaylor.minecraft.quickstart.Util;
import uk.co.drnaylor.minecraft.quickstart.api.service.QuickStartWarpService;
import uk.co.drnaylor.minecraft.quickstart.internal.CommandBase;
import uk.co.drnaylor.minecraft.quickstart.internal.annotations.Permissions;

import java.text.MessageFormat;
import java.util.regex.Pattern;

@Permissions(root = "warp")
public class SetWarpCommand extends CommandBase<Player> {
    private final Pattern warpRegex = Pattern.compile("^[A-Za-z][A-Za-z0-9]{0,25}$");

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().executor(this)
                .arguments(
                        GenericArguments.onlyOne(GenericArguments.string(Text.of(Util.messageBundle.getString(WarpsCommand.warpNameArg))))
                )
                .description(Text.of("Sets a warp at the player's location."))
                .build();
    }

    @Override
    public String[] getAliases() {
        return new String[] { "set" };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws CommandException {
        String warp = args.<String>getOne(WarpsCommand.warpNameArg).get();

        // Needs to match the name...
        if (!warpRegex.matcher(warp).matches()) {
            src.sendMessage(Text.of(TextColors.RED, Util.messageBundle.getString("command.warps.invalidname")));
            return CommandResult.empty();
        }

        // Get the service, does it exist?
        QuickStartWarpService qs = Sponge.getServiceManager().provideUnchecked(QuickStartWarpService.class);
        if (qs.getWarp(warp).isPresent()) {
            // You have to delete to set the same name
            src.sendMessage(Text.of(TextColors.RED, Util.messageBundle.getString("command.warps.nooverwrite")));
            return CommandResult.empty();
        }

        // OK! Set it.
        if (qs.setWarp(warp, src.getLocation(), src.getRotation())) {
            // Worked. Tell them.
            src.sendMessage(Text.of(TextColors.GREEN, MessageFormat.format(Util.messageBundle.getString("command.warps.set"), warp)));
            return CommandResult.success();
        }

        // Didn't work. Tell them.
        src.sendMessage(Text.of(TextColors.RED, Util.messageBundle.getString("command.warps.seterror")));
        return CommandResult.empty();
    }
}
