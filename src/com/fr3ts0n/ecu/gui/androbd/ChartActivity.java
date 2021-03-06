/*
 * (C) Copyright 2015 by fr3ts0n <erwin.scheuch-heilig@gmx.at>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package com.fr3ts0n.ecu.gui.androbd;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.fr3ts0n.ecu.EcuDataPv;
import com.fr3ts0n.ecu.prot.ObdProt;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * <code>Activity</code> that displays the readout of one <code>Sensor</code>.
 * This <code>Activity</code> must be started with an <code>Intent</code> that
 * passes in the number of the <code>Sensor</code>(s) to display. If none is
 * passed, the first available <code>Sensor</code> is used.
 */
public class ChartActivity extends Activity
{

	/**
	 * minimum time between screen updates
	 */
	public static final long MIN_UPDATE_TIME = 1000;

	/**
	 * For passing the index number of the <code>Sensor</code> in its
	 * <code>SensorManager</code>
	 */
	public static final String PID = "PID";

	/**
	 * List of colors to be used for series
	 */
	public static final int[] colors =
		{
			Color.RED,
			Color.YELLOW,
			Color.BLUE,
			Color.GREEN,
			Color.MAGENTA,
			Color.CYAN,
			Color.WHITE,
			Color.LTGRAY,
		};

	/**
	 * list of colors to be used for series
	 */
	public static final BasicStroke stroke[] =
		{
			BasicStroke.SOLID,
			BasicStroke.DASHED,
			BasicStroke.DOTTED,
		};

	/**
	 * The displaying component
	 */
	private GraphicalView chartView;

	/**
	 * Dataset of the graphing component
	 */
	private XYMultipleSeriesDataset sensorData;

	/**
	 * Renderer for actually drawing the graph
	 */
	private XYMultipleSeriesRenderer renderer;

	/**
	 * the wake lock to keep app communication alive
	 */
	private static WakeLock wakeLock;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.chart, menu);
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// prevent activity from falling asleep
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
			getString(R.string.app_name));
		wakeLock.acquire();

		setTitle("OBD data graph");

		/* get PIDs to be shown */
		int pids[] = getIntent().getIntArrayExtra(PID);

		// set up overall chart properties
		sensorData = new XYMultipleSeriesDataset();
		renderer = new XYMultipleSeriesRenderer(pids.length);
		chartView = ChartFactory.getTimeChartView(this, sensorData, renderer, "H:mm:ss");
		// set up global renderer
		renderer.setXTitle(getString(R.string.time));
		renderer.setXLabels(5);
		renderer.setYLabels(5);
		renderer.setGridColor(Color.DKGRAY);
		renderer.setShowGrid(true);
		renderer.setFitLegend(true);
		renderer.setClickEnabled(false);
		// set up chart data
		setUpChartData(pids);
		// make chart visible
		setContentView(chartView);
		// limit selected PIDs to selection
		ObdProt.setFixedPid(pids);
	}


	/**
	 * Handle menu selections
	 *
	 * @param item selected menu item
	 * @return result of super call
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.share:
				new ExportTask(this).execute(sensorData);
				break;

			case R.id.snapshot:
				Screenshot.takeScreenShot(this, getWindow().peekDecorView());
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Handle destroy of the Activity
	 */
	@Override
	protected void onDestroy()
	{
		ObdProt.resetFixedPid();
		// allow sleeping again
		wakeLock.release();
		super.onDestroy();
	}

	Timer refreshTimer = new Timer();

	/**
	 * Timer Task to cyclically update data screen
	 */
	private TimerTask updateTask = new TimerTask()
	{
		@Override
		public void run()
		{
			/* update chart */
			chartView.repaint();
		}
	};


	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart()
	{
		super.onStart();
		// start display update task
		try
		{
			refreshTimer.schedule(updateTask, 0, 1000);
		} catch (Exception e)
		{
			// exception ignored here ...
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop()
	{
		refreshTimer.purge();
		super.onStop();
	}

	/**
	 * Set up all the charting data series
	 *
	 * @param pids PIDs to be used for chart data
	 */
	private void setUpChartData(int[] pids)
	{
		long startTime = System.currentTimeMillis();
		int i = 0;
		EcuDataPv currPv;
		XYSeries currSeries;
		// loop through all PIDs
		for (int pid : pids)
		{
			// get corresponding Process variable
			currPv = (EcuDataPv) ObdProt.PidPvs.get(pid);
			if (currPv == null) continue;
			// get contained data series
			currSeries = (XYSeries) currPv.get(ObdItemAdapter.FID_DATA_SERIES);
			if (currSeries == null) continue;
			// add initial measurement to series data to ensure
			// at least one measurement is available
			if (currSeries.getItemCount() < 1)
				currSeries.add(startTime, (Float) currPv.get(EcuDataPv.FID_VALUE));

			// set scale to display series
			currSeries.setScaleNumber(i);
			// register series to graph
			sensorData.addSeries(i, currSeries);
			/* set up series visual parameters */
			renderer.setYTitle(String.valueOf(currPv.get(EcuDataPv.FID_UNITS)), i);
			renderer.setYAxisAlign(((i % 2) == 0) ? Align.LEFT : Align.RIGHT, i);
			renderer.setYLabelsAlign(((i % 2) == 0) ? Align.LEFT : Align.RIGHT, i);
			renderer.setYLabelsColor(i, colors[i % colors.length]);
			/* set up new line renderer */
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor(colors[i % colors.length]);
			r.setStroke(stroke[(i / colors.length) % stroke.length]);
			// register line renderer
			renderer.addSeriesRenderer(i, r);
			i++;
		}
	}
}
