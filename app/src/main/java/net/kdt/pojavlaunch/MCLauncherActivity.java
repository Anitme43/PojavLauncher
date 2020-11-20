package net.kdt.pojavlaunch;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.support.design.widget.*;
import android.support.v4.app.*;
import android.support.v4.view.*;
import android.support.v7.app.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import com.google.gson.*;
import com.kdt.filerapi.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.fragments.*;
import net.kdt.pojavlaunch.prefs.*;
import net.kdt.pojavlaunch.utils.*;
import net.kdt.pojavlaunch.value.*;
import org.lwjgl.glfw.*;

import android.support.v7.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.app.*;
import org.apache.commons.io.*;
import net.kdt.pojavlaunch.tasks.*;
//import android.support.v7.view.menu.*;
//import net.zhuoweizhang.boardwalk.downloader.*;

public class MCLauncherActivity extends BaseLauncherActivity
{
	//private FragmentTabHost mTabHost;
	private LinearLayout fullTab;
    /*
	private PojavLauncherViewPager viewPager;
	private VerticalTabLayout tabLayout;
    */
    
    private ViewPager viewPager;
	private TabLayout tabLayout;

	private TextView tvUsernameView;
	private String profilePath = null;
	private ViewPagerAdapter viewPageAdapter;

	private Button switchUsrBtn, logoutBtn; // MineButtons
	private ViewGroup leftView, rightView;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        
		viewInit();

		if (BuildConfig.DEBUG) {
			Toast.makeText(this, "Launcher process id: " + android.os.Process.myPid(), Toast.LENGTH_LONG).show();
        }
	}
	// DEBUG
	//new android.support.design.widget.NavigationView(this);

	private String getStr(int id, Object... val) {
		if (val != null && val.length > 0) {
			return getResources().getString(id, val);
		} else {
			return getResources().getString(id);
		}
	}

	private void viewInit() {
        // setContentView(R.layout.launcher_main_v3);
		setContentView(R.layout.launcher_main);

		fullTab = findViewById(R.id.launchermainFragmentTabView);
		tabLayout = findViewById(R.id.launchermainTabLayout);
		viewPager = findViewById(R.id.launchermainTabPager);

		mConsoleView = new ConsoleFragment();
		mCrashView = new CrashFragment();

		viewPageAdapter = new ViewPagerAdapter(getSupportFragmentManager());

		viewPageAdapter.addFragment(new LauncherFragment(), getStr(R.string.mcl_tab_news));
		viewPageAdapter.addFragment(mConsoleView, getStr(R.string.mcl_tab_console));
		viewPageAdapter.addFragment(mCrashView, getStr(R.string.mcl_tab_crash));

		viewPager.setAdapter(viewPageAdapter);
		tabLayout.setupWithViewPager(viewPager);

		tvUsernameView = (TextView) findId(R.id.launcherMainUsernameView);
		mTextVersion = (TextView) findId(R.id.launcherMainVersionView);

		try {
			profilePath = PojavProfile.getCurrentProfilePath(this);
			mProfile = PojavProfile.getCurrentProfileContent(this);

			tvUsernameView.setText(mProfile.getUsername());
		} catch(Exception e) {
			//Tools.throwError(this, e);
			e.printStackTrace();
			Toast.makeText(this, getStr(R.string.toast_login_error, e.getMessage()), Toast.LENGTH_LONG).show();
			finish();
		}
/*
        File logFile = new File(Tools.MAIN_PATH, "latestlog.txt");
        if (logFile.exists() && logFile.length() < 20480) {
            String errMsg = "Error occurred during initialization of ";
            try {
                String logContent = Tools.read(logFile.getAbsolutePath());
                if (logContent.contains(errMsg + "VM") && 
                  logContent.contains("Could not reserve enough space for")) {
                    OutOfMemoryError ex = new OutOfMemoryError("Java error: " + logContent);
                    ex.setStackTrace(null);
                    Tools.showError(MCLauncherActivity.this, ex);
                      
                    // Do it so dialog will not shown for second time
                    Tools.write(logFile.getAbsolutePath(), logContent.replace(errMsg + "VM", errMsg + "JVM"));
                }
            } catch (Throwable th) {
                System.err.println("Could not detect java crash");
                th.printStackTrace();
            }
        }
*/
		//showProfileInfo();

		List<String> versions = new ArrayList<String>();
		final File fVers = new File(Tools.versnDir);

		try {
			if (fVers.listFiles().length < 1) {
				throw new Exception(getStr(R.string.error_no_version));
			}

			for (File fVer : fVers.listFiles()) {
                if (fVer.isDirectory())
				    versions.add(fVer.getName());
			}
		} catch (Exception e) {
			versions.add(getStr(R.string.global_error) + ":");
			versions.add(e.getMessage());

		} finally {
			mAvailableVersions = versions.toArray(new String[0]);
		}

		//availableVersions;

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mAvailableVersions);
		adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
		mVersionSelector = (Spinner) findId(R.id.launcherMainSelectVersion);
		mVersionSelector.setAdapter(adapter);

		mLaunchProgress = (ProgressBar) findId(R.id.progressDownloadBar);
		mLaunchTextStatus = (TextView) findId(R.id.progressDownloadText);
		LinearLayout exitLayout = (LinearLayout) findId(R.id.launcherMainExitbtns);
		switchUsrBtn = (Button) exitLayout.getChildAt(0);
		logoutBtn = (Button) exitLayout.getChildAt(1);

		leftView = (LinearLayout) findId(R.id.launcherMainLeftLayout);
		mPlayButton = (Button) findId(R.id.launcherMainPlayButton);
		rightView = (ViewGroup) findId(R.id.launcherMainRightLayout);

		statusIsLaunching(false);
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
        Tools.updateWindowSize(this);
	}

	private float updateWidthHeight() {
		float leftRightWidth = (float) CallbackBridge.windowWidth / 100f * 32f;
		float mPlayButtonWidth = CallbackBridge.windowWidth - leftRightWidth * 2f;
		LinearLayout.LayoutParams leftRightParams = new LinearLayout.LayoutParams((int) leftRightWidth, (int) Tools.dpToPx(this, CallbackBridge.windowHeight / 9));
		LinearLayout.LayoutParams mPlayButtonParams = new LinearLayout.LayoutParams((int) mPlayButtonWidth, (int) Tools.dpToPx(this, CallbackBridge.windowHeight / 9));
		leftView.setLayoutParams(leftRightParams);
		rightView.setLayoutParams(leftRightParams);
		mPlayButton.setLayoutParams(mPlayButtonParams);

		return leftRightWidth;
	}

	public void mcaccSwitchUser(View view)
	{
		showProfileInfo();
	}

	public void mcaccLogout(View view)
	{
		//PojavProfile.reset();
		finish();
	}

	private void showProfileInfo()
	{
		/*
		 new AlertDialog.Builder(this)
		 .setTitle("Info player")
		 .setMessage(
		 "AccessToken=" + profile.getAccessToken() + "\n" +
		 "ClientID=" + profile.getClientID() + "\n" +
		 "ProfileID=" + profile.getProfileID() + "\n" +
		 "Username=" + profile.getUsername() + "\n" +
		 "Version=" + profile.getVersion()
		 ).show();
		 */
	}

	private void selectTabPage(int pageIndex){
		if (tabLayout.getSelectedTabPosition() != pageIndex) {
			tabLayout.setScrollPosition(pageIndex,0f,true);
			viewPager.setCurrentItem(pageIndex);
		}
	}

	@Override
	protected void onResumeFragments()
	{
		super.onResumeFragments();
		new RefreshVersionListTask(this).execute();
		
		try{
			final ProgressDialog barrier = new ProgressDialog(this);
			barrier.setMessage("Waiting");
			barrier.setProgressStyle(barrier.STYLE_SPINNER);
			barrier.setCancelable(false);
			barrier.show();

			new Thread(new Runnable(){

					@Override
					public void run()
					{
						while (mConsoleView == null) {
							try {
								Thread.sleep(20);
							} catch (Throwable th) {}
						}

						try {
							Thread.sleep(100);
						} catch (Throwable th) {}

						runOnUiThread(new Runnable() {
								@Override
								public void run()
								{
									try {
										mConsoleView.putLog("");
										barrier.dismiss();
									} catch (Throwable th) {
										startActivity(getIntent());
										finish();
									}
								}
							});
					}
				}).start();

			File lastCrashFile = Tools.lastFileModified(Tools.crashPath);
			if(CrashFragment.isNewCrash(lastCrashFile) || !mCrashView.getLastCrash().isEmpty()){
				mCrashView.resetCrashLog = false;
				selectTabPage(2);
			} else throw new Exception();
		} catch(Throwable e){
			selectTabPage(tabLayout.getSelectedTabPosition());
		}
	}

	@Override
	protected void onResume(){
		super.onResume();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
	}

	private boolean canBack = false;
	public void statusIsLaunching(boolean isLaunching)
	{
		LinearLayout.LayoutParams reparam = new LinearLayout.LayoutParams((int) updateWidthHeight(), LinearLayout.LayoutParams.WRAP_CONTENT);
		ViewGroup.MarginLayoutParams lmainTabParam = (ViewGroup.MarginLayoutParams) fullTab.getLayoutParams();
		int launchVisibility = isLaunching ? View.VISIBLE : View.GONE;
		mLaunchProgress.setVisibility(launchVisibility);
		mLaunchTextStatus.setVisibility(launchVisibility);
		lmainTabParam.bottomMargin = reparam.height;
		leftView.setLayoutParams(reparam);

		switchUsrBtn.setEnabled(!isLaunching);
		logoutBtn.setEnabled(!isLaunching);
		mVersionSelector.setEnabled(!isLaunching);
		canBack = !isLaunching;
	}
	@Override
	public void onBackPressed()
	{
		if (canBack) {
			super.onBackPressed();
		}
	}
	
	// Catching touch exception
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	public void launchGame(View v)
	{
		if (!canBack && mIsAssetsProcessing) {
			mIsAssetsProcessing = false;
			statusIsLaunching(false);
		} else if (canBack) {
			v.setEnabled(false);
			mTask = new MinecraftDownloaderTask(this);
			mTask.execute(mProfile.getVersion());
			mCrashView.resetCrashLog = true;
		}
	}

	
    
	public View findId(int id)
	{
		return findViewById(id);
	}

	public void launcherMenu(View view)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.mcl_options);
		builder.setItems(R.array.mcl_options, new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface p1, int p2)
				{
					switch (p2) {
						case 0: // Mod installer
							installMod(false);
							break;
                        case 1: // Mod installer with java args 
                            installMod(true);
							break;
						case 2: // Custom controls
							if (Tools.enableDevFeatures) {
								startActivity(new Intent(MCLauncherActivity.this, CustomControlsActivity.class));
							}
							break;
						case 3: // Settings
							startActivity(new Intent(MCLauncherActivity.this, LauncherPreferenceActivity.class));
							break;
						case 4: { // About
								final AlertDialog.Builder aboutB = new AlertDialog.Builder(MCLauncherActivity.this);
								aboutB.setTitle(R.string.mcl_option_about);
								try
								{
									aboutB.setMessage(Html.fromHtml(String.format(Tools.read(getAssets().open("about_en.txt")),
																	Tools.APP_NAME,
																	Tools.usingVerName,
																	"3.2.3")
													  ));
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
								aboutB.setPositiveButton(android.R.string.ok, null);
								aboutB.show();
							} break;
					}
				}
			});
		builder.show();
	}

	private void installMod(boolean customJavaArgs) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.alerttitle_installmod);
		builder.setNegativeButton(android.R.string.cancel, null);
        
		final AlertDialog dialog;
        if (customJavaArgs) {
            final EditText edit = new EditText(this);
            edit.setSingleLine();
            edit.setHint("-jar/-cp /path/to/file.jar ...");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface di, int i) {
                    Intent intent = new Intent(MCLauncherActivity.this, JavaGUILauncherActivity.class);
                    intent.putExtra("javaArgs", edit.getText().toString());
                    startActivity(intent);
                }
            });
            dialog = builder.create();
            dialog.setView(edit);
        } else {
            dialog = builder.create();
            FileListView flv = new FileListView(dialog);
            flv.setFileSelectedListener(new FileSelectedListener(){
                    @Override
                    public void onFileSelected(File file, String path) {
                        if (file.getName().endsWith(".jar")) {
                            Intent intent = new Intent(MCLauncherActivity.this, JavaGUILauncherActivity.class);
                            intent.putExtra("modFile", file);
                            startActivity(intent);
                            dialog.dismiss();
                        }
                    }
                });
            dialog.setView(flv);
        }
		dialog.show();
	}

	private class ViewPagerAdapter extends FragmentPagerAdapter {

		List<Fragment> fragmentList = new ArrayList<>();
		List<String> fragmentTitles = new ArrayList<>();

		public ViewPagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}

		@Override
		public Fragment getItem(int position) {
			return fragmentList.get(position);
		}

		@Override
		public int getCount() {
			return fragmentList.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return fragmentTitles.get(position);
		}

		public void addFragment(Fragment fragment, String name) {
			fragmentList.add(fragment);
			fragmentTitles.add(name);
		}

		public void setFragment(int index, Fragment fragment, String name) {
			fragmentList.set(index, fragment);
			fragmentTitles.set(index, name);
		}

		public void removeFragment(int index) {
			fragmentList.remove(index);
			fragmentTitles.remove(index);
		}
	}
}
