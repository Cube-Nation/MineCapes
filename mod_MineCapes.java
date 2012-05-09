package net.minecraft.src;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;
import java.net.*;
import net.minecraft.client.Minecraft;

public class mod_MineCapes extends BaseMod
{
	int tick;
	private HashMap<String, String> checked = new HashMap<String, String>();
	private ArrayList<String> ignored = new ArrayList<String>();
	private ArrayList<String> capeURLs = new ArrayList<String>();
	
    public mod_MineCapes() {
    }

	public String getVersion() {
    	return "1.1";
	}
    
    public void load() {
		ModLoader.setInGameHook(this, true, true);
		ModLoader.registerPacketChannel(this, "minecapes");

		System.out.println("[MineCapes] Searching for capes directories ...");
		try {
	        URL dirList = new URL("http://www.minecapes.net/capesDirectory.list");
   	     	BufferedReader in = new BufferedReader(new InputStreamReader(dirList.openStream()));

        	String inputLine;
        	while ((inputLine = in.readLine()) != null) capeURLs.add(inputLine);
        	in.close();
			System.out.println("[MineCapes] " + capeURLs.size() + " Directories loaded!");
			
		} catch (Exception e) {
			System.out.println("[MineCapes] " + capeURLs.size() + " Directories could not be loaded. Try again on restart...");

		}

    }
    
    public void serverConnect(NetClientHandler netclienthandler) {
    	try {
	        URL dirList = new URL("http://www.minecapes.net/version");
   	     	BufferedReader in = new BufferedReader(new InputStreamReader(dirList.openStream()));

        	String inputLine = in.readLine();
        	if (inputLine != null && !inputLine.equals(getVersion())) {
		    	ModLoader.getMinecraftInstance().thePlayer.addChatMessage("Es gibt eine neue Version von MineCapes (Version "+inputLine+")!");
        	}
		} catch (Exception e) {}
    }
    
	public void receiveCustomPacket(Packet250CustomPayload packet250custompayload) {
		if (packet250custompayload.channel.equalsIgnoreCase("minecapes")) {
			String message = new String(packet250custompayload.data);
		    if (message.equalsIgnoreCase("reloadCapes")) {
		    	checked.clear();
		    	ignored.clear();
		    	updateCloakURLs();
		    }
		}
    }
    
    public boolean onTickInGame(float f, Minecraft minecraft)
    {
    	updateCloakURLs();
       	return true;
    }
    
    public void updateCloakURLs() {
    	if (tick == 20) {
    		tick = 0;
    	
	    	Minecraft mc = ModLoader.getMinecraftInstance();
    		if (mc == null || mc.theWorld == null || mc.theWorld.playerEntities == null || mc.renderEngine == null) return;
    	
        	List playerEntities = mc.theWorld.playerEntities; //get the player
        	for (int i = 0; i < playerEntities.size(); i++)
       		{
        	   	EntityPlayer entityplayer = (EntityPlayer)playerEntities.get(i);
        	   	String playerName = entityplayer.username;
        	   	
        	   	if (ignored.contains(playerName)) {
        	   		// ignore
        	   		
        	   	} else if (checked.containsKey(playerName)) {
        	   		String checkURL = checked.get(playerName);
        	   		
        	   		if (!entityplayer.playerCloakUrl.equalsIgnoreCase(checkURL)) {
        	   			entityplayer.playerCloakUrl = checkURL;
		        		entityplayer.cloakUrl = checkURL;
            			mc.renderEngine.obtainImageData(checkURL, new ImageBufferDownload());
        	   		}
        	   		
        	   	} else {
        	   	
        	   		System.out.println("[MineCapes] Found new player: " + entityplayer.username);

        	   		
        	   		boolean found = false;
        	   		for (String capeURLcheck : capeURLs) {
        	  			String url = (new StringBuilder()).append(capeURLcheck).append(playerName).append(".png").toString();
        	  			
        	  			try {
      						HttpURLConnection.setFollowRedirects(false);
      						HttpURLConnection con =	(HttpURLConnection) new URL(url).openConnection();
      						con.setRequestMethod("HEAD");
							if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
							
								System.out.println("[MineCapes] Found cloak at: " + capeURLcheck);

		       					entityplayer.playerCloakUrl = url;
		        				entityplayer.cloakUrl = url;
            					mc.renderEngine.obtainImageData(entityplayer.cloakUrl, new ImageBufferDownload());
            					
				           		checked.put(entityplayer.username, url);
				           		found = true;
				           		break;

							}
							
    					} catch (Exception e) {}

        	   		}
        	   		
        	   		if (!found) {
        	   			System.out.println("[MineCapes] Could not find any cloak, ignoring ...");
        	   			ignored.add(entityplayer.username);
					}
           		}
           		
       		}
       		
       	} else {
       		tick++;
       	}
       	
    }

}
