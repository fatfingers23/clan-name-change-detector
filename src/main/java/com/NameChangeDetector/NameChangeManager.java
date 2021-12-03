package com.NameChangeDetector;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import static net.runelite.http.api.RuneLiteAPI.GSON;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Singleton
public class NameChangeManager
{

	private final OkHttpClient client;
	private final ClientThread clientThread;

	private static final Type typeToken = new TypeToken<List<WOMNameChangesModel>>() {
	}.getType();

	//https://api.wiseoldman.net/players/username/{PlayerName}/names
	//https://crystalmathlabs.com/tracker/api.php?type=previousname&player={PlayerName}
	private String wiseOldManBaseApiUrl = "https://api.wiseoldman.net";
	private String crystalMathBaseApiUrl = "https://crystalmathlabs.com";

	@Inject
	public NameChangeManager(OkHttpClient client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	public List<String> getPreviousNames(String rsn){
		String cleanRsn = Text.removeTags(Text.toJagexName(rsn)).toLowerCase();

		List<String> previousNames =  new ArrayList<>();
		List<String> namesFromWOM = this.getPreviousNamesFromWOM(cleanRsn);
		previousNames.addAll(namesFromWOM);
		return previousNames;
	}

	public List<String> getPreviousNamesFromWOM(String rsn){

		String url = this.wiseOldManBaseApiUrl + "/players/username/" + rsn + "/names";

		Request request = new Request.Builder().url(url).build();

		try(Response response = client.newCall(request).execute())
		{
			List<WOMNameChangesModel> nameChanges = GSON.fromJson(new InputStreamReader(response.body().byteStream()), typeToken);
			return nameChanges.stream().map(WOMNameChangesModel::getOldName).collect(Collectors.toList());

		}
		catch (IOException e)
		{
			log.error("failed to check Wise Old Man for name changes: {}", e.toString());
		}

		return Collections.emptyList();
	}
}
