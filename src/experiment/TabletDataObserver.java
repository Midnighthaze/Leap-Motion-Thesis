package experiment;

import com.leapmotion.leap.Vector;

public interface TabletDataObserver {
    public void tabletDataEventObserved(Vector touchPoint);
}