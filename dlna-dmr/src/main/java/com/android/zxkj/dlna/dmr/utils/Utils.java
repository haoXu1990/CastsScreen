package com.android.zxkj.dlna.dmr.utils;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;

public class Utils {
    public static final String VIDEO_PREFIX = "video";
    public static final String AUDIO_PREFIX = "audio";
    public static final String IMAGE_PREFIX = "image";

    public enum MEDIA_TYPE {
        AUDIO, IMAGE, VIDEO
    }


    private Utils.MEDIA_TYPE getMediaType(String metadata) {
        String itemClass = "";
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(metadata));
            int eventType = parser.getEventType();

            loop: while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:

                        break;
                    case XmlPullParser.START_TAG:
                        String tag = parser.getName();
                        if (tag.equalsIgnoreCase("class")) {
                            itemClass = parser.nextText().toLowerCase();
                            break loop;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        if (itemClass.contains(Utils.AUDIO_PREFIX))
            return Utils.MEDIA_TYPE.AUDIO;
        else if (itemClass.contains(Utils.VIDEO_PREFIX))
            return Utils.MEDIA_TYPE.VIDEO;
        else
            return Utils.MEDIA_TYPE.IMAGE;
    }

}
