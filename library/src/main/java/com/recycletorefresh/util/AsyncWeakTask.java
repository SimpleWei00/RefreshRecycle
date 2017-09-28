package com.recycletorefresh.util;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AsyncWeakTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result>
{

    private List<WeakReference<Object>> mObjReferences          = null;
    private Exception mExcep = null;

    public AsyncWeakTask(Object... objs)
    {
        mObjReferences = new ArrayList<>(objs.length);
        for (Object obj : objs)
        {
            addToWeakReference(obj);
        }
    }

    public final void addToWeakReference(Object obj)
    {
        if (obj == null)
            throw new NullPointerException();
        mObjReferences.add(new WeakReference<Object>(obj));
    }

    protected void onPreExecute(Object[] objs)
    {
    }

    protected abstract Result doInBackgroundImpl(Params... params) throws Exception;

    protected void onProgressUpdate(Object[] objs, Progress... values)
    {
    }

    protected void onCancelled(Object[] objs)
    {
    }

    protected void onPostExecute(Object[] objs, Result result)
    {
    }

    protected void onException(Object[] objs, Exception e)
    {
    }

    @Override
    protected final void onPreExecute()
    {
        Object[] objs = getObjects();
        if (objs == null)
            cancel(true);
        else
            onPreExecute(objs);
    }

    @Override
    protected final Result doInBackground(Params... params)
    {
        try
        {
            return doInBackgroundImpl(params);
        } catch (Exception e)
        {
            mExcep = e;
            return null;
        }
    }

    @Override
    protected final void onProgressUpdate(Progress... values)
    {
        if(isCancelled())
            return;
        Object[] objs = getObjects();
        if (objs == null)
            cancel(true);
        else
            onProgressUpdate(objs, values);
    }

    @Override
    protected final void onCancelled()
    {
        Object[] objs = getObjects();
        if (objs != null)
            onCancelled(objs);
    }

    @Override
    protected final void onPostExecute(Result result)
    {
        Object[] objs = getObjects();
        if (objs != null)
        {
            if(mExcep == null)
                onPostExecute(objs, result);
            else
                onException(objs, mExcep);
        }
    }

    private Object[] getObjects()
    {
        Object[] objs = new Object[mObjReferences.size()];
        Iterator<WeakReference<Object>> objIterator = mObjReferences.iterator();
        for (int i = 0; i < objs.length; i++)
        {
            objs[i] = objIterator.next().get();
            if (objs[i] == null)
                return null;
        }
        return objs;
    }
}