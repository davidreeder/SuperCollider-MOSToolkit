//
// Synth_MOS.sc
//
//
// PUBLIC METHODS--
//   oscPath
//   roundSecondsToBlockSampleSize
//   secondsToFrames  [s2f]
//   framesToSeconds  [f2s]
//   bundleControls
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Log
//   Object_MOS
//   OSC
//   Parse
//   Z
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Synth  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Instance methods.

  //------------------------- -o-
  oscPath  { |oscToken|  ^OSC.synthPath(this, oscToken); }


  //------------------------- -o-
  roundSecondsToBlockSampleSize  { | seconds
                                   |
    ^Z.roundSecondsToBlockSampleSize(
         seconds, this.server, logContext:thisFunction);
  }


  //------------------------- -o-
  secondsToFrames  { |second|  
    ^Z.secondsToFrames(
         second, -1, this.server.sampleRate, logContext:thisFunction);
  }

  //
  framesToSeconds  { |frame|   
    ^Z.framesToSeconds(
         frame, -1, this.server.sampleRate, logContext:thisFunction);
  }


  //ALIAS
  s2f  { |second|  ^this.secondsToFrames(second); }  
  f2s  { |frame|   ^this.framesToSeconds(frame); }  


  //------------------------- -o-
  // ASSUME
  //   . args Array contains directives to Synth.set().
  //   . previousBundle is properly formed.
  //
  bundleControls  { | delayInSeconds,
                      previousBundle
                      ...args
                    |
    //
    if (Parse.isInstanceOfClass(thisFunction, 
                  delayInSeconds, "delayInSeconds", Number, 
                  isOkayToBeNil:true).not,              { ^nil; });

    if (delayInSeconds.notNil, {
      if (delayInSeconds < 0, 
      {
        Log.error(thisFunction, 
              "delayInSeconds MUST be GREATER THAN OR EQUAL to zero.  (%)",
                 delayInSeconds
            ).postln; 
                                                          ^nil;
      });
    });


    if (Parse.isInstanceOfArrayOfTypeWithSize(thisFunction, 
                  previousBundle, "previousBundle", Array, minSize:1, 
                  isOkayToBeNil:true).not,              { ^nil; });
                

    if (Parse.isInstanceOfClass(thisFunction, 
                  args, "...args", Array).not,          { ^nil; });

    if ((0 == args.size) || ((args.size % 2) != 0), {
      Log.error(thisFunction, 
               "...args input MUST consist of ONE OR MORE "
            ++ "key/value pairs for Synth.set().  (%)", args
          ).postln; 
                                                          ^nil; 
    });
                 

    //
    this.server.makeBundle( delayInSeconds, 
                            { this.set(*args); }, 
                            previousBundle
                );
  }


} //Synth

