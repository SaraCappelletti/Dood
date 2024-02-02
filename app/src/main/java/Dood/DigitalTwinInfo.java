package Dood;

import java.util.List;

public class DigitalTwinInfo {
    private final String dtImage;
    private final String dtId;
    private final String ownerId;
    private final List<String> tagsList;
    private final String dtPort;

    public DigitalTwinInfo(String dtImage, String dtId, String ownerId, List<String> tagsList, String dtPort) {
        this.dtImage = dtImage;
        this.dtId = dtId;
        this.ownerId = ownerId;
        this.tagsList = tagsList;
        this.dtPort = dtPort;
    }

    public String getDtImage() {
        return dtImage;
    }

    public String getDtId() {
        return dtId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<String> getTagsList() {
        return tagsList;
    }

    public String getDtPort() {
        return dtPort;
    }
}
