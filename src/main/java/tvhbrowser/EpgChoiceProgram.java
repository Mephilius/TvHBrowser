package tvhbrowser;

import devplugin.Channel;
import devplugin.Date;
import devplugin.ProgramFieldType;
import tvdataservice.MutableProgram;

public class EpgChoiceProgram extends MutableProgram {
    private final EpgProgram epgProgram;

    public EpgChoiceProgram(Channel channel, EpgProgram epgProgram) {
        super(channel, epgProgram.getDate(), epgProgram.getStartDateTime().getHour(),
                epgProgram.getStartDateTime().getMinute(), false);
        this.epgProgram = epgProgram;

        setTitle(epgProgram.getTitle() == null ? "" : epgProgram.getTitle());
        String subtitle = epgProgram.getSubtitle();
        if (subtitle != null && !subtitle.isBlank()) {
            setTextField(ProgramFieldType.EPISODE_TYPE, subtitle);
        }
        String description = epgProgram.getDescription();
        if (description != null && !description.isBlank()) {
            setDescription(description.trim());
            setShortInfo(generateShortInfoFromDescription(description.trim()));
        }
        if (epgProgram.getStop() > epgProgram.getStart()) {
            setLength((int) ((epgProgram.getStop() - epgProgram.getStart()) / 60));
        }
    }

    public EpgProgram getEpgProgram() {
        return epgProgram;
    }
}
