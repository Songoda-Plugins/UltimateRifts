package com.songoda.ultimaterifts.gui;

import com.craftaro.core.gui.AnvilGui;
import com.craftaro.core.gui.CustomizableGui;
import com.craftaro.core.gui.GuiUtils;
import com.craftaro.core.utils.ItemUtils;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.Member;
import com.songoda.ultimaterifts.rift.Rift;
import com.songoda.ultimaterifts.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MembersGui extends CustomizableGui {
    private final UltimateRifts plugin;
    private final Rift rift;
    private SortType sortType = SortType.DEFAULT;

    public MembersGui(UltimateRifts plugin, Rift rift) {
        super(plugin, "members");
        this.rift = rift;
        this.plugin = plugin;
        this.setRows(6);
        this.setTitle(plugin.getLocale().getMessage("interface.members.title").getMessage());

        ItemStack glass2 = GuiUtils.getBorderItem(XMaterial.BLACK_STAINED_GLASS_PANE);
        ItemStack glass3 = GuiUtils.getBorderItem(XMaterial.GRAY_STAINED_GLASS_PANE);

        // edges will be type 3
        setDefaultItem(glass3);

        // decorate corners
        mirrorFill("mirrorfill_1", 0, 0, true, true, glass2);
        mirrorFill("mirrorfill_2", 1, 0, true, true, glass2);
        mirrorFill("mirrorfill_3", 0, 1, true, true, glass2);

        // exit buttons
        this.setButton("back", 0, GuiUtils.createButtonItem(XMaterial.OAK_FENCE_GATE,
                        plugin.getLocale().getMessage("general.interface.back").getMessage()),
                (event) -> this.guiManager.showGUI(event.player, new OverviewGui(rift, event.player)));
        this.setButton("back", 8, this.getItem(0),
                (event) -> this.guiManager.showGUI(event.player, new OverviewGui(rift, event.player)));

        // Member Stats (update on refresh)
        this.setItem("stats", 4, XMaterial.PAINTING.parseItem());

        // Filters
        this.setButton("sort", 5, XMaterial.HOPPER.parseItem(), (event) -> toggleSort());

        // Add member button
        this.setButton("add_member", 5, 1, GuiUtils.createButtonItem(XMaterial.PLAYER_HEAD,
                        plugin.getLocale().getMessage("interface.members.addmembertitle").toText(),
                        plugin.getLocale().getMessage("interface.members.addmemberlore").toText().split("\\|")),
                (event) -> {
                    if (rift.getMembers().size() >= Settings.MAX_RIFT_MEMBERS.getInt()) {
                        plugin.getLocale().getMessage("event.invite.maxmembers").sendPrefixedMessage(event.player);
                        XSound.ENTITY_VILLAGER_NO.play(event.player);
                        return;
                    }
                    AnvilGui gui = new AnvilGui(event.player, this);
                    gui.setAction((e) -> {
                        String reply = gui.getInputText().trim();
                        // Handle adding member
                        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(reply);
                        if (targetPlayer != null && targetPlayer.hasPlayedBefore()) {
                            rift.addMember(targetPlayer.getUniqueId()).save();
                            showPage();
                        } else {
                            plugin.getLocale().getMessage("command.addmember.playernotfound")
                                    .sendPrefixedMessage(event.player);
                        }
                        e.manager.showGUI(e.player, this);
                    });
                    guiManager.showGUI(event.player, gui);
                });

        // Transfer ownership button
        this.setButton("transfer_ownership", 5, 7, GuiUtils.createButtonItem(XMaterial.ENDER_PEARL,
                        plugin.getLocale().getMessage("interface.members.transferownershiptitle").toText(),
                        plugin.getLocale().getMessage("interface.members.transferownershiplore").toText().split("\\|")),
                (event) -> {
                    AnvilGui gui = new AnvilGui(event.player, this);
                    gui.setAction((e) -> {
                        String reply = gui.getInputText().trim();
                        // Handle transferring ownership
                        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(reply);
                        if (targetPlayer != null && targetPlayer.hasPlayedBefore()) {
                            rift.setOwner(targetPlayer).save();
                            for (UUID uuid : rift.getMembers().keySet()) {
                                Member member = rift.getMembers().get(uuid);
                                member.save();
                            }
                            showPage();
                        } else {
                            event.player.sendMessage(plugin.getLocale().getMessage("command.transferownership.playernotfound").toText());
                        }
                        e.manager.showGUI(e.player, this);
                    });
                    guiManager.showGUI(event.player, gui);
                });

        // enable page events
        setNextPage(5, 6, GuiUtils.createButtonItem(XMaterial.ARROW, plugin.getLocale().getMessage("general.interface.next").getMessage()));
        setPrevPage(5, 2, GuiUtils.createButtonItem(XMaterial.ARROW, plugin.getLocale().getMessage("general.interface.previous").getMessage()));
        setOnPage((event) -> showPage());
        showPage();
    }

    private void showPage() {
        // refresh stats
        this.setItem("stats", 4, GuiUtils.updateItem(this.getItem(4),
                this.plugin.getLocale().getMessage("interface.members.statstitle").toText(),
                this.plugin.getLocale().getMessage("interface.members.statslore")
                        .processPlaceholder("members", rift.getMembers().size()).toText().split("\\|")));

        // Sort button
        this.setItem("sort", 5, GuiUtils.updateItem(this.getItem(5),
                this.plugin.getLocale().getMessage("interface.members.changesorttitle").toText(),
                this.plugin.getLocale().getMessage("general.interface.current")
                        .processPlaceholder("current",
                                this.plugin.getLocale().getMessage(this.sortType.getLocalePath()).toText())
                        .toText().split("\\|")));

        // show members
        List<UUID> toDisplay = new ArrayList<>(rift.getMembers().keySet());

        if (this.sortType == SortType.MEMBER_SINCE) {
            toDisplay = toDisplay.stream().sorted(Comparator.comparingLong(uuid -> {
                if (uuid.equals(rift.getOwner().getUniqueId()))
                    return 0;
                return rift.getMembers().get(uuid).getTimestamp();
            })).collect(Collectors.toList());
        }

        Collections.reverse(toDisplay);
        this.pages = (int) Math.max(1, Math.ceil(toDisplay.size() / (7 * 4)));
        this.page = Math.max(this.page, this.pages);

        int currentMember = 21 * (this.page - 1);
        for (int row = 1; row < this.rows - 1; row++) {
            for (int col = 1; col < 8; col++) {
                if (toDisplay.size() - 1 < currentMember) {
                    this.clearActions(row, col);
                    this.setItem(row, col, AIR);
                    continue;
                }

                UUID playerUUID = toDisplay.get(currentMember);
                OfflinePlayer skullPlayer = Bukkit.getOfflinePlayer(playerUUID);
                Member member = rift.getMembers().get(playerUUID);

                List<String> lore = new ArrayList<>(Arrays.asList(plugin.getLocale().getMessage("interface.members.skulllore").toText().split("\\|")));
                lore.add("");
                if (member.isOwner()) {
                    lore.add(ChatColor.YELLOW + plugin.getLocale().getMessage("interface.members.owner").toText());
                } else {
                    lore.add(plugin.getLocale().getMessage("interface.members.member").toText());
                    lore.add("");
                    lore.add(plugin.getLocale().getMessage("interface.members.membersince")
                            .processPlaceholder("membersince", new SimpleDateFormat("dd/MM/yyyy").format(new Date(member.getTimestamp())))
                            .toText());
                    this.setAction(row, col, (event) -> {
                        rift.removeMember(playerUUID);
                        member.delete();

                        showPage();
                    });
                }
                this.setItem(row, col, GuiUtils.createButtonItem(ItemUtils.getPlayerSkull(skullPlayer), ChatColor.AQUA + skullPlayer.getName(), lore));
                currentMember++;
            }
        }
    }

    void toggleSort() {
        switch (this.sortType) {
            case DEFAULT:
                this.sortType = SortType.MEMBER_SINCE;
                break;
            case MEMBER_SINCE:
                this.sortType = SortType.DEFAULT;
                break;
        }
        showPage();
    }

    public enum SortType {
        DEFAULT("interface.members.sortingmode.default"),
        MEMBER_SINCE("interface.members.sortingmode.membersince");

        private final String localePath;

        SortType(String localePath) {
            this.localePath = localePath;
        }

        public String getLocalePath() {
            return this.localePath;
        }
    }
}