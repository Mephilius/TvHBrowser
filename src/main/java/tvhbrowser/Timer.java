package tvhbrowser;

import devplugin.Date;
import devplugin.Program;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.security.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Timer extends TimerEpgInterface {
    private String uuid;
    private boolean enabled;
    private long create;
    private int watched;
    protected Map<String, String> title;
    protected Map<String, String> subtitle; // Untertitel hinzugefügt
    protected Map<String, String> description;
    protected Map<String, String> extractedtitel;
    
    public Map<String, String> getExtractedtitel() {
        return extractedtitel;
    }

    public void setExtractedtitel(Map<String, String> extractedtitel) {
        this.extractedtitel = extractedtitel;
    }

    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

    public Map<String, String> getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(Map<String, String> subtitle) {
        this.subtitle = subtitle;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(Map<String, String> description) {
        this.description = description;
    }

    private int start_extra;
    private long start_real;

    private int stop_extra;
    private long stop_real;
    private int duration;
    private String channel;
    private String channelname;
    private String image;
    private String fanart_image;

    private String disp_title;

    private String disp_subtitle;
    private String disp_summary;
    private String disp_description;
    private String disp_extratext;
    private int pri;
    private int retention;
    private int removal;
    private int playposition;
    private int playcount;
    private String config_name;
    private String creator;
    private String filename;
    private int errorcode;
    private int errors;
    private int data_errors;
    private int dvb_eid;
    private boolean noresched;
    private boolean norerecord;
    private int fileremoved;
    private String autorec;
    private String autorec_caption;
    private String timerec;
    private String timerec_caption;
    private String parent;
    private String child;
    private int content_type;
    private int copyright_year;
    private int broadcast;
    private String episode_disp;
    private String url;
    private long filesize;
    private String status;
    private String sched_status;
    private int duplicate;
    private int first_aired;
    private List<String> category;
    private Map<String, String> credits;
    private List<String> keyword;
    private List<Integer> genre;
    private int age_rating;
    private String rating_label_uuid;
    private String rating_icon;
    private String rating_label;


    // Getter and Setter methods for uuid
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    // Getter and Setter methods for enabled
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Getter and Setter methods for create
    public long getCreate() {
        return create;
    }

    public void setCreate(long create) {
        this.create = create;
    }

    // Getter and Setter methods for watched
    public int getWatched() {
        return watched;
    }

    public void setWatched(int watched) {
        this.watched = watched;
    }

    // Getter and Setter methods for start
    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    // Getter and Setter methods for start_extra
    public int getStart_extra() {
        return start_extra;
    }

    public void setStart_extra(int start_extra) {
        this.start_extra = start_extra;
    }

    // Getter and Setter methods for start_real
    public long getStart_real() {
        return start_real;
    }

    public void setStart_real(long start_real) {
        this.start_real = start_real;
    }



    // Getter and Setter methods for stop_extra
    public int getStop_extra() {
        return stop_extra;
    }

    public void setStop_extra(int stop_extra) {
        this.stop_extra = stop_extra;
    }

    // Getter and Setter methods for stop_real
    public long getStop_real() {
        return stop_real;
    }

    public void setStop_real(long stop_real) {
        this.stop_real = stop_real;
    }

    // Getter and Setter methods for duration
    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    // Getter and Setter methods for channel
    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    // Getter and Setter methods for channelname
    public String getChannelname() {
        return channelname;
    }

    public void setChannelname(String channelname) {
        this.channelname = channelname;
    }

    // Getter and Setter methods for image
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // Getter and Setter methods for fanart_image
    public String getFanart_image() {
        return fanart_image;
    }

    public void setFanart_image(String fanart_image) {
        this.fanart_image = fanart_image;
    }

    public LocalDateTime getStartRealDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(this.start_real), ZoneOffset.systemDefault());
    }

    

    // Getter and Setter methods for disp_title
    public String getDisp_title() {
        return disp_title;
    }

    public void setDisp_title(String disp_title) {
        this.disp_title = disp_title;
    }

 

    // Getter and Setter methods for disp_subtitle
    public String getDisp_subtitle() {
        return disp_subtitle;
    }

    public void setDisp_subtitle(String disp_subtitle) {
        this.disp_subtitle = disp_subtitle;
    }

    // Getter and Setter methods for disp_summary
    public String getDisp_summary() {
        return disp_summary;
    }

    public void setDisp_summary(String disp_summary) {
        this.disp_summary = disp_summary;
    }



    // Getter and Setter methods for disp_description
    public String getDisp_description() {
        return disp_description;
    }

    public void setDisp_description(String disp_description) {
        this.disp_description = disp_description;
    }

    // Getter and Setter methods for disp_extratext
    public String getDisp_extratext() {
        return disp_extratext;
    }

    public void setDisp_extratext(String disp_extratext) {
        this.disp_extratext = disp_extratext;
    }

    // Getter and Setter methods for pri
    public int getPri() {
        return pri;
    }

    public void setPri(int pri) {
        this.pri = pri;
    }

    // Getter and Setter methods for retention
    public int getRetention() {
        return retention;
    }

    public void setRetention(int retention) {
        this.retention = retention;
    }

    // Getter and Setter methods for removal
    public int getRemoval() {
        return removal;
    }

    public void setRemoval(int removal) {
        this.removal = removal;
    }

    // Getter and Setter methods for playposition
    public int getPlayposition() {
        return playposition;
    }

    public void setPlayposition(int playposition) {
        this.playposition = playposition;
    }

    // Getter and Setter methods for playcount
    public int getPlaycount() {
        return playcount;
    }

    public void setPlaycount(int playcount) {
        this.playcount = playcount;
    }

    // Getter and Setter methods for config_name
    public String getConfig_name() {
        return config_name;
    }

    public void setConfig_name(String config_name) {
        this.config_name = config_name;
    }

    // Getter and Setter methods for creator
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    // Getter and Setter methods for filename
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    // Getter and Setter methods for errorcode
    public int getErrorcode() {
        return errorcode;
    }

    public void setErrorcode(int errorcode) {
        this.errorcode = errorcode;
    }

    // Getter and Setter methods for errors
    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    // Getter and Setter methods for data_errors
    public int getData_errors() {
        return data_errors;
    }

    public void setData_errors(int data_errors) {
        this.data_errors = data_errors;
    }

    // Getter and Setter methods for dvb_eid
    public int getDvb_eid() {
        return dvb_eid;
    }

    public void setDvb_eid(int dvb_eid) {
        this.dvb_eid = dvb_eid;
    }

    // Getter and Setter methods for noresched
    public boolean isNoresched() {
        return noresched;
    }

    public void setNoresched(boolean noresched) {
        this.noresched = noresched;
    }

    // Getter and Setter methods for norerecord
    public boolean isNorerecord() {
        return norerecord;
    }

    public void setNorerecord(boolean norerecord) {
        this.norerecord = norerecord;
    }

    // Getter and Setter methods for fileremoved
    public int getFileremoved() {
        return fileremoved;
    }

    public void setFileremoved(int fileremoved) {
        this.fileremoved = fileremoved;
    }

    // Getter and Setter methods for autorec
    public String getAutorec() {
        return autorec;
    }

    public void setAutorec(String autorec) {
        this.autorec = autorec;
    }

    // Getter and Setter methods for autorec_caption
    public String getAutorec_caption() {
        return autorec_caption;
    }

    public void setAutorec_caption(String autorec_caption) {
        this.autorec_caption = autorec_caption;
    }

    // Getter and Setter methods for timerec
    public String getTimerec() {
        return timerec;
    }

    public void setTimerec(String timerec) {
        this.timerec = timerec;
    }

    // Getter and Setter methods for timerec_caption
    public String getTimerec_caption() {
        return timerec_caption;
    }

    public void setTimerec_caption(String timerec_caption) {
        this.timerec_caption = timerec_caption;
    }

    // Getter and Setter methods for parent
    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    // Getter and Setter methods for child
    public String getChild() {
        return child;
    }

    public void setChild(String child) {
        this.child = child;
    }

    // Getter and Setter methods for content_type
    public int getContent_type() {
        return content_type;
    }

    public void setContent_type(int content_type) {
        this.content_type = content_type;
    }

    // Getter and Setter methods for copyright_year
    public int getCopyright_year() {
        return copyright_year;
    }

    public void setCopyright_year(int copyright_year) {
        this.copyright_year = copyright_year;
    }

    // Getter and Setter methods for broadcast
    public int getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(int broadcast) {
        this.broadcast = broadcast;
    }

    // Getter and Setter methods for episode_disp
    public String getEpisode_disp() {
        return episode_disp;
    }

    public void setEpisode_disp(String episode_disp) {
        this.episode_disp = episode_disp;
    }

    // Getter and Setter methods for url
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // Getter and Setter methods for filesize
    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    // Getter and Setter methods for status
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Getter and Setter methods for sched_status
    public String getSched_status() {
        return sched_status;
    }

    public void setSched_status(String sched_status) {
        this.sched_status = sched_status;
    }

    // Getter and Setter methods for duplicate
    public int getDuplicate() {
        return duplicate;
    }

    public void setDuplicate(int duplicate) {
        this.duplicate = duplicate;
    }

    // Getter and Setter methods for first_aired
    public int getFirst_aired() {
        return first_aired;
    }

    public void setFirst_aired(int first_aired) {
        this.first_aired = first_aired;
    }

    // Getter and Setter methods for category
    public List<String> getCategory() {
        return category;
    }

    public void setCategory(List<String> category) {
        this.category = category;
    }

    // Getter and Setter methods for credits
    public Map<String, String> getCredits() {
        return credits;
    }

    public void setCredits(Map<String, String> credits) {
        this.credits = credits;
    }

    // Getter and Setter methods for keyword
    public List<String> getKeyword() {
        return keyword;
    }

    public void setKeyword(List<String> keyword) {
        this.keyword = keyword;
    }

    // Getter and Setter methods for genre
    public List<Integer> getGenre() {
        return genre;
    }

    public void setGenre(List<Integer> genre) {
        this.genre = genre;
    }

    // Getter and Setter methods for age_rating
    public int getAge_rating() {
        return age_rating;
    }

    public void setAge_rating(int age_rating) {
        this.age_rating = age_rating;
    }

    // Getter and Setter methods for rating_label_uuid
    public String getRating_label_uuid() {
        return rating_label_uuid;
    }

    public void setRating_label_uuid(String rating_label_uuid) {
        this.rating_label_uuid = rating_label_uuid;
    }

    // Getter and Setter methods for rating_icon
    public String getRating_icon() {
        return rating_icon;
    }

    public void setRating_icon(String rating_icon) {
        this.rating_icon = rating_icon;
    }

    // Getter and Setter methods for rating_label
    public String getRating_label() {
        return rating_label;
    }

    public void setRating_label(String rating_label) {
        this.rating_label = rating_label;
    }

    private Calendar convertToCalendar(long start) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start * 1000);
        return cal;
    }

    public Date getDate() {
        return new Date(convertToCalendar(this.start));
    }

    @Override
    public String getTitle(String Lang) {
        return this.title.get(Lang);
    }

    @Override
    public Map<String, String> getTitles() {
        return this.title;
    }

    @Override
    public String getSubtitle(String Lang) {
        return this.subtitle.get(Lang);
    }

    @Override
    public Map<String, String> getSubtitles() {
       return this.subtitle;
    }

    @Override
    public void setExtractedTitle(String Lang, String Title) {
        this.extractedtitel.put(Lang, Title);
    }

    @Override
    public String getExtractedTitle(String Lang) {
       return this.extractedtitel.get(Lang);
    }

    @Override
    public Map<String, String> getExtractedTitles() {
        return this.extractedtitel;
    }
}
