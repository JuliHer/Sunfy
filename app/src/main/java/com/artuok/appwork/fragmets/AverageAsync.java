package com.artuok.appwork.fragmets;

import android.os.AsyncTask;

public class AverageAsync {

    private ListenerOnEvent executeListeners;

    public AverageAsync(ListenerOnEvent executeListeners) {
        this.executeListeners = executeListeners;
    }

    public void exec(boolean b) {
        TaskAsync task = new TaskAsync();

        task.execute(b);
    }

    private class TaskAsync extends AsyncTask<Boolean, Boolean, Boolean> {

        @Override
        protected void onPreExecute() {
            executeListeners.onPreExecute();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {
            executeListeners.onExecute(booleans[0]);
            return booleans[0];
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            executeListeners.onPostExecute(aBoolean);
        }
    }


    public interface ListenerOnEvent {
        void onPreExecute();

        void onExecute(boolean b);

        void onPostExecute(boolean b);
    }
}
