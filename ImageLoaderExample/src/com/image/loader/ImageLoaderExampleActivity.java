package com.image.loader;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageLoaderExampleActivity extends Activity {

	ImageLoader il;
	ArrayList<String> images;

	class myArrayAdapter extends BaseAdapter {

		Context context;
		ArrayList<String> images;
		
		public myArrayAdapter(Context context, ArrayList<String> list) {
			super();
			this.context = context;
			this.images = list;
		}

		@Override 
		public View getView(int position, View v, ViewGroup vg) {
	    	ImageView imageView = null;
	    	if(v == null) {
		    	imageView = new ImageView(this.context);
				imageView.setLayoutParams(new GridView.LayoutParams(250, 200));
				imageView.setScaleType(ImageView.ScaleType.FIT_XY);
				imageView.setPadding(0, 0, 0, 0);
	    	} else {
		    	imageView = (ImageView) v;
	    	}
	    	
	    	imageView.setImageResource(R.drawable.loadinganimation);
			if(this.images.get(position) != null) {
				il.loadImage(this.images.get(position), imageView, new ImageLoader.ImageLoaderCallback() {

					@Override
					void OnDownload(String imageUrl, ImageView iv, Bitmap b) {
						if(iv != null) {
							iv.setImageBitmap(b);
						}
					}

				}, 3, 0, "default");
				//il.loadImage(this.images.get(position), imageView, 3, 0);
			}
			return imageView;
		}

		@Override
		public int getCount() {
			return this.images.size();
		}

		@Override
		public Object getItem(int position) {
			return this.images.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        Log.d("asd" , "" + System.currentTimeMillis());
        setContentView(R.layout.main);
        il = ImageLoader.initialize(this);
        
        GridView grid = (GridView) findViewById(R.id.gridView1);
        
        images = new ArrayList<String>();
        load();

		myArrayAdapter aa = new myArrayAdapter(this, images);
		grid.setAdapter(aa);
    }
	
    public void load() {
    	
		
 		//Any Sizes
/*     	images.add("http://2.bp.blogspot.com/-Gv4pVsalT0E/TbLRcp_5hnI/AAAAAAAAA0Q/I1MZGsNZBIg/s1600/Space-Art-Wallpapers-02.jpg");
    	images.add("https://encrypted-tbn2.google.com/images?q=tbn:ANd9GcQzia5K0XPEDdkFrlo70uV8Diy2ZivPxR_uX1jAVDorjdLHrtJCMw");
       	images.add("http://img219.imageshack.us/img219/8831/450normalmistyriverzv2.jpg");
    	images.add("http://blaberize.com/wp-content/uploads/2009/12/winter-wallpaper-by_karil.png");
    	images.add("http://maxcdn.crazyleafdesign.com/blog/wp-content/uploads/2008/09/cool-wallpapers-for-designers-16.jpg");
    	images.add("http://maxcdn.crazyleafdesign.com/blog/wp-content/uploads/2008/09/cool-wallpapers-for-designers-11.jpg");
    	images.add("http://maxcdn.crazyleafdesign.com/blog/wp-content/uploads/2008/09/abduzeedo-wallpaper-1.jpg");
    	images.add("http://farm3.static.flickr.com/2209/2330120041_eb51e6a9ec_o.jpg");
    	images.add("http://3d-wallpapers.net/wp-content/gallery/nature/montana-fire-by-john-mccolgan-2.jpg");
    	images.add("http://www.guimods.com/wp-content/uploads/raindrops-vista-wallpaper.jpg");
    	images.add("http://maxcdn.crazyleafdesign.com/blog/wp-content/uploads/2008/09/cool-wallpapers-for-designers-12.jpg");
    	images.add("http://www.wallcoo.net/1920x1440/1920x1440_Vista_wallpapers_01/images/%5Bwallcoo.com%5D_1920x1440_Vista_wallpaper_vplants11_007.jpg");
    	images.add("http://desdevweb.com/wp-content/uploads/25312_1600x1200-wallpaper-cb1286207398.jpg");
    	images.add("http://www.guimods.com/wp-content/uploads/crepuscule-vista-wallpaper.jpg");
    	images.add("http://www.wallpaper77.com/upload/DesktopWallpapers/thumbs/Fire-Flower-vectors-abstract-wallpapers_big.jpg");
    	images.add("http://noupe.com/img/wallpaper-3.jpg");
    	images.add("http://3.bp.blogspot.com/_8X7XqaUxGR0/TG6qqfbFQPI/AAAAAAAACM8/H15PzL--zWE/s1600/2011_New_Year_wallpaper.jpg");
    	images.add("http://blaberize.com/wp-content/uploads/2009/08/more-Abstract-wallpapers.png");
    	images.add("http://www.tricksdaddy.com/wp-content/uploads/2009/08/Retro_Lines_Wallpaper.jpg");
    	images.add("http://ancient-temple-ruins-animated-wallpaper.smartcode.com/images/sshots/ancient_temple_ruins_animated_wallpaper_26306.jpeg");

    	
    	
    	

  		//400 * 300
    	images.add("http://highdefinitionwallpaperss.com/wp-content/uploads/2012/04/Windows-Xp-Hd-Wallpapers.jpg");
    	images.add("http://3.bp.blogspot.com/_iTwpjOELp_0/SSy20nYmLXI/AAAAAAAABaQ/W5Lx0E2d4MQ/s400/Black+Wallpaper+5.jpg");
    	images.add("http://0.tqn.com/d/freebies/1/0/3/O/red-rose-wallpaper.jpg");
    	images.add("http://christian-bloggers.com/wp-content/plugins/wp-o-matic/cache/27ffd_jesus-wallpapers-0104.jpg");
    	images.add("http://2.bp.blogspot.com/_Gq1jO6iuU2U/SRn_-kgE43I/AAAAAAAAENs/SnHPTn3vt_M/s400/camaro-rear-angle-wallpapers_11363_1600x1200.jpg");
    	images.add("http://bp0.blogger.com/_0cRzUq2dhFU/Rex2s5sv3KI/AAAAAAAAAHE/Akb3bwOOhtw/s400/homer-simpson-wallpaper-brain-1024.jpg");
    	images.add("http://www.wallpapers22.com/images/3d-desktop-wallpapers-p.jpg");
    	images.add("http://imgs.mi9.com/uploads/holiday/1194/maldives-wallpapers_422_17619.jpg");
    	images.add("http://2.bp.blogspot.com/-O-lg_u3S2xQ/TcIUKMCtdjI/AAAAAAAAZMs/0Ud92ABUkKI/s400/Digital-3D-Art-Desktop-Backgrounds-Wallpapers2.jpg");
    	images.add("http://2.bp.blogspot.com/_Gq1jO6iuU2U/SGFCBiWsHdI/AAAAAAAACj8/gkRggX0rg6Q/s400/Radioactive+Bio+Hazard+Logo+Wallpaper+stockwallpapers.blogspot.com++3.jpg");
    	images.add("http://acsbiology.info/images/green-nature-wallpaper.jpg");
    	images.add("http://1.bp.blogspot.com/_89oxiIwvtak/SYDgzJ22Q3I/AAAAAAAAHUg/-qfAPg4LvFY/s400/Abstract+Wallpapers+(81).jpg");
    	images.add("http://www.allbestwallpapers.com/tagwallpaper/window-wallpapers.jpg");
    	*/
    	

// 		250 * 200
     	images.add("http://2.imimg.com/data2/FN/CK/MY-3288002/wallpaper-250x250.jpg");
    	images.add("http://www.danduna.com/wallpapers/thumb/wallpaper_131.jpg");
    	images.add("http://spiderman-web.com/spiderman/wallpaper/fan_art/joec/spidey3_venom_small.jpg");
    	images.add("http://www.designboom.com/weblog/images/zaha250.jpg");
    	images.add("http://cache.lifehacker.com/assets/images/17/2011/11/medium_73d60c1cc4df2437b29f54b989b14d17.jpg");
    	images.add("http://downloadsquad.switched.com/media/2006/03/wallpaper1.jpg");
    	images.add("http://indussound.com/images/wallpapers/wallpapers/nature-wallpaper-icon-2.jpg");
    	images.add("http://www.windows7wallpaper.org/d/2847-2/3d+Wallpaper+_20_.jpg");
    	images.add("http://www.gomywallpaper.com/s_wallpaper/2011-7/72220119254721.jpg");
    	images.add("http://www.orbwallpaper.com/thumb/1111/green-nature/green-nature-wallpaper.jpg");
    	images.add("http://www.oscustomize.com/wallpapers/technical/thumbs/sterlingware_wallpaper.jpg");
    	images.add("http://www.orbwallpaper.com/thumb/1794/fighter-planes/fighter-planes-flying-wallpapers96671024x768.jpg");
    	images.add("http://www.oscustomize.com/wallpapers/vector/thumbs/Vector_Wallpaper_1280x1024_by_WildNight.jpg");
    	images.add("http://i3.squidoocdn.com/resize/squidoo_images/-1/lens18489676_1315679905Green_Wallpaper_by_LuckyH");
    	images.add("http://free-wallpaper-backgrounds.com/direct/462x200/3D-Wallpapers/3d-wallpapers-backgrounds-39.jpg");
    	images.add("http://t3.gstatic.com/images?q=tbn:ANd9GcQ26BpjXtT3hSAo5Y-emmYDWUdHf6GPvzW-ywwVFl5P5Rok_sfGTE1g-Aan");
    	images.add("http://t0.gstatic.com/images?q=tbn:ANd9GcQjEwasliETBLy5lz_smimoxBYGRyMjlz6NleKGwgaNevtm4L05CR8KURhzsQ");
    	images.add("http://t1.gstatic.com/images?q=tbn:ANd9GcTF_3cXXDok_iqSsFVveXcaxoJnpw6qNrHA2R2wEICGcp9rH2rB2JCW0pif");
    	images.add("http://4.bp.blogspot.com/_az699VCrotM/TIE0uS-DWVI/AAAAAAAAAEg/Qrdqk0W72fA/S250/Beach_Sunset_Wallpaper.jpg");
    	images.add("http://1.bp.blogspot.com/_VhXOcgebxSc/TT6u3z1tWPI/AAAAAAAAACc/h02dz3gZpRA/s250/candle_wallpaper_candle_1009.jpg");
    	images.add("http://www.brainfitnessforlife.com/wp-content/uploads/2010/04/brain-games-bbc-nature.jpg");
    	images.add("http://www.conservationfund.org/sites/default/files/_nature_boy_biking_woodsiStockphoto_com_Acik.jpg");
    	images.add("http://www.my-photo-gallery.com/images/Hummingbird.jpg");
    	images.add("http://www.tfhmagazine.com/assets/008/22057_250wh.jpg");
    	images.add("http://crazycrackerz.com/wp-content/uploads/2010/11/grizzly2.jpg");
    	images.add("http://earthpathschool.com/images/earthpath1.jpg");
    	images.add("http://1.bp.blogspot.com/_qX4_LOtQRi8/TKBaTFf2xHI/AAAAAAAAAB0/OaPC7oMEGZU/S250/dolphin-wallpaper-1.jpg");
    	images.add("http://www.wallloop.com/uploads/allimg/2012-03/03200158-1-243R-lp.jpg");
    	images.add("http://blog.kis.ac.th/chisato/files/2010/03/figure221.jpeg");
    	images.add("http://www.sharewallpapers.org/d/472695-2/Autumn+scene+097+1280x1024.jpg");
    }
}