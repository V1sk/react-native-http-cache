package cn.reactnative.httpcache;

import android.content.Intent;

import com.facebook.cache.disk.DiskStorageCache;
import com.facebook.cache.disk.FileCache;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.network.OkHttpClientProvider;
import okhttp3.Cache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.io.File;
/**
 * Created by tdzl2_000 on 2015-10-10.
 */
public class HttpCacheModule extends ReactContextBaseJavaModule {
    public HttpCacheModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return "RCTHttpCache";
    }

    public String getDiskCacheDir() {
        String cachePath = null;
        if (android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment.getExternalStorageState())
                || !android.os.Environment.isExternalStorageRemovable()) {
            cachePath = getReactApplicationContext().getExternalCacheDir().getPath();
        } else {
            cachePath = getReactApplicationContext().getCacheDir().getPath();
        }
        return cachePath;
    }


    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
          if (dir.isFile()) {
                return dir.delete();
            }else{
                return true;
            }
        //return dir != null && dir.delete();
    }

        @ReactMethod
        public void clearWebViewCache(){
           String path = getReactApplicationContext().getFilesDir().getParent() + "/app_webview";
           File dir = new File(path);
           String[] children = dir.list();

           String cPath = this.getDiskCacheDir(); //getReactApplicationContext().getCacheDir().getAbsolutePath();
           File cacheDir = new File(cPath);
           deleteDir(cacheDir);

           for (String aChildren : children) {
                File cDir = new File(dir, aChildren);
                if(cDir.isDirectory() && aChildren.toLowerCase().indexOf("cache") != -1){
                    deleteDir(cDir);
                    return;
                }
           }

            //if (dir.isDirectory()) {
            //    deleteDir(dir);
            //}
        }

    @ReactMethod
    public void clearCache(Promise promise){
        try {
            Cache cache = OkHttpClientProvider.getOkHttpClient().cache();
            if (cache != null) {
                cache.delete();
            }
            promise.resolve(null);
        } catch(IOException e){
            promise.reject(e);
        }
    }

    @ReactMethod
    public void getHttpCacheSize(Promise promise){
        try {
            Cache cache = OkHttpClientProvider.getOkHttpClient().cache();
            promise.resolve(cache != null ? ((double)cache.size()) : 0);
        } catch(IOException e){
            promise.reject(e);
        }
    }

    static Method update;
    private void updateCacheSize(DiskStorageCache cache) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (update == null){
            update = DiskStorageCache.class.getDeclaredMethod("maybeUpdateFileCacheSize");
            update.setAccessible(true);
        }
        update.invoke(cache);
    }

    @ReactMethod
    public void getImageCacheSize(Promise promise){
        FileCache cache1 = ImagePipelineFactory.getInstance().getMainDiskStorageCache();
        long size1 = cache1.getSize();
        if (size1 < 0){
            try {
                updateCacheSize((DiskStorageCache)cache1);
            } catch (Exception e){
                promise.reject(e);
                return;
            }
            size1 = cache1.getSize();
        }
        FileCache cache2 = ImagePipelineFactory.getInstance().getSmallImageDiskStorageCache();
        long size2 = cache2.getSize();
        if (size2 < 0){
            try {
                updateCacheSize((DiskStorageCache)cache2);
            } catch (Exception e){
                promise.reject(e);
                return;
            }
            size2 = cache2.getSize();
        }
        promise.resolve(((double)(size1+size2)));
    }

    @ReactMethod
    public void clearImageCache(Promise promise){
        FileCache cache1 = ImagePipelineFactory.getInstance().getMainFileCache();
        cache1.clearAll();
        FileCache cache2 = ImagePipelineFactory.getInstance().getSmallImageFileCache();
        cache2.clearAll();
        promise.resolve(null);
    }
}
