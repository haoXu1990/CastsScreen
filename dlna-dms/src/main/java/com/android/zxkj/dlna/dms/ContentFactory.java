package com.android.zxkj.dlna.dms;

import android.content.Context;

import androidx.annotation.NonNull;

import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.BrowseResult;

public class ContentFactory {

    private static class Holder {
        private static final ContentFactory sInstance = new ContentFactory();
    }

    public static ContentFactory getInstance() {
        return Holder.sInstance;
    }

    private ContentFactory() {}

    private IContentFactory mContentFactory;

    public void setServerUrl(Context context, String url) {
        mContentFactory = new IContentFactory.ContentFactoryImpl(context, url);
    }

    @NonNull
    public BrowseResult getContent(String objecID) throws ContentDirectoryException {
        if (mContentFactory == null) return null;
        return mContentFactory.getBrowseResult(objecID);
    }

}
