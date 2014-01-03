package de.derflash.minecapes;

import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;

import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;
import java.util.logging.Handler;
import java.lang.reflect.Field;
import java.net.*;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.src.*;
import net.minecraft.util.ResourceLocation;

@Mod(modid = MineCapes.MODID, version = MineCapes.VERSION)
public class MineCapes
{
    public static final String MODID = "minecapes";
    public static final String VERSION = "2.1-SNAPSHOT";
    
    int tick = 0;
    int sweep = 20;

    private ArrayList<String> capeDIRs = null;

    private HashMap<String, ThreadDownloadImageData> checked = new HashMap<String, ThreadDownloadImageData>();
    private ArrayList<String> applied = new ArrayList<String>();
    private ArrayList<String> ignored = new ArrayList<String>();
    private ArrayList<String> players = new ArrayList<String>();

    
    private String stdCapesDir    = "http://www.minecapes.net/players/cape/";
    private String hdCapesDir    = "http://www.minecapes.net/players/hdcape/";
    
    boolean checking = false;
    boolean shouldClear = false;
    int _iCanHazHDMod = -1;
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        findCapesDirectories();
        
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void tick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            updateCloakURLs();
        }
    }
    
    /*
     * Removed for now due to update to 1.7.x
     * 
    // ModLoader @ MC 1.3+
    public void clientConnect() {
        checkForUpdate();
    }
    
    // ModLoader @ MC 1.3+
    public void clientCustomPayload(NetClientHandler clientHandler, Packet250CustomPayload packet250custompayload) {
        if (packet250custompayload.channel.equalsIgnoreCase("minecapes")) {
            handleMCMessage(new String(packet250custompayload.data));
        }
    }
    */
    
    //////////////////////////////////////////
    ///// private stuff
    //////////////////////////////////////////
    
    private void checkForUpdate() {
        System.out.println("[MineCapes] Checking for a new MineCapes Version now...");

        new Thread() {
            public void run() {
                try {
                    URL dirList = new URL("http://www.minecapes.net/version");
                        BufferedReader in = new BufferedReader(new InputStreamReader(dirList.openStream()));

                    final String inputLine = in.readLine();
                    if (inputLine != null && !inputLine.equals(VERSION)) {
                        System.out.println("There's a new version of MineCapes (Version "+inputLine+")! Go get it from: minecapes.net/install");
                        
                        new java.util.Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                for (int i = 0; i < 10; i++) {
                                    if (Minecraft.getMinecraft().inGameHasFocus && Minecraft.getMinecraft().thePlayer != null) {
                                        Minecraft.getMinecraft().thePlayer.sendChatMessage("There's a new version of MineCapes (Version "+inputLine+")! Go get it from: minecapes.net/install");
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
    
    private void updateCloakURLs() {
        Minecraft mc = Minecraft.getMinecraft();
        
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
                   String playerName = entityplayer.getCommandSenderName();
                   players.add(playerName);
                   
                   ThreadDownloadImageData usersCape = checked.get(playerName);
                   
                   if (usersCape != null) {

                       AbstractClientPlayer aPlayer = (AbstractClientPlayer) entityplayer;
                       ThreadDownloadImageData currentCape = null;
                       Field downloadImageCape = null;
                    
                       // make cloak resource field accessible and get current cape
                       try {
                           downloadImageCape = AbstractClientPlayer.class.getDeclaredField("field_110315_c");
                           
                           if (!downloadImageCape.isAccessible()) downloadImageCape.setAccessible(true);
                           
                           currentCape = (ThreadDownloadImageData) downloadImageCape.get(aPlayer);
                           
                       } catch (Exception e) {
                    	   e.printStackTrace();
                       }
                    
                    
                       // check if needs update
                       if (downloadImageCape != null && usersCape != currentCape) {

                           System.out.println("[MineCapes] Applying (new) cape for: " + playerName);

                           // set as users cloak resource
                           try {
                               downloadImageCape.set(aPlayer, usersCape);
                           } catch (Exception e) {
                        	   e.printStackTrace();
                           }
                           
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
                    HttpURLConnection con =    (HttpURLConnection) new URL(url).openConnection();
                    con.setRequestMethod("HEAD");
                    con.setRequestProperty ( "User-agent", "MineCapes " + VERSION);
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
                   ResourceLocation resourcePackCloak = aPlayer.getLocationCape(aPlayer.getCommandSenderName());
                
                   TextureManager texturemanager = Minecraft.getMinecraft().getTextureManager();
                   ThreadDownloadImageData object = new ThreadDownloadImageData(found, null, null);
                   texturemanager.loadTexture(resourcePackCloak, (ITextureObject)object);
                   
                   checked.put(playerName, object);
                   System.out.println("[MineCapes] Found cloak: " + found);

               }
               
               
           }        
    }

}