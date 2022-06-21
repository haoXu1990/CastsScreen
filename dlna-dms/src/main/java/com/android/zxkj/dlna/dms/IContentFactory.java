package com.android.zxkj.dlna.dms;


import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import com.android.zxkj.dlna.core.ContentType;

import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.Movie;
import org.fourthline.cling.support.model.item.MusicTrack;

import java.util.ArrayList;
import java.util.List;

public interface IContentFactory {
    BrowseResult getBrowseResult(String objectID) throws ContentDirectoryException;

    // Implement
    class ContentFactoryImpl implements IContentFactory {
        private final Context mContext;
        private final String mBaseUrl;

        public ContentFactoryImpl(@NonNull Context context,@NonNull String baseUrl) {
            this.mBaseUrl = baseUrl;
            this.mContext = context;
        }

        @Override
        public BrowseResult getBrowseResult(String objectID) throws ContentDirectoryException {
            Container container = getContent(objectID);
            DIDLContent didlContent = new DIDLContent();
            for (Container c: container.getContainers()) didlContent.addContainer(c);
            int count = container.getChildCount();
            String result;
            try {
                result = new DIDLParser().generate(didlContent, true);
            } catch (Exception e) {
                throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, e.toString());
            }
            return  new BrowseResult(result,count,count);
        }

        private Container getContent(@NonNull String containerId) {
            if (ContentType.ALL.match(containerId)) {
                Container result = new Container();

                // set Audio source
                Container audioContainer = getAudioContents();
                audioContainer.setParentID(ContentType.ALL.id);
                audioContainer.setId(ContentType.AUDIO.id);
                audioContainer.setClazz(ContentType.AUDIO.clazz);
                audioContainer.setTitle(ContentType.AUDIO.name());
                result.addContainer(audioContainer);

                // set Image Source
                Container imageContainer = getImageContents();
                imageContainer.setParentID(ContentType.ALL.id);
                imageContainer.setId(ContentType.IMAGE.id);
                imageContainer.setClazz(ContentType.IMAGE.clazz);
                imageContainer.setTitle(ContentType.IMAGE.name());
                result.addContainer(imageContainer);

                // set Video Source
                Container videoContainer = getVideoContents();
                videoContainer.setParentID(ContentType.ALL.id);
                videoContainer.setId(ContentType.VIDEO.id);
                videoContainer.setClazz(ContentType.VIDEO.clazz);
                videoContainer.setTitle(ContentType.VIDEO.name());
                result.addContainer(videoContainer);
                // 3个子节点
                result.setChildCount(3);
                return result;
            } else if (ContentType.IMAGE.match(containerId)) {
                return getImageContents();
            } else if (ContentType.AUDIO.match(containerId)) {
                return getAudioContents();
            } else if (ContentType.VIDEO.match(containerId)) {
                return getVideoContents();
            }
            throw new IllegalArgumentException("Can not parse ContainerId: " + containerId);
        }

        private Container getVideoContents() {
            IMediaContentDao contentDao = new IMediaContentDao.MediaContentDao(mBaseUrl);
            List<Item> items = contentDao.getVideoItems(mContext);
            return getContents(items);
        }

        private Container getAudioContents() {
            IMediaContentDao contentDao = new IMediaContentDao.MediaContentDao(mBaseUrl);
            List<Item> items = contentDao.getAudioItems(mContext);
            return getContents(items);
        }

        private Container getImageContents() {
            IMediaContentDao contentDao = new IMediaContentDao.MediaContentDao(mBaseUrl);
            List<Item> items = contentDao.getImageItems(mContext);
            return getContents(items);
        }

        private Container getContents(@NonNull List<Item> items) {
            Container result = new Container();
            for (Item item: items) result.addItem(item);
            result.setChildCount(items.size());
            return result;
        }
    }

    interface IMediaContentDao {
        @NonNull
        List<Item> getImageItems(@NonNull Context context);

        @NonNull
        List<Item> getAudioItems(@NonNull Context context);

        @NonNull
        List<Item> getVideoItems(@NonNull Context context);


        // Implement
        final class MediaContentDao implements IMediaContentDao {

            private static final String[] CONTENT_IMAGE_COLUMNS = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.SIZE};

            private static final String[] CONTENT_AUDIO_COLUMNS = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.ARTIST, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.SIZE,MediaStore.MediaColumns.ALBUM
            };

            private static final String[] CONTENT_VIDEO_COLUMNS = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.TITLE, MediaStore.MediaColumns.ARTIST,
                    MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.RESOLUTION};

            private final String mBaseUrl;

            public MediaContentDao(String baseUrl) {
                this.mBaseUrl = baseUrl;
            }
            @NonNull
            @Override
            public List<Item> getImageItems(@NonNull Context context) {
                List<Item> items = new ArrayList<>();
                try (Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, CONTENT_IMAGE_COLUMNS,null,null)) {
                    if (cursor == null) return items;

                    cursor.moveToFirst();
                    while (cursor.moveToNext()) {
                        String id = String.valueOf(cursor.getInt(cursor.getColumnIndex(CONTENT_IMAGE_COLUMNS[0])));
                        String title = cursor.getString(cursor.getColumnIndex(CONTENT_IMAGE_COLUMNS[1]));
                        String data = cursor.getString(cursor.getColumnIndex(CONTENT_IMAGE_COLUMNS[2]));
                        String mimeType = cursor.getString(cursor.getColumnIndex(CONTENT_IMAGE_COLUMNS[3]));
                        long size = cursor.getLong(cursor.getColumnIndex(CONTENT_IMAGE_COLUMNS[4]));
                        String url = mBaseUrl + data;
                        Res res = new Res(mimeType, size, "", null ,url);
                        ImageItem imageItem = new ImageItem(id, ContentType.IMAGE.id, title, "", res);
                        items.add(imageItem);
                    }
                }
                return items;
            }

            @NonNull
            @Override
            public List<Item> getAudioItems(@NonNull Context context) {
                List<Item> items = new ArrayList<>();
                try (Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, CONTENT_AUDIO_COLUMNS,null, null)){
                    if (cursor == null) return items;
                    while (cursor.moveToNext()) {
                        String id = String.valueOf(cursor.getInt(cursor.getColumnIndex(CONTENT_AUDIO_COLUMNS[0])));
                        String title = cursor.getString(cursor.getColumnIndex(CONTENT_AUDIO_COLUMNS[1]));
                        String creator = cursor.getString(cursor.getColumnIndex(CONTENT_AUDIO_COLUMNS[2]));
                        String data = cursor.getString(cursor.getColumnIndex(CONTENT_AUDIO_COLUMNS[3]));
                        String mimeType = cursor.getString(cursor.getColumnIndex(CONTENT_AUDIO_COLUMNS[4]));
                        long size = cursor.getLong(cursor.getColumnIndex(CONTENT_AUDIO_COLUMNS[5]));
                        String album = cursor.getString(cursor.getColumnIndex(CONTENT_AUDIO_COLUMNS[6]));
                        String url = mBaseUrl + data;
                        Res res = new Res(mimeType, size,"", null, url);
                        MusicTrack musicTrack = new MusicTrack(id, ContentType.AUDIO.id, title, creator, album, new PersonWithRole(creator), res);
                        items.add(musicTrack);
                    }
                }
                return items;
            }

            @NonNull
            @Override
            public List<Item> getVideoItems(@NonNull Context context) {
                List<Item> items = new ArrayList<>();
                try (Cursor cur = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, CONTENT_VIDEO_COLUMNS, null, null, null)) {
                    if (cur == null) return items;
                    cur.moveToFirst();
                    while (cur.moveToNext()) {
                        String id = String.valueOf(cur.getInt(cur.getColumnIndex(CONTENT_VIDEO_COLUMNS[0])));
                        String title = cur.getString(cur.getColumnIndex(CONTENT_VIDEO_COLUMNS[1]));
                        String creator = cur.getString(cur.getColumnIndex(CONTENT_VIDEO_COLUMNS[2]));
                        String data = cur.getString(cur.getColumnIndexOrThrow(CONTENT_VIDEO_COLUMNS[3]));
                        String mimeType = cur.getString(cur.getColumnIndex(CONTENT_VIDEO_COLUMNS[4]));
                        long size = cur.getLong(cur.getColumnIndex(CONTENT_VIDEO_COLUMNS[5]));
                        String resolution = cur.getString(cur.getColumnIndex(CONTENT_VIDEO_COLUMNS[6]));
                        String url = mBaseUrl + data;
                        Res res = new Res(mimeType, size, "", null, url);
                        res.setResolution(resolution);
                        Movie movie = new Movie(id, ContentType.VIDEO.id, title, creator, res);
                        items.add(movie);
                    }
                }
                return items;
            }
        }

    }
}
