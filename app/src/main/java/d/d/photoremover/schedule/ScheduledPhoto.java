package d.d.photoremover.schedule;

import java.io.Serializable;

public class ScheduledPhoto implements Serializable {
    private int id = -1;
    private String uri;
    private String filePath;
    private long expiryDate;

    public ScheduledPhoto(String uri, String filePath, long expiryDate) {
        this.uri = uri;
        this.filePath = filePath;
        this.expiryDate = expiryDate;
    }

    public ScheduledPhoto(String uri, String filePath) {
        this(uri, filePath, -1);
    }

    public String getUri() {
        return uri;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public boolean hasId(){
        return this.id != -1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setExpiryDurationFromNow(long duration){
        this.setExpiryDate(System.currentTimeMillis() + duration);
    }
}
