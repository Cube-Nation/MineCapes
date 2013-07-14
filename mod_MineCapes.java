import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;
import java.util.logging.Handler;
import java.lang.reflect.Field;
import java.net.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.src.*;
import net.minecraft.util.ResourceLocation;

public class mod_MineCapes extends BaseMod
{
	int tick = 0;
	int sweep = 20;

	private ArrayList<String> capeDIRs = null;

	private HashMap<String, ThreadDownloadImageData> checked = new HashMap<String, ThreadDownloadImageData>();
	private ArrayList<String> applied = new ArrayList<String>();
	private ArrayList<String> ignored = new ArrayList<String>();
	private ArrayList<String> players = new ArrayList<String>();

	
	private String stdCapesDir	= "http://www.minecapes.net/players/cape/";
	private String hdCapesDir	= "http://www.minecapes.net/players/hdcape/";
	
    boolean checking = false;
    boolean shouldClear = false;
    int _iCanHazHDMod = -1;

    public mod_MineCapes() {
    }
    
	public String getVersion() {
    	return "2.0";
	}
    
    public void load() {
		ModLoader.setInGameHook(this, true, true);
		ModLoader.registerPacketChannel(this, "minecapes");
		
		findCapesDirectories();
    }
    
    // ModLoader @ MC 1.2.5
    public void serverConnect(NetClientHandler netclienthandler) {
    	checkForUpdate(netclienthandler);
    }
	// ModLoader @ MC 1.3+
    public void clientConnect(NetClientHandler netclienthandler) {
    	serverConnect(netclienthandler);
    }
    

    // ModLoader @ MC 1.2.5
	public void receiveCustomPacket(Packet250CustomPayload packet250custompayload) {
		if (packet250custompayload.channel.equalsIgnoreCase("minecapes")) {
			handleMCMessage(new String(packet250custompayload.data));
		}
	}
	// ModLoader @ MC 1.3+
	public void clientCustomPayload(NetClientHandler clientHandler, Packet250CustomPayload packet250custompayload) {
		receiveCustomPacket(packet250custompayload);
    }
    
    public boolean onTickInGame(float f, Minecraft minecraft)
    {
    	updateCloakURLs(minecraft);
       	return true;
    }
    
    
    
    //////////////////////////////////////////
    ///// private stuff
    //////////////////////////////////////////
    
    private void checkForUpdate(NetClientHandler netclienthandler) {
    	System.out.println("[MineCapes] Checking for a new MineCapes Version now...");

    	new Thread() {
            public void run() {
            	try {
        	        URL dirList = new URL("http://www.minecapes.net/version");
           	     	BufferedReader in = new BufferedReader(new InputStreamReader(dirList.openStream()));

                	final String inputLine = in.readLine();
                	if (inputLine != null && !inputLine.equals(getVersion())) {
            			System.out.println("There's a new version of MineCapes (Version "+inputLine+")! Go get it from: minecapes.net/install");
            			
                		new java.util.Timer().schedule(new TimerTask() {
        					@Override
        					public void run() {
        						for (int i = 0; i < 10; i++) {
        							if (ModLoader.getMinecraftInstance().inGameHasFocus && ModLoader.getMinecraftInstance().thePlayer != null) {
                				    	ModLoader.getMinecraftInstance().thePlayer.addChatMessage("There's a new version of MineCapes (Version "+inputLine+")! Go get it from: minecapes.net/install");
                				    	try { Thread.sleep(1000); } catch (InterruptedException e) {}
                				    	return;
        							}
        						}
        					}}, 5000);
                	}
        		} catch (Exception e) {
        			System.out.println("[MineCapes] Could not check for a new MineCapes Version :-(");

        		}
            }
    	}.start();
    }
    

    private void clearCloaks(List<EntityPlayer> playerEntities, Minecraft mc) {
   		System.out.println("[MineCapes] Clearing capes...");
   		
    	checked.clear();
    	ignored.clear();
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
    
    private void updateCloakURLs(Minecraft mc) {
    	if (capeDIRs == null || capeDIRs.isEmpty()) return;
    	if (checking) return;
    	
    	if (tick >= sweep) {
    		tick = 0;
    		
    		if (mc == null || mc.theWorld == null || mc.theWorld.playerEntities == null || mc.renderEngine == null) {
    			return;
    		}
    		
        	final List<EntityPlayer> playerEntities = mc.theWorld.playerEntities; //get the players

        	// clear cloaks if requested
        	if (shouldClear) {
        		shouldClear = false;
        		clearCloaks(playerEntities, mc);
        		tick = sweep;
        		return;
        	}
        	
        	// apply found cloaks / find players
        	players.clear();
        	for (EntityPlayer entityplayer : playerEntities) {
        	   	String playerName = entityplayer.username;
        	   	players.add(playerName);
        	   	
        	   	ThreadDownloadImageData usersCape = checked.get(playerName);
        	   	
        	   	if (usersCape != null) {

            		AbstractClientPlayer aPlayer = (AbstractClientPlayer) entityplayer;
            	   	ThreadDownloadImageData currentCape = null;
            	   	Field field_110315_c = null;
            		
            		// make cloak resource field accessible and get current cape
                    try {
                		field_110315_c = AbstractClientPlayer.class.getDeclaredField("field_110315_c");
                		
                        if (!field_110315_c.isAccessible()) field_110315_c.setAccessible(true);
                        
                        currentCape = (ThreadDownloadImageData) field_110315_c.get(aPlayer);
                        
    				} catch (Exception e) { e.printStackTrace(); }
                    
                    
                    // check if needs update
        	   		if (field_110315_c != null && usersCape != currentCape) {

        		   		System.out.println("[MineCapes] Applying cape for: " + playerName);

                        // set as users cloak resource
                        try {
    						field_110315_c.set(aPlayer, usersCape);
    					} catch (Exception e) { e.printStackTrace(); }
        	   			
        	   		}
        	   		
        	   	}

        	}
    		
        	// run cloak find in another thread
    		checking = true;
    		Thread checkThread = new Thread() {
                public void run() {
            		checkCloakURLs(players);
                	checking = false;
                }
    		};
    		checkThread.setPriority(Thread.MIN_PRIORITY);
    		checkThread.start();
    		
       	} else {
       		tick++;
       	}
       	
    }
    
    private String removeColorFromString(String string) {
    	string = string.replaceAll("\\xa4\\w", "");
    	string = string.replaceAll("\\xa7\\w", "");
    	
    	return string;
    }

	protected void checkCloakURLs(List<String> playerNames) {		
    	for (String playerName : playerNames) {    		
    	   	
    	   	if (ignored.contains(playerName) || checked.containsKey(playerName)) continue;
    	   	
	   		System.out.println("[MineCapes] Found new player: " + playerName);
	   		
	   		String found = null;
	   		for (String capeURLcheck : capeDIRs) {

	   			String url = capeURLcheck + removeColorFromString(playerName) + ".png";
	  			try {
					HttpURLConnection con =	(HttpURLConnection) new URL(url).openConnection();
					con.setRequestMethod("HEAD");
					con.setRequestProperty ( "User-agent", "MineCapes " + getVersion());
					con.setRequestProperty ( "Java-Version", System.getProperty("java.version"));
					con.setConnectTimeout(2000);
					con.setUseCaches(false);
					con.setFollowRedirects(false);
					
					if (con.getResponseCode() == HttpURLConnection.HTTP_OK) found = url;

					con.disconnect();
					
				} catch (Exception e) {}
	  			

				if (found != null) break;
	   		}
	   		
	   		if (found == null) {
	   			ignored.add(playerName);
	   			System.out.println("[MineCapes] Could not find any cloak, ignoring ...");
	   			
	   		} else {
        		AbstractClientPlayer aPlayer = (AbstractClientPlayer) Minecraft.getMinecraft().theWorld.getPlayerEntityByName(playerName);

                // get cloak
                ResourceLocation resourcePackCloak = aPlayer.func_110299_g(aPlayer.username);
                
                TextureManager texturemanager = Minecraft.getMinecraft().func_110434_K();
                ThreadDownloadImageData object = new ThreadDownloadImageData(found, null, null);
                texturemanager.func_110579_a(resourcePackCloak, (TextureObject)object);
	   			
	       		checked.put(playerName, object);
				System.out.println("[MineCapes] Found cloak: " + found);

	   		}
	   		
       		
   		}		
	}

}
