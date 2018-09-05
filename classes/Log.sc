//
// Log.sc
//
// Log message format: <timestamp>  <logType>  <signature> [-- <message>]
// <timestamp> is time since sclang began.
//
// Pass in caller context by setting <logContext> to "thisFunction."
//
// NB  Returns values as strings.  
//     In general, NEVER automatically post to console.
//
//
// PUBLIC METHODS--
//   *mark  *debug  *info  *error  *warning  *fatal
//   *assert
//   *msg
//
// PRIVATE METHODS--
//   *logit
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// DEPENDENCIES--
//   Date_MOS
//   Function_MOS
//   MOSErrorClasses
//
// THROWS--
//   MOSErrorAssert
//   MOSErrorFatal
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------

Log : MobileSound
{
  classvar classVersion = "0.3";   //RELEASE

  //------------------------ -o-
  *mosVersion { ^super.mosVersion(classVersion); }




  //--------------------------------------------------------------- -o-
  // Public class methods.

  //------------------------ -o-
  *mark  { |logContext, msg ...args|
    ^this.logit("MARK", logContext, msg, *args);
  }

  *debug  { |logContext, msg ...args|
    ^this.logit("DEBUG", logContext, msg, *args);
  }

  *info  { |logContext, msg ...args|
    ^this.logit("INFO", logContext, msg, *args);
  }

  *warning  { |logContext, msg ...args|
    ^this.logit("WARNING", logContext, msg, *args);
  }

  *error  { |logContext, msg ...args|
    ^this.logit("ERROR", logContext, msg, *args);
  }


  //------------------------ -o-
  *msg  { |msg ...args|
    ^this.logit(nil, "", msg, *args);
  }


  //------------------------ -o-
  *fatal  { |logContext, msg ...args|
    this.logit("FATAL", logContext, msg, *args);
    MOSErrorFatal.throw;
    ^'';
  }

  *assert  { | logContext, assertFunction
             |
    if ( assertFunction.().not, 
    {
      this.logit("ASSERT", logContext, assertFunction.cs);
      MOSErrorAssert.throw;
    });

    ^'';
  }




  //--------------------------------------------------------------- -o-
  // Private class methods.

  //------------------------ -o-
  // NB  args are for insertion into msg.
  //
  // NBXXX  Only Log.msg() passes in logType of nil.
  //
  *logit  { |   logType     = nil,
                logContext  = nil,
                msg         = nil
                ...args
            |

    var  logEntry   = "",
         signature  = "",
         separator  = " -- ";


    if (logType.notNil, { 
      logType = logType ++ "  "; 

      if (logContext.notNil, {
        signature = logContext.signature;
      });
    });


    //
    logEntry = ("%  %%")
                   .format(Date.elapsedTime, logType ?? "", signature ?? "");

    if (msg.notNil, { 
      if (signature.size <= 0, { separator = ""; });

      logEntry = logEntry ++ (separator ++ msg).format(*args); 
    });

    ^logEntry;
  }

} // Log

