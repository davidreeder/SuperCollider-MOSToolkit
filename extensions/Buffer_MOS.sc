//
// Buffer_MOS.sc
//
// See also BufferCache.
//
//
// SCLANG HAS NO CRITICAL SECTION
//   If many Buffers are loaded very quickly, BufferCache may not properly 
//   register all interactions.  This means that any given held Buffer
//   may not appear in BufferCache, though this discrepancy may be 
//   resolved by again calling BufferCache.storeOrReturnDuplicate() which
//   will also free the held buffer if it is not stored by BufferCache.
//
//
// PUBLIC METHODS--
//   *loadAsync  [*load]
//
//   seconds  frames
//   secondsToFrames  framesToSeconds  [s2f f2s]
//
//   roundSecondsToBlockSampleSize  
//
//   normalizedFrameIndex  [nfi]
//   frameIndexInSeconds  [fiis]
//
//   isEqualToByCharacteristics  [==]
//   isUndefined
//
//
// BUFFERCACHE METHODS--
//   storeOrReturnDuplicate
//   releaseBufferCacheEntry
//   findBufferCacheEntry 
//   channelArray 
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//   pretty
//
//
// MOS DEPENDENCIES--
//   BufferCache
//   Event_MOS
//   Log
//   Object_MOS
//   Parse
//   String_MOS
//   Z
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Buffer  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Class methods.

  //------------------------ -o-
  // Lookup previously loaded Buffer as an alternative to
  // loading anew, per tuple: (server, path, channelArray).  This
  // ASSUMES location, path and loaded channels uniquely identify a 
  // given Buffer.
  //
  // If further Buffer characteristics are required to determine if
  // it has been previously loaded and stored, this may be checked
  // after the new Buffer load is complete via
  // BufferCache.storeOrReturnDuplicate().  See BufferCache for more details.
  //
  // NB  completionFunction takes two arguments--
  //       logContext  thisFunction as found in class method .loadAsync()
  //       bufferObj   this Buffer
  //
  *loadAsync  { | path, 
                  channelArray,
                  server,
                  completionFunction,
                  verbose
                |
    var  cachedBuffer   = nil,
         readCallback   = nil;

    var  logMessageBody              = nil,
         logMessagePrefix            = "",
         logMessageGenerateFunction  = nil;

    //DEFAULTS.
    server       = server       ?? Server.default;
    verbose      = verbose      ?? true;


    if (Parse.isInstanceOfClass(thisFunction, 
                  path, "path", String).not,                    { ^nil; });

    if (channelArray.notNil, {
      if (BufferCache.sanityCheckChannelArray(
                        channelArray, thisFunction).not,        { ^nil; });
    });

    if (Parse.isInstanceOfClass(thisFunction, 
                  server, "server", Server).not,                { ^nil; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  completionFunction, "completionFunction", Function, 
                  isOkayToBeNil:true).not,                      { ^nil; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  verbose, "verbose", Boolean).not,             { ^nil; });


    //
    logMessageGenerateFunction = { | initialBody
                                   |
      logMessageBody = initialBody;

      if (channelArray.notNil, {
        logMessageBody = 
             ("%, with channels %").format(logMessageBody, channelArray);
      });

      logMessageBody ++ ".";
    };


    // Search for existing buffer per path (basename).
    //
    cachedBuffer = 
      BufferCache.searchForDuplicateByTuple(
                    path, channelArray, server, quiet:verbose.not);

    if (cachedBuffer.notNil, { 
      Log.info(thisFunction, 
            logMessageGenerateFunction.( 
                        ("CACHE CONTAINS MATCH FOR \"%\"").format(path) 
            )
          ).postln; 

      completionFunction.(thisFunction, cachedBuffer);

      ^cachedBuffer; 
    });


    // Read into a new buffer.
    //
    logMessageGenerateFunction.( (" LOAD \"%\"").format(path) );

    readCallback = { | bufferObj
                     |
      var  str  = "";

      if (bufferObj.isNil, {
        logMessagePrefix = "FAILED TO";

      }, {
        logMessagePrefix = "SUCCESSFUL";
        str = "\n" ++ bufferObj.pretty();

        //NB  Cannot .storeUniqueBuffer() because another 
        //      thread may have already done so...
        //
        BufferCache.storeOrReturnDuplicate(
                      bufferObj, channelArray, 
                      freeDuplicate:false, quiet:verbose.not
                   );
      });

      str = ("%%").format(logMessagePrefix ++ logMessageBody, str);

      if (verbose, { Log.info(thisFunction, str).postln; });


      //
      completionFunction.(thisFunction, bufferObj);

      '';
    };


    //
    if (channelArray.notNil, {
      ^Buffer.readChannel(
                server, path, channels:channelArray, action:readCallback);
    }, {
      ^Buffer.read(server, path, action:readCallback);
    });

  }  // *loadAsync


  //ALIAS
  *load  { | path, channelArray, server, 
             completionFunction,
             verbose
           | 
    ^this.loadAsync(
       path, channelArray, server, completionFunction, verbose);
  }




  //------------------------------------------- -o--
  // Instance methods.

  //------------------------ -o-
  // NB  Rounded results!
  //
  seconds  { ^( (this.numFrames ?? 0) / this.sampleRate).round(0.01); }

  //
  frames   { ^this.numFrames; }



  //------------------------ -o-
  secondsToFrames  { | second,
                       logContext
                     |  
    //DEFAULTS.
    if (logContext.isNil, { logContext = thisFunction; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  logContext, "logContext", Function).not, { ^''; });

    //
    ^Z.secondsToFrames( second, 
                        (this.numFrames / this.sampleRate),
                        this.sampleRate
                        , logContext:logContext
                      );
  }

  //
  framesToSeconds  { | frame,
                       logContext
                     |   
    //DEFAULTS.
    if (logContext.isNil, { logContext = thisFunction; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  logContext, "logContext", Function).not, { ^''; });

    //
    ^Z.framesToSeconds(
         frame, this.numFrames, this.sampleRate, logContext:logContext);
  }


  //ALIAS
  s2f  { |second, logContext|  ^this.secondsToFrames(second, logContext); }  
  f2s  { |frame, logContext|   ^this.framesToSeconds(frame, logContext); }  



  //------------------------ -o-
  roundSecondsToBlockSampleSize  { | seconds,
                                     logContext
                                   |
    //DEFAULTS.
    if (logContext.isNil, { logContext = thisFunction; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  logContext, "logContext", Function).not, { ^''; });

    //
    ^Z.roundSecondsToBlockSampleSize(
                       seconds, this.server, logContext:logContext);
  }



  //------------------------ -o-
  normalizedFrameIndex  { | index
                          |
    //
    if (Parse.isInstanceOfClass(
              thisFunction, index, "index", Number).not, { ^nil; });

    if ((index < 0) || (index > 1), {
      Log.error( thisFunction, 
                 "index MUST BE in range [0.0, 1.0].  (%)", index
               ).postln; 
      ^nil;
    });

    //
    ^(this.numFrames * index);
  }


  //ALIAS
  nfi  { |index|  ^this.normalizedFrameIndex(index); }



  //------------------------ -o-
  // RETURNS  Frame index at seconds.
  //          If seconds is negative, seek backwards from last frame.
  //
  frameIndexInSeconds  { | seconds
                         |
    var  secondsMax  = this.framesToSeconds(
                              this.numFrames, logContext:thisFunction),
         isNegative,
         secondsAbs;

    //
    if (Parse.isInstanceOfClass(
              thisFunction, seconds, "seconds", Number).not, { ^nil; });

    isNegative = (seconds < 0);
    secondsAbs = seconds.abs;

    if (secondsAbs > secondsMax, 
    {
      Log.error( thisFunction, 
                 "Absolute value of seconds CANNOT BE GREATER "
                    ++ "than buffer size in seconds.  (%)", secondsMax
               ).postln; 
      ^nil;
    });


    //
    if (isNegative, {
      ^this.secondsToFrames(secondsMax + seconds);
    }, {
      ^this.secondsToFrames(seconds);
    });
  }


  //ALIAS
  fiis  { |seconds|  ^this.frameIndexInSeconds(seconds); }



  //------------------------ -o-
  isEqualToByCharacteristics  { | otherBuffer, verbose
                                | 
    var  bceThis, bceOther;


    //DEFAULTS.
    verbose = verbose ?? false;

    if (Parse.isInstanceOfClass(thisFunction, 
          otherBuffer, "otherBuffer", Buffer, isOkayToBeNil:false).not, 
                                                        { ^false; });
    if (Parse.isInstanceOfClass(thisFunction, 
          verbose, "verbose", Boolean).not,             { ^false; });


    //
    if (this.bufnum == otherBuffer.bufnum, { ^true; });

    if (    (this.server.name    != otherBuffer.server.name)
         || (this.path.basename  != otherBuffer.path.basename)
         || (this.numChannels    != otherBuffer.numChannels)
         || (this.numFrames      != otherBuffer.numFrames)
         || (this.sampleRate     != otherBuffer.sampleRate),  { ^false; });


    //
    bceThis   = this.findBufferCacheEntry();
    bceOther  = otherBuffer.findBufferCacheEntry();

    if (bceThis.notNil && bceOther.notNil, 
    {
      if (    bceThis[BufferCache.tokenChannelArray] 
           == bceOther[BufferCache.tokenChannelArray], { ^true; });

      ^false;

    }, {
      Log.warning(thisFunction, 
               "Buffer comparison TRUE WITHOUT considering channelArray."
            ++ "  (this=%  other=%)", 
                 this.path.basename, otherBuffer.path.basename
         ).postln; 
    });


    //
    ^true;
  }


  //ALIAS.
  ==  { |otherBuffer|  ^this.isEqualToByCharacteristics(otherBuffer); }


  //------------------------ -o-
  isUndefined  
  {
    if (    this.numChannels.isNil
         || this.numFrames.isNil
         || this.sampleRate.isNil
         || this.path.isNil,    { ^true; });

    ^false;
  }




  //---------------------------------------------------------- -o--
  // BufferCache methods.

  //------------------------ -o-
  storeOrReturnDuplicate  { | channelArray, quiet
                            |
    ^BufferCache.storeOrReturnDuplicate(this, channelArray, quiet);
  }


  //------------------------ -o-
  releaseBufferCacheEntry  { ^BufferCache.releaseBuffer(this, this.channelArray); }


  //------------------------ -o-
  findBufferCacheEntry  { ^BufferCache.findBufferEntry(this); }


  //------------------------ -o-
  channelArray  { 
    var  entry  = this.findBufferCacheEntry;
    
    if (entry.notNil, { ^entry.channelArray; });

    ^nil;
  }




  //---------------------------------------------------------- -o--
  // MOS protocol methods.

  //------------------------ -o-
  pretty  {     | pattern, elisionLength, depth,
                  casei, compileString, indent, initialCallee, 
                  enumerate, bulletIndex, enumerationOffset,
                  minimumSpacerSize, nolog, shortVersion
                |
    var  title  = this.classname.toUpper,
         filename;

    var  maxKeyLength  = "numChannels".size;

    var  enviroA1, enviroA2, enviroA3, enviroA4,
         enviroArray = [];  // ADD


    //
    case
      { this.isUndefined; }
          { filename = this.labelUndefined; }

      { this.path.notNil; }
          { filename = this.path.basename; }
      
    ; // endcase

    title = ("%  %").format(title, filename ?? "(no filename)");


    //
    if (this.isUndefined.not, {
      enviroA1 = (
        bufnum:       this.bufnum,

        numChannels:  this.numChannels  ?? "nil",
        numFrames:    this.numFrames    ?? "nil",

        sampleRate:   this.sampleRate,
      );

      enviroA2 = (
        duration:     ("% (%)")
                          .format( this.seconds.roundPadRight(0.001),
                                   Date.secondsToClocktime(
                                          this.seconds,
                                          printHours:         false,
                                          printMilliseconds:  true,
                                          clearLeadingZero:   true
                                        )
                                 ),
      );

      enviroA3 = (
        server:       this.server.name,
      );

      enviroA4 = (
        path:         this.path,
      );

      enviroArray = [ enviroA1, enviroA2, enviroA3, enviroA4 ];
    });


    //
    ^this.prettyLocal(
            enviroArray, title,
            pattern, elisionLength, depth,
              casei, compileString, indent, initialCallee, 
            enumerate, bulletIndex, enumerationOffset, 
            minimumSpacerSize:maxKeyLength, 
              nolog:nolog, shortVersion:shortVersion
          );
  }


} //Buffer

