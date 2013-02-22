Android-Image-Loader
====================

Introduction:


  ImageLoader is a simple, fast, optimized and efficient java component to load remote Images. ImageLoader component (abbreviated as IL here on) loads remote image URLs into Android ImageViews. IL handles multiple parallel remote URL requests asynchronously using a pool of threads, which is optimized for connection reuse. IL supports priority oriented loading of images by internally managing three level priority queue. It provides a callback mechanism to enable user to get notified about the state of the download. It utilizes in-memory caching of bitmap of images for better UE experience. It also supports filesystem based caching for efficiency. 

Flow:

  User initializes the ImageLoader component. Then user can pass-in required Image URL, required ImageView and optional priority value to IL. IL initializes the thread pool and initiates a download task. At the same time IL also keeps track of the tasks on the thread pool. Remote images are
being downloaded as image bitmaps by the task. These bitmaps are then handed over to the main thread for being loaded in the ImageViews. Same bitmaps are also handed over to a different pool of threads to be saved to filesystem. IL supervises each of these tasks and keeps track of the status of every request. 

Features:


1) IL handles multiple parallel remote URL requests asynchronously using a pool of threads, which is optimized for connection reuse. This pool of threads is basically configurable pool of Android AsyncTasks. Each task handles one request at a time. If number of parallel requests are more than the configured pool size then those requests are queued. And the AsyncTasks are re-used without re-spwaning new ones. The default pool size is set at 5.

2) Downloaded image bitmaps are handed over to main thread, where main thread takes care of loading the bitmaps into their respective Android ImageViews or it notifies the user application by means of registered callbacks.

3) IL further hand overs the image bitmaps to another pool of configurable Android AsyncTasks. Where these tasks take care of saving the image bitmaps to the filesystem. It stores the bitmaps in the form of png files. As this is lazy saving of bitmaps to the filesystem, we can configure this pool to be of size 1.

4) IL features priority based handling of parallel requests. It enables user to provide a priority level with every request to define the urgency of the same. IL internally manages three level priority request processing with the help of priority based queue.

5) IL caches loaded bitmaps internally using in-memory hash-map based cache for faster and responsive user experience.

6) IL enables user to register a callback to get notified about the request state. Once the image bitmap is downloaded IL calls the user's callback rather than directly loading the bitmap into ImageView. This lets the user post-process the bitmap before using. (Ex : Adding a reflection to the image bitmap).

7) IL limits the use of in-memory cache based on the memory class of the device and the runtime memory available to the application. IL restricts the in-memory cache between two limits upper limit and lower limit. Upper limit is approximately defined as one-forth part of memory class and lower limit is defined as upper limit - 4 MB. If the in-memory cache exceeds the upper limit IL auto-purges the memory used to lower limit value.

8) IL provides a feature for user driven purging. In this case user can pass a tag string along with every image request. This tag string is associated with the image request. Occasionally user can choose to purge all the image cache associated with specific tag string.

10) IL provides a feature to scale an image by a scale factor. Default scale factor is 1-no scaling.

11) IL takes care of multiple image requests for same URL. So even if same image is asked to be loaded at multiple ImageViews , the image is downloaded only once but the same bitmap is loaded at all the ImageViews.

12) IL provides a feature to set an expire time representing file-system cache time for every request. If the expire time is set to 0 then that image is not stored in the filesystem but just kept in in-memory cache. Periodically the IL purges the expired image cache.

13) IL class is a java singleton class, hence there is only 1 instance of the IL across entire application. And therefore IL's internal cache is also shared across all the activities of an application.

14) IL associates timestamp with every image request. This helps in controlling requests with different ImageURLs but same ImageView. The most recent timestamp associated request remains valid and IL always loads the most recent Image request into the ImageView irrespective of Image download time. 
