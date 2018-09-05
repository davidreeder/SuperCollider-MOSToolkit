//
// Routine_MOS.sc
//
//
// PUBLIC METHODS--
//   *spinlock
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Log
//   MOSErrorClasses
//   Object_MOS
//   Parse
//
// THROWS--
//   MOSErrorSpinlockTimeout
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Routine {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Class methods.

  //------------------------ -o-
  // Wait testOnsetWait seconds then...
  // ...spinlock by polling testFunction every testIterationWait seconds.
  //
  // spinlock stops when testFunction returns true
  //   -OR- when testIterationWaitMax expires.
  //
  // RETURN  accruedWaitTime 
  //         OR -(testIterationWaitMax) if spinlock times out.
  //
  // NB  testIterationWaitMax is approximate, compared against sum of all 
  //     testIterationWait periods.  It is not a separate timer; it does 
  //     not account for time passed, including time spent in testFunction.
  //
  // NB  Only works within a Routine.
  //     testFunction MUST return true/false.
  //
  //
  // NBXXX  Seems that testFunctionString must be spelled out
  //        independently from testFunction.  Using .cs() or .compile()
  //        to reuse a variable name confuses the logic.
  //
  *spinlock  { | testFunction,                          //NEEDS FORK
                 testOnsetWait         = 0.000, 
                 testIterationWait     = 0.100, 
                 testIterationWaitMax  = 5.000, 
                 tag,  
                 testFunctionString,
                 disallowExceptionError,
                 logContext,
                 verbose
               |
    var  str,
         accruedWaitTime  = 0,
         returnTime,
         timeoutString    = " TIMEOUT",
         verboseWaitMessageFunction;

    //DEFAULTS.
    tag                 = tag                     ?? "";
    verbose             = verbose                 ?? false;
    testFunctionString  = testFunctionString      ?? testFunction.cs;
    disallowExceptionError 
                        = disallowExceptionError  ?? false;

    //NBXXX  Using "= ??" syntax to assign default of logContext
    //       somehow causes tight, infinite loop...
    //
    if (logContext.isNil, { logContext = thisFunction; });


    //
    if (Parse.isInstanceOfClass(thisFunction, 
          testFunction, "testFunction", Function).not,            { ^''; });

    if (Parse.isInstanceOfClass(thisFunction, 
          testFunctionString, "testFunctionString", String).not,  { ^''; });

    if (Parse.isInstanceOfClass(thisFunction, 
          testOnsetWait, "testOnsetWait", Number).not             { ^''; });

    if (Parse.isInstanceOfClass(thisFunction, 
          testIterationWait, "testIterationWait", Number).not     { ^''; });

    if (Parse.isInstanceOfClass(thisFunction, 
          testIterationWaitMax, "testIterationWaitMax", Number).not     
                                                                  { ^''; });
    if (Parse.isInstanceOfClass(thisFunction, 
          tag, "tag", String).not                                 { ^''; });

    if (Parse.isInstanceOfClass(thisFunction, 
          disallowExceptionError, "disallowExceptionError", Boolean).not                                 
                                                                  { ^''; });
    if (Parse.isInstanceOfClass(thisFunction, 
          logContext, "logContext", Function).not                 { ^''; });

    if (Parse.isInstanceOfClass(thisFunction, 
          verbose, "verbose", Boolean).not                        { ^''; });


    if (testIterationWait >= testIterationWaitMax, {
      ^Log.error( thisFunction, 
                  "testIterationWait (%) MUST BE "
                     ++ "less than testIterationWaitMax (%).",
                       testIterationWait, testIterationWaitMax
                ).postln; 
    });


    //
    if (tag.size > 0, { tag = ":: " ++ tag; });

    verboseWaitMessageFunction = { | accruedWaitTime
                                   |
      ("WAITING FOR %  :: onset=% iteration=% max=% accrued=%  %")
          .format(
             testFunctionString.pr(elisionLength:31),   //XXX
             testOnsetWait.roundPadRight(0.001),
             testIterationWait.roundPadRight(0.001),
             testIterationWaitMax.roundPadRight(0.001),
             accruedWaitTime.roundPadRight(0.001),
             tag,
           );
    };


    //
    if (verbose, { 
      Log.info(logContext, 
            "BEGIN  %", verboseWaitMessageFunction.(accruedWaitTime)).postln; 
    });

    testOnsetWait.wait;


    while (    testFunction.not
            && { accruedWaitTime < testIterationWaitMax; }, 
    { 
      testIterationWait.wait; 
      accruedWaitTime = accruedWaitTime + testIterationWait;

      if (verbose, { 
        Log.info(logContext, 
              "...%", verboseWaitMessageFunction.(accruedWaitTime)).postln; 
      });
    });


    if (accruedWaitTime < testIterationWaitMax, 
    { 
      timeoutString = ""; 
      returnTime = accruedWaitTime;
    }, {
      returnTime = testIterationWaitMax.neg;
    });

    if (verbose, { 
      Log.info( logContext, 
                "END%  %", timeoutString, 
                             verboseWaitMessageFunction.(accruedWaitTime)
              ).postln; 
    });


    //
    if ( (returnTime < 0) && disallowExceptionError.not, { 
      MOSErrorSpinlockTimeout.throw; 
    });

    ^returnTime;
  }


} //Routine

