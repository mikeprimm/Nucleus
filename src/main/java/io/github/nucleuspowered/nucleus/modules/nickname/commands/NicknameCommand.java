/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.commands;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.NameUtil;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.nickname.config.NicknameConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.nickname.datamodules.NicknameUserDataModule;
import io.github.nucleuspowered.nucleus.modules.nickname.events.ChangeNicknameEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

@NonnullByDefault
@RegisterCommand({"nick", "nickname"})
@Permissions
@EssentialsEquivalent(value = {"nick", "nickname"}, isExact = false,
        notes = "To remove a nickname, use '/delnick'")
public class NicknameCommand extends AbstractCommand<CommandSource> {

    private final UserDataManager loader;
    private final NicknameConfigAdapter nicknameConfigAdapter;

    @Inject
    public NicknameCommand(UserDataManager loader, NicknameConfigAdapter nicknameConfigAdapter) {
        this.loader = loader;
        this.nicknameConfigAdapter = nicknameConfigAdapter;
    }

    private final String playerKey = "subject";
    private final String nickName = "nickname";

    // Order is important here! TODO: Need to de-dup
    private final Map<String, String> permissionToDesc = Maps.newHashMap();
    private final Map<String[], Tuple<Matcher, Text>> replacements = Maps.newHashMap();

    @Override protected void afterPostInit() {
        super.afterPostInit();

        MessageProvider mp = Nucleus.getNucleus().getMessageProvider();

        String colPerm = permissions.getPermissionWithSuffix("colour.");
        String colPerm2 = permissions.getPermissionWithSuffix("color.");

        NameUtil.getColours().forEach((key, value) -> {
            replacements.put(new String[]{colPerm + value.getName(), colPerm2 + value.getName()},
                Tuple.of(Pattern.compile("[&]+" + key.toString().toLowerCase(), Pattern.CASE_INSENSITIVE).matcher(""),
                    mp.getTextMessageWithFormat("command.nick.colour.nopermswith", value.getName())));

            permissionToDesc.put(colPerm + value.getName(),
                    mp.getMessageWithFormat("permission.nick.colourspec", value.getName().toLowerCase(), key.toString()));
            permissionToDesc.put(colPerm2 + value.getName(), mp.getMessageWithFormat("permission.nick.colorspec", value.getName().toLowerCase(), key.toString()));
        });

        String stylePerm = permissions.getPermissionWithSuffix("style.");
        NameUtil.getStyles().entrySet().stream().filter(x -> x.getKey().getFirst() != 'k').forEach((k) -> {
            replacements.put(new String[] { stylePerm + k.getKey().getSecond().toLowerCase() },
                Tuple.of(Pattern.compile("[&]+" + k.getKey().getFirst().toString().toLowerCase(), Pattern.CASE_INSENSITIVE).matcher(""),
                    mp.getTextMessageWithFormat("command.nick.style.nopermswith", k.getKey().getSecond().toLowerCase())));

            permissionToDesc.put(stylePerm + k.getKey().getSecond().toLowerCase(),
                mp.getMessageWithFormat("permission.nick.stylespec", k.getKey().getSecond().toLowerCase(), k.getKey().getFirst().toString()));
        });

        replacements.put(new String[] { permissions.getPermissionWithSuffix("magic") },
            Tuple.of(Pattern.compile("[&]+k", Pattern.CASE_INSENSITIVE).matcher(""),
                mp.getTextMessageWithFormat("command.nick.style.nopermswith", "magic")));
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", PermissionInformation.getWithTranslation("permission.nick.others", SuggestedLevel.ADMIN));
        m.put("colour", PermissionInformation.getWithTranslation("permission.nick.colour", SuggestedLevel.ADMIN, true, false));
        m.put("color", PermissionInformation.getWithTranslation("permission.nick.color", SuggestedLevel.ADMIN));
        m.put("color.<color>", PermissionInformation.getWithTranslation("permission.nick.colorsingle", SuggestedLevel.ADMIN, false, true));
        m.put("style", PermissionInformation.getWithTranslation("permission.nick.style", SuggestedLevel.ADMIN));
        m.put("style.<style>", PermissionInformation.getWithTranslation("permission.nick.stylesingle", SuggestedLevel.ADMIN, false, true));
        m.put("magic", PermissionInformation.getWithTranslation("permission.nick.magic", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    protected Map<String, PermissionInformation> permissionsToRegister() {
        return permissionToDesc.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey, v -> new PermissionInformation(v.getValue(), SuggestedLevel.ADMIN, true, false)));
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optionalWeak(GenericArguments.requiringPermission(
                        GenericArguments.onlyOne(GenericArguments.user(Text.of(playerKey))), permissions.getPermissionWithSuffix("others"))),
                GenericArguments.onlyOne(GenericArguments.string(Text.of(nickName)))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        User pl = this.getUserFromArgs(User.class, src, playerKey, args);
        String name = args.<String>getOne(nickName).get();

        // Does the user exist?
        try {
            Optional<User> match =
                Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(TextSerializers.FORMATTING_CODE.stripCodes(name));

            // The only person who can use such a name is oneself.
            if (match.isPresent() && !match.get().getUniqueId().equals(pl.getUniqueId())) {
                // Fail - cannot use another's name.
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.nick.nameinuse", name));
                return CommandResult.empty();
            }
        } catch (IllegalArgumentException ignored) {
            // We allow some other nicknames too.
        }

        // Giving subject must have the colour permissions and whatnot. Also,
        // colour and color are the two spellings we support. (RULE BRITANNIA!)
        stripPermissionless(src, name);

        String strippedName = name.replaceAll("&[0-9a-fomlnk]", "");
        Pattern p = nicknameConfigAdapter.getNodeOrDefault().getPattern();
        if (!p.matcher(strippedName).matches()) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.nick.nopattern", p.pattern()));
        }

        int strippedNameLength = strippedName.length();

        // Do a regex remove to check minimum length requirements.
        if (strippedNameLength < Math.max(nicknameConfigAdapter.getNodeOrDefault().getMinNicknameLength(), 1)) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.nick.tooshort"));
            return CommandResult.empty();
        }

        // Do a regex remove to check maximum length requirements. Will be at least the minimum length
        if (strippedNameLength > Math.max(nicknameConfigAdapter.getNodeOrDefault().getMaxNicknameLength(), nicknameConfigAdapter.getNodeOrDefault().getMinNicknameLength())) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.nick.toolong"));
            return CommandResult.empty();
        }

        // Send an event
        ChangeNicknameEvent cne = new ChangeNicknameEvent(Cause.of(NamedCause.source(src)), TextSerializers.FORMATTING_CODE.deserialize(name), pl);
        if (Sponge.getEventManager().post(cne)) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.nick.eventcancel", pl.getName()));
            return CommandResult.empty();
        }

        NicknameUserDataModule nicknameUserDataModule = loader.get(pl).get().get(NicknameUserDataModule.class);
        nicknameUserDataModule.setNickname(name);
        Text set = nicknameUserDataModule.getNicknameAsText().get();

        if (!src.equals(pl)) {
            src.sendMessage(Text.builder().append(plugin.getMessageProvider().getTextMessageWithFormat("command.nick.success.other", pl.getName()))
                    .append(Text.of(" - ", TextColors.RESET, set)).build());
        }

        if (pl.isOnline()) {
            pl.getPlayer().get().sendMessage(Text.builder().append(plugin.getMessageProvider().getTextMessageWithFormat("command.nick.success.base"))
                    .append(Text.of(" - ", TextColors.RESET, set)).build());
        }

        return CommandResult.success();
    }


    private void stripPermissionless(Subject source, String message) throws ReturnMessageException {
        if (message.contains("&")) {
            for (Map.Entry<String[], Tuple<Matcher, Text>> r : replacements.entrySet()) {
                // If we don't have the required permission...
                if (r.getValue().getFirst().reset(message).find() && Arrays.stream(r.getKey()).noneMatch(source::hasPermission)) {
                    // throw
                    throw new ReturnMessageException(r.getValue().getSecond());
                }
            }
        }
    }
}
