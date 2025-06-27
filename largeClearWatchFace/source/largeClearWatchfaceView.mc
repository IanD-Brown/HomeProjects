import Toybox.WatchUi;

using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.System;
using Toybox.Lang;
using Toybox.Time.Gregorian;
using Toybox.ActivityMonitor;
using Toybox.Application;

class largeClearWatchfaceView extends WatchUi.WatchFace {
    var font;
    var symbols;

    var primary;
    var secondary;
    var background;

    function initialize() {
        WatchFace.initialize();
    }

    // Load your resources here
    function onLayout(dc) {
        setLayout(Rez.Layouts.WatchFace(dc));
        font = WatchUi.loadResource(Rez.Fonts.id_extraBold);
        symbols = WatchUi.loadResource(Rez.Fonts.id_symbols);
    }

    // Called when this View is brought to the foreground. Restore
    // the state of this View and prepare it to be shown. This includes
    // loading resources into memory.
    function onShow() {
    }

    // Update the view
    function onUpdate(dc) {
        primary = Application.Properties.getValue("PrimaryColor");
        secondary = Application.Properties.getValue("SecondaryColor");
        background = Application.Properties.getValue("BackgroundColor");

        setDate();
        setSteps();
        setBattery();
        setTime();

        // Call the parent onUpdate function to redraw the layout
        View.onUpdate(dc);
    }

    function setTime() as Void {
        var clockTime = System.getClockTime();
        var hours = clockTime.hour;
        if (!System.getDeviceSettings().is24Hour) {
            if (hours > 12) {
                hours = hours - 12;
            }
        } else {
            if (Application.Properties.getValue("UseMilitaryFormat")) {
                hours = hours.format("%02d");
            }
        }

        var timeString = Lang.format("$1$:$2$", [hours.format("%2d"), clockTime.min.format("%02d")]);
        var timeDisplay = View.findDrawableById("TimeLabel") as Text;
        timeDisplay.setLocation(timeDisplay.locX, System.getDeviceSettings().screenHeight/2 - 80);
        timeDisplay.setFont(font);
        timeDisplay.setText(timeString);
    }

    // Called when this View is removed from the screen. Save the
    // state of this View here. This includes freeing resources from
    // memory.
    function onHide() {
    }

    // The user has just looked at their watch. Timers and animations may be started here.
    function onExitSleep() {
    }

    // Terminate any active timers and prepare for slow updates.
    function onEnterSleep() {
    }

    hidden function setDate() {
        var date = Gregorian.info(Time.now(), Time.FORMAT_LONG);
        var dateString = Lang.format("$1$ $2$", [date.day_of_week, date.day]);

        var dateDisplay = View.findDrawableById("DateLabel") as Text;
        dateDisplay.setColor(secondary);
        dateDisplay.setText(dateString);
    }

    hidden function setSteps() {
        var stepCount = ActivityMonitor.getInfo().steps.toString();
        var stepCountDisplay = View.findDrawableById("StepLabel") as Text;
        stepCountDisplay.setColor(secondary);

        stepCountDisplay.setText(stepCount);

        var stepIcon = View.findDrawableById("StepIcon") as Text; 
        stepIcon.setColor(secondary);
    }

    hidden function setBattery() {
        var battery = System.getSystemStats().battery;
        var batteryDisplay = View.findDrawableById("BatteryLabel") as Text;
        batteryDisplay.setColor(secondary);
        batteryDisplay.setText(battery.format("%d"));

        var batteryIcon = View.findDrawableById("BatteryIcon") as Text;
        batteryIcon.setColor(secondary);
        if (System.getSystemStats().charging) {
            batteryIcon.setText("c");
        } else if (battery > 90) {
            batteryIcon.setText("0");
        } else if (battery > 75) {
            batteryIcon.setText("1");
        } else if (battery > 40) {
            batteryIcon.setText("2");
        } else if (battery > 20) {
            batteryIcon.setText("3");
        } else {
            batteryIcon.setText("4");
        }
    }
}
