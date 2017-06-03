package com.gotenna.sdk.sample.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

/**
 * An abstract activity used to force an 'up' button in the Activity's ActionBar.
 *
 * Created on 2/10/16
 *
 * @author ThomasColligan
 */
public abstract class ChildActivity extends AppCompatActivity
{
    // ================================================================================
    // Life-Cycle Methods
    // ================================================================================

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar actions click
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
