package experiment.playback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import utilities.FileUtilities;
import utilities.MyUtilities;
import keyboard.IKeyboard;

import com.leapmotion.leap.Vector;

import enums.DataType;
import enums.Direction;
import enums.FileExt;
import enums.FileName;
import enums.FilePath;
import enums.Key;

public class PlaybackManager {
    private static final String TUTORIAL = FileName.TUTORIAL.getName();
    private static final long NANO_SECOND = 1000000000;
    private final boolean isTutorial;
	private ArrayList<PlaybackObserver> observers = new ArrayList<PlaybackObserver>();
	private boolean isRepeating = false;
	private ArrayList<PlaybackData> playbackData = new ArrayList<PlaybackData>();
	private int playbackIndex = 0;
	private long startTime = 0;
	private long finishTime = 0;
	private long elapsedTime = 0;
	private long previousTime;
	
	// TODO:
	// 1) Need to open and read file based on subject ID or 'tutorial'
	// 2) Scan through contents and determine start time
	// 3) Use a timer to know when to throw events
	// 4) Use observer pattern to update keyboard contents
	// 5) If repeat is enabled, loop playback until shutdown.
	// 6) If repeat is disabled, play once
	
	public PlaybackManager(boolean repeat, String subjectID, IKeyboard keyboard) {
	    isTutorial = TUTORIAL.equals(subjectID);
		isRepeating = repeat;
		String filePath = FilePath.DATA.getPath() + subjectID + "/";;
		String wildcardFileName = subjectID + "_" + keyboard.getFileName() + FileUtilities.WILDCARD + FileExt.DAT.getExt();
		try {
            ArrayList<String> fileData = MyUtilities.FILE_IO_UTILITIES.readListFromWildcardFile(filePath, wildcardFileName);
            parseFileData(fileData);
        } catch (IOException e) {
            System.out.println("Failed to open up data file for playback.");
            e.printStackTrace();
        }
		previousTime = System.nanoTime();
		elapsedTime = startTime - NANO_SECOND;
	}

    public void update() {
        long now = System.nanoTime();
        elapsedTime += now - previousTime;
        previousTime = now;
        
		// Fire off any appropriate events based on their time.
        if(playbackIndex < playbackData.size()) {            
            // Check if the currentPlayback is ready to fire and then fire all of it's events.
            PlaybackData currentPlayback = playbackData.get(playbackIndex);
            if(elapsedTime >= currentPlayback.getTime()) {
                while(currentPlayback.hasNext()) {
                    Entry<DataType, Object> currentData = currentPlayback.next();
                    
                    switch(currentData.getKey()) {
                        case KEY_PRESSED:
                            notifyListenersPressedEvent((Key) currentData.getValue());
                            break;
                        case POINT_POSITION:
                            notifyListenersPointPositionEvent((Vector) currentData.getValue());
                            break;
                        case TOOL_DIRECTION:
                            notifyListenersToolDirectionEvent((Vector) currentData.getValue());
                            break;
                        case DIRECTION_PRESSED:
                            notifyListenersControllerDirectionEvent((Direction) currentData.getValue());
                            break;
                        default: break;
                    }
                }
                playbackIndex++;
            }
        }
        
		if(isRepeating && playbackIndex == playbackData.size() && elapsedTime > finishTime) {
		    // Reset playback and give a 1 second delay before next start.
		    playbackIndex = 0;
		    elapsedTime = startTime - NANO_SECOND;
		    previousTime = System.nanoTime();
		    notifyListenersResetEvent();
		}
	}
	
	public void setRepeat(boolean repeat) {
		isRepeating = repeat;
	}
	
    public void registerObserver(PlaybackObserver observer) {
        if(observers.contains(observer)) {
            return;
        }
        observers.add(observer);
    }
    
    public void removeObserver(PlaybackObserver observer) {
        observers.remove(observer);
    }
    
    protected void notifyListenersPressedEvent(Key key) {
        for(PlaybackObserver observer : observers) {
            observer.pressedEventObserved(key);
        }
    }
    
    protected void notifyListenersResetEvent() {
        for(PlaybackObserver observer : observers) {
            if(observer instanceof ControllerPlaybackObserver) {
                ((ControllerPlaybackObserver) observer).resetEventObserved();
            }
        }
    }
    
    protected void notifyListenersControllerDirectionEvent(Direction direction) {
        for(PlaybackObserver observer : observers) {
            if(observer instanceof ControllerPlaybackObserver) {
                ((ControllerPlaybackObserver) observer).directionEventObserved(direction);
            }
        }
    }
    
    protected void notifyListenersToolDirectionEvent(Vector toolDirection) {
        for(PlaybackObserver observer : observers) {
            if(observer instanceof LeapPlaybackObserver) {
                ((LeapPlaybackObserver) observer).directionEventObserved(toolDirection);
            }
        }
    }
    
    protected void notifyListenersPointPositionEvent(Vector pointPosition) {
        for(PlaybackObserver observer : observers) {
            if(observer instanceof LeapPlaybackObserver) {
                ((LeapPlaybackObserver) observer).positionEventObserved(pointPosition);
            } else if(observer instanceof TabletPlaybackObserver) {
                ((TabletPlaybackObserver) observer).touchEventObserved(pointPosition);
            }
        }
    }
    
    private void parseFileData(ArrayList<String> fileData) {
        for(String line: fileData) {            
            // Remove whitespace from line and then delimit the string based on semicolon.
            line = line.replaceAll("\\s+|\\(|\\)", "");
            String[] events = line.split(";");
            
            // Want to get the event time and a list of data that was recorded at that time.
            long eventTime = 0;
            LinkedHashMap<DataType, Object> entries = new LinkedHashMap<DataType, Object>();
            
            for(int i = 0; i < events.length; i++) {
                // Delimit by colon to break into data type, value pair.
                String[] eventInfo = events[i].split(":");
                
                // Want to get the data type and data value unless it's a time value.
                Object dataValue = null;
                DataType dataType = DataType.getByName(eventInfo[0]);
                
                switch(dataType) {
                    case TIME_EXPERIMENT_START:
                        startTime = Long.valueOf(eventInfo[1]);
                        break;
                    case TIME_EXPERIMENT_END:
                        finishTime = Long.valueOf(eventInfo[1]);
                        break;
                    case TIME_WORD_START:
                        if(!isTutorial) {
                            eventTime = Long.valueOf(eventInfo[1]);
                        }
                        break;
                    case TIME_WORD_END:
                        if(!isTutorial) {
                            eventTime = Long.valueOf(eventInfo[1]);
                        }
                        break;
                    case TIME_PRESSED:
                        eventTime = Long.valueOf(eventInfo[1]);
                        break;
                    case TIME_SPECIAL:
                        eventTime = Long.valueOf(eventInfo[1]);
                        break;
                    case WORD_VALUE:
                        if(!isTutorial) {
                            dataValue = eventInfo[1];
                        }
                        break;
                    case KEY_PRESSED:
                        dataValue = Key.getByName(eventInfo[1]);
                        break;
                    /*case KEY_PRESSED_UPPER:
                        dataValue = Boolean.valueOf(eventInfo[1]);
                        break;
                    /*case KEY_EXPECTED:
                        if(!isTutorial) {
                            dataValue = Key.getByName(eventInfo[1]);
                        }
                        break;
                    case KEY_EXPECTED_UPPER:
                        if(!isTutorial) {
                            dataValue = Boolean.valueOf(eventInfo[1]);
                        }
                        break;*/
                    case POINT_POSITION:
                        String[] vectorPos = eventInfo[1].split(",");
                        float xPos = Float.valueOf(vectorPos[0]);
                        float yPos = Float.valueOf(vectorPos[1]);
                        float zPos = Float.valueOf(vectorPos[2]);
                        dataValue = new Vector(xPos, yPos, zPos);
                        break;
                    case TOOL_DIRECTION:
                        String[] vectorDir = eventInfo[1].split(",");
                        float xDir = Float.valueOf(vectorDir[0]);
                        float yDir = Float.valueOf(vectorDir[1]);
                        float zDir = Float.valueOf(vectorDir[2]);
                        dataValue = new Vector(xDir, yDir, zDir);
                        break;
                    case DIRECTION_PRESSED:
                        dataValue = Direction.getByName(eventInfo[1]);
                        break;
                    default: break;
                }
                if(dataValue != null) {
                    entries.put(dataType, dataValue);
                }
            }
            if(!entries.isEmpty() && eventTime > 0) {
                playbackData.add(new PlaybackData(eventTime, new ArrayList<Entry<DataType, Object>>(entries.entrySet())));
            }
        }
    }
    
    private class PlaybackData {
        private long eventTime;
        private ArrayList<Entry<DataType, Object>> eventData;
        private int eventIndex = 0;
        
        PlaybackData(long eventTime, ArrayList<Entry<DataType, Object>> eventData) {
            this.eventTime = eventTime;
            this.eventData = eventData;
        }
        
        public long getTime() {
            return eventTime;
        }
        
        public boolean hasNext() {
            if(eventIndex < eventData.size()) {
                return true;
            }
            eventIndex = 0;
            return false;
        }
        
        public Entry<DataType, Object> next() {
            if(hasNext()) {
                return eventData.get(eventIndex++);
            }
            return null;
        }
    }
}
