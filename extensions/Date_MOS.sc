//
// Date_MOS.sc
//
//
// PUBLIC METHODS--
//   *datestamp
//   *hourMinuteSecond  [*hms]
//   *secondsToClocktime
//   *elapsedTime
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Log
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Date  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Class methods.

  //-------------- -o-
  *datestamp  { ^this.getDate.format("%Y%b%e,%H%M%S").removeWhitespace; }


  //-------------- -o-
  *hourMinuteSecond  { ^Date.localtime.format("%H:%M:%S").asString; }

  *hms  { ^this.hourMinuteSecond; }  //ALIAS


  //------------------------ -o-
  // DEFAULTS are for Log timestamp.
  //
  // NBXXX  BE CAREFUL when invoking Log in this method!
  //        Log also invokes this method causing infinite recursion.
  //
  *secondsToClocktime  { | seconds, 
                           printHours, printMilliseconds,
                           clearLeadingZero
                         |
    var  hours, minutes, secs, milliseconds,
         hoursString, minutesString, secsString, millisecondsString,
         str;

    var  zero  = $0;


    //
    if (seconds.isNil, { ^Log.error(thisFunction, "seconds is nil."); });

    //DEFAULTS.
    printHours         = printHours         ?? true;
    printMilliseconds  = printMilliseconds  ?? false;
    clearLeadingZero   = clearLeadingZero   ?? false;


    //
    hours         = seconds / 60 / 60;
    minutes       = (seconds / 60) % 60;
    secs          = seconds % 60;
    milliseconds  = (seconds - seconds.floor(1)) * 1000;

    hoursString         = hours        .floor(1).asString.padLeft(2, "0");
    minutesString       = minutes      .floor(1).asString.padLeft(2, "0");
    secsString          = secs         .floor(1).asString.padLeft(2, "0");
    millisecondsString  = milliseconds .floor(1).asString.padRight(3, "0");

    if (hours.floor(1) > 0, { printHours = true; });


    //
    if (printHours, {
      str = ("%:%:%").format(hoursString, minutesString, secsString);
    }, {
      str = ("%:%").format(minutesString, secsString);
    });

    if (printMilliseconds, {
      str = ("%.%").format(str, millisecondsString);
    });

    if (clearLeadingZero && (zero == str[0]), {
      var  strArray  = str.split(zero);
      strArray.removeAt(0);
      str = strArray.join(zero);
    });


    //
    ^str;
  }


  //------------------------ -o-
  *elapsedTime  { ^this.secondsToClocktime(Main.elapsedTime); }


} //Date

