package com.artuok.appwork.fragmets;

import android.os.AsyncTask;

public class AverageAsync {

    private ListenerOnEvent executeListeners;
    private TaskAsync task;

    public AverageAsync(ListenerOnEvent executeListeners) {
        task = new TaskAsync();
        this.executeListeners = executeListeners;
    }

    public void exec(boolean b) {
        if (task != null) {
            task.execute(b);
        }
    }

    public void stop(boolean mayInterruptIfRunning) {
        task.cancel(mayInterruptIfRunning);
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
