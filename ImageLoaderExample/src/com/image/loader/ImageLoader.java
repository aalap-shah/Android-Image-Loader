package com.image.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

/**
 * @author Aalap Shah
 * A helper class to load remote images asynchronously.
 */
public class ImageLoader{

    private static final String TAG = "ImageLoader";

	/**
	 * Object representing the image request 
	 */
	private class imageObject {
		/**
		 * imgaeUrl holds the remote URL of the image
		 */
		String imageUrl;
		/**
		 * imageBitmap holds the bitmap of the corresponding image once downloaded
		 */
		Bitmap imageBitmap;
		/**
		 * Scale factor of the image. It basically reduces the image quality by sampling while decoding. 
		 */
		Integer scale;
		/**
		 * Linked Queue of imageObjectPairs object which hold the ImageView and/or ImageCallback for the requests for
		 * same image URL.
		 */
		ConcurrentLinkedQueue<imageObjectPair> imageObjectPairs;
		/**
		 * Priority of this request
		 */
		int loadPriority;
		/** 
		 * Expire time of the request
		 */
		int expireTime;
		/**
		 * Tag assigned to the image request , used for timely purging of memory cache.
		 */
		String tag;
		/** 
		 * File pointer if the user chooses to store the image to filesystem
		 */
		File file;
	}

	/**
	 * This is an internal class. Basically stores the ImageView and/or ImageLoaderCallback and the time of the request. 
	 */
	private class imageObjectPair {
		ImageView iv;
		ImageLoaderCallback ilcb;
		long time;
		imageObjectPair(ImageView i, ImageLoaderCallback cb) {
			iv = i;
			ilcb = cb;
			time = System.currentTimeMillis();
		}
	}

	/** 
	 * This is an internal static class which represents the ImgaeLoaderCallback used 
	 * if user chooses to get notified about the download.
	 */
	public static class ImageLoaderCallback {
		void OnDownload(String imageUrl, ImageView iv, Bitmap b) {
			if(iv != null) {
				iv.setImageBitmap(b);
			}
		}
	}
	
	/**
	 * Singleton ImageLoader Reference instance.
	 */
	static ImageLoader ILRef;

	
	/**  
	 * Linked Hash Map storing the imageURL TO imageObject {@link imageObject}
	 */
	static LinkedHashMap<String, imageObject> imageCache;
	/** 
	 * Current image cache size, Initialized to 0
	 */
	static long imageCacheSize = 0;
	/**
	 * Image cache upper limit value. Initialized to default value of 10 MB. These get modified after getting the memory class
	 */
	static long imageCacheUpperLimit = 10*1024*1024;
	/**
	 * Image cache lower limit value. Initialized to default value of 8 MB.
	 */
	static long imageCacheLowerLimit = 8*1024*1024;
	/**
	 * Hashmap used to store the tag TO array of imageURLs. 
	 */
	static HashMap<String, ArrayList<String>> tagMap;
	/**
	 * Hash-map maintaining ImageView request to timestamp of the request.
	 */
	static HashMap<ImageView, Long> imageViewUpdateTimeMap;
	
	
	/**
	 * Download thread synchronizing object. 
	 */
	static final Object mTaskLock = new Object();
	/**
	 * download thread counter.
	 */
	static int mTaskCounter = 0;
	/**
	 * These are three priority values for image requests
	 */
	static int FASTEST_QUEUE = 3;
	static int FASTER_QUEUE = 2;
	static int FAST_QUEUE = 1;
	/**
	 * These are 3 concurrent linked queues representing three level priority queue, 
	 * namely Fastest, Faster, Fast. They hold the imageObject {@link imageObject}
	 */
	static ConcurrentLinkedQueue<Object> fastestQueue, fasterQueue, fastQueue;

	
	
	/**
	 * save thread synchronizing object.
	 */
	static final Object mSaveImageLock = new Object();
	/**
	 * Save image thread counter. 
	 */
	static int mSaveImageCounter = 0;
	/**
	 * File Directory pointer for storing the file-system cached images.
	 */
	static File fileDir;
	/**
	 * Thread used to lazy saving of images to file-system
	 */
	static Thread saveThread;
	/** 
	 * This is a concurrent linked queue holding imageObjects {@link imageObject} with images already download.
	 * Used to store the imageBitmaps to file system 
	 */
	static ConcurrentLinkedQueue<Object> saveQueue;

	
	
	
	/**
	 * read from file system thread synchronizing object.
	 */
	static final Object mReadImageLock = new Object();
	/**
	 * This is a concurrent linked queue holding imageObjects {@link imageObject} with images being loaded from the file-system.
	 */
	static ConcurrentLinkedQueue<Object> readQueue;
	/** 
	 * Flag indicating if the file-system read thread is instantiated or not.
	 */
	static boolean mReadImageFlag = false;

	
	
	/** 
	 * Private constructor as its a singleton object.
	 */
	private ImageLoader() {
	}

    /**
     * initializes the image loader and its internal structures.
     *
     * @param context - context is application's base context. Used to get the file-system access.
     * @return returns the reference to the ImageLoader object used for further APIs.
     */
	public static ImageLoader initialize(Context context) {
		if(ILRef == null) {
			ILRef = new ImageLoader();

			imageCache = new LinkedHashMap<String, imageObject>(200, 0.75f, true);
			tagMap = new HashMap<String, ArrayList<String>>();
			ArrayList<String> defaultList = new ArrayList<String>();
			tagMap.put("default", defaultList);

			fastestQueue = new ConcurrentLinkedQueue<Object>();
			fasterQueue = new ConcurrentLinkedQueue<Object>();
			fastQueue = new ConcurrentLinkedQueue<Object>();

			saveQueue = new ConcurrentLinkedQueue<Object>();
			readQueue = new ConcurrentLinkedQueue<Object>();

			mSaveImageCounter = 0;
			mTaskCounter = 0;
			fileDir = context.getCacheDir();
			if (!fileDir.exists()) {
				fileDir.mkdirs();
			}

			ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
			int memoryClass = activityManager.getMemoryClass();
			imageCacheUpperLimit = 1024*1024*(memoryClass/4);
			imageCacheLowerLimit = 1024*1024*(memoryClass/4 - 4);

			imageViewUpdateTimeMap = new HashMap<ImageView, Long>();
		}
		return ILRef;
	}

    /**
     * converts the image url into a file name to be used to store the image to file system
     *
     * @param imageUrl Image url of the image
     * @return string name equivalent of the url converted by replacing / with .
     */
	private static String convertImageUrl(String imageUrl) {
		String[] tokens = imageUrl.split("//", 2);
		if(tokens.length >= 2 && tokens[1] != null) {
			return tokens[1].replace('/', '.');
		} else {
			return imageUrl;
		}
	}

	/**
	 * This API lets user reset priorities of all the older movies, basically very useful in case user switches from 
	 * a screen to new screen in application. So all the requests from older screen are not immediately required anymore.
	 * Hence priorities of those can me reduced so new requests for new screen can get the bandwidth.
	 * It basically moves all of those with fastest priority to faster priority and all those with faster priority to 
	 * fast priority.
	 */
	public void resetPriortiy() {
		imageObject iO = null;
		while((iO = (imageObject) fasterQueue.poll()) != null) {
			fastQueue.add(iO);
		}
		while((iO = (imageObject) fastestQueue.poll()) != null) {
			fasterQueue.add(iO);
		}
	}

	/**
	 * loadImage is an API to load the remote image URL into Android ImageView or
	 * get notified about the download complete of the image by ImageLoaderCallback.
	 * It lets you define the load priority of the image url. Images can be loaded with 
	 * three different priorities namely fastest, faster, fast. User can choose from
	 * {@link FASTEST_QUEUE, FASTER_QUEUE, FAST_QUEUE}. It allows user to define the 
	 * expire time with the image. Expire time defines for how long will that image be 
	 * stored in the file-system. Tag is a string which is associated with the request. 
	 * It is used for purging. You can assign tag string with every request and do a bulk purging 
	 * based on the tag string. Scale is scaling factor used to reduce the image quality. 
	 * It affects the image's sampling while decoding.
	 * 
	 * @param imageUrl remote URL of the image to be download. 
	 * @param imageView ImageView in which the image has to be applied. if image callback is used still 
	 * image view can be passed. In that case in callback the imageview would be returned back. 
	 * @param ilcb Image loader callback if user wishes to be notified about the download.
	 * @param loadPriority image loading priority
	 * @param expireTime time for which the image should be cached in file-system
	 * @param tag tag string associated with the image.
	 * @param scale scaling factor of the image. Default it should be 1 (no scaling)
	 */
	public void loadImage(String imageUrl, ImageView imageView, ImageLoaderCallback ilcb, int loadPriority, int expireTime, String tag, int scale) {

		if(tag != null)
			LoadImage(imageUrl, imageView, ilcb, loadPriority, expireTime, tag, scale);
		else
			LoadImage(imageUrl, imageView, ilcb, loadPriority, expireTime, "default", scale);
	}

	/**
	 * load Image API with fewer parameters. The task is same as the main imageLoader API
	 *
	 * @param imageUrl remote URL of the image to be download. 
	 * @param imageView ImageView in which the image has to be applied. if image callback is used still 
	 * image view can be passed. In that case in callback the imageview would be returned back. 
	 * @param ilcb Image loader callback if user wishes to be notified about the download.
	 * @param loadPriority image loading priority
	 * @param expireTime time for which the image should be cached in file-system
	 * @param tag tag string associated with the image.
	 */
	public void loadImage(String imageUrl, ImageView imageView, ImageLoaderCallback ilcb, int loadPriority, int expireTime, String tag) {

		if(tag != null)
			LoadImage(imageUrl, imageView, ilcb, loadPriority, expireTime, tag, 1);
		else
			LoadImage(imageUrl, imageView, ilcb, loadPriority, expireTime, "default",1);
	}

	/**
	 * load Image API with fewer parameters. The task is same as the main imageLoader API
	 *
	 * @param imageUrl remote URL of the image to be download. 
	 * @param imageView ImageView in which the image has to be applied.
	 * @param loadPriority image loading priority
	 * @param expireTime time for which the image should be cached in file-system
	 * @param tag tag string associated with the image.
	 * @param scale scaling factor of the image. Default it should be 1 (no scaling)
	 */
	public void loadImage(String imageUrl, ImageView imageView, int loadPriority, int expireTime, String tag, int scale) {

		if(tag != null)
			LoadImage(imageUrl, imageView, null, loadPriority, expireTime, tag, scale);
		else
			LoadImage(imageUrl, imageView, null, loadPriority, expireTime, "default", scale);
	}

	/**
	 * load Image API with fewer parameters. The task is same as the main imageLoader API
	 *
	 * @param imageUrl remote URL of the image to be download. 
	 * @param imageView ImageView in which the image has to be applied.
	 * @param loadPriority image loading priority
	 * @param expireTime time for which the image should be cached in file-system
	 * @param tag tag string associated with the image.
	 */
	public void loadImage(String imageUrl, ImageView imageView, int loadPriority, int expireTime, String tag) {

		if(tag != null)
			LoadImage(imageUrl, imageView, null, loadPriority, expireTime, tag, 1);
		else
			LoadImage(imageUrl, imageView, null, loadPriority, expireTime, "default",1);
	}

	/**
	 * load Image API with fewer parameters. The task is same as the main imageLoader API
	 *
	 * @param imageUrl remote URL of the image to be download. 
	 * @param imageView ImageView in which the image has to be applied.
	 * @param loadPriority image loading priority
	 * @param expireTime time for which the image should be cached in file-system
	 */
	public void loadImage(String imageUrl, ImageView imageView, int loadPriority, int expireTime) {

		LoadImage(imageUrl, imageView, null, loadPriority, expireTime, "default",1);
	}

	/**
	 * load Image API with fewer parameters. The task is same as the main imageLoader API
	 *
	 * @param imageUrl remote URL of the image to be download. 
	 * @param loadPriority image loading priority
	 * @param expireTime time for which the image should be cached in file-system
	 * @param tag tag string associated with the image.
	 */
	public void loadImage(String imageUrl, int loadPriority, int expireTime, String tag) {

		if(tag != null)
			LoadImage(imageUrl, null, null, loadPriority, expireTime, tag, 1);
		else
			LoadImage(imageUrl, null, null, loadPriority, expireTime, "default", 1);
	}

	/**
	 * load Image API with fewer parameters. The task is same as the main imageLoader API
	 *
	 * @param imageUrl remote URL of the image to be download. 
	 * @param loadPriority image loading priority
	 * @param expireTime time for which the image should be cached in file-system
	 */
	public void loadImage(String imageUrl, int loadPriority, int expireTime) {

		LoadImage(imageUrl, null, null, loadPriority, expireTime, "default", 1);
	}

	/**
	 * This is the main LoadImage API which is made private and other wrappers are provided on top of this.
	 * 
	 * @param imageUrl remote URL of the image to be download. 
	 * @param imageView ImageView in which the image has to be applied. if image callback is used still 
	 * image view can be passed. In that case in callback the imageview would be returned back. 
	 * @param ilcb Image loader callback if user wishes to be notified about the download.
	 * @param loadPriority image loading priority
	 * @param expireTime time for which the image should be cached in file-system
	 * @param tag tag string associated with the image.
	 * @param scale scaling factor of the image. Default it should be 1 (no scaling)
	 */
	private void LoadImage(String imageUrl, ImageView imageView, ImageLoaderCallback imageCallback, int loadPriority, int expireTime, String tag, int scale ) {
//		Log.d(TAG, "Current Cache Size >" + imageCacheSize);
//		Log.d(TAG, "imageUrl > " + imageUrl + "   imageView > " + imageView);
		
		if((imageUrl == null) || (imageUrl.length() == 0)) {
			Log.e(TAG, "Image URL is null or Empty");
			return;
		}

		/** NOTE: New image is being loaded in imageView hence mark its timestamp. So that if same imageView was used to load
		 * some other image , once that other image is downloaded it should not update this version
		 */
		imageViewUpdateTimeMap.put(imageView, new Long(System.currentTimeMillis()));
		
		if (imageCache.containsKey(imageUrl) == true) {
			imageObject iO = imageCache.get(imageUrl);
			if(iO != null && iO.imageBitmap != null) {

				if(imageCallback != null) {
					imageCallback.OnDownload(imageUrl, imageView, iO.imageBitmap);
				} else if(imageView != null) {
					imageView.setImageBitmap(iO.imageBitmap);
				}
				imageObjectPair iOP;
				while((iOP = (imageObjectPair) iO.imageObjectPairs.poll()) != null) {
					if(iOP.ilcb != null) {
						iOP.ilcb.OnDownload(imageUrl, iOP.iv, iO.imageBitmap);
					} else {
						iOP.iv.setImageBitmap(iO.imageBitmap);
					}
				}
				return;
			} else if(iO != null) {
				boolean existsFlag = false; 
				for (Iterator<imageObjectPair> it = iO.imageObjectPairs.iterator(); it.hasNext();) {
					imageObjectPair iOP = (imageObjectPair)it.next();
					if(iOP.ilcb != null && iOP.ilcb == imageCallback && iOP.iv == imageView) {
						existsFlag = true;
						iOP.time = System.currentTimeMillis();
						break;
					} else if(iOP.ilcb == null && iOP.iv == imageView) {
						existsFlag = true;
						iOP.time = System.currentTimeMillis();
						break;
					}
				}
				if(existsFlag == false && imageCallback != null) {
					imageObjectPair iOP = new imageObjectPair(imageView, imageCallback);
					iO.imageObjectPairs.add(iOP);
				} else if(existsFlag == false && imageView != null) {
					imageObjectPair iOP = new imageObjectPair(imageView, null);
					iO.imageObjectPairs.add(iOP);
				}
				//imageCache.remove(imageUrl);
				return;
			} else {
				//Log.d(TAG, "Did the softrefernce clear my data ?>?????? for image url " + imageUrl);
			}
		}

		File f = new File(fileDir, convertImageUrl(imageUrl));
		if (f.exists()) {
			//Log.d(TAG, "Got the data from file :)" + imageUrl);

			imageObject iO1 = new imageObject();
			iO1 = new imageObject();
			iO1.imageUrl = imageUrl;

			iO1.imageObjectPairs = new ConcurrentLinkedQueue<imageObjectPair>();

			if(imageCallback != null) {
				imageObjectPair iOP = new imageObjectPair(imageView, imageCallback);
				iO1.imageObjectPairs.add(iOP);
			} else if(imageView != null) {
				imageObjectPair iOP = new imageObjectPair(imageView, null);
				iO1.imageObjectPairs.add(iOP);
			}

			iO1.file = f;
			iO1.tag = tag;

			synchronized (getClass()) {
				imageCache.put(imageUrl, iO1);
				tagImageUrl(tag, imageUrl);
			}

			readQueue.add(iO1);
			synchronized (mReadImageLock) {
				if(mReadImageFlag == false) {
					mReadImageFlag = true;
					//Log.d(TAG, "file exists mReadImageFlag true");
					new ReadImageTask().execute();
				}
			}
			return;
		}

		/** Fetch it again*/
		imageObject iO1 = new imageObject();
		iO1 = new imageObject();
		iO1.imageUrl = imageUrl;
		iO1.scale = scale;
		iO1.expireTime = (int) (System.currentTimeMillis()/1000 + expireTime);
		iO1.loadPriority = loadPriority;
		iO1.imageObjectPairs = new ConcurrentLinkedQueue<imageObjectPair>();
		//iO1.imageViews = new ConcurrentLinkedQueue<ImageView>();

		if(imageCallback != null) {
			imageObjectPair iOP = new imageObjectPair(imageView, imageCallback);
			iO1.imageObjectPairs.add(iOP);
		} else if(imageView != null) {
			//imageObjectPair iOP = new imageObjectPair(imageView, mILC);
			imageObjectPair iOP = new imageObjectPair(imageView, null);
			iO1.imageObjectPairs.add(iOP);
		}

		iO1.file = f;
		iO1.tag = tag;

		synchronized (getClass()) {
			imageCache.put(imageUrl, iO1);
			tagImageUrl(tag, imageUrl);
		}

		if(loadPriority == FASTEST_QUEUE) {
			fastestQueue.add(iO1);
		} else if(loadPriority == FASTER_QUEUE) {
			fasterQueue.add(iO1);
		} else if(loadPriority == FAST_QUEUE) {
			fastQueue.add(iO1);
		}

		synchronized (mSaveImageLock) {
			mSaveImageCounter++;
			if(mSaveImageCounter == 1) {
				new SaveImageTask().execute();
			}
		}

		synchronized (mTaskLock) {
			if(mTaskCounter < 5) {
				mTaskCounter++;
				new DownloadImageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])(null));
			}
		}
	}

	/**
	 * This API is private and used internally to store imageUrl and corresponding 
	 * tag value in the hashmap.
	 * 
	 * @param tag tag string to be associated.
	 * @param url imageUrl to be associated to.
	 */
	private void tagImageUrl(String tag, String url) {
		if(tag != null) {
			if (tagMap.containsKey(tag) == true) {
				ArrayList<String> list = tagMap.get(tag);
				list.add(url);
			} else {
				ArrayList<String> list = new ArrayList<String>();
				list.add(url);
				tagMap.put(tag, list);
			}
		} else {
			ArrayList<String> list = tagMap.get("default");
			list.add(url);
		}
	}

	/**
	 * This is AsyncTask which reads cached images from the file-system.  
	 */
	private class ReadImageTask extends AsyncTask<Void, imageObject, Void> {

		@Override 
		protected Void doInBackground(Void... urls) {

			imageObject iO = null;

			while((iO = (imageObject) readQueue.poll()) != null) {

				iO.imageBitmap = BitmapFactory.decodeFile(iO.file.getPath());

				//Log.d(TAG, "publishing progress for" + iO.imageUrl);
				publishProgress(iO);
				iO = null;
			}
			return null;
		}

		protected void onProgressUpdate(imageObject... values) {
			super.onProgressUpdate(values);

			imageObjectPair iOP = null;
			if(values[0].imageBitmap != null) {
				synchronized (getClass()) {
					if(imageCacheSize >= imageCacheUpperLimit) {
						pergeCache();
					}
					
					//Log.d(TAG, values[0].imageBitmap.getByteCount() + "   " + values[0].imageBitmap.getHeight() * values[0].imageBitmap.getWidth());
					imageCacheSize = imageCacheSize + values[0].imageBitmap.getByteCount();
				}
				while((iOP = (imageObjectPair) values[0].imageObjectPairs.poll()) != null) {
					Long l = (Long)imageViewUpdateTimeMap.get(iOP.iv);
					if(iOP.time >= l.longValue()) {
						if(iOP.ilcb != null) {
							iOP.ilcb.OnDownload(values[0].imageUrl, iOP.iv, values[0].imageBitmap);
						} else {
							iOP.iv.setImageBitmap(values[0].imageBitmap);
						}
					}
				}
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if(readQueue.size() > 0 ) {
				//Log.d(TAG,"Images are still in ReadQueue, starting Read queue thread again.");
				synchronized (mReadImageLock) {
					mReadImageFlag = true;
					new ReadImageTask().execute();
				}

			} else {
				synchronized (mReadImageLock) {
					mReadImageFlag = false;
					//Log.d(TAG, "+onPostExecute mReadImageFlag false");
				}
			}
		}

	}

	/**
	 * This is AsyncTask which is responsible for downloading image bitmaps
	 * for remote image URLs.
	 * NOTE : Even though we might not get the Bitmap from the getBitmap API we need to  
	 * get through the entire chain till save imageBitmap , because if we ignore it then 
	 * the saveImageCounter might get screwed. And if we also decrease that then we might 
	 * just lose track of breaking the loop of the SaveImageTask loop. And we cant force 
	 * break the loop from outside cause it might be saving some images. So its better to
	 * let the broken image go through the chain and ignore while saving it.
	*/
	private class DownloadImageTask extends AsyncTask<Void, imageObject, Void> {

		Bitmap getBitmap(String url, Integer scale) {
			Bitmap bitmap = null;
			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = scale;
				bitmap = BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream(), null, options);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return bitmap;
		}

		@Override 
		protected Void doInBackground(Void... urls) {

			imageObject iO = null;

			while(true) {
				if((iO = (imageObject) fastestQueue.poll()) != null) {
					iO.imageBitmap = getBitmap(iO.imageUrl, iO.scale);
					//downloadedQueue.add(iO);
					//Log.d(TAG, "publishing progress for" + iO.imageUrl);
					publishProgress(iO);
					iO = null;
					continue;
				} 
				if ((iO = (imageObject) fasterQueue.poll()) != null) {
					iO.imageBitmap = getBitmap(iO.imageUrl, iO.scale);
					//downloadedQueue.add(iO);
					publishProgress(iO);
					iO = null;
					continue;
				}
				if((iO = (imageObject) fastQueue.poll()) != null) {
					iO.imageBitmap = getBitmap(iO.imageUrl, iO.scale);
					//downloadedQueue.add(iO);
					publishProgress(iO);
					iO = null;
					continue;
				}
				synchronized (mTaskLock) {
					mTaskCounter--;
					break;
				}
			}
			return null;
		}

		protected void onProgressUpdate(imageObject... values) {
			super.onProgressUpdate(values);

			imageObjectPair iOP = null;
			if(values[0].imageBitmap != null) {
				synchronized (getClass()) {
					if(imageCacheSize >= imageCacheUpperLimit) {
						pergeCache();
					}
					imageCacheSize = imageCacheSize + values[0].imageBitmap.getByteCount();
				}
				while((iOP = (imageObjectPair) values[0].imageObjectPairs.poll()) != null) {
					Long l = (Long)imageViewUpdateTimeMap.get(iOP.iv);
					if(iOP.time >= l.longValue()) {
						if(iOP.ilcb != null) {
							iOP.ilcb.OnDownload(values[0].imageUrl, iOP.iv, values[0].imageBitmap);
						} else {
							//Log.d(TAG, "setting imageUrl > " + values[0].imageUrl +  "   imageView > " + iOP.iv);
							iOP.iv.setImageBitmap(values[0].imageBitmap);
						}
					}
				}
			}
			//Log.d(TAG, "Adding to savQueue" + values[0].imageUrl);
			saveQueue.add(values[0]);
		}
		@Override
		protected void onPostExecute(Void result) {
	        //Log.d(TAG , "" + System.currentTimeMillis());
		}
	}

	/**
	 * This class is an AsyncTask which saves the download image bitmaps into the file-system 
	 */
	private class SaveImageTask extends AsyncTask<Void, imageObject, Void> {

		@Override 
		protected Void doInBackground(Void... urls) {

			imageObject iO = null;

			while(true) {
				if((iO = (imageObject) saveQueue.poll()) != null) {
					try {
						//Log.d(TAG, "saving file" + iO.imageUrl);
						if(iO.imageBitmap != null) {
							FileOutputStream fileOS = new FileOutputStream(iO.file);
							iO.imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOS);
						} else {
							/* Note : If imageBitmap is null then remove it from cache 
								cause otherwise it would not fetch it ever again*/
							synchronized (getClass()) {
								imageCache.remove(iO.imageUrl);
							}
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					iO = null;
					synchronized (mSaveImageLock) {
						mSaveImageCounter--;
						if(mSaveImageCounter == 0)
						{
							break;
						}
						continue;
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			//Log.d(TAG, "Current Cache Size >" + imageCacheSize);
			synchronized (getClass()) {
				if(imageCacheSize >= imageCacheUpperLimit) {
					pergeCache();
				}
			}
		}
	}

	/**
	 * This API is internally used to purge the in-memory cache when the cache size goes beyond upper limit
	 * It purges the cache till it reaches lower limit.
	 */
	private void pergeCache() {
		//Log.d(TAG, "Perging Cache > Image Cache UpperLimit[" + imageCacheUpperLimit + "]  LowerLimit[" + imageCacheLowerLimit + "]");

		Iterator<Entry<String, imageObject>> it = imageCache.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, imageObject> pairs = (Map.Entry<String, imageObject>)it.next();
			imageObject io = (imageObject) pairs.getValue();
			if(io != null && io.imageBitmap != null) {
				imageCacheSize = imageCacheSize - io.imageBitmap.getByteCount();
			}
			if(imageCacheSize <= imageCacheLowerLimit) {
				break;
			}
			it.remove(); // avoids a ConcurrentModificationException
		}
		//Log.d(TAG, "Cache Size after Perging >" + imageCacheSize + "  current imageCache size >" + imageCache.size());
	}

	/**
	 * This API completely clears the internal image cache. Should be used only in emergency.
	 */
	public void decache() {

		if(!imageCache.isEmpty()) {
			imageCache.clear();
			imageCacheSize = 0;
		}
	}

	/**
	 * This API is exposed to user to do manual tag based purging. So once they have passed tags along with
	 * image requests and they choose to purge all those images associated with specific tag name can be purged.
	 * 
	 * @param tag tag string to purge with.
	 */
	public void decacheByTag(String tag) {
		if(tag != null) {
			if(tagMap.containsKey(tag)) {
				ArrayList<String> list = tagMap.remove(tag);
				for(String item: list) {
					if(imageCache.containsKey(item)) {
						imageObject io = imageCache.remove(item);
						if(io != null && io.imageBitmap != null) {
							imageCacheSize = imageCacheSize - io.imageBitmap.getByteCount();
						}
					}
				}
			}
		} else {
			decache();
		}
	}
}
