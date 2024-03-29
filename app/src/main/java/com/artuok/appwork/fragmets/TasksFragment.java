package com.artuok.appwork.fragmets;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.adapters.KanbanAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.dialogs.AnnouncementDialog;
import com.artuok.appwork.kanban.KanbanFragment;
import com.artuok.appwork.kanban.TaskFragment;
import com.artuok.appwork.library.Constants;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TasksFragment extends Fragment {

    ViewPager2 viewPager;
    TextView pageTitle;
    String mainTitle;
    String kanbanTitle;
    List<Fragment> fragmentList = new ArrayList<>();
    private InterstitialAd mInterstitialAd;
    TaskFragment taskFragment = new TaskFragment();
    KanbanFragment pendingFragment = new KanbanFragment();
    KanbanFragment inProcessFragment = new KanbanFragment();
    KanbanFragment completedFragment = new KanbanFragment();

    LinearLayout dotsView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainTitle = requireActivity().getString(R.string.task_to_do)+" ->";
        kanbanTitle = requireActivity().getString(R.string.kanban_table);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(requireActivity(), "ca-app-pub-5838551368289900/3738961693", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;

                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        mInterstitialAd = null;
                    }
                });
        initKanban();
        View root = inflater.inflate(R.layout.fragment_tasks, container, false);
        initViewPager(root);
        initTable();

        return root;
    }

    private void initKanban(){
        pendingFragment.initProject(0);
        pendingFragment.setTitle(requireContext().getString(R.string.pending_activities));
        inProcessFragment.initProject(1);
        inProcessFragment.setTitle(requireContext().getString(R.string.in_process_activities));
        completedFragment.initProject(2);
        completedFragment.setTitle(requireContext().getString(R.string.completed_tasks));
    }

    private void initTable(){
        KanbanAdapter viewPagerAdapter = new KanbanAdapter(requireActivity(), fragmentList);
        new AverageAsync(new AverageAsync.ListenerOnEvent() {
            @Override
            public void onPreExecute() {

            }

            @Override
            public void onExecute(boolean b) {
                taskFragment.setOnTaskModify(i -> {
                    notifyGlobalChanged(i);
                    inProcessFragment.restart(i);
                    pendingFragment.restart(i);
                    completedFragment.restart(i);
                });
                pendingFragment.setOnTaskModifyListener(i -> {
                    NotifyChanged();
                    notifyGlobalChanged(i);
                    completedFragment.restart(i);
                    inProcessFragment.restart(i);
                    taskFragment.reinitializate();
                });
                inProcessFragment.setOnTaskModifyListener(i -> {
                    NotifyChanged();
                    notifyGlobalChanged(i);
                    DbHelper dbHelper = new DbHelper(requireActivity());
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    Cursor pendingTasks = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status < '2'", null);
                    if (pendingTasks.getCount() < 1) {
                        showCongratulations();
                    }
                    pendingTasks.close();
                    mInterstitialAd.show(requireActivity());
                    pendingFragment.restart(i);
                    completedFragment.restart(i);
                    taskFragment.reinitializate();
                });
                completedFragment.setOnTaskModifyListener(i -> {
                    NotifyChanged();
                    notifyGlobalChanged(i);

                    inProcessFragment.restart(i);
                    pendingFragment.restart(i);
                    taskFragment.reinitializate();
                });
                fragmentList.clear();
                fragmentList.add(taskFragment);
                fragmentList.add(pendingFragment);
                fragmentList.add(inProcessFragment);
                fragmentList.add(completedFragment);
            }

            @Override
            public void onPostExecute(boolean b) {
                viewPager.setAdapter(viewPagerAdapter);
                viewPagerAdapter.notifyItemRangeInserted(0, fragmentList.size());
                taskFragment.reinitializate();
                pendingFragment.reInit();
                inProcessFragment.reInit();
                completedFragment.reInit();
                initDots(viewPagerAdapter.getItemCount());
            }
        }).exec(true);
    }

    private void initDots(int numPages){
        if(!isAdded())
            return;
        ImageView[] images = new ImageView[numPages];
        for (int i = 0; i < numPages; i++) {
            images[i] = new ImageView(requireActivity());
            if(i == 0){
                images[i].setImageResource(R.drawable.dot_selected);
            }else{
                images[i].setImageResource(R.drawable.dot_unselected);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsView.addView(images[i], params);
        }
    }

    private void onIndicatorChange(int position){


        for (int i = 0; i < dotsView.getChildCount(); i++) {
            ImageView image = (ImageView) dotsView.getChildAt(i);
            if(i == position){
                image.setImageResource(R.drawable.dot_selected);
            }else{
                image.setImageResource(R.drawable.dot_unselected);
            }

            image.requestLayout();
        }

    }

    private void initViewPager(View root){
        pageTitle = root.findViewById(R.id.page_title);
        pageTitle.setText(mainTitle);
        dotsView = root.findViewById(R.id.dotsLayout);
        viewPager = root.findViewById(R.id.viewpager);
        viewPager.setClipToPadding(false);
        viewPager.setClipChildren(false);
        viewPager.setOffscreenPageLimit(3);
        viewPager.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        viewPager.setPageTransformer(compositePageTransformer);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if(position == 0){
                    pageTitle.setText(mainTitle);
                }else{
                    pageTitle.setText(kanbanTitle);
                }
                onIndicatorChange(position);
            }
        });
    }


    public void showCongratulations() {
        MediaPlayer mp = MediaPlayer.create(requireActivity(), R.raw.completed);
        mp.start();

        AnnouncementDialog dialog = new AnnouncementDialog();
        dialog.setTitle(getString(R.string.completed_tasks));
        dialog.setText(getString(R.string.congratulations_1));
        dialog.setDrawable(R.drawable.ic_check_circle);
        dialog.setAgree(false);
        dialog.setBackgroundCOlor(requireActivity().getColor(R.color.blue_400));
        dialog.setOnPositiveClickListener(requireActivity().getString(R.string.Accept_M), view -> {
            dialog.dismiss();
        });

        dialog.setOnNegativeClickListener(requireActivity().getString(R.string.dismiss), view -> {
            dialog.dismiss();
        });

        dialog.show(requireActivity().getSupportFragmentManager(), "Congratulations to user");
    }

    public void NotifyChanged() {
//        pendingFragment.reinitializate();
//        inProcessFragment.reinitializate();
//        completedFragment.reinitializate();
//        taskFragment.reinitializate();
    }

    public void notifyGlobalChanged(int id){
        int i = 0;
        try {
            i = getPositionOfId(requireActivity(), id);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (i >= 0) {
            ((MainActivity) requireActivity()).notifyChanged(i);
        } else {
            ((MainActivity) requireActivity()).notifyAllChanged();
        }
    }



    public static int getPositionOfId(Context context, int id) throws ParseException {
        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor i = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE id = '" + id + "'", null);

        int pos = -1;
        if (i.moveToFirst()) {
            long date = i.getLong(5);


            Calendar c = Calendar.getInstance();
            int today = c.get(Calendar.DAY_OF_WEEK) - 1;
            c.setTimeInMillis(date);
            for (int j = 0; j < 7; j++) {
                if ((c.get(Calendar.DAY_OF_WEEK) - 1) == (today + j) % 7) {
                    pos = j;
                    break;
                }
            }
        }

        i.close();
        return pos;
    }

}