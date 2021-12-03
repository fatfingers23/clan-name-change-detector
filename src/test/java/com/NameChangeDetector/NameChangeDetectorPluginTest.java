package com.NameChangeDetector;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class NameChangeDetectorPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(NameChangeDetectorPlugin.class);
		RuneLite.main(args);
	}
}