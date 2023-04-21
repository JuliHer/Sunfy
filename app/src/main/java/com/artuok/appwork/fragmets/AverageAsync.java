package com.artuok.appwork.fragmets;

import android.os.AsyncTask;

public class AverageAsync {

    private ListenerOnEvent executeListeners;
    private TaskAsync task;
    private Boolean executing;

    public AverageAsync(ListenerOnEvent executeListeners) {
        task = new TaskAsync();
        executing = false;
        this.executeListeners = executeListeners;
    }

    public void exec(boolean b) {
        if (task != null) {
            if(!executing){
                task.execute(b);
                executing = true;
            }
        }
    }

    public Boolean isExecuting() {
        return executing;
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
            executing = false;
            executeListeners.onPostExecute(aBoolean);
        }
    }


    public interface ListenerOnEvent {
        void onPreExecute();

        void onExecute(boolean b);

        void onPostExecute(boolean b);
    }
}
