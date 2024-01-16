package com.NameChangeDetector;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

//Some code taken from while-loop/runelite-plugin runewatch to help with menu entry creation.
//https://github.com/while-loop/runelite-plugins/blob/runewatch/src/main/java/com/runewatch/RuneWatchPlugin.java


@Slf4j
@PluginDescriptor(
	name = "Name Change Detector"
)
public class NameChangeDetectorPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private NameChangeManager nameChangeManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ScheduledExecutorService executor;

	private static final String INVESTIGATE = "Previous names";

	private static final List<Integer> MENU_WIDGET_IDS = ImmutableList.of(
		InterfaceID.FRIEND_LIST,
		ComponentID.CHATBOX_FRAME,
		InterfaceID.RAIDING_PARTY,
		InterfaceID.PRIVATE_CHAT,
		InterfaceID.FRIENDS_CHAT,
		InterfaceID.IGNORE_LIST,
		ToGroup(ComponentID.CLAN_MEMBERS),
		ToGroup(ComponentID.CLAN_GUEST_MEMBERS)
	);

	private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of(
		"Message", "Add ignore", "Remove friend", "Delete", "Kick", "Reject"
	);


	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {

		int groupId = ToGroup(event.getActionParam1());
		String option = event.getOption();

		if (!MENU_WIDGET_IDS.contains(groupId) || !AFTER_OPTIONS.contains(option)) {
			return;
		}

		for (MenuEntry me : client.getMenuEntries()) {
			// don't add menu option if we've already added investigate
			if (INVESTIGATE.equals(me.getOption())) {
				return;
			}
		}

		client.createMenuEntry(-1)
			.setOption(INVESTIGATE)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.setParam0(event.getActionParam0())
			.setParam1(event.getActionParam1())
			.setIdentifier(event.getIdentifier());
	}

	//https://github.com/while-loop/runelite-plugins/blob/2b3ded2bb6d12546bf46884c6ec0d876cce99636/src/main/java/com/runewatch/RuneWatchPlugin.java#L303
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		String option = event.getMenuOption();
		MenuAction action = event.getMenuAction();


		if ((action == MenuAction.RUNELITE || action == MenuAction.RUNELITE_PLAYER) && option.equals(INVESTIGATE)) {
			final String target;
			if (action == MenuAction.RUNELITE_PLAYER) {
				// The player id is included in the event, so we can use that to get the player name,
				// which avoids having to parse out the combat level and any icons preceding the name.
				Player player = client.getCachedPlayers()[event.getId()];
				if (player != null) {
					target = player.getName();
				} else {
					target = null;
				}
			} else {
				target = Text.removeTags(event.getMenuTarget());
			}

			if (target != null) {
				//https://api.wiseoldman.net/players/username/harming/names
				//https://crystalmathlabs.com/tracker/api.php?type=previousname&player=Harming
				executor.execute(() -> {
					List<String> names = nameChangeManager.getPreviousNames(target);
					printPreviousNames(target, names);
				});

			}
		}

	}


	private void printPreviousNames(String currentRsn, List<String> names) {
		currentRsn = Text.toJagexName(currentRsn);

		ChatMessageBuilder response = new ChatMessageBuilder();

		long countOfNames = names.stream().count();

		if( countOfNames > 0){
			response.append(ChatColorType.HIGHLIGHT).append(currentRsn).append(ChatColorType.NORMAL);
			response.append(" has also gone by: ").append(ChatColorType.HIGHLIGHT);
			int timesRan = 1;
			boolean whoCaresAboutAOxfordComma = false;
			for (String name : names)
			{

				if(whoCaresAboutAOxfordComma && timesRan == countOfNames){
					response.append(ChatColorType.NORMAL).append(" and ").append(ChatColorType.HIGHLIGHT);
				}
				response.append(name);

				if(timesRan != countOfNames){
					response.append(ChatColorType.NORMAL).append(", ").append(ChatColorType.HIGHLIGHT);
				}

				if(timesRan + 1 == countOfNames){
					whoCaresAboutAOxfordComma = true;
				}
				timesRan++;
			}

		}else{
			response.append("No previous names were found for ")
				.append(ChatColorType.HIGHLIGHT);
			response.append(currentRsn);
		}

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(response.build())
			.build());
	}

	public static int ToGroup(int id) {
		return id >>> 16;
	}
}
