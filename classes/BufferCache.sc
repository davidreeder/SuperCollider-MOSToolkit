//
// BufferCache.sc
//
// Store "unique" allocated Buffers.
//
// INTUITION--
//   . Each server has its own BufferCache namespace, by definition;
//   . Each Buffer has a (unique) name;
//   . Any given Buffer may have multiple channels and therefore could 
//       be loaded with as many different channelArray combinations;
//   . Each Buffer load generates a new bufnum.
//   . So... buffers are indexed by server-name-bufnum and contain
//       the Buffer itself, the channelArray configuration which which
//       it was loaded, and a reference count.
//
//
// Define Buffers to be equivalent if the following elements are equal:
//   . server name
//   . path BASENAME
//   . numChannels
//   . numFrames
//   . sampleSize
//   . (if possible) channelArray (identical size, order, elements)
//
//   NB  channelArray documents which channels were read from the
//       original sample file.  buffer.numChannels represents the number of
//       channels read from the original file, which may be smaller than
//       the actual number of channels available in the original file.
//       Consequently: channelArray.size <= originalBuffer.numChannels,
//       whereas, channelArray unsigned integer elements may 
//       be > readBuffer.numChannels.
//
//       Also, though individual elements of channelArray may be greater
//       or equal to numChannels, the size of channelArray MUST equal
//       numChannels.
//
//       When channelArray=nil it will be expanded to (0..(numChannels-1)),
//       creating a contiguous list of channels starting at zero.  This 
//       implies the all the channels of the associated buffer have been 
//       read and their order has been preserved.
//
//       Consequently, the developer MUST REMEMBER to pair the same 
//       channelArray metadata with Buffer.storeOrReturnDuplicate() 
//       as they did with Buffer.load() in order to accurately save 
//       or search for a given buffer instance.  (Buffer.loadAsync() 
//       does this automatically.)
//
//
// Buffers are determined to be EQUAL BASED UPON THEIR CHARACTERISTICS,
// NOT upon their array contents.  This is reasonable if all Buffers 
// in use have unique basenames and associated channelArrays.
//
//
// Use of BufferCache is a convenience -- it is not mandatory. 
// Its use must be requested via BufferCache or classes that rely upon it. 
// (Buffer extention methods ASSUME reliance upon BufferCache.)
//
// BufferCache ASSUMES the input buffer is FINISHED LOADING.
//
//
// NB  Preventing memory leaks in fast threaded environment...
// 
// Regarding interaction between Buffer and BufferCache:
//   1 Search fails and buffer is loaded, returned to caller;
//   2 Callback function tries to store, but finds it listed already by name;
//   3 BufferCache is sound because reference count is incremented, duplicate
//       buffer is not deleted in this case;
//   4 When releasing this buffer, if found buffer is not identical, then
//       after decrementing refernce count, input buffer is also released.
//
//
// SCHEMA for bufferDictionary--
//   entry = bufferDictionary[ SERVER ][ BUFFER_BASENAME ][ BUFFER_BUFNUM ]
//   buffer                = entry[ BUFFER ]
//   bufferChannelArray    = entry[ CHANNELARRAY ]
//   bufferReferenceCount  = entry[ REFERENCECOUNT ]
//
//
// PUBLIC METHODS--
//   *clear
//   
//   *searchForDuplicateByBuffer
//   *searchForDuplicateByTuple
//   *storeOrReturnDuplicate
//   *storeUniqueBuffer
//   *releaseBuffer
//   *findBufferEntry
//
//
// HELPER METHODS--
//   *isBufferUndefined
//   *sanityCheckChannelArray
//   *sanityCheckBufferAndChannelArray
//
//
// PRIVATE METHODS--
//   *searchForDuplicate
//
//
// MOS PROTOCOL SUPPORT--
//   *demo
//   *mosVersion
//   *pretty
//
//
//
// DEPENDENCIES--
//   Buffer
//   Log
//   Object
//   Parse
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


BufferCache : MobileSound
{
  classvar  classVersion = "0.1";   //RELEASE


  // Class variables.
  //
  classvar  <  bufferDictionary,
            <  bd;                 //ALIAS.

  classvar  <  tokenChannelArray,
            <  tokenBuffer,
            <  tokenReferenceCount;




  //---------------------------------------------------------- -o--
  *mosVersion { ^super.mosVersion(classVersion); }
                



  //---------------------------------------------------------- -o--
  // Lifecycle, constructors, class memory.

  //------------------------ -o-
  *initClass  
  {
    this.clear();

    //XXX
    tokenChannelArray    = \channelArray;
    tokenBuffer          = \buffer;
    tokenReferenceCount  = \referenceCount;

  } // *initClass


  //------------------------ -o-
  *clear
  {
    if (bufferDictionary.size > 0, 
    { 
      bufferDictionary.do({ | serverIndex
                            |
        if (serverIndex.size > 0, 
        {
          serverIndex.do({ | basenameIndex
                           |
            if (basenameIndex.size > 0, 
            {
              basenameIndex.do({ | bufnumIndex
                                 |
                bufnumIndex[tokenBuffer].free;
                bufnumIndex[tokenBuffer] = nil;

                bufnumIndex[tokenChannelArray].free;
                bufnumIndex[tokenChannelArray] = nil;

                bufnumIndex[tokenReferenceCount] = 0;

                basenameIndex[bufnumIndex] = nil;
              });
            });   // endif -- basenameIndex.size

            serverIndex[basenameIndex] = nil;
          });
        });   // endif -- serverIndex.size

        bufferDictionary[serverIndex] = nil;
      });
    });   // endif -- bufferDictionary.size


    //
    bufferDictionary = ();
    bd = bufferDictionary;   //ALIAS


    //
    ^this;
  }




  //---------------------------------------------------------- -o--
  // Public class methods.

  //------------------------ -o-
  *searchForDuplicateByTuple  { | path, channelArray, server,
                                  incrementReferenceCount,
                                  quiet
                                |
    ^this.searchForDuplicate(nil, channelArray, path, server, incrementReferenceCount, quiet);
  }

  //
  *searchForDuplicateByBuffer  { | buffer, channelArray,
                                   incrementReferenceCount,
                                   quiet
                                 |
    ^this.searchForDuplicate(buffer, channelArray, nil, nil, incrementReferenceCount, quiet);
  }


  //------------------------ -o-
  // NB  Always get rid of the new duplicate.  
  //     The previously existing Buffer may already be in use.
  //
  *storeOrReturnDuplicate  { | buffer, channelArray,
                               freeDuplicate,
                               quiet
                             |
    var  foundBuffer,
         isSameBuffer,
         currentReferenceCount;


    //DEFAULTS.
    quiet          = quiet          ?? true;
    freeDuplicate  = freeDuplicate  ?? true;


    if (this.sanityCheckBufferAndChannelArray(
                          buffer, channelArray, thisFunction).not,  { ^nil; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  freeDuplicate, "freeDuplicate", Boolean).not,   { ^nil; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  quiet, "quiet", Boolean).not,   { ^nil; });


    //
    foundBuffer = this.searchForDuplicateByBuffer(buffer, channelArray, incrementReferenceCount:false, quiet:quiet);

    if (foundBuffer.notNil, 
    { 
      //NB  Generally only happens if an attempt is made to 
      //      store a buffer that has already been stored.
      //
      isSameBuffer = (buffer === foundBuffer);

      if (isSameBuffer.not, {
        Log.info(thisFunction, 
              "FOUND DUPLICATE.  "
                 ++ "FREEING NEW Buffer \"%\", bufnum=%, channelArray=%.", 
                 buffer.path.basename, buffer.bufnum, channelArray
            ).postln; 

        if (freeDuplicate, {
          buffer.free;
          buffer = nil;
        });
      });

      //
      ^foundBuffer;
    });


    //
    this.storeUniqueBuffer(buffer, channelArray, quiet);

    ^buffer;
  }


  //------------------------ -o-
  // RETURN  true on success; false otherwise.
  //
  // NB  Does not search.  Pre-existing buffer entry is an error.
  //
  *storeUniqueBuffer  { | buffer, channelArray,
                          quiet
                        |
    var  serverIndex    = nil,
         basename,
         basenameIndex  = nil,
         bufnumIndex    = nil;


    //DEFAULTS.
    quiet = quiet ?? false;

    if (this.sanityCheckBufferAndChannelArray(
                          buffer, channelArray, thisFunction).not,  { ^nil; });

    channelArray = channelArray ?? (0..(buffer.numChannels - 1));



    //
    serverIndex = bufferDictionary[buffer.server.name];
    if (serverIndex.isNil, { 
      bufferDictionary[buffer.server.name] = (); 
      serverIndex = bufferDictionary[buffer.server.name];
    });


    basename       = buffer.path.basename;
    basenameIndex  = serverIndex[basename.asSymbol];
    if (basenameIndex.isNil, { 
      serverIndex[basename.asSymbol] = (); 
      basenameIndex = serverIndex[basename.asSymbol];
    });


    bufnumIndex = basenameIndex[buffer.bufnum];

    if (bufnumIndex.notNil, 
    {
      if (quiet.not, {
        Log.error( thisFunction, 
                   "ENTRY ALREADY EXISTS for Buffer "
                      ++ "\"%\", bufnum=%, channelArray=%.", 
                      basename, buffer.bufnum, bufnumIndex[tokenChannelArray]
            ).postln; 

        if (channelArray != bufnumIndex[tokenChannelArray], {
          Log.error(thisFunction, "Pre-existing Buffer HAS DIFFERENT CHANNEL ARRAY."
                      ++ "  (existing=%  requested=%)",
                      bufnumIndex[tokenChannelArray], channelArray
                   ).postln; 
        });
      });

      ^false;
    });

    basenameIndex[buffer.bufnum] = ();
    bufnumIndex = basenameIndex[buffer.bufnum];

    bufnumIndex[tokenBuffer]          = buffer;
    bufnumIndex[tokenChannelArray]    = channelArray;
    bufnumIndex[tokenReferenceCount]  = 1;

    if (quiet.not, {
      Log.info(  thisFunction, 
                 "STORING Buffer \"%\", bufnum=%, channelArray=%.", 
                    basename, buffer.bufnum, channelArray
          ).postln;
    });


    //
    ^true;
  }


  //------------------------ -o-
  // RETURN Boolean:  
  //   true   Decrement reference count, possibly removing buffer;
  //   false  Otherwise.
  //
  // Search for buffer.  
  //   If there is a match, decrement/free the result.
  //   If the result is not identical with the input, then also free the input.
  //
  *releaseBuffer  { | buffer, channelArray
                    |
    var  foundBuffer  = nil,

         foundBufnum,
         foundChannelArray,
         foundReferenceCount,

         basename,
         serverIndex,
         basenameIndex,
         bufnumIndex,

         inputBufferEqualToFoundBuffer,

         actionLabel  = "DECREMENTED REFERENCE COUNT for";


    //
    if (Parse.isInstanceOfClass(thisFunction, 
                buffer, "buffer", Buffer).not,  { ^false; });

    if (buffer.isUndefined(), { 
      Log.error(thisFunction, "buffer is UNDEFINED.").postln; 
      ^false; 
    });

    //NB  channelArray parsed by this.searchForDuplicateByBuffer().


    //
    foundBuffer = this.searchForDuplicateByBuffer(buffer, channelArray, incrementReferenceCount:false);


    //
    basename     = foundBuffer.path.basename;
    serverIndex  = bufferDictionary[buffer.server.name];

    if (serverIndex.notNil, 
    {
      basenameIndex = serverIndex[basename.asSymbol];

      if (basenameIndex.notNil, 
      {
        bufnumIndex = basenameIndex[foundBuffer.bufnum];

        if (bufnumIndex.isNil, {
          Log.error(thisFunction, 
                         "FAILED TO FIND bufnumIndex.  "
                      ++ "SHOULD NOT FAIL for local search..."
                   ).postln; 
 
          ^false;
        });

        foundBufnum          = bufnumIndex[tokenBuffer].bufnum;
        foundChannelArray    = bufnumIndex[tokenChannelArray];
        foundReferenceCount  = bufnumIndex[tokenReferenceCount] - 1;

        inputBufferEqualToFoundBuffer = (buffer === bufnumIndex[tokenBuffer]);


        //
        if (foundReferenceCount <= 0, {
          actionLabel = "REMOVED";

          bufnumIndex[tokenBuffer].free;
          bufnumIndex[tokenBuffer] = nil;
          bufnumIndex[tokenChannelArray].free;
          bufnumIndex[tokenChannelArray] = nil;
          bufnumIndex[tokenReferenceCount] = 0;

          basenameIndex[foundBufnum] = nil;
        
        }, {
          bufnumIndex[tokenReferenceCount] = foundReferenceCount;
        });


        //
        Log.info(thisFunction, 
          "% Buffer \"%\", bufnum=%, channelArray=%.", 
             actionLabel, 
             basename, foundBufnum, foundChannelArray
        ).postln; 

        if (inputBufferEqualToFoundBuffer.not, {
          Log.warning(thisFunction, 
                           "Input buffer (bufnum=%) NOT EQUAL to "
                        ++ "found buffer (bufnum=%).  "
                        ++ "FREEING INPUT BUFFER.",
                              buffer.bufnum, foundBufnum
                     ).postln; 

          buffer.free;
          buffer = nil;
        });

        ^true;

      });  // endif -- basenameIndex.notNil
    });  // endif -- serverIndex.notNil


    //
    ^false;
  }


  //------------------------ -o-
  // RETURN: Entry for existing Buffer  -OR-  nil.
  //
  *findBufferEntry  { | buffer
                      |
    var  serverIndex,
         basenameIndex,
         bufnumIndex;

    //
    if (Parse.isInstanceOfClass(thisFunction, 
                  buffer, "buffer", Buffer).not, { ^nil; });

    if (this.isBufferUndefined(buffer),          { ^nil; });


    //
    serverIndex = bufferDictionary[buffer.server.name];

    if (serverIndex.notNil, 
    {
      basenameIndex = serverIndex[buffer.path.basename.asSymbol];

      if (basenameIndex.notNil, 
      {
        bufnumIndex = basenameIndex[buffer.bufnum];

        if (bufnumIndex.notNil, { ^bufnumIndex; });
      });
    });


    //
    ^nil;
  }


  //---------------------------------------------------------- -o--
  // Helper class methods.

  //------------------------ -o-
  // NB  Upon free, Buffer.server is the only field defined.
  //     Not enough to warrant "incomplete" status.
  //
  *isBufferUndefined  { | buffer                //LOGS
                        |
     var  characterization  = nil,
          basename          = nil;


     //
     if (Parse.isInstanceOfClass(thisFunction, 
                   buffer, "buffer", Buffer).not,       { ^true; });


     //
     case
       { buffer.isNil; }
           { characterization = "nil"; }

       { buffer.isUndefined(); }
           { 
             characterization = "UNDEFINED";

             if (buffer.path.notNil, { 
               characterization  = "INCOMPLETE";
               basename          = buffer.path.basename; 
             }); 
           }

     ; //endcase


     //
     if (characterization.notNil, 
     {
       if (basename.size > 0, { basename = ("(%)").format(basename); });

       Log.error(thisFunction, 
             "Buffer is %.  %", characterization, basename).postln; 

       ^true;
     });

                          
     //
     ^false;
  }


  //------------------------ -o-
  // RETURN: true if valid, otherwise false.
  //
  *sanityCheckChannelArray  { | channelArray, logContext        //LOGS
                              |                        
    var  channelArrayValidator;


    //
    if (Parse.isInstanceOfClass(thisFunction, 
          logContext, "logContext", Function).not,  { ^false; });

    //
    channelArrayValidator = { | logContext, 
                                value, valueName, 
                                classRequirement,
                                validatorContextArray
                              |
        // NB  No info about number of channels in Buffer.
        //
        var  rval  = (value.isInteger && (value >= 0));

        if (rval.not, {
          Log.error(thisFunction, 
            "% contains ELEMENTS OUT OF RANGE.  (%)", valueName, value).postln; 
        });

        rval;
      }.cs;

    if (Parse.isInstanceOfArrayOfTypeWithSize(logContext, 
                  channelArray, "channelArray", Integer, 
                  validatorFunctionString:channelArrayValidator,
                  minSize:1
              ).not,                            { ^false });

    //
    ^true;
  }


  //------------------------ -o-
  *sanityCheckBufferAndChannelArray  { | buffer, channelArray,       //LOGS.
                                         logContext
                                       |                        
    //
    if (Parse.isInstanceOfClass(thisFunction, 
                  logContext, "logContext", Function).not,  { ^false; });

    if (Parse.isInstanceOfClass(logContext, 
                  buffer, "buffer", Buffer).not,            { ^false; });

    if (this.isBufferUndefined(buffer),                     { ^false; });


    //
    if (channelArray.isNil, {
      channelArray = channelArray ?? (0..(buffer.numChannels - 1));

    }, {
      if (channelArray.size != buffer.numChannels, {
        Log.error(logContext, 
                    "channelArray.size (%) NOT EQUAL to buffer.numChannels (%).",
                    channelArray.size, buffer.numChannels
                 ).postln; 

        ^false;
      });
    });


    //
    ^this.sanityCheckChannelArray(channelArray, logContext);
  }




  //---------------------------------------------------------- -o--
  // Private class methods.

  //------------------------ -o-
  // INPUTS: buffer and channelArray.
  //         path and server may substitute for buffer.
  //
  // RETURN: matching Buffer  -OR-  nil.
  //
  //
  // ASSUME  Identical index indicates identical buffer.
  //           (See bufferDictionary SCHEMA.)
  // ASSUME  buffer is finished loading.  Otherwise, this method should fail.
  //
  // NB  channelArray is always defined when buffer is defined.
  // NB  This is a private method.
  // NB  path is input, but only path.basename is used.
  //
  *searchForDuplicate  { | buffer, channelArray,                //LOGS
                           path, server,
                           incrementReferenceCount,
                           quiet
                         |
    var  foundBuffer        = nil,
         foundBufnumIndex   = nil,
         foundChannelArray  = nil,
         identical          = "",
         basename,
         serverIndex,
         basenameIndex,
         bufnumIndex;


    //DEFAULTS.
    quiet                    = quiet                    ?? false;
    incrementReferenceCount  = incrementReferenceCount  ?? true;



    // Sanity checks.
    //
    if (buffer.notNil, 
    {
      if (this.sanityCheckBufferAndChannelArray(
                 buffer, channelArray, thisFunction).not,  { ^nil; });

      if (path.notNil || server.notNil, {
        Log.error( thisFunction, 
                   "path and server MUST BE nil if buffer is defined.  "
                      ++ "(path=%  server=%)", path, server.name
            ).postln; 

        ^nil; 
      });

      channelArray = channelArray ?? (0..(buffer.numChannels - 1));

      path    = buffer.path;
      server  = buffer.server;

    }, {
      if (channelArray.notNil, {
        if (this.sanityCheckChannelArray(channelArray, thisFunction).not,
                                                          { ^nil; });
      });

      if (Parse.isInstanceOfClass(thisFunction, path, "path", String).not, 
                                                          { ^nil; });

      if (Parse.isInstanceOfClass(thisFunction, server, "server", Server).not, 
                                                          { ^nil; });
    });


    if (Parse.isInstanceOfClass(thisFunction, 
          incrementReferenceCount, "incrementReferenceCount", Boolean).not, 
                                                          { ^nil; });
    if (Parse.isInstanceOfClass(thisFunction, quiet, "quiet", Boolean).not, 
                                                          { ^nil; });


    //
    if (bufferDictionary.size <= 0, { ^nil; });

    basename = path.basename;

    serverIndex = bufferDictionary[server.name];
    if (serverIndex.isNil, { ^nil; });

    basenameIndex = serverIndex[basename.asSymbol];
    if (basenameIndex.isNil, { ^nil; });


    block  { | break
             |
      // Does buffer.bufnum already exist?  Does it match channelArray?
      //
      if (buffer.notNil, {
        bufnumIndex = basenameIndex[buffer.bufnum];

        if (bufnumIndex.notNil, 
        {
          // NB  The Array "==" operator accounts for size, order and elements.
          //
          if (channelArray == bufnumIndex[tokenChannelArray], 
          {
            foundBufnumIndex = bufnumIndex;

          }, {
            if (quiet.not, 
            {
              Log.error(thisFunction, 
                "channelArray MISMATCH WITH IDENTICAL Buffer "
                   ++ "\"%\", bufnum=%, "
                   ++ "bufferChannelArray=%, channelArrayRequest=%.", 
                   basename, buffer.bufnum, 
                     bufnumIndex[tokenChannelArray], channelArray
              ).postln; 
            });

            ^nil;
          });

          break.();

        }); // endif -- bufnumIndex.notNil
      });


      // Otherwise search through all buffers with the same name.
      // Handles cases where...
      //   . Buffer names are not unique;
      //   . When race conditions result in a single Buffer loaded twice,
      //       registered by a bufnum different from the initial search,
      //       but still available by name.
      //
      // Find an exact match if channelArray is defined,
      //   ELSE take the first one with the most channels.
      //   NB  This choice is totally arbitray.
      //
      basenameIndex.do({ | bufnumIndex
                         |
        var  currentBuffer        = bufnumIndex[tokenBuffer],
             currentChannelArray  = bufnumIndex[tokenChannelArray];

        //
        if (channelArray.notNil, {
          if (channelArray == currentChannelArray,
          {
            foundBufnumIndex = bufnumIndex;
            break.();
          });

        }, {
          var  captureThisOne  = false;

          case
            { foundBufnumIndex.isNil; }
                { captureThisOne = true; }

            { foundBufnumIndex[tokenBuffer].numChannels < currentBuffer.numChannels; }
                { captureThisOne = true; }

          ; //endcase

          if (captureThisOne, {
            foundBufnumIndex = bufnumIndex;
          });
        });

      });  // enddo -- basenameIndex

    };  // end -- block


    //
    if (foundBufnumIndex.notNil, {
      var  foundBuffer     = foundBufnumIndex[tokenBuffer];
      var  referenceCount  = foundBufnumIndex[tokenReferenceCount];

      if (incrementReferenceCount, {
        foundBufnumIndex[tokenReferenceCount] = referenceCount + 1;
      });

      if (quiet.not, {
        if (buffer.notNil, {
          if (buffer === foundBuffer, { identical = "IDENTICAL "; });
        });

        Log.info(thisFunction, 
              "FOUND %Buffer \"%\", bufnum=%, numChannels=%, channelArray=%.", 
                 identical, basename, 
                   foundBuffer.bufnum, foundBuffer.numChannels, foundBufnumIndex[tokenChannelArray]
            ).postln; 
      });

      ^foundBuffer;
    });

    ^nil;
  }




  //---------------------------------------------------------- -o--
  // Demo.

  *demo  
  {                                                     //FORKS
    var  soundsDir, soundFiles, soundFilePath;
    var  bufferA, bufferB, bufferC;

    var  oscMuted  = OSC.messageCountMuted;

    var  logContext  = thisFunction;


    //
    Log.info(thisFunction, "BEGIN").postln; 

    OSC.messageCountMuted = true;
    BufferCache.pretty().postln;


    //
    soundsDir   = Platform.resourceDir +/+ "sounds";
    soundFiles  = File.findFilesUnderArrayOfDirectoriesWithSuffixPattern(
                        [soundsDir], [\aif, \wav] 
                      ).scramble;

    soundFilePath = soundsDir +/+ soundFiles.removeAt(0);

    if (soundFilePath.isFile.not, {
      Log.error(thisFunction, 
                  "CANNOT FIND FILE.  Exiting demo...  (%)", 
                     soundFilePath
               ).postln; 
      ^'';
    });


    //
    {
                                // Initial load, no cache match.
                                // Stored in cache.
                                //
      "".postln;
      Log.info(logContext, "Load bufferA...").postln; 

      bufferA = Buffer.load(soundFilePath);
      Routine.spinlock( { bufferA.isUndefined.not; } );

      bufferA.pretty;

      BufferCache.pretty().postln;


                                // Load same file, search and find in cache.
                                // Stored only once in cache.
                                //
      "".postln;
      Log.info(logContext, "Load bufferB...").postln; 

      bufferB = Buffer.load(soundFilePath);
      Routine.spinlock( { bufferB.isUndefined.not; } );

      bufferB.pretty;

      BufferCache.pretty().postln;


                                // Load new file.
                                // Stored in cache.
                                //
      if (soundFiles.size > 0, {
        soundFilePath = soundsDir +/+ soundFiles.removeAt(0);

        "".postln;
        Log.info(logContext, "Load bufferC...").postln; 

        bufferC = Buffer.load(soundFilePath);
        Routine.spinlock( { bufferC.isUndefined.not; } );

        bufferC = bufferC.storeOrReturnDuplicate();
        bufferC.pretty;

        BufferCache.pretty().postln;
      });


      //
      "".postln;
      Log.info(logContext, "Release bufferA and bufferC...").postln; 

      bufferA.releaseBufferCacheEntry();

      if (bufferC.isUndefined.not, {
        bufferC.releaseBufferCacheEntry();
      });

      BufferCache.pretty().postln;


      //
      "".postln;
      Log.info(thisFunction, "END").postln; 

      OSC.messageCountMuted = oscMuted;

    }.fork;

    '';
  } // *demo




  //---------------------------------------------------------- -o--
  // MOS protocol methods.

  //------------------------ -o-
  *pretty  { | pattern, elisionLength, depth,
               casei, compileString, indent, initialCallee, 
               enumerate, bulletIndex, enumerationOffset,
               minimumSpacerSize, nolog, shortVersion
             |
    var  title              = this.classname.toUpper;

    var  maxKeyLength       = "bufferDictionary".size;

    var  bufferDictionaryOutput  = "";

    var  enviroA, 
         enviroArray;  // ADD


    //
    if (bufferDictionary.size <= 0, {
      bufferDictionaryOutput = Object.labelEmpty();
    }, {
      bufferDictionaryOutput = bufferDictionary.pretty(depth:4, indent:"\t");
      compileString = false;
    });


    //
    enviroA = (
      bufferDictionary:  bufferDictionaryOutput,
    );



    //
    enviroArray = [ enviroA,
                  ];
                        // ADD

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


} // BufferCache

