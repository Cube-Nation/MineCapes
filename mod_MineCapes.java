package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;
import java.net.*;
import net.minecraft.client.Minecraft;

public class mod_MineCapes extends BaseMod
{
	int tick = 1337;
	int sweep = 60;

	private ArrayList<String> capeDIRs = null;

	private HashMap<String, String> checked = new HashMap<String, String>();
	private ArrayList<String> ignored = new ArrayList<String>();
	
	private String stdCapesDir	= "http://www.minecapes.net/players/cape/";
	private String hdCapesDir	= "http://www.minecapes.net/players/hdcape/";
	
    boolean checking = false;
    boolean shouldClear = false;
    int _iCanHazHDMod = -1;

    public mod_MineCapes() {
    }
    
	public String getVersion() {
    	return "1.8";
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
    
    private void clearCloaks(List<EntityPlayer> playerEntities, Minecraft mc) {
    	checked.clear();
    	ignored.clear();
    	
    	for (EntityPlayer entityplayer : playerEntities)
   		{
    	   	String cloakURL = entityplayer.cloakUrl;
    	   	if (cloakURL != null) {
    	   		mc.renderEngine.releaseImageData(cloakURL);
           		System.out.println("[MineCapes] Cleared cape for " + entityplayer.username);
    	   	}
   		}
    }
    
    private void findCapesDirectories() {
    	new Thread() {
            public void run() {
           		System.out.println("[MineCapes] Searching for capes directories ...");
           		
    			ArrayList<String> _capeDIRs = new ArrayList<String>();
        		try {
        	        URL dirList = new URL("http://www.minecapes.net/capesDirectory.list");
           	     	BufferedReader in = new BufferedReader(new InputStreamReader(dirList.openStream()));

                	String inputLine;
                	while ((inputLine = in.readLine()) != null) _capeDIRs.add(inputLine);
                	in.close();
        			
        		} catch (Exception e) {
        			System.out.println("[MineCapes] External cape directories could not be found. Try again on restart...");
        		}

        		// add default dir
        		_capeDIRs.add(0, stdCapesDir);

        		if (iCanHazHDMod()) {
        			System.out.println("[MineCapes] Found HD Patch! Adding HD directory.");
        			_capeDIRs.add(0, hdCapesDir);
        		}

    			System.out.println("[MineCapes] " + _capeDIRs.size() + " directories loaded!");
    			capeDIRs = _capeDIRs;
            }

    	}.start();

	}
    
    private void handleMCMessage(String message) {
		System.out.println("[MineCapes] Got a message: " + message);
	    if (message.equalsIgnoreCase("reloadCapes")) {
	    	shouldClear = true;
	    }
    }
    
    private boolean iCanHazHDMod() {
    	if (_iCanHazHDMod == -1) {
        	if (new ImageBufferDownload().parseUserSkin(new BufferedImage(128,64,BufferedImage.TYPE_INT_RGB)).getWidth() == 64) {
        		_iCanHazHDMod = 0;
        	} else {
        		_iCanHazHDMod = 1;
        	}
    	}
    	return (_iCanHazHDMod == 1 ? true : false);
    	
    }
    
    private void updateCloakURLs() {
    	if (capeDIRs == null || capeDIRs.isEmpty()) return;
    	if (checking) return;
    	
    	if (tick >= sweep) {
    		tick = 0;
    		checking = true;
    		
    		Thread checkThread = new Thread() {
                public void run() {
        	    	Minecraft mc = ModLoader.getMinecraftInstance();
            		if (mc == null || mc.theWorld == null || mc.theWorld.playerEntities == null || mc.renderEngine == null) return;
            	
                	List<EntityPlayer> playerEntities = mc.theWorld.playerEntities; //get the players
                	
                	if (shouldClear) {
                		shouldClear = false;
                		clearCloaks(playerEntities, mc);
                	}
                	
            		checkCloakURLs(playerEntities, mc);
                	
                	checking = false;
                }
    		};
    		
    		checkThread.setPriority(Thread.MIN_PRIORITY);
    		checkThread.start();
    		
       	} else {
       		tick++;
       	}
       	
    }

	protected void checkCloakURLs(List<EntityPlayer> playerEntities, Minecraft mc) {		
    	for (EntityPlayer entityplayer : playerEntities) {    		
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
    	   		System.out.println("[MineCapes] Found new player: " + playerName);
    	   		
    	   		String found = null;
    	   		for (String capeURLcheck : capeDIRs) {

    	   			String url = capeURLcheck + playerName + ".png";
    	  			try {
  						HttpURLConnection con =	(HttpURLConnection) new URL(url).openConnection();
  						con.setRequestMethod("HEAD");
  						con.setRequestProperty ( "User-agent", "MineCapes " + getVersion());
  						con.setRequestProperty ( "Java-Version", System.getProperty("java.version"));
  						con.setConnectTimeout(2000);
  						con.setDefaultUseCaches(false);
  						con.setFollowRedirects(false);
						if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
							System.out.println("[MineCapes] Found cloak: " + url);
			           		found = url;
						}
						con.disconnect();
						
					} catch (Exception e) {}

					if (found != null) break;
    	   		}
    	   		    	   		
    	   		if (found != null) {
	           		checked.put(playerName, found);
	           		
    	   			entityplayer.playerCloakUrl = found;
	        		entityplayer.cloakUrl = found;
        			mc.renderEngine.obtainImageData(found, new ImageBufferDownload());

    	   		} else {
    	   			ignored.add(playerName);
    	   			System.out.println("[MineCapes] Could not find any cloak, ignoring ...");
				}
       		}
       		
   		}		
	}

}
