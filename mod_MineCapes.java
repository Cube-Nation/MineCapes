package net.minecraft.src;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;
import java.net.*;
import net.minecraft.client.Minecraft;

public class mod_MineCapes extends BaseMod
{
	int tick;
	int sweep = 20;
	
	private HashMap<String, String> checked = new HashMap<String, String>();
	private ArrayList<String> ignored = new ArrayList<String>();
	private ArrayList<String> capeURLs = null;
	private Thread checkThread = null;
	
    boolean checking = false;
    boolean reloading = false;

    public mod_MineCapes() {
    }
    
	public String getVersion() {
    	return "1.3";
	}
    
    public void load() {
		ModLoader.setInGameHook(this, true, true);
		ModLoader.registerPacketChannel(this, "minecapes");
		
		findCapesDirectories();
    }
    
    public void serverConnect(NetClientHandler netclienthandler) {
    	checkForUpdate();
    }
    
	public void receiveCustomPacket(Packet250CustomPayload packet250custompayload) {
		if (packet250custompayload.channel.equalsIgnoreCase("minecapes")) {
			handleMCMessage(new String(packet250custompayload.data));
		}
    }
    
    public boolean onTickInGame(float f, Minecraft minecraft)
    {
    	updateCloakURLs();
       	return true;
    }
    
    
    
    //////////////////////////////////////////
    ///// private stuff
    //////////////////////////////////////////
    
    private void cancelCheck() {
    	if (checking && checkThread != null) {
	    	checkThread.interrupt();
	    	checkThread = null;
	    	checking = false;
    	}
    }
    
    private void checkForUpdate() {
    	new Thread() {
            public void run() {
            	try {
        	        URL dirList = new URL("http://www.minecapes.net/version");
           	     	BufferedReader in = new BufferedReader(new InputStreamReader(dirList.openStream()));

                	final String inputLine = in.readLine();
                	if (inputLine != null && !inputLine.equals(getVersion())) {
                		new java.util.Timer().schedule(new TimerTask() {
        					@Override
        					public void run() {
        				    	ModLoader.getMinecraftInstance().thePlayer.addChatMessage("There's a new version of MineCapes (Version "+inputLine+")! Go get it from: minecapes.net/install");
        					}}, 2000);
                	}
        		} catch (Exception e) {}
            }
    	}.start();
    }
    
    private void clearCloaks() {
    	Minecraft mc = ModLoader.getMinecraftInstance();
    	List playerEntities = mc.theWorld.playerEntities; //get the players
    	for (int i = 0; i < playerEntities.size(); i++)
   		{
    	   	EntityPlayer entityplayer = (EntityPlayer)playerEntities.get(i);
    	   	String cloakURL = entityplayer.cloakUrl;
    	   	if (cloakURL != null) {
    	   		mc.renderEngine.releaseImageData(cloakURL);
           		System.out.println("[MineCapes] Cleared cape for " + entityplayer.username);
    	   	}
   		}

    	checked.clear();
    	ignored.clear();
    }
    
    private void findCapesDirectories() {
    	new Thread() {
            public void run() {
           		System.out.println("[MineCapes] Searching for capes directories ...");
        		try {
        			ArrayList<String> _capeURLs = new ArrayList<String>();

        	        URL dirList = new URL("http://www.minecapes.net/capesDirectory.list");
           	     	BufferedReader in = new BufferedReader(new InputStreamReader(dirList.openStream()));

                	String inputLine;
                	while ((inputLine = in.readLine()) != null) _capeURLs.add(inputLine);
                	in.close();
                	
        			System.out.println("[MineCapes] " + _capeURLs.size() + " Directories loaded!");
        			capeURLs = _capeURLs;
        			
        		} catch (Exception e) {
        			System.out.println("[MineCapes] Directories could not be loaded. Try again on restart...");

        		}
            }

    	}.start();

	}
    
    private void handleMCMessage(String message) {
		System.out.println("[MineCapes] Got a message: " + message);
	    if (message.equalsIgnoreCase("reloadCapes")) {
			reloading = true;
			
			cancelCheck();
	    	clearCloaks();

	    	reloading = false;
	    }
    }
    
    private void updateCloakURLs() {
    	if (capeURLs == null) return;
    	if (checking || reloading) return;
    	
    	if (tick >= sweep) {
    		tick = 0;
    		
    		checking = true;
    		checkThread = new Thread() {
                public void run() {
        	    	Minecraft mc = ModLoader.getMinecraftInstance();
            		if (mc == null || mc.theWorld == null || mc.theWorld.playerEntities == null || mc.renderEngine == null) return;
            	
                	List playerEntities = mc.theWorld.playerEntities; //get the players
                	for (int i = 0; i < playerEntities.size(); i++)
               		{
                		if (Thread.interrupted()) break;
                		
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
        							
        								System.out.println("[MineCapes] Found cloak: " + url);

        		       					entityplayer.playerCloakUrl = url;
        		        				entityplayer.cloakUrl = url;
                    					mc.renderEngine.obtainImageData(url, new ImageBufferDownload());
                    					
                                		if (Thread.interrupted()) break;
        				           		checked.put(entityplayer.username, url);
        				           		found = true;
        				           		break;

        							}
        							
            					} catch (Exception e) {}

                	   		}
                	   		
                    		if (Thread.interrupted()) break;
                	   		
                	   		if (!found) {
                	   			ignored.add(entityplayer.username);
                	   			System.out.println("[MineCapes] Could not find any cloak, ignoring ...");
        					}
                   		}
                   		
               		}
                	
                	checking = false;
                	checkThread = null;
                }
    		};
    		
    		checkThread.setPriority(Thread.MIN_PRIORITY);
    		checkThread.start();
    		
       	} else {
       		tick++;
       	}
       	
    }

}
