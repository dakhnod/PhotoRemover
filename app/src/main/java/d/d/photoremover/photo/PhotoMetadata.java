package d.d.photoremover.photo;

import java.io.Serializable;

public class PhotoMetadata implements Serializable {
    private int orientation;

    public PhotoMetadata(int orientation) {
        this.orientation = orientation;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }
}
