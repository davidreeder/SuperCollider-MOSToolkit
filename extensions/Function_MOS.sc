//
// Function_MOS.sc
//
//
// METHODS--
//   signature
//   methodname
//
//   systemSchedule  systemScheduleAbsolute  
//   appSchedule             
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//   sanityCheckTimeValue
//
//
// MOS DEPENDENCIES--
//   Log
//   Object_MOS
//   Parse
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Function  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.2";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Instance methods.

  //---------------------- -o-
  signature  { | 
                 showArgs    = false
                 //showArgs    = true
               |
    var  signature  = nil,
         tokens,
         argsArray  = nil,
         args       = "";


    //
    signature  = this.def.asString.replace("Meta_", "").replace(":", ".");
    tokens     = signature.split($ );

    if (tokens.size > 1, {
      signature = tokens[tokens.size - 2];  //XXX
    });
        

    //
    argsArray = this.argNames.copy;

    if ( argsArray.size > 0, {
      argsArray.removeAt(0);

      if ( showArgs, {
        argsArray.do { |item|  args = args ++ ("%,").format(item); };
        args = args.drop(-1);
      }, {
        argsArray.do { args = args ++ ":"; };
      });
    });


    //
    ^("%(%)").format(signature, args);
  }


  //---------------------- -o-
  methodname  { | preserveUnderscore=false
                |
    var  methname  = this.signature.split($.)[1];

    if (methname.size <= 0, { ^this.labelUndefined; });

    methname = methname.split($()[0];

    if (preserveUnderscore.not, { methname = methname.replace("_", ""); });

    //
    ^methname;
  }



  //---------------------- -o-
  systemSchedule  { | time
                    |  
    time = this.sanityCheckTimeValue(time, thisFunction);

    ^SystemClock.sched(time, this); 
  }

  systemScheduleAbsolute  { | time
                            |  
    time = this.sanityCheckTimeValue(time, thisFunction);

    ^SystemClock.schedAbs(time, this); 
  }

  appSchedule  { | time
                 | 
    time = this.sanityCheckTimeValue(time, thisFunction);

    ^AppClock.sched(time, this); 
  }




  //------------------------------------------- -o--
  // Support methods.

  //---------------------- -o-
  // RETURN:  time if value; otherwise nil.
  //
  sanityCheckTimeValue  { | time, logContext
                          |
    //DEFAULTS.
    time = time ?? 0.0;

    if (Parse.isInstanceOfClass(thisFunction, 
                  logContext, "logContext", Function).not, { ^nil; });

    if (Parse.isInstanceOfClass(logContext, 
                  time, "time", Number).not, { ^nil; });

    if (time < 0, {
      Log.error(logContext, 
                  "time MUST BE GREATER OR EQUAL to zero.  (%)", time).postln; 
      ^nil;
    });


    //
    ^time;
  }


} //Function

