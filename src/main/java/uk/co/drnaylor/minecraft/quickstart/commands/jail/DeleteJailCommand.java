package uk.co.drnaylor.minecraft.quickstart.commands.jail;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import uk.co.drnaylor.minecraft.quickstart.internal.CommandBase;
import uk.co.drnaylor.minecraft.quickstart.internal.annotations.*;

@Permissions(root = "jail")
@RunAsync
@NoWarmup
@NoCooldown
@NoCost
public class DeleteJailCommand extends CommandBase {
    @Override
    public CommandSpec createSpec() {
        return null;
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        return null;
    }
}