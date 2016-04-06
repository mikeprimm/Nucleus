/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.OldCommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;

/**
 * Allows a user to warp to the specified warp.
 *
 * Command Usage: /world Permission: nucleus.world.base
 *
 */
@Permissions(suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand("world")
public class WorldCommand extends OldCommandBase<CommandSource> {
    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder()
                .children(this.createChildCommands())
                .build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        return CommandResult.empty();
    }
}