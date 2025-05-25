package tvhbrowser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static devplugin.Plugin.getPluginManager;

/**
 * The ChannelManager class manages the mapping between TVBrowser channels and TVHeadend channels.
 */
public class ChannelManager {
    private final TvHBrowser tvhBrowser;
    private final TVHeadendConnection connection;

    private Map<String, String> channelToTVHMap;
    private Map<String, String> tvhToChannelMap;
    private Map<String, TVHeadendChannel> tvhChannelMap;
    private Map<String, devplugin.Channel> tvbChannelMap;

    /**
     * Constructs a ChannelManager object.
     *
     * @param tvhBrowser  The TvHBrowser instance.
     * @param connection  The TVHeadendConnection instance.
     */
    public ChannelManager(TvHBrowser tvhBrowser, TVHeadendConnection connection) {
        this.tvhBrowser = tvhBrowser;
        this.connection = connection;
        this.channelToTVHMap = new HashMap<>();
        this.tvhToChannelMap = new HashMap<>();
        this.tvhChannelMap = new HashMap<>();
        this.tvbChannelMap = new HashMap<>();
    }

    /**
     * Retrieves the TVBrowser channels and stores them in the tvbChannelMap.
     */
    public void getTVBrowserChannels() {
        this.tvbChannelMap.clear();
        devplugin.Channel[] channels = getPluginManager().getSubscribedChannels();
        for (devplugin.Channel channel : channels) {
            tvbChannelMap.put(channel.getUniqueId(), channel);
        }
    }

    /**
     * Imports the channel lists by retrieving both TVBrowser and TVHeadend channels.
     */
    public void importChannelLists() {
        getTVBrowserChannels();
        getTVHeadendChannels();
    }

    /**
     * Retrieves the TVHeadend channels and stores them in the tvhChannelMap.
     */
    public void getTVHeadendChannels() {
        this.tvhChannelMap.clear();

        List<TVHeadendChannel> channels = this.connection.getChannelList();
        for (TVHeadendChannel channel : channels) {
            String key = channel.getKey();
            this.tvhChannelMap.put(key, channel);
        }
    }

    /**
     * Maps a TVBrowser channel ID to a TVHeadend channel key.
     *
     * @param tvbChannelId    The TVBrowser channel ID.
     * @param tvhChannelKey   The TVHeadend channel key.
     */
    public void mapChannels(String tvbChannelId, String tvhChannelKey) {
        channelToTVHMap.put(tvbChannelId, tvhChannelKey);
        tvhToChannelMap.put(tvhChannelKey, tvbChannelId);
    }

    /**
     * Retrieves the TVHeadend channel with the specified key.
     *
     * @param tvhChannelKey   The TVHeadend channel key.
     * @return                The TVHeadendChannel object.
     */
    public TVHeadendChannel getTVHChannelbyKey(String tvhChannelKey) {
        return tvhChannelMap.get(tvhChannelKey);
    }

    /**
     * Retrieves the TVBrowser channel associated with the specified TVHeadend channel key.
     *
     * @param tvhChannelKey   The TVHeadend channel key.
     * @return                The devplugin.Channel object.
     */
    public devplugin.Channel getTVBChannelbyTVHChannelKey(String tvhChannelKey) {
        String channelId = tvhToChannelMap.get(tvhChannelKey);

        if (channelId == null) {
            return null;
        }

        return tvbChannelMap.get(channelId);
    }

    /**
     * Retrieves the TVBrowser channel with the specified ID.
     *
     * @param tvbChannelId    The TVBrowser channel ID.
     * @return                The devplugin.Channel object.
     */
    public devplugin.Channel getTVBChannelbyID(String tvbChannelId) {
        return tvbChannelMap.get(tvbChannelId);
    }

    /**
     * Retrieves the unmapped TVHeadend channels.
     *
     * @return    A collection of unmapped TVHeadendChannel objects.
     */
    public Collection<TVHeadendChannel> getUnmappedTVHChannels() {
        if (tvhChannelMap.size() == 0) {
            this.getTVHeadendChannels();
        }

        List<TVHeadendChannel> unmappedChannels = new ArrayList<>();
        for (TVHeadendChannel channel : tvhChannelMap.values()) {
            if (!tvhToChannelMap.containsKey(channel.getKey())) {
                unmappedChannels.add(channel);
            }
        }
        return unmappedChannels;
    }

    /**
     * Maps a TVBrowser channel name to a TVHeadend channel name.
     *
     * @param tvbChannelName   The TVBrowser channel name.
     * @param tvhChannelName   The TVHeadend channel name.
     */
    public void mapChannelsByName(String tvbChannelName, String tvhChannelName) {
        devplugin.Channel tvbChannel = getTVBChannelbyName(tvbChannelName);
        TVHeadendChannel tvhChannel = getTVHChannelbyName(tvhChannelName);

        if (tvbChannel != null && tvhChannel != null) {
            mapChannels(tvbChannel.getUniqueId(), tvhChannel.getKey());
        } else {
            if (tvbChannel == null) {
                this.tvhBrowser.showError("Channel mapping failed: TVB channel not found. " + tvbChannelName);
            }
            if (tvhChannel == null) {
                this.tvhBrowser.showError("Channel mapping failed: TVH channel not found. " + tvhChannelName);
            }
        }
    }

    /**
     * Unmaps a TVBrowser channel name from a TVHeadend channel name.
     *
     * @param tvbChannelName   The TVBrowser channel name.
     * @param tvhChannelName   The TVHeadend channel name.
     */
    public void unmapChannelsByName(String tvbChannelName, String tvhChannelName) {
        devplugin.Channel tvbChannel = getTVBChannelbyName(tvbChannelName);
        TVHeadendChannel tvhChannel = getTVHChannelbyName(tvhChannelName);

        if (tvbChannel != null && tvhChannel != null) {
            writeLog("Remove vom channelToTVHMap:" + tvbChannelName + " " + tvbChannel.getKey());
            channelToTVHMap.remove(tvbChannel.getUniqueId());
            writeLog("Remove vom channelToTVHMap:" + tvbChannelName + " " + tvbChannel.getUniqueId());
            tvhToChannelMap.remove(tvhChannel.getKey());
        } else {
            if (tvbChannel == null) {
                this.tvhBrowser.showError("Channel unmapping failed: TVB channel not found. " + tvbChannelName);
            }
            if (tvhChannel == null) {
                this.tvhBrowser.showError("Channel unmapping failed: TVH channel not found. " + tvhChannelName);
            }
        }
    }

    private TVHeadendChannel getTVHChannelbyName(String tvhChannelName) {
        for (TVHeadendChannel channel : tvhChannelMap.values()) {
            if (channel.getVal().equals(tvhChannelName)) {
                return channel;
            }
        }
        return null;
    }

    private devplugin.Channel getTVBChannelbyName(String tvbChannelName) {
        for (devplugin.Channel channel : tvbChannelMap.values()) {
            if (channel.getName().equals(tvbChannelName)) {
                return channel;
            }
        }
        return null;
    }

    /**
     * Retrieves the TVBrowser channel list.
     *
     * @return    A collection of devplugin.Channel objects.
     */
    public Collection<devplugin.Channel> getTVBChannelList() {
        if (tvbChannelMap.size() == 0) {
            this.getTVBrowserChannels();
        }

        return tvbChannelMap.values();
    }

    /**
     * Retrieves the TVHeadend channel mapped to the specified TVBrowser channel ID.
     *
     * @param tvbChannelId   The TVBrowser channel ID.
     * @return               The TVHeadendChannel object.
     */
    public TVHeadendChannel getMappedTvhChannel(String tvbChannelId) {
        writeLog("Get mapped TVH channel for " + tvbChannelId);
        return tvhChannelMap.get(channelToTVHMap.get(tvbChannelId));
    }

    /**
     * Retrieves the TVBrowser channel mapped to the specified TVHeadend channel.
     *
     * @param tvhChannel   The TVHeadend channel.
     * @return             The devplugin.Channel object.
     */
    public devplugin.Channel getMappedTvbChannel(String tvhChannel) {
        writeLog("Get mapped TVB channel for " + tvhChannel);
        return tvbChannelMap.get(tvhToChannelMap.get(tvhChannel));
    }

    /**
     * Writes the channel mapping data to an ObjectOutputStream.
     *
     * @param out   The ObjectOutputStream to write to.
     * @throws IOException   If an I/O error occurs while writing the data.
     */
    public void writeData(ObjectOutputStream out) throws IOException {
        out.writeInt(channelToTVHMap.size());
        for (Map.Entry<String, String> entry : channelToTVHMap.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }
    }

    /**
     * Reads the channel mapping data from an ObjectInputStream.
     *
     * @param in   The ObjectInputStream to read from.
     * @throws IOException   If an I/O error occurs while reading the data.
     */
    public void readData(ObjectInputStream in) throws IOException {
        if (tvbChannelMap.size() == 0) {
            this.getTVBrowserChannels();
        }

        if (tvhChannelMap.size() == 0) {
            this.getTVHeadendChannels();
        }

        String tvbChannelId;
        String tvhChannelKey;
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            tvbChannelId = in.readUTF();
            tvhChannelKey = in.readUTF();
            mapChannels(tvbChannelId, tvhChannelKey);
        }
    }

    public void writeLog(String message) {
        tvhBrowser.writeLog(message);
    }
}
