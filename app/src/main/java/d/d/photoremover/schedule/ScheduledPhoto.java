package d.d.photoremover.schedule;

import java.io.Serializable;

import d.d.photoremover.R;

public class ScheduledPhoto implements Serializable {
    private int id = -1;
    private String uri;
    private String filePath;
    private long expiryDate;
    private State state;

    public enum State{
        SCHEDULED(R.string.state_scheduled, false),
        ERROR_FILE_NOT_FOUND(R.string.state_error_file_not_found, true),
        ERROR_FILE_NO_ACCESS(R.string.state_error_file_not_writable, true),
        ERROR_FILE_DELETE_FAILED(R.string.state_error_deletion_failed, true);

        private int stateDescriptionResource;
        private boolean isError;

        State(int stateDescriptionResource, boolean isError) {
            this.stateDescriptionResource = stateDescriptionResource;
            this.isError = isError;
        }

        public String toString(){
            return this.name();
        }

        public int getStateDescriptionResource(){
            return this.stateDescriptionResource;
        }

        public static State fromString(String stateString){
            for(State s : values()){
                if(s.name().equals(stateString)) return s;
            }
            return null;
        }

        public boolean isError(){
            return isError;
        }
    }

    public ScheduledPhoto(String uri, String filePath, State state, long expiryDate) {
        this.uri = uri;
        this.filePath = filePath;
        this.expiryDate = expiryDate;
        this.state = state;
    }

    public ScheduledPhoto(String uri, String filePath, State state) {
        this(uri, filePath, state, -1);
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

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
